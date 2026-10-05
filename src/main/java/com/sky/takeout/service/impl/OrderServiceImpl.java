package com.sky.takeout.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.takeout.entity.AddressBook;
import com.sky.takeout.entity.OrderDetail;
import com.sky.takeout.entity.Orders;
import com.sky.takeout.entity.ShoppingCart;
import com.sky.takeout.mapper.AddressBookMapper;
import com.sky.takeout.mapper.OrderDetailMapper;
import com.sky.takeout.mapper.OrdersMapper;
import com.sky.takeout.mapper.ShoppingCartMapper;
import com.sky.takeout.mapper.CouponMapper;
import com.sky.takeout.mapper.UserCouponMapper;
import com.sky.takeout.entity.Coupon;
import com.sky.takeout.entity.UserCoupon;
import com.sky.takeout.service.NotificationService;
import com.sky.takeout.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrdersMapper ordersMapper;
    private final OrderDetailMapper detailMapper;
    private final ShoppingCartMapper cartMapper;
    private final AddressBookMapper addressMapper;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final NotificationService notifications;

    public OrderServiceImpl(OrdersMapper ordersMapper, OrderDetailMapper detailMapper,
                            ShoppingCartMapper cartMapper, AddressBookMapper addressMapper, CouponMapper couponMapper,
                            UserCouponMapper userCouponMapper, NotificationService notifications) {
        this.ordersMapper = ordersMapper;
        this.detailMapper = detailMapper;
        this.cartMapper = cartMapper;
        this.addressMapper = addressMapper;
        this.couponMapper = couponMapper; this.userCouponMapper = userCouponMapper; this.notifications = notifications;
    }

    @Override
    @Transactional
    public SubmitResult submit(Long userId, Long addressBookId, String remark, String couponCode) {
        AddressBook address = addressMapper.selectOne(new LambdaQueryWrapper<AddressBook>()
                .eq(AddressBook::getId, addressBookId)
                .eq(AddressBook::getUserId, userId));
        if (address == null) throw new IllegalArgumentException("Delivery address not found");

        List<ShoppingCart> carts = cartMapper.selectList(new LambdaQueryWrapper<ShoppingCart>()
                .eq(ShoppingCart::getUserId, userId));
        if (carts.isEmpty()) throw new IllegalArgumentException("Cart must not be empty");

        BigDecimal total = carts.stream()
                .map(item -> item.getAmount().multiply(BigDecimal.valueOf(item.getNumber())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = BigDecimal.ZERO; Coupon coupon = null; UserCoupon userCoupon = null; boolean consumeStock = false;
        if (couponCode != null && !couponCode.isBlank()) {
            coupon = couponMapper.selectOne(new LambdaQueryWrapper<Coupon>().eq(Coupon::getCode, couponCode.trim().toUpperCase()));
            LocalDateTime checkTime = LocalDateTime.now();
            if (coupon == null || coupon.getStatus() != 1 || checkTime.isBefore(coupon.getStartTime()) || checkTime.isAfter(coupon.getEndTime()) || total.compareTo(coupon.getThresholdAmount()) < 0) throw new IllegalArgumentException("Coupon is unavailable or the minimum spend is not met");
            userCoupon = userCouponMapper.selectOne(new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getUserId, userId).eq(UserCoupon::getCouponId, coupon.getId()));
            if (userCoupon != null && userCoupon.getStatus() == 1) throw new IllegalArgumentException("This coupon has already been used");
            if (userCoupon == null) { if (coupon.getRemainingStock() <= 0) throw new IllegalArgumentException("Coupon out of stock"); userCoupon = new UserCoupon(); userCoupon.setUserId(userId); userCoupon.setCouponId(coupon.getId()); userCoupon.setStatus(0); userCoupon.setClaimedTime(checkTime); userCouponMapper.insert(userCoupon); consumeStock = true; }
            discount = coupon.getDiscountAmount().min(total);
        }
        BigDecimal payable = total.subtract(discount).max(BigDecimal.ZERO);
        LocalDateTime now = LocalDateTime.now();
        Orders order = new Orders();
        order.setNumber(now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 8));
        order.setUserId(userId);
        order.setAddressBookId(addressBookId);
        order.setStatus(1);
        order.setUserDeleted(0);
        order.setAmount(payable);
        order.setOriginalAmount(total);
        order.setDiscountAmount(discount);
        order.setCouponCode(discount.signum() > 0 ? couponCode.toUpperCase() : null);
        order.setRemark(remark == null || remark.isBlank() ? null : remark.trim());
        order.setConsignee(address.getConsignee());
        order.setPhone(address.getPhone());
        order.setAddress(address.getDetail());
        order.setOrderTime(now);
        ordersMapper.insert(order);
        if (coupon != null) {
            if (consumeStock && couponMapper.decreaseStock(coupon.getId()) != 1)
                throw new IllegalArgumentException("Coupon out of stock");
            userCoupon.setStatus(1); userCoupon.setUsedTime(now); userCoupon.setOrderId(order.getId()); userCouponMapper.updateById(userCoupon);
        }

        for (ShoppingCart cart : carts) {
            OrderDetail detail = new OrderDetail();
            detail.setOrderId(order.getId());
            detail.setDishId(cart.getDishId());
            detail.setSetmealId(cart.getSetmealId());
            detail.setName(cart.getName());
            detail.setImage(cart.getImage());
            detail.setAmount(cart.getAmount());
            detail.setNumber(cart.getNumber());
            detailMapper.insert(detail);
        }
        cartMapper.delete(new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, userId));
        notifications.admin("NEW_ORDER", order);
        return new SubmitResult(order.getId(), order.getNumber(), payable, total, discount, order.getCouponCode());
    }

    @Override
    public List<Orders> list(Long userId, Integer status, String number) {
        return ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getUserId, userId).eq(Orders::getUserDeleted, 0)
                .eq(status != null, Orders::getStatus, status)
                .like(number != null && !number.isBlank(), Orders::getNumber, number)
                .orderByDesc(Orders::getOrderTime));
    }

    @Override
    public Orders getOwned(Long userId, Long orderId) {
        return ordersMapper.selectOne(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getId, orderId).eq(Orders::getUserId, userId).eq(Orders::getUserDeleted, 0));
    }

    @Override
    public List<OrderDetail> details(Long orderId) {
        return detailMapper.selectList(new LambdaQueryWrapper<OrderDetail>()
                .eq(OrderDetail::getOrderId, orderId));
    }

    @Override
    public String cancel(Long userId, Long orderId) {
        Orders order = getOwned(userId, orderId);
        if (order == null) return "Order not found";
        if (order.getStatus() != 1) return "Order status does not allow cancellation";
        order.setStatus(6);
        order.setCancelTime(LocalDateTime.now());
        ordersMapper.updateById(order);
        notifications.user(order.getUserId(), "ORDER_STATUS", order);
        return null;
    }

    @Override
    public String pay(Long userId, Long orderId) {
        Orders order = getOwned(userId, orderId);
        if (order == null) return "Order not found";
        if (order.getStatus() != 1) return "Order status does not allow payment";
        order.setStatus(2);
        order.setPayTime(LocalDateTime.now());
        ordersMapper.updateById(order);
        notifications.admin("ORDER_PAID", order);
        return null;
    }

    @Override
    public String remind(Long userId, Long orderId) {
        Orders order = getOwned(userId, orderId);
        if (order == null) return "Order not found";
        if (order.getStatus() != 2 && order.getStatus() != 3 && order.getStatus() != 4) {
            return "Order status does not allow a reminder";
        }
        order.setReminder(1);
        ordersMapper.updateById(order);
        notifications.admin("REMINDER", order);
        return null;
    }

    @Override
    public String deleteHistory(Long userId, Long orderId) {
        Orders order = getOwned(userId, orderId);
        if (order == null || Integer.valueOf(1).equals(order.getUserDeleted())) return "Order not found";
        if (order.getStatus() != 5 && order.getStatus() != 6) return "Only completed or cancelled orders can be deleted";
        order.setUserDeleted(1);
        ordersMapper.updateById(order);
        return null;
    }

    @Override
    public String changeStatus(Long orderId, Integer expectedStatus, Integer newStatus) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) return "Order not found";
        if (!expectedStatus.equals(order.getStatus())) return "Order status does not allow this operation";
        order.setStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();
        if (newStatus == 4) order.setDeliveryTime(now);
        if (newStatus == 5) order.setCheckoutTime(now);
        if (newStatus == 6) order.setCancelTime(now);
        ordersMapper.updateById(order);
        notifications.user(order.getUserId(), "ORDER_STATUS", order);
        return null;
    }

    @Override
    @Transactional
    public int cancelUnpaidOrders(LocalDateTime deadline) {
        List<Orders> expired = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getStatus, 1).lt(Orders::getOrderTime, deadline));
        for (Orders order : expired) {
            order.setStatus(6);
            order.setCancelTime(LocalDateTime.now());
            ordersMapper.updateById(order);
        }
        return expired.size();
    }
}

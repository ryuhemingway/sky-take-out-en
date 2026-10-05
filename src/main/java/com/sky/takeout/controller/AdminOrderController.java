package com.sky.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.takeout.common.ApiResponse;
import com.sky.takeout.entity.Orders;
import com.sky.takeout.mapper.OrdersMapper;
import com.sky.takeout.mapper.DeliveryMapper;
import com.sky.takeout.mapper.RiderMapper;
import com.sky.takeout.entity.Delivery;
import com.sky.takeout.entity.Rider;
import com.sky.takeout.service.OrderService;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.LinkedHashMap;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/order")
public class AdminOrderController {
    private final OrdersMapper mapper;
    private final OrderService service;
    private final DeliveryMapper deliveryMapper;
    private final RiderMapper riderMapper;

    public AdminOrderController(OrdersMapper mapper, OrderService service, DeliveryMapper deliveryMapper, RiderMapper riderMapper) {
        this.mapper = mapper;
        this.service = service;
        this.deliveryMapper = deliveryMapper;
        this.riderMapper = riderMapper;
    }

    @GetMapping("/page")
    public ApiResponse<List<Orders>> page(@RequestParam(name = "status", required = false) Integer status,
                                          @RequestParam(name = "number", required = false) String number,
                                          @RequestParam(name = "userId", required = false) Long userId,
                                          @RequestParam(name = "beginTime", required = false) LocalDateTime beginTime,
                                          @RequestParam(name = "endTime", required = false) LocalDateTime endTime) {
        return ApiResponse.ok(mapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(status != null, Orders::getStatus, status)
                .like(number != null && !number.isBlank(), Orders::getNumber, number)
                .eq(userId != null, Orders::getUserId, userId)
                .ge(beginTime != null, Orders::getOrderTime, beginTime)
                .le(endTime != null, Orders::getOrderTime, endTime)
                .orderByDesc(Orders::getOrderTime)));
    }

    @GetMapping("/statistics")
    public ApiResponse<LinkedHashMap<String, Object>> statistics(
            @RequestParam(name = "beginTime", required = false) LocalDateTime beginTime,
            @RequestParam(name = "endTime", required = false) LocalDateTime endTime) {
        List<Orders> orders = mapper.selectList(new LambdaQueryWrapper<Orders>()
                .ge(beginTime != null, Orders::getOrderTime, beginTime)
                .le(endTime != null, Orders::getOrderTime, endTime));
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("totalCount", orders.size());
        result.put("totalAmount", orders.stream().map(Orders::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        result.put("completedAmount", orders.stream().filter(o -> o.getStatus() == 5)
                .map(Orders::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        for (int status = 1; status <= 6; status++) {
            final int current = status;
            result.put("status" + status + "Count", orders.stream().filter(o -> current == o.getStatus()).count());
        }
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderController.OrderView> detail(@PathVariable("id") Long id) {
        Orders order = mapper.selectById(id);
        return order == null ? ApiResponse.fail("Order not found")
                : ApiResponse.ok(new OrderController.OrderView(order, service.details(id)));
    }

    @PutMapping("/{id}/confirm")
    public ApiResponse<Void> confirm(@PathVariable("id") Long id) { return result(service.changeStatus(id, 2, 3)); }

    @PutMapping("/{id}/delivery")
    @Transactional
    public ApiResponse<Void> delivery(@PathVariable("id") Long id) {
        Delivery delivery = deliveryMapper.selectOne(new LambdaQueryWrapper<Delivery>().eq(Delivery::getOrderId, id));
        if (delivery == null || delivery.getStatus() != 1) return ApiResponse.fail("Assign a rider to the order first");
        delivery.setStatus(2); delivery.setPickedTime(LocalDateTime.now()); deliveryMapper.updateById(delivery);
        return result(service.changeStatus(id, 3, 4));
    }

    @PutMapping("/{id}/complete")
    @Transactional
    public ApiResponse<Void> complete(@PathVariable("id") Long id) {
        Delivery delivery = deliveryMapper.selectOne(new LambdaQueryWrapper<Delivery>().eq(Delivery::getOrderId, id));
        if (delivery == null || delivery.getStatus() != 2) return ApiResponse.fail("Invalid delivery status");
        delivery.setStatus(3); delivery.setDeliveredTime(LocalDateTime.now()); deliveryMapper.updateById(delivery);
        Rider rider = riderMapper.selectById(delivery.getRiderId());
        if (rider != null) { rider.setStatus(1); riderMapper.updateById(rider); }
        return result(service.changeStatus(id, 4, 5));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable("id") Long id) {
        Orders order = mapper.selectById(id);
        if (order == null) return ApiResponse.fail("Order not found");
        if (order.getStatus() == 5 || order.getStatus() == 6) return ApiResponse.fail("Order status does not allow cancellation");
        return result(service.changeStatus(id, order.getStatus(), 6));
    }

    private ApiResponse<Void> result(String error) {
        return error == null ? ApiResponse.ok() : ApiResponse.fail(error);
    }
}

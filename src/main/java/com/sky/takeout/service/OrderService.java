package com.sky.takeout.service;

import com.sky.takeout.entity.OrderDetail;
import com.sky.takeout.entity.Orders;
import java.math.BigDecimal;
import java.util.List;

public interface OrderService {
    SubmitResult submit(Long userId, Long addressBookId, String remark, String couponCode);
    List<Orders> list(Long userId, Integer status, String number);
    Orders getOwned(Long userId, Long orderId);
    List<OrderDetail> details(Long orderId);
    String cancel(Long userId, Long orderId);
    String pay(Long userId, Long orderId);
    String remind(Long userId, Long orderId);
    String deleteHistory(Long userId, Long orderId);
    String changeStatus(Long orderId, Integer expectedStatus, Integer newStatus);
    int cancelUnpaidOrders(java.time.LocalDateTime deadline);

    record SubmitResult(Long id, String number, BigDecimal amount, BigDecimal originalAmount, BigDecimal discount, String couponCode) {}
}

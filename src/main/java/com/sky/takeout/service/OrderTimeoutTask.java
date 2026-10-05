package com.sky.takeout.service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
@Component
public class OrderTimeoutTask {
    private final OrderService orderService;
    public OrderTimeoutTask(OrderService orderService) { this.orderService = orderService; }
    @Scheduled(fixedDelay = 300000)
    public void cancelExpiredUnpaidOrders() { orderService.cancelUnpaidOrders(LocalDateTime.now().minusMinutes(30)); }
}

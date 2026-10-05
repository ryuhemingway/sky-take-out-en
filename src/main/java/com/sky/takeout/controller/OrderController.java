package com.sky.takeout.controller;

import com.sky.takeout.common.ApiResponse;
import com.sky.takeout.entity.OrderDetail;
import com.sky.takeout.entity.Orders;
import com.sky.takeout.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.sky.takeout.service.ShopStatusService;

@RestController
@RequestMapping("/api/user/order")
public class OrderController {
    private final OrderService service;
    private final ShopStatusService shopStatusService;

    public OrderController(OrderService service, ShopStatusService shopStatusService) {
        this.service = service; this.shopStatusService = shopStatusService;
    }

    @PostMapping("/submit")
    public ApiResponse<OrderService.SubmitResult> submit(@RequestAttribute("userId") Long userId,
                                                          @Valid @RequestBody SubmitRequest request) {
        try {
            if (shopStatusService.getStatus() == 0) return ApiResponse.fail("Shop is closed, checkout failed");
            return ApiResponse.ok(service.submit(userId, request.addressBookId, request.remark, request.couponCode));
        } catch (IllegalArgumentException exception) {
            return ApiResponse.fail(exception.getMessage());
        }
    }

    @GetMapping("/list")
    public ApiResponse<List<Orders>> list(@RequestAttribute("userId") Long userId,
                                          @RequestParam(name = "status", required = false) Integer status,
                                          @RequestParam(name = "number", required = false) String number) {
        return ApiResponse.ok(service.list(userId, status, number));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderView> detail(@RequestAttribute("userId") Long userId,
                                         @PathVariable("id") Long id) {
        Orders order = service.getOwned(userId, id);
        if (order == null) return ApiResponse.fail("Order not found");
        return ApiResponse.ok(new OrderView(order, service.details(id)));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@RequestAttribute("userId") Long userId,
                                    @PathVariable("id") Long id) {
        String error = service.cancel(userId, id);
        return error == null ? ApiResponse.ok() : ApiResponse.fail(error);
    }

    @PutMapping("/{id}/pay")
    public ApiResponse<Void> pay(@RequestAttribute("userId") Long userId,
                                 @PathVariable("id") Long id) {
        String error = service.pay(userId, id);
        return error == null ? ApiResponse.ok() : ApiResponse.fail(error);
    }

    @PutMapping("/{id}/reminder")
    public ApiResponse<Void> remind(@RequestAttribute("userId") Long userId,
                                    @PathVariable("id") Long id) {
        String error = service.remind(userId, id);
        return error == null ? ApiResponse.ok() : ApiResponse.fail(error);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteHistory(@RequestAttribute("userId") Long userId,
                                           @PathVariable("id") Long id) {
        String error = service.deleteHistory(userId, id);
        return error == null ? ApiResponse.ok() : ApiResponse.fail(error);
    }

    public static class SubmitRequest {
        @NotNull(message = "Delivery address is required")
        public Long addressBookId;
        @Size(max = 255, message = "Order note must not exceed 255 characters")
        public String remark;
        public String couponCode;
    }

    public record OrderView(Orders order, List<OrderDetail> details) {}
}

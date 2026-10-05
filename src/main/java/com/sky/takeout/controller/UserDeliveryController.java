package com.sky.takeout.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.takeout.common.ApiResponse;
import com.sky.takeout.entity.Delivery;
import com.sky.takeout.entity.Orders;
import com.sky.takeout.entity.Rider;
import com.sky.takeout.mapper.DeliveryMapper;
import com.sky.takeout.mapper.OrdersMapper;
import com.sky.takeout.mapper.RiderMapper;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/user/delivery")
public class UserDeliveryController {
 private final DeliveryMapper deliveries;private final RiderMapper riders;private final OrdersMapper orders;
 public UserDeliveryController(DeliveryMapper d,RiderMapper r,OrdersMapper o){deliveries=d;riders=r;orders=o;}
 public record View(Integer status,String riderName,String riderPhone,java.time.LocalDateTime assignedTime,java.time.LocalDateTime pickedTime,java.time.LocalDateTime deliveredTime){}
 @GetMapping("/order/{orderId}") public ApiResponse<View> get(@RequestAttribute("userId") Long uid,@PathVariable Long orderId){Orders o=orders.selectById(orderId);if(o==null||!uid.equals(o.getUserId()))return ApiResponse.fail("Order not found");Delivery d=deliveries.selectOne(new LambdaQueryWrapper<Delivery>().eq(Delivery::getOrderId,orderId));if(d==null)return ApiResponse.ok(null);Rider r=riders.selectById(d.getRiderId());return ApiResponse.ok(new View(d.getStatus(),r==null?null:r.getName(),r==null?null:r.getPhone(),d.getAssignedTime(),d.getPickedTime(),d.getDeliveredTime()));}
}

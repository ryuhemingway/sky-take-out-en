package com.sky.takeout.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.takeout.common.ApiResponse;
import com.sky.takeout.entity.Delivery;
import com.sky.takeout.entity.Orders;
import com.sky.takeout.entity.Rider;
import com.sky.takeout.mapper.DeliveryMapper;
import com.sky.takeout.mapper.OrdersMapper;
import com.sky.takeout.mapper.RiderMapper;
import com.sky.takeout.service.NotificationService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController @RequestMapping("/api/admin/delivery")
public class AdminDeliveryController {
 private final DeliveryMapper deliveries; private final RiderMapper riders; private final OrdersMapper orders; private final NotificationService notifications;
 public AdminDeliveryController(DeliveryMapper d,RiderMapper r,OrdersMapper o,NotificationService n){deliveries=d;riders=r;orders=o;notifications=n;}
 public record RiderRequest(String name,String phone){}
 public record DeliveryView(Delivery delivery,Rider rider,Orders order){}
 @GetMapping("/riders") public ApiResponse<List<Rider>> riders(){return ApiResponse.ok(riders.selectList(new LambdaQueryWrapper<Rider>().orderByDesc(Rider::getCreateTime)));}
 @PostMapping("/riders") public ApiResponse<Long> addRider(@RequestBody RiderRequest request){if(request.name()==null||request.name().isBlank()||request.phone()==null||request.phone().isBlank())return ApiResponse.fail("Rider name and phone number are required"); Rider r=new Rider();r.setName(request.name().trim());r.setPhone(request.phone().trim());r.setStatus(1);r.setCreateTime(LocalDateTime.now());riders.insert(r);return ApiResponse.ok(r.getId());}
 @PutMapping("/riders/{id}/status/{status}") public ApiResponse<Void> riderStatus(@PathVariable Long id,@PathVariable Integer status){if(status<0||status>2)return ApiResponse.fail("Invalid rider status");Rider r=riders.selectById(id);if(r==null)return ApiResponse.fail("Rider not found");r.setStatus(status);riders.updateById(r);return ApiResponse.ok();}
 @GetMapping("/page") public ApiResponse<List<DeliveryView>> page(){return ApiResponse.ok(deliveries.selectList(new LambdaQueryWrapper<Delivery>().orderByDesc(Delivery::getAssignedTime)).stream().map(d->new DeliveryView(d,riders.selectById(d.getRiderId()),orders.selectById(d.getOrderId()))).toList());}
 @PostMapping("/order/{orderId}/assign/{riderId}") @Transactional public ApiResponse<Void> assign(@PathVariable Long orderId,@PathVariable Long riderId){Orders o=orders.selectById(orderId);Rider r=riders.selectById(riderId);if(o==null)return ApiResponse.fail("Order not found");if(o.getStatus()!=3)return ApiResponse.fail("Only accepted orders can be assigned a rider");if(r==null||r.getStatus()!=1)return ApiResponse.fail("Rider is currently unavailable");Delivery d=deliveries.selectOne(new LambdaQueryWrapper<Delivery>().eq(Delivery::getOrderId,orderId));if(d==null){d=new Delivery();d.setOrderId(orderId);}d.setRiderId(riderId);d.setStatus(1);d.setAssignedTime(LocalDateTime.now());if(d.getId()==null)deliveries.insert(d);else deliveries.updateById(d);r.setStatus(2);riders.updateById(r);notifications.user(o.getUserId(),"RIDER_ASSIGNED",new DeliveryView(d,r,o));return ApiResponse.ok();}
 @PutMapping("/order/{orderId}/pickup") @Transactional public ApiResponse<Void> pickup(@PathVariable Long orderId){Delivery d=find(orderId);if(d==null||d.getStatus()!=1)return ApiResponse.fail("Delivery not assigned or has an invalid status");Orders o=orders.selectById(orderId);d.setStatus(2);d.setPickedTime(LocalDateTime.now());deliveries.updateById(d);o.setStatus(4);o.setDeliveryTime(LocalDateTime.now());orders.updateById(o);notifications.user(o.getUserId(),"DELIVERY_PICKED",new DeliveryView(d,riders.selectById(d.getRiderId()),o));return ApiResponse.ok();}
 @PutMapping("/order/{orderId}/delivered") @Transactional public ApiResponse<Void> delivered(@PathVariable Long orderId){Delivery d=find(orderId);if(d==null||d.getStatus()!=2)return ApiResponse.fail("Invalid delivery status");Orders o=orders.selectById(orderId);d.setStatus(3);d.setDeliveredTime(LocalDateTime.now());deliveries.updateById(d);o.setStatus(5);o.setCheckoutTime(LocalDateTime.now());orders.updateById(o);Rider r=riders.selectById(d.getRiderId());r.setStatus(1);riders.updateById(r);notifications.user(o.getUserId(),"DELIVERY_COMPLETED",new DeliveryView(d,r,o));return ApiResponse.ok();}
 private Delivery find(Long orderId){return deliveries.selectOne(new LambdaQueryWrapper<Delivery>().eq(Delivery::getOrderId,orderId));}
}

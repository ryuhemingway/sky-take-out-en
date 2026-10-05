package com.sky.takeout.controller;
import com.sky.takeout.common.ApiResponse;
import com.sky.takeout.service.ShopStatusService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/shop/status")
public class ShopStatusController {
    private final ShopStatusService service;
    public ShopStatusController(ShopStatusService service) { this.service=service; }
    @GetMapping public ApiResponse<Map<String,Integer>> get() { return ApiResponse.ok(Map.of("status",service.getStatus())); }
    @PutMapping public ApiResponse<Void> set(@RequestParam(name="status") Integer status) { if(status==null||status<0||status>1)return ApiResponse.fail("Status must be 0 or 1"); service.setStatus(status); return ApiResponse.ok(); }
}

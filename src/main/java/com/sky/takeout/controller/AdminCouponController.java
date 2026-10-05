package com.sky.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.takeout.common.ApiResponse;
import com.sky.takeout.entity.Coupon;
import com.sky.takeout.mapper.CouponMapper;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/coupon")
public class AdminCouponController {
    private final CouponMapper mapper;
    public AdminCouponController(CouponMapper mapper) { this.mapper = mapper; }

    @GetMapping("/page")
    public ApiResponse<List<Coupon>> page() {
        return ApiResponse.ok(mapper.selectList(new LambdaQueryWrapper<Coupon>().orderByDesc(Coupon::getCreateTime)));
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody Coupon coupon) {
        if (coupon.getCode() == null || coupon.getCode().isBlank() || coupon.getName() == null || coupon.getName().isBlank()
                || coupon.getDiscountAmount() == null || coupon.getThresholdAmount() == null || coupon.getTotalStock() == null
                || coupon.getTotalStock() < 1 || coupon.getStartTime() == null || coupon.getEndTime() == null
                || !coupon.getEndTime().isAfter(coupon.getStartTime())) return ApiResponse.fail("Coupon parameters are incomplete or the validity period is invalid");
        if (mapper.selectOne(new LambdaQueryWrapper<Coupon>().eq(Coupon::getCode, coupon.getCode().trim().toUpperCase())) != null)
            return ApiResponse.fail("Coupon code already exists");
        coupon.setCode(coupon.getCode().trim().toUpperCase()); coupon.setName(coupon.getName().trim());
        coupon.setRemainingStock(coupon.getTotalStock()); coupon.setStatus(coupon.getStatus() == null ? 1 : coupon.getStatus());
        coupon.setCreateTime(LocalDateTime.now()); mapper.insert(coupon); return ApiResponse.ok(coupon.getId());
    }

    @PutMapping("/{id}/status/{status}")
    public ApiResponse<Void> status(@PathVariable Long id, @PathVariable Integer status) {
        if (status != 0 && status != 1) return ApiResponse.fail("Invalid coupon status");
        Coupon coupon = mapper.selectById(id); if (coupon == null) return ApiResponse.fail("Coupon not found");
        coupon.setStatus(status); mapper.updateById(coupon); return ApiResponse.ok();
    }
}

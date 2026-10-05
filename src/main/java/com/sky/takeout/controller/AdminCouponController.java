package com.sky.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.takeout.common.ApiResponse;
import com.sky.takeout.entity.Coupon;
import com.sky.takeout.entity.UserCoupon;
import com.sky.takeout.mapper.CouponMapper;
import com.sky.takeout.mapper.UserCouponMapper;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/coupon")
public class AdminCouponController {
    private static final String INVALID = "Coupon parameters are incomplete or the validity period is invalid";
    private final CouponMapper mapper;
    private final UserCouponMapper userCoupons;
    public AdminCouponController(CouponMapper mapper, UserCouponMapper userCoupons) { this.mapper = mapper; this.userCoupons = userCoupons; }
    public record CouponRequest(Long id, String code, String name, BigDecimal discountAmount, BigDecimal thresholdAmount,
                                Integer totalStock, LocalDateTime startTime, LocalDateTime endTime) {}

    @GetMapping("/page")
    public ApiResponse<List<Coupon>> page() {
        return ApiResponse.ok(mapper.selectList(new LambdaQueryWrapper<Coupon>().orderByDesc(Coupon::getCreateTime)));
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody Coupon coupon) {
        if (invalid(coupon)) return ApiResponse.fail(INVALID);
        if (mapper.selectOne(new LambdaQueryWrapper<Coupon>().eq(Coupon::getCode, coupon.getCode().trim().toUpperCase())) != null)
            return ApiResponse.fail("Coupon code already exists");
        coupon.setCode(coupon.getCode().trim().toUpperCase()); coupon.setName(coupon.getName().trim());
        coupon.setRemainingStock(coupon.getTotalStock()); coupon.setStatus(coupon.getStatus() == null ? 1 : coupon.getStatus());
        coupon.setCreateTime(LocalDateTime.now()); mapper.insert(coupon); return ApiResponse.ok(coupon.getId());
    }

    @PutMapping
    public ApiResponse<Void> update(@RequestBody CouponRequest r) {
        Coupon old = r.id() == null ? null : mapper.selectById(r.id());
        if (old == null) return ApiResponse.fail("Coupon not found");
        Coupon coupon = new Coupon(); coupon.setId(old.getId()); coupon.setCode(r.code()); coupon.setName(r.name());
        coupon.setDiscountAmount(r.discountAmount()); coupon.setThresholdAmount(r.thresholdAmount()); coupon.setTotalStock(r.totalStock());
        coupon.setStartTime(r.startTime()); coupon.setEndTime(r.endTime());
        if (invalid(coupon)) return ApiResponse.fail(INVALID);
        coupon.setCode(coupon.getCode().trim().toUpperCase()); coupon.setName(coupon.getName().trim());
        if (mapper.selectCount(new LambdaQueryWrapper<Coupon>().eq(Coupon::getCode, coupon.getCode()).ne(Coupon::getId, old.getId())) > 0)
            return ApiResponse.fail("Coupon code already exists");
        // ponytail: read-then-write, a claim landing between the read and this update is lost; move to one conditional UPDATE if that matters
        int claimed = old.getTotalStock() - old.getRemainingStock();
        if (coupon.getTotalStock() < claimed) return ApiResponse.fail("Total stock cannot be lower than the number already claimed");
        coupon.setRemainingStock(coupon.getTotalStock() - claimed);
        mapper.updateById(coupon); return ApiResponse.ok();
    }

    @PutMapping("/{id}/status/{status}")
    public ApiResponse<Void> status(@PathVariable Long id, @PathVariable Integer status) {
        if (status != 0 && status != 1) return ApiResponse.fail("Invalid coupon status");
        Coupon coupon = mapper.selectById(id); if (coupon == null) return ApiResponse.fail("Coupon not found");
        coupon.setStatus(status); mapper.updateById(coupon); return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        if (mapper.selectById(id) == null) return ApiResponse.fail("Coupon not found");
        if (userCoupons.selectCount(new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getCouponId, id)) > 0)
            return ApiResponse.fail("Coupon has been claimed; disable it instead");
        mapper.deleteById(id); return ApiResponse.ok();
    }

    private static boolean invalid(Coupon c) {
        return c.getCode() == null || c.getCode().isBlank() || c.getName() == null || c.getName().isBlank()
                || c.getDiscountAmount() == null || c.getThresholdAmount() == null || c.getTotalStock() == null
                || c.getTotalStock() < 1 || c.getStartTime() == null || c.getEndTime() == null
                || !c.getEndTime().isAfter(c.getStartTime());
    }
}

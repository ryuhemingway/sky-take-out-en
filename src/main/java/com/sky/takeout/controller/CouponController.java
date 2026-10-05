package com.sky.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.takeout.common.ApiResponse;
import com.sky.takeout.entity.Coupon;
import com.sky.takeout.entity.UserCoupon;
import com.sky.takeout.mapper.CouponMapper;
import com.sky.takeout.mapper.UserCouponMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/user/coupon")
public class CouponController {
    private final CouponMapper coupons;
    private final UserCouponMapper userCoupons;

    public CouponController(CouponMapper coupons, UserCouponMapper userCoupons) {
        this.coupons = coupons;
        this.userCoupons = userCoupons;
    }

    public record CouponView(Long id, String code, String name, BigDecimal discount,
                             BigDecimal threshold, Integer remainingStock, Integer claimed) {}

    @GetMapping("/list")
    public ApiResponse<List<CouponView>> list(@RequestAttribute("userId") Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<CouponView> result = coupons.selectList(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, 1).le(Coupon::getStartTime, now).ge(Coupon::getEndTime, now)
                .orderByAsc(Coupon::getThresholdAmount)).stream().map(coupon -> {
            UserCoupon owned = userCoupons.selectOne(new LambdaQueryWrapper<UserCoupon>()
                    .eq(UserCoupon::getUserId, userId).eq(UserCoupon::getCouponId, coupon.getId()));
            return new CouponView(coupon.getId(), coupon.getCode(), coupon.getName(),
                    coupon.getDiscountAmount(), coupon.getThresholdAmount(), coupon.getRemainingStock(),
                    owned == null ? 0 : 1);
        }).filter(view -> view.remainingStock() > 0 || view.claimed() == 1).toList();
        return ApiResponse.ok(result);
    }

    @PostMapping("/{id}/claim")
    @Transactional
    public ApiResponse<Void> claim(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        Coupon coupon = coupons.selectById(id);
        LocalDateTime now = LocalDateTime.now();
        if (coupon == null || coupon.getStatus() != 1 || now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            return ApiResponse.fail("Coupon cannot be claimed");
        }
        if (userCoupons.selectOne(new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, id)) != null) return ApiResponse.fail("You have already claimed this coupon");
        if (coupons.decreaseStock(id) != 1) return ApiResponse.fail("Coupon out of stock");
        UserCoupon owned = new UserCoupon();
        owned.setUserId(userId); owned.setCouponId(id); owned.setStatus(0); owned.setClaimedTime(now);
        userCoupons.insert(owned);
        return ApiResponse.ok();
    }

    @GetMapping("/my")
    public ApiResponse<List<UserCoupon>> mine(@RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(userCoupons.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId).orderByDesc(UserCoupon::getClaimedTime)));
    }
}

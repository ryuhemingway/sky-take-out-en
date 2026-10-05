package com.sky.takeout.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.takeout.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {
    @Update("UPDATE coupon SET remaining_stock = remaining_stock - 1 WHERE id = #{id} AND remaining_stock > 0")
    int decreaseStock(Long id);
}

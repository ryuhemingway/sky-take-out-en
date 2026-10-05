package com.sky.takeout.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data public class Coupon { @TableId(type=IdType.AUTO) private Long id; private String code; private String name; private BigDecimal discountAmount; private BigDecimal thresholdAmount; private Integer totalStock; private Integer remainingStock; private Integer status; private LocalDateTime startTime; private LocalDateTime endTime; private LocalDateTime createTime; }

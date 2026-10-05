package com.sky.takeout.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data public class ShoppingCart {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long dishId;
    private Long setmealId;
    private String name;
    private String image;
    private BigDecimal amount;
    private Integer number;
    private LocalDateTime createTime;
}

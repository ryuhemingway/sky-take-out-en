package com.sky.takeout.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
@Data public class SetmealDish {
    @TableId(type = IdType.AUTO) private Long id;
    private Long setmealId;
    private Long dishId;
    private Integer copies;
}

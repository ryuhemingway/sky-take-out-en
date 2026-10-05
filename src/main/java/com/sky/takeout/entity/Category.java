package com.sky.takeout.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class Category {
    @TableId(type = IdType.AUTO) private Long id;
    @NotBlank(message = "Category type is required") private String type;
    @NotBlank(message = "Category name is required")
    private String name;
    @NotNull(message = "Sort order is required") @Min(value = 0, message = "Sort order must not be less than 0")
    private Integer sort;
    @NotNull(message = "Status is required") @Min(0) @Max(1)
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

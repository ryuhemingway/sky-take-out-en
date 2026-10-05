package com.sky.takeout.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class Setmeal {
    @TableId(type = IdType.AUTO) private Long id;
    @NotBlank(message = "Set meal name is required") private String name;
    @NotNull(message = "Category is required") private Long categoryId;
    @NotNull @DecimalMin(value = "0.01", message = "Price must be greater than 0") private BigDecimal price;
    private String image; private String description; private Integer status;
    private LocalDateTime createTime; private LocalDateTime updateTime;
}

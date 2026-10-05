package com.sky.takeout.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AddressBook {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    @NotBlank(message = "Recipient is required")
    private String consignee;
    private String sex;
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^1\\d{10}$", message = "Invalid phone number format")
    private String phone;
    @NotBlank(message = "Detailed address is required")
    private String detail;
    private String label;
    private Integer isDefault;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

package com.sky.takeout.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Orders {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String number;
    private Long userId;
    private Long addressBookId;
    private Integer status;
    private Integer reminder;
    private Integer userDeleted;
    private BigDecimal amount;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private String couponCode;
    private String remark;
    private String consignee;
    private String phone;
    private String address;
    private LocalDateTime orderTime;
    private LocalDateTime payTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime checkoutTime;
    private LocalDateTime cancelTime;
}

package com.sky.takeout.entity;
import com.baomidou.mybatisplus.annotation.IdType; import com.baomidou.mybatisplus.annotation.TableId; import lombok.Data; import java.time.LocalDateTime;
@Data public class UserCoupon { @TableId(type=IdType.AUTO) private Long id; private Long userId; private Long couponId; private Integer status; private LocalDateTime claimedTime; private LocalDateTime usedTime; private Long orderId; }

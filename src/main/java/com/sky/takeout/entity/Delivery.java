package com.sky.takeout.entity;
import com.baomidou.mybatisplus.annotation.IdType; import com.baomidou.mybatisplus.annotation.TableId; import lombok.Data; import java.time.LocalDateTime;
@Data public class Delivery { @TableId(type=IdType.AUTO) private Long id; private Long orderId; private Long riderId; private Integer status; private LocalDateTime assignedTime; private LocalDateTime pickedTime; private LocalDateTime deliveredTime; }

package com.sky.takeout.entity;
import com.baomidou.mybatisplus.annotation.IdType; import com.baomidou.mybatisplus.annotation.TableId; import lombok.Data; import java.time.LocalDateTime;
@Data public class Rider { @TableId(type=IdType.AUTO) private Long id; private String name; private String phone; private Integer status; private LocalDateTime createTime; }

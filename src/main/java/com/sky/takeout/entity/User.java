package com.sky.takeout.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class User {
    @TableId(type = IdType.AUTO) private Long id;
    private String username; private String name; private String phone; private String avatar;
    private LocalDateTime createTime; private LocalDateTime updateTime;
}

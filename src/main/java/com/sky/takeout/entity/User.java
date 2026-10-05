package com.sky.takeout.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class User {
    @TableId(type = IdType.AUTO) private Long id;
    private String username;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) private String password;
    private String name; private String phone; private String avatar;
    private LocalDateTime createTime; private LocalDateTime updateTime;
}

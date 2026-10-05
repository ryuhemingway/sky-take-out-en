package com.sky.takeout.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.takeout.common.ApiResponse;
import com.sky.takeout.entity.User;
import com.sky.takeout.mapper.UserMapper;
import com.sky.takeout.security.JwtService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;
@RestController @RequestMapping("/api/user")
public class UserController {
    private final UserMapper mapper; private final JwtService jwtService;
    public UserController(UserMapper mapper, JwtService jwtService){this.mapper=mapper;this.jwtService=jwtService;}
    @PostMapping("/login") public ApiResponse<?> login(@RequestBody LoginRequest request){
        if(request.username==null||request.username.isBlank())return ApiResponse.fail("Username is required");
        User user=mapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername,request.username));
        if(user==null){user=new User();user.setUsername(request.username);user.setName(request.name==null?"Web User":request.name);user.setCreateTime(LocalDateTime.now());user.setUpdateTime(LocalDateTime.now());mapper.insert(user);}
        return ApiResponse.ok(Map.of("id",user.getId(),"name",user.getName(),"token",jwtService.createUser(user.getId(),user.getUsername())));
    }
    public static class LoginRequest { public String username; public String name; }
}

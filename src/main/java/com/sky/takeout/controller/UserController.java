package com.sky.takeout.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.takeout.common.ApiResponse;
import com.sky.takeout.entity.User;
import com.sky.takeout.mapper.UserMapper;
import com.sky.takeout.security.JwtService;
import com.sky.takeout.service.UserAccountService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;
@RestController @RequestMapping("/api/user")
public class UserController {
    private final UserMapper mapper; private final JwtService jwtService; private final PasswordEncoder encoder; private final UserAccountService accounts;
    public UserController(UserMapper mapper, JwtService jwtService, PasswordEncoder encoder, UserAccountService accounts){this.mapper=mapper;this.jwtService=jwtService;this.encoder=encoder;this.accounts=accounts;}
    public record LoginRequest(String username, String password) {}
    public record RegisterRequest(String username, String password, String name, String phone) {}
    public record ProfileRequest(String name, String phone) {}
    public record PasswordRequest(String oldPassword, String newPassword) {}
    public record ProfileView(Long id, String username, String name, String phone, LocalDateTime createTime) {}

    @PostMapping("/register") public ApiResponse<?> register(@RequestBody RegisterRequest r){
        String error=accounts.checkNew(r.username(),r.password(),r.name(),r.phone());
        return error!=null?ApiResponse.fail(error):session(accounts.create(r.username(),r.password(),r.name(),r.phone()));
    }
    @PostMapping("/login") public ApiResponse<?> login(@RequestBody LoginRequest r){
        User user=r.username()==null||r.password()==null?null:mapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername,r.username().trim()));
        if(user==null||!encoder.matches(r.password(),user.getPassword()))return ApiResponse.fail("Incorrect username or password");
        return session(user);
    }
    @GetMapping("/profile") public ApiResponse<ProfileView> profile(@RequestAttribute("userId") Long userId){
        User u=mapper.selectById(userId);
        return u==null?ApiResponse.fail("User not found"):ApiResponse.ok(new ProfileView(u.getId(),u.getUsername(),u.getName(),u.getPhone(),u.getCreateTime()));
    }
    @PutMapping("/profile") public ApiResponse<Void> updateProfile(@RequestAttribute("userId") Long userId, @RequestBody ProfileRequest r){
        String error=accounts.update(userId,r.name(),r.phone(),null); return error!=null?ApiResponse.fail(error):ApiResponse.ok();
    }
    @PutMapping("/profile/password") public ApiResponse<Void> changePassword(@RequestAttribute("userId") Long userId, @RequestBody PasswordRequest r){
        String error=accounts.changePassword(userId,r.oldPassword(),r.newPassword()); return error!=null?ApiResponse.fail(error):ApiResponse.ok();
    }
    @DeleteMapping("/profile") public ApiResponse<Void> deleteAccount(@RequestAttribute("userId") Long userId){
        String error=accounts.delete(userId); return error!=null?ApiResponse.fail(error):ApiResponse.ok();
    }
    private ApiResponse<?> session(User user){return ApiResponse.ok(Map.of("id",user.getId(),"name",user.getName(),"token",jwtService.createUser(user.getId(),user.getUsername())));}
}

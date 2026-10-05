package com.sky.takeout.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.takeout.common.ApiResponse;
import com.sky.takeout.entity.User;
import com.sky.takeout.mapper.UserMapper;
import com.sky.takeout.service.UserAccountService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/admin/user")
public class AdminUserController {
    private final UserMapper mapper; private final UserAccountService accounts;
    public AdminUserController(UserMapper mapper, UserAccountService accounts){this.mapper=mapper;this.accounts=accounts;}
    public record UpdateRequest(Long id, String name, String phone, String password) {}

    @GetMapping("/page") public ApiResponse<List<User>> page(@RequestParam(name="keyword", required=false) String keyword){
        String kw=keyword==null?"":keyword.trim();
        return ApiResponse.ok(mapper.selectList(new LambdaQueryWrapper<User>()
                .and(!kw.isEmpty(), w->w.like(User::getUsername,kw).or().like(User::getName,kw).or().like(User::getPhone,kw))
                .orderByDesc(User::getCreateTime)));
    }
    @PostMapping public ApiResponse<Long> add(@RequestBody UserController.RegisterRequest r){
        String error=accounts.checkNew(r.username(),r.password(),r.name(),r.phone());
        return error!=null?ApiResponse.fail(error):ApiResponse.ok(accounts.create(r.username(),r.password(),r.name(),r.phone()).getId());
    }
    @PutMapping public ApiResponse<Void> update(@RequestBody UpdateRequest r){
        String error=accounts.update(r.id(),r.name(),r.phone(),r.password()); return error!=null?ApiResponse.fail(error):ApiResponse.ok();
    }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable("id") Long id){
        String error=accounts.delete(id); return error!=null?ApiResponse.fail(error):ApiResponse.ok();
    }
}

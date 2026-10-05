package com.sky.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.takeout.common.ApiResponse;
import com.sky.takeout.entity.Employee;
import com.sky.takeout.mapper.EmployeeMapper;
import com.sky.takeout.security.JwtService;
import com.sky.takeout.service.UserAccountService;
import lombok.Data;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/employee")
public class EmployeeController {
    private final EmployeeMapper mapper; private final PasswordEncoder encoder; private final JwtService jwtService;
    public EmployeeController(EmployeeMapper mapper, PasswordEncoder encoder, JwtService jwtService) { this.mapper=mapper; this.encoder=encoder; this.jwtService=jwtService; }
    @PostMapping("/login") public ApiResponse<?> login(@RequestBody LoginRequest request) {
        Employee employee=mapper.selectOne(new LambdaQueryWrapper<Employee>().eq(Employee::getUsername, request.username));
        if(employee==null || employee.getStatus()!=1 || !encoder.matches(request.password, employee.getPassword())) return ApiResponse.fail("Incorrect username or password");
        return ApiResponse.ok(Map.of("id", employee.getId(), "userName", employee.getUsername(), "name", employee.getName(), "token", jwtService.create(employee.getId(), employee.getUsername())));
    }
    @Data public static class LoginRequest { private String username; private String password; }
    public record CreateRequest(String username, String password, String name) {}
    public record UpdateRequest(Long id, String name, String password) {}

    @GetMapping("/page") public ApiResponse<List<Employee>> page() {
        return ApiResponse.ok(mapper.selectList(new LambdaQueryWrapper<Employee>().orderByDesc(Employee::getCreateTime)));
    }
    @PostMapping public ApiResponse<Long> add(@RequestBody CreateRequest r) {
        if(r.username()==null || !r.username().trim().matches("[A-Za-z0-9_.-]{3,32}")) return ApiResponse.fail("Username must be 3-32 characters: letters, digits, '_', '.' or '-'");
        String error=UserAccountService.checkPassword(r.password());
        if(error==null) error=UserAccountService.checkName(r.name());
        if(error!=null) return ApiResponse.fail(error);
        if(mapper.selectCount(new LambdaQueryWrapper<Employee>().eq(Employee::getUsername, r.username().trim()))>0) return ApiResponse.fail("Username already taken");
        Employee e=new Employee(); e.setUsername(r.username().trim()); e.setPassword(encoder.encode(r.password())); e.setName(r.name().trim()); e.setStatus(1);
        e.setCreateTime(LocalDateTime.now()); e.setUpdateTime(LocalDateTime.now()); mapper.insert(e); return ApiResponse.ok(e.getId());
    }
    @PutMapping public ApiResponse<Void> update(@RequestBody UpdateRequest r) {
        if(r.id()==null || mapper.selectById(r.id())==null) return ApiResponse.fail("Employee not found");
        boolean newPassword=r.password()!=null && !r.password().isBlank();
        String error=UserAccountService.checkName(r.name());
        if(error==null && newPassword) error=UserAccountService.checkPassword(r.password());
        if(error!=null) return ApiResponse.fail(error);
        Employee e=new Employee(); e.setId(r.id()); e.setName(r.name().trim()); if(newPassword) e.setPassword(encoder.encode(r.password()));
        e.setUpdateTime(LocalDateTime.now()); mapper.updateById(e); return ApiResponse.ok();
    }
    @PutMapping("/{id}/status/{status}") public ApiResponse<Void> status(@RequestAttribute("employeeId") Long me, @PathVariable("id") Long id, @PathVariable("status") Integer status) {
        if(status!=0 && status!=1) return ApiResponse.fail("Status must be 0 or 1");
        if(mapper.selectById(id)==null) return ApiResponse.fail("Employee not found");
        if(status==0 && id.equals(me)) return ApiResponse.fail("You cannot disable your own account");
        Employee e=new Employee(); e.setId(id); e.setStatus(status); e.setUpdateTime(LocalDateTime.now()); mapper.updateById(e); return ApiResponse.ok();
    }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@RequestAttribute("employeeId") Long me, @PathVariable("id") Long id) {
        if(mapper.selectById(id)==null) return ApiResponse.fail("Employee not found");
        if(id.equals(me)) return ApiResponse.fail("You cannot delete your own account");
        mapper.deleteById(id); return ApiResponse.ok();
    }
}

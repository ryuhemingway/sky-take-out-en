package com.sky.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.takeout.common.ApiResponse;
import com.sky.takeout.entity.Employee;
import com.sky.takeout.mapper.EmployeeMapper;
import com.sky.takeout.security.JwtService;
import lombok.Data;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
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
}

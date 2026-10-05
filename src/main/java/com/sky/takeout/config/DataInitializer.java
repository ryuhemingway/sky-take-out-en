package com.sky.takeout.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.takeout.entity.Employee;
import com.sky.takeout.mapper.EmployeeMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedAdmin(EmployeeMapper mapper, PasswordEncoder encoder) {
        return args -> {
            if (mapper.selectCount(new LambdaQueryWrapper<Employee>().eq(Employee::getUsername, "admin")) == 0) {
                Employee employee = new Employee();
                employee.setUsername("admin");
                employee.setPassword(encoder.encode("123456"));
                employee.setName("admin");
                employee.setStatus(1);
                employee.setCreateTime(LocalDateTime.now());
                employee.setUpdateTime(LocalDateTime.now());
                mapper.insert(employee);
            }
        };
    }
}

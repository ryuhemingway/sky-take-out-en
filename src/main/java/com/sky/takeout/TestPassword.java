package com.sky.takeout;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestPassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPwd = "123456";
        String encryptPwd = encoder.encode(rawPwd);
        System.out.println("Encrypted password: " + encryptPwd);
    }
}
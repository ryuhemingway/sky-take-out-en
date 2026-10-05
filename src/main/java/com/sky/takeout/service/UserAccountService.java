package com.sky.takeout.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sky.takeout.entity.Orders;
import com.sky.takeout.entity.ShoppingCart;
import com.sky.takeout.entity.User;
import com.sky.takeout.mapper.OrdersMapper;
import com.sky.takeout.mapper.ShoppingCartMapper;
import com.sky.takeout.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/** Customer account rules shared by the customer's own endpoints and the admin ones. Checks return an error message, or null when valid. */
@Service
public class UserAccountService {
    private final UserMapper users; private final OrdersMapper orders; private final ShoppingCartMapper carts; private final PasswordEncoder encoder;
    public UserAccountService(UserMapper users, OrdersMapper orders, ShoppingCartMapper carts, PasswordEncoder encoder) { this.users=users; this.orders=orders; this.carts=carts; this.encoder=encoder; }

    public static String checkName(String name) { return name == null || name.isBlank() || name.trim().length() > 32 ? "Name is required and must be at most 32 characters" : null; }
    public static String checkPassword(String password) { return password == null || password.length() < 6 ? "Password must be at least 6 characters" : null; }
    public static String checkProfile(String name, String phone) {
        if (checkName(name) != null) return checkName(name);
        return phone != null && phone.trim().length() > 20 ? "Phone number must be at most 20 characters" : null;
    }

    public String checkNew(String username, String password, String name, String phone) {
        if (username == null || !username.trim().matches("[A-Za-z0-9_.-]{3,64}")) return "Username must be 3-64 characters: letters, digits, '_', '.' or '-'";
        String error = checkPassword(password);
        if (error == null) error = checkProfile(name, phone);
        if (error != null) return error;
        return users.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username.trim())) > 0 ? "Username already taken" : null;
    }

    /** Call checkNew first. */
    public User create(String username, String password, String name, String phone) {
        User user = new User();
        user.setUsername(username.trim()); user.setPassword(encoder.encode(password)); user.setName(name.trim()); user.setPhone(blankToNull(phone));
        user.setCreateTime(LocalDateTime.now()); user.setUpdateTime(LocalDateTime.now());
        users.insert(user); return user;
    }

    /** Updates name and phone; a null or blank password leaves the current one unchanged. */
    public String update(Long id, String name, String phone, String password) {
        if (id == null || users.selectById(id) == null) return "User not found";
        boolean newPassword = password != null && !password.isBlank();
        String error = checkProfile(name, phone);
        if (error == null && newPassword) error = checkPassword(password);
        if (error != null) return error;
        LambdaUpdateWrapper<User> update = new LambdaUpdateWrapper<User>().eq(User::getId, id)
                .set(User::getName, name.trim()).set(User::getPhone, blankToNull(phone)).set(User::getUpdateTime, LocalDateTime.now());
        if (newPassword) update.set(User::getPassword, encoder.encode(password));
        users.update(null, update); return null;
    }

    public String changePassword(Long id, String oldPassword, String newPassword) {
        User user = users.selectById(id);
        if (user == null || oldPassword == null || !encoder.matches(oldPassword, user.getPassword())) return "Current password is incorrect";
        if (checkPassword(newPassword) != null) return "New password must be at least 6 characters";
        users.update(null, new LambdaUpdateWrapper<User>().eq(User::getId, id)
                .set(User::getPassword, encoder.encode(newPassword)).set(User::getUpdateTime, LocalDateTime.now()));
        return null;
    }

    /** shopping_cart has no foreign key, so its rows go explicitly; address_book and user_coupon cascade. */
    @Transactional
    public String delete(Long id) {
        if (users.selectById(id) == null) return "User not found";
        if (orders.selectCount(new LambdaQueryWrapper<Orders>().eq(Orders::getUserId, id)) > 0) return "Accounts with orders cannot be deleted";
        carts.delete(new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, id));
        users.deleteById(id); return null;
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}

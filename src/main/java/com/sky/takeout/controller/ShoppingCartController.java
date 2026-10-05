package com.sky.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.takeout.common.ApiResponse;
import com.sky.takeout.entity.Dish;
import com.sky.takeout.entity.ShoppingCart;
import com.sky.takeout.mapper.DishMapper;
import com.sky.takeout.mapper.ShoppingCartMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/user/shoppingCart")
public class ShoppingCartController {
    private final ShoppingCartMapper cartMapper;
    private final DishMapper dishMapper;

    public ShoppingCartController(ShoppingCartMapper cartMapper, DishMapper dishMapper) {
        this.cartMapper = cartMapper;
        this.dishMapper = dishMapper;
    }

    @GetMapping("/list")
    public ApiResponse<List<ShoppingCart>> list(@RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(cartMapper.selectList(new LambdaQueryWrapper<ShoppingCart>()
                .eq(ShoppingCart::getUserId, userId).orderByAsc(ShoppingCart::getCreateTime)));
    }

    @PostMapping("/add")
    @Transactional
    public ApiResponse<Void> add(@RequestAttribute("userId") Long userId,
                                 @RequestBody CartRequest request) {
        if (request.dishId == null || request.number == null || request.number < 1) return ApiResponse.fail("Dish and quantity must be valid");
        Dish dish = dishMapper.selectById(request.dishId);
        if (dish == null) return ApiResponse.fail("Dish not found");
        if (dish.getStatus() == 0) return ApiResponse.fail("Dish is disabled");
        ShoppingCart existing = cartMapper.selectOne(new LambdaQueryWrapper<ShoppingCart>()
                .eq(ShoppingCart::getUserId, userId).eq(ShoppingCart::getDishId, request.dishId));
        if (existing == null) {
            ShoppingCart cart = new ShoppingCart(); cart.setUserId(userId); cart.setDishId(dish.getId());
            cart.setName(dish.getName()); cart.setImage(dish.getImage()); cart.setAmount(dish.getPrice());
            cart.setNumber(request.number); cart.setCreateTime(LocalDateTime.now()); cartMapper.insert(cart);
        } else { existing.setNumber(existing.getNumber() + request.number); cartMapper.updateById(existing); }
        return ApiResponse.ok();
    }

    @PutMapping("/number")
    public ApiResponse<Void> number(@RequestAttribute("userId") Long userId,
                                    @RequestBody CartRequest request) {
        if (request.dishId == null || request.number == null || request.number < 1) return ApiResponse.fail("Dish and quantity must be valid");
        ShoppingCart cart = findDish(userId, request.dishId);
        if (cart == null) return ApiResponse.fail("Dish not found in cart");
        cart.setNumber(request.number); cartMapper.updateById(cart); return ApiResponse.ok();
    }

    @DeleteMapping("/clean")
    public ApiResponse<Void> clean(@RequestAttribute("userId") Long userId) {
        cartMapper.delete(new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, userId)); return ApiResponse.ok();
    }

    @DeleteMapping("/sub")
    public ApiResponse<Void> sub(@RequestAttribute("userId") Long userId,
                                 @RequestParam(name = "dishId") Long dishId) {
        ShoppingCart cart = findDish(userId, dishId); if (cart == null) return ApiResponse.fail("Dish not found in cart");
        cartMapper.deleteById(cart.getId()); return ApiResponse.ok();
    }

    private ShoppingCart findDish(Long userId, Long dishId) { return cartMapper.selectOne(new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId,userId).eq(ShoppingCart::getDishId,dishId)); }
    public static class CartRequest { public Long dishId; public Integer number; }
}

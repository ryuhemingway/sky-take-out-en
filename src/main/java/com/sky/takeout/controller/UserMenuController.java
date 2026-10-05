package com.sky.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.takeout.common.ApiResponse;
import com.sky.takeout.entity.Category;
import com.sky.takeout.entity.Dish;
import com.sky.takeout.entity.Setmeal;
import com.sky.takeout.mapper.CategoryMapper;
import com.sky.takeout.mapper.DishMapper;
import com.sky.takeout.mapper.SetmealMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/user/menu")
public class UserMenuController {
    private final CategoryMapper categoryMapper;
    private final DishMapper dishMapper;
    private final SetmealMapper setmealMapper;

    public UserMenuController(CategoryMapper categoryMapper, DishMapper dishMapper, SetmealMapper setmealMapper) {
        this.categoryMapper = categoryMapper;
        this.dishMapper = dishMapper;
        this.setmealMapper = setmealMapper;
    }

    @GetMapping("/categories")
    public ApiResponse<List<Category>> categories(@RequestParam(name = "type", required = false) String type) {
        return ApiResponse.ok(categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1)
                .eq(type != null && !type.isBlank(), Category::getType, type)
                .orderByAsc(Category::getSort)));
    }

    @GetMapping("/dishes")
    public ApiResponse<List<Dish>> dishes(@RequestParam(name = "categoryId", required = false) Long categoryId) {
        return ApiResponse.ok(dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStatus, 1)
                .eq(categoryId != null, Dish::getCategoryId, categoryId)
                .orderByDesc(Dish::getUpdateTime)));
    }

    @GetMapping("/setmeals")
    public ApiResponse<List<Setmeal>> setmeals(@RequestParam(name = "categoryId", required = false) Long categoryId) {
        return ApiResponse.ok(setmealMapper.selectList(new LambdaQueryWrapper<Setmeal>()
                .eq(Setmeal::getStatus, 1)
                .eq(categoryId != null, Setmeal::getCategoryId, categoryId)
                .orderByDesc(Setmeal::getUpdateTime)));
    }
}

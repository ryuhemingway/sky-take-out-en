package com.sky.takeout.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.takeout.entity.Category;
import com.sky.takeout.entity.Dish;
import com.sky.takeout.mapper.CategoryMapper;
import com.sky.takeout.mapper.DishMapper;
import com.sky.takeout.service.DishService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {
    private final DishMapper dishMapper;
    private final CategoryMapper categoryMapper;

    public DishServiceImpl(DishMapper dishMapper, CategoryMapper categoryMapper) {
        this.dishMapper = dishMapper;
        this.categoryMapper = categoryMapper;
    }

    public List<Dish> page(Long categoryId, String name, Integer status) {
        return dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(categoryId != null, Dish::getCategoryId, categoryId)
                .like(name != null && !name.isBlank(), Dish::getName, name)
                .eq(status != null, Dish::getStatus, status)
                .orderByDesc(Dish::getUpdateTime));
    }

    public Dish getById(Long id) { return dishMapper.selectById(id); }

    public String add(Dish dish) {
        String error = validateCategory(dish.getCategoryId());
        if (error != null) return error;
        if (existsName(dish.getName(), null)) return "Dish name already exists";
        dish.setStatus(1);
        dish.setCreateTime(LocalDateTime.now());
        dish.setUpdateTime(LocalDateTime.now());
        dishMapper.insert(dish);
        return null;
    }

    public String update(Dish dish) {
        if (dish.getId() == null || dishMapper.selectById(dish.getId()) == null) return "Dish not found";
        String error = validateCategory(dish.getCategoryId());
        if (error != null) return error;
        if (existsName(dish.getName(), dish.getId())) return "Dish name already exists";
        dish.setUpdateTime(LocalDateTime.now());
        dishMapper.updateById(dish);
        return null;
    }

    public String changeStatus(Long id, Integer status) {
        if (status != 0 && status != 1) return "Status must be 0 or 1";
        Dish dish = dishMapper.selectById(id);
        if (dish == null) return "Dish not found";
        dish.setStatus(status); dish.setUpdateTime(LocalDateTime.now()); dishMapper.updateById(dish);
        return null;
    }

    public String delete(Long id) {
        if (dishMapper.selectById(id) == null) return "Dish not found";
        dishMapper.deleteById(id); return null;
    }

    private String validateCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) return "Category not found";
        return category.getStatus() == 0 ? "Category is disabled and cannot be linked to dishes" : null;
    }

    private boolean existsName(String name, Long excludeId) {
        LambdaQueryWrapper<Dish> q = new LambdaQueryWrapper<Dish>().eq(Dish::getName, name);
        if (excludeId != null) q.ne(Dish::getId, excludeId);
        return dishMapper.selectCount(q) > 0;
    }
}

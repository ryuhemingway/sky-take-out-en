package com.sky.takeout.service;

import com.sky.takeout.entity.Dish;
import java.util.List;

public interface DishService {
    List<Dish> page(Long categoryId, String name, Integer status);
    Dish getById(Long id);
    String add(Dish dish);
    String update(Dish dish);
    String changeStatus(Long id, Integer status);
    String delete(Long id);
}

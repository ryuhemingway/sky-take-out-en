package com.sky.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.takeout.common.ApiResponse;
import com.sky.takeout.entity.Category;
import com.sky.takeout.entity.Setmeal;
import com.sky.takeout.entity.SetmealDish;
import com.sky.takeout.mapper.CategoryMapper;
import com.sky.takeout.mapper.DishMapper;
import com.sky.takeout.mapper.SetmealDishMapper;
import com.sky.takeout.mapper.SetmealMapper;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/setmeal")
public class SetmealController {
    private final SetmealMapper mapper;
    private final CategoryMapper categoryMapper;
    private final SetmealDishMapper dishMapper;
    private final DishMapper dishInfoMapper;

    public SetmealController(SetmealMapper mapper, CategoryMapper categoryMapper,
                             SetmealDishMapper dishMapper, DishMapper dishInfoMapper) {
        this.mapper = mapper; this.categoryMapper = categoryMapper;
        this.dishMapper = dishMapper; this.dishInfoMapper = dishInfoMapper;
    }

    @GetMapping("/page")
    public ApiResponse<List<Setmeal>> page(@RequestParam(name="categoryId", required=false) Long categoryId,
                                           @RequestParam(name="name", required=false) String name,
                                           @RequestParam(name="status", required=false) Integer status) {
        return ApiResponse.ok(mapper.selectList(new LambdaQueryWrapper<Setmeal>()
                .eq(categoryId != null, Setmeal::getCategoryId, categoryId)
                .like(name != null && !name.isBlank(), Setmeal::getName, name)
                .eq(status != null, Setmeal::getStatus, status)
                .orderByDesc(Setmeal::getUpdateTime)));
    }

    @GetMapping("/{id}")
    public ApiResponse<Setmeal> get(@PathVariable("id") Long id) {
        Setmeal setmeal = mapper.selectById(id);
        return setmeal == null ? ApiResponse.fail("Set meal not found") : ApiResponse.ok(setmeal);
    }

    @GetMapping("/{id}/dishes")
    public ApiResponse<List<SetmealDish>> dishes(@PathVariable("id") Long id) {
        if (mapper.selectById(id) == null) return ApiResponse.fail("Set meal not found");
        return ApiResponse.ok(dishMapper.selectList(new LambdaQueryWrapper<SetmealDish>()
                .eq(SetmealDish::getSetmealId, id)));
    }

    @PostMapping
    @Transactional
    public ApiResponse<Long> add(@Valid @RequestBody SetmealRequest request) {
        Setmeal setmeal = request.toSetmeal();
        String error = validate(setmeal, null);
        if (error != null) return ApiResponse.fail(error);
        error = validateDishes(request.dishes);
        if (error != null) return ApiResponse.fail(error);
        setmeal.setStatus(1); setmeal.setCreateTime(LocalDateTime.now()); setmeal.setUpdateTime(LocalDateTime.now());
        mapper.insert(setmeal); saveDishes(setmeal.getId(), request.dishes); return ApiResponse.ok(setmeal.getId());
    }

    @PutMapping
    @Transactional
    public ApiResponse<Void> update(@Valid @RequestBody SetmealRequest request) {
        Setmeal setmeal = request.toSetmeal();
        if (setmeal.getId() == null || mapper.selectById(setmeal.getId()) == null) return ApiResponse.fail("Set meal not found");
        String error = validate(setmeal, setmeal.getId());
        if (error != null) return ApiResponse.fail(error);
        error = validateDishes(request.dishes);
        if (error != null) return ApiResponse.fail(error);
        setmeal.setUpdateTime(LocalDateTime.now()); mapper.updateById(setmeal);
        dishMapper.delete(new LambdaQueryWrapper<SetmealDish>().eq(SetmealDish::getSetmealId, setmeal.getId()));
        saveDishes(setmeal.getId(), request.dishes); return ApiResponse.ok();
    }

    @PatchMapping("/{id}/status/{status}")
    public ApiResponse<Void> status(@PathVariable("id") Long id, @PathVariable("status") Integer status) {
        if (status != 0 && status != 1) return ApiResponse.fail("Status must be 0 or 1");
        Setmeal setmeal = mapper.selectById(id); if (setmeal == null) return ApiResponse.fail("Set meal not found");
        setmeal.setStatus(status); setmeal.setUpdateTime(LocalDateTime.now()); mapper.updateById(setmeal); return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        if (mapper.selectById(id) == null) return ApiResponse.fail("Set meal not found");
        mapper.deleteById(id); return ApiResponse.ok();
    }

    private String validate(Setmeal setmeal, Long excludeId) {
        Category category = categoryMapper.selectById(setmeal.getCategoryId());
        if (category == null) return "Category not found";
        if (category.getStatus() == 0) return "Category is disabled";
        LambdaQueryWrapper<Setmeal> query = new LambdaQueryWrapper<Setmeal>().eq(Setmeal::getName, setmeal.getName());
        if (excludeId != null) query.ne(Setmeal::getId, excludeId);
        return mapper.selectCount(query) > 0 ? "Set meal name already exists" : null;
    }

    private String validateDishes(List<SetmealDish> dishes) {
        if (dishes == null || dishes.isEmpty()) return "A set meal must contain at least one dish";
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (SetmealDish item : dishes) {
            if (item.getDishId() == null || item.getCopies() == null || item.getCopies() < 1) return "Dish ID and quantity must be valid";
            if (!ids.add(item.getDishId())) return "The same dish cannot be added twice to a set meal";
            var dish = dishInfoMapper.selectById(item.getDishId());
            if (dish == null) return "Dish not found";
            if (dish.getStatus() == 0) return "Dish is disabled";
        }
        return null;
    }

    private void saveDishes(Long setmealId, List<SetmealDish> dishes) {
        for (SetmealDish item : dishes) { item.setId(null); item.setSetmealId(setmealId); dishMapper.insert(item); }
    }

    public static class SetmealRequest {
        public Long id;
        @jakarta.validation.constraints.NotBlank(message="Set meal name is required") public String name;
        @jakarta.validation.constraints.NotNull(message="Category is required") public Long categoryId;
        @jakarta.validation.constraints.NotNull(message="Price is required")
        @jakarta.validation.constraints.DecimalMin(value="0.01", message="Price must be greater than 0") public java.math.BigDecimal price;
        public String image; public String description; public Integer status; public List<SetmealDish> dishes;
        Setmeal toSetmeal() { Setmeal s=new Setmeal(); s.setId(id); s.setName(name); s.setCategoryId(categoryId); s.setPrice(price); s.setImage(image); s.setDescription(description); s.setStatus(status); return s; }
    }
}

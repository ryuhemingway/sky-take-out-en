package com.sky.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.takeout.common.ApiResponse;
import com.sky.takeout.entity.Category;
import com.sky.takeout.mapper.CategoryMapper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/category")
public class CategoryController {
    private final CategoryMapper mapper;
    public CategoryController(CategoryMapper mapper) { this.mapper = mapper; }
    @GetMapping("/page") public ApiResponse<List<Category>> list(@RequestParam(required=false) String type) {
        return ApiResponse.ok(mapper.selectList(new LambdaQueryWrapper<Category>().eq(type != null && !type.isBlank(), Category::getType, type).orderByAsc(Category::getSort)));
    }
    @PostMapping
    public ApiResponse<Void> add(@Valid @RequestBody Category category) {
        if (existsName(category.getName(), category.getType(), null)) return ApiResponse.fail("Category name already exists");
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        category.setStatus(1);
        mapper.insert(category);
        return ApiResponse.ok();
    }

    @PutMapping
    public ApiResponse<Void> update(@Valid @RequestBody Category category) {
        if (category.getId() == null || mapper.selectById(category.getId()) == null) return ApiResponse.fail("Category not found");
        if (existsName(category.getName(), category.getType(), category.getId())) return ApiResponse.fail("Category name already exists");
        category.setUpdateTime(LocalDateTime.now());
        mapper.updateById(category);
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/status/{status}")
    public ApiResponse<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        if (status != 0 && status != 1) return ApiResponse.fail("Status must be 0 or 1");
        Category category = mapper.selectById(id);
        if (category == null) return ApiResponse.fail("Category not found");
        category.setStatus(status);
        category.setUpdateTime(LocalDateTime.now());
        mapper.updateById(category);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        if (mapper.selectById(id) == null) return ApiResponse.fail("Category not found");
        mapper.deleteById(id);
        return ApiResponse.ok();
    }

    private boolean existsName(String name, String type, Long excludeId) {
        LambdaQueryWrapper<Category> query = new LambdaQueryWrapper<Category>()
                .eq(Category::getName, name).eq(Category::getType, type);
        if (excludeId != null) query.ne(Category::getId, excludeId);
        return mapper.selectCount(query) > 0;
    }
}

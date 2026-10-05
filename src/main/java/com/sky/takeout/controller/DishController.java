package com.sky.takeout.controller;
import com.sky.takeout.common.ApiResponse;
import com.sky.takeout.entity.Dish;
import com.sky.takeout.service.DishService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/admin/dish")
public class DishController {
    private final DishService service;
    public DishController(DishService service) { this.service = service; }
    @GetMapping("/page")
    public ApiResponse<List<Dish>> page(@RequestParam(name="categoryId", required=false) Long categoryId, @RequestParam(name="name", required=false) String name, @RequestParam(name="status", required=false) Integer status) { return ApiResponse.ok(service.page(categoryId, name, status)); }
    @GetMapping("/{id}")
    public ApiResponse<Dish> getById(@PathVariable("id") Long id) { Dish dish = service.getById(id); return dish == null ? ApiResponse.fail("Dish not found") : ApiResponse.ok(dish); }
    @PostMapping public ApiResponse<Long> add(@Valid @RequestBody Dish dish) { String e=service.add(dish); return e==null?ApiResponse.ok(dish.getId()):ApiResponse.fail(e); }
    @PutMapping public ApiResponse<Void> update(@Valid @RequestBody Dish dish) { String e=service.update(dish); return e==null?ApiResponse.ok():ApiResponse.fail(e); }
    @PatchMapping("/{id}/status/{status}") public ApiResponse<Void> changeStatus(@PathVariable("id") Long id,@PathVariable("status") Integer status) { String e=service.changeStatus(id,status); return e==null?ApiResponse.ok():ApiResponse.fail(e); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable("id") Long id) { String e=service.delete(id); return e==null?ApiResponse.ok():ApiResponse.fail(e); }
}

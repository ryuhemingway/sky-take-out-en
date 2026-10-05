package com.sky.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.takeout.common.ApiResponse;
import com.sky.takeout.entity.AddressBook;
import com.sky.takeout.mapper.AddressBookMapper;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/user/addressBook")
public class AddressBookController {
    private final AddressBookMapper mapper;

    public AddressBookController(AddressBookMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping("/list")
    public ApiResponse<List<AddressBook>> list(@RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(mapper.selectList(new LambdaQueryWrapper<AddressBook>()
                .eq(AddressBook::getUserId, userId)
                .orderByDesc(AddressBook::getIsDefault)
                .orderByDesc(AddressBook::getUpdateTime)));
    }

    @GetMapping("/{id}")
    public ApiResponse<AddressBook> get(@RequestAttribute("userId") Long userId,
                                        @PathVariable("id") Long id) {
        AddressBook address = findOwned(userId, id);
        return address == null ? ApiResponse.fail("Address not found") : ApiResponse.ok(address);
    }

    @GetMapping("/default")
    public ApiResponse<AddressBook> getDefault(@RequestAttribute("userId") Long userId) {
        AddressBook address = mapper.selectOne(new LambdaQueryWrapper<AddressBook>()
                .eq(AddressBook::getUserId, userId)
                .eq(AddressBook::getIsDefault, 1));
        return address == null ? ApiResponse.fail("No default address set") : ApiResponse.ok(address);
    }

    @PostMapping
    public ApiResponse<Long> add(@RequestAttribute("userId") Long userId,
                                 @Valid @RequestBody AddressBook address) {
        address.setId(null);
        address.setUserId(userId);
        address.setIsDefault(0);
        address.setCreateTime(LocalDateTime.now());
        address.setUpdateTime(LocalDateTime.now());
        mapper.insert(address);
        return ApiResponse.ok(address.getId());
    }

    @PutMapping
    public ApiResponse<Void> update(@RequestAttribute("userId") Long userId,
                                    @Valid @RequestBody AddressBook address) {
        if (address.getId() == null || findOwned(userId, address.getId()) == null) {
            return ApiResponse.fail("Address not found");
        }
        address.setUserId(userId);
        address.setIsDefault(null);
        address.setCreateTime(null);
        address.setUpdateTime(LocalDateTime.now());
        mapper.updateById(address);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/default")
    @Transactional
    public ApiResponse<Void> setDefault(@RequestAttribute("userId") Long userId,
                                        @PathVariable("id") Long id) {
        AddressBook address = findOwned(userId, id);
        if (address == null) return ApiResponse.fail("Address not found");
        AddressBook clear = new AddressBook();
        clear.setIsDefault(0);
        mapper.update(clear, new LambdaQueryWrapper<AddressBook>().eq(AddressBook::getUserId, userId));
        address.setIsDefault(1);
        address.setUpdateTime(LocalDateTime.now());
        mapper.updateById(address);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@RequestAttribute("userId") Long userId,
                                    @PathVariable("id") Long id) {
        AddressBook address = findOwned(userId, id);
        if (address == null) return ApiResponse.fail("Address not found");
        mapper.deleteById(id);
        return ApiResponse.ok();
    }

    private AddressBook findOwned(Long userId, Long id) {
        return mapper.selectOne(new LambdaQueryWrapper<AddressBook>()
                .eq(AddressBook::getId, id)
                .eq(AddressBook::getUserId, userId));
    }
}

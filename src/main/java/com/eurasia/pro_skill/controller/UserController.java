package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.domain.User;
import com.eurasia.pro_skill.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户管理", description = "用户相关接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取所有用户", description = "返回系统内全部用户列表（不含密码），仅 ADMIN 可访问")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<User> listAll() {
        return userService.listAll();
    }
}

package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.common.PageResponse;
import com.eurasia.pro_skill.controller.request.CreateUserRequest;
import com.eurasia.pro_skill.controller.response.CreateUserResponse;
import com.eurasia.pro_skill.domain.User;
import com.eurasia.pro_skill.application.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户管理", description = "用户相关接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页获取用户列表", description = "返回分页用户列表（不含密码），仅 ADMIN 可访问")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public PageResponse<User> listPage(
            @Parameter(description = "页码，1-based") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize
    ) {
        return userService.listPage(current, pageSize);
    }

    @Operation(summary = "创建用户", description = "创建新用户，可指定角色，仅 ADMIN 可访问")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public CreateUserResponse createUser(@RequestBody @Valid CreateUserRequest request) {
        Long userId = userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getDisplayName(),
                request.getRole(),
                request.getEmail(),
                request.getPhone()
        );
        return new CreateUserResponse(userId, request.getUsername(), request.getRole(), "创建成功");
    }
}

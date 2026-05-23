package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.controller.request.CreateUserRequest;
import com.eurasia.pro_skill.controller.request.UpdateUserRequest;
import com.eurasia.pro_skill.controller.response.CreateUserResponse;
import com.eurasia.pro_skill.controller.response.DeleteUserResponse;
import com.eurasia.pro_skill.controller.response.UpdateUserResponse;
import com.eurasia.pro_skill.controller.response.UserDetailResponse;
import com.eurasia.pro_skill.controller.response.UserPageResponse;
import com.eurasia.pro_skill.controller.response.UserResponseMapper;
import com.eurasia.pro_skill.domain.User;
import com.eurasia.pro_skill.application.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口层（Controller）。
 *
 * <p>约定：对外 JSON 统一使用 {@code controller.response} 包下的 *Response，
 * Service 仍返回 Domain；在本类中通过 {@link UserResponseMapper} 转换。
 *
 * <p>教学文档：{@code docs/用户管理CRUD全流程.md}
 */
@Tag(name = "用户管理", description = "用户相关接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页获取用户列表", description = "返回分页用户列表（不含密码），仅 ADMIN 可访问")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public UserPageResponse listPage(
            @Parameter(description = "页码，1-based") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "用户名模糊搜索") @RequestParam(required = false) String username,
            @Parameter(description = "角色") @RequestParam(required = false) String role,
            @Parameter(description = "状态：1-启用 0-禁用") @RequestParam(required = false) Integer status
    ) {
        return UserResponseMapper.fromPage(
                userService.listPage(current, pageSize, username, role, status)
        );
    }

    @Operation(summary = "获取用户详情", description = "按 ID 查询用户（不含密码），仅 ADMIN 可访问")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public UserDetailResponse getById(@Parameter(description = "用户 ID") @PathVariable Long id) {
        User user = userService.getById(id);
        return new UserDetailResponse(UserResponseMapper.from(user), "查询成功");
    }

    @Operation(summary = "创建用户", description = "创建新用户，可指定角色，仅 ADMIN 可访问")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public CreateUserResponse createUser(@RequestBody @Valid CreateUserRequest request) {
        User user = userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getDisplayName(),
                request.getRole(),
                request.getEmail(),
                request.getPhone()
        );
        return new CreateUserResponse(user.getId(), UserResponseMapper.from(user), "创建成功");
    }

    @Operation(summary = "更新用户", description = "更新显示名、角色、联系方式、状态；password 可选（不传则不修改），仅 ADMIN 可访问")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public UpdateUserResponse updateUser(
            @Parameter(description = "用户 ID") @PathVariable Long id,
            @RequestBody @Valid UpdateUserRequest request
    ) {
        User user = userService.update(
                id,
                request.getDisplayName(),
                request.getRole(),
                request.getEmail(),
                request.getPhone(),
                request.getStatus(),
                request.getPassword()
        );
        return new UpdateUserResponse(UserResponseMapper.from(user), "更新成功");
    }

    @Operation(summary = "禁用用户", description = "将用户状态设为禁用（软删除），仅 ADMIN 可访问")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public DeleteUserResponse deleteUser(@Parameter(description = "用户 ID") @PathVariable Long id) {
        userService.delete(id);
        return new DeleteUserResponse(id, "禁用成功");
    }
}

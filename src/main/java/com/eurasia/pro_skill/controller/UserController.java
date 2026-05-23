package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.common.PageResponse;
import com.eurasia.pro_skill.controller.request.CreateUserRequest;
import com.eurasia.pro_skill.controller.request.UpdateUserRequest;
import com.eurasia.pro_skill.controller.response.CreateUserResponse;
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
 * 用户管理接口层（Controller / MVCS 中的 C）。
 *
 * <p>职责（只做这些）：
 * <ul>
 *   <li>定义 URL 路径与 HTTP 方法（GET/POST/PUT/DELETE）</li>
 *   <li>接收请求参数（Query、Path、JSON Body）</li>
 *   <li>调用 {@link UserService}，把结果原样或包装后返回给前端</li>
 * </ul>
 *
 * <p>不负责：写 SQL、加密密码、判断用户名是否重复（这些在 Service / Mapper）。
 *
 * <p>教学文档：{@code docs/用户管理CRUD全流程.md}
 */
@Tag(name = "用户管理", description = "用户相关接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    /** 注入业务层；由 Spring 自动创建 UserService 实例 */
    private final UserService userService;

    /**
     * 【R-Read 列表】分页查询用户。
     *
     * <p>示例：{@code GET /users?current=1&pageSize=10&username=admin&status=1}
     */
    @Operation(summary = "分页获取用户列表", description = "返回分页用户列表（不含密码），仅 ADMIN 可访问")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public PageResponse<User> listPage(
            @Parameter(description = "页码，1-based") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "用户名模糊搜索") @RequestParam(required = false) String username,
            @Parameter(description = "角色") @RequestParam(required = false) String role,
            @Parameter(description = "状态：1-启用 0-禁用") @RequestParam(required = false) Integer status
    ) {
        return userService.listPage(current, pageSize, username, role, status);
    }

    /**
     * 【R-Read 详情】按主键 id 查单个用户。
     *
     * <p>示例：{@code GET /users/3}，其中 3 会绑定到参数 {@code id}。
     */
    @Operation(summary = "获取用户详情", description = "按 ID 查询用户（不含密码），仅 ADMIN 可访问")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public User getById(@Parameter(description = "用户 ID") @PathVariable Long id) {
        return userService.getById(id);
    }

    /**
     * 【C-Create 创建】管理员新建用户。
     *
     * <p>{@code @RequestBody}：把 JSON 请求体转成 {@link CreateUserRequest} 对象。
     * <p>{@code @Valid}：触发 Request 类上的校验注解（如 @NotBlank），失败返回 400。
     */
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

    /**
     * 【U-Update 更新】按 id 修改用户信息。
     *
     * <p>URL 中的 id 与 Body 分离：id 表示改谁，Body 表示改成什么。
     */
    @Operation(summary = "更新用户", description = "更新显示名、角色、联系方式、状态；password 可选（不传则不修改），仅 ADMIN 可访问")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public User updateUser(
            @Parameter(description = "用户 ID") @PathVariable Long id,
            @RequestBody @Valid UpdateUserRequest request
    ) {
        return userService.update(
                id,
                request.getDisplayName(),
                request.getRole(),
                request.getEmail(),
                request.getPhone(),
                request.getStatus(),
                request.getPassword()
        );
    }

    /**
     * 【D-Delete 禁用】软删除：不删表行，只把 status 设为 0。
     *
     * <p>返回 void 时，HTTP 状态码一般为 200，响应体为空。
     */
    @Operation(summary = "禁用用户", description = "将用户状态设为禁用（软删除），仅 ADMIN 可访问")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteUser(@Parameter(description = "用户 ID") @PathVariable Long id) {
        userService.delete(id);
    }
}

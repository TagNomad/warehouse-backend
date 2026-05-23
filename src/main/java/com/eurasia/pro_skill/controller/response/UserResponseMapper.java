package com.eurasia.pro_skill.controller.response;

import com.eurasia.pro_skill.common.PageResponse;
import com.eurasia.pro_skill.domain.User;

import java.util.List;

/**
 * Domain {@link User} → Controller 层 Response 的转换（仅做字段拷贝，无业务逻辑）。
 */
public final class UserResponseMapper {

    private UserResponseMapper() {
    }

    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setDisplayName(user.getDisplayName());
        response.setRole(user.getRole());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    public static List<UserResponse> fromList(List<User> users) {
        return users.stream().map(UserResponseMapper::from).toList();
    }

    public static UserPageResponse fromPage(PageResponse<User> page) {
        return new UserPageResponse(
                page.getCurrent(),
                page.getPageSize(),
                page.getTotal(),
                fromList(page.getRecords())
        );
    }
}

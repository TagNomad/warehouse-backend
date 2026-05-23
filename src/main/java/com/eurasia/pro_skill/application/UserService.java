package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.common.PageResponse;
import com.eurasia.pro_skill.converter.UserConverter;
import com.eurasia.pro_skill.domain.User;
import com.eurasia.pro_skill.entity.UserEntity;
import com.eurasia.pro_skill.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户业务层（Application Service）。
 *
 * <p>职责：
 * <ul>
 *   <li>实现业务规则：查重、密码加密、分页计算、软删除</li>
 *   <li>组装 {@link UserEntity}，调用 {@link UserMapper} 读写数据库</li>
 *   <li>查询结果转为 {@link User}（Domain），避免 password 泄露到 Controller</li>
 * </ul>
 *
 * <p>{@code @Transactional}：一个 public 方法默认在一个数据库事务里执行，
 * 方法正常结束则提交，抛异常则回滚。
 *
 * <p>教学文档：{@code docs/用户管理CRUD全流程.md}
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    /** 用户状态：启用（可登录） */
    private static final int STATUS_ENABLED = 1;
    /** 用户状态：禁用（软删除后） */
    private static final int STATUS_DISABLED = 0;

    private final UserMapper userMapper;
    private final UserConverter userConverter;
    /** Spring Security 提供的 BCrypt 加密器，用于把明文密码变成哈希再存库 */
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页列表（Read）。
     *
     * <p>{@code readOnly = true}：只读事务，提示数据库可做读优化。
     */
    @Transactional(readOnly = true)
    public PageResponse<User> listPage(int current, int pageSize, String username, String role, Integer status) {
        // 防止非法页码：最小第 1 页
        int safeCurrent = Math.max(current, 1);
        // 每页条数限制在 1～100，防止一次查太多拖慢数据库
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        // 分页偏移量：第 1 页 offset=0，第 2 页 offset=pageSize，以此类推
        int offset = (safeCurrent - 1) * safePageSize;

        long total = userMapper.count(username, role, status);
        List<User> records = userMapper.findPage(safePageSize, offset, username, role, status)
                .stream()
                .map(userConverter::toDomain) // Entity → Domain，去掉 password
                .toList();
        return PageResponse.of(safeCurrent, safePageSize, total, records);
    }

    /**
     * 按 id 查详情（Read）。
     */
    @Transactional(readOnly = true)
    public User getById(Long id) {
        UserEntity entity = userMapper.findById(id);
        if (entity == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在：" + id);
        }
        return userConverter.toDomain(entity);
    }

    /**
     * 创建用户（Create）。
     *
     * <p>关键步骤：查重 → 组装 Entity → 加密密码 → insert → 返回 Domain。
     * <p>注意：必须调用 {@code userMapper.insert}，否则 Swagger 仍可能返回「创建成功」但库里无数据。
     */
    public User createUser(String username, String password, String displayName, String role,
                           String email, String phone) {
        // 1. 业务规则：用户名唯一
        if (userMapper.findByUsername(username) != null) {
            throw new BusinessException("USER_USERNAME_EXISTS", "用户名已存在：" + username);
        }

        // 2. 组装持久化对象（对应 tb_user 表的一行）
        UserEntity newUser = new UserEntity();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password)); // 永不存明文密码
        newUser.setDisplayName(displayName != null ? displayName : username);
        newUser.setRole(role);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setStatus(STATUS_ENABLED);
        LocalDateTime now = LocalDateTime.now();
        newUser.setCreatedAt(now);
        newUser.setUpdatedAt(now);

        // 3. 执行 INSERT（SQL 在 UserMapper 接口上）
        userMapper.insert(newUser);
        // insert 成功后，MyBatis 会把数据库生成的 id 写回 newUser.getId()
        return userConverter.toDomain(newUser);
    }

    /**
     * 更新用户（Update）。
     *
     * <p>password 为 null 或空字符串时不修改密码。
     */
    public User update(Long id, String displayName, String role, String email, String phone,
                       Integer status, String password) {
        UserEntity entity = userMapper.findById(id);
        if (entity == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在：" + id);
        }

        entity.setDisplayName(displayName);
        entity.setRole(role);
        entity.setEmail(email);
        entity.setPhone(phone);
        if (status != null) {
            entity.setStatus(status);
        }
        if (StringUtils.hasText(password)) {
            entity.setPassword(passwordEncoder.encode(password));
        }
        entity.setUpdatedAt(LocalDateTime.now());

        userMapper.update(entity);
        return userConverter.toDomain(entity);
    }

    /**
     * 禁用用户（Delete — 软删除）。
     *
     * <p>不执行 DELETE SQL，只把 status 改为 0，所以数据库里仍能看到该行。
     */
    public void delete(Long id) {
        UserEntity entity = userMapper.findById(id);
        if (entity == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在：" + id);
        }
        entity.setStatus(STATUS_DISABLED);
        entity.setUpdatedAt(LocalDateTime.now());
        userMapper.update(entity);
    }
}

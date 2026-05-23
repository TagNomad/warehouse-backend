package com.eurasia.pro_skill.converter;

import com.eurasia.pro_skill.domain.User;
import com.eurasia.pro_skill.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * UserEntity ↔ User 转换器（MapStruct 在编译期自动生成实现类）。
 *
 * <p>为什么需要它？
 * <ul>
 *   <li>查库得到的是 Entity（含 password）</li>
 *   <li>返回 API 要用 Domain（无 password）</li>
 *   <li>手写 get/set 易漏字段，MapStruct 按同名属性自动拷贝</li>
 * </ul>
 *
 * <p>{@code componentModel = SPRING}：生成的类带 {@code @Component}，可被注入到 UserService。
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserConverter {

    /**
     * 数据库对象 → 业务对象。
     * Domain 无 password 字段，MapStruct 会自动忽略 Entity.password。
     */
    User toDomain(UserEntity entity);

    /**
     * 业务对象 → 数据库对象（如需从 Domain 写库时使用）。
     * password 必须 ignore，由 Service 单独 {@code passwordEncoder.encode} 后 set。
     */
    @Mapping(target = "password", ignore = true)
    UserEntity toEntity(User domain);
}

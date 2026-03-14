package com.eurasia.pro_skill.converter;

import com.eurasia.pro_skill.domain.User;
import com.eurasia.pro_skill.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * UserEntity (PO) ↔ User (Domain) 转换器
 *
 * <p>MapStruct 在编译期自动生成实现类，字段同名则直接映射。
 * Domain 无 password 字段，MapStruct 自动忽略，无需 @Mapping 显式声明。
 *
 * <p>componentModel = SPRING：生成的实现类带 @Component，可直接 @RequiredArgsConstructor 注入。
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserConverter {

    /** Entity → Domain（password 字段因 Domain 无对应字段而自动忽略） */
    User toDomain(UserEntity entity);

    /** Domain → Entity（写入 DB 前转换，password 需调用方单独设置） */
    @Mapping(target = "password", ignore = true)
    UserEntity toEntity(User domain);
}

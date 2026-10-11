package com.eurasia.pro_skill.converter;

import com.eurasia.pro_skill.domain.Sku;
import com.eurasia.pro_skill.entity.SkuEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

// 插入成功后，把表的一行变回业务对象，供接口返回。字段名相同的会自动拷贝。
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING) // 生成的实现类交给 Spring 注入
public interface SkuConverter {

    Sku toDomain(SkuEntity entity); // SkuEntity → Sku，包含数据库写回的 id
}

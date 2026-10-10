package com.eurasia.pro_skill.converter;

import com.eurasia.pro_skill.domain.Sku;
import com.eurasia.pro_skill.entity.SkuEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SkuConverter {

    Sku toDomain(SkuEntity entity);
}

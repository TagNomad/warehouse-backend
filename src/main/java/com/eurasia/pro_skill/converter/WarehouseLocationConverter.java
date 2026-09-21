package com.eurasia.pro_skill.converter;

import com.eurasia.pro_skill.domain.WarehouseLocation;
import com.eurasia.pro_skill.entity.WarehouseLocationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WarehouseLocationConverter {

    WarehouseLocation toDomain(WarehouseLocationEntity entity);
}

package com.eurasia.pro_skill.converter;

import com.eurasia.pro_skill.domain.Warehouse;
import com.eurasia.pro_skill.entity.WarehouseEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WarehouseConverter {

    Warehouse toDomain(WarehouseEntity entity);
    WarehouseEntity toEntity(Warehouse domain);
}

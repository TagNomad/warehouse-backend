package com.eurasia.pro_skill.converter;

import com.eurasia.pro_skill.domain.WarehouseZone;
import com.eurasia.pro_skill.entity.WarehouseZoneEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WarehouseZoneConverter {

    WarehouseZone toDomain(WarehouseZoneEntity entity);
}

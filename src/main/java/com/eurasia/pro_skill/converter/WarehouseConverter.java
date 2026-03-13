package com.eurasia.pro_skill.converter;

import com.eurasia.pro_skill.domain.Warehouse;
import com.eurasia.pro_skill.entity.WarehouseEntity;
import com.eurasia.pro_skill.controller.request.WarehouseCreateRequest;
import com.eurasia.pro_skill.controller.request.WarehouseUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface WarehouseConverter {

    // Entity ↔ Domain 转换
    Warehouse toDomain(WarehouseEntity entity);
    WarehouseEntity toEntity(Warehouse domain);

    // Request → Domain 转换
    Warehouse createRequestToDomain(WarehouseCreateRequest request);
    void updateRequestToDomain(WarehouseUpdateRequest request, @MappingTarget Warehouse domain);
}

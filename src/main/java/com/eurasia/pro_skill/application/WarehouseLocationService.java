package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.common.PageResponse;
import com.eurasia.pro_skill.converter.WarehouseConverter;
import com.eurasia.pro_skill.converter.WarehouseLocationConverter;
import com.eurasia.pro_skill.converter.WarehouseZoneConverter;
import com.eurasia.pro_skill.domain.Warehouse;
import com.eurasia.pro_skill.domain.WarehouseLocation;
import com.eurasia.pro_skill.domain.WarehouseLocationDetail;
import com.eurasia.pro_skill.domain.WarehouseZone;
import com.eurasia.pro_skill.entity.WarehouseEntity;
import com.eurasia.pro_skill.entity.WarehouseLocationEntity;
import com.eurasia.pro_skill.entity.WarehouseZoneEntity;
import com.eurasia.pro_skill.mapper.WarehouseLocationMapper;
import com.eurasia.pro_skill.mapper.WarehouseMapper;
import com.eurasia.pro_skill.mapper.WarehouseZoneMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseLocationService {

    private final WarehouseMapper warehouseMapper;
    private final WarehouseZoneMapper warehouseZoneMapper;
    private final WarehouseLocationMapper warehouseLocationMapper;
    private final WarehouseLocationConverter warehouseLocationConverter;
    private final WarehouseZoneConverter warehouseZoneConverter;
    private final WarehouseConverter warehouseConverter;

    public WarehouseLocation create(Long zoneId, WarehouseLocation input) {
        requireZoneExists(zoneId);

        WarehouseLocationEntity existing = warehouseLocationMapper.getByZoneIdAndCode(zoneId, input.getCode());
        if (existing != null) {
            throw new BusinessException("LOCATION_CODE_EXISTS", "库位编码已存在：" + input.getCode());
        }

        WarehouseLocationEntity entity = new WarehouseLocationEntity();
        entity.setZoneId(zoneId);
        entity.setCode(input.getCode());
        entity.setName(input.getName());
        entity.setSpec(input.getSpec());
        entity.setStatus(WarehouseLocation.STATUS_AVAILABLE);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        warehouseLocationMapper.insert(entity);
        return warehouseLocationConverter.toDomain(entity);
    }

    public WarehouseLocation update(Long id, WarehouseLocation changes) {
        WarehouseLocationEntity entity = requireLocationExists(id);

        if (WarehouseLocation.STATUS_OCCUPIED.equals(entity.getStatus())
                && changes.getStatus() != null
                && !WarehouseLocation.STATUS_OCCUPIED.equals(changes.getStatus())) {
            throw new BusinessException("LOCATION_OCCUPIED", "库位已被占用，无法修改为非占用状态：" + id);
        }

        if (changes.getName() != null) {
            entity.setName(changes.getName());
        }
        if (changes.getSpec() != null) {
            entity.setSpec(changes.getSpec());
        }
        if (changes.getStatus() != null) {
            entity.setStatus(changes.getStatus());
        }
        entity.setUpdatedAt(LocalDateTime.now());

        warehouseLocationMapper.update(entity);
        return warehouseLocationConverter.toDomain(entity);
    }

    public void delete(Long id) {
        WarehouseLocationEntity entity = requireLocationExists(id);
        if (WarehouseLocation.STATUS_OCCUPIED.equals(entity.getStatus())) {
            throw new BusinessException("LOCATION_OCCUPIED", "库位已被占用，无法删除：" + id);
        }
        warehouseLocationMapper.delete(id);
    }

    public WarehouseLocation getById(Long id) {
        return warehouseLocationConverter.toDomain(requireLocationExists(id));
    }

    @Transactional(readOnly = true)
    public WarehouseLocationDetail getDetail(Long id) {
        WarehouseLocationEntity locationEntity = requireLocationExists(id);
        WarehouseZoneEntity zoneEntity = requireZoneExists(locationEntity.getZoneId());
        WarehouseEntity warehouseEntity = requireWarehouseExists(zoneEntity.getWarehouseId());

        return WarehouseLocationDetail.builder()
                .location(warehouseLocationConverter.toDomain(locationEntity))
                .zone(warehouseZoneConverter.toDomain(zoneEntity))
                .warehouse(warehouseConverter.toDomain(warehouseEntity))
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<WarehouseLocation> listPage(Integer current, Integer pageSize,
                                                    Long warehouseId, Long zoneId,
                                                    String code, String name, String status) {
        int safeCurrent = Math.max(current, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safeCurrent - 1) * safePageSize;

        List<WarehouseLocationEntity> entities = warehouseLocationMapper.listPage(
                offset, safePageSize, warehouseId, zoneId, code, name, status);
        long total = warehouseLocationMapper.count(warehouseId, zoneId, code, name, status);

        List<WarehouseLocation> records = entities.stream()
                .map(warehouseLocationConverter::toDomain)
                .toList();
        return PageResponse.of(safeCurrent, safePageSize, total, records);
    }

    WarehouseLocationEntity requireLocationExists(Long id) {
        WarehouseLocationEntity entity = warehouseLocationMapper.getById(id);
        if (entity == null) {
            throw new BusinessException("LOCATION_NOT_FOUND", "库位不存在：" + id);
        }
        return entity;
    }

    private WarehouseZoneEntity requireZoneExists(Long zoneId) {
        WarehouseZoneEntity entity = warehouseZoneMapper.getById(zoneId);
        if (entity == null) {
            throw new BusinessException("ZONE_NOT_FOUND", "库区不存在：" + zoneId);
        }
        return entity;
    }

    private WarehouseEntity requireWarehouseExists(Long warehouseId) {
        WarehouseEntity entity = warehouseMapper.getById(warehouseId);
        if (entity == null) {
            throw new BusinessException("WAREHOUSE_NOT_FOUND", "仓库不存在：" + warehouseId);
        }
        return entity;
    }
}

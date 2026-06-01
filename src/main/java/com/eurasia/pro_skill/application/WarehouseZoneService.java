package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.common.PageResponse;
import com.eurasia.pro_skill.converter.WarehouseZoneConverter;
import com.eurasia.pro_skill.domain.WarehouseZone;
import com.eurasia.pro_skill.entity.WarehouseEntity;
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
public class WarehouseZoneService {

    private final WarehouseMapper warehouseMapper;
    private final WarehouseZoneMapper warehouseZoneMapper;
    private final WarehouseLocationMapper warehouseLocationMapper;
    private final WarehouseZoneConverter warehouseZoneConverter;

    public WarehouseZone create(Long warehouseId, WarehouseZone input) {
        requireWarehouseExists(warehouseId);

        WarehouseZoneEntity existing = warehouseZoneMapper.getByWarehouseIdAndCode(warehouseId, input.getCode());
        if (existing != null) {
            throw new BusinessException("ZONE_CODE_EXISTS", "库区编码已存在：" + input.getCode());
        }

        WarehouseZoneEntity entity = new WarehouseZoneEntity();
        entity.setWarehouseId(warehouseId);
        entity.setCode(input.getCode());
        entity.setName(input.getName());
        entity.setType(input.getType());
        entity.setStatus(WarehouseZone.STATUS_ENABLED);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        warehouseZoneMapper.insert(entity);
        return warehouseZoneConverter.toDomain(entity);
    }

    public WarehouseZone update(Long id, WarehouseZone changes) {
        WarehouseZoneEntity entity = requireZoneExists(id);

        entity.setName(changes.getName());
        entity.setType(changes.getType());
        entity.setStatus(changes.getStatus());
        entity.setUpdatedAt(LocalDateTime.now());

        warehouseZoneMapper.update(entity);
        return warehouseZoneConverter.toDomain(entity);
    }

    public void delete(Long id) {
        WarehouseZoneEntity entity = requireZoneExists(id);
        long locationCount = warehouseLocationMapper.countByZoneId(entity.getId());
        if (locationCount > 0) {
            throw new BusinessException("ZONE_HAS_LOCATIONS", "库区下存在库位，无法删除：" + id);
        }
        warehouseZoneMapper.delete(id);
    }

    public WarehouseZone getById(Long id) {
        return warehouseZoneConverter.toDomain(requireZoneExists(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<WarehouseZone> listPage(Integer current, Integer pageSize, Long warehouseId,
                                              String code, String name, String type, String status) {
        int safeCurrent = Math.max(current, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safeCurrent - 1) * safePageSize;

        List<WarehouseZoneEntity> entities = warehouseZoneMapper.listPage(
                offset, safePageSize, warehouseId, code, name, type, status);
        long total = warehouseZoneMapper.count(warehouseId, code, name, type, status);

        List<WarehouseZone> records = entities.stream()
                .map(warehouseZoneConverter::toDomain)
                .toList();
        return PageResponse.of(safeCurrent, safePageSize, total, records);
    }

    WarehouseZoneEntity requireZoneExists(Long id) {
        WarehouseZoneEntity entity = warehouseZoneMapper.getById(id);
        if (entity == null) {
            throw new BusinessException("ZONE_NOT_FOUND", "库区不存在：" + id);
        }
        return entity;
    }

    private void requireWarehouseExists(Long warehouseId) {
        WarehouseEntity warehouse = warehouseMapper.getById(warehouseId);
        if (warehouse == null) {
            throw new BusinessException("WAREHOUSE_NOT_FOUND", "仓库不存在：" + warehouseId);
        }
    }
}

package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.common.PageResponse;
import com.eurasia.pro_skill.converter.WarehouseConverter;
import com.eurasia.pro_skill.converter.WarehouseLocationConverter;
import com.eurasia.pro_skill.converter.WarehouseZoneConverter;
import com.eurasia.pro_skill.domain.Warehouse;
import com.eurasia.pro_skill.domain.WarehouseLocation;
import com.eurasia.pro_skill.domain.WarehouseTree;
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
public class WarehouseService {

    private final WarehouseMapper warehouseMapper;
    private final WarehouseZoneMapper warehouseZoneMapper;
    private final WarehouseLocationMapper warehouseLocationMapper;
    private final WarehouseConverter warehouseConverter;
    private final WarehouseZoneConverter warehouseZoneConverter;
    private final WarehouseLocationConverter warehouseLocationConverter;

    public Warehouse create(Warehouse input) {
        WarehouseEntity existing = warehouseMapper.getByCode(input.getCode());
        if (existing != null) {
            throw new BusinessException("WAREHOUSE_CODE_EXISTS", "仓库编码已存在：" + input.getCode());
        }

        WarehouseEntity entity = new WarehouseEntity();
        entity.setCode(input.getCode());
        entity.setName(input.getName());
        entity.setType(input.getType());
        entity.setAddress(input.getAddress());
        entity.setManager(input.getManager());
        entity.setPhone(input.getPhone());
        entity.setCapacity(input.getCapacity());
        entity.setStatus(Warehouse.STATUS_ENABLED);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        warehouseMapper.insert(entity);
        return warehouseConverter.toDomain(entity);
    }

    public Warehouse update(Long id, Warehouse changes) {
        WarehouseEntity entity = warehouseMapper.getById(id);
        if (entity == null) {
            throw new BusinessException("WAREHOUSE_NOT_FOUND", "仓库不存在：" + id);
        }

        entity.setName(changes.getName());
        entity.setType(changes.getType());
        entity.setAddress(changes.getAddress());
        entity.setManager(changes.getManager());
        entity.setPhone(changes.getPhone());
        entity.setStatus(changes.getStatus());
        entity.setCapacity(changes.getCapacity());
        entity.setUpdatedAt(LocalDateTime.now());

        warehouseMapper.update(entity);
        return warehouseConverter.toDomain(entity);
    }

    public void delete(Long id) {
        WarehouseEntity entity = warehouseMapper.getById(id);
        if (entity == null) {
            throw new BusinessException("WAREHOUSE_NOT_FOUND", "仓库不存在：" + id);
        }
        long zoneCount = warehouseZoneMapper.countByWarehouseId(id);
        if (zoneCount > 0) {
            throw new BusinessException("WAREHOUSE_HAS_ZONES", "仓库下存在库区，无法删除：" + id);
        }
        warehouseMapper.delete(id);
    }

    public Warehouse getById(Long id) {
        WarehouseEntity entity = warehouseMapper.getById(id);
        if (entity == null) {
            throw new BusinessException("WAREHOUSE_NOT_FOUND", "仓库不存在：" + id);
        }
        return warehouseConverter.toDomain(entity);
    }

    @Transactional(readOnly = true)
    public PageResponse<Warehouse> listPage(Integer current, Integer pageSize,
                                            String code, String name, String type, String status) {
        int safeCurrent = Math.max(current, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safeCurrent - 1) * safePageSize;
        List<WarehouseEntity> entities = warehouseMapper.listPage(offset, safePageSize, code, name, type, status);
        long total = warehouseMapper.count(code, name, type, status);

        List<Warehouse> domains = entities.stream()
                .map(warehouseConverter::toDomain)
                .toList();

        return PageResponse.of(safeCurrent, safePageSize, total, domains);
    }

    /** FR-01 库位视图：仓库 → 库区 → 库位 树形结构 */
    @Transactional(readOnly = true)
    public WarehouseTree getTree(Long warehouseId) {
        WarehouseEntity warehouseEntity = warehouseMapper.getById(warehouseId);
        if (warehouseEntity == null) {
            throw new BusinessException("WAREHOUSE_NOT_FOUND", "仓库不存在：" + warehouseId);
        }

        List<WarehouseZoneEntity> zoneEntities = warehouseZoneMapper.listByWarehouseId(warehouseId);
        List<WarehouseTree.WarehouseZoneNode> zoneNodes = zoneEntities.stream()
                .map(zoneEntity -> {
                    WarehouseZone zone = warehouseZoneConverter.toDomain(zoneEntity);
                    List<WarehouseLocationEntity> locationEntities =
                            warehouseLocationMapper.listByZoneId(zoneEntity.getId());
                    List<WarehouseLocation> locations = locationEntities.stream()
                            .map(warehouseLocationConverter::toDomain)
                            .toList();
                    return WarehouseTree.WarehouseZoneNode.builder()
                            .zone(zone)
                            .locations(locations)
                            .build();
                })
                .toList();

        return WarehouseTree.builder()
                .warehouse(warehouseConverter.toDomain(warehouseEntity))
                .zones(zoneNodes)
                .build();
    }
}

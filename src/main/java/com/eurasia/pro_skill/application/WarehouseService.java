package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.common.PageResponse;
import com.eurasia.pro_skill.converter.WarehouseConverter;
import com.eurasia.pro_skill.domain.Warehouse;
import com.eurasia.pro_skill.controller.request.WarehouseUpdateRequest;
import com.eurasia.pro_skill.entity.WarehouseEntity;
import com.eurasia.pro_skill.mapper.WarehouseMapper;
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
    private final WarehouseConverter warehouseConverter;

    public Warehouse create(Warehouse domain) {
        // 检查编码唯一性
        WarehouseEntity existing = warehouseMapper.getByCode(domain.getCode());
        if (existing != null) {
            throw new BusinessException("WAREHOUSE_CODE_EXISTS", "仓库编码已存在：" + domain.getCode());
        }

        WarehouseEntity entity = warehouseConverter.toEntity(domain);
        entity.setStatus(Warehouse.STATUS_ENABLED);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        warehouseMapper.insert(entity);
        return warehouseConverter.toDomain(entity);
    }

    public Warehouse update(Long id, WarehouseUpdateRequest request) {
        WarehouseEntity entity = warehouseMapper.getById(id);
        if (entity == null) {
            throw new BusinessException("WAREHOUSE_NOT_FOUND", "仓库不存在：" + id);
        }

        // 使用 Converter 将 Request 转换并更新到 Domain
        Warehouse domain = warehouseConverter.toDomain(entity);
        warehouseConverter.updateRequestToDomain(request, domain);

        // 将更新后的 Domain 转回 Entity
        WarehouseEntity updated = warehouseConverter.toEntity(domain);
        updated.setId(id);
        updated.setUpdatedAt(LocalDateTime.now());

        warehouseMapper.update(updated);
        return warehouseConverter.toDomain(updated);
    }

    public void delete(Long id) {
        WarehouseEntity entity = warehouseMapper.getById(id);
        if (entity == null) {
            throw new BusinessException("WAREHOUSE_NOT_FOUND", "仓库不存在：" + id);
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
        // 分页上限保护
        int safePageSize = Math.min(pageSize, 100);
        int offset = (current - 1) * safePageSize;
        List<WarehouseEntity> entities = warehouseMapper.listPage(offset, safePageSize, code, name, type, status);
        long total = warehouseMapper.count(code, name, type, status);

        List<Warehouse> domains = entities.stream()
                .map(warehouseConverter::toDomain)
                .toList();

        return PageResponse.of(current, safePageSize, total, domains);
    }
}

package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.common.PageResponse;
import com.eurasia.pro_skill.converter.WarehouseConverter;
import com.eurasia.pro_skill.domain.Warehouse;
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
}

package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.converter.WarehouseZoneConverter;
import com.eurasia.pro_skill.domain.WarehouseZone;
import com.eurasia.pro_skill.entity.WarehouseEntity;
import com.eurasia.pro_skill.entity.WarehouseZoneEntity;
import com.eurasia.pro_skill.mapper.WarehouseLocationMapper;
import com.eurasia.pro_skill.mapper.WarehouseMapper;
import com.eurasia.pro_skill.mapper.WarehouseZoneMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WarehouseZoneService 单元测试")
class WarehouseZoneServiceTest {

    @Mock
    private WarehouseMapper warehouseMapper;
    @Mock
    private WarehouseZoneMapper warehouseZoneMapper;
    @Mock
    private WarehouseLocationMapper warehouseLocationMapper;
    @Spy
    private WarehouseZoneConverter warehouseZoneConverter = Mappers.getMapper(WarehouseZoneConverter.class);

    @InjectMocks
    private WarehouseZoneService warehouseZoneService;

    @Test
    @DisplayName("创建库区成功")
    void create_success() {
        WarehouseEntity warehouse = new WarehouseEntity();
        warehouse.setId(1L);
        when(warehouseMapper.getById(1L)).thenReturn(warehouse);
        when(warehouseZoneMapper.getByWarehouseIdAndCode(1L, "Z-01")).thenReturn(null);
        when(warehouseZoneMapper.insert(any(WarehouseZoneEntity.class))).thenAnswer(invocation -> {
            WarehouseZoneEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return 1;
        });

        WarehouseZone input = WarehouseZone.builder()
                .code("Z-01")
                .name("A区")
                .type(WarehouseZone.TYPE_STORAGE)
                .build();

        WarehouseZone result = warehouseZoneService.create(1L, input);

        assertEquals(10L, result.getId());
        assertEquals(WarehouseZone.STATUS_ENABLED, result.getStatus());
    }

    @Test
    @DisplayName("创建库区失败：仓库不存在")
    void create_warehouseNotFound() {
        when(warehouseMapper.getById(99L)).thenReturn(null);

        WarehouseZone input = WarehouseZone.builder().code("Z-01").name("A区").type(WarehouseZone.TYPE_STORAGE).build();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> warehouseZoneService.create(99L, input));

        assertEquals("WAREHOUSE_NOT_FOUND", ex.getCode());
    }

    @Test
    @DisplayName("创建库区失败：编码重复")
    void create_codeExists() {
        when(warehouseMapper.getById(1L)).thenReturn(new WarehouseEntity());
        when(warehouseZoneMapper.getByWarehouseIdAndCode(1L, "Z-01")).thenReturn(new WarehouseZoneEntity());

        WarehouseZone input = WarehouseZone.builder().code("Z-01").name("A区").type(WarehouseZone.TYPE_STORAGE).build();

        assertThrows(BusinessException.class, () -> warehouseZoneService.create(1L, input));
    }

    @Test
    @DisplayName("删除库区成功")
    void delete_success() {
        WarehouseZoneEntity zone = new WarehouseZoneEntity();
        zone.setId(10L);
        when(warehouseZoneMapper.getById(10L)).thenReturn(zone);
        when(warehouseLocationMapper.countByZoneId(10L)).thenReturn(0L);

        warehouseZoneService.delete(10L);

        verify(warehouseZoneMapper).delete(10L);
    }

    @Test
    @DisplayName("更新库区成功")
    void update_success() {
        WarehouseZoneEntity entity = new WarehouseZoneEntity();
        entity.setId(10L);
        entity.setCode("Z-01");
        when(warehouseZoneMapper.getById(10L)).thenReturn(entity);

        WarehouseZone changes = WarehouseZone.builder()
                .name("新名称")
                .type(WarehouseZone.TYPE_PICKING)
                .status(WarehouseZone.STATUS_DISABLED)
                .build();

        WarehouseZone result = warehouseZoneService.update(10L, changes);

        assertEquals("新名称", result.getName());
        verify(warehouseZoneMapper).update(entity);
    }

    @Test
    @DisplayName("删除库区失败：存在库位")
    void delete_hasLocations() {
        WarehouseZoneEntity zone = new WarehouseZoneEntity();
        zone.setId(10L);
        when(warehouseZoneMapper.getById(10L)).thenReturn(zone);
        when(warehouseLocationMapper.countByZoneId(10L)).thenReturn(3L);

        BusinessException ex = assertThrows(BusinessException.class, () -> warehouseZoneService.delete(10L));

        assertEquals("ZONE_HAS_LOCATIONS", ex.getCode());
    }

    @Test
    @DisplayName("查询库区详情成功")
    void getById_success() {
        WarehouseZoneEntity entity = new WarehouseZoneEntity();
        entity.setId(10L);
        entity.setCode("Z-01");
        when(warehouseZoneMapper.getById(10L)).thenReturn(entity);

        WarehouseZone result = warehouseZoneService.getById(10L);

        assertEquals("Z-01", result.getCode());
    }

    @Test
    @DisplayName("分页查询库区")
    void listPage_success() {
        when(warehouseZoneMapper.listPage(0, 10, 1L, null, null, null, null)).thenReturn(List.of());
        when(warehouseZoneMapper.count(1L, null, null, null, null)).thenReturn(0L);

        var page = warehouseZoneService.listPage(1, 10, 1L, null, null, null, null);

        assertEquals(0, page.getTotal());
    }
}

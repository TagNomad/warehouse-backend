package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WarehouseService 单元测试")
class WarehouseServiceTest {

    @Mock
    private WarehouseMapper warehouseMapper;
    @Mock
    private WarehouseZoneMapper warehouseZoneMapper;
    @Mock
    private WarehouseLocationMapper warehouseLocationMapper;
    @Spy
    private WarehouseConverter warehouseConverter = Mappers.getMapper(WarehouseConverter.class);
    @Spy
    private WarehouseZoneConverter warehouseZoneConverter = Mappers.getMapper(WarehouseZoneConverter.class);
    @Spy
    private WarehouseLocationConverter warehouseLocationConverter = Mappers.getMapper(WarehouseLocationConverter.class);

    @InjectMocks
    private WarehouseService warehouseService;

    private Warehouse createInput;

    @BeforeEach
    void setUp() {
        createInput = Warehouse.builder()
                .code("WH-TEST")
                .name("测试仓")
                .type(Warehouse.TYPE_RAW_MATERIAL)
                .address("测试地址")
                .capacity(1000)
                .build();
    }

    @Test
    @DisplayName("创建仓库成功")
    void create_success() {
        when(warehouseMapper.getByCode("WH-TEST")).thenReturn(null);
        when(warehouseMapper.insert(any(WarehouseEntity.class))).thenAnswer(invocation -> {
            WarehouseEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return 1;
        });

        Warehouse result = warehouseService.create(createInput);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("WH-TEST", result.getCode());
        assertEquals(Warehouse.STATUS_ENABLED, result.getStatus());
        verify(warehouseMapper).insert(any(WarehouseEntity.class));
    }

    @Test
    @DisplayName("创建仓库失败：编码已存在")
    void create_codeExists_throws() {
        when(warehouseMapper.getByCode("WH-TEST")).thenReturn(new WarehouseEntity());

        BusinessException ex = assertThrows(BusinessException.class, () -> warehouseService.create(createInput));

        assertEquals("WAREHOUSE_CODE_EXISTS", ex.getCode());
        verify(warehouseMapper, never()).insert(any());
    }

    @Test
    @DisplayName("更新仓库成功")
    void update_success() {
        WarehouseEntity existing = buildEntity(1L, "WH-TEST");
        when(warehouseMapper.getById(1L)).thenReturn(existing);

        Warehouse changes = Warehouse.builder()
                .name("新名称")
                .type(Warehouse.TYPE_FINISHED_GOODS)
                .status(Warehouse.STATUS_DISABLED)
                .capacity(2000)
                .build();

        Warehouse result = warehouseService.update(1L, changes);

        assertEquals("新名称", result.getName());
        assertEquals(Warehouse.STATUS_DISABLED, result.getStatus());
        verify(warehouseMapper).update(existing);
    }

    @Test
    @DisplayName("更新仓库失败：不存在")
    void update_notFound_throws() {
        when(warehouseMapper.getById(99L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> warehouseService.update(99L, Warehouse.builder().name("x").build()));
    }

    @Test
    @DisplayName("删除仓库成功")
    void delete_success() {
        when(warehouseMapper.getById(1L)).thenReturn(buildEntity(1L, "WH-TEST"));
        when(warehouseZoneMapper.countByWarehouseId(1L)).thenReturn(0L);

        warehouseService.delete(1L);

        verify(warehouseMapper).delete(1L);
    }

    @Test
    @DisplayName("删除仓库失败：存在库区")
    void delete_hasZones_throws() {
        when(warehouseMapper.getById(1L)).thenReturn(buildEntity(1L, "WH-TEST"));
        when(warehouseZoneMapper.countByWarehouseId(1L)).thenReturn(2L);

        BusinessException ex = assertThrows(BusinessException.class, () -> warehouseService.delete(1L));

        assertEquals("WAREHOUSE_HAS_ZONES", ex.getCode());
        verify(warehouseMapper, never()).delete(anyLong());
    }

    @Test
    @DisplayName("查询库位视图成功")
    void getTree_success() {
        WarehouseEntity warehouse = buildEntity(1L, "WH-001");
        WarehouseZoneEntity zone = new WarehouseZoneEntity();
        zone.setId(10L);
        zone.setWarehouseId(1L);
        zone.setCode("Z-01");
        zone.setName("A区");
        zone.setType(WarehouseZone.TYPE_STORAGE);
        zone.setStatus(WarehouseZone.STATUS_ENABLED);

        WarehouseLocationEntity location = new WarehouseLocationEntity();
        location.setId(100L);
        location.setZoneId(10L);
        location.setCode("A-01-01");
        location.setStatus(WarehouseLocation.STATUS_AVAILABLE);

        when(warehouseMapper.getById(1L)).thenReturn(warehouse);
        when(warehouseZoneMapper.listByWarehouseId(1L)).thenReturn(List.of(zone));
        when(warehouseLocationMapper.listByZoneId(10L)).thenReturn(List.of(location));

        WarehouseTree tree = warehouseService.getTree(1L);

        assertEquals("WH-001", tree.getWarehouse().getCode());
        assertEquals(1, tree.getZones().size());
        assertEquals(1, tree.getZones().getFirst().getLocations().size());
    }

    @Test
    @DisplayName("查询仓库详情成功")
    void getById_success() {
        when(warehouseMapper.getById(1L)).thenReturn(buildEntity(1L, "WH-TEST"));

        Warehouse result = warehouseService.getById(1L);

        assertEquals("WH-TEST", result.getCode());
    }

    @Test
    @DisplayName("查询仓库详情失败：不存在")
    void getById_notFound() {
        when(warehouseMapper.getById(99L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> warehouseService.getById(99L));
    }

    @Test
    @DisplayName("分页查询会校正页码与每页条数")
    void listPage_normalizesPagination() {
        when(warehouseMapper.listPage(0, 100, null, null, null, null)).thenReturn(List.of());
        when(warehouseMapper.count(null, null, null, null)).thenReturn(0L);

        var page = warehouseService.listPage(0, 200, null, null, null, null);

        assertEquals(1, page.getCurrent());
        assertEquals(100, page.getPageSize());
    }

    @Test
    @DisplayName("库位视图失败：仓库不存在")
    void getTree_notFound() {
        when(warehouseMapper.getById(99L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> warehouseService.getTree(99L));
    }

    private WarehouseEntity buildEntity(Long id, String code) {
        WarehouseEntity entity = new WarehouseEntity();
        entity.setId(id);
        entity.setCode(code);
        entity.setName("测试仓");
        entity.setType(Warehouse.TYPE_RAW_MATERIAL);
        entity.setStatus(Warehouse.STATUS_ENABLED);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }
}

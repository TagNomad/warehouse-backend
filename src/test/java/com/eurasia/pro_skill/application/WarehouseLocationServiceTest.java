package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.converter.WarehouseConverter;
import com.eurasia.pro_skill.converter.WarehouseLocationConverter;
import com.eurasia.pro_skill.converter.WarehouseZoneConverter;
import com.eurasia.pro_skill.domain.WarehouseLocation;
import com.eurasia.pro_skill.domain.WarehouseLocationDetail;
import com.eurasia.pro_skill.entity.WarehouseEntity;
import com.eurasia.pro_skill.entity.WarehouseLocationEntity;
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
@DisplayName("WarehouseLocationService 单元测试")
class WarehouseLocationServiceTest {

    @Mock
    private WarehouseMapper warehouseMapper;
    @Mock
    private WarehouseZoneMapper warehouseZoneMapper;
    @Mock
    private WarehouseLocationMapper warehouseLocationMapper;
    @Spy
    private WarehouseLocationConverter warehouseLocationConverter = Mappers.getMapper(WarehouseLocationConverter.class);
    @Spy
    private WarehouseZoneConverter warehouseZoneConverter = Mappers.getMapper(WarehouseZoneConverter.class);
    @Spy
    private WarehouseConverter warehouseConverter = Mappers.getMapper(WarehouseConverter.class);

    @InjectMocks
    private WarehouseLocationService warehouseLocationService;

    @Test
    @DisplayName("创建库位成功")
    void create_success() {
        WarehouseZoneEntity zone = new WarehouseZoneEntity();
        zone.setId(10L);
        when(warehouseZoneMapper.getById(10L)).thenReturn(zone);
        when(warehouseLocationMapper.getByZoneIdAndCode(10L, "A-01-01")).thenReturn(null);
        when(warehouseLocationMapper.insert(any(WarehouseLocationEntity.class))).thenAnswer(invocation -> {
            WarehouseLocationEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return 1;
        });

        WarehouseLocation input = WarehouseLocation.builder()
                .code("A-01-01")
                .name("1排1位")
                .spec("1m×1m")
                .build();

        WarehouseLocation result = warehouseLocationService.create(10L, input);

        assertEquals(100L, result.getId());
        assertEquals(WarehouseLocation.STATUS_AVAILABLE, result.getStatus());
    }

    @Test
    @DisplayName("更新库位失败：占用状态不可改为可用")
    void update_occupiedCannotChangeStatus() {
        WarehouseLocationEntity entity = new WarehouseLocationEntity();
        entity.setId(100L);
        entity.setZoneId(10L);
        entity.setStatus(WarehouseLocation.STATUS_OCCUPIED);
        when(warehouseLocationMapper.getById(100L)).thenReturn(entity);

        WarehouseLocation changes = WarehouseLocation.builder()
                .status(WarehouseLocation.STATUS_AVAILABLE)
                .build();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> warehouseLocationService.update(100L, changes));

        assertEquals("LOCATION_OCCUPIED", ex.getCode());
    }

    @Test
    @DisplayName("删除库位失败：占用中")
    void delete_occupied() {
        WarehouseLocationEntity entity = new WarehouseLocationEntity();
        entity.setId(100L);
        entity.setStatus(WarehouseLocation.STATUS_OCCUPIED);
        when(warehouseLocationMapper.getById(100L)).thenReturn(entity);

        assertThrows(BusinessException.class, () -> warehouseLocationService.delete(100L));
    }

    @Test
    @DisplayName("库位详情聚合成功")
    void getDetail_success() {
        WarehouseLocationEntity location = new WarehouseLocationEntity();
        location.setId(100L);
        location.setZoneId(10L);
        location.setCode("A-01-01");

        WarehouseZoneEntity zone = new WarehouseZoneEntity();
        zone.setId(10L);
        zone.setWarehouseId(1L);
        zone.setCode("Z-01");

        WarehouseEntity warehouse = new WarehouseEntity();
        warehouse.setId(1L);
        warehouse.setCode("WH-001");

        when(warehouseLocationMapper.getById(100L)).thenReturn(location);
        when(warehouseZoneMapper.getById(10L)).thenReturn(zone);
        when(warehouseMapper.getById(1L)).thenReturn(warehouse);

        WarehouseLocationDetail detail = warehouseLocationService.getDetail(100L);

        assertEquals("A-01-01", detail.getLocation().getCode());
        assertEquals("Z-01", detail.getZone().getCode());
        assertEquals("WH-001", detail.getWarehouse().getCode());
    }

    @Test
    @DisplayName("创建库位失败：编码重复")
    void create_codeExists() {
        WarehouseZoneEntity zone = new WarehouseZoneEntity();
        zone.setId(10L);
        when(warehouseZoneMapper.getById(10L)).thenReturn(zone);
        when(warehouseLocationMapper.getByZoneIdAndCode(10L, "A-01-01")).thenReturn(new WarehouseLocationEntity());

        WarehouseLocation input = WarehouseLocation.builder().code("A-01-01").name("x").build();

        assertThrows(BusinessException.class, () -> warehouseLocationService.create(10L, input));
    }

    @Test
    @DisplayName("更新库位成功")
    void update_success() {
        WarehouseLocationEntity entity = new WarehouseLocationEntity();
        entity.setId(100L);
        entity.setZoneId(10L);
        entity.setStatus(WarehouseLocation.STATUS_AVAILABLE);
        when(warehouseLocationMapper.getById(100L)).thenReturn(entity);

        WarehouseLocation changes = WarehouseLocation.builder()
                .name("新名称")
                .spec("2m×2m")
                .status(WarehouseLocation.STATUS_DISABLED)
                .build();

        WarehouseLocation result = warehouseLocationService.update(100L, changes);

        assertEquals("新名称", result.getName());
        verify(warehouseLocationMapper).update(entity);
    }

    @Test
    @DisplayName("删除库位成功")
    void delete_success() {
        WarehouseLocationEntity entity = new WarehouseLocationEntity();
        entity.setId(100L);
        entity.setStatus(WarehouseLocation.STATUS_AVAILABLE);
        when(warehouseLocationMapper.getById(100L)).thenReturn(entity);

        warehouseLocationService.delete(100L);

        verify(warehouseLocationMapper).delete(100L);
    }

    @Test
    @DisplayName("查询库位详情成功")
    void getById_success() {
        WarehouseLocationEntity entity = new WarehouseLocationEntity();
        entity.setId(100L);
        entity.setCode("A-01-01");
        when(warehouseLocationMapper.getById(100L)).thenReturn(entity);

        WarehouseLocation result = warehouseLocationService.getById(100L);

        assertEquals("A-01-01", result.getCode());
    }

    @Test
    @DisplayName("分页查询库位")
    void listPage_success() {
        when(warehouseLocationMapper.listPage(0, 10, null, null, null, null, null)).thenReturn(List.of());
        when(warehouseLocationMapper.count(null, null, null, null, null)).thenReturn(0L);

        var page = warehouseLocationService.listPage(1, 10, null, null, null, null, null);

        assertEquals(0, page.getTotal());
    }
}

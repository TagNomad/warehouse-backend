package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.domain.InboundOrder;
import com.eurasia.pro_skill.domain.InboundOrderLine;
import com.eurasia.pro_skill.entity.InboundOrderEntity;
import com.eurasia.pro_skill.entity.InboundOrderLineEntity;
import com.eurasia.pro_skill.entity.InventoryEntity;
import com.eurasia.pro_skill.entity.SkuEntity;
import com.eurasia.pro_skill.entity.WarehouseLocationEntity;
import com.eurasia.pro_skill.mapper.InboundOrderLineMapper;
import com.eurasia.pro_skill.mapper.InboundOrderMapper;
import com.eurasia.pro_skill.mapper.InventoryMapper;
import com.eurasia.pro_skill.mapper.SkuMapper;
import com.eurasia.pro_skill.mapper.WarehouseLocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InboundService {

    private final InboundOrderMapper orderMapper;
    private final InboundOrderLineMapper lineMapper;
    private final SkuMapper skuMapper;
    private final WarehouseLocationMapper locationMapper;
    private final InventoryMapper inventoryMapper; // 按货品和库位记账，和改状态在同一个事务里

    public InboundOrder create() {
        InboundOrderEntity entity = new InboundOrderEntity();
        entity.setOrderNo("PENDING"); // 占位，拿到主键后再改成 IN-主键
        entity.setStatus(InboundOrder.DRAFT); // 新建就是草稿
        entity.setCreatedAt(LocalDateTime.now());
        orderMapper.insert(entity); // 插入后 entity.id 有值
        entity.setOrderNo("IN-" + entity.getId()); // 单号用主键，避免重复
        orderMapper.updateOrderNo(entity);
        return toDomain(entity);
    }

    public InboundOrder addLine(Long orderId, String skuCode, String locationCode, int quantity) {
        InboundOrderEntity order = requireDraft(orderId); // 只有草稿能加行
        SkuEntity sku = skuMapper.getByCode(skuCode); // 按编码找货品
        if (sku == null) {
            throw new BusinessException("SKU_NOT_FOUND", "货品不存在：" + skuCode);
        }
        WarehouseLocationEntity location = locationMapper.getByCode(locationCode); // 按编码找库位
        if (location == null) {
            throw new BusinessException("LOCATION_NOT_FOUND", "库位不存在：" + locationCode);
        }
        InboundOrderLineEntity line = new InboundOrderLineEntity();
        line.setOrderId(order.getId()); // 属于这张单
        line.setSkuId(sku.getId()); // 记下货品主键
        line.setLocationId(location.getId()); // 记下库位主键
        line.setQuantity(quantity); // 这一行的数量
        lineMapper.insert(line);
        return toDomain(order);
    }

    public InboundOrder complete(Long orderId) {
        InboundOrderEntity order = requireDraft(orderId);
        List<InboundOrderLineEntity> lines = lineMapper.listByOrderId(order.getId());
        if (lines.isEmpty()) {
            throw new BusinessException("INBOUND_LINE_REQUIRED", "入库单还没有行");
        }
        for (InboundOrderLineEntity line : lines) {
            // 这一行记到「货品 + 库位」。和下面改状态是同一个事务，任何一步失败都会一起撤回
            addInventory(line.getSkuId(), line.getLocationId(), line.getQuantity());
        }
        order.setStatus(InboundOrder.COMPLETED); // 数量记下之后，同一事务里把草稿改成完成
        orderMapper.updateStatus(order);
        return toDomain(orderMapper.getById(order.getId()));
    }

    private void addInventory(Long skuId, Long locationId, int quantity) {
        InventoryEntity existing = inventoryMapper.getBySkuAndLocation(skuId, locationId);
        if (existing == null) {
            InventoryEntity row = new InventoryEntity();
            row.setSkuId(skuId); // 哪一种货
            row.setLocationId(locationId); // 哪一个格子
            row.setQuantity(quantity); // 第一次入库，数量就是这一行
            row.setVersion(0); // 先写入，乐观锁以后再拿它做条件
            inventoryMapper.insert(row);
            return;
        }
        inventoryMapper.addQuantity(skuId, locationId, quantity); // 已有这一行，数量加上，version 加一
    }

    private InboundOrderEntity requireDraft(Long orderId) {
        InboundOrderEntity order = orderMapper.getById(orderId);
        if (order == null) {
            throw new BusinessException("INBOUND_NOT_FOUND", "入库单不存在：" + orderId);
        }
        if (!InboundOrder.DRAFT.equals(order.getStatus())) {
            throw new BusinessException("INBOUND_NOT_DRAFT", "只有草稿可以继续：" + order.getOrderNo());
        }
        return order;
    }

    private InboundOrder toDomain(InboundOrderEntity entity) {
        InboundOrder order = new InboundOrder();
        order.setId(entity.getId());
        order.setOrderNo(entity.getOrderNo());
        order.setStatus(entity.getStatus());
        order.setCreatedAt(entity.getCreatedAt());
        for (InboundOrderLineEntity lineEntity : lineMapper.listByOrderId(entity.getId())) {
            InboundOrderLine line = new InboundOrderLine();
            line.setId(lineEntity.getId());
            line.setOrderId(lineEntity.getOrderId());
            line.setSkuId(lineEntity.getSkuId());
            line.setLocationId(lineEntity.getLocationId());
            line.setQuantity(lineEntity.getQuantity());
            SkuEntity sku = skuMapper.getById(lineEntity.getSkuId());
            if (sku != null) {
                line.setSkuCode(sku.getCode());
            }
            WarehouseLocationEntity location = locationMapper.getById(lineEntity.getLocationId());
            if (location != null) {
                line.setLocationCode(location.getCode());
            }
            order.getLines().add(line);
        }
        return order;
    }
}

package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.domain.OutboundOrder;
import com.eurasia.pro_skill.domain.OutboundOrderLine;
import com.eurasia.pro_skill.entity.InventoryEntity;
import com.eurasia.pro_skill.entity.OutboundOrderEntity;
import com.eurasia.pro_skill.entity.OutboundOrderLineEntity;
import com.eurasia.pro_skill.entity.SkuEntity;
import com.eurasia.pro_skill.entity.WarehouseLocationEntity;
import com.eurasia.pro_skill.mapper.InventoryMapper;
import com.eurasia.pro_skill.mapper.OutboundOrderLineMapper;
import com.eurasia.pro_skill.mapper.OutboundOrderMapper;
import com.eurasia.pro_skill.mapper.SkuMapper;
import com.eurasia.pro_skill.mapper.WarehouseLocationMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// 出库业务。完成时从 inventory 里减数量。
@Service
@RequiredArgsConstructor
@Transactional // 这个方法里的读取和更新在同一个事务里。事务不保证别人读到的数量还有效
public class OutboundService {

    private static final Logger log = LoggerFactory.getLogger(OutboundService.class);

    private final OutboundOrderMapper orderMapper; // 写出库单
    private final OutboundOrderLineMapper lineMapper; // 写出库行
    private final SkuMapper skuMapper; // 编码换成货品主键
    private final WarehouseLocationMapper locationMapper; // 编码换成库位主键
    private final InventoryMapper inventoryMapper; // 减这个格子上的数量
    private final LessonSwitches switches; // outbound-check：off 不看剩余够不够

    public OutboundOrder create() { // 没有页面字段
        OutboundOrderEntity entity = new OutboundOrderEntity(); // 还没有 id
        entity.setOrderNo("PENDING"); // 主键出来之前先占位
        entity.setStatus(OutboundOrder.DRAFT); // 新建是草稿
        entity.setCreatedAt(LocalDateTime.now()); // 时间不来自页面
        orderMapper.insert(entity); // 插入后 entity.getId() 才有值
        entity.setOrderNo("OUT-" + entity.getId()); // 单号用主键，例如 OUT-1
        orderMapper.updateOrderNo(entity); // 把 PENDING 改成 OUT-主键
        return toDomain(entity);
    }

    public OutboundOrder addLine(Long orderId, String skuCode, String locationCode, int quantity) {
        OutboundOrderEntity order = requireDraft(orderId); // 路径上的主键，只有草稿能加行
        SkuEntity sku = skuMapper.getByCode(skuCode); // 页面上的编码换成货品那一行
        if (sku == null) {
            throw new BusinessException("SKU_NOT_FOUND", "货品不存在：" + skuCode);
        }
        WarehouseLocationEntity location = locationMapper.getByCode(locationCode); // 库位编码换成库位那一行
        if (location == null) {
            throw new BusinessException("LOCATION_NOT_FOUND", "库位不存在：" + locationCode);
        }
        OutboundOrderLineEntity line = new OutboundOrderLineEntity();
        line.setOrderId(order.getId()); // 属于这张出库单
        line.setSkuId(sku.getId()); // 写下货品主键
        line.setLocationId(location.getId()); // 写下库位主键
        line.setQuantity(quantity); // 页面传来的数量
        lineMapper.insert(line);
        return toDomain(order);
    }

    public OutboundOrder complete(Long orderId) { // 路径上只有主键
        OutboundOrderEntity order = requireDraft(orderId);
        List<OutboundOrderLineEntity> lines = lineMapper.listByOrderId(order.getId());
        if (lines.isEmpty()) {
            throw new BusinessException("OUTBOUND_LINE_REQUIRED", "出库单还没有行");
        }
        for (OutboundOrderLineEntity line : lines) {
            InventoryEntity inventory = inventoryMapper.getBySkuAndLocation(line.getSkuId(), line.getLocationId());
            if (inventory == null) { // 这个格子上还没有这种货
                throw new BusinessException("INVENTORY_NOT_FOUND", "这个格子上没有库存");
            }
            int current = inventory.getQuantity(); // 读到的剩余
            log.info("读到数量 {}", current); // 和下面的更新要靠 TraceId 才能圈成同一次请求
            int next = current - line.getQuantity(); // 减这一行的数量。剩余不够时这里会变成负数
            if (switches.outboundCheck() && next < 0) { // 开关 off 时这一行不执行，负数也会写进去
                throw new BusinessException("INSUFFICIENT_STOCK", "INSUFFICIENT_STOCK：库存不足，剩余 " + current);
            }
            int updated;
            if (switches.optimisticLock()) { // 本周默认 off。打开后，version 对不上就改不到行
                updated = inventoryMapper.updateQuantityIfVersion(inventory.getId(), next, inventory.getVersion());
                if (updated == 0) {
                    throw new BusinessException("STOCK_CONFLICT", "STOCK_CONFLICT：库存已被别人改过");
                }
            } else {
                updated = inventoryMapper.updateQuantity(inventory.getId(), next); // 只按 id 更新，不带 version
            }
            log.info("把数量改成 {}，影响行数 {}", next, updated);
        }
        order.setStatus(OutboundOrder.COMPLETED); // 数量减完，同一事务里改成完成
        orderMapper.updateStatus(order);
        return toDomain(orderMapper.getById(order.getId()));
    }

    public OutboundOrder updateLine(Long orderId, Long lineId, int quantity) {
        OutboundOrderEntity order = orderMapper.getById(orderId); // 先找出库单
        if (order == null) {
            throw new BusinessException("OUTBOUND_NOT_FOUND", "出库单不存在：" + orderId);
        }
        if (switches.orderStateEnforced() && !OutboundOrder.DRAFT.equals(order.getStatus())) {
            // 开关是 open 时，已完成的单也能改行上的数量
            throw new BusinessException("ORDER_NOT_EDITABLE", "ORDER_NOT_EDITABLE：只有草稿可以改数量：" + order.getOrderNo());
        }
        OutboundOrderLineEntity line = lineMapper.getById(lineId);
        if (line == null || !order.getId().equals(line.getOrderId())) {
            throw new BusinessException("OUTBOUND_LINE_NOT_FOUND", "出库行不存在：" + lineId);
        }
        lineMapper.updateQuantity(lineId, quantity); // 只改行上的数量，不改 inventory
        return toDomain(order);
    }

    private OutboundOrderEntity requireDraft(Long orderId) {
        OutboundOrderEntity order = orderMapper.getById(orderId);
        if (order == null) {
            throw new BusinessException("OUTBOUND_NOT_FOUND", "出库单不存在：" + orderId);
        }
        if (!OutboundOrder.DRAFT.equals(order.getStatus())) {
            throw new BusinessException("OUTBOUND_NOT_DRAFT", "只有草稿可以继续：" + order.getOrderNo());
        }
        return order;
    }

    private OutboundOrder toDomain(OutboundOrderEntity entity) {
        OutboundOrder order = new OutboundOrder();
        order.setId(entity.getId());
        order.setOrderNo(entity.getOrderNo());
        order.setStatus(entity.getStatus());
        order.setCreatedAt(entity.getCreatedAt());
        for (OutboundOrderLineEntity lineEntity : lineMapper.listByOrderId(entity.getId())) {
            OutboundOrderLine line = new OutboundOrderLine();
            line.setId(lineEntity.getId());
            line.setOrderId(lineEntity.getOrderId());
            line.setSkuId(lineEntity.getSkuId());
            line.setLocationId(lineEntity.getLocationId());
            line.setQuantity(lineEntity.getQuantity());
            SkuEntity sku = skuMapper.getById(lineEntity.getSkuId()); // 主键查回编码
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

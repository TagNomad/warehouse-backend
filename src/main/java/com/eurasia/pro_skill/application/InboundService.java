package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.domain.InboundOrder;
import com.eurasia.pro_skill.domain.InboundOrderLine;
import com.eurasia.pro_skill.entity.InboundOrderEntity;
import com.eurasia.pro_skill.entity.InboundOrderLineEntity;
import com.eurasia.pro_skill.entity.SkuEntity;
import com.eurasia.pro_skill.entity.WarehouseLocationEntity;
import com.eurasia.pro_skill.mapper.InboundOrderLineMapper;
import com.eurasia.pro_skill.mapper.InboundOrderMapper;
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
    private final SkuOnHandWriter onHandWriter; // 加数量用它自己的事务

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
            // 每一行的数量加到货品的 on_hand。这一步自己提交，后面中断也撤不掉
            onHandWriter.add(line.getSkuId(), line.getQuantity());
        }
        // 数量已经提交。状态还没改成 COMPLETED，这里中断
        throw new BusinessException(
                "INBOUND_STATUS_INTERRUPTED",
                "INBOUND_STATUS_INTERRUPTED：入库完成在改状态前中断");
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

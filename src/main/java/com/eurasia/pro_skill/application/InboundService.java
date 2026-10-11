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

// 入库业务。Controller 把字段传进来，这里决定写哪张表。
@Service // 注册成 Bean，Controller 才能注入
@RequiredArgsConstructor // 为下面的 final 字段生成构造方法
@Transactional // 这个类的方法默认开一个事务。加数量那一步不在这个事务里
public class InboundService {

    private final InboundOrderMapper orderMapper; // 写 inbound_order
    private final InboundOrderLineMapper lineMapper; // 写 inbound_order_line
    private final SkuMapper skuMapper; // 按编码或主键找货品，也负责改 on_hand
    private final WarehouseLocationMapper locationMapper; // 按编码或主键找库位
    private final SkuOnHandWriter onHandWriter; // 加数量用它自己的事务，外层中断撤不掉

    public InboundOrder create() { // 没有页面字段。单号和状态都在这里生成
        InboundOrderEntity entity = new InboundOrderEntity(); // 准备要插入的那一行，此时还没有 id
        entity.setOrderNo("PENDING"); // 先占位。主键还没有，单号暂时不能写成 IN-主键
        entity.setStatus(InboundOrder.DRAFT); // 新建就是草稿，还没完成
        entity.setCreatedAt(LocalDateTime.now()); // 创建时间不来自页面
        orderMapper.insert(entity); // 插入后，数据库把自增 id 写回 entity.getId()
        entity.setOrderNo("IN-" + entity.getId()); // 单号用主键，例如 id 是 1 就是 IN-1
        orderMapper.updateOrderNo(entity); // 把占位的 PENDING 改成 IN-主键
        return toDomain(entity); // 表的一行转成返回给页面的 InboundOrder
    }

    public InboundOrder addLine(Long orderId, String skuCode, String locationCode, int quantity) {
        InboundOrderEntity order = requireDraft(orderId); // 只有草稿能加行。orderId 来自路径
        SkuEntity sku = skuMapper.getByCode(skuCode); // skuCode 是页面上的编码，这里换成货品那一行
        if (sku == null) { // 编码在 sku 表里不存在
            throw new BusinessException("SKU_NOT_FOUND", "货品不存在：" + skuCode); // 400，不再插入行
        }
        WarehouseLocationEntity location = locationMapper.getByCode(locationCode); // 库位编码换成库位那一行
        if (location == null) {
            throw new BusinessException("LOCATION_NOT_FOUND", "库位不存在：" + locationCode);
        }
        InboundOrderLineEntity line = new InboundOrderLineEntity(); // 准备 inbound_order_line 的一行
        line.setOrderId(order.getId()); // 属于这张入库单
        line.setSkuId(sku.getId()); // 写下货品主键，不写 SKU-001 这个字符串
        line.setLocationId(location.getId()); // 写下库位主键，不写 A-01-01 这个字符串
        line.setQuantity(quantity); // 页面传来的数量
        lineMapper.insert(line); // 插入后，行主键写回 line.getId()
        return toDomain(order); // 把这张单和它的全部行一起返回
    }

    public InboundOrder complete(Long orderId) { // 路径上只有主键，没有新的数量
        InboundOrderEntity order = requireDraft(orderId); // 不是草稿就不能再完成一次
        List<InboundOrderLineEntity> lines = lineMapper.listByOrderId(order.getId()); // 这张单上已经加过的行
        if (lines.isEmpty()) { // 没有行就没有数量可加
            throw new BusinessException("INBOUND_LINE_REQUIRED", "入库单还没有行");
        }
        for (InboundOrderLineEntity line : lines) {
            // 数量加到货品的 on_hand。这一步自己提交，下面抛出也撤不掉
            // 行上的 locationId 没有传进去，所以两个格子的数量会加在同一个总数上
            onHandWriter.add(line.getSkuId(), line.getQuantity());
        }
        // 数量已经提交。状态还没改成 COMPLETED，这里中断，方法到不了 updateStatus
        throw new BusinessException(
                "INBOUND_STATUS_INTERRUPTED",
                "INBOUND_STATUS_INTERRUPTED：入库完成在改状态前中断");
    }

    private InboundOrderEntity requireDraft(Long orderId) {
        InboundOrderEntity order = orderMapper.getById(orderId); // #{id} 就是路径上的主键
        if (order == null) {
            throw new BusinessException("INBOUND_NOT_FOUND", "入库单不存在：" + orderId);
        }
        if (!InboundOrder.DRAFT.equals(order.getStatus())) { // 已完成的单不能再加行，也不能再完成
            throw new BusinessException("INBOUND_NOT_DRAFT", "只有草稿可以继续：" + order.getOrderNo());
        }
        return order;
    }

    private InboundOrder toDomain(InboundOrderEntity entity) { // 表的一行变成页面看到的 JSON
        InboundOrder order = new InboundOrder();
        order.setId(entity.getId()); // 主键
        order.setOrderNo(entity.getOrderNo()); // IN-主键
        order.setStatus(entity.getStatus()); // DRAFT 或 COMPLETED
        order.setCreatedAt(entity.getCreatedAt());
        for (InboundOrderLineEntity lineEntity : lineMapper.listByOrderId(entity.getId())) {
            InboundOrderLine line = new InboundOrderLine();
            line.setId(lineEntity.getId());
            line.setOrderId(lineEntity.getOrderId());
            line.setSkuId(lineEntity.getSkuId()); // 行上存的是主键
            line.setLocationId(lineEntity.getLocationId());
            line.setQuantity(lineEntity.getQuantity());
            SkuEntity sku = skuMapper.getById(lineEntity.getSkuId()); // 主键再查回编码，页面才看得懂
            if (sku != null) {
                line.setSkuCode(sku.getCode()); // 例如 SKU-001
            }
            WarehouseLocationEntity location = locationMapper.getById(lineEntity.getLocationId());
            if (location != null) {
                line.setLocationCode(location.getCode()); // 例如 A-01-01
            }
            order.getLines().add(line);
        }
        return order;
    }
}

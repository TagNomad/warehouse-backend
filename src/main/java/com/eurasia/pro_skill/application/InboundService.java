package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.domain.InboundOrder;
import com.eurasia.pro_skill.domain.InboundOrderLine;
import com.eurasia.pro_skill.entity.InboundOrderEntity;
import com.eurasia.pro_skill.entity.InboundOrderLineEntity;
import com.eurasia.pro_skill.entity.InventoryEntity;
import com.eurasia.pro_skill.entity.SkuEntity;
import com.eurasia.pro_skill.entity.WarehouseLocationEntity;
import com.eurasia.pro_skill.entity.StockFlowEntity;
import com.eurasia.pro_skill.mapper.InboundOrderLineMapper;
import com.eurasia.pro_skill.mapper.InboundOrderMapper;
import com.eurasia.pro_skill.mapper.InventoryMapper;
import com.eurasia.pro_skill.mapper.StockFlowMapper;
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
@Transactional // 这个类里的方法默认开一个事务。complete 里加库存和改状态一起提交，失败就一起撤回
public class InboundService {

    private final InboundOrderMapper orderMapper; // 写 inbound_order
    private final InboundOrderLineMapper lineMapper; // 写 inbound_order_line
    private final SkuMapper skuMapper; // 按编码或主键找货品
    private final WarehouseLocationMapper locationMapper; // 按编码或主键找库位
    private final InventoryMapper inventoryMapper; // 按货品 + 库位记账
    private final StockFlowMapper stockFlowMapper; // 和入库同一次提交时用它写流水
    private final StockFlowWriter stockFlowWriter; // 开关关掉时，流水用另一个事务先提交
    private final LessonSwitches switches;

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
        if (lines.isEmpty()) { // 没有行就没有数量可记
            throw new BusinessException("INBOUND_LINE_REQUIRED", "入库单还没有行");
        }
        for (InboundOrderLineEntity line : lines) { // 每一行单独记到自己的货品和库位
            addInventory(line.getSkuId(), line.getLocationId(), line.getQuantity());
            writeFlow(order.getOrderNo(), line); // 流水。开关决定它跟不跟下面的状态在同一个事务
        }
        if (switches.inboundFailBeforeStatus()) { // 演示用：主事务在改状态前失败
            throw new BusinessException("INBOUND_MAIN_ROLLED_BACK", "INBOUND_MAIN_ROLLED_BACK：主事务在改状态前回滚");
        }
        order.setStatus(InboundOrder.COMPLETED); // 数量记下之后，同一事务里把草稿改成完成
        orderMapper.updateStatus(order); // SQL 只改 status 这一列
        return toDomain(orderMapper.getById(order.getId())); // 再查一次，返回的 status 是 COMPLETED
    }

    private void addInventory(Long skuId, Long locationId, int quantity) {
        InventoryEntity existing = inventoryMapper.getBySkuAndLocation(skuId, locationId); // 这个格子有没有这一货
        if (existing == null) { // 第一次放到这个格子，插入新的一行
            InventoryEntity row = new InventoryEntity();
            row.setSkuId(skuId); // 哪一种货
            row.setLocationId(locationId); // 哪一个格子
            row.setQuantity(quantity); // 第一次的数量就是这一行的数量
            row.setVersion(0); // 先写入。乐观锁以后再拿它做更新条件
            inventoryMapper.insert(row); // 唯一键是货品主键 + 库位主键，同一个格子不会有第二行
            return;
        }
        inventoryMapper.addQuantity(skuId, locationId, quantity); // 已有这一行：quantity 加上，version 加一
    }

    private void writeFlow(String orderNo, InboundOrderLineEntity line) {
        if (switches.flowAfterCommit()) { // on：流水和改状态在 InboundService 这一个事务里
            StockFlowEntity row = new StockFlowEntity(); // 准备 stock_flow 的一行
            row.setBizNo(orderNo); // 写进 biz_no：入库单号，例如 IN-3
            row.setSkuId(line.getSkuId()); // 写进 sku_id：来自入库行
            row.setLocationId(line.getLocationId()); // 写进 location_id：来自入库行
            row.setQuantity(line.getQuantity()); // 写进 quantity：这一行入库的数量
            row.setCreatedAt(java.time.LocalDateTime.now()); // 写进 created_at
            stockFlowMapper.insert(row); // 还没提交。主事务回滚时，这一行一起没了
            return;
        }
        // off：交给另一个 Bean，用 REQUIRES_NEW 另开事务。这一行马上提交，主事务回滚也撤不掉
        stockFlowWriter.write(orderNo, line.getSkuId(), line.getLocationId(), line.getQuantity());
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

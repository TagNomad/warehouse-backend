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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

// 出库业务。Controller 把路径和字段传进来，这里决定读哪张表、写哪张表。完成时从 inventory 里减数量。
@Service // 注册成 Bean，OutboundController 才能注入
@RequiredArgsConstructor // 为下面的 final 字段生成构造方法
@Transactional // 每个方法开一个事务。事务只保证这一次请求里的写一起提交，不保证别人读到的数量还有效
public class OutboundService {

    private static final Logger log = LoggerFactory.getLogger(OutboundService.class);

    private final OutboundOrderMapper orderMapper; // 写 outbound_order
    private final OutboundOrderLineMapper lineMapper; // 写 outbound_order_line
    private final SkuMapper skuMapper; // 货品编码换成货品主键
    private final WarehouseLocationMapper locationMapper; // 库位编码换成库位主键
    private final InventoryMapper inventoryMapper; // 读、改这个格子上的数量
    private final LessonSwitches switches; // 课堂开关，决定走有漏洞的一侧还是修好的一侧
    private final LessonRedis redis; // 记 Idempotency-Key
    private final StockLock stockLock; // 一个格子一把 Redis 锁
    private final InventoryCache inventoryCache; // 数量改了以后删缓存
    private final TransactionTemplate transactionTemplate; // 在方法中间开事务：complete 要先拿锁再开

    private static final long IDEMPOTENCY_TTL_MS = 24 * 60 * 60 * 1000L; // 一天内重发同一个键都算重复

    public OutboundOrder create() { // POST /outbound-orders 没有页面字段，单号和状态都在这里生成
        OutboundOrderEntity entity = new OutboundOrderEntity(); // 准备 outbound_order 的一行，此时还没有 id
        entity.setOrderNo("PENDING"); // 先占位。主键还没出来，单号暂时不能写成 OUT-主键
        entity.setStatus(OutboundOrder.DRAFT); // 新建就是草稿，写进 status 列
        entity.setCreatedAt(LocalDateTime.now()); // 创建时间不来自页面，写进 created_at 列
        orderMapper.insert(entity); // 插入后，MySQL 把自增 id 写回 entity.getId()。返回的 int 是 1
        entity.setOrderNo("OUT-" + entity.getId()); // 单号用主键，例如 id 是 1 就是 OUT-1
        orderMapper.updateOrderNo(entity); // 把这一行的 order_no 从 PENDING 改成 OUT-主键
        return toDomain(entity); // 表的一行转成返回给页面的 OutboundOrder
    }

    public OutboundOrder addLine(Long orderId, String idempotencyKey, String skuCode, String locationCode, int quantity) {
        // orderId 来自路径 /outbound-orders/{id}/lines，idempotencyKey 来自请求头，另外三个值来自 JSON
        OutboundOrderEntity order = requireDraft(orderId); // 只有草稿能加行，不是草稿直接 400
        if (switches.submitOnce() && idempotencyKey != null && !idempotencyKey.isBlank()) { // submit-once 是 off 时不看这个头
            String idemKey = "idem:outbound-line:" + idempotencyKey; // 例如 idem:outbound-line:line-001
            // SET idemKey 单号 NX PX 一天。第一次写得进去是 true；同一个键再来是 false
            if (!redis.setIfAbsent(idemKey, order.getOrderNo(), IDEMPOTENCY_TTL_MS)) {
                log.info("Idempotency-Key {} 已经处理过，不再插入", idempotencyKey);
                return toDomain(order); // 把这张单原样返回，行数不变
            }
            forgetKeyOnRollback(idemKey); // 这次插入失败回滚了，就删掉这个键，让重发能再试
        }
        SkuEntity sku = skuMapper.getByCode(skuCode); // SKU-001 这个编码换成 sku 表的那一行
        if (sku == null) { // 编码在 sku 表里不存在
            throw new BusinessException("SKU_NOT_FOUND", "货品不存在：" + skuCode); // 400，不再插入行
        }
        WarehouseLocationEntity location = locationMapper.getByCode(locationCode); // A-01-01 换成库位那一行
        if (location == null) { // 库位编码不存在
            throw new BusinessException("LOCATION_NOT_FOUND", "库位不存在：" + locationCode);
        }
        OutboundOrderLineEntity line = new OutboundOrderLineEntity(); // 准备 outbound_order_line 的一行
        line.setOrderId(order.getId()); // 写进 order_id：属于这张出库单
        line.setSkuId(sku.getId()); // 写进 sku_id：货品主键，不写 SKU-001 这个字符串
        line.setLocationId(location.getId()); // 写进 location_id：库位主键，不写 A-01-01
        line.setQuantity(quantity); // 写进 quantity：页面传来的数量
        lineMapper.insert(line); // 插入后，行主键写回 line.getId()。返回的 int 是 1
        return toDomain(order); // 把这张单和它的全部行一起返回
    }

    // POST /outbound-orders/{id}/complete，路径上只有主键。
    // 这个方法自己不开事务：sku-location 要先拿锁，再开事务读数量。
    // MySQL 默认可重复读，事务里第一次查询就定下了能看到的数据，锁拿得再早，事务开得比锁早也会读到旧数。
    @Transactional(propagation = Propagation.SUPPORTS)
    public OutboundOrder complete(Long orderId) {
        if (!switches.lockBeforeRead()) { // stock-lock 是 after-check：直接开事务，锁在 deduct 里判断完才拿
            return transactionTemplate.execute(status -> deduct(orderId));
        }
        OutboundOrderEntity order = orderMapper.getById(orderId); // 还没开事务，只为拿到单号和行
        if (order == null) {
            throw new BusinessException("OUTBOUND_NOT_FOUND", "出库单不存在：" + orderId);
        }
        Set<String> lockKeys = new TreeSet<>(); // 排好序再加锁。两张单都按同一顺序拿，不会互相等死
        for (OutboundOrderLineEntity line : lineMapper.listByOrderId(orderId)) {
            lockKeys.add(StockLock.key(line.getSkuId(), line.getLocationId())); // lock:inv:货品主键:库位主键
        }
        Map<String, String> owners = new LinkedHashMap<>(); // 锁的键 → 加锁时写进去的标记
        try {
            for (String lockKey : lockKeys) {
                owners.put(lockKey, stockLock.acquire(lockKey, order.getOrderNo())); // 别人拿着就等，最多 10 秒
            }
            return transactionTemplate.execute(status -> deduct(orderId)); // 拿到锁才开事务；返回时已经提交
        } finally {
            owners.forEach(stockLock::release); // 提交或回滚之后才放，下一张单读到的是提交后的数
        }
    }

    private OutboundOrder deduct(Long orderId) { // 在 transactionTemplate 开的事务里执行
        OutboundOrderEntity order = requireDraft(orderId); // 已完成的单不能再完成一次
        List<OutboundOrderLineEntity> lines = lineMapper.listByOrderId(order.getId()); // 这张单上加过的行
        if (lines.isEmpty()) { // 没有行就没有数量可减
            throw new BusinessException("OUTBOUND_LINE_REQUIRED", "出库单还没有行");
        }
        for (OutboundOrderLineEntity line : lines) { // 每一行减自己那个格子
            String lockKey = StockLock.key(line.getSkuId(), line.getLocationId()); // lock:inv:货品主键:库位主键
            // 用行上记下的两个主键，找到 inventory 里这种货在这个格子上的那一行
            InventoryEntity inventory = inventoryMapper.getBySkuAndLocation(line.getSkuId(), line.getLocationId());
            if (inventory == null) { // 这个格子上从来没入过这种货
                throw new BusinessException("INVENTORY_NOT_FOUND", "这个格子上没有库存");
            }
            int current = inventory.getQuantity(); // 读到的剩余，来自 inventory.quantity
            log.info("读到数量 {}", current); // 两次请求同时进来时，日志里会有两句读到同一个数
            pauseAfterRead(); // 课堂复现用。默认 0 毫秒，不停
            int next = current - line.getQuantity(); // 减去这一行的数量。剩余不够时这里会变成负数
            if (next < 0) { // 剩余不够就停下，不把负数写进去
                throw new BusinessException("INSUFFICIENT_STOCK", "INSUFFICIENT_STOCK：库存不足，剩余 " + current);
            }
            String owner = null; // after-check 这一侧在这里才拿锁
            if (!switches.lockBeforeRead()) { // stock-lock 是 after-check：上面读到的数可能已经旧了，锁只包住下面的写
                owner = stockLock.acquire(lockKey, order.getOrderNo());
            }
            int updated; // 改到了几行。1 是改到了，0 是没改到
            try {
                if (switches.optimisticLock()) { // optimistic-lock 是 on：带上读到的 version 去改
                    // 只有 version 还等于刚才读到的那一版，才改得到这一行
                    updated = inventoryMapper.updateQuantityIfVersion(inventory.getId(), next, inventory.getVersion());
                    if (updated == 0) { // 别人在我读完之后已经改过这一行，version 对不上
                        throw new BusinessException("STOCK_CONFLICT", "STOCK_CONFLICT：库存已被别人改过");
                    }
                } else { // optimistic-lock 是 off：不管别人改没改过，直接按 id 写成 next
                    updated = inventoryMapper.updateQuantity(inventory.getId(), next);
                }
            } finally {
                if (owner != null) { // after-check：写完马上放。这时事务还没提交，别人读到的仍是旧数
                    stockLock.release(lockKey, owner);
                }
            }
            log.info("把数量改成 {}，影响行数 {}", next, updated);
            if (switches.cacheEvict()) { // cache-evict 是 off 时缓存不动，查询还会拿到旧数量
                inventoryCache.evictAfterCommit(line.getSkuId(), line.getLocationId()); // 提交后 DEL inv:货品:库位
            }
        }
        order.setStatus(OutboundOrder.COMPLETED); // 数量减完，把状态改成已完成
        orderMapper.updateStatus(order); // 同一个事务里写 outbound_order.status
        return toDomain(orderMapper.getById(order.getId())); // 再查一次，返回的 status 是 COMPLETED
    }

    public OutboundOrder updateLine(Long orderId, Long lineId, int quantity) {
        // PATCH /outbound-orders/{id}/lines/{lineId}：两个主键来自路径，quantity 来自 JSON
        OutboundOrderEntity order = orderMapper.getById(orderId); // 先找出库单
        if (order == null) { // 路径上的单号不存在
            throw new BusinessException("OUTBOUND_NOT_FOUND", "出库单不存在：" + orderId);
        }
        // 不是 DRAFT 就停在这里：已完成的单，库存已经按原数量减过，不能再改行
        if (!OutboundOrder.DRAFT.equals(order.getStatus())) {
            throw new BusinessException("ORDER_NOT_EDITABLE", "ORDER_NOT_EDITABLE：只有草稿可以改数量：" + order.getOrderNo());
        }
        OutboundOrderLineEntity line = lineMapper.getById(lineId); // 按行主键找这一行
        if (line == null || !order.getId().equals(line.getOrderId())) { // 行不存在，或者不属于这张单
            throw new BusinessException("OUTBOUND_LINE_NOT_FOUND", "出库行不存在：" + lineId);
        }
        lineMapper.updateQuantity(lineId, quantity); // 只改 outbound_order_line.quantity，不动 inventory
        return toDomain(order); // 返回这张单，行上已经是新数量
    }

    private void forgetKeyOnRollback(String idemKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) { // 提交、回滚之后都会调用
                if (status != STATUS_COMMITTED) { // 没提交成功，这一行其实没加上
                    redis.delete(idemKey);
                }
            }
        });
    }

    private void pauseAfterRead() {
        long pause = switches.outboundReadPauseMs(); // 来自 app.lesson.outbound-read-pause-ms
        if (pause <= 0) { // 默认 0，不停
            return;
        }
        try {
            Thread.sleep(pause); // 停在读完数量、还没改之前。另一次请求这时进来，读到的还是同一个数
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 保留中断标记，交给外层处理
        }
    }

    private OutboundOrderEntity requireDraft(Long orderId) {
        OutboundOrderEntity order = orderMapper.getById(orderId); // #{id} 就是路径上的主键
        if (order == null) { // 查不到这张单
            throw new BusinessException("OUTBOUND_NOT_FOUND", "出库单不存在：" + orderId);
        }
        if (!OutboundOrder.DRAFT.equals(order.getStatus())) { // 已完成的单不能再加行，也不能再完成
            throw new BusinessException("OUTBOUND_NOT_DRAFT", "只有草稿可以继续：" + order.getOrderNo());
        }
        return order; // 是草稿，交回给调用的方法
    }

    private OutboundOrder toDomain(OutboundOrderEntity entity) { // 表的一行变成页面看到的 JSON
        OutboundOrder order = new OutboundOrder();
        order.setId(entity.getId()); // 主键
        order.setOrderNo(entity.getOrderNo()); // OUT-主键
        order.setStatus(entity.getStatus()); // DRAFT 或 COMPLETED
        order.setCreatedAt(entity.getCreatedAt());
        for (OutboundOrderLineEntity lineEntity : lineMapper.listByOrderId(entity.getId())) { // 这张单的每一行
            OutboundOrderLine line = new OutboundOrderLine();
            line.setId(lineEntity.getId()); // 行主键，改数量时路径上要用
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

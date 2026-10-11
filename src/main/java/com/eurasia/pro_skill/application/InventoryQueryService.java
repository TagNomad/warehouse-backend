package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.entity.InventoryEntity;
import com.eurasia.pro_skill.mapper.InventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// 查库存。先读缓存，没有再读库。库才是数量的来源。
@Service
@RequiredArgsConstructor
public class InventoryQueryService {

    private final InventoryMapper inventoryMapper;
    private final LessonRedis redis;
    private final LessonSwitches switches;

    public int quantity(Long skuId, Long locationId) {
        String key = "inv:" + skuId + ":" + locationId; // 一个格子一个键
        String cached = redis.get(key); // 命中就直接把缓存里的数字交回去
        if (cached != null) {
            return Integer.parseInt(cached);
        }
        InventoryEntity row = inventoryMapper.getBySkuAndLocation(skuId, locationId); // 未命中才查库
        int quantity = row == null ? 0 : row.getQuantity();
        redis.set(key, Integer.toString(quantity)); // 查到的数写进缓存
        return quantity;
    }

    public List<InventoryEntity> list(Integer page, Integer size) {
        if (!switches.inventoryPage()) { // full：不接页码，一次把表读完
            return inventoryMapper.listAll();
        }
        int pageNo = page == null || page < 1 ? 1 : page; // 页码从 1 开始
        int pageSize = size == null || size < 1 ? 20 : size;
        int offset = (pageNo - 1) * pageSize; // 跳过前面几页
        return inventoryMapper.listPage(offset, pageSize);
    }
}

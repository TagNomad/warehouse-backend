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

    private final InventoryMapper inventoryMapper; // 读 inventory 表
    private final InventoryCache inventoryCache; // 读写 Redis 里的 inv:货品:库位
    private final LessonSwitches switches;

    public int quantity(Long skuId, Long locationId) { // 两个主键来自 GET /inventories?skuId=&locationId=
        Integer cached = inventoryCache.get(skuId, locationId); // GET inv:货品:库位
        if (cached != null) { // 命中：不查库，直接把缓存里的数交回去。缓存没删，这里就一直是旧数
            return cached;
        }
        InventoryEntity row = inventoryMapper.getBySkuAndLocation(skuId, locationId); // 未命中才查 inventory
        int quantity = row == null ? 0 : row.getQuantity(); // 这个格子没有这种货就是 0
        inventoryCache.put(skuId, locationId, quantity); // SET inv:货品:库位 数量，下一次直接命中
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

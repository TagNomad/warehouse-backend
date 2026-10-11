package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.StockFlowEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

// 流水的 SQL。#{bizNo} 等取自实体的同名属性。
@Mapper
public interface StockFlowMapper {

    @Insert("INSERT INTO stock_flow (biz_no, sku_id, location_id, quantity, created_at) " +
            "VALUES (#{bizNo}, #{skuId}, #{locationId}, #{quantity}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id") // 把自增主键写回 entity.id
    int insert(StockFlowEntity entity); // 成功是 1，不是主键
}

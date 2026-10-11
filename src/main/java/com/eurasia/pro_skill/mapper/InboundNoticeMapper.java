package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.InboundNoticeEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

// 到货通知的 SQL。#{msgId} 等取自实体的同名属性。
@Mapper
public interface InboundNoticeMapper {

    // 六个值都取自 entity 的同名属性。id 不在列清单里，由 MySQL 自增。
    @Insert("INSERT INTO inbound_notice (msg_id, biz_no, sku_id, location_id, quantity, created_at) " +
            "VALUES (#{msgId}, #{bizNo}, #{skuId}, #{locationId}, #{quantity}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id") // 把自增主键写回 entity.id
    int insert(InboundNoticeEntity entity); // 成功是 1，不是主键
}

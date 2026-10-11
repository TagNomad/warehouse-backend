package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.InboundOrderEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InboundOrderMapper {

    // 先插入草稿。单号此时用占位，插入后改成 IN-主键。
    @Insert("INSERT INTO inbound_order (order_no, status, created_at) VALUES (#{orderNo}, #{status}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id") // 把自增主键写回 entity.id
    int insert(InboundOrderEntity entity);

    // #{orderNo} 取实体的单号，#{id} 取主键。
    @Update("UPDATE inbound_order SET order_no = #{orderNo} WHERE id = #{id}")
    int updateOrderNo(InboundOrderEntity entity);

    // 只改状态。完成时应写成 COMPLETED。
    @Update("UPDATE inbound_order SET status = #{status} WHERE id = #{id}")
    int updateStatus(InboundOrderEntity entity);

    @Select("SELECT * FROM inbound_order WHERE id = #{id}")
    InboundOrderEntity getById(Long id);
}

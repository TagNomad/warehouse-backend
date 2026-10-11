package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.OutboundOrderEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

// 出库单的 SQL。#{属性名} 取的是参数对象上的同名属性。
@Mapper
public interface OutboundOrderMapper {

    // 插入草稿。列清单里没有 id，id 由 MySQL 自增。
    @Insert("INSERT INTO outbound_order (order_no, status, created_at) VALUES (#{orderNo}, #{status}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id") // 插入后把自增主键写进 entity.id
    int insert(OutboundOrderEntity entity); // 返回写下了几行。成功是 1，不是主键

    // #{orderNo} 是 OUT-主键，#{id} 是刚生成的主键。把占位的 PENDING 改掉。
    @Update("UPDATE outbound_order SET order_no = #{orderNo} WHERE id = #{id}")
    int updateOrderNo(OutboundOrderEntity entity); // 改到这一行就返回 1

    // 完成时只改状态。#{status} 要写成 COMPLETED，#{id} 是这张单的主键。
    @Update("UPDATE outbound_order SET status = #{status} WHERE id = #{id}")
    int updateStatus(OutboundOrderEntity entity);

    // #{id} 是调用时传入的主键。查不到返回 null。
    @Select("SELECT * FROM outbound_order WHERE id = #{id}")
    OutboundOrderEntity getById(Long id);
}

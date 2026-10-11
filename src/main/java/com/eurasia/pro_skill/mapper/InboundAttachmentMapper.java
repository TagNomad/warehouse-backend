package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.InboundAttachmentEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

// 入库附件的 SQL。#{orderId} 等取自实体的同名属性。
@Mapper
public interface InboundAttachmentMapper {

    // 三个值都取自 entity 的同名属性。id 不在列清单里，由 MySQL 自增。
    @Insert("INSERT INTO inbound_attachment (order_id, file_name, file_path) VALUES (#{orderId}, #{fileName}, #{filePath})")
    @Options(useGeneratedKeys = true, keyProperty = "id") // 把自增主键写回 entity.id
    int insert(InboundAttachmentEntity entity); // 成功是 1，不是主键

    // 下载时按附件主键找回 file_path。没有这一行返回 null
    @Select("SELECT * FROM inbound_attachment WHERE id = #{id}")
    InboundAttachmentEntity getById(Long id);
}

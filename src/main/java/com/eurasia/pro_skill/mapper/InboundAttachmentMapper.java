package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.InboundAttachmentEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InboundAttachmentMapper {

    // #{orderId}、#{fileName}、#{filePath} 取自实体同名属性。id 不传。
    @Insert("INSERT INTO inbound_attachment (order_id, file_name, file_path) VALUES (#{orderId}, #{fileName}, #{filePath})")
    @Options(useGeneratedKeys = true, keyProperty = "id") // 把自增主键写回 entity.id
    int insert(InboundAttachmentEntity entity); // 成功是 1

    @Select("SELECT * FROM inbound_attachment WHERE id = #{id}")
    InboundAttachmentEntity getById(Long id);
}

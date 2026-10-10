package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.SkuEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SkuMapper {

    @Insert("INSERT INTO sku (code, name, unit, created_at) VALUES (#{code}, #{name}, #{unit}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SkuEntity entity);

    @Select("SELECT * FROM sku ORDER BY id")
    List<SkuEntity> listAll();
}

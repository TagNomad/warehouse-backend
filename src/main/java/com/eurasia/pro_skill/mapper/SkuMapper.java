package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.SkuEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// SQL 只写在这一层。#{字段名} 取的是 SkuEntity 上的同名属性。
@Mapper // 让 MyBatis 为这个接口生成实现
public interface SkuMapper {

    /**
     * 按编码查已有的一行。
     * #{code} 是调用时传入的编码字符串，不是写死的 SKU-001。
     * 查不到返回 null。LIMIT 1 表示只要一行。
     */
    @Select("SELECT * FROM sku WHERE code = #{code} LIMIT 1")
    SkuEntity getByCode(String code);

    /**
     * 插入一行货品。
     * <p>
     * INSERT 括号里只写了 code、name、unit、created_at，没有 id。
     * id 是表上的自增主键，插入时不传，由 MySQL 自己生成。
     * <p>
     * #{code} 不是写死的字，而是取出参数 entity 里的 code。
     * #{name}、#{unit}、#{createdAt} 同样取出 entity 里的同名属性。
     * createdAt 对应列 created_at。
     * <p>
     * useGeneratedKeys = true：插入完成后，去读 MySQL 刚生成的那个主键。
     * keyProperty = "id"：把读到的主键放进 entity 的 id 字段，也就是 entity.setId(...)。
     * 所以 insert 返回之后，entity.getId() 才有值。
     * <p>
     * 方法返回的 int 是这次写下了几行。成功插入一行就是 1，不是主键。
     */
    @Insert("INSERT INTO sku (code, name, unit, created_at) VALUES (#{code}, #{name}, #{unit}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SkuEntity entity);

    @Select("SELECT * FROM sku WHERE id = #{id}")
    SkuEntity getById(Long id);

    @Select("SELECT * FROM sku ORDER BY id") // 查出全部货品，按主键从小到大
    List<SkuEntity> listAll(); // GET /skus 使用
}

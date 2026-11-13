package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.InboundOrderEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

// SQL 只写在这一层。#{属性名} 取的是参数对象上的同名属性。
@Mapper // 让 MyBatis 为这个接口生成实现
public interface InboundOrderMapper {

    // 插入一张草稿。括号里没有 id，id 是自增主键，插入时不传。
    // #{orderNo} 取出 entity 的 orderNo。#{status}、#{createdAt} 同样取出同名属性。
    @Insert("INSERT INTO inbound_order (order_no, status, created_at) VALUES (#{orderNo}, #{status}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id") // 插入后把 MySQL 生成的主键写进 entity.setId
    int insert(InboundOrderEntity entity); // 返回值是写下了几行。成功是 1，不是主键

    // 插入时单号先占着 PENDING。拿到主键后改成 IN-主键。
    // #{orderNo} 是新单号，#{id} 是刚生成的主键。
    @Update("UPDATE inbound_order SET order_no = #{orderNo} WHERE id = #{id}")
    int updateOrderNo(InboundOrderEntity entity); // 改到这一行就返回 1

    // 完成时只改状态。#{status} 是要写成的状态，#{id} 是这张单的主键。
    @Update("UPDATE inbound_order SET status = #{status} WHERE id = #{id}")
    int updateStatus(InboundOrderEntity entity); // 改到这一行就返回 1

    // 数据库原子比较并更新：同一张单只有仍为 DRAFT 的那一次能改到 1 行。
    // InnoDB 对同一行的 UPDATE 串行执行，等待者会在前者提交后重新检查 status，返回 0。
    @Update("UPDATE inbound_order SET status = 'COMPLETED' WHERE id = #{id} AND status = 'DRAFT'")
    int completeIfDraft(Long id);

    // #{id} 是调用时传入的主键，不是写死的 1。查不到返回 null。
    @Select("SELECT * FROM inbound_order WHERE id = #{id}")
    InboundOrderEntity getById(Long id);
}

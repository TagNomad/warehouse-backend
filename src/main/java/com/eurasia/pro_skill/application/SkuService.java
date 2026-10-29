package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.converter.SkuConverter;
import com.eurasia.pro_skill.domain.Sku;
import com.eurasia.pro_skill.entity.SkuEntity;
import com.eurasia.pro_skill.mapper.SkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// 货品业务。创建时先查编码有没有用过，再把请求里的三个字段补上时间，交给 Mapper。
@Service // 注册成 Spring Bean，Controller 才能注入
@RequiredArgsConstructor // 为下面两个 final 字段生成构造方法
@Transactional // 这个类里的方法默认开事务，插入失败会回滚
public class SkuService {

    private final SkuMapper skuMapper; // 执行 SQL
    private final SkuConverter skuConverter; // 表的一行转回 Sku

    public Sku create(String code, String name, String unit) { // 三个参数来自 Controller，此时还没有 id
        SkuEntity existing = skuMapper.getByCode(code); // 按编码查表里有没有这一行，没有是 null
        if (existing != null) { // 查到了，说明这个编码已经占用
            // 停在这里，不再 insert。页面收到 400，message 里有 SKU_CODE_DUPLICATED
            throw new BusinessException(
                    "SKU_CODE_DUPLICATED",
                    "SKU_CODE_DUPLICATED：货品编码已存在：" + code);
        }
        SkuEntity entity = new SkuEntity(); // 准备要写入表的那一行
        entity.setCode(code); // 编码放进这一行
        entity.setName(name); // 名称放进这一行
        entity.setUnit(unit); // 单位放进这一行
        entity.setCreatedAt(LocalDateTime.now()); // 创建时间不来自页面，在这里生成
        skuMapper.insert(entity); // 插入后，数据库把自增 id 写回 entity.getId()
        return skuConverter.toDomain(entity); // 带 id 的一行变回 Sku，返回给 Controller
    }

    @Transactional(readOnly = true) // 只查询，不需要写事务
    public List<Sku> list() {
        // 每一行实体都转成 Sku，页面拿到的是业务对象列表
        return skuMapper.listAll().stream().map(skuConverter::toDomain).toList();
    }
}

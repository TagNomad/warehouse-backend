package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.converter.SkuConverter;
import com.eurasia.pro_skill.domain.Sku;
import com.eurasia.pro_skill.entity.SkuEntity;
import com.eurasia.pro_skill.mapper.SkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SkuService {

    private final SkuMapper skuMapper;
    private final SkuConverter skuConverter;

    /**
     * 课堂缺陷：插入前不查 code。相同编码可以落成两行，入库和出库无法知道指的是哪一条。
     * 学生修复：插入前按 code 查询，已存在则抛出 BusinessException，错误码 SKU_CODE_DUPLICATED。
     * 同时还要另写一条 Flyway 迁移，把 code 改成唯一索引。只改这里挡不住直接写库。
     */
    public Sku create(Sku input) {
        SkuEntity entity = new SkuEntity();
        entity.setCode(input.getCode());
        entity.setName(input.getName());
        entity.setUnit(input.getUnit());
        entity.setCreatedAt(LocalDateTime.now());
        skuMapper.insert(entity);
        return skuConverter.toDomain(entity);
    }

    @Transactional(readOnly = true)
    public List<Sku> list() {
        return skuMapper.listAll().stream().map(skuConverter::toDomain).toList();
    }
}

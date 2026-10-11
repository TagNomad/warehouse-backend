-- 货品表：一种货一行，记下编码、名称、单位。
CREATE TABLE sku (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键', -- 自增主键，插入时不用传
    code VARCHAR(50) NOT NULL COMMENT '货品编码', -- 这一版没有唯一约束，相同编码可以有多行
    name VARCHAR(100) NOT NULL COMMENT '货品名称',
    unit VARCHAR(20) NOT NULL COMMENT '单位',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' -- 不传时用数据库当前时间
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货品'; -- InnoDB 支持事务，utf8mb4 能存中文

-- 课堂一开始就能查到的两行，不是页面创建的。
INSERT INTO sku (code, name, unit) VALUES
('SKU-001', '轴承', '个'), -- 第一种货
('SKU-002', '内六角螺栓', '盒'); -- 第二种货

-- 货品表。
-- 课堂缺陷：code 只有普通索引，没有唯一索引。相同编码可以插入两行。
-- 学生修复时另写一条迁移，把 idx_code 换成唯一索引，不要改这条已经执行过的脚本。
CREATE TABLE IF NOT EXISTS sku (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(50) NOT NULL COMMENT '货品编码',
    name VARCHAR(100) NOT NULL COMMENT '货品名称',
    unit VARCHAR(20) NOT NULL COMMENT '单位',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货品';

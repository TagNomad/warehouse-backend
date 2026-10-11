-- 每个货品在每个库位一行。version 先写上，乐观锁以后再用这一列。
CREATE TABLE inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    sku_id BIGINT NOT NULL COMMENT '货品',
    location_id BIGINT NOT NULL COMMENT '库位',
    quantity INT NOT NULL DEFAULT 0 COMMENT '这个格子上的数量',
    version INT NOT NULL DEFAULT 0 COMMENT '每次改数量加一',
    UNIQUE KEY uk_sku_location (sku_id, location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='按货品和库位记账';

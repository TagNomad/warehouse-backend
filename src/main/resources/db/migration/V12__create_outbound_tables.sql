-- 出库单。完成前是 DRAFT，完成后应是 COMPLETED。
CREATE TABLE outbound_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_no VARCHAR(50) NOT NULL COMMENT '单号',
    status VARCHAR(20) NOT NULL COMMENT 'DRAFT 或 COMPLETED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_outbound_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单';

-- 出库单的一行：哪种货，从哪个库位出，多少数量。
CREATE TABLE outbound_order_line (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_id BIGINT NOT NULL COMMENT '出库单',
    sku_id BIGINT NOT NULL COMMENT '货品',
    location_id BIGINT NOT NULL COMMENT '库位',
    quantity INT NOT NULL COMMENT '数量',
    KEY idx_outbound_line_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单行';

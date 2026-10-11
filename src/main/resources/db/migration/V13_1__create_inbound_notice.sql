-- 入库到货通知。消费者每处理一条 Stream 消息写一行，同一条消息只应有一行。
CREATE TABLE inbound_notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    msg_id VARCHAR(64) NOT NULL COMMENT 'Stream 消息编号',
    biz_no VARCHAR(50) NOT NULL COMMENT '入库单号',
    sku_id BIGINT NOT NULL COMMENT '货品',
    location_id BIGINT NOT NULL COMMENT '库位',
    quantity INT NOT NULL COMMENT '数量',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '写入时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库到货通知';

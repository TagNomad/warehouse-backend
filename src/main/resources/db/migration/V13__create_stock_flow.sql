-- 库存流水。入库完成之后应留下一条，并且和入库单一起提交。
CREATE TABLE stock_flow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    biz_no VARCHAR(50) NOT NULL COMMENT '业务单号',
    sku_id BIGINT NOT NULL COMMENT '货品',
    location_id BIGINT NOT NULL COMMENT '库位',
    quantity INT NOT NULL COMMENT '数量',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '写入时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水';

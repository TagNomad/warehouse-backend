-- FR-01 仓库与库位管理：库区表
CREATE TABLE IF NOT EXISTS warehouse_zone (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    warehouse_id BIGINT       NOT NULL COMMENT '所属仓库 ID',
    code         VARCHAR(50)  NOT NULL COMMENT '库区编码（同仓库内唯一）',
    name         VARCHAR(100) NOT NULL COMMENT '库区名称',
    type         VARCHAR(50)  NOT NULL COMMENT '库区类型：STORAGE/PICKING',
    status       VARCHAR(20)  DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_warehouse_zone_code (warehouse_id, code),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_zone_type (type),
    KEY idx_zone_status (status),
    CONSTRAINT fk_zone_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库区表';

-- FR-01 仓库与库位管理：库位表
CREATE TABLE IF NOT EXISTS warehouse_location (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    zone_id    BIGINT       NOT NULL COMMENT '所属库区 ID',
    code       VARCHAR(50)  NOT NULL COMMENT '库位编码（同库区内唯一）',
    name       VARCHAR(100) COMMENT '库位名称',
    spec       VARCHAR(100) COMMENT '规格（如 1.2m×1m×2m）',
    status     VARCHAR(20)  DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE/DISABLED/OCCUPIED',
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_zone_location_code (zone_id, code),
    KEY idx_zone_id (zone_id),
    KEY idx_location_status (status),
    CONSTRAINT fk_location_zone FOREIGN KEY (zone_id) REFERENCES warehouse_zone (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库位表';

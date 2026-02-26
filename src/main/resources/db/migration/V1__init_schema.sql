-- V1: 初始化用户表
CREATE TABLE IF NOT EXISTS `tb_user`
(
    `id`           BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`     VARCHAR(64)      NOT NULL                COMMENT '登录账号',
    `password`     VARCHAR(128)     NOT NULL                COMMENT '密码（bcrypt 哈希）',
    `display_name` VARCHAR(64)      NOT NULL                COMMENT '显示名称',
    `role`         VARCHAR(32)      NOT NULL                COMMENT '角色：ADMIN / WAREHOUSE_MANAGER / OPERATOR / BUSINESS',
    `email`        VARCHAR(128)     NULL                    COMMENT '邮箱',
    `phone`        VARCHAR(20)      NULL                    COMMENT '手机号',
    `status`       TINYINT UNSIGNED NOT NULL DEFAULT 1      COMMENT '状态：1-启用 0-禁用',
    `created_at`   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '用户表';

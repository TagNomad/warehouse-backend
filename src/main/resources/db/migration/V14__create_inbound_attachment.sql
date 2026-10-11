-- 入库单附件。缺陷路径存本机绝对路径，修好之后存对象 key。
CREATE TABLE inbound_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_id BIGINT NOT NULL COMMENT '入库单',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '本机路径或对象 key'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库附件';

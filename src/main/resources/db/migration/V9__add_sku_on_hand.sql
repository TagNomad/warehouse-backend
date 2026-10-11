-- 数量先记在货品这一行上，不按库位分开。
ALTER TABLE sku ADD COLUMN on_hand INT NOT NULL DEFAULT 0 COMMENT '账面数量合计';

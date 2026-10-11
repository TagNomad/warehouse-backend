-- 给货品编码加上唯一约束。V7 已经执行过，所以写在新文件里。
-- 表里同一个 code 只能有一行。已有重复编码时，这条语句会失败。
ALTER TABLE sku ADD UNIQUE KEY uk_code (code);

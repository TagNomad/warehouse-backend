-- FR-01 演示数据：为 WH-001 原料仓（若存在）添加库区与库位
INSERT INTO warehouse_zone (warehouse_id, code, name, type, status)
SELECT w.id, 'Z-STORAGE-01', 'A区存储区', 'STORAGE', 'ENABLED'
FROM warehouse w
WHERE w.code = 'WH-001'
  AND NOT EXISTS (
    SELECT 1 FROM warehouse_zone z WHERE z.warehouse_id = w.id AND z.code = 'Z-STORAGE-01'
);

INSERT INTO warehouse_zone (warehouse_id, code, name, type, status)
SELECT w.id, 'Z-PICK-01', 'B区拣货区', 'PICKING', 'ENABLED'
FROM warehouse w
WHERE w.code = 'WH-001'
  AND NOT EXISTS (
    SELECT 1 FROM warehouse_zone z WHERE z.warehouse_id = w.id AND z.code = 'Z-PICK-01'
);

INSERT INTO warehouse_location (zone_id, code, name, spec, status)
SELECT z.id, 'A-01-01', 'A区1排1位', '1.2m×1m×2m', 'AVAILABLE'
FROM warehouse_zone z
         JOIN warehouse w ON z.warehouse_id = w.id
WHERE w.code = 'WH-001' AND z.code = 'Z-STORAGE-01'
  AND NOT EXISTS (
    SELECT 1 FROM warehouse_location l WHERE l.zone_id = z.id AND l.code = 'A-01-01'
);

INSERT INTO warehouse_location (zone_id, code, name, spec, status)
SELECT z.id, 'A-01-02', 'A区1排2位', '1.2m×1m×2m', 'AVAILABLE'
FROM warehouse_zone z
         JOIN warehouse w ON z.warehouse_id = w.id
WHERE w.code = 'WH-001' AND z.code = 'Z-STORAGE-01'
  AND NOT EXISTS (
    SELECT 1 FROM warehouse_location l WHERE l.zone_id = z.id AND l.code = 'A-01-02'
);

INSERT INTO warehouse_location (zone_id, code, name, spec, status)
SELECT z.id, 'B-01-01', 'B区拣货位1', '0.8m×0.8m×1.5m', 'AVAILABLE'
FROM warehouse_zone z
         JOIN warehouse w ON z.warehouse_id = w.id
WHERE w.code = 'WH-001' AND z.code = 'Z-PICK-01'
  AND NOT EXISTS (
    SELECT 1 FROM warehouse_location l WHERE l.zone_id = z.id AND l.code = 'B-01-01'
);

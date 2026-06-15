# WMS 模块作业教案 — FR-01 仓库与库位管理

完整实现 WMS 需求书 **「基础数据管理 → 仓库与库位管理」**（FR-01），含 **仓库 + 库区 + 库位** 三层 CRUD、库位树形视图、库位详情。

---

## 学习路径

| 顺序 | 文档 | 报告章节 |
| --- | --- | --- |
| 1 | [01-需求分析](./01-需求分析.md) | 第 1 章 |
| 2 | [02-系统设计](./02-系统设计.md) | 第 2 章 |
| 3 | [03-作业一评阅总结](./03-作业一评阅总结.md) | 讲评 / 改进参考（教师用） |
| 4 | [04-测试与质量保障](./04-测试与质量保障.md) | 第 4 章（测试） |
| 4b | [06-Kimi生成单元测试操作指南](./06-Kimi生成单元测试操作指南.md) | 用 Kimi 对话写单元测试 |
| 4c | [07-Postman性能测试零基础教程](./07-Postman性能测试零基础教程.md) | 第 4 章 4.3.1 性能测试（Postman 一步一步） |
| 5 | 学校模版第 3 章 | 实现说明 |

**对照材料**：

- 《WMS 详细需求规格说明书》— FR-01 需求来源
- 《功能模块设计与实现报告-模版.doc》— Word 格式

---

## 数据库（Flyway）

| 版本 | 脚本 | 内容 |
| --- | --- | --- |
| V3 | `V3__create_warehouse_table.sql` | 仓库表 |
| V4 | `V4__create_warehouse_zone_and_location.sql` | 库区表 + 库位表 |
| V5 | `V5__seed_warehouse_zone_location.sql` | 为 **已存在的** WH-001 插入演示库区/库位（无 WH-001 时 0 行） |
| V6 | `V6__seed_warehouse_wh001_demo.sql` | **完整演示数据**：WH-001 仓库 + 2 库区 + 3 库位（幂等） |

---

## 接口一览（Swagger 三组）

| 分组 | 路径前缀 | 说明 |
| --- | --- | --- |
| FR-01 仓库管理 | `/warehouses` | 含 `GET /{id}/tree` 库位视图 |
| FR-01 库区管理 | `/warehouses/{id}/zones`、`/warehouse-zones` | 库区 CRUD |
| FR-01 库位管理 | `/warehouse-zones/{id}/locations`、`/warehouse-locations` | 含 `/detail` 库位详情 |

测试文件：[api-contract/http/warehouse.http](../api-contract/http/warehouse.http)（功能测试）

性能测试（Postman）：[api-contract/postman/FR-01-性能测试.postman_collection.json](../api-contract/postman/FR-01-性能测试.postman_collection.json) — 见 [07-Postman性能测试零基础教程](./07-Postman性能测试零基础教程.md)

自动化测试（JUnit + Mockito + MockMvc）：

| 类型 | 路径 |
| --- | --- |
| 单元测试 | `src/test/java/.../application/Warehouse*Test.java` |
| 集成测试 | `src/test/java/.../controller/WarehouseControllerIntegrationTest.java` |
| 覆盖率报告 | 执行 `mvn test jacoco:report` → `target/site/jacoco/index.html` |
| 测试教案 | [04-测试与质量保障](./04-测试与质量保障.md) |

---

## draw.io 图源（FR-01 完整版）

| 文件 | 报告图号 |
| --- | --- |
| 图1-1-仓库维护流程.drawio | 图1-1 完整业务流程 |
| 图1-2-仓库用例图.drawio | 图1-2 完整用例图 |
| 图1-3-仓库实体类图.drawio | 图1-3 | 3 个持久化实体类 + 1:N 关联（与表1-5 前三行一致） |
| 图2-0-仓库状态图.drawio | 2.2 状态（建议） |
| 图2-1-创建库位时序图.drawio | 图2-1 |
| 图2-1b-库位视图时序图.drawio | 图2-1b（建议） |
| 图2-2-仓库ER图.drawio | 图2-2 E-R |

详见 [diagrams/README.md](./diagrams/README.md)

---

## 相关文档

- [用户管理 CRUD 全流程](../用户管理CRUD全流程.md)

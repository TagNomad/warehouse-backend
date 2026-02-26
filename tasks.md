创建 Spring Boot 项目骨架（Spring Initializr）
- [x] 在 Initializr 阶段引入 Lombok
- [x] 引入依赖（Spring Web、MyBatis Spring Boot Starter、Flyway、MySQL Driver、Validation、Swagger、Actuator）
- [x] 配置 `application.yml`（端口、MySQL 数据源、日志级别）
- [x] 配置 MyBatis 参数（mapper 扫描、`map-underscore-to-camel-case`、日志输出、分页策略如需）
- [x] 配置 Flyway 参数（locations、baseline 等）
- [x] 配置 Swagger/OpenAPI（生成接口文档；可选：启动后自动打开 Swagger 页面）
- [x] 配置 Actuator（健康检查端点最小可用）


### 数据库迁移（Flyway）
- [x] 创建迁移目录 `db/migration`
- [x] 编写迁移脚本 `V1__init_schema.sql`（建表/索引/约束）
- [x] 编写迁移脚本 `V2__seed_data.sql`（可选：演示数据）
- [x] 启动应用并验证迁移执行成功（生成 `flyway_schema_history`）
- [x] 按查询场景设计索引（避免过度索引；优先复合索引覆盖查询）


### 接口调用（Rest Client）
- [x] 创建 Rest Client 请求文件（`.http`）覆盖 CRUD 接口
- [ ] 补充 `.http` 中的错误场景用例（校验失败/不存在/约束冲突）
- [x] 在 `.http` 中抽取变量（如 `@baseUrl`、token 等）


### 鉴权与权限（RBAC）

- [x] 选型并接入鉴权方案（Spring Security + JWT）
- [x] 实现登录与 token 签发（最小可用：用户名密码）

- [x] SecurityConfiguration进行合适的配置，比如/auth/* swagger 等需要放开
- [x] 增加登录接口 /auth/login
- [x] GET/users 添加 token

---
name: layered-architecture-guard
description: 守卫并审查 Spring Boot 小团队分层架构（MVCS + Application + Domain），发现违规并给出可执行重构方案。
compatibility: opencode
license: MIT
metadata:
  scope: project
  style: guard
---

# 何时使用此技能

当用户在做以下事情时启用：
- 新增/修改 Controller、Application(Service)、Mapper、Entity、Domain、Converter
- 代码审查（PR/变更 review）
- 架构走样排查（Controller 变胖、Entity 外泄、Domain 带技术注解等）
- 需要把讨论结论转为 Obsidian 任务清单

# 输入期望

用户可能提供：
- 代码片段 / 文件路径 / 包结构
- 需求描述（要新增一个接口、一个用例、一个领域对象）
- 报错信息（如 MapStruct/Lombok）

如信息不足，也必须先给出“最小可行审查”，并明确列出缺失信息点（不强制追问）。

# 核心规则（必须遵守）

## 1. 依赖方向（硬约束）
- Controller → Application → Mapper
- Domain 不依赖 Controller、Mapper、Spring、DB 技术
- Entity 不出 application 层

## 2. 各层职责（硬约束）

### entity/XxxxEntity
- 1:1 映射 DB 表（可包含 password/createdAt 等原始字段）
- 只在 Mapper ↔ Application(XxxxService) 流通
- 禁止：Entity 直接返回给前端/序列化输出

### domain/Xxxx
- 业务核心对象，命名无后缀（User/Order/Product）
- 不含技术注解：无 @Table、无 @JsonIgnore、无 DB/序列化相关注解
- 允许（鼓励）包含纯业务行为：状态流转、不变量校验
- 禁止：注入 Spring Bean、调用外部依赖、持久化/网络/IO

### controller/request/XxxxRequest
- 只用于 Controller 入参，携带校验注解（@Valid/@NotBlank 等）
- 禁止：Request 直接传入 Application(XxxxService)（Controller 负责转换）
- 审查时重点看：Service 方法签名、import 列表、Converter 是否出现 `controller.request` 引用

### controller/response/XxxxResponse（按需）
- 默认返回 Domain；仅在必要时创建 Response：
  - 屏蔽字段
  - 多 Domain 聚合视图
  - 展示格式转换（枚举→文案、金额单位换算）
  - 同一 Domain 多种展示形态
  - 对外 API 需要契约隔离

### application/XxxxService
- 用例编排（Use Case），业务逻辑唯一入口
- 事务边界由 application 管理（@Transactional 只出现在 XxxxService）
- 可调用多个 Mapper
- 禁止：HTTP 相关逻辑（不感知 HttpServletRequest/Header/状态码）
- 禁止：DB 技术细节（SQL 属于 Mapper；字段映射属于 Converter）
- 入参：基本类型或 Domain；出参：Domain 或 PageResponse
- 重点反例：`update(Long id, XxxxUpdateRequest request)`、`import controller.request.*`

### converter/XxxxConverter（MapStruct）
- 仅做 Entity ↔ Domain 转换
- @Mapper(componentModel = SPRING)
- 命名为 XxxxConverter，避免与 MyBatis @Mapper 混淆
- 若同时承担 Request ↔ Domain 转换，通常意味着 Controller 边界正在向内泄漏，需要优先审查 Service 是否已依赖 Request
- 若出现 `XxxxConverter` Bean 注入失败、生成实现异常、另一台机器正常但本机异常，先排查 Maven / IDE / 注解处理器构建链路，再判断是否为分层设计问题

### common
- ApiError：{ code, message, path, timestamp }
- PageResponse<T>：{ current, pageSize, total, records } 与 AntDV 对齐

## 3. 异常约定（硬约束）
- application(XxxxService) 抛业务异常（BusinessException / NotFoundException 等）
- Controller 不捕获业务异常
- GlobalExceptionHandler 统一转换为 ApiError
- 禁止：用 null 表示“未找到/异常”

## 4. 禁止清单（命中即判违规）
- Controller 直接调用 Mapper
- XxxxService 返回 XxxxResponse
- XxxxService import 或接收 `controller.request.XxxxRequest`
- XxxxConverter 同时承担 `Request ↔ Domain` 与 `Entity ↔ Domain`
- Domain 上出现 @Table/@JsonIgnore 等技术注解
- @Transactional 写在 Controller 上
- Entity 被直接序列化返回前端
- 用 DTO/VO 命名替代 Request/Response/Domain

<!-- Evolution: 2026-03-13 | source: warehouse-service-refactor | skill: layered-architecture-guard -->

## Pattern Added (2026-03-13)

**Pattern**: Review `Service` imports and converter responsibilities together when checking layering

**Source**: Warehouse service review/refactor

**Confidence**: 0.95

### Updated Checklist
- [ ] `application/*Service` 不 import `controller.request` 或 `controller.response`
- [ ] Controller 在进入 Service 前完成 Request -> Domain/primitive 转换
- [ ] `converter/*Converter` 只负责 Entity <-> Domain 映射
- [ ] 若 Converter Bean 异常，先排除 MapStruct / IDE autobuild / Maven 产物污染，再做架构定责
- [ ] 分页接口同时检查 `current/pageSize` 的上下限保护

# 审查输出格式（必须按此输出）

当用户提供代码或描述变更，请输出：

## ✅ 架构符合性结论
- 结论：通过 / 不通过（若不通过，指出最关键 1-3 条违规）

## 🔎 发现的问题（按严重度）
- [S1] ...（破坏分层/安全/事务边界）
- [S2] ...（可维护性/演进风险）
- [S3] ...（风格一致性）

## 🛠 建议的重构路径（可执行步骤）
1) ...
2) ...
3) ...

## 🧩 参考代码/目录调整（尽量给片段）
- ...

## ✅ 生成 Obsidian 任务（如用户需要或问题较多）
使用 Tasks 格式：
- [ ] <动作> #<projectKey> #backend|#frontend 📅 YYYY-MM-DD（如未给日期则省略）
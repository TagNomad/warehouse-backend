---
name: layered-architecture-generator
description: 按“分层架构约定”生成 Spring Boot 模块骨架（Controller/Request/Response/Application Service/Mapper/Entity/Domain/Converter/Common）。
compatibility: opencode
license: MIT
metadata:
  scope: project
  style: generator
---

# 何时使用此技能

当用户要求：
- “生成一个 X 模块/CRUD”
- “新增一个接口（创建/查询/分页/详情）”
- “按分层架构写一套骨架/目录结构/类模板”
- “把某需求拆成 Controller + Service + Mapper + Domain + Entity + Converter”

# 输入（从用户自然语言中提取）

必须识别以下信息（缺失也要用默认值继续生成）：
- 模块名（如 User / WorkOrder / SourceOrder）
- 用例列表（如 create/update/delete/get/listPage）
- 是否需要 Response（默认不需要，除非命中触发场景）
- 是否分页（默认 listPage 使用 PageResponse）
- 关键字段（可选；缺失则生成 TODO 占位）

# 生成约束（必须遵守）

## 包结构（固定）
com.eurasia.pro_skill/
- controller/
  - request/
  - response/（按需）
- application/
- mapper/
- entity/
- domain/
- converter/
- common/

## 默认策略
- 默认 Controller 返回 Domain
- 默认不创建 XxxxResponse
- Request 只用于 Controller，Controller 转成 Domain/参数后调用 application
- application 只返回 Domain 或 PageResponse
- Entity 不出 application
- Domain 不含任何技术注解
- Converter 只负责 Entity ↔ Domain

## 分页策略（固定）
- Controller 用 @RequestParam 接收 current/pageSize
- 返回 PageResponse<T>，字段：current/pageSize/total/records

## 异常策略（固定）
- application 抛 BusinessException/NotFoundException
- 不返回 null 表示未找到
- 不在 Controller 捕获业务异常

# 输出内容（必须包含）

当用户要求生成模块时，输出必须包含以下四块：

## 1) 目录与类清单（树状）
- 列出将创建的文件路径（相对 src/main/java）

## 2) 类模板（最小可编译）
至少生成：
- Controller（含示例接口）
- Request（含 @Valid 示例）
- Application Service（含 @Transactional 示例）
- Mapper（MyBatis 接口占位）
- Entity（字段占位）
- Domain（包含一个示例业务方法/不变量校验占位）
- Converter（MapStruct）
- PageResponse/ApiError（若项目还没有则给模板）

## 3) 数据流说明（1 段）
说明：Request → Domain/参数 → Entity → DB → Entity → Domain → Response/Domain

## 4) 后续 TODO 列表（可直接变任务）
- 需要补字段、补 SQL、补索引、补单测等

# 生成风格要求

- 代码命名遵守：XxxxRequest / XxxxService / XxxxMapper / XxxxEntity / XxxxConverter / Domain= Xxxx
- 不生成 DTO/VO
- 不让 Controller 直接调 Mapper
- 不让 Service 返回 Response
- 必要时提示：Response 仅按需创建（聚合视图/格式转换/契约隔离等）

# 示例触发（用户说法示例）

- “生成 WorkOrder 的分页查询与详情接口”
- “新增 User 创建接口，注意校验”
- “把来源单 SourceOrder 按分层架构生成一套骨架”
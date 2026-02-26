# 分层架构约定

> 适用范围：中小型项目 / 1-2 人团队 / MVCS 架构风格

---

## 包结构

```
com.eurasia.pro_skill/
├── controller/
│   ├── request/        XxxxRequest      HTTP 入参
│   └── response/       XxxxResponse     HTTP 出参（按需建，见下文）
├── service/            业务逻辑
├── mapper/             数据访问（MyBatis）
├── entity/             XxxxEntity       DB PO，1:1 映射表结构
├── domain/             Xxxx             业务核心对象
├── converter/          XxxxConverter    Entity ↔ Domain 转换（MapStruct）
└── common/             ApiResult 等公共结构
```

---

## 各层职责与命名

### `entity/XxxxEntity` — DB 持久化对象

- 1:1 映射数据库表（含 `password`、`createdAt` 等所有原始字段）
- 只在 `Mapper ↔ Service` 之间流通，**不出 Service 层**
- 命名带 `Entity` 后缀，明确表达"这是有 DB 技术绑定的实现细节"

### `domain/Xxxx` — 业务核心对象

- 业务语言的第一公民，是 Service 层的通用语言
- **不含技术实现细节**（无 DB 注解、无序列化注解）
- `password` 等敏感/技术字段不出现在 Domain 中，天然安全
- 命名最简洁，不加任何后缀（`User`、`Order`、`Product`）

### `controller/request/XxxxRequest` — HTTP 入参

- 对应一个具体的 HTTP 接口入参，放在 `controller/request/` 包下
- 携带 `@NotBlank`、`@Valid` 等校验注解
- 只在 Controller 层使用，**不传入 Service**（Controller 负责转换）

### `controller/response/XxxxResponse` — HTTP 出参（按需建）

**默认直接返回 Domain 对象**，无需建 `XxxxResponse`。

仅在以下情况才创建：

| 触发场景 | 说明 |
|---|---|
| 需要屏蔽字段 | Domain 不应为展示目的删减字段（但 Domain 天然不含敏感字段，通常不触发） |
| 多 Domain 聚合视图 | 如 `OrderDetailResponse` = `Order` + `User` + `List<Item>` |
| 字段格式转换 | 枚举 → 描述文字、金额单位换算等展示逻辑 |
| 同一 Domain 多种展示形态 | `UserSummaryResponse`（列表）vs `UserDetailResponse`（详情） |
| 对外 API / 接口契约需稳定隔离 | Domain 变动不应直接影响接口 |

> **原则：Domain 设计得干净，Response 自然就少。**

### `converter/XxxxConverter` — 对象转换

- 使用 MapStruct，命名为 `XxxxConverter`（避免与 MyBatis 的 `@Mapper` 混淆）
- 主要职责：**`XxxxEntity ↔ Xxxx（Domain）`**
- `componentModel = SPRING`，可直接注入

```java
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserConverter {
    User toDomain(UserEntity entity);
    UserEntity toEntity(User domain);
}
```

### `common/` — 全局公共结构

- `ApiError`：统一错误响应体 `{ code, message, path, timestamp }`
- `PageResponse<T>`：分页响应包装，字段与 AntDV `<a-pagination>` 对齐

```json
{ "current": 1, "pageSize": 10, "total": 42, "records": [...] }
```

分页请求参数直接用 `@RequestParam` 接收，无需单独建 `PageRequest` 类：

```java
@GetMapping
public PageResponse<User> listPage(
        @RequestParam(defaultValue = "1")  int current,
        @RequestParam(defaultValue = "10") int pageSize
) { ... }
```

---

## 依赖流向

```
Controller  →  Service  →  Mapper
    ↓              ↓           ↓
 Request       Domain /    Entity
 Response      Entity
```

**核心原则：内层不依赖外层。**

- `Service` 不返回 `XxxxResponse`（那是 Controller 的职责）
- `Entity` 不出 `Service` 层
- `Domain` 对 `Controller`、`Mapper` 一无所知

---

## 数据流示例

```
HTTP Request
    → XxxxRequest（Controller 接收）
    → Domain / 参数（Controller 调 Service）
    → XxxxEntity（Service 经 Converter 转换后调 Mapper）
    → DB

DB
    → XxxxEntity（Mapper 返回）
    → Domain（Service 经 Converter 转换）
    → Domain 或 XxxxResponse（Controller 返回，视需要而定）
    → HTTP Response
```

---

## 不引入的概念

| 概念 | 原因 |
|---|---|
| `XxxxDTO` | 语义过于宽泛，用 `Request` / `Response` / `Domain` 替代 |
| `XxxxVO` | 统一归入 `Response`（按需建），消除命名歧义 |
| `XxxxCommand` / `XxxxQuery` | 来自 CQRS，当前规模引入是过度设计 |

---

## MapStruct + Lombok 配置要点

两者共存时，必须在 `maven-compiler-plugin` 的 `annotationProcessorPaths` 中：

1. **Lombok 排在 MapStruct 之前**（否则 MapStruct 编译时看不到 Lombok 生成的 getter/setter）
2. 引入 `lombok-mapstruct-binding` 强制处理器顺序

详见 [pom.xml](pom.xml) 中的 `maven-compiler-plugin` 配置。

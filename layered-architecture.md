# 分层架构约定

> 适用范围：中小型项目 / 1-2 人团队 / MVCS 架构风格

---

## 包结构

```
com.eurasia.pro_skill/
├── controller/
│   ├── request/        XxxxRequest      HTTP 入参
│   └── response/       XxxxResponse     HTTP 出参（按需建，见下文）
├── application/        XxxxService      业务逻辑（Use Case 编排）
├── mapper/             数据访问（MyBatis）
├── entity/             XxxxEntity       DB PO，1:1 映射表结构
├── domain/             Xxxx             业务核心对象
├── converter/          XxxxConverter    Entity ↔ Domain 转换（MapStruct）
└── common/             ApiError、PageResponse 等公共结构
```

---

## 各层职责与命名

### `entity/XxxxEntity` — DB 持久化对象

- 1:1 映射数据库表（含 `password`、`createdAt` 等所有原始字段）
- 只在 `Mapper ↔ XxxxService` 之间流通，**不出 `application/` 层**
- 命名带 `Entity` 后缀，明确表达"这是有 DB 技术绑定的实现细节"

### `domain/Xxxx` — 业务核心对象

- 业务语言的第一公民，是 `application/` 层的通用语言
- **不含技术实现细节**（无 DB 注解、无序列化注解）
- `password` 等敏感/技术字段不出现在 Domain 中，天然安全
- 命名最简洁，不加任何后缀（`User`、`Order`、`Product`）

### `controller/request/XxxxRequest` — HTTP 入参

- 对应一个具体的 HTTP 接口入参，放在 `controller/request/` 包下
- 携带 `@NotBlank`、`@Valid` 等校验注解
- 只在 Controller 层使用，**不传入 `XxxxService`**（Controller 负责转换）

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

### `application/XxxxService` — 应用服务层

- **编排业务流程**（Use Case），是业务逻辑的唯一入口
- 管理**事务边界**（`@Transactional`），不在 Controller 或 Mapper 上加事务
- 可调用多个 Mapper，将多步 DB 操作组合为一个原子业务操作
- **不做 HTTP 相关逻辑**（不感知 `HttpServletRequest`、Header、状态码）
- **不做 DB 技术细节操作**（SQL 归 Mapper，字段映射归 Converter）
- 入参为基本类型或 Domain 对象，出参为 Domain 对象或 `PageResponse`

### `domain/Xxxx` 中的业务行为

Domain 对象可以（也应该）包含**纯业务行为**，例如状态流转、不变量校验：

```java
public class Order {
    private OrderStatus status;

    public void cancel() {
        if (status == OrderStatus.SHIPPED) {
            throw new BusinessException("已发货订单不可取消");
        }
        this.status = OrderStatus.CANCELLED;
    }
}
```

Domain **不包含**任何外部依赖：无 `@Table`、无数据库注解、无 `@JsonIgnore`、不注入 Spring Bean。

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

## 异常约定

- **`XxxxService`** 抛出业务异常（`BusinessException` 等自定义运行时异常），携带明确的错误信息
- **Controller 层不捕获业务异常**，一律由 `GlobalExceptionHandler` 统一转换为标准错误响应
- **不用 `null` 表示异常情况**，找不到资源时抛 `NotFoundException`，而非返回 `null` 让调用方猜

```
XxxxService 抛 BusinessException
    → GlobalExceptionHandler 捕获
    → 返回 ApiError { code, message, path, timestamp }
```

---

## 依赖流向

```
Controller  →  Application  →  Mapper
    ↓               ↓              ↓
 Request        Domain /        Entity
 Response       Entity
```

**核心原则：内层不依赖外层。**

- `XxxxService` 不返回 `XxxxResponse`（那是 Controller 的职责）
- `Entity` 不出 `application/` 层
- `Domain` 对 `Controller`、`Mapper` 一无所知

---

## 数据流示例

```
HTTP Request
    → XxxxRequest（Controller 接收）
    → Domain / 参数（Controller 调 XxxxService）
    → XxxxEntity（XxxxService 经 Converter 转换后调 Mapper）
    → DB

DB
    → XxxxEntity（Mapper 返回）
    → Domain（XxxxService 经 Converter 转换）
    → Domain 或 XxxxResponse（Controller 返回，视需要而定）
    → HTTP Response
```

---

## 反例（禁止）

| 反例 | 违反的原则 | 正确做法 |
|---|---|---|
| `UserEntity` 直接序列化返回前端 | Entity 不出 `application/` 层，且含敏感字段 | 返回 `User`（Domain）或 `XxxxResponse` |
| `XxxxService` 方法返回 `XxxxResponse` | 内层不依赖外层，Service 不关心展示 | 返回 Domain，Controller 做转换 |
| `Domain` 上加 `@Table`、`@JsonIgnore` 等注解 | Domain 不含技术实现细节 | DB 注解放 Entity，序列化控制放 Response |
| `Controller` 直接调 `Mapper` | 跳过 Application 层，事务与业务逻辑无处安放 | 所有业务入口经过 `XxxxService` |
| 用 `null` 表示"未找到" | 语义不清晰，调用方被迫防御性判空 | 抛 `NotFoundException`，全局异常处理器处理 |
| `@Transactional` 写在 `Controller` 上 | 事务边界属于业务，不应感知 HTTP 层 | 事务注解只出现在 `XxxxService` 方法上 |

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

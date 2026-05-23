# 用户管理增删改查 — 上课教案（讲师用）

> **配套材料**  
> - 学生动手：`docs/用户管理CRUD全流程.md`（Swagger 逐步操作）  
> - 源码参考：带中文注释的 `UserController` / `UserService` / `UserMapper` 等  
> - 建议课时：**2 学时（约 90～120 分钟）**，或拆成「讲 45min + 练 45min」两次课  

---

## 一、教学目标（下课学生应能做到）

| 目标 | 可检验方式 |
|------|------------|
| 说清一次请求经过哪几层 | 能画出 Controller → Service → Mapper → DB |
| 知道 CRUD 对应什么 HTTP 方法 | 能说出 GET/POST/PUT/DELETE 与用户五个接口的对应关系 |
| 会在 Swagger 登录并 Authorize | 能调通 `/users` 列表 |
| 独立完成「创建用户」链路 | Mapper 有 insert、Service 调 insert、Swagger 返回非 null 的 userId |
| 理解软删除 | 知道 DELETE 后库里还有行，只是 status=0 |
| 会用日志排查问题 | 能在控制台找到 `UserMapper.insert` 和 `Updates: 1` |

---

## 二、课前准备（讲师 checklist）

- [ ] 本机 `java -version` ≥ 21，`./mvnw spring-boot:run` 能启动  
- [ ] MySQL 有库 `pro_skill`，Flyway 已跑完（有 `admin` 等种子用户）  
- [ ] 浏览器打开 Swagger：http://localhost:8080/swagger-ui.html  
- [ ] 投影/共享屏幕准备好：IDE 项目结构 + Swagger + 可选 DBeaver  
- [ ] 确认学生仓库能拉代码，或发 ZIP  
- [ ] 演示账号：**admin / Admin@123**（与 `api-contract/http/user.http` 一致）  

**课堂策略建议：**

- **第一遍（你讲）**：用**已完成的项目**演示 Swagger 全流程，建立直觉。  
- **第二遍（学生做）**：按 `用户管理CRUD全流程.md` 从创建接口开始写（或只写 create + list，视课时压缩）。  
- 仓库完整代码作**答案**，不要第一节课就全开；可规定「练到 insert 再允许看 Service 答案」。

---

## 三、时间分配（120 分钟示例）

| 时段 | 时长 | 内容 | 形式 |
|------|------|------|------|
| 1 | 15 min | 背景 + 分层 + HTTP 与 CRUD 概念 | 讲授 |
| 2 | 10 min | 登录、JWT、Authorize 演示 | 演示 |
| 3 | 12 min | **查**：列表 + 详情（只讲思路，Swagger 演示） | 讲授+演示 |
| 4 | 20 min | **增**：创建用户（重点讲代码链路与 insert） | 讲授+演示 |
| 5 | 10 min | **改**：更新用户 | 讲授+演示 |
| 6 | 8 min | **删**：软删除 | 讲授+演示 |
| 7 | 35 min | 学生跟着做：登录 → 创建 → 列表验证 | 实操 |
| 8 | 10 min | 答疑、作业布置 | 互动 |

课时紧时：压缩「改、删」为演示 5 分钟，实操只做「增 + 查」。

---

## 四、第一部分：讲授知识点（先讲再做）

### 4.1 开场（3 分钟）— 这节课在做什么？

**话术参考：**

> 今天做后台的「用户管理」：管理员在网页或 Swagger 里能**查用户列表、看详情、新建账号、改资料、禁用账号**。  
> 前端发的是 **HTTP 请求**，我们写的是 **Spring Boot 接口**，数据在 **MySQL 表 tb_user** 里。  
> 五个能力对应英文 **CRUD**：Create 增、Read 查、Update 改、Delete 删。

**板书 / PPT 一行：**

```
用户管理 = 对 tb_user 表的增删改查 + 权限控制（只有 ADMIN 能操作）
```

---

### 4.2 分层架构（8 分钟）— 最重要的一张图

**话术参考：**

> 不要在一个类里写完 SQL 又写 URL，我们拆成四层，每层只干一件事。

**画图（建议照抄到白板）：**

```
  浏览器 / Swagger
        │  HTTP + JSON
        ▼
  ┌─────────────────┐
  │ UserController  │  接待员：接请求、返回 JSON
  └────────┬────────┘
           ▼
  ┌─────────────────┐
  │ UserService     │  经理：业务规则（查重、加密密码、分页）
  └────────┬────────┘
           ▼
  ┌─────────────────┐
  │ UserMapper      │  仓库管理员：只负责 SQL
  └────────┬────────┘
           ▼
      MySQL tb_user
```

**知识点清单（逐条点明）：**

| 名词 | 一句话 |
|------|--------|
| Controller | 定义地址，如 `GET /users`、`POST /users` |
| Service | 不写 SQL；管「用户名能不能重复」「密码要加密」 |
| Mapper | 接口 + `@Select` `@Insert`，MyBatis 帮你执行 SQL |
| Entity | 和表字段一致，**含 password** |
| Domain (User) | 给前端用的对象，**不含 password** |
| Request | POST/PUT 的 JSON 长什么样（如 CreateUserRequest） |

**强调一句（防学生以后写乱）：**

> Controller **禁止**直接调 Mapper；必须经过 Service。

---

### 4.3 HTTP 方法与五个接口（4 分钟）

**表格（建议投影）：**

| 业务 | 方法 | 路径 | Swagger 标题 |
|------|------|------|----------------|
| 分页列表 | GET | `/users` | 分页获取用户列表 |
| 详情 | GET | `/users/{id}` | 获取用户详情 |
| 创建 | POST | `/users` | 创建用户 |
| 更新 | PUT | `/users/{id}` | 更新用户 |
| 禁用 | DELETE | `/users/{id}` | 禁用用户 |

**知识点：**

- **GET**：查，参数在 URL（`?current=1`）或路径（`/users/5`）  
- **POST**：新建，数据在 **Request body** JSON  
- **PUT**：修改，id 在路径，新数据在 body  
- **DELETE**：本项目是**改 status**，不是真删行  

---

### 4.4 安全：为什么要先登录？（5 分钟）

**演示顺序（只演示，不让学生敲）：**

1. 不 Authorize 直接点 `GET /users` → **401**  
2. `POST /auth/login` 用 admin 登录 → 拿到 **token**  
3. 右上角 **Authorize** → `Bearer <token>`  
4. 再 `GET /users` → **200**

**话术参考：**

> token 像临时门禁卡；后面每个请求在 Header 里带 `Authorization: Bearer xxx`。  
> 用户管理还要求角色是 **ADMIN**，普通账号会 **403**。

**不必展开 JWT 原理**，可说：「登录成功服务器签发一串字符，过滤器会校验」。

---

### 4.5 两种「用户对象」为什么分开？（3 分钟）

**话术参考：**

> 数据库里一行有 **password**（加密后的乱码）。  
> 我们不能把密码原样返回给 Swagger/前端。  
> 所以：**Entity** 在 Service 和 Mapper 之间用；转成 **User（Domain）** 再给 Controller 返回。  
> 转换由 **UserConverter**（MapStruct）自动完成，编译时生成代码。

**可演示：** `GET /users/1` 的 JSON 里没有 password 字段。

---

## 五、第二部分：按接口讲解细节（演示 + 知识点）

> 每个接口建议结构：**业务含义 → Swagger 怎么点 → 代码在哪 → 易错点**  
> 演示时 IDE 打开对应方法，边指边讲。

---

### 5.1 查 — 分页列表 `GET /users`（10 分钟）

**业务：** 管理员翻页看用户，可按用户名、角色、状态筛选。

**Swagger 演示要点：**

- Try it out → current=1, pageSize=10 → Execute  
- 指给学生看：`total`、`records` 数组  

**代码指读（从上到下）：**

1. `UserController.listPage` — `@GetMapping`、`@RequestParam`  
2. `UserService.listPage` — **offset 公式**：`(current-1)*pageSize`  
3. `UserMapper.count` + `findPage` — 先数总数再取一页  

**知识点：**

- 分页：第 2 页、每页 10 条 → 跳过前 10 条（offset=10）  
- `PageResponse` 与前端 Ant Design 分页字段对齐  

**易错点（预告）：**

- 页码从 **1** 开始，不是 0  

---

### 5.2 查 — 详情 `GET /users/{id}`（5 分钟）

**业务：** 点某一条，看完整信息。

**Swagger：** id 填 1 → Execute。

**代码指读：**

- `@PathVariable Long id` — 路径里的 5 就是 id=5  
- `findById` 没有 → `BusinessException` → **400**（不是 500）  

**对比列表：** 列表是多条；详情是单条，SQL 带 `WHERE id=?`。

---

### 5.3 增 — 创建 `POST /users`（20 分钟，全课核心）

**业务：** 管理员新建账号，指定角色，密码要加密存储。

**Swagger 演示 JSON（现场改 username 防重复）：**

```json
{
  "username": "demo01",
  "password": "Password@123",
  "displayName": "演示用户",
  "role": "OPERATOR",
  "email": "demo01@example.com",
  "phone": "13800001111"
}
```

**成功标准（三条必须讲清）：**

1. 响应里 **userId 有数字**  
2. **GET /users** 能搜到 demo01  
3. 控制台有 **`UserMapper.insert`** 且 **`Updates: 1`**

**代码指读顺序（与实现顺序一致）：**

| 步骤 | 位置 | 讲什么 |
|------|------|--------|
| 1 | `CreateUserRequest` | JSON 字段、@NotBlank 校验失败→400 |
| 2 | `UserController.createUser` | @PostMapping、@RequestBody @Valid |
| 3 | `UserService.createUser` | findByUsername 查重 |
| 4 | 同上 | passwordEncoder.encode — **永不存明文** |
| 5 | 同上 | **userMapper.insert** — 漏了则「假成功」 |
| 6 | `UserMapper.insert` | @Insert SQL、@Options 回填 id |
| 7 | `CreateUserResponse` | 为什么创建单独一个 Response |

**板书 — 创建链路口诀：**

```
JSON → Request → Service 组装 Entity → 加密密码 → insert → 返回 id
```

**易错点（重点强调，结合上届学生问题）：**

| 现象 | 原因 |
|------|------|
| Swagger 成功，库里没有 | Mapper 写了 insert，Service 没调用 |
| userId 为 null | 没 insert 或没配 useGeneratedKeys |
| 400 用户名已存在 | 重复 username |
| 500 | 看控制台 Caused by，常见表不存在、Java 版本不对 |

**可选演示：** 故意注释掉 `insert` 一行，Execute 后看 userId 和数据库 — 再恢复。

---

### 5.4 改 — 更新 `PUT /users/{id}`（8 分钟）

**业务：** 改显示名、角色、手机等；密码可选。

**Swagger：** id=刚创建的 id，body 改 displayName。

**代码要点：**

- URL 的 id：改**谁**  
- Body：改成**什么**  
- `password` 为空 → Service 里**不更新**密码字段  

**对比创建：** 创建用 POST + insert；更新用 PUT + update SQL。

---

### 5.5 删 — 禁用 `DELETE /users/{id}`（8 分钟）

**业务：** 不用了 → 禁用登录，但数据保留。

**话术参考：**

> 我们的删除叫**软删除**：`UPDATE status=0`，不是 `DELETE FROM`。  
> 所以 Swagger 删成功后，数据库里**还能查到这一行**，status 变成 0。

**Swagger 演示后：**

- `GET /users/{id}` 看 status  
- 可选 DBeaver 打开表对比  

**列表仍能看到禁用用户**，除非传 `status=1` 筛选 — 可顺带讲筛选参数。

---

## 六、第三部分：学生跟着做（实操环节）

### 6.1 发布任务（说清楚终点）

**任务单（可投屏）：**

1. 启动项目，Swagger 登录并 Authorize  
2. 调通 `GET /users`  
3. **从零实现或跟文档完成 `POST /users`**（必做）  
4. 用列表/详情确认创建成功  
5.（进阶）完成 PUT、DELETE 其中一个  

**材料：** 发 `docs/用户管理CRUD全流程.md` 链接或仓库路径。

### 6.2 讲师巡视要点

| 学生卡在哪 | 你怎么帮 |
|------------|----------|
| 401 | 没 Authorize 或 token 过期，重新 login |
| 403 | 没用 admin 登录 |
| 启动失败 Java 17 | 提醒 JAVA_HOME 21+，见 README |
| 创建 200 无数据 | 看日志有没有 insert；看 Service 有没有调用 insert |
| 重复用户名 | 换 username |

### 6.3 收尾验收（可选 2 分钟）

请一位同学投屏：创建用户 → 列表出现 → 控制台指 INSERT 日志。

---

## 七、知识点小结（下课前 3 分钟口述）

1. **CRUD = 四种 HTTP 操作 + 五条用户接口**  
2. **分层：Controller 接客，Service 决策，Mapper 干活**  
3. **创建必走 insert，成功看 userId 和日志 Updates: 1**  
4. **删除是软删除，行还在**  
5. **500 看堆栈，400 多半是参数或业务校验**  
6. **Swagger 成功 ≠ 数据库一定有数据**  
7. **对外 JSON 一律用 *Response，Service 仍用 Domain User**

---

## 八、作业建议

| 层次 | 内容 |
|------|------|
| 必做 | 独立完成创建 + 列表；截图 Swagger 响应（含 userId）和一条 INSERT 日志 |
| 选做 | 完成更新、禁用；写 5 句话说明 Entity 和 User 区别 |
| 挑战 | 给列表默认只查 status=1（改 Service 默认参数或文档说明） |

---

## 九、常见问题 FAQ（讲师备用）

**Q：为什么不用 @Repository 用 @Mapper？**  
A：MyBatis 习惯叫 Mapper，和 JPA 的 Repository 类似，都是数据访问层。

**Q：为什么有 UserResponse，又有 CreateUserResponse？**  
A：`UserResponse` 是用户信息字段的「积木」；每个接口再用 *Response 包一层（如加 `message`、分页、userId），五个接口形状统一、Swagger 清晰。

**Q：能否在 Swagger 测注册接口代替创建？**  
A：注册是 `/auth/register`，角色默认不同；管理员建用户用 `POST /users` 可指定 role。

**Q：GitHub Desktop 报认证失败？**  
A：那是 Git 推送问题，和 Swagger 调接口无关；Gitee 用 SSH 或私人令牌。

---

## 十、与文档、代码的对照表（讲师备课用）

| 教案章节 | 学生文档章节 | 源码入口 |
|----------|--------------|----------|
| 4.2 分层 | §1 文件清单 | 项目包结构 |
| 4.4 登录 | §0.3 | AuthController |
| 5.1 列表 | §2 | UserController.listPage |
| 5.2 详情 | §3 | getById |
| 5.3 创建 | §4 | createUser 全链路 |
| 5.4 更新 | §5 | update |
| 5.5 禁用 | §6 | delete |

---

## 十一、压缩版大纲（45 分钟只讲不练）

1. 分层图（10min）  
2. 登录 + Authorize + GET 列表演示（10min）  
3. POST 创建代码走读 + insert 强调（15min）  
4. 软删除 + 易错点（5min）  
5. 布置跟文档实操作业（5min）  

---

*教案版本：与仓库 main 分支用户管理模块一致，讲师可根据班级进度删减「更新/删除」实操。*

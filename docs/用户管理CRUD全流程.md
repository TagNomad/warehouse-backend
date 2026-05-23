# 用户管理接口：从零实现 + Swagger 操作手册

> 面向零基础。假设 **Swagger 里还没有** 这 5 个用户接口，按下面顺序在代码里「做出来」，再在 Swagger 里「点出来」验证。  
> 仓库里若已有代码，可当作参考答案，课堂建议 **关掉答案自己敲一遍**，再对照 diff。

---

## 0. 上课前先准备好

### 0.1 启动后端

```bash
cd backend-server
java -version    # 必须是 21 或以上
./mvnw spring-boot:run
```

看到日志里有 **`Started ProSkillApplication`** 才算成功。

### 0.2 打开 Swagger

浏览器访问：**http://localhost:8080/swagger-ui.html**

左侧会看到分组，本教程主要用：

| 分组名 | 用途 |
|--------|------|
| **鉴权** | 先登录拿 token |
| **用户管理** | 本文 5 个 CRUD 接口 |

### 0.3 先登录，再点「Authorize」（非常重要）

用户管理接口都要 **ADMIN** 权限，必须先拿 token。

**Swagger 操作步骤：**

1. 展开左侧 **鉴权** → 点击 **`POST /auth/login`**
2. 点击右侧 **Try it out**
3. 在 **Request body** 里填入（演示账号，密码来自种子数据）：

```json
{
  "username": "admin",
  "password": "Admin@123"
}
```

4. 点击 **Execute**
5. 在 **Response body** 里复制 **`token`** 字段整段字符串（很长的一串）
6. 点 Swagger 页面 **右上角绿色 Authorize 按钮**
7. 在弹窗 Value 里输入：`Bearer ` + 刚复制的 token（注意 `Bearer` 后面有一个空格）  
   示例：`Bearer eyJhbGciOiJIUzI1NiIs...`
8. 点 **Authorize** → **Close**

之后调 **用户管理** 下的接口，都会自动带上这个头。

---

## 1. 从零做 5 个接口：要建哪些文件？

按 **从下往上** 的顺序写代码（先能存数据库，再写业务，最后暴露 HTTP）：

| 顺序 | 文件路径 | 作用 |
|------|----------|------|
| ① | `entity/UserEntity.java` | 和表 `tb_user` 字段对应 |
| ② | `domain/User.java` | 返回给前端的对象（无 password） |
| ③ | `converter/UserConverter.java` | Entity ↔ Domain 转换 |
| ④ | `mapper/UserMapper.java` | 写 SQL（MyBatis） |
| ⑤ | `application/UserService.java` | 业务逻辑 |
| ⑥ | `controller/request/CreateUserRequest.java` | 创建接口 JSON 入参 |
| ⑦ | `controller/request/UpdateUserRequest.java` | 更新接口 JSON 入参 |
| ⑧ | `controller/response/CreateUserResponse.java` | 创建接口专用返回（可选） |
| ⑨ | `controller/UserController.java` | 定义 URL，对接 Swagger |

表结构由 Flyway 脚本 `db/migration/V1__init_schema.sql` 建好，**不用手写建表 SQL**。

每做一个接口，通常是：**Mapper 加 SQL → Service 加方法 → Controller 加方法 → 重启 → Swagger 点 Execute**。

---

## 2. 接口一：分页列表 `GET /users`

### 2.1 在代码里怎么加（从 0）

**① UserMapper.java** 增加：

```java
@Select("<script>SELECT COUNT(*) FROM tb_user <where>...</where></script>")
long count(@Param("username") String username, ...);

@Select("<script>SELECT * FROM tb_user <where>...</where> ORDER BY id LIMIT #{pageSize} OFFSET #{offset}</script>")
List<UserEntity> findPage(...);
```

**② UserService.java** 增加 `listPage`：算 `offset` → 调 `count` + `findPage` → 转 `User` → 包进 `PageResponse`。

**③ UserController.java** 增加：

```java
@GetMapping
@PreAuthorize("hasRole('ADMIN')")
public PageResponse<User> listPage(
    @RequestParam(defaultValue = "1") int current,
    @RequestParam(defaultValue = "10") int pageSize,
    @RequestParam(required = false) String username,
    ...
) {
    return userService.listPage(current, pageSize, username, role, status);
}
```

保存后 **重启** `./mvnw spring-boot:run`。

### 2.2 在 Swagger 里怎么操作

| 步骤 | 操作 |
|------|------|
| 1 | 左侧 **用户管理** → **`GET /users`** 分页获取用户列表 |
| 2 | **Try it out** |
| 3 | 填参数（都可先留默认） |

| 参数名 | 填什么 | 说明 |
|--------|--------|------|
| current | `1` | 第几页 |
| pageSize | `10` | 每页条数 |
| username | 留空 或 `admin` | 模糊搜用户名 |
| role | 留空 或 `ADMIN` | 按角色筛 |
| status | 留空 或 `1` | `1` 启用，`0` 禁用 |

| 4 | **Execute** |

### 2.3 成功时 Response 长什么样

**Code: 200**，body 类似：

```json
{
  "current": 1,
  "pageSize": 10,
  "total": 4,
  "records": [
    {
      "id": 1,
      "username": "admin",
      "displayName": "系统管理员",
      "role": "ADMIN",
      "status": 1,
      ...
    }
  ]
}
```

**没有 password 字段** 是正常的。

### 2.4 失败时

| Code | 原因 | 处理 |
|------|------|------|
| 401 | 没 Authorize | 做第 0.3 节 |
| 403 | 不是 ADMIN | 用 admin 账号登录 |

---

## 3. 接口二：用户详情 `GET /users/{id}`

### 3.1 在代码里怎么加（从 0）

**① UserMapper**

```java
@Select("SELECT * FROM tb_user WHERE id = #{id}")
UserEntity findById(Long id);
```

**② UserService**

```java
public User getById(Long id) {
    UserEntity entity = userMapper.findById(id);
    if (entity == null) throw new BusinessException("USER_NOT_FOUND", "用户不存在：" + id);
    return userConverter.toDomain(entity);
}
```

**③ UserController**

```java
@GetMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public User getById(@PathVariable Long id) {
    return userService.getById(id);
}
```

重启项目。

### 3.2 在 Swagger 里怎么操作

| 步骤 | 操作 |
|------|------|
| 1 | **用户管理** → **`GET /users/{id}`** 获取用户详情 |
| 2 | **Try it out** |
| 3 | **id** 填 `1`（先由列表接口看有哪些 id） |
| 4 | **Execute** |

### 3.3 成功响应示例

```json
{
  "id": 1,
  "username": "admin",
  "displayName": "系统管理员",
  "role": "ADMIN",
  "email": "admin@example.com",
  "status": 1,
  ...
}
```

### 3.4 故意测失败

id 填 `99999` → **400**，message 含「用户不存在」。

---

## 4. 接口三：创建用户 `POST /users`（重点）

### 4.1 在代码里怎么加（从 0）

**① 新建 `CreateUserRequest.java`**

字段：`username`、`password`、`displayName`、`role`、`email`、`phone`，加 `@NotBlank` 等校验。

**② UserMapper**

```java
@Insert("INSERT INTO tb_user (username, password, display_name, ...) VALUES (...)")
@Options(useGeneratedKeys = true, keyProperty = "id")
int insert(UserEntity entity);
```

**③ UserService.createUser**

1. `findByUsername` 查重  
2. `new UserEntity()` 并 set 各字段  
3. `passwordEncoder.encode(password)`  
4. **`userMapper.insert(newUser)`** ← 漏这句会「Swagger 成功但库里没数据」  
5. `return newUser.getId()`

**④ 新建 `CreateUserResponse.java`**（userId、username、role、message）

**⑤ UserController**

```java
@PostMapping
@PreAuthorize("hasRole('ADMIN')")
public CreateUserResponse createUser(@RequestBody @Valid CreateUserRequest request) {
    Long userId = userService.createUser(...);
    return new CreateUserResponse(userId, request.getUsername(), request.getRole(), "创建成功");
}
```

重启项目。

### 4.2 在 Swagger 里怎么操作

| 步骤 | 操作 |
|------|------|
| 1 | **用户管理** → **`POST /users`** 创建用户 |
| 2 | **Try it out** |
| 3 | **Request body** 改成下面 JSON（用户名每次换一个，避免重复） |

```json
{
  "username": "student01",
  "password": "Password@123",
  "displayName": "学生01",
  "role": "OPERATOR",
  "email": "student01@example.com",
  "phone": "13800138001"
}
```

**role 只能填下面之一**（和数据库种子一致）：

- `ADMIN`
- `WAREHOUSE_MANAGER`
- `OPERATOR`
- `BUSINESS`

| 4 | **Execute** |

### 4.3 成功响应示例

```json
{
  "userId": 5,
  "username": "student01",
  "role": "OPERATOR",
  "message": "创建成功"
}
```

**检查点：**

- `userId` **不能是 null**
- 再调 **`GET /users`** 或 **`GET /users/5`** 能查到
- 控制台有 `UserMapper.insert` 和 `<== Updates: 1`

### 4.4 常见失败

| 现象 | 原因 |
|------|------|
| 400 用户名已存在 | 换 username 再试 |
| 400 校验失败 | 密码少于 6 位、用户名为空等 |
| 200 但 userId 为 null | Service 没调 insert |
| 200 但列表没有 | 没重启 / 看错数据库 |

---

## 5. 接口四：更新用户 `PUT /users/{id}`

### 5.1 在代码里怎么加（从 0）

**① 新建 `UpdateUserRequest.java`**

**② UserMapper**

```java
@Update("UPDATE tb_user SET display_name=#{displayName}, ... WHERE id=#{id}")
int update(UserEntity entity);
```

**③ UserService.update**

`findById` → 改字段 → 若 password 非空则加密 → `userMapper.update` → 返回 `User`。

**④ UserController**

```java
@PutMapping("/{id}")
public User updateUser(@PathVariable Long id, @RequestBody @Valid UpdateUserRequest request) {
    return userService.update(id, ...);
}
```

重启。

### 5.2 在 Swagger 里怎么操作

| 步骤 | 操作 |
|------|------|
| 1 | **`PUT /users/{id}`** 更新用户 |
| 2 | **Try it out** |
| 3 | **id** 填要改的用户，例如 `5` |
| 4 | **Request body** 示例 |

```json
{
  "displayName": "学生01-已改名",
  "role": "OPERATOR",
  "email": "newmail@example.com",
  "phone": "13800139999",
  "status": 1
}
```

不修改密码时：**不要写 password 字段**，或留空。

| 5 | **Execute** |

### 5.3 成功响应

返回更新后的 **User** 对象（无 password），`displayName` 等应为新值。

再用 **`GET /users/5`** 核对。

---

## 6. 接口五：禁用用户 `DELETE /users/{id}`

> 本项目的「删除」= **软删除**：只把 `status` 改成 `0`，**数据库行还在**。

### 6.1 在代码里怎么加（从 0）

**UserService.delete**

```java
entity.setStatus(0);
entity.setUpdatedAt(LocalDateTime.now());
userMapper.update(entity);   // 不是 DELETE SQL
```

**UserController**

```java
@DeleteMapping("/{id}")
public void deleteUser(@PathVariable Long id) {
    userService.delete(id);
}
```

重启。

### 6.2 在 Swagger 里怎么操作

| 步骤 | 操作 |
|------|------|
| 1 | **`DELETE /users/{id}`** 禁用用户 |
| 2 | **Try it out** |
| 3 | **id** 填 `5` |
| 4 | **Execute** |

### 6.3 成功时

**Code: 200**，响应体通常为空。

验证：

- **`GET /users/5`** → `status` 应为 `0`
- 数据库 `SELECT * FROM tb_user WHERE id=5` → 行还在，只是 `status=0`
- **`GET /users?status=1`** 列表里可能不再出现该用户

---

## 7. 五个接口在 Swagger 中的对照表（速查）

| # | Swagger 标题 | 方法 | 路径 | Body | 路径/Query 参数 |
|---|--------------|------|------|------|-----------------|
| 0 | 登录 | POST | `/auth/login` | username, password | 先执行，再 Authorize |
| 1 | 分页获取用户列表 | GET | `/users` | 无 | current, pageSize, username?, role?, status? |
| 2 | 获取用户详情 | GET | `/users/{id}` | 无 | id |
| 3 | 创建用户 | POST | `/users` | CreateUserRequest JSON | 无 |
| 4 | 更新用户 | PUT | `/users/{id}` | UpdateUserRequest JSON | id |
| 5 | 禁用用户 | DELETE | `/users/{id}` | 无 | id |

---

## 8. 推荐课堂练习顺序

1. 第 0 节：登录 + Authorize（全员先做）  
2. 第 4 节：创建用户（练 Mapper insert + Service 调用）  
3. 第 2 节：列表（验证能查到刚建的）  
4. 第 3 节：详情  
5. 第 5 节：更新  
6. 第 6 节：禁用（理解软删除）

---

## 9. 附录：Swagger 界面词对照

| 英文 | 含义 |
|------|------|
| Try it out | 进入可编辑、可发送状态 |
| Execute | 真正发 HTTP 请求 |
| Parameters | URL 查询参数或路径参数 |
| Request body | POST/PUT 的 JSON |
| Responses | 返回状态码和 JSON |
| Authorize | 设置 Bearer token |

---

## 10. 附录：也可用 HTTP 文件调试

项目里 `api-contract/http/user.http` 有相同请求示例，用 VS Code REST Client 插件可一键发送（需先登录把 token 填进变量）。

---

## 11. 对照源码位置

| 接口 | Controller 方法 | Service 方法 | Mapper 方法 |
|------|-----------------|--------------|-------------|
| 列表 | `listPage` | `listPage` | `count`, `findPage` |
| 详情 | `getById` | `getById` | `findById` |
| 创建 | `createUser` | `createUser` | `findByUsername`, `insert` |
| 更新 | `updateUser` | `update` | `findById`, `update` |
| 禁用 | `deleteUser` | `delete` | `findById`, `update` |

详细中文注释见各 Java 源文件。

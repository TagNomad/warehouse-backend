# 用户管理 CRUD 全流程（零基础教学版）

> **写给谁看**：完全没写过后端的同学。  
> **怎么学**：按章节顺序读；每做一个接口，先在代码里跟敲，再用 Swagger 点一遍。  
> **目标**：搞懂 5 个用户接口各自「干什么、代码怎么一层层写、Swagger 怎么测」。

---

## 一、先搞懂几个词

### 1.1 什么是 CRUD？

对用户这张表，最常见的 4 类操作叫 **CRUD**（英文首字母）：

| 字母 | 英文 | 中文 | 本项目的接口 |
|------|------|------|----------------|
| **C** | Create | 新增 | `POST /users` 创建用户 |
| **R** | Read | 查询 | `GET /users` 列表、`GET /users/{id}` 详情 |
| **U** | Update | 修改 | `PUT /users/{id}` 更新用户 |
| **D** | Delete | 删除 | `DELETE /users/{id}` 禁用用户（软删除） |

### 1.2 一次请求在程序里怎么走？

可以想成 **前台 → 经理 → 仓库管理员 → 数据库**：

```
浏览器 / Swagger
    ↓  HTTP（网址 + 方法 + JSON）
Controller（接待员：接请求、回 JSON）
    ↓
Service（经理：业务规则，比如「用户名不能重复」）
    ↓
Mapper（仓库员：只负责执行 SQL）
    ↓
MySQL 数据库表 tb_user
```

**为什么要分这么多层？**

- **Controller**：只管「接口长什么样」，不写 SQL。
- **Service**：写业务（查重、加密密码、算分页）。
- **Mapper**：只写 `SELECT / INSERT / UPDATE`。
- **Entity**：和数据库表字段一一对应（含 password）。
- **Domain**：给业务层用的「安全用户对象」（**没有 password**）。
- **Request / Response**：和前端 JSON 字段一一对应。

### 1.3 本教程 5 个接口一览

| 序号 | 方法 | 路径 | 作用 |
|------|------|------|------|
| 1 | GET | `/users` | 分页查用户列表 |
| 2 | GET | `/users/{id}` | 按 id 查一个用户 |
| 3 | POST | `/users` | 新建用户 |
| 4 | PUT | `/users/{id}` | 修改用户 |
| 5 | DELETE | `/users/{id}` | 禁用用户（status 改 0，不真删行） |

表 `tb_user` 由 Flyway 脚本 `db/migration/V1__init_schema.sql` 自动建好，**不用手写建表**。

---

## 二、上课前：启动项目 + Swagger 登录

### 2.1 启动后端

```bash
cd backend-server
java -version    # 需要 21 或以上
./mvnw spring-boot:run
```

日志里出现 **`Started ProSkillApplication`** 表示启动成功。

### 2.2 打开 Swagger

浏览器访问：**http://localhost:8080/swagger-ui.html**

左侧分组：

| 分组 | 用途 |
|------|------|
| **鉴权** | 登录拿 token |
| **用户管理** | 本文 5 个接口 |

### 2.3 先登录，再点 Authorize（必做）

用户管理接口要求 **ADMIN** 角色，必须先登录。

1. 展开 **鉴权** → **`POST /auth/login`** → **Try it out**
2. Request body 填：

```json
{
  "username": "admin",
  "password": "Admin@123"
}
```

3. **Execute**，在 Response 里复制 **`token`** 整段
4. 点页面右上角绿色 **Authorize**
5. Value 填：`Bearer ` + token（`Bearer` 后有一个空格）
6. **Authorize** → **Close**

之后调用户接口会自动带上登录凭证。

---

## 三、公共基础：第一次写用户功能前要认识的类

后面每个接口都会用到这些概念，**先读一遍**，做接口时遇到再回来查。

### 3.1 `UserEntity` —— 和数据库表一一对应

路径：`entity/UserEntity.java`

```java
@Data
public class UserEntity {
    private Long id;
    private String username;
    private String password;      // 存的是加密后的哈希，不是明文
    private String displayName;
    private String role;
    private String email;
    private String phone;
    private Integer status;       // 1=启用，0=禁用
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

| 写法 | 含义 |
|------|------|
| `@Data` | Lombok 自动生成 getter/setter，少写样板代码 |
| `displayName` | Java 用驼峰；表里列名是 `display_name`，由配置自动映射 |

**为什么要有 Entity？**  
MyBatis 查库时，一行记录 → 一个 `UserEntity` 对象。

---

### 3.2 `User`（Domain）—— 业务里用的「安全用户」

路径：`domain/User.java`

和 `UserEntity` 字段几乎一样，但 **没有 `password` 字段**。

**为什么多这一层？**  
防止密码一路传到 Controller，最后出现在 JSON 里。  
规则：**查库用 Entity，返回前端前转成 Domain / Response。**

---

### 3.3 `UserConverter` —— Entity 转 Domain

路径：`converter/UserConverter.java`

```java
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserConverter {
    User toDomain(UserEntity entity);
}
```

| 写法 | 含义 |
|------|------|
| `@Mapper` | MapStruct：编译时自动生成实现类，按同名属性拷贝 |
| `toDomain` | `UserEntity` → `User`，自动 **不拷贝** Domain 里没有的 `password` |

Service 里常见写法：`userConverter.toDomain(entity)`。

---

### 3.4 `UserResponse` —— 最终返回给前端的 JSON 结构

路径：`controller/response/UserResponse.java`

字段和 `User` 一致，专门给 **HTTP 响应** 用。  
Controller 不直接返回 `User`，而是 `UserResponse` 或包一层 `UserPageResponse` 等。

`UserResponseMapper.from(user)` 负责 **Domain → UserResponse** 字段拷贝。

---

## 四、接口 1：分页列表 `GET /users`

### 4.1 这个接口是干什么的？

管理员在后台「用户列表」页：一页显示 10 条，可以翻页，可以按用户名、角色、状态筛选。  
对应：**查（Read）**，而且是 **分页 + 条件查询**。

- **方法**：`GET`（查询，一般不传 Request body）
- **路径**：`/users`
- **谁都能访问吗？** 不能，要 `@PreAuthorize("hasRole('ADMIN')")`，且 Swagger 里要先 Authorize。

---

### 4.2 实现顺序（从下往上）

```
① UserMapper     写 SQL：count + findPage
② UserService    算 offset、调 Mapper、转 Domain、包分页
③ UserPageResponse + UserResponseMapper
④ UserController 接参数、调 Service、转 Response
```

---

### 4.3 第一步：Mapper 写 SQL

文件：`mapper/UserMapper.java`

#### （1）统计总条数 `count`

```java
@Select("<script>" +
        "SELECT COUNT(*) FROM tb_user " +
        "<where>" +
        "<if test='username != null and username != \"\"'>AND username LIKE CONCAT('%', #{username}, '%')</if>" +
        "<if test='role != null and role != \"\"'>AND role = #{role}</if>" +
        "<if test='status != null'>AND status = #{status}</if>" +
        "</where>" +
        "</script>")
long count(@Param("username") String username, @Param("role") String role, @Param("status") Integer status);
```

**逐行解释：**

| 代码 | 解释 |
|------|------|
| `@Select` | 这是查询 SQL，MyBatis 会执行它 |
| `<script> ... </script>` | 动态 SQL：条件可有可无 |
| `COUNT(*)` | 数一数一共有多少条（分页要显示「共 N 条」） |
| `<if test='username != null ...'>` | 只有前端传了 username，才拼 `AND username LIKE ...` |
| `CONCAT('%', #{username}, '%')` | 模糊查询，例如传 `admin` 能匹配 `admin`、`myadmin` |
| `@Param("username")` | 方法参数名和 SQL 里 `#{username}` 对应 |

#### （2）查当前页数据 `findPage`

```java
@Select("<script>" +
        "SELECT * FROM tb_user " +
        "<where>" +
        "<if test='username != null and username != \"\"'>AND username LIKE CONCAT('%', #{username}, '%')</if>" +
        "<if test='role != null and role != \"\"'>AND role = #{role}</if>" +
        "<if test='status != null'>AND status = #{status}</if>" +
        "</where>" +
        "ORDER BY id LIMIT #{pageSize} OFFSET #{offset}" +
        "</script>")
List<UserEntity> findPage(@Param("pageSize") int pageSize, @Param("offset") int offset,
                          @Param("username") String username, @Param("role") String role,
                          @Param("status") Integer status);
```

**逐行解释：**

| 代码 | 解释 |
|------|------|
| `SELECT *` | 查出当前页每一行的所有列 |
| `ORDER BY id` | 按 id 排序，列表顺序稳定 |
| `LIMIT #{pageSize}` | 每页最多几条 |
| `OFFSET #{offset}` | 跳过前面多少条（第 2 页要跳过第 1 页的数据） |
| 返回 `List<UserEntity>` | 多行 → 多个对象放进列表 |

**offset 怎么算？** 在 Service 里算，见下一节。

---

### 4.4 第二步：Service 写业务

文件：`application/UserService.java`

```java
@Transactional(readOnly = true)
public PageResponse<User> listPage(int current, int pageSize, String username, String role, Integer status) {
    int safeCurrent = Math.max(current, 1);
    int safePageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (safeCurrent - 1) * safePageSize;

    long total = userMapper.count(username, role, status);
    List<User> records = userMapper.findPage(safePageSize, offset, username, role, status)
            .stream()
            .map(userConverter::toDomain)
            .toList();
    return PageResponse.of(safeCurrent, safePageSize, total, records);
}
```

**逐行解释：**

| 代码 | 解释 |
|------|------|
| `@Transactional(readOnly = true)` | 只读事务，适合纯查询 |
| `Math.max(current, 1)` | 页码最小是 1，防止传 0 或负数 |
| `Math.min(Math.max(pageSize, 1), 100)` | 每页 1～100 条，防止一次查太多 |
| `offset = (页码-1) × 每页条数` | 第 1 页 offset=0；第 2 页每页 10 条则 offset=10 |
| `userMapper.count(...)` | 先查总数 |
| `userMapper.findPage(...)` | 再查当前页 Entity 列表 |
| `.stream().map(userConverter::toDomain)` | 每个 Entity 转成 Domain（去掉 password） |
| `PageResponse.of(...)` | 包成 `{ current, pageSize, total, records }` |

---

### 4.5 第三步：Response 类

**`UserPageResponse`**（给前端的分页 JSON）：

```java
public class UserPageResponse {
    private int current;      // 当前第几页
    private int pageSize;     // 每页几条
    private long total;       // 一共多少条
    private List<UserResponse> records;  // 当前页用户列表
}
```

**`UserResponseMapper.fromPage(...)`**：把 Service 返回的 `PageResponse<User>` 转成 `UserPageResponse`（里面的用户变成 `UserResponse` 列表）。

---

### 4.6 第四步：Controller 暴露 HTTP

文件：`controller/UserController.java`

```java
@GetMapping
@PreAuthorize("hasRole('ADMIN')")
public UserPageResponse listPage(
        @RequestParam(defaultValue = "1") int current,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String username,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) Integer status
) {
    return UserResponseMapper.fromPage(
            userService.listPage(current, pageSize, username, role, status)
    );
}
```

**逐行解释：**

| 注解 / 代码 | 解释 |
|-------------|------|
| `@GetMapping` | 处理 GET 请求；类上有 `@RequestMapping("/users")`，合起来是 `GET /users` |
| `@PreAuthorize("hasRole('ADMIN')")` | 只有 ADMIN 角色能调 |
| `@RequestParam` | URL 问号后面的参数，如 `?current=1&pageSize=10` |
| `defaultValue = "1"` | 前端不传时默认第 1 页 |
| `required = false` | 筛选条件可选，不传就不筛选 |
| `UserResponseMapper.fromPage(...)` | Domain 分页 → 对外 JSON 结构 |

改完代码要 **重启** `./mvnw spring-boot:run`。

---

### 4.7 Swagger 怎么测

1. **用户管理** → **`GET /users`** → **Try it out**
2. 参数可先默认：`current=1`，`pageSize=10`，其余留空
3. **Execute**

**成功示例（200）：**

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
      "email": "admin@example.com",
      "phone": "13800000001",
      "createdAt": "...",
      "updatedAt": "..."
    }
  ]
}
```

**没有 `password` 是正常的。**

| 状态码 | 原因 |
|--------|------|
| 401 | 没做 Authorize |
| 403 | 登录账号不是 ADMIN |

---

## 五、接口 2：用户详情 `GET /users/{id}`

### 5.1 这个接口是干什么的？

列表里看到一行，点「查看」要知道完整信息 → 根据 **id** 查 **一个** 用户。  
路径里的 `{id}` 叫 **路径参数**，例如 `/users/1` 表示查 id=1 的用户。

---

### 5.2 实现顺序

```
① UserMapper.findById
② UserService.getById（没有就抛业务异常）
③ UserDetailResponse（user + message）
④ UserController.getById
```

---

### 5.3 第一步：Mapper

```java
@Select("SELECT * FROM tb_user WHERE id = #{id}")
UserEntity findById(Long id);
```

| 代码 | 解释 |
|------|------|
| `WHERE id = #{id}` | `#{id}` 会被方法参数 `id` 替换，防止 SQL 注入 |
| 返回 `UserEntity` | 查不到时返回 `null`（不是抛异常，异常在 Service 里抛） |

---

### 5.4 第二步：Service

```java
@Transactional(readOnly = true)
public User getById(Long id) {
    UserEntity entity = userMapper.findById(id);
    if (entity == null) {
        throw new BusinessException("USER_NOT_FOUND", "用户不存在：" + id);
    }
    return userConverter.toDomain(entity);
}
```

| 代码 | 解释 |
|------|------|
| `findById` | 调 Mapper 查库 |
| `entity == null` | 没有这个 id |
| `BusinessException` | 业务错误，全局会转成 HTTP 400 + JSON 提示 |
| `toDomain` | 转成无密码的 Domain |

---

### 5.5 第三步：Response

```java
public class UserDetailResponse {
    private UserResponse user;
    private String message;   // 例如 "查询成功"
}
```

Controller 里：

```java
User user = userService.getById(id);
return new UserDetailResponse(UserResponseMapper.from(user), "查询成功");
```

---

### 5.6 第四步：Controller

```java
@GetMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public UserDetailResponse getById(@PathVariable Long id) {
    User user = userService.getById(id);
    return new UserDetailResponse(UserResponseMapper.from(user), "查询成功");
}
```

| 注解 | 解释 |
|------|------|
| `@GetMapping("/{id}")` | 和类上的 `/users` 拼成 `GET /users/{id}` |
| `@PathVariable Long id` | 把 URL 里的 `1` 绑定到变量 `id` |

---

### 5.7 Swagger 怎么测

1. **`GET /users/{id}`** → **Try it out**
2. **id** 填 `1`（可先调列表看有哪些 id）
3. **Execute**

**成功：**

```json
{
  "user": { "id": 1, "username": "admin", ... },
  "message": "查询成功"
}
```

**故意测失败**：id 填 `99999` → **400**，提示用户不存在。

---

## 六、接口 3：创建用户 `POST /users`（重点）

### 6.1 这个接口是干什么的？

在系统里 **新增** 一个可登录用户：要传用户名、密码、角色等。  
**POST** 一般表示「新建」；数据放在 **Request body** 的 JSON 里。

---

### 6.2 实现顺序

```
① CreateUserRequest（JSON 长什么样 + 校验）
② UserMapper.findByUsername + insert
③ UserService.createUser（查重、加密密码、insert）
④ CreateUserResponse
⑤ UserController.createUser
```

---

### 6.3 第一步：Request 类（前端 JSON → Java 对象）

文件：`controller/request/CreateUserRequest.java`

```java
@Data
public class CreateUserRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度需在 3-50 个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度需在 6-100 个字符之间")
    private String password;

    private String displayName;

    @NotBlank(message = "角色不能为空")
    private String role;

    private String email;
    private String phone;
}
```

| 注解 | 解释 |
|------|------|
| `@NotBlank` | 不能是 null 或空字符串 |
| `@Size(min=6)` | 密码至少 6 位 |
| Controller 上的 `@Valid` | 触发校验，不通过直接 400，进不了 Service |

**role 只能填**：`ADMIN`、`WAREHOUSE_MANAGER`、`OPERATOR`、`BUSINESS`（和数据库种子一致）。

---

### 6.4 第二步：Mapper

```java
@Select("SELECT * FROM tb_user WHERE username = #{username} LIMIT 1")
UserEntity findByUsername(String username);

@Insert("INSERT INTO tb_user (username, password, display_name, role, email, phone, status, created_at, updated_at) " +
        "VALUES (#{username}, #{password}, #{displayName}, #{role}, #{email}, #{phone}, #{status}, #{createdAt}, #{updatedAt})")
@Options(useGeneratedKeys = true, keyProperty = "id")
int insert(UserEntity entity);
```

| 代码 | 解释 |
|------|------|
| `findByUsername` | 创建前查重：用户名已存在就不能再插 |
| `@Insert` | 插入一行 |
| `#{displayName}` | Java 属性名，对应表列 `display_name` |
| `useGeneratedKeys = true` | 插入后把数据库自增的 **id** 写回 `entity.id` |
| `keyProperty = "id"` | 主键写回到 Entity 的哪个字段 |
| 返回值 `int` | 受影响行数，正常为 1 |

---

### 6.5 第三步：Service（核心业务）

```java
public User createUser(String username, String password, String displayName, String role,
                       String email, String phone) {
    // 1. 查重
    if (userMapper.findByUsername(username) != null) {
        throw new BusinessException("USER_USERNAME_EXISTS", "用户名已存在：" + username);
    }

    // 2. 组装 Entity
    UserEntity newUser = new UserEntity();
    newUser.setUsername(username);
    newUser.setPassword(passwordEncoder.encode(password));  // 明文 → BCrypt 哈希
    newUser.setDisplayName(displayName != null ? displayName : username);
    newUser.setRole(role);
    newUser.setEmail(email);
    newUser.setPhone(phone);
    newUser.setStatus(1);
    LocalDateTime now = LocalDateTime.now();
    newUser.setCreatedAt(now);
    newUser.setUpdatedAt(now);

    // 3. 插入数据库（漏了这句会：Swagger 看似成功，库里却没有数据）
    userMapper.insert(newUser);

    // 4. 返回 Domain（此时 newUser.getId() 已有值）
    return userConverter.toDomain(newUser);
}
```

| 步骤 | 解释 |
|------|------|
| `passwordEncoder.encode` | **绝不能**把明文密码直接存库 |
| `displayName != null ? ... : username` | 没填显示名就用用户名 |
| `userMapper.insert` | **必须调用**，否则没有数据 |
| `createUser` 返回 `User` | Controller 再转成 `CreateUserResponse` |

---

### 6.6 第四步：Controller

```java
@PostMapping
@PreAuthorize("hasRole('ADMIN')")
public CreateUserResponse createUser(@RequestBody @Valid CreateUserRequest request) {
    User user = userService.createUser(
            request.getUsername(),
            request.getPassword(),
            request.getDisplayName(),
            request.getRole(),
            request.getEmail(),
            request.getPhone()
    );
    return new CreateUserResponse(user.getId(), UserResponseMapper.from(user), "创建成功");
}
```

| 注解 | 解释 |
|------|------|
| `@PostMapping` | POST `/users` |
| `@RequestBody` | 把 JSON 自动转成 `CreateUserRequest` |
| `@Valid` | 校验 Request 里的 `@NotBlank` 等 |
| `CreateUserResponse` | 返回 userId + user + message |

---

### 6.7 Swagger 怎么测

1. **`POST /users`** → **Try it out**
2. Request body（**username 每次换一个**，避免重复）：

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

3. **Execute**

**成功：**

```json
{
  "userId": 5,
  "user": { "id": 5, "username": "student01", ... },
  "message": "创建成功"
}
```

**检查点：**

- `userId` 不能是 null
- 再调 `GET /users/5` 能查到
- 控制台有 `UserMapper.insert` 且 `Updates: 1`

| 现象 | 原因 |
|------|------|
| 400 用户名已存在 | 换 username |
| 400 校验失败 | 密码太短、用户名为空等 |
| 200 但 userId 为 null | Service 没调 `insert` |
| 200 但列表没有 | 没重启 / 看错库 |

---

## 七、接口 4：更新用户 `PUT /users/{id}`

### 7.1 这个接口是干什么的？

修改已有用户：显示名、角色、邮箱、手机、状态；密码 **可选**（不传就不改密码）。  
**PUT** + 路径里的 `id` 表示「改 id 为 X 的那条记录」。

---

### 7.2 第一步：Request

文件：`controller/request/UpdateUserRequest.java`

```java
@Data
public class UpdateUserRequest {
    @NotBlank private String displayName;
    @NotBlank private String role;
    private String email;
    private String phone;
    private Integer status;      // 1 启用，0 禁用
    private String password;     // 可选：null 或空 = 不改密码
}
```

**要改谁？** 由 URL 的 `id` 决定，不在 JSON 里。

---

### 7.3 第二步：Mapper

```java
@Update("UPDATE tb_user SET display_name=#{displayName}, role=#{role}, email=#{email}, phone=#{phone}, " +
        "status=#{status}, password=#{password}, updated_at=#{updatedAt} WHERE id=#{id}")
int update(UserEntity entity);
```

根据 `entity.id` 更新对应行；`password` 若 Service 没改，仍是库里的旧哈希。

---

### 7.4 第三步：Service

```java
public User update(Long id, String displayName, String role, String email, String phone,
                   Integer status, String password) {
    UserEntity entity = userMapper.findById(id);
    if (entity == null) {
        throw new BusinessException("USER_NOT_FOUND", "用户不存在：" + id);
    }

    entity.setDisplayName(displayName);
    entity.setRole(role);
    entity.setEmail(email);
    entity.setPhone(phone);
    if (status != null) {
        entity.setStatus(status);
    }
    if (StringUtils.hasText(password)) {
        entity.setPassword(passwordEncoder.encode(password));
    }
    entity.setUpdatedAt(LocalDateTime.now());

    userMapper.update(entity);
    return userConverter.toDomain(entity);
}
```

| 代码 | 解释 |
|------|------|
| 先 `findById` | 没有用户就报错 |
| `StringUtils.hasText(password)` | 只有传了非空密码才加密并更新 |
| `userMapper.update` | 把改好的 Entity 写回数据库 |

---

### 7.5 第四步：Controller

```java
@PutMapping("/{id}")
public UpdateUserResponse updateUser(@PathVariable Long id,
                                     @RequestBody @Valid UpdateUserRequest request) {
    User user = userService.update(id, request.getDisplayName(), request.getRole(),
            request.getEmail(), request.getPhone(), request.getStatus(), request.getPassword());
    return new UpdateUserResponse(UserResponseMapper.from(user), "更新成功");
}
```

---

### 7.6 Swagger 怎么测

1. **`PUT /users/{id}`**，**id** 填 `5`
2. Request body：

```json
{
  "displayName": "学生01-已改名",
  "role": "OPERATOR",
  "email": "newmail@example.com",
  "phone": "13800139999",
  "status": 1
}
```

不修改密码：**不要写 `password` 字段**。

3. **Execute**，再用 **`GET /users/5`** 核对。

---

## 八、接口 5：禁用用户 `DELETE /users/{id}`

### 8.1 这个接口是干什么的？

后台「删除用户」很多公司 **不会真删数据库行**（要留审计），只把 `status` 改成 `0`（禁用），叫 **软删除**。  
本项目 **DELETE** 接口做的就是这件事，SQL 仍是 `UPDATE`，不是 `DELETE FROM`。

---

### 8.2 Service

```java
public void delete(Long id) {
    UserEntity entity = userMapper.findById(id);
    if (entity == null) {
        throw new BusinessException("USER_NOT_FOUND", "用户不存在：" + id);
    }
    entity.setStatus(0);
    entity.setUpdatedAt(LocalDateTime.now());
    userMapper.update(entity);   // 复用 update 方法，不是 DELETE SQL
}
```

---

### 8.3 Controller

```java
@DeleteMapping("/{id}")
public DeleteUserResponse deleteUser(@PathVariable Long id) {
    userService.delete(id);
    return new DeleteUserResponse(id, "禁用成功");
}
```

---

### 8.4 Swagger 怎么测

1. **`DELETE /users/{id}`**，**id** 填 `5`
2. **Execute**

**成功：**

```json
{
  "userId": 5,
  "message": "禁用成功"
}
```

**验证：**

- `GET /users/5` → `status` 应为 `0`
- 数据库里 `SELECT * FROM tb_user WHERE id=5` → **行还在**，只是 `status=0`

---

## 九、五个接口 + Swagger 速查表

| # | 方法 | 路径 | Body | 主要参数 |
|---|------|------|------|----------|
| 0 | POST | `/auth/login` | username, password | 先登录再 Authorize |
| 1 | GET | `/users` | 无 | current, pageSize, username?, role?, status? |
| 2 | GET | `/users/{id}` | 无 | id |
| 3 | POST | `/users` | CreateUserRequest | — |
| 4 | PUT | `/users/{id}` | UpdateUserRequest | id |
| 5 | DELETE | `/users/{id}` | 无 | id |

---

## 十、推荐课堂练习顺序

1. **第 0 节**：登录 + Authorize（全员先做）  
2. **第六节**：创建用户（练 insert，最容易漏代码）  
3. **第四节**：列表（验证能查到刚建的）  
4. **第五节**：详情  
5. **第七节**：更新  
6. **第八节**：禁用（理解软删除）

---

## 十一、Swagger 界面词对照

| 英文 | 含义 |
|------|------|
| Try it out | 进入可编辑、可发送状态 |
| Execute | 真正发 HTTP 请求 |
| Parameters | URL 查询参数或路径参数 |
| Request body | POST/PUT 的 JSON |
| Responses | 返回状态码和 JSON |
| Authorize | 设置 Bearer token |

---

## 十二、源码对照（查作业用）

| 接口 | Controller | Service | Mapper |
|------|------------|---------|--------|
| 列表 | `listPage` | `listPage` | `count`, `findPage` |
| 详情 | `getById` | `getById` | `findById` |
| 创建 | `createUser` | `createUser` | `findByUsername`, `insert` |
| 更新 | `updateUser` | `update` | `findById`, `update` |
| 禁用 | `deleteUser` | `delete` | `findById`, `update` |

完整代码路径：`src/main/java/com/eurasia/pro_skill/` 下对应包。  
也可用 `api-contract/http/user.http` + REST Client 插件调试（需先登录填 token）。

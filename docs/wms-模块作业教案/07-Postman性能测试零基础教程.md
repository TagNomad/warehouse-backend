# FR-01 性能测试 — Postman 零基础一步一步教程

> **写给谁**：完全没做过性能测试的同学  
> **测什么**：`backend-server` 后端 FR-01 的 **3 个读接口**  
> **对应报告**：课程报告第 4 章 → **4.3.1 性能测试** → 表4-3  
> **工具**：Postman（功能测试用过的话，不用装新软件）

---

## 零、三种测试别搞混

| 类型 | 工具 | 问什么 |
| --- | --- | --- |
| **单元测试** | JUnit + Mockito | 业务代码逻辑对不对？ |
| **功能测试** | Postman / `.http` | 返回的数据对不对？ |
| **性能测试** | **Postman Runner** | 连续打很多次，**平均快不快、有没有失败**？ |

性能测试看的是：

- **平均响应时间**（Average response time）
- **最慢几次**（看 Runner 结果表里的 Duration）
- **失败数**（Tests failed 应为 0）

---

## 一、准备软件和环境

### 1.1 需要已安装的

| 软件 | 检查命令 |
| --- | --- |
| JDK 21 | `java -version` |
| Docker | `docker --version` |
| Postman | 打开桌面应用或 [postman.com/downloads](https://www.postman.com/downloads/) |

### 1.2 启动后端（必做）

**终端 A** — 在项目根目录 `backend-server/`：

```bash
# 1. 数据库（首次或重启电脑后）
docker compose -f docker-compose.dev.yml up -d

# 2. 本地配置（只做一次）
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties

# 3. 启动后端（窗口保持打开）
./mvnw spring-boot:run
```

等到日志出现 `Tomcat started on port 8080`。

**浏览器验证**：打开 `http://localhost:8080/swagger-ui.html`，`POST /auth/login` 用 `admin` / `Admin@123` 能拿到 token。

> 后端没启动就跑 Postman，全是红色失败，没有意义。

---

## 二、导入现成 Postman 集合

项目已准备好集合文件，不用自己一个个建请求。

### 第 1 步：导入

1. 打开 Postman  
2. 左上角 **Import**  
3. 选文件：`api-contract/postman/FR-01-性能测试.postman_collection.json`  
4. 导入后左侧出现集合 **「FR-01 性能测试」**

### 第 2 步：看集合里有什么

```text
FR-01 性能测试
├── 0-登录（先跑一次）
├── PERF-01 仓库列表
│   └── GET 仓库列表
├── PERF-02 库位树
│   └── GET 库位树形视图
└── PERF-03 库位详情
    └── GET 库位完整详情
```

### 第 3 步：确认变量

点集合名 → **Variables** 标签：

| 变量 | 初始值 |
| --- | --- |
| `baseUrl` | `http://localhost:8080` |
| `token` | （空，登录后自动填入） |

---

## 三、先登录（只做一次）

1. 点开 **「0-登录（先跑一次）」**  
2. 点蓝色 **Send**  
3. 下方应显示 **200 OK**，Body 里有 `"token":"..."`  
4. 点集合 → **Variables**，确认 `token` 的 **Current Value** 已有内容

登录请求的 **Tests** 脚本会自动执行：

```javascript
pm.collectionVariables.set('token', body.token);
```

后面三个 PERF 请求会用 `Authorization: Bearer {{token}}`。

> token 有效期 24 小时。隔天测试失败，重新 Send 一次登录即可。

---

## 四、三条性能用例怎么跑

课程报告 **表4-3** 对应关系：

| 编号 | Postman 文件夹 | 接口 | 建议 Runner 设置 |
| --- | --- | --- | --- |
| **PERF-01** | PERF-01 仓库列表 | `GET /warehouses/list` | Iterations **500**，Delay **0 ms** |
| **PERF-02** | PERF-02 库位树 | `GET /warehouses/1/tree` | Iterations **100**，Delay **0 ms** |
| **PERF-03** | PERF-03 库位详情 | `GET /warehouse-locations/1/detail` | Iterations **100**，Delay **0 ms** |

### 4.1 跑 PERF-01（常规列表）

1. 鼠标悬停在文件夹 **「PERF-01 仓库列表」** 上  
2. 点右侧 **···** → **Run folder**（或 Run collection 只勾选该文件夹）  
3. 在 Runner 窗口设置：

| 选项 | 值 | 说明 |
| --- | --- | --- |
| Iterations | `500` | 连续请求 500 次 |
| Delay | `0` | 请求之间不等待 |
| Save responses | 不勾 | 省内存 |

4. 点 **Run FR-01 性能测试**（或 Run PERF-01）  
5. 跑完后看底部统计：

| 指标 | 在哪看 | 报告里怎么写 |
| --- | --- | --- |
| 平均响应时间 | **Average response time** | 「平均 xxx ms」 |
| 总请求数 | **Total requests** | 「样本 xxx 次」 |
| 失败数 | **Failed tests** | 必须是 **0** |
| 单次最慢 | 结果表里 Duration 最大值 | 可选写「最大 xxx ms」 |

### 4.2 跑 PERF-02、PERF-03

同样方法，分别 Run 文件夹 **PERF-02**、**PERF-03**（Iterations 用 100）。

每次跑前如果报 401，先重新 Send 登录请求。

---

## 五、达标标准（填「预期结果」）

| 编号 | 平均响应 | 最慢单次（参考） | 失败 |
| --- | --- | --- | --- |
| PERF-01 | ≤ **2000 ms**（2 秒） | 大部分 ≤ 2500 ms | 0 |
| PERF-02 | ≤ **3000 ms**（3 秒） | ≤ 3000 ms | 0 |
| PERF-03 | ≤ **3000 ms**（3 秒） | ≤ 3000 ms | 0 |

本地 `localhost` 数据少，往往只有几毫秒～几十毫秒，**达标很容易**；老师主要看你是否按步骤测了、数据是否真实。

---

## 六、和课程模版「10 并发 5 分钟」怎么对应？

模版举例是 JMeter 的「10 用户并发 5 分钟」。Postman Runner **是单线程连续请求**，不是真 10 人同时在线。

**报告里可以这样写（诚实且合理）**：

> 测试场景：使用 Postman Collection Runner，对目标接口连续发起 500 次请求，请求间隔 0 ms，模拟常规业务高峰下的连续读取负载。  
> 测试工具：Postman。  
> 说明：本实训为单机后端，采用连续压测方式验证响应时间是否满足 ≤2s / ≤3s 的非功能指标。

Iterations 可按机器调整：想跑久一点可设 **Iterations=3000**（约几分钟，视机器而定）。

---

## 附录 A：手动测「创建仓库」时编码重复怎么办？

`POST /warehouses` 要求 **`code` 全局唯一**。Runner 循环 100 次如果 Body 里一直写 `"code": "WH-001"`，第 2 次起就会返回：

```json
{
  "code": "WAREHOUSE_CODE_EXISTS",
  "message": "仓库编码已存在：WH-001"
}
```

这是**业务规则**，不是 Postman 坏了。

### 办法一：每次自动生成唯一编码（推荐）

在 **POST 创建仓库** 请求的 **Pre-request Script** 标签页粘贴：

```javascript
// 每次请求前生成唯一仓库编码（≤50 字符）
const uniqueCode = 'WH-PERF-' + Date.now() + '-' + Math.floor(Math.random() * 10000);
pm.variables.set('warehouseCode', uniqueCode);
pm.variables.set('warehouseName', '性能测试仓-' + uniqueCode);
```

Body 改成用变量（不要用固定 WH-001）：

```json
{
  "code": "{{warehouseCode}}",
  "name": "{{warehouseName}}",
  "type": "RAW_MATERIAL",
  "address": "性能测试地址",
  "capacity": 1000
}
```

Header 照旧：`Authorization: Bearer {{token}}`

然后 Runner 跑这个请求，每次 `code` 都不同，不会撞车。

**Tests** 可写：

```javascript
pm.test('创建成功 HTTP 200', function () {
    pm.response.to.have.status(200);
});
```

### 办法二：只测读接口，不测创建（课程报告默认方案）

创建会**往数据库里塞数据**，跑 500 次会多出 500 条仓库，列表越来越慢，结果失真。

课程报告 **表4-3** 测的是三个 **GET 读接口**，不测 POST 创建——这是正常做法。

| 操作类型 | 适不适合循环压测 |
| --- | --- |
| GET 列表 / 树 / 详情 | ✅ 适合 |
| POST 创建 | ⚠️ 要动态编码 + 注意数据膨胀 |
| PUT / DELETE | ⚠️ 一般不做性能循环 |

### 办法三：测完清理（可选）

如果已经创建了很多 `WH-PERF-*` 测试仓，可在 Adminer（`http://localhost:8081`）里手动删，或 Swagger 对无子级的仓库调 `DELETE /warehouses/{id}`。

### 库区、库位创建同理

| 接口 | 唯一规则 | 变量示例 |
| --- | --- | --- |
| `POST /warehouses/{id}/zones` | 同仓库内 `code` 唯一 | `Z-PERF-{{$timestamp}}` |
| `POST /warehouse-zones/{id}/locations` | 同库区内 `code` 唯一 | `L-PERF-{{$timestamp}}` |

Pre-request Script 写法相同，把 `warehouseCode` 换成 `zoneCode` / `locationCode` 即可。

Postman 内置动态变量：`{{$timestamp}}`、`{{$randomInt}}` 也可直接写在 Body 里：

```json
{
  "code": "WH-{{$timestamp}}",
  "name": "测试仓-{{$timestamp}}",
  "type": "RAW_MATERIAL"
}
```

---


复制下表到 Word，把 **实际结果** 换成你 Runner 里看到的数字：

| 用例编号 | 测试项 | 测试目的 | 测试场景与负载 | 测试步骤 | 预期结果 | 实际结果 | 测试状态 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| PERF-01 | 仓库列表接口 | 验证列表查询响应时间 | Postman Runner 连续 500 次，Delay 0 | 导入集合→登录→Run PERF-01 文件夹 | 平均≤2s，失败0 | *填你的 Average* | *正常/异常* |
| PERF-02 | 库位树形视图 | 验证复杂聚合查询 | 连续 100 次 | Run PERF-02 文件夹 | 平均≤3s，失败0 | *填* | *填* |
| PERF-03 | 库位详情 | 验证详情聚合查询 | 连续 100 次 | Run PERF-03 文件夹 | 平均≤3s，失败0 | *填* | *填* |

**测试步骤**（可照抄进报告）：

1. 启动 Docker MySQL 与 Spring Boot（8080）  
2. Postman 导入 `api-contract/postman/FR-01-性能测试.postman_collection.json`  
3. 执行登录请求，保存 JWT 到集合变量 `token`  
4. 对 PERF-01/02/03 文件夹分别使用 Collection Runner  
5. 记录 Average response time 与 Failed tests  

---

## 八、交作业要截什么图

至少 **2 张截图**：

1. **Runner 跑完后的汇总页** — 能看到 Average response time、Total requests、Failed = 0  
2. **某次 PERF 的结果列表** — 显示多条绿色 Passed  

可选：登录成功、`token` 变量有值的 Variables 截图。

---

## 九、常见问题

### Q1：全是 401 Unauthorized

- 先单独 Send **「0-登录」**  
- 检查集合 Variables 里 `token` 是否有值  
- 业务请求 Header 是否为 `Bearer {{token}}`（注意有空格）

### Q2：Connection refused

后端没启动。终端 A 执行 `./mvnw spring-boot:run`。

### Q3：GET /warehouses/1/tree 返回 400

数据库没有 id=1 的仓库。重启后端让 Flyway 跑 V6 种子数据，或 Swagger 先查 `GET /warehouses/list`。

### Q4：Runner 里 Failed tests 不为 0

点红色行看 **Test Results**：

- 401 → 重新登录  
- 500 → 看后端终端报错，多半是 MySQL 没起来  

### Q5：创建仓库性能测试第二次起全失败 WAREHOUSE_CODE_EXISTS

`code` 不能重复。见上文 **附录 A**：Pre-request Script 生成 `{{warehouseCode}}`，Body 不要用固定 `WH-001`。

### Q6：和 `warehouse.http` 什么关系？

| 文件 | 用途 |
| --- | --- |
| `api-contract/http/warehouse.http` | VS Code 里做**功能测试** |
| `api-contract/postman/FR-01-性能测试.postman_collection.json` | Postman 做**性能测试** |

接口相同，工具不同。建议功能测试先跑通 `.http`，再做性能测试。

---

## 十、操作清单（可打印）

```text
□ 1. docker compose -f docker-compose.dev.yml up -d
□ 2. ./mvnw spring-boot:run（终端保持运行）
□ 3. Swagger 确认 login 成功
□ 4. Postman Import → FR-01-性能测试.postman_collection.json
□ 5. Send「0-登录」，token 有值
□ 6. Run PERF-01：Iterations=500, Delay=0，Failed=0
□ 7. Run PERF-02：Iterations=100
□ 8. Run PERF-03：Iterations=100
□ 9. 截 Runner 汇总图
□ 10. 填 Word 表4-3
```

---

## 十一、相关文件

| 文件 | 说明 |
| --- | --- |
| `api-contract/postman/FR-01-性能测试.postman_collection.json` | 导入即用 |
| `api-contract/http/warehouse.http` | 功能测试对照 |
| `04-测试与质量保障.md` | 单元测试（4.1） |
| 本文件 | 性能测试（4.3.1） |

第 4 章分工：

| 小节 | 用什么 |
| --- | --- |
| 4.1 单元测试 | `mvn test` + JaCoCo |
| 4.2 功能测试 | Postman 或 `warehouse.http` 用例表 |
| 4.3.1 性能测试 | **本篇 Postman Runner** |

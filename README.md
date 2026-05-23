# Pro Skill Backend

Spring Boot 后端服务。

## 本地开发前置准备

0. **JDK 21 或以上**（`pom.xml` 中 `java.version=21`）。终端执行 `java -version` 应显示 21+；若仍是 17，在 `~/.zshrc` 中设置：

    ```bash
    export JAVA_HOME="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
    export PATH="$JAVA_HOME/bin:$PATH"
    ```

    Homebrew 安装：`brew install openjdk`（或 `openjdk@21`）。修改后执行 `source ~/.zshrc` 或新开终端。

1. **Docker Desktop**（推荐，用于本地 MySQL）。安装后确认：

    ```bash
    docker --version
    docker compose version
    ```

2. 在 `src/main/resources/` 目录下创建 `application-local.properties`，填写本机数据库连接信息（参考同目录下的同名模板文件）：

    ```bash
    cp src/main/resources/application-local.properties.example \
       src/main/resources/application-local.properties
    ```

    使用下方 Docker 方式时，模板里的密码 `root` 可直接使用；本机安装的 MySQL 请改成实际密码。

    ```properties
    db.host=localhost
    db.port=3306
    db.name=pro_skill

    spring.datasource.url=jdbc:mysql://${db.host}:${db.port}/${db.name}?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    spring.datasource.username=root
    spring.datasource.password=root
    ```

   > 此文件已加入 `.gitignore`，不会被提交。

## 本地 MySQL（Docker，推荐）

项目根目录提供 `docker-compose.dev.yml`，包含 **MySQL 8** 和 **Adminer**（Web 数据库管理界面）。

### 首次启动

```bash
docker compose -f docker-compose.dev.yml up -d
```

会创建两个容器：

| 容器名 | 用途 | 端口 |
|--------|------|------|
| `pro-skill-mysql` | MySQL，库名 `pro_skill` | `3306` |
| `pro-skill-adminer` | Web 管理界面 | `8081` |

默认账号：`root` / `root`。数据库 `pro_skill` 会自动创建；**表结构由 Flyway 在应用启动时迁移，无需手动建表**。

查看状态：

```bash
docker compose -f docker-compose.dev.yml ps
```

### 容器停了 / 重启电脑后

```bash
docker compose -f docker-compose.dev.yml start
# 或
docker compose -f docker-compose.dev.yml up -d
```

### 停止（数据保留）

```bash
docker compose -f docker-compose.dev.yml stop
```

### 彻底删除容器但保留数据卷

```bash
docker compose -f docker-compose.dev.yml down
```

数据保存在 Docker 卷 `pro-skill-mysql-data` 中，下次 `up -d` 会恢复。

### 清空数据库重来（慎用）

```bash
docker compose -f docker-compose.dev.yml down -v
docker compose -f docker-compose.dev.yml up -d
```

然后重新 `./mvnw spring-boot:run`，Flyway 会重新建表。

### 已有同名容器冲突时

若之前手动 `docker run` 过 `pro-skill-mysql`，先删除旧容器再启动 compose：

```bash
docker rm -f pro-skill-mysql pro-skill-adminer
docker compose -f docker-compose.dev.yml up -d
```

> 旧容器若使用匿名卷，数据不会自动迁移到新卷；开发环境一般直接重来即可。

## 可视化查看 MySQL（Adminer）

确保 compose 已启动后，浏览器打开：

```
http://localhost:8081
```

登录信息：

| 字段 | 值 |
|------|-----|
| 系统 | MySQL |
| 服务器 | `mysql`（compose 内已预填；若连本机 MySQL 填 `host.docker.internal`） |
| 用户名 | `root` |
| 密码 | `root` |
| 数据库 | `pro_skill` |

可浏览 `tb_user`、`flyway_schema_history` 等表。

## 备选：本机安装 MySQL（不用 Docker）

```bash
brew install mysql
brew services start mysql
mysql -u root -p -e "CREATE DATABASE pro_skill DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

`application-local.properties` 中密码改为本机 MySQL 实际密码。注意 **不要与 Docker MySQL 同时占用 3306 端口**。

## 教学文档（用户 CRUD 全流程）

零基础学生建议先阅读：

- [docs/用户管理CRUD上课教案.md](docs/用户管理CRUD上课教案.md) — **讲师用**：知识点、话术、时间分配、演示顺序  
- [docs/用户管理CRUD全流程.md](docs/用户管理CRUD全流程.md) — **学生用**：从零加接口 + Swagger 逐步点击

源码中 `UserController` / `UserService` / `UserMapper` 等已配有详细中文注释，可与文档对照阅读。

## 启动

```bash
# 1. 确保 MySQL 在跑（Docker 方式）
docker compose -f docker-compose.dev.yml up -d

# 2. 启动后端
./mvnw spring-boot:run

# 或先打包再运行
./mvnw package -DskipTests
java -jar target/pro-skill-0.0.1-SNAPSHOT.jar
```

## Swagger UI

服务启动后，浏览器打开：

```
http://localhost:8080/swagger-ui.html
```

OpenAPI 原始 JSON 地址：

```
http://localhost:8080/v3/api-docs
```

## 健康检查（Actuator）

```
http://localhost:8080/actuator/health
```

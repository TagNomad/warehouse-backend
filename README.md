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

项目根目录提供 `docker-compose.dev.yml`，包含 **MySQL 8**、**Adminer** 和 **应用**。这是后面加 Redis、MinIO、Nginx 时用的同一份基座。

### 首次启动

```bash
docker compose -f docker-compose.dev.yml up -d --build
```

会创建三个容器：

| 容器名 | 用途 | 端口 |
|--------|------|------|
| `pro-skill-mysql` | MySQL，库名 `pro_skill` | `3306` |
| `pro-skill-adminer` | Web 管理界面 | `8081` |
| `pro-skill-app` | 本仓库的 Spring Boot | `8080` |

应用容器里的数据库主机是服务名 `mysql`。要看写错主机会怎样：

```bash
docker compose -f docker-compose.dev.yml -f docker-compose.localhost.yml up -d --force-recreate app
docker logs pro-skill-app
```

改回基座：

```bash
docker compose -f docker-compose.dev.yml up -d --force-recreate app
```

TraceId 默认关闭。打开：

```bash
APP_LESSON_TRACE_ID=on docker compose -f docker-compose.dev.yml up -d --force-recreate app
```

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

## 教学文档

本分支是备课分支。登录、用户、仓库–库区–库位仍可运行，用来接这学期的新内容。

- [docs/备课/第一部分-讲义.html](docs/备课/第一部分-讲义.html) — 第 10 周课堂讲义，浏览器打开后按步骤讲
- [docs/备课/第一部分-上课教案.md](docs/备课/第一部分-上课教案.md) — 第 10 周教师用的时间安排
- [docs/备课/教案.md](docs/备课/教案.md) — 按内容块写的教案（周次标在每一块上）
- [docs/综合能力实训-教学上下文.md](docs/综合能力实训-教学上下文.md) — 课表、作业时间和漏洞对照

## 启动

Compose 会同时拉起 MySQL 和应用，应用在 `http://localhost:8080`。

```bash
docker compose -f docker-compose.dev.yml up -d --build
```

要在宿主机上跑进程、数据库仍用容器时，先停掉应用容器，避免占用 8080，并准备好 `application-local.properties`（数据库主机 `localhost`）：

```bash
docker compose -f docker-compose.dev.yml stop app
./mvnw spring-boot:run
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

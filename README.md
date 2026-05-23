# Pro Skill Backend

Spring Boot 后端服务。

## 本地开发前置准备

0. **JDK 21 或以上**（`pom.xml` 中 `java.version=21`）。终端执行 `java -version` 应显示 21+；若仍是 17，在 `~/.zshrc` 中设置：

    ```bash
    export JAVA_HOME="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
    export PATH="$JAVA_HOME/bin:$PATH"
    ```

    Homebrew 安装：`brew install openjdk`（或 `openjdk@21`）。修改后执行 `source ~/.zshrc` 或新开终端。

1. 在 `src/main/resources/` 目录下创建 `application-local.properties`，填写本机数据库连接信息（参考同目录下的同名模板文件）：

    ```properties
    db.host=localhost
    db.port=3306
    db.name=pro_skill

    spring.datasource.url=jdbc:mysql://${db.host}:${db.port}/${db.name}?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    spring.datasource.username=root
    spring.datasource.password=your_password
    ```

   > 此文件已加入 `.gitignore`，不会被提交。

2. 创建数据库 `pro_skill`：

    打开 MySQL 命令行或客户端工具，执行：

    ```sql
    CREATE DATABASE pro_skill DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    ```

    或使用命令行：

    ```bash
    mysql -u root -p -e "CREATE DATABASE pro_skill DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    ```

    > Flyway 会在应用启动时自动执行迁移脚本，无需手动建表。

## 启动

```bash
# 使用项目自带的 Maven Wrapper（推荐，自动使用阿里云镜像）
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

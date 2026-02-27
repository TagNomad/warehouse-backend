# Pro Skill Backend

Spring Boot 后端服务。

## 本地开发前置准备

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

2. 确保本地 MySQL 中已创建数据库 `pro_skill`，Flyway 会在启动时自动执行迁移脚本。

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

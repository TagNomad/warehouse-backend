---
name: java-build-troubleshooter
description: 排查 Spring Boot / Maven / VS Code Java 构建链路问题，尤其是 MapStruct、Lombok、注解处理器、IDE 自动编译导致的“能编译但运行失败”、Bean 注入异常、生成类漂移。适用于“另一台电脑正常这台不行”“clean compile 正常但 spring-boot:run 失败”“generated-sources 正常但注入不到”“怀疑是缓存/IDE/自动编译问题”等场景。
compatibility: opencode
license: MIT
metadata:
  scope: project
  style: guard
---

# 何时使用此技能

当用户遇到以下问题时启用：
- `./mvnw spring-boot:run` 启动失败，但 `compile` 看起来正常
- MapStruct 生成类相关问题：Bean 注入失败、实现类缺失、接口类型找不到
- Lombok + MapStruct 混用时怀疑是注解处理器顺序或 IDE 干扰
- 同一项目“在另一台电脑正常，本机异常”
- VS Code / Java Language Server 可能覆盖 Maven 编译产物
- 需要为 OpenAPI 导出脚本、启动脚本、CI 构建收敛到 Maven 单一真源
- 用户口语化描述为：“是不是缓存问题”“是不是 IDE 搞坏了”“为什么 clean compile 没问题但 run 挂了”“generated-sources 都有为什么 Spring 找不到 bean”“javap 看起来不对”

# 输入期望

用户可能提供：
- 启动报错 / 编译报错 / Bean 注入异常
- `pom.xml`、`SKILL.md`、`.vscode/settings.json`
- `javap` 输出、`target/generated-sources` 内容、`target/classes` 反编译结果
- “另一台机器正常，这台不正常”的现象描述

如信息不足，也必须先做“最小可行排查”，优先区分：
- 编译期失败
- 运行期失败
- IDE/构建链路污染

# 核心规则（必须遵守）

## 1. 先区分编译期问题和运行期问题
- 编译期问题：MapStruct 未生成实现、Lombok getter/setter 不可见、annotation processor 配置缺失
- 运行期问题：源码/生成源码正常，但 Spring 按类型注入失败、Bean 注册异常、`.class` 与源码语义不一致
- 不允许把两类问题混为一谈

## 2. Maven 是注解处理器项目的单一真源
- 优先使用 `./mvnw clean compile` 或 `./mvnw clean spring-boot:run`
- 对 MapStruct/Lombok 项目，优先怀疑 IDE 自动编译覆盖了 Maven 产物
- 若 VS Code Java 自动构建干扰：
  - `.vscode/settings.json` 中设置 `java.autobuild.enabled=false`
  - `java.configuration.updateBuildConfiguration=interactive`

## 3. MapStruct + Lombok 排查顺序（固定）
1. 检查 `pom.xml` 是否存在：
   - `mapstruct`
   - `mapstruct-processor`
   - `lombok`
   - `lombok-mapstruct-binding`
2. 检查 `maven-compiler-plugin` 的 `annotationProcessorPaths` 顺序
3. 执行 `./mvnw clean compile`
4. 检查 `target/generated-sources/annotations`
5. 用 `javap` 对比 `target/classes` 的真实签名
6. 再判断是否为 Spring 扫描 / Bean 类型匹配问题

## 4. 不要只看生成源码，必须核对编译后的 `.class`
- 若 `generated-sources` 正常，但运行时类型注入失败：
  - 必查 `javap -classpath target/classes ...`
- 若生成源码与 `.class` 语义不一致：
  - 高度怀疑 IDE 或增量编译污染
- 这类场景下，不应直接归因到业务代码错误

## 5. 优先做“最小确定性验证”
固定验证链路：
1. `./mvnw clean compile`
2. `javap -classpath target/classes ...`
3. `./mvnw spring-boot:run`
4. `curl /actuator/health` 或目标接口
5. 再运行脚本（如 OpenAPI 导出脚本）

## 6. 常见结论模板
- “这是编译期注解处理器问题，不是 Spring 注入问题”
- “这是运行期类型不一致问题，优先怀疑 IDE 覆盖 Maven 产物”
- “这是普通 MapStruct warning，不是启动失败根因”
- “这是 Lombok/MapStruct 混用配置问题，需要检查 APT 路径和顺序”

<!-- Evolution: 2026-03-14 | source: backend-server-openapi-export-debug | skill: java-build-troubleshooter -->

## Pattern Added (2026-03-14)

**Pattern**: 当 MapStruct 生成类“源码正常但运行时无法按接口注入”时，先验证 `.class` 和 IDE autobuild，而不是先替换掉 MapStruct

**Source**: backend-server OpenAPI 导出排障

**Confidence**: 0.92

### Updated Checklist
- [ ] 先区分编译期失败和运行期失败
- [ ] `clean compile` 后检查 `target/generated-sources/annotations`
- [ ] 用 `javap` 核对 `target/classes` 的真实签名
- [ ] 若另一台机器正常，本机异常，优先排查 IDE 自动构建
- [ ] 为项目添加 `.vscode/settings.json`，收敛到 Maven 单一构建真源

# 输出格式（建议遵守）

当用户给出构建/启动异常时，输出建议包含：

## ✅ 初步结论
- 属于：编译期 / 运行期 / IDE 构建链路问题

## 🔎 证据
- 哪个命令成功
- 哪个命令失败
- 生成源码与 `.class` 是否一致
- Spring 扫描到了什么、注入失败在什么类型上

## 🛠 修复动作
1. ...
2. ...
3. ...

## ✅ 回归验证
- `./mvnw clean compile`
- `./mvnw spring-boot:run`
- 脚本或目标接口验证

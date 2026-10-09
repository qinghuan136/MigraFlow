# MigraFlow

基于 Java、LangGraph4j 与 OpenRewrite 的 Java 项目迁移工具。

## 工程结构

采用 Maven 单模块工程，Java 基础包为 `com.qinghuan.migraflow`，应用使用 JDK 21 与 Spring Boot 4.1.1，以非 Web 模式运行。

- `src/main/java/com/qinghuan/migraflow/`：应用代码，先按职责保留一级功能包。
- `src/main/java/com/qinghuan/migraflow/cli/`：持续交互 CLI 的输入、命令处理与终端展示。
- `src/main/resources/`：随应用发布的配置和提示词等资源。
- `src/test/java/com/qinghuan/migraflow/cli/`：CLI 测试代码。
- `src/test/resources/`：测试资源。
- `.mvn/wrapper/`：Maven Wrapper 配置。
- `docs/`：项目设计说明。

细分包在实际实现需要时创建。空目录暂用 `.gitkeep` 保留，加入实际文件后可移除。

## CLI 与迁移流程

当前 CLI 启动后只接收要迁移的本地目录路径。支持绝对路径、相对于启动工作目录的路径、中文及空格；路径外可使用双引号。无效路径会提示并继续等待输入，空行忽略，输入 `/exit` 或输入流结束时退出。当前不接收 Git URL、迁移目标或启动参数。

目录检查通过后，以规范化的绝对路径调用 `MigrationService.prepareWorkspace`。该方法暂留空，CLI 仅提示目录已接收，不创建 Workspace、不修改目标项目，也不运行 Agent。

CLI 负责输入输出，后续由迁移入口协调 Workspace 准备、Agent 工作流与结果汇总。

启动时由 Spring 创建服务与 CLI Bean，`CliRunner` 通过构造函数注入 CLI，在 `CommandLineRunner` 中进入交互循环。交互结束后关闭 Spring 容器。CLI 的输入输出对象在 `CliConfiguration` 中组装，业务服务使用 `@Service` 管理，Workspace 入口仍留空。

每次迁移采用 Workspace → Planner → Executor 的流程，直接修改用户指定的目标项目目录。持续交互不要求引入持久化 Task / Session 业务层。

## 构建与运行

使用 JDK 21，`JAVA_HOME` 指向该 JDK；下面的运行命令要求 `java` 在 PATH 中。Maven Wrapper 固定 Maven 3.9.16，首次使用需要下载 Maven 与构建依赖。

Windows PowerShell：

```powershell
.\mvnw.cmd verify
java -jar .\target\migraflow.jar
```

Linux / macOS：

```bash
sh ./mvnw verify
java -jar target/migraflow.jar
```

交互示例：

```text
migraflow> ./old-project
已接收本地目录：<规范化的项目绝对路径>
migraflow> /exit
已退出。
```

验证命令会编译、执行 CLI 和 Spring 集成测试，并由 Spring Boot Maven 插件打包包含运行依赖的可执行 JAR。

## 已接入的开发依赖

| 依赖 | 用途 |
|---|---|
| Spring Boot Starter | IoC、自动配置、YAML 配置和默认日志 |
| Spring Boot AspectJ Starter | Spring AOP 与切面注解支持 |
| Spring Boot Validation Starter | Jakarta Bean Validation 参数与配置校验 |
| Spring Boot JSON Starter | Jackson JSON 读写，使用 Boot 4 默认的 Jackson 3 |
| Apache Commons Lang | 常用字符串及通用辅助功能 |
| Apache Commons IO | 文件和流处理 |
| Lombok | 构造函数等编译期代码生成 |
| Spring Boot Configuration Processor | 配置元数据与 IDE 提示 |
| Spring Boot Test Starter | Spring 测试、JUnit、AssertJ 和 Mockito |

版本优先由 Spring Boot Parent 管理，Commons IO 的版本单独固定。Lombok 先于配置处理器执行；二者均排除在可执行 JAR 的运行依赖之外。现有日志配置保留 WARN 及以上日志，避免干扰 CLI 提示符。

LangGraph4j、OpenRewrite 和模型接入依赖在实现相关能力时再添加。

项目范围和架构参见 [项目描述](docs/about-the-project.md)，开发约定参见 [AGENTS.md](AGENTS.md)。

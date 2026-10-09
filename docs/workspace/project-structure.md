# Workspace 项目结构解析设计

本文记录 Workspace 初始化阶段的项目结构解析共识。它确定职责、信息来源及实现边界，不预设具体类、字段、接口或工作流 state。整体架构参见 [项目描述](../about-the-project.md)。

## 目标与当前范围

当前目标是：CLI 输入本地项目根目录后，准备项目环境，建立 ProjectModel 与内存 LST，返回可用的 Workspace。此阶段不启动 Planner、Executor，也不执行迁移 Recipe。

- 先支持常见 Maven 项目，包括嵌套多模块；Gradle 和复杂构建场景分阶段扩展。
- 直接使用目标项目目录；运行日志、分析结果和临时数据单独管理，不复制项目或创建 Git worktree。
- ProjectIndex 暂不实现；模块、源码分组与 LST 的基本关联直接维护。
- 不构建全项目调用图、控制流图、知识图谱或独立知识平台。

## ProjectModel：统一的项目结构视图

ProjectModel 将不同构建工具提供的项目信息抽象为 MigraFlow 内部通用的表达，供项目查询、源码解析及后续 Agent 使用。统一共同概念和访问方式，同时保留必要的构建工具差异。

需要表达的信息类别包括：

- 构建工具、项目基本信息及本次加载所采用的构建环境。
- 模块标识、实际目录、组织关系和模块间依赖。
- SourceSet，即源码分组，以及对应的源码、测试和资源目录。
- 各模块和源码分组需要的依赖、Classpath、Java 语言与编译配置。
- 必要的构建工具特有信息，如 Maven 父 POM、Profile、依赖管理，或 Gradle Configuration。

SourceSet 在统一视图中表示源码分组，不要求各构建工具原生提供同名对象。ProjectModel 只整合迁移所需信息，不完整复制 Maven / Gradle 的内部模型。

Maven 的聚合、父 POM 继承和模块依赖分别表达；Gradle 的逻辑项目路径和实际目录也分别处理，不按文件夹层级推断这些关系。

依赖的实际解析结果与原始声明位置须能区分，便于后续定位版本修改应落在模块构建文件、父 POM、属性或依赖管理中。具体表达方式在实现时确定。

## LST：文件级语义结构

ProjectModel 描述项目及构建上下文；LST 保存 OpenRewrite 解析后的文件级语义结构。两者分开管理，并支持基本的双向定位：

- 从项目模型定位某个模块或源码分组对应的 LST。
- 从 LST 追溯所属模块、源码分组及解析时使用的构建上下文。
- 构建文件、资源文件等按实际归属关联，不强行归入 Java SourceSet。

OpenRewrite Marker 可提供部分关联信息，但其完整程度依赖具体加载流程。不能假定基础 Parser 会自动提供完整的模块、依赖和构建元数据；必要的关联由 MigraFlow 补齐。

完整 LST 在 MigraFlow JVM 内维护，不注入 LLM 上下文或工作流 checkpoint。后续 Agent 先查询项目概览，再按需获取相关源码与语义信息。

## 信息来源与集成边界

优先复用构建系统的项目模型和 OpenRewrite 的解析能力，由 MigraFlow 负责整合、关联与维护。

- Maven 信息依据 Reactor、有效构建模型及依赖解析结果获取，不能只递归扫描 `pom.xml`。
- Gradle 信息依据实际构建模型获取，不能只扫描 `build.gradle` 或根据目录猜测模块。
- 复用 OpenRewrite 插件的项目加载思路和可用能力，但不预设插件提供可直接作为 SDK 调用的完整项目模型接口。
- 构建信息获取可以使用构建工具适配；源码解析和 LST 变换仍通过应用内部 OpenRewrite Java API 完成。

ProjectModel、源码解析和基线验证使用一致的构建上下文，包括适用的 Profile、JDK 与构建参数。MigraFlow 自身的 Java 21 运行环境与目标项目的构建 JDK 分开管理。

## 初始化步骤与就绪标准

1. 定位目标项目，确认构建入口，准备运行数据位置并记录文件基线及已有 Git 改动。
2. 获取构建系统实际生效的模块、源码分组、依赖及编译配置，形成 ProjectModel。
3. 准备目标项目的构建与解析环境，执行构建和测试基线检查，保存结果与日志。
4. 根据项目模型和构建上下文调用 OpenRewrite 解析文件，在 JVM 内维护 LST，并建立基本关联。
5. 检查所需信息和解析质量，汇总 Workspace 初始化结果，由 CLI 展示摘要并继续等待输入。

需要预先生成源码或模块编译产物时，通过对应构建流程准备，随后同步项目模型与解析输入；具体加载顺序按实现需要确定。

可用的 Workspace 应能说明项目如何组织和构建，支持定位待解析文件、取得相应解析上下文，并查询关联的 LST。不能仅因获得语法树就认定类型信息完整。

基线失败或当前所需构建、解析信息缺失时，记录原因并停止初始化。诊断应区分信息缺失与确认不存在，避免将未解析到的依赖、模块或源码当作不存在。

Recipe 目录、迁移知识检索和 Agent Tools 在后续迁移能力接入时补充，不作为当前项目结构解析阶段的前置条件。

## 一致性与生命周期

- 磁盘源码及构建配置是持久化输入；ProjectModel 与 LST 是对应当前输入和构建环境的派生结果。
- 源码变更后刷新相关 LST 及其归属信息；构建配置、依赖或 Classpath 变化后更新项目模型，并重新解析受影响的类型信息。
- 初期优先保证正确，必要时重新加载受影响模块；精细增量更新后续再优化。
- ProjectModel 可按需缓存；LST 不要求逐个序列化，重启后可基于当前输入重新构建。
- 初始化服务及集成组件由 Spring 管理；Workspace 及其中项目数据按本次运行创建、维护和释放。
- 替换 Workspace、初始化失败或 CLI 退出时释放相关内存与进程资源，保留目标项目及必要诊断产物。

## 参考资料

- [Maven 多模块与 Reactor](https://maven.apache.org/guides/mini/guide-multiple-modules.html)
- [Maven Profile](https://maven.apache.org/guides/introduction/introduction-to-profiles)
- [Gradle Tooling API](https://docs.gradle.org/current/userguide/tooling_api.html)
- [OpenRewrite Marker](https://docs.openrewrite.org/reference/framework-provided-markers)

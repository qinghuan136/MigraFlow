# Migration Agent MVP 架构设计方案（V1）

## 一、项目目标

基于 **LangGraph + OpenRewrite**，实现面向 Java 项目的自动化代码迁移系统。

优先利用现有 Recipe 完成确定性迁移，再通过 Agent 进行验证、错误诊断与长尾修复，形成完整的迁移闭环。

**MVP 暂不支持：** 自动生成新 Recipe、动态重新规划、多 Agent 自由协作。

## 二、技术架构

- **LangGraph**：工作流编排、状态管理、条件分支与循环。
- **OpenRewrite（Java）**：源码解析、LST 构建、Recipe 执行、代码变换。
- **LLM**：迁移规划、智能验证、错误诊断与代码修复。
- **Maven / Gradle**：项目构建、依赖分析与测试执行。

采用固定节点的工作流架构，各节点通过共享的 `MigrationState` 传递信息。

## 三、核心模块

### 1. Analyzer（项目分析）

- 识别项目结构、模块、技术栈及版本。
- 分析依赖关系和迁移影响范围。
- 检查迁移前的构建状态。

**输出：** `ProjectModel`

### 2. Planner（迁移规划）

- 根据迁移目标检索现有 Recipe。
- 选择、配置并组合适用 Recipe。
- 确定迁移步骤及执行顺序。
- 标记暂时无法覆盖的迁移需求。

**输出：** `MigrationPlan`

MVP 中只生成一次计划，不支持运行中重新规划。

### 3. Executor（迁移执行）

- 调用 OpenRewrite 加载并解析项目。
- 按计划执行现有 Recipe。
- 完成 LST 变换并生成新源码。
- 记录变更文件、Recipe 执行结果及 Diff。

**输出：** `ExecutionResult`

### 4. Validator（迁移验证）

- 执行编译、单元测试及必要的集成测试。
- 检查代码 Diff、依赖版本及迁移目标。
- 利用 LLM 分析可疑修改和潜在遗漏。
- 汇总错误并判断是否需要修复。

**输出：** `ValidationResult`

### 5. Diagnosis（诊断与修复）

- 分析编译错误、测试失败及迁移遗漏。
- 结合源码、迁移文档定位问题。
- 通过 Coding Agent 进行局部代码修复。
- 修复后重新进入 Validator。

**输出：** `RepairResult`

无法安全修复或超过重试上限时，标记阻塞，不自动修改 MigrationPlan。

## 四、工作流程

**主流程：**

用户输入项目及迁移目标 → Analyzer → Planner → Executor → Validator

**验证分支：**

- 验证成功且存在后续步骤 → Executor 执行下一步。
- 验证成功且全部步骤完成 → 输出迁移报告。
- 验证失败 → Diagnosis → 局部修复 → Validator。
- 无法修复或超过重试上限 → 标记阻塞，交由人工处理。

Planner 只运行一次，Executor 按既定计划推进，Diagnosis 仅负责局部修复。

## 五、共享状态设计

通过 LangGraph 的 `MigrationState` 保存工作流状态。

| 字段 | 含义 |
|---|---|
| `project_model` | 项目分析结果 |
| `migration_target` | 迁移目标 |
| `migration_plan` | 迁移执行计划 |
| `current_step` | 当前执行步骤 |
| `execution_result` | Recipe 执行结果及 Diff |
| `validation_result` | 编译、测试及智能验证结果 |
| `repair_history` | 历史诊断与修复记录 |
| `retry_count` | 当前步骤重试次数 |
| `status` | 当前迁移状态 |

各节点读取所需状态，执行后更新相应字段。

## 六、MVP 实现范围

**必须实现：**

1. Java 项目分析与现有 Recipe 检索。
2. 基于 LangGraph 的固定迁移工作流。
3. OpenRewrite Recipe 执行及 Diff 生成。
4. 编译、测试与基础智能验证。
5. Agent 局部修复与有限次数重试。
6. 最终迁移报告及未解决问题记录。

**暂缓实现：**

- 自动生成 YAML / Java Recipe。
- 复杂项目关系图（Project Graph）。
- 运行时重新规划 MigrationPlan。
- 多 Agent 并行协作。
- 自动证明业务语义完全一致。

## 七、预期成果

MVP 最终能够接收一个 Java 项目及迁移目标，自动选择现有 Recipe、执行迁移、验证结果，并对剩余问题进行有限的自主修复。

最终产出包括：

- 迁移后的项目代码
- Recipe 执行记录及代码 Diff
- 构建与测试结果
- Agent 修复记录
- 未覆盖或未解决问题清单

**核心原则：确定性迁移交给 OpenRewrite，迁移决策、智能验证与长尾修复交给 Agent。**
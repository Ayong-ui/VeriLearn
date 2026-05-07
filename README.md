# VeriLearn

[中文](#中文说明) | [English](#english)

---

## 中文说明

### 项目简介

VeriLearn 是一个面向自学场景的验证式学习系统后端项目。

它关注的核心问题不是“如何提供更多学习资料”，而是“如何让学习过程形成可规划、可验证、可记录、可调整的闭环”。

当前仓库对应 VeriLearn 的第一版后端实现（V1）。这一版的目标不是一次性做完整平台，而是先把最小可运行、可验证、可扩展的核心链路搭起来，并为后续“章节驱动学习系统”演进提供稳定底座。

### 产品定位

VeriLearn 希望成为一个运行在飞书上的 **AI 学习执行系统**。

它不是单纯的提醒 Bot，也不是只会被动答疑的聊天助手，而更像一位会主动推进学习流程的线上老师 / 学习教练：

- 按章节组织学习内容
- 按时间推送当天任务
- 在理论学习后安排练习或 Demo
- 根据学习反馈动态调整下一步
- 持续维护学习进度与复习节奏

如果用一句话描述它：

> VeriLearn 是一个在飞书上主动推进学习流程的 AI 老师，它会按章节为你安排任务、布置练习、收集反馈、动态调整计划，并提醒你复习。

### 当前范围

V1 当前聚焦：

- 学习目标设置
- 面向测试版的用户流程编排入口
- 知识点结构保存与确认
- 章节初始化、章节步骤推进与复习标记
- 今日任务生成
- 验证项生成与任务提交
- 基础分流逻辑
- 基础进度查询
- MySQL 数据持久化
- 统一接口规范与异常处理

### 背景

很多学习产品擅长提供内容，但不擅长回答这些问题：

- 用户今天具体该学什么
- 用户是否真正掌握了内容
- 当前结果应该如何影响下一步学习安排

VeriLearn 期望支撑这样的流程：

```text
目标设置
  -> 知识点拆分
  -> 每日任务生成
  -> 学习验证
  -> 结果分流
  -> 后续安排调整
```

当前已经落地的后端主链路是：

```text
提交学习目标
  -> 查找或创建学习用户
  -> 创建或更新学习目标
  -> 保存知识点列表
  -> 确认知识点初始状态
  -> 生成今日任务
  -> 生成验证项
  -> 提交验证结果
  -> 执行基础分流
  -> 查询当前学习进度
```

从产品终态看，VeriLearn 最终希望支持的不是单纯“知识点任务系统”，而是：

```text
开始第 N 章
  -> 生成或读取理论文档
  -> 学习理论
  -> 进入 Demo / 练习
  -> 收集反馈
  -> 动态调整下一步
  -> 更新进度
  -> 标记复习
  -> 进入下一章
```

当前 V1 可以理解为这套终态系统的执行底座。

### 核心机制

VeriLearn 的核心不是“给内容”，而是“推动学习闭环”。

这套闭环可以拆成 5 个关键动作：

1. 任务推送
   系统按章节和当前进度，主动安排今天该学什么。
2. 理论学习
   用户先学习当前章节的理论内容，而不是直接做题。
3. 练习 / Demo
   在理论之后进入练习、实操或 Demo 环节。
4. 反馈采集
   系统根据题目结果、Demo 完成情况、答疑反馈等信息判断当前学习质量。
5. 动态调整
   系统根据反馈决定继续推进、补充解释、追加练习、安排复习，或回退重学。

也就是说，Demo 在产品里不是独立功能，而是章节学习中的练习环节；验证也不只是判断对错，而是服务于后续教学调整。

### 当前状态

项目当前处于 `V1 测试版后端已成型` 阶段。

已完成：

- Spring Boot 后端工程骨架
- 统一响应结构
- 全局异常处理
- MySQL 接入
- 学习目标创建与更新接口
- 知识点批量保存、确认、查询接口
- 章节初始化、步骤推进、复习完成接口
- 今日任务生成接口
- 章节驱动的任务选择与任务上下文返回
- 验证提交流程
- 验证结果与章节步骤联动推进
- 分流结果记录
- 进度查询接口
- 用户流程编排接口
- Swagger UI 接口调试入口
- 控制层与持久化链路自动化测试

下一步计划：

- 飞书命令入口
- 飞书任务卡片回调
- AI 生成章节材料
- AI 生成 Demo / 练习
- 复习机制落地

### 功能特性

当前后端能力包括：

- `GET /ping`
  - 服务健康检查接口

- 统一响应结构
  - 标准 `code / message / data` 返回格式

- 全局异常处理
  - 统一处理参数校验异常与业务异常

- `POST /api/goals`
  - 按 `feishuOpenId` 创建或更新学习目标

- `POST /api/learners/setup`
  - 一次性完成测试用户初始化：创建或更新目标、初始化知识点并确认状态

- `GET /api/learners/{feishuOpenId}/today-task`
  - 按用户身份直接生成或返回今日任务

- `GET /api/learners/{feishuOpenId}/progress`
  - 按用户身份直接查询当前进度

- `GET /api/learners/{feishuOpenId}/chapters`
  - 按用户身份直接查看当前目标下的章节概览

- `GET /api/learners/{feishuOpenId}/dashboard`
  - 返回测试版总览数据：今日任务、进度、章节概览和待复习章节

- `POST /api/goals/{goalId}/knowledge-nodes`
  - 批量覆盖保存某个学习目标下的知识点列表

- `POST /api/goals/{goalId}/knowledge-nodes/confirm`
  - 将知识点从草稿状态初始化为正式学习状态

- `GET /api/goals/{goalId}/knowledge-nodes`
  - 查询某个学习目标下的知识点列表

- `POST /api/goals/{goalId}/chapters/bootstrap`
  - 根据已确认的知识点初始化章节、步骤和默认材料

- `GET /api/goals/{goalId}/chapters/reviews/pending`
  - 查询当前目标下待复习的章节

- `GET /api/chapters/{chapterId}`
  - 查看章节详情、步骤与默认材料

- `POST /api/chapters/{chapterId}/start`
  - 启动章节学习并激活第一个步骤

- `POST /api/chapters/{chapterId}/steps/submit`
  - 提交章节步骤反馈并推进到下一步

- `POST /api/chapters/{chapterId}/review/complete`
  - 完成章节复习并更新复习状态

- `POST /api/tasks/generate`
  - 为某个学习目标生成今日唯一任务，并优先选择当前章节的当前步骤

- `POST /api/tasks/{taskId}/submit`
  - 提交当前任务的验证结果，并返回追加验证或最终分流结果；若任务来自章节步骤，则同步推进章节

- `GET /api/progress/{userId}`
  - 查询当前目标、知识点状态统计、章节统计和最近任务

### 架构说明

当前项目采用标准单体分层结构：

```text
Controller
  -> Service
  -> Mapper
  -> MySQL
```

以当前主链路为例：

```text
GoalController
  -> GoalServiceImpl
  -> LearnerUserMapper / LearningGoalMapper

KnowledgeController
  -> KnowledgeServiceImpl
  -> KnowledgeNodeMapper / LearningGoalMapper

TaskController
  -> TaskServiceImpl
  -> DailyTaskMapper / KnowledgeNodeMapper / LearningGoalMapper
  -> ValidationServiceImpl

ProgressController
  -> ProgressServiceImpl
  -> DailyTaskMapper / KnowledgeNodeMapper / LearningGoalMapper / LearningChapterMapper

ChapterController
  -> ChapterServiceImpl
  -> LearningChapterMapper / ChapterStepMapper / ChapterMaterialMapper / ChapterReviewRecordMapper

LearnerWorkflowController
  -> LearnerWorkflowServiceImpl
  -> GoalService / KnowledgeService / ChapterService / TaskService / ProgressService
```

职责划分：

- Controller 负责 HTTP 请求与响应
- Service 负责业务规则与流程控制
- Mapper 负责数据库访问
- MySQL 负责持久化

### 技术栈

- Java 17
- Spring Boot 3.5.13
- Maven 3.9.x
- MySQL 8.x
- MyBatis-Plus 3.5.15
- SpringDoc OpenAPI / Swagger UI
- JUnit 5

技术选型原则：

- 保持单体工程简单可控
- 先完成后端主闭环，再扩展更复杂能力
- 保持结构清晰，便于学习、演示和后续迭代

当前关于技术栈的重要判断：

- **主干技术栈没有变化**
- 当前只新增了 `SpringDoc OpenAPI / Swagger UI` 作为测试版接口调试入口
- 当前新增了项目级 `maven-settings.xml` 用于隔离本地 Maven 仓库与全局环境问题

### 数据模型

当前已落地的核心表：

- `learner_user`
- `learning_goal`
- `knowledge_node`
- `learning_chapter`
- `chapter_step`
- `chapter_material`
- `chapter_review_record`
- `daily_task`
- `validation_item`
- `validation_submission`
- `diversion_record`

### 仓库结构

```text
VeriLearn/
|-- README.md
|-- .gitignore
`-- verilearn/
    |-- pom.xml
    |-- mvnw
    |-- mvnw.cmd
    |-- .mvn/
    `-- src/
        |-- main/
        |   |-- java/com/verilearn/
        |   |   |-- common/
        |   |   |-- user/
        |   |   |-- goal/
        |   |   |-- knowledge/
        |   |   |-- task/
        |   |   |-- validation/
        |   |   |-- progress/
        |   |   |-- ai/
        |   |   `-- infra/
        |   `-- resources/
        `-- test/
```

### 快速开始

#### 环境要求

- JDK 17
- Maven 3.9.x
- MySQL 8.x

#### 1. 创建数据库

```sql
CREATE DATABASE verilearn DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 2. 配置本地数据库

当前本地开发配置默认使用：

- database: `verilearn`
- username: `root`
- password: `root`

当前 JDBC URL 包含：

- `useUnicode=true`
- `characterEncoding=utf8`
- `serverTimezone=Asia/Shanghai`
- `useSSL=false`
- `allowPublicKeyRetrieval=true`

主配置文件：

- `verilearn/src/main/resources/application.yml`

推荐通过环境变量覆盖敏感配置，而不是直接修改源码。当前已支持：

- `VERILEARN_DB_URL`
- `VERILEARN_DB_USERNAME`
- `VERILEARN_DB_PASSWORD`
- `VERILEARN_AI_BASE_URL`
- `VERILEARN_AI_API_KEY`
- `VERILEARN_AI_MODEL`

#### 3. 启动项目

```bash
mvn -s verilearn/maven-settings.xml -f verilearn/pom.xml spring-boot:run
```

当前 `schema.sql` 采用开发 / 测试环境下的“启动即重建核心表”策略，目的是保证快速迭代阶段的数据模型始终和代码一致。  
这适合当前测试版，不代表生产环境迁移方案。

#### 4. 运行测试

```bash
mvn -s verilearn/maven-settings.xml -f verilearn/pom.xml test
```

#### 5. 打开接口文档

启动成功后访问：

```text
http://localhost:8080/swagger-ui/index.html
```

### 接口概览

- `GET /ping`
- `POST /api/learners/setup`
- `GET /api/learners/{feishuOpenId}/today-task`
- `GET /api/learners/{feishuOpenId}/progress`
- `GET /api/learners/{feishuOpenId}/chapters`
- `GET /api/learners/{feishuOpenId}/dashboard`
- `POST /api/goals`
- `POST /api/goals/{goalId}/knowledge-nodes`
- `POST /api/goals/{goalId}/knowledge-nodes/confirm`
- `GET /api/goals/{goalId}/knowledge-nodes`
- `POST /api/goals/{goalId}/chapters/bootstrap`
- `GET /api/goals/{goalId}/chapters/reviews/pending`
- `GET /api/chapters/{chapterId}`
- `POST /api/chapters/{chapterId}/start`
- `POST /api/chapters/{chapterId}/steps/submit`
- `POST /api/chapters/{chapterId}/review/complete`
- `POST /api/tasks/generate`
- `POST /api/tasks/{taskId}/submit`
- `GET /api/progress/{userId}`

### 测试

当前自动化测试包括：

- `DemoControllerTest`
- `ChapterControllerTest`
- `LearnerWorkflowControllerTest`
  - 验证统一响应结构与全局异常处理

- `GoalControllerTest`
  - 验证学习目标创建与更新链路

- `KnowledgeControllerTest`
  - 验证知识点保存、覆盖、确认与查询

- `TaskControllerTest`
  - 验证今日任务生成与同日幂等

- `ValidationFlowTest`
  - 验证验证项提交、追加验证和基础分流

- `ProgressControllerTest`
  - 验证进度统计与最近任务查询

- `DatabaseSmokeTest`
  - 验证数据库连接与基础读写

### 版本规划

#### V1

当前版本重点完成：

- 后端工程骨架
- 面向测试版的用户流程入口
- 学习目标创建与更新
- 知识点保存与确认
- 章节初始化、步骤推进与复习标记
- 今日任务生成
- 验证项生成
- 任务提交与基础分流
- 基础进度查询
- MySQL 数据持久化
- 基础自动化测试

#### V2

V2 将重点从“知识点任务系统”升级到“章节驱动学习系统”，核心调整方向包括：

- 在现有 `chapter`、`chapter_step`、`chapter_review_record` 基础上继续扩展
- 将理论文档、Demo 指南、复习节奏做成更完整的学习产物
- 将任务、验证、复习进一步统一到章节驱动主线
- 把当前测试版工作流接到飞书交互与 AI 生成能力上

对应方案文档：

- [VeriLearn-V2-章节驱动模型调整方案.md](./VeriLearn-V2-%E7%AB%A0%E8%8A%82%E9%A9%B1%E5%8A%A8%E6%A8%A1%E5%9E%8B%E8%B0%83%E6%95%B4%E6%96%B9%E6%A1%88.md)

#### 后续迭代

- `V1.x`
  - 强化当前测试版后端能力
  - 完善 Swagger、测试和演示链路
  - 优化验证轮次控制
  - 优化分流策略

- `V2.x`
  - 飞书命令入口
  - 飞书消息交互与卡片回调
  - 章节步骤推进
  - 学习进度与复习记录联动

- `V3`
  - AI 章节材料生成
  - AI Demo / 练习生成
  - 文档回写与答疑补写
  - 更完整的工业化学习工作流

### 贡献

如果你希望参与：

1. Fork 仓库
2. 新建功能分支
3. 保持改动聚焦且清晰
4. 在行为变化时补充或更新测试
5. 发起带有清晰说明的 Pull Request

### 许可证

当前仓库尚未添加正式开源许可证。

---

## English

### Overview

VeriLearn is a backend project for a verification-based self-learning system.

Its goal is not simply to provide more content, but to turn learning into a workflow that can be planned, verified, recorded, and adjusted by the system.

This repository represents the first backend version of VeriLearn (V1). The purpose of this version is to establish a minimal, runnable, verifiable, and extensible core workflow before evolving toward a chapter-driven learning system.

### Product Positioning

VeriLearn is intended to evolve into an **AI learning execution system running inside Feishu**.

It is not just a reminder bot, and not merely a passive chat assistant. It should behave more like an online teacher or learning coach that actively drives the learning workflow:

- organizing study by chapter
- pushing the right task at the right time
- arranging exercises or demos after theory study
- collecting feedback from the learner
- dynamically adjusting the next step
- maintaining progress and review rhythm

In one sentence:

> VeriLearn is an AI teacher inside Feishu that actively drives the learning workflow by assigning chapter-based tasks, arranging exercises, collecting feedback, adjusting plans, and reminding the learner to review.

### Current Scope

V1 currently focuses on:

- learning goal setup
- learner-oriented setup and daily entry flow
- knowledge node persistence and confirmation
- chapter bootstrap, step progression, and review marking
- daily task generation
- validation item generation and task submission
- basic diversion logic
- basic progress query
- MySQL persistence
- unified API conventions and exception handling

### Background

Many learning products provide content, but do not answer questions such as:

- What should the learner study today?
- Has the learner really understood the content?
- How should the next step change based on the result?

VeriLearn is intended to support a workflow like this:

```text
Goal setup
  -> knowledge breakdown
  -> daily task generation
  -> learning validation
  -> result diversion
  -> next-step adjustment
```

The currently implemented backend flow is:

```text
Submit learning goal
  -> find or create learner
  -> create or update goal
  -> save knowledge nodes
  -> confirm initial knowledge statuses
  -> generate today's task
  -> generate validation items
  -> submit validation results
  -> execute basic diversion logic
  -> query current progress
```

From the product end-state perspective, VeriLearn is expected to evolve into a workflow like:

```text
Start chapter N
  -> generate or load theory materials
  -> study theory
  -> run demos or exercises
  -> collect feedback
  -> dynamically adjust the next step
  -> update progress
  -> mark review
  -> move to the next chapter
```

The current V1 should be understood as the execution substrate for that future system.

### Core Mechanism

The real value of VeriLearn is not "providing more content", but "driving a learning loop".

That loop can be described as:

1. task push
2. theory study
3. demo or exercise
4. feedback collection
5. dynamic adjustment

In this design, demos are not standalone artifacts. They act as in-chapter exercises, and validation is not only about right or wrong answers, but about collecting signals that help the system decide what to do next.

### Project Status

The project is currently in the `V1 test-build backend` stage.

Implemented:

- Spring Boot backend scaffold
- unified response model
- global exception handling
- MySQL integration
- learning goal create/update API
- knowledge node save/confirm/query APIs
- chapter bootstrap, step progression, and review APIs
- daily task generation API
- chapter-driven task selection and task context output
- validation submission flow
- chapter-step progression linked to validation result
- diversion result recording
- progress query API
- learner workflow orchestration APIs
- Swagger UI debugging entry
- automated tests for controller and persistence flows

Planned next:

- Feishu command entry
- Feishu card callbacks
- richer AI-generated demos and exercises
- stronger AI-guided validation and feedback orchestration
- review workflow

### Features

Current backend capabilities include:

- `GET /ping`
  - health check endpoint

- Unified API response
  - standard `code / message / data` format

- Global exception handling
  - centralized handling for validation and business exceptions

- `POST /api/goals`
  - create or update a learning goal by `feishuOpenId`

- `POST /api/learners/setup`
  - bootstrap a test learner flow in one request: create or update goal, initialize knowledge nodes, and confirm them

- `GET /api/learners/{feishuOpenId}/today-task`
  - generate or return today’s task directly by learner identity

- `GET /api/learners/{feishuOpenId}/progress`
  - query current progress directly by learner identity

- `GET /api/learners/{feishuOpenId}/chapters`
  - view chapter summaries directly by learner identity

- `GET /api/learners/{feishuOpenId}/dashboard`
  - return the test-build overview: today task, progress, chapter summaries, and pending reviews

- `POST /api/goals/{goalId}/knowledge-nodes`
  - replace and save a full list of knowledge nodes for one goal

- `POST /api/goals/{goalId}/knowledge-nodes/confirm`
  - initialize draft knowledge nodes into formal learning status

- `GET /api/goals/{goalId}/knowledge-nodes`
  - query knowledge nodes for one goal

- `POST /api/goals/{goalId}/chapters/bootstrap`
  - initialize chapters, steps, and default materials from confirmed knowledge nodes

- `GET /api/goals/{goalId}/chapters/reviews/pending`
  - list chapters that still need review

- `GET /api/chapters/{chapterId}`
  - inspect chapter details, steps, and default materials

- `POST /api/chapters/{chapterId}/start`
  - start a chapter and activate its first step

- `POST /api/chapters/{chapterId}/steps/submit`
  - submit chapter-step feedback and advance the chapter

- `POST /api/chapters/{chapterId}/review/complete`
  - complete a chapter review

- `POST /api/chapters/{chapterId}/materials/generate`
  - generate theory and demo materials for a chapter through the AI integration, with template fallback when AI output is unavailable

- `POST /api/tasks/generate`
  - generate the unique task for a given goal and day, prioritizing the current chapter step

- `POST /api/tasks/{taskId}/submit`
  - submit validation results and get either appended validation items or a final diversion result; if the task belongs to a chapter step, the chapter will be progressed too

- `POST /api/tasks/{taskId}/validation-items/generate`
  - regenerate round-one validation items for an unsubmitted task through the AI integration, replacing the initial template items

- `GET /api/progress/{userId}`
  - query current goal, knowledge status summary, chapter status summary, and recent tasks

### Architecture

The project follows a standard layered monolith structure:

```text
Controller
  -> Service
  -> Mapper
  -> MySQL
```

There is now an additional orchestration layer at the API level for the test build:

```text
LearnerWorkflowController
  -> LearnerWorkflowServiceImpl
  -> GoalService / KnowledgeService / TaskService / ProgressService
```

### Data Model

Core tables currently implemented:

- `learner_user`
- `learning_goal`
- `knowledge_node`
- `learning_chapter`
- `chapter_step`
- `chapter_material`
- `chapter_review_record`
- `daily_task`
- `validation_item`
- `validation_submission`
- `diversion_record`

### Tech Stack

- Java 17
- Spring Boot 3.5.13
- Maven 3.9.x
- MySQL 8.x
- MyBatis-Plus 3.5.15
- SpringDoc OpenAPI / Swagger UI
- DeepSeek Chat Completions API
- JUnit 5

Important note about the stack:

- **There is still no core stack change in this update**
- The current AI enhancement is implemented as an external integration through the `DeepSeek Chat Completions API`
- The Java / Spring Boot / MySQL / MyBatis-Plus core stack remains the same

### Quick Start

#### Prerequisites

- JDK 17
- Maven 3.9.x
- MySQL 8.x

#### 1. Create database

```sql
CREATE DATABASE verilearn DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 2. Configure local database

Current local configuration expects:

- database: `verilearn`
- username: `root`
- password: `root`

The current JDBC URL includes:

- `useUnicode=true`
- `characterEncoding=utf8`
- `serverTimezone=Asia/Shanghai`
- `useSSL=false`
- `allowPublicKeyRetrieval=true`

Main config file:

- `verilearn/src/main/resources/application.yml`

Sensitive values should be overridden through environment variables instead of being written into source files. The current build supports:

- `VERILEARN_DB_URL`
- `VERILEARN_DB_USERNAME`
- `VERILEARN_DB_PASSWORD`
- `VERILEARN_AI_BASE_URL`
- `VERILEARN_AI_API_KEY`
- `VERILEARN_AI_MODEL`

#### 3. Run the application

```bash
mvn -s verilearn/maven-settings.xml -f verilearn/pom.xml spring-boot:run
```

In the current development and test stage, `schema.sql` rebuilds the core tables on startup so that the fast-evolving data model stays aligned with the code.  
This is suitable for the current test build, not a production migration strategy.

#### 4. Run tests

```bash
mvn -s verilearn/maven-settings.xml -f verilearn/pom.xml test
```

#### 5. Open API docs

After the application starts, open:

```text
http://localhost:8080/swagger-ui/index.html
```

This is the recommended entry for the current test build because it shortens the validation path for the full learner workflow.

### API Overview

- `GET /ping`
- `POST /api/learners/setup`
- `GET /api/learners/{feishuOpenId}/today-task`
- `GET /api/learners/{feishuOpenId}/progress`
- `GET /api/learners/{feishuOpenId}/chapters`
- `GET /api/learners/{feishuOpenId}/dashboard`
- `POST /api/goals`
- `POST /api/goals/{goalId}/knowledge-nodes`
- `POST /api/goals/{goalId}/knowledge-nodes/confirm`
- `GET /api/goals/{goalId}/knowledge-nodes`
- `POST /api/goals/{goalId}/chapters/bootstrap`
- `GET /api/goals/{goalId}/chapters/reviews/pending`
- `GET /api/chapters/{chapterId}`
- `POST /api/chapters/{chapterId}/start`
- `POST /api/chapters/{chapterId}/steps/submit`
- `POST /api/chapters/{chapterId}/review/complete`
- `POST /api/chapters/{chapterId}/materials/generate`
- `POST /api/tasks/generate`
- `POST /api/tasks/{taskId}/validation-items/generate`
- `POST /api/tasks/{taskId}/submit`
- `GET /api/progress/{userId}`

### Testing

Current automated tests include:

- `DemoControllerTest`
- `ChapterControllerTest`
- `LearnerWorkflowControllerTest`
- `GoalControllerTest`
- `KnowledgeControllerTest`
- `TaskControllerTest`
- `ValidationFlowTest`
- `ProgressControllerTest`
- `DatabaseSmokeTest`

In addition to automated tests, the current test build has also passed a manual smoke flow:

```text
POST /api/learners/setup
  -> GET /api/learners/{feishuOpenId}/today-task
  -> POST /api/tasks/{taskId}/validation-items/generate
```

### Version Plan

#### V1

Current version focuses on:

- backend scaffold
- learner-oriented test-build entry flow
- goal create/update
- knowledge node persistence and confirmation
- chapter bootstrap, step progression, and review marking
- daily task generation
- validation item generation
- task submission and basic diversion
- basic progress query
- AI-generated chapter materials with template fallback
- AI-regenerated validation items for unsubmitted tasks
- MySQL persistence
- automated tests

#### Future Iterations

- `V1.x`
  - strengthen the current test-build backend
  - improve Swagger, testing, and demo flows
  - refine validation rounds
  - refine diversion strategy

- `V2`
  - continue extending the current chapter-driven learning model
  - enrich theory materials, demo guides, and review lifecycle
  - connect chapter steps with Feishu interaction and AI-generated content

- `V2.x`
  - Feishu command entry
  - Feishu interaction and card callbacks
  - chapter-step progression
  - review workflow integration

- `V3`
  - AI-driven chapter expansion and stronger exercise orchestration
  - document write-back and guided Q&A
  - review-aware learning planning
  - a more industrialized learning workflow

### Contributing

If you want to contribute:

1. Fork the repository
2. Create a feature branch
3. Keep changes focused and readable
4. Add or update tests when behavior changes
5. Open a pull request with a clear description

### License

No open source license has been added yet.

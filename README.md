# VeriLearn

[中文](#中文说明) | [English](#english)

---

## 中文说明

### 项目简介

VeriLearn 是一个面向自学场景的 **AI 学习执行系统** 后端项目。  
它不想解决“资料不够多”的问题，而是想把学习过程做成一条可执行、可验证、可调整、可复盘的闭环。

当前仓库是 VeriLearn 的 **V1 测试版后端实现**。  
这一版的目标不是一次做完整个平台，而是先把下面这条核心链路跑通：

```text
开始学习主题
  -> 初始化知识点和章节
  -> 生成今天任务
  -> 查看理论 / Demo 材料
  -> 完成本地实操
  -> 提交学习结果
  -> AI 评估掌握度
  -> 生成评估报告与下一步建议
```

### 产品定位

VeriLearn 更像一个运行在飞书中的 **学习教练**，而不是单纯的提醒机器人或问答机器人。

它负责：

- 按章节组织学习内容
- 生成当天任务
- 给出理论材料和 Demo 指南
- 收集用户反馈
- 用 AI 评估掌握度
- 根据结果安排下一步或复习

### 当前状态

项目当前处于 **V1 测试版后端已成型** 阶段，已经具备：

- Spring Boot 后端骨架
- MySQL 持久化
- 统一响应结构与全局异常处理
- 学习目标、知识点、章节、任务、验证、进度模块
- DeepSeek AI 理论 / Demo / 评估能力接入
- 学习空间 `Markdown` 材料落盘
- 飞书事件入口与本地可测的交互骨架
- 自动化测试

### 核心能力

#### 1. 学习目标与章节初始化

- 创建或更新学习目标
- 保存并确认知识点
- 基于知识点初始化章节、步骤和默认材料

#### 2. 今日任务生成

- 按当前章节步骤生成唯一任务
- 返回任务对应的理论文件和 Demo 文件路径

#### 3. 学习空间材料

系统会把章节材料真正落成 `.md` 文件，当前默认目录结构类似：

```text
verilearn/learning-space/
  spring-boot/
    01-spring-basics/
      user/
        theory/
          theory.md
        demo/
          demo-task.md
        summary/
          evaluation-report.md
          next-step.md
```

#### 4. Demo 提交与 AI 评估

用户完成本地 Demo 后，可以提交：

- 完成说明
- 代码片段
- 提问内容

系统随后会：

- 调用 AI 评估掌握度
- 生成 `evaluation-report.md`
- 生成 `next-step.md`
- 推进章节状态

#### 5. 工作流聚合接口

除了底层 REST 接口，当前还提供了更适合演示和后续飞书接入的工作流接口：

- `POST /api/learners/setup`
- `GET /api/learners/{feishuOpenId}/today-task`
- `GET /api/learners/{feishuOpenId}/progress`
- `GET /api/learners/{feishuOpenId}/chapters`
- `GET /api/learners/{feishuOpenId}/dashboard`
- `GET /api/learners/{feishuOpenId}/current-context`

其中 `current-context` 会返回：

- 今日任务
- 当前章节详情
- 最近评估报告文件路径
- 下一步建议文件路径

### 飞书接入状态

当前仓库已经完成了 **飞书代码接入层**：

- `POST /api/feishu/events`
- 文本命令解析：`/start`、`/today`、`/progress`、`/dashboard`
- 飞书文本回复客户端
- 飞书卡片预览 / 回调骨架

但 **真实飞书平台联调还没有完成**。  
也就是说，当前已经具备：

- 本地可测的飞书适配层

还未完成：

- 公网回调地址联通
- 飞书后台真实事件订阅
- 真实机器人消息 / 卡片回发验证

### AI 职责

在 VeriLearn 中，AI 的职责是：

- 生成理论材料
- 生成 Demo 指南
- 分析用户提交结果
- 生成评估报告
- 生成下一步建议

AI **不直接**：

- 更新数据库
- 创建目录或写文件
- 调用后端接口
- 发送飞书消息

这些都由后端系统统一执行，这样更可控、更容易测试。

### 技术栈

- Java 17
- Spring Boot 3.5.13
- Maven 3.9.x
- MySQL 8.x
- MyBatis-Plus 3.5.15
- SpringDoc OpenAPI / Swagger UI
- DeepSeek Chat Completions API
- JUnit 5

### 目录结构

```text
VeriLearn/
├── README.md
├── .gitignore
└── verilearn/
    ├── pom.xml
    ├── mvnw
    ├── mvnw.cmd
    ├── maven-settings.xml
    └── src/
        ├── main/
        │   ├── java/com/verilearn/
        │   │   ├── ai/
        │   │   ├── chapter/
        │   │   ├── common/
        │   │   ├── goal/
        │   │   ├── infra/
        │   │   ├── knowledge/
        │   │   ├── progress/
        │   │   ├── task/
        │   │   ├── user/
        │   │   ├── validation/
        │   │   └── workflow/
        │   └── resources/
        └── test/
```

### 快速开始

#### 环境要求

- JDK 17
- Maven 3.9.x
- MySQL 8.x
- Windows（当前优先验证的部署环境）

#### 1. 创建数据库

```sql
CREATE DATABASE verilearn DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 2. 配置环境变量

当前支持的主要环境变量包括：

- `VERILEARN_DB_URL`
- `VERILEARN_DB_USERNAME`
- `VERILEARN_DB_PASSWORD`
- `VERILEARN_AI_BASE_URL`
- `VERILEARN_AI_API_KEY`
- `VERILEARN_AI_MODEL`
- `VERILEARN_FEISHU_BASE_URL`
- `VERILEARN_FEISHU_APP_ID`
- `VERILEARN_FEISHU_APP_SECRET`
- `VERILEARN_FEISHU_VERIFICATION_TOKEN`
- `VERILEARN_LEARNING_SPACE_ROOT`

#### 3. 启动项目

```bash
mvn -s verilearn/maven-settings.xml -f verilearn/pom.xml spring-boot:run
```

#### 4. 运行测试

```bash
mvn -s verilearn/maven-settings.xml -f verilearn/pom.xml test
```

#### 5. 打开接口文档

```text
http://localhost:8080/swagger-ui/index.html
```

### 关键接口

- `POST /api/learners/setup`
- `GET /api/learners/{feishuOpenId}/today-task`
- `GET /api/learners/{feishuOpenId}/dashboard`
- `GET /api/learners/{feishuOpenId}/current-context`
- `GET /api/chapters/{chapterId}`
- `GET /api/materials/{materialId}/content`
- `POST /api/chapters/{chapterId}/steps/submit`
- `POST /api/chapters/{chapterId}/demo-evaluations`
- `POST /api/tasks/{taskId}/submit`
- `POST /api/tasks/{taskId}/validation-items/generate`
- `POST /api/feishu/events`

### 测试情况

当前自动化测试覆盖：

- 目标设置
- 知识点保存与确认
- 章节初始化与步骤推进
- 今日任务生成
- Demo 结果评估
- 进度查询
- 飞书事件入口
- 数据库读写冒烟测试

当前本地测试结果：

- `25` tests
- `0` failures
- `0` errors

### 后续路线

#### V1 继续收口

- 真正打通飞书平台联调
- 让飞书消息直接指向学习空间和材料内容
- 收口最终演示链路

#### V2

- 更完整的章节驱动学习模型
- 更稳定的 AI 上下文组织
- 更丰富的 Demo / 练习 / 复习机制
- 更强的学习产物管理

### License

当前仓库尚未添加正式开源许可证。

---

## English

### Overview

VeriLearn is an **AI learning execution system** backend for self-learning scenarios.

Its purpose is not simply to provide more content, but to turn learning into a workflow that can be executed, verified, adjusted, and reviewed.

This repository currently represents the **V1 test-build backend**.  
The goal of this version is to establish a runnable and demoable core flow:

```text
Start a learning topic
  -> initialize knowledge nodes and chapters
  -> generate today's task
  -> read theory and demo materials
  -> finish local practice
  -> submit learning result
  -> let AI evaluate mastery
  -> generate evaluation and next-step notes
```

### Product Positioning

VeriLearn is designed as an **AI learning coach inside Feishu**, not just a reminder bot or a passive Q&A bot.

It is responsible for:

- organizing learning by chapter
- generating daily tasks
- providing theory and demo materials
- collecting user feedback
- evaluating learning results with AI
- deciding the next step or review

### Current Status

The project is currently in the **V1 test-build backend** stage and already includes:

- Spring Boot backend scaffold
- MySQL persistence
- unified response and global exception handling
- goal / knowledge / chapter / task / validation / progress modules
- DeepSeek-powered theory, demo, and evaluation generation
- local Markdown learning-space files
- Feishu callback and local interaction skeleton
- automated tests

### Core Features

- learning goal setup and update
- knowledge node save / confirm / query
- chapter bootstrap and chapter-step progression
- daily task generation
- theory and demo Markdown material generation
- local learning-space persistence
- demo submission and AI evaluation
- evaluation report and next-step note generation
- learner-oriented workflow APIs
- Feishu event callback skeleton

### Learning Space

Generated materials are stored as Markdown files under a local learning-space structure, for example:

```text
verilearn/learning-space/
  spring-boot/
    01-spring-basics/
      user/
        theory/
          theory.md
        demo/
          demo-task.md
        summary/
          evaluation-report.md
          next-step.md
```

### Feishu Status

The repository already includes the **Feishu code integration layer**:

- event callback endpoint
- command parsing
- text reply client
- card preview / callback skeleton

However, **real Feishu platform integration is not finished yet**.  
The code side is ready; the actual platform-side webhook/public URL verification still remains.

### AI Responsibility

In VeriLearn, AI is responsible for:

- generating theory materials
- generating demo guides
- analyzing user submissions
- generating evaluation reports
- generating next-step suggestions

AI does **not** directly:

- update the database
- create files by itself
- call backend APIs
- send Feishu messages

Those actions are executed by the backend so that the workflow stays controllable and testable.

### Tech Stack

- Java 17
- Spring Boot 3.5.13
- Maven 3.9.x
- MySQL 8.x
- MyBatis-Plus 3.5.15
- SpringDoc OpenAPI / Swagger UI
- DeepSeek Chat Completions API
- JUnit 5

### Quick Start

#### Requirements

- JDK 17
- Maven 3.9.x
- MySQL 8.x
- Windows (current primary deployment target)

#### Create database

```sql
CREATE DATABASE verilearn DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### Run

```bash
mvn -s verilearn/maven-settings.xml -f verilearn/pom.xml spring-boot:run
```

#### Test

```bash
mvn -s verilearn/maven-settings.xml -f verilearn/pom.xml test
```

#### API Docs

```text
http://localhost:8080/swagger-ui/index.html
```

### Key APIs

- `POST /api/learners/setup`
- `GET /api/learners/{feishuOpenId}/today-task`
- `GET /api/learners/{feishuOpenId}/dashboard`
- `GET /api/learners/{feishuOpenId}/current-context`
- `GET /api/chapters/{chapterId}`
- `GET /api/materials/{materialId}/content`
- `POST /api/chapters/{chapterId}/steps/submit`
- `POST /api/chapters/{chapterId}/demo-evaluations`
- `POST /api/tasks/{taskId}/submit`
- `POST /api/tasks/{taskId}/validation-items/generate`
- `POST /api/feishu/events`

### Test Status

Current local automated result:

- `25` tests
- `0` failures
- `0` errors

### Roadmap

- real Feishu platform integration
- stronger learning-space navigation
- richer chapter-driven workflow
- stronger AI context management
- richer review and learning artifact support

### License

No open-source license has been added yet.

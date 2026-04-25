# VeriLearn

[中文](#中文说明) | [English](#english)

---

## 中文说明

### 项目简介

VeriLearn 是一个面向自学场景的验证式学习系统后端项目。

它关注的核心问题不是“如何提供更多学习资料”，而是“如何让学习过程形成可规划、可验证、可记录、可调整的闭环”。

当前仓库包含基于 Spring Boot 的后端 MVP，重点放在：

- 学习目标设置
- 数据持久化
- 统一接口规范
- 后续任务生成与学习验证所需的基础数据链路

### 背景

很多学习产品擅长提供资源，但不擅长回答下面这些问题：

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

当前已经落地的核心链路是：

```text
提交学习目标
  -> 查找或创建学习用户
  -> 创建或更新学习目标
  -> 写入 MySQL
  -> 返回统一 JSON 响应
```

### 项目状态

项目当前处于 MVP 持续开发阶段。

已完成：

- Spring Boot 后端工程骨架
- 统一响应结构
- 全局异常处理
- MySQL 接入
- 用户、学习目标、知识点节点三张核心表骨架
- 学习目标创建与更新接口
- 控制层与持久化链路自动化测试

计划中：

- 知识树生成
- 每日任务生成
- 学习验证流程
- 进度查询
- 飞书接入

### 功能特性

当前后端能力包括：

- `GET /ping`
  - 服务健康检查接口

- 统一响应结构
  - 标准 `code / message / data` 返回格式

- 全局异常处理
  - 统一处理控制层异常响应

- `POST /api/goals`
  - 按 `feishuOpenId` 创建或更新学习目标

- MySQL 持久化
  - 已验证真实数据库读写链路

- 核心数据模型
  - `learner_user`
  - `learning_goal`
  - `knowledge_node`

### 架构说明

当前项目采用标准单体分层结构：

```text
Controller
  -> Service
  -> Mapper
  -> MySQL
```

以学习目标功能为例：

```text
GoalController
  -> GoalServiceImpl
  -> LearnerUserMapper / LearningGoalMapper
  -> learner_user / learning_goal
```

职责划分：

- Controller 负责接收 HTTP 请求与返回响应
- Service 负责业务规则与流程控制
- Mapper 负责数据库访问
- MySQL 负责数据持久化

### 技术栈

- Java 17
- Spring Boot 3.5.13
- Maven 3.9.x
- MySQL 8.x
- MyBatis-Plus 3.5.15
- JUnit 5

技术选型原则：

- 保持单体工程简单可控
- 先完成后端闭环，再扩展更复杂能力
- 保持结构清晰，便于学习、展示和后续迭代

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

包职责：

- `common`：统一响应、异常处理、健康检查等通用能力
- `user`：学习用户领域
- `goal`：学习目标领域
- `knowledge`：知识结构领域
- `task`：每日任务领域预留
- `validation`：学习验证领域预留
- `progress`：进度查询领域预留
- `ai`：模型接入能力预留
- `infra`：基础设施与第三方接入预留

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

主配置文件：

- `verilearn/src/main/resources/application.yml`

#### 3. 启动项目

```bash
cd verilearn
mvn spring-boot:run
```

#### 4. 运行测试

```bash
cd verilearn
mvn test
```

### 接口概览

#### 健康检查

`GET /ping`

返回：

```text
VeriLearn API is running
```

#### 创建或更新学习目标

`POST /api/goals`

请求示例：

```json
{
  "feishuOpenId": "demo-user-001",
  "topic": "Java backend",
  "targetLevel": "intern",
  "dailyMinutes": 180
}
```

响应示例：

```json
{
  "code": 0,
  "message": "goal saved successfully",
  "data": {
    "userId": 1,
    "goalId": 1,
    "feishuOpenId": "demo-user-001",
    "topic": "Java backend",
    "targetLevel": "intern",
    "dailyMinutes": 180,
    "status": "ACTIVE"
  }
}
```

### 测试

当前自动化测试包括：

- `DemoControllerTest`
  - 验证统一响应结构与全局异常处理

- `GoalControllerTest`
  - 验证学习目标创建与更新链路

- `DatabaseSmokeTest`
  - 验证数据库连接与基础读写操作

### Roadmap

- 支持学习目标对应的知识点生成
- 支持每日任务生成
- 支持学习验证问题流转
- 支持进度查询接口
- 支持飞书机器人回调接入

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

Its core focus is not simply delivering more learning content, but turning learning into a process that can be planned, verified, recorded, and adjusted by the system.

This repository currently contains a Spring Boot based backend MVP focused on:

- learning goal setup
- persistence
- unified API conventions
- the core data flow needed for future task generation and validation

### Background

Many learning products provide content well, but do not answer questions such as:

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
  -> persist into MySQL
  -> return unified JSON response
```

### Project Status

The project is currently in active MVP development.

Implemented:

- Spring Boot backend scaffold
- unified response model
- global exception handling
- MySQL integration
- core schema for learner, goal, and knowledge node
- goal creation and update API
- automated tests for controller and persistence paths

Planned:

- knowledge tree generation
- daily task generation
- learning validation flow
- progress query
- Feishu integration

### Features

Current backend capabilities include:

- `GET /ping`
  - health check endpoint

- Unified API response
  - standard `code / message / data` format

- Global exception handling
  - centralized controller-layer error handling

- `POST /api/goals`
  - create or update a learning goal by `feishuOpenId`

- MySQL persistence
  - real database read/write flow already verified

- Core schema
  - `learner_user`
  - `learning_goal`
  - `knowledge_node`

### Architecture

The project currently follows a standard layered monolith structure:

```text
Controller
  -> Service
  -> Mapper
  -> MySQL
```

Current goal flow example:

```text
GoalController
  -> GoalServiceImpl
  -> LearnerUserMapper / LearningGoalMapper
  -> learner_user / learning_goal
```

Responsibilities:

- Controller handles HTTP request and response
- Service handles business rules and flow
- Mapper handles persistence
- MySQL stores data

### Tech Stack

- Java 17
- Spring Boot 3.5.13
- Maven 3.9.x
- MySQL 8.x
- MyBatis-Plus 3.5.15
- JUnit 5

Selection principles:

- keep the monolith simple and manageable
- complete the backend core loop before adding heavier capabilities
- maintain a clear structure for learning, presentation, and iteration

### Repository Structure

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

Package responsibilities:

- `common`: shared response model, exception handling, health endpoint
- `user`: learner domain
- `goal`: learning goal domain
- `knowledge`: knowledge structure domain
- `task`: daily task placeholder
- `validation`: validation placeholder
- `progress`: progress query placeholder
- `ai`: model integration placeholder
- `infra`: infrastructure and third-party integration placeholder

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

The current local configuration expects:

- database: `verilearn`
- username: `root`
- password: `root`

Main config file:

- `verilearn/src/main/resources/application.yml`

#### 3. Run the application

```bash
cd verilearn
mvn spring-boot:run
```

#### 4. Run tests

```bash
cd verilearn
mvn test
```

### API Overview

#### Health Check

`GET /ping`

Response:

```text
VeriLearn API is running
```

#### Create or Update Goal

`POST /api/goals`

Request example:

```json
{
  "feishuOpenId": "demo-user-001",
  "topic": "Java backend",
  "targetLevel": "intern",
  "dailyMinutes": 180
}
```

Response example:

```json
{
  "code": 0,
  "message": "goal saved successfully",
  "data": {
    "userId": 1,
    "goalId": 1,
    "feishuOpenId": "demo-user-001",
    "topic": "Java backend",
    "targetLevel": "intern",
    "dailyMinutes": 180,
    "status": "ACTIVE"
  }
}
```

### Testing

Current automated tests include:

- `DemoControllerTest`
  - verifies unified response and global exception handling

- `GoalControllerTest`
  - verifies goal creation and update flow

- `DatabaseSmokeTest`
  - verifies database connectivity and basic persistence operations

### Roadmap

- generate structured knowledge nodes for a goal
- support daily task generation
- support validation question flow
- support progress query APIs
- integrate with Feishu bot callbacks

### Contributing

If you want to contribute:

1. Fork the repository
2. Create a feature branch
3. Keep changes focused and readable
4. Add or update tests when behavior changes
5. Open a pull request with a clear description

### License

No open source license has been added yet.

# VeriLearn

VeriLearn 是一个面向自学场景的验证式学习系统后端项目。

它关注的重点不是“如何提供更多学习资料”，而是“如何让学习过程形成可验证、可记录、可调整的闭环”。
项目当前以 Java 后端工程为核心，先完成目标设置、数据持久化、统一接口规范和后续任务编排所需的基础能力。

## 项目介绍

很多学习产品擅长提供内容，但不擅长回答几个关键问题：

- 用户今天具体该学什么
- 学完之后是否真的掌握
- 当前结果应该如何影响下一步安排

VeriLearn 想做的是一套支持这类闭环的后端系统。当前版本先聚焦最小可运行能力：

- 学习目标设置
- 用户与目标数据持久化
- 知识点结构的数据库骨架
- 统一响应与异常处理
- 可验证的接口与数据库链路

## 核心流程

从产品视角看，VeriLearn 的目标流程如下：

```text
学习目标设置
    -> 知识点拆分
    -> 每日任务生成
    -> 学习验证
    -> 结果分流
    -> 后续安排调整
```

当前代码已经落地的是前半段能力：

```text
提交学习目标
    -> 查找或创建学习用户
    -> 创建或更新学习目标
    -> 写入 MySQL
    -> 返回统一 JSON 响应
```

## 技术栈

- `Java 17`
- `Spring Boot 3.5.13`
- `Maven 3.9.x`
- `MySQL 8.x`
- `MyBatis-Plus 3.5.15`
- `JUnit 5`

当前技术选型原则很明确：

- 保持单体工程简单可控
- 先完成后端闭环，再逐步扩展 AI、任务调度和第三方接入
- 让代码结构足够清晰，便于学习、演示和后续迭代

## 当前功能

当前仓库已经具备这些基础能力：

- `GET /ping`
  用于验证服务启动、接口通路和 Spring MVC 是否正常

- 统一响应结构
  普通接口统一返回 `code / message / data`

- 全局异常处理
  已具备最小异常兜底能力

- `POST /api/goals`
  支持按 `feishuOpenId` 创建或更新学习目标

- MySQL 数据接入
  已完成真实数据库连接与最小读写验证

- 核心表骨架
  当前已建立：
  - `learner_user`
  - `learning_goal`
  - `knowledge_node`

- 自动化测试
  当前已覆盖：
  - 统一响应与异常处理
  - 学习目标创建与更新
  - 数据库连接与读写冒烟测试

## 项目结构

仓库当前包含一个 Spring Boot 单体工程：

```text
VeriLearn/
├─ README.md
├─ .gitignore
└─ verilearn/
   ├─ pom.xml
   ├─ mvnw
   ├─ mvnw.cmd
   ├─ .mvn/
   └─ src/
      ├─ main/
      │  ├─ java/com/verilearn/
      │  │  ├─ common/
      │  │  ├─ user/
      │  │  ├─ goal/
      │  │  ├─ knowledge/
      │  │  ├─ task/
      │  │  ├─ validation/
      │  │  ├─ progress/
      │  │  ├─ ai/
      │  │  └─ infra/
      │  └─ resources/
      └─ test/
```

各包当前职责如下：

- `common`
  通用能力，如统一返回结构、全局异常处理、健康检查接口

- `user`
  学习用户基础信息

- `goal`
  学习目标设置与目标相关业务

- `knowledge`
  知识点结构与后续知识树能力预留

- `task`
  每日任务生成相关能力预留

- `validation`
  学习验证与验证结果处理能力预留

- `progress`
  学习进度查询能力预留

- `ai`
  模型调用与结构化输出能力预留

- `infra`
  第三方接入、配置与基础设施能力预留

## 模块关系

当前后端按典型单体工程方式组织：

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

这套结构的目的很明确：

- Controller 负责接收请求与返回响应
- Service 负责业务规则与流程控制
- Mapper 负责数据访问
- MySQL 负责持久化

## 数据模型概览

当前阶段已经落地的核心表如下。

### `learner_user`

表示学习用户。

关键字段：

- `id`
- `feishu_open_id`
- `created_at`
- `updated_at`

### `learning_goal`

表示用户当前的学习目标。

关键字段：

- `id`
- `user_id`
- `topic`
- `target_level`
- `daily_minutes`
- `status`

### `knowledge_node`

表示知识点结构中的节点。

关键字段：

- `id`
- `user_id`
- `goal_id`
- `parent_id`
- `node_name`
- `sequence_no`
- `status`

## 核心接口

### 健康检查

`GET /ping`

返回：

```text
VeriLearn API is running
```

### 学习目标设置

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

返回示例：

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

## 测试

当前测试覆盖三类基础能力：

- `DemoControllerTest`
  验证统一响应结构与异常处理

- `GoalControllerTest`
  验证学习目标的创建与更新链路

- `DatabaseSmokeTest`
  验证数据库连接、插入与查询链路

## 本地运行

### 准备数据库

先创建本地数据库：

```sql
CREATE DATABASE verilearn DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

当前本地开发配置使用：

- 数据库：`verilearn`
- 用户名：`root`
- 密码：`root`

### 启动项目

进入 `verilearn/` 目录后执行：

```bash
mvn spring-boot:run
```

### 运行测试

```bash
mvn test
```

## 说明

- 仓库只保留适合公开同步的工程内容
- 开发日记与过程性内部记录不进入仓库
- 当前项目采用“单体工程 + 按业务域分包”的组织方式，而不是 Maven 聚合工程

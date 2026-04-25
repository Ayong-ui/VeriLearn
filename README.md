# VeriLearn

VeriLearn 是一个面向自学场景的验证式学习系统后端项目。

它关注的不只是“给用户学习内容”，而是把下面这条链路做成系统闭环：

`目标设置 -> 知识点拆分 -> 每日任务 -> 学习验证 -> 结果分流 -> 后续安排`

项目当前以 Java 后端实现为主，目标是先完成一个结构清晰、可持续扩展、能够真实跑通核心链路的 MVP。

---

## 1. 项目简介

很多学习产品擅长提供资源，但不擅长判断“用户到底有没有真正掌握”。  
VeriLearn 想解决的问题是：

- 用户每天应该学什么
- 学完之后如何验证
- 验证结果如何影响下一步学习安排

当前版本先聚焦后端基础能力建设，包括：

- 学习目标设置
- 用户与目标数据持久化
- 知识点结构的数据库骨架
- 统一接口返回结构
- 全局异常处理
- 基础测试与数据库链路验证

---

## 2. 技术栈

当前项目采用：

- `Java 17`
- `Spring Boot 3.5.13`
- `Maven 3.9.x`
- `MySQL 8.x`
- `MyBatis-Plus 3.5.15`
- `JUnit 5`

当前技术选型的目标不是“堆技术”，而是保证：

- 工程结构清晰
- 学习成本可控
- 后续功能容易继续往下接

---

## 3. 当前功能

当前已经完成的核心能力包括：

- `GET /ping`
  用于验证服务启动、接口链路和 Spring MVC 是否正常

- 统一响应结构
  普通接口统一返回 `code / message / data`

- 全局异常处理
  当前已具备最小异常兜底能力

- `POST /api/goals`
  支持按 `feishuOpenId` 创建或更新学习目标

- MySQL 数据接入
  当前已经完成真实数据库连接与最小数据读写验证

- 核心表骨架
  当前已建立：
  - `learner_user`
  - `learning_goal`
  - `knowledge_node`

- 自动化测试
  当前已覆盖：
  - 接口统一返回测试
  - 目标设置接口测试
  - 数据库读写冒烟测试

---

## 4. 项目结构

仓库当前主要包含一个 Spring Boot 单体工程：

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

各包的职责大致如下：

- `common`
  放通用能力，例如统一返回结构、全局异常处理、健康检查接口

- `user`
  负责学习用户基础信息

- `goal`
  负责学习目标设置与目标相关业务

- `knowledge`
  负责知识点结构与后续知识树能力

- `task`
  负责后续每日任务生成

- `validation`
  负责后续学习验证与验证结果处理

- `progress`
  负责后续学习进度查询

- `ai`
  负责后续模型调用与结构化输出

- `infra`
  负责第三方接入、配置、基础设施类能力

---

## 5. 当前核心接口

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

---

## 6. 当前开发阶段

项目目前还处在 MVP 的早期阶段，重点在于：

- 把后端基础骨架搭稳
- 让核心数据链路先真实跑通
- 为后续知识点生成、任务生成、验证与分流留下清晰扩展点

下一阶段会继续向这些能力推进：

- 知识点拆分与保存
- 每日任务生成
- 学习验证与结果分流
- 飞书侧交互接入

---

## 7. 本地运行

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

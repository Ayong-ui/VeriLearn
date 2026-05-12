# VeriLearn

VeriLearn 是一个运行在飞书中的 AI 学习执行系统。

它不是资料库，不是普通问答机器人，也不是单纯的提醒 Bot。它更像一个学习教练：帮用户安排今天学什么、去哪里看理论、做什么 Demo、学完后提交什么结果，并根据反馈决定下一步是继续、补充还是复习。

当前仓库是 **V1 测试版后端实现**。这一版优先解决的不是“内容够不够多”，而是把学习过程做成一条可执行、可验证、可调整、可复习的闭环。

---

## 1. 产品是什么

从用户视角看，VeriLearn 的使用方式应该是：

1. 在飞书里开始一个学习主题，例如 `/start Java 后端`
2. 系统初始化学习目标、章节和默认学习材料
3. 用户每天接收“今天学什么”的任务
4. 用户查看理论材料和 Demo 指南
5. 用户在本地完成 Demo，并把结果或反馈发回系统
6. 系统调用 AI 评估掌握度，生成评估报告和下一步建议
7. 系统继续推进学习，或把章节送入复习

对应的产品主链路是：

```text
学习主题
  -> 章节
  -> 今日任务
  -> 理论文档
  -> Demo 实操
  -> 用户反馈
  -> AI 评估
  -> 下一步建议 / 复习
```

---

## 2. 为什么这样设计

VeriLearn 的核心判断是：

- 真正缺的不是更多资料，而是更稳定的学习推进机制
- 用户经常停在“看懂了”，而不是“做出来了”
- AI 更适合承担内容生成、内容压缩和反馈分析，而不是替用户学习
- 飞书适合作为学习入口，因为它天然适合推送、提醒和轻量交互

所以，这个项目没有把重点放在“再做一个内容平台”，而是放在：

- 如何生成今天的学习任务
- 如何把理论和 Demo 交给用户
- 如何回收用户结果
- 如何用 AI 评估并决定下一步

---

## 3. 底层交互原理

这个系统的底层交互可以概括成四层：

### 3.1 飞书交互层

负责用户入口和消息出口。

用户在飞书里做这些事：

- 发起学习主题
- 接收今天任务
- 查看当前章节
- 查看理论和 Demo 入口
- 提交完成反馈
- 接收评估结果和下一步建议

### 3.2 后端工作流层

负责整个学习流程的控制与编排。

后端决定：

- 当前用户学到哪一章
- 今天该做哪一步
- 哪些材料应该展示给用户
- 什么时候调用 AI
- AI 返回结果后怎么更新数据库、写文件、回复飞书

### 3.3 AI 能力层

AI 在当前设计里不是系统控制器，而是内容生成与反馈分析引擎。

AI 负责：

- 生成理论材料
- 生成 Demo 指南
- 分析用户提交的学习结果
- 生成评估报告
- 生成下一步建议

AI 不直接：

- 更新数据库
- 创建目录或写文件
- 发送飞书消息
- 调用后端接口

这些动作都由后端执行，这样系统更可控、更容易测试。

### 3.4 学习空间层

学习材料最终不是只停留在数据库里，而是会真正落成 Markdown 文件。

这是为了让系统不只是“会推任务”，而是能给用户一个真实的学习空间。

当前默认结构类似：

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

---

## 4. 数据库和文件系统分别做什么

这是项目里一个很重要的设计点。

### 数据库负责

- 用户
- 学习目标
- 知识点
- 章节
- 章节步骤
- 今日任务
- 验证项
- 评估状态
- 材料索引

也就是：**数据库负责状态、关系和流程控制。**

### 文件系统负责

- 理论文档
- Demo 任务说明
- 评估报告
- 下一步建议

也就是：**文件系统负责真正给用户看的内容。**

因此这个项目不是“数据库单存储”，而是：

**数据库管理流程，文件系统承载内容。**

---

## 5. 当前已经完成了什么

当前 V1 测试版后端已经具备这些能力：

- 学习目标初始化
- 知识点保存与确认
- 章节初始化
- 章节步骤推进
- 今日任务生成
- DeepSeek 理论材料生成
- Demo 指南生成
- 本地学习空间 Markdown 落盘
- Demo 结果提交
- AI 评估掌握度
- 评估报告与下一步建议生成
- 进度查询
- 当前学习上下文聚合
- 飞书事件接入代码骨架
- 飞书文本消息发送代码骨架
- 飞书卡片预览 / 回调骨架

当前更接近：

**可本地联调、可演示流程的学习执行系统测试版**

而不是已经完成真实飞书平台联调的成品。

---

## 6. 当前的可演示主流程

如果在本地演示，当前最适合展示这条链路：

1. `POST /api/learners/setup`
2. `GET /api/learners/{feishuOpenId}/today-task`
3. `GET /api/materials/{materialId}/content`
4. `POST /api/learners/{feishuOpenId}/chapters/{chapterId}/demo-evaluations`
5. `GET /api/learners/{feishuOpenId}/current-context`
6. `GET /api/learners/{feishuOpenId}/dashboard`

这条链能完整展示：

- 今天学什么
- 去哪里看理论
- 去哪里做 Demo
- Demo 做完后怎么评估
- 系统如何生成评估报告与下一步建议

---

## 7. 关键接口

### 学习工作流

- `POST /api/learners/setup`
- `GET /api/learners/{feishuOpenId}/today-task`
- `GET /api/learners/{feishuOpenId}/progress`
- `GET /api/learners/{feishuOpenId}/chapters`
- `GET /api/learners/{feishuOpenId}/dashboard`
- `GET /api/learners/{feishuOpenId}/current-context`
- `POST /api/learners/{feishuOpenId}/chapters/{chapterId}/demo-evaluations`

### 章节与材料

- `GET /api/chapters/{chapterId}`
- `POST /api/chapters/{chapterId}/steps/submit`
- `GET /api/materials/{materialId}/content`

### 任务与验证

- `POST /api/tasks/{taskId}/submit`
- `POST /api/tasks/{taskId}/validation-items/generate`

### 飞书代码接入骨架

- `POST /api/feishu/events`
- `GET /api/feishu/cards/learners/{openId}/today-task`
- `GET /api/feishu/cards/learners/{openId}/dashboard`
- `POST /api/feishu/cards/callbacks`

---

## 8. 飞书当前做到哪一步了

这部分需要特别说明清楚。

当前仓库已经完成的是：

- 飞书事件接收接口
- 命令解析骨架
- 文本回复客户端
- 卡片预览与回调骨架

但这并不等于：

- 真实飞书机器人已经完全打通

当前状态应该准确理解为：

**飞书代码适配层已完成，真实平台联调尚未完成。**

真实联调后续还需要：

- 配置真实 `App ID / App Secret / Verification Token`
- 公网回调地址
- 飞书后台事件订阅
- 真机发消息验证

---

## 9. 技术栈

- Java 17
- Spring Boot 3.5.13
- Maven 3.9.x
- MySQL 8.x
- MyBatis-Plus 3.5.15
- SpringDoc OpenAPI / Swagger UI
- DeepSeek Chat Completions API
- JUnit 5

---

## 10. 项目结构

```text
VeriLearn/
├── README.md
├── .gitignore
└── verilearn/
    ├── pom.xml
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

---

## 11. 本地启动

### 环境要求

- JDK 17
- Maven 3.9.x
- MySQL 8.x
- Windows

### 创建数据库

```sql
CREATE DATABASE verilearn DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 常用环境变量

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

### 启动项目

```bash
mvn -s verilearn/maven-settings.xml -f verilearn/pom.xml spring-boot:run
```

### 运行测试

```bash
mvn -s verilearn/maven-settings.xml -f verilearn/pom.xml test
```

### 查看 Swagger

```text
http://localhost:8080/swagger-ui/index.html
```

---

## 12. 测试状态

当前本地自动化测试结果：

- `26` tests
- `0` failures
- `0` errors

覆盖范围包括：

- 目标设置
- 知识点保存与确认
- 章节初始化与推进
- 材料查看
- 今日任务生成
- Demo 结果评估
- 进度查询
- 工作流聚合接口
- 飞书事件入口骨架
- 数据库读写冒烟测试

---

## 13. 接下来要做什么

当前离“真实可演示产品”最近的后续工作主要是：

1. 把飞书 `/today` 正式做成任务卡片
2. 让飞书消息真正引用理论 / Demo / 评估 / 下一步的内容入口
3. 打通真实飞书平台联调
4. 收口最终演示脚本

---

## 14. English Summary

VeriLearn is an AI learning execution system designed for Feishu-based learning workflows.

This repository currently contains the **V1 test-build backend**, with support for:

- learning goal setup
- chapter bootstrap
- daily task generation
- Markdown learning materials
- local demo submission
- AI evaluation
- current-context aggregation
- Feishu integration skeleton

The current version is **demo-ready on the backend side**, but **real Feishu platform linkage is still pending**.

---

## License

No open-source license has been added yet.

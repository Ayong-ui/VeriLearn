# VeriLearn

VeriLearn 是一个运行在飞书中的 **AI 自学执行系统**。  
它不是课程平台，不是普通问答机器人，也不是只会提醒打卡的 Bot。

它解决的是另一类问题：

- 今天该学什么
- 理论材料在哪里看
- Demo 在哪里做
- 做完以后怎么提交结果
- 系统如何根据完成情况判断用户是否真正掌握
- 下一步应该继续、补充还是复习

当前仓库是 **V1 测试版后端实现**。  
这一版优先打通的是「学习闭环」和「本地可演示能力」，而不是先做复杂前端或真实飞书平台联调。

---

## 1. 产品定位

VeriLearn 的核心定位是：

> 帮助用户围绕一个学习主题，完成「理论自学 -> Demo 实操 -> 结果提交 -> AI 评估 -> 下一步建议」的闭环。

从用户视角看，使用流程应该是：

1. 在飞书中开始一个学习主题
2. 系统生成章节、理论材料和 Demo 任务
3. 用户查看理论文档和 Demo 指南
4. 用户在本地完成 Demo
5. 用户提交结果或反馈
6. AI 评估掌握情况
7. 系统生成评估报告与下一步建议
8. 系统继续推进学习或安排复习

这里没有“老师角色”：

- 用户是自学者
- AI 负责内容生成与结果评估
- 后端负责流程控制、状态管理和文件落盘
- 飞书负责入口与出口

---

## 2. 为什么这样设计

VeriLearn 背后的核心判断是：

- 真正缺的往往不是更多资料，而是更稳定的学习推进机制
- 很多学习中断都发生在“看懂了”但“做不出来”这一步
- AI 更适合承担内容生成、内容压缩和结果评估，而不是替用户学习
- 飞书适合作为入口，因为它天然适合消息推送、提醒和轻量交互

所以系统重点不是“堆内容”，而是：

- 生成今天的任务
- 把理论和 Demo 交给用户
- 回收用户结果
- 调用 AI 做评估
- 决定下一步如何推进

---

## 3. 底层交互原理

这个项目的底层交互可以拆成四层。

### 3.1 飞书交互层

飞书负责：

- 接收用户命令
- 推送今日任务
- 提供材料入口
- 接收用户反馈
- 返回评估结果和下一步建议

当前代码层已经具备：

- 飞书事件接收接口
- 文本命令解析
- 文本消息发送客户端
- 卡片预览与卡片回调骨架

真实飞书平台联调仍未完成，这部分会在后续收口。

### 3.2 后端工作流层

后端负责：

- 维护学习主题、章节、步骤、任务、进度
- 判断今天该学什么
- 决定何时调用 AI
- 把 AI 返回结果落到数据库和 Markdown 文件
- 再把结果组织成飞书可消费的文本或卡片

也就是说：

> AI 不直接控制系统流程，后端才是总控者。

### 3.3 AI 能力层

AI 在 VeriLearn 中的职责是：

- 生成理论材料
- 生成 Demo 指南
- 评估用户提交结果
- 生成评估报告
- 生成下一步建议

AI 不直接做这些事：

- 更新数据库
- 写文件
- 发飞书消息
- 调后端接口

这些都由后端执行。

### 3.4 学习空间层

真正给用户看的内容不会只存在数据库里，而是会落成本地 Markdown 文件。

典型目录结构类似：

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

这样系统不只是“会推任务”，而是真的给用户提供一个学习空间。

---

## 4. 数据库和文件系统分别负责什么

### 数据库负责

- 用户
- 学习目标
- 知识点
- 章节
- 章节步骤
- 今日任务
- 验证项
- 提交记录
- 材料索引
- 评估状态

也就是说：

> 数据库负责状态、关系和流程控制。

### 文件系统负责

- 理论文档
- Demo 指南
- 评估报告
- 下一步建议

也就是说：

> 文件系统负责真正给用户看的学习内容。

当前项目采用的是：

> 数据库管理流程，Markdown 文件承载内容。

---

## 5. AI 提供方切换能力

当前项目默认使用 **DeepSeek** 作为系统级 AI 提供方。

为了避免学习流程与单一模型厂商强耦合，当前版本已经补上了 **用户级 AI 提供方配置能力**：

- 默认回退到系统级 DeepSeek
- 支持保存用户自己的模型配置
- 支持激活某个已保存配置
- 后端统一根据当前用户配置路由到对应模型

当前已经支持的 provider 类型：

- `DEEPSEEK`
- `OPENAI`
- `OPENAI_COMPATIBLE`

### 安全策略

为了避免敏感信息泄露，当前实现遵循这些原则：

- API Key 不明文落库
- 使用服务端主密钥进行加密存储
- 查询配置时只返回掩码 Key
- 业务层不直接读取明文密钥

当前主密钥来自环境变量：

- `VERILEARN_SECRET_MASTER_KEY`

当前后端接口已经具备：

- `GET /api/learners/{feishuOpenId}/ai-provider-configs/current`
- `GET /api/learners/{feishuOpenId}/ai-provider-configs`
- `POST /api/learners/{feishuOpenId}/ai-provider-configs`
- `POST /api/learners/{feishuOpenId}/ai-provider-configs/{configId}/activate`

### 安全配置入口

正式产品方案中，不推荐用户直接在普通聊天框里发送 API Key。  
当前本地演示版已经补了一个安全配置页入口：

- `GET /ai/provider-config-page?openId={feishuOpenId}`

这个页面会：

- 展示当前生效模型
- 展示已保存模型列表
- 允许新增一份模型配置
- 允许切换到某个已保存模型
- 通过后端接口安全保存配置

后续真实飞书联调时，这个入口可以挂到飞书卡片按钮上。

---

## 6. 当前已经完成的能力

当前 V1 测试版后端已经具备：

- 学习目标初始化
- 知识点保存与确认
- 章节初始化
- 章节步骤推进
- 今日任务生成
- 理论材料生成
- Demo 指南生成
- 学习空间 Markdown 落盘
- Demo 结果提交
- AI 掌握度评估
- 评估报告与下一步建议生成
- 进度查询
- 当前学习上下文聚合
- 飞书事件接入代码骨架
- 飞书文本消息发送代码骨架
- 飞书卡片预览 / 回调骨架
- 飞书侧 AI 模型查看 / 切换命令与卡片
- 用户级 AI 提供方配置与路由
- 本地安全 AI 配置页

当前更准确的阶段是：

> 可本地运行、可本地测试、可做完整演示链路的后端测试版

而不是：

> 已经完成真实飞书平台联调的成品

---

## 7. 当前可演示主流程

如果现在在本地演示，最适合走这条链路：

1. `POST /api/learners/setup`
2. `GET /api/learners/{feishuOpenId}/today-task`
3. `GET /api/materials/{materialId}/content`
4. `POST /api/learners/{feishuOpenId}/demo-feedback/current`
5. `GET /api/learners/{feishuOpenId}/current-context`
6. `GET /api/learners/{feishuOpenId}/dashboard`
7. `GET /api/learners/{feishuOpenId}/ai-provider-configs/current`
8. `GET /ai/provider-config-page?openId={feishuOpenId}`

这条链能完整展示：

- 今天学什么
- 去哪里看理论
- 去哪里看 Demo
- Demo 做完后如何评估
- 如何生成下一步建议
- 如何查看和切换当前 AI 提供方

---

## 8. 关键接口

### 学习工作流

- `POST /api/learners/setup`
- `GET /api/learners/{feishuOpenId}/today-task`
- `GET /api/learners/{feishuOpenId}/progress`
- `GET /api/learners/{feishuOpenId}/chapters`
- `GET /api/learners/{feishuOpenId}/dashboard`
- `GET /api/learners/{feishuOpenId}/current-context`
- `POST /api/learners/{feishuOpenId}/demo-feedback/current`
- `POST /api/learners/{feishuOpenId}/chapters/{chapterId}/demo-evaluations`

### 章节与材料

- `GET /api/chapters/{chapterId}`
- `POST /api/chapters/{chapterId}/steps/submit`
- `GET /api/materials/{materialId}/content`

### 任务与验证

- `POST /api/tasks/{taskId}/submit`
- `POST /api/tasks/{taskId}/validation-items/generate`

### AI 提供方配置

- `GET /api/learners/{feishuOpenId}/ai-provider-configs/current`
- `GET /api/learners/{feishuOpenId}/ai-provider-configs`
- `POST /api/learners/{feishuOpenId}/ai-provider-configs`
- `POST /api/learners/{feishuOpenId}/ai-provider-configs/{configId}/activate`
- `GET /ai/provider-config-page?openId={feishuOpenId}`

### 飞书接入骨架

- `POST /api/feishu/events`
- `GET /api/feishu/local-setup`
- `GET /api/feishu/cards/learners/{openId}/today-task`
- `GET /api/feishu/cards/learners/{openId}/dashboard`
- `GET /api/feishu/cards/learners/{openId}/current-context`
- `GET /api/feishu/cards/learners/{openId}/ai-provider`
- `POST /api/feishu/cards/callbacks`

---

## 9. 技术栈

- Java 17
- Spring Boot 3.5.13
- Maven 3.9.x
- MySQL 8.x
- MyBatis-Plus 3.5.15
- SpringDoc OpenAPI / Swagger UI
- DeepSeek / OpenAI compatible chat-completions integration
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
    ├── src/
    │   ├── main/
    │   │   ├── java/com/verilearn/
    │   │   │   ├── ai/
    │   │   │   ├── chapter/
    │   │   │   ├── common/
    │   │   │   ├── goal/
    │   │   │   ├── infra/
    │   │   │   ├── knowledge/
    │   │   │   ├── progress/
    │   │   │   ├── task/
    │   │   │   ├── user/
    │   │   │   ├── validation/
    │   │   │   └── workflow/
    │   │   └── resources/
    │   └── test/
    └── learning-space/   (runtime-generated, ignored by git)
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
- `VERILEARN_AI_PROVIDER_TYPE`
- `VERILEARN_AI_BASE_URL`
- `VERILEARN_AI_API_KEY`
- `VERILEARN_AI_MODEL`
- `VERILEARN_SECRET_MASTER_KEY`
- `VERILEARN_FEISHU_BASE_URL`
- `VERILEARN_FEISHU_APP_ID`
- `VERILEARN_FEISHU_APP_SECRET`
- `VERILEARN_FEISHU_VERIFICATION_TOKEN`
- `VERILEARN_LEARNING_SPACE_ROOT`

### 启动项目

```bash
mvn -s verilearn/maven-settings.xml -f verilearn/pom.xml spring-boot:run
```

### Windows 下配置飞书环境变量

可以直接运行：

```powershell
.\verilearn\scripts\feishu\set-feishu-env.ps1 -AppId "你的AppId" -AppSecret "你的AppSecret" -VerificationToken "你的VerificationToken"
```

### 查看本地飞书联调检查清单

项目启动后可以运行：

```powershell
.\verilearn\scripts\feishu\show-feishu-local-setup.ps1
```

或者直接访问：

```text
GET /api/feishu/local-setup
```

它会告诉你：

- 当前是否已经配置 `APP_ID / APP_SECRET / VERIFICATION_TOKEN`
- 当前是否具备真实消息发送能力
- 回调地址模板应该填什么
- 下一步还缺哪些平台侧配置

### 运行测试

```bash
mvn -s verilearn/maven-settings.xml -f verilearn/pom.xml test
```

### 查看 Swagger

[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## 12. 测试状态

当前本地自动化测试结果：

- `39` tests
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
- 飞书卡片预览与回调
- AI 提供方配置与激活切换
- 安全配置页
- 数据库读写冒烟测试

---

## 13. 接下来要做什么

距离“真实可演示产品”最近的后续工作主要是：

1. 把飞书 `/today` 正式做成任务卡片
2. 让飞书文本或卡片真正引用理论 / Demo / 评估 / 下一步内容入口
3. 把安全 AI 配置入口真正接到飞书卡片跳转里
4. 打通真实飞书平台联调
5. 收口最终演示脚本

如果只差飞书真实联调，当前最先要完成的是：

1. 通过 `set-feishu-env.ps1` 或系统环境变量配置真实 `APP_ID / APP_SECRET / VERIFICATION_TOKEN`
2. 用 `GET /api/feishu/local-setup` 检查本地缺口
3. 准备一个公网地址，把 `{PUBLIC_BASE_URL}/api/feishu/events` 配到飞书事件订阅
4. 至少订阅 `im.message.receive_v1`
5. 再做真实收发消息验证

---

## 14. License

No open-source license has been added yet.

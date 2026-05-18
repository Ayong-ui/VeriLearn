package com.verilearn.workflow.service.impl;

import com.verilearn.workflow.service.LocalDemoPageService;
import org.springframework.stereotype.Service;

@Service
public class LocalDemoPageServiceImpl implements LocalDemoPageService {

    @Override
    public String buildPage() {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>VeriLearn 本地演示入口</title>
                    <style>
                        :root {
                            --bg: #f4f7fb;
                            --panel: #ffffff;
                            --text: #1f2937;
                            --muted: #526071;
                            --primary: #2563eb;
                            --primary-hover: #1d4ed8;
                            --border: #d7deea;
                            --success: #0f766e;
                            --warning-bg: #fff8e8;
                            --warning-border: #d6a000;
                        }
                        * { box-sizing: border-box; }
                        body {
                            margin: 0;
                            padding: 24px;
                            font-family: "Microsoft YaHei", sans-serif;
                            background: linear-gradient(180deg, #eef4ff 0%, var(--bg) 100%);
                            color: var(--text);
                        }
                        .container {
                            max-width: 1100px;
                            margin: 0 auto;
                            display: grid;
                            gap: 20px;
                        }
                        .panel {
                            background: var(--panel);
                            border-radius: 18px;
                            padding: 20px 22px;
                            box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
                        }
                        h1, h2, h3 { margin-top: 0; }
                        p, li { color: var(--muted); line-height: 1.65; }
                        .grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                            gap: 16px;
                        }
                        label {
                            display: block;
                            font-weight: 600;
                            margin-bottom: 8px;
                        }
                        input, textarea {
                            width: 100%;
                            padding: 12px 14px;
                            border: 1px solid var(--border);
                            border-radius: 12px;
                            font-size: 14px;
                            background: #fcfdff;
                            margin-bottom: 14px;
                        }
                        textarea {
                            min-height: 110px;
                            resize: vertical;
                        }
                        button, a.button {
                            display: inline-flex;
                            align-items: center;
                            justify-content: center;
                            gap: 8px;
                            border: none;
                            border-radius: 12px;
                            padding: 11px 16px;
                            background: var(--primary);
                            color: #ffffff;
                            text-decoration: none;
                            cursor: pointer;
                            font-size: 14px;
                            font-weight: 600;
                        }
                        button:hover, a.button:hover {
                            background: var(--primary-hover);
                        }
                        button.secondary, a.button.secondary {
                            background: #eef2ff;
                            color: #1e3a8a;
                            border: 1px solid #c7d2fe;
                        }
                        button.secondary:hover, a.button.secondary:hover {
                            background: #dbeafe;
                        }
                        .button-row {
                            display: flex;
                            flex-wrap: wrap;
                            gap: 10px;
                        }
                        .hint {
                            background: #eff6ff;
                            border-left: 4px solid var(--primary);
                            border-radius: 10px;
                            padding: 14px 16px;
                        }
                        .warning {
                            background: var(--warning-bg);
                            border-left: 4px solid var(--warning-border);
                            border-radius: 10px;
                            padding: 14px 16px;
                        }
                        .result {
                            background: #0f172a;
                            color: #e2e8f0;
                            border-radius: 14px;
                            padding: 16px;
                            overflow: auto;
                            white-space: pre-wrap;
                            word-break: break-word;
                            min-height: 180px;
                        }
                        .link-list {
                            display: grid;
                            gap: 10px;
                        }
                        .link-item {
                            display: flex;
                            flex-direction: column;
                            gap: 6px;
                            border: 1px solid var(--border);
                            border-radius: 12px;
                            padding: 14px;
                            background: #fbfcff;
                        }
                        .link-item strong {
                            color: var(--text);
                        }
                        .muted {
                            color: var(--muted);
                            font-size: 13px;
                        }
                        .empty {
                            color: var(--muted);
                            font-size: 14px;
                            padding: 14px;
                            border: 1px dashed var(--border);
                            border-radius: 12px;
                        }
                    </style>
                </head>
                <body>
                <div class="container">
                    <section class="panel">
                        <h1>VeriLearn 本地演示入口</h1>
                        <p>这个页面用于在不接真实飞书的前提下，本地完整演示一条产品链路：初始化学习主题、查看今日任务、打开理论和 Demo 文档、提交 Demo 反馈、查看评估结果和下一步建议、打开 AI 安全配置页。</p>
                        <div class="hint">
                            推荐演示顺序：
                            <strong>/start → /today → 查看 theory.md / demo-task.md → 提交 Demo 反馈 → 查看 evaluation-report.md / next-step.md → 打开 AI 安全配置页</strong>
                        </div>
                    </section>

                    <section class="panel">
                        <h2>1. 初始化本地学习会话</h2>
                        <div class="grid">
                            <div>
                                <label for="openId">Feishu OpenId</label>
                                <input id="openId" value="ou_demo_local" />
                            </div>
                            <div>
                                <label for="topic">学习主题</label>
                                <input id="topic" value="Java 后端" />
                            </div>
                            <div>
                                <label for="targetLevel">目标水平</label>
                                <input id="targetLevel" value="实习可面试" />
                            </div>
                            <div>
                                <label for="dailyMinutes">每日学习分钟数</label>
                                <input id="dailyMinutes" value="45" />
                            </div>
                        </div>
                        <div class="button-row">
                            <button id="setupButton">初始化学习主题</button>
                            <button class="secondary" id="refreshButton">刷新当前状态</button>
                            <a class="button secondary" id="openAiConfigButton" href="#" target="_blank" rel="noopener noreferrer">打开 AI 安全配置页</a>
                        </div>
                    </section>

                    <section class="panel">
                        <h2>2. 当前学习材料导航</h2>
                        <div id="materialLinks" class="link-list">
                            <div class="empty">初始化或刷新后，这里会显示理论文档、Demo 指南、评估报告和下一步建议的查看入口。</div>
                        </div>
                    </section>

                    <section class="panel">
                        <h2>3. 提交 Demo 反馈</h2>
                        <p>这里模拟用户在本地完成 Demo 后，通过系统提交结果。后端会调用 AI 评估，再生成 <code>evaluation-report.md</code> 和 <code>next-step.md</code>。</p>
                        <label for="submissionSummary">完成说明</label>
                        <textarea id="submissionSummary">我已经完成了今天的 Demo，并理解了为什么启动类需要放在根包下。</textarea>
                        <label for="codeSnippet">代码片段（可选）</label>
                        <textarea id="codeSnippet">@RestController
public class PingController {
    @GetMapping("/ping")
    public String ping() {
        return "ok";
    }
}</textarea>
                        <label for="question">仍有疑问（可选）</label>
                        <textarea id="question">为什么 Spring Boot 能默认扫描到这个 Controller？</textarea>
                        <div class="button-row">
                            <button id="submitDemoButton">提交 Demo 反馈</button>
                        </div>
                    </section>

                    <section class="panel">
                        <h2>4. 当前返回结果</h2>
                        <div class="warning">
                            这里展示当前接口返回的原始 JSON，方便本地联调与排错。正式产品里，飞书文本和卡片会消费同一套后端数据。
                        </div>
                        <pre id="result" class="result">请先点击“初始化学习主题”或“刷新当前状态”。</pre>
                    </section>
                </div>

                <script>
                    const resultElement = document.getElementById("result");
                    const materialLinksElement = document.getElementById("materialLinks");

                    function openId() {
                        return document.getElementById("openId").value.trim();
                    }

                    function setAiConfigLink() {
                        const id = encodeURIComponent(openId() || "ou_demo_local");
                        document.getElementById("openAiConfigButton").href = `/ai/provider-config-page?openId=${id}`;
                    }

                    function pretty(data) {
                        return JSON.stringify(data, null, 2);
                    }

                    async function readApi(response) {
                        const payload = await response.json();
                        if (!response.ok || payload.code !== 0) {
                            throw new Error(payload.message || "请求失败");
                        }
                        return payload.data;
                    }

                    function renderMaterials(task, context) {
                        const materials = [];
                        if (task?.theoryViewUrl) {
                            materials.push({
                                title: "理论文档",
                                filePath: task.theoryFilePath,
                                viewUrl: task.theoryViewUrl,
                                contentUrl: task.theoryContentUrl
                            });
                        }
                        if (task?.demoViewUrl) {
                            materials.push({
                                title: "Demo 指南",
                                filePath: task.demoFilePath,
                                viewUrl: task.demoViewUrl,
                                contentUrl: task.demoContentUrl
                            });
                        }
                        if (context?.evaluationViewUrl) {
                            materials.push({
                                title: "评估报告",
                                filePath: context.evaluationFilePath,
                                viewUrl: context.evaluationViewUrl,
                                contentUrl: context.evaluationContentUrl
                            });
                        }
                        if (context?.nextStepViewUrl) {
                            materials.push({
                                title: "下一步建议",
                                filePath: context.nextStepFilePath,
                                viewUrl: context.nextStepViewUrl,
                                contentUrl: context.nextStepContentUrl
                            });
                        }

                        if (materials.length === 0) {
                            materialLinksElement.innerHTML = "<div class='empty'>当前还没有可展示的材料，请先初始化学习主题并刷新当前状态。</div>";
                            return;
                        }

                        materialLinksElement.innerHTML = materials.map(item => `
                            <div class="link-item">
                                <strong>${escapeHtml(item.title)}</strong>
                                <div class="muted">${escapeHtml(item.filePath || "未生成文件路径")}</div>
                                <div class="button-row">
                                    <a class="button" href="${escapeHtml(item.viewUrl)}" target="_blank" rel="noopener noreferrer">打开阅读页</a>
                                    <a class="button secondary" href="${escapeHtml(item.contentUrl)}" target="_blank" rel="noopener noreferrer">查看 JSON 内容</a>
                                </div>
                            </div>
                        `).join("");
                    }

                    function escapeHtml(value) {
                        return String(value ?? "")
                            .replaceAll("&", "&amp;")
                            .replaceAll("<", "&lt;")
                            .replaceAll(">", "&gt;")
                            .replaceAll("\\"", "&quot;")
                            .replaceAll("'", "&#39;");
                    }

                    async function setupLearner() {
                        setAiConfigLink();
                        const payload = {
                            feishuOpenId: openId(),
                            topic: document.getElementById("topic").value.trim(),
                            targetLevel: document.getElementById("targetLevel").value.trim(),
                            dailyMinutes: Number(document.getElementById("dailyMinutes").value.trim())
                        };
                        const response = await fetch("/api/learners/setup", {
                            method: "POST",
                            headers: { "Content-Type": "application/json" },
                            body: JSON.stringify(payload)
                        });
                        const data = await readApi(response);
                        resultElement.textContent = pretty({
                            step: "setup",
                            data
                        });
                        await refreshContext();
                    }

                    async function refreshContext() {
                        setAiConfigLink();
                        const encodedOpenId = encodeURIComponent(openId());
                        const [todayTask, currentContext, dashboard] = await Promise.all([
                            fetch(`/api/learners/${encodedOpenId}/today-task`, {method: 'POST'}).then(readApi),
                            fetch(`/api/learners/${encodedOpenId}/current-context`).then(readApi),
                            fetch(`/api/learners/${encodedOpenId}/dashboard`).then(readApi)
                        ]);

                        renderMaterials(todayTask, currentContext);
                        resultElement.textContent = pretty({
                            step: "refresh",
                            todayTask,
                            currentContext,
                            dashboard
                        });
                    }

                    async function submitDemoFeedback() {
                        setAiConfigLink();
                        const encodedOpenId = encodeURIComponent(openId());
                        const payload = {
                            submissionSummary: document.getElementById("submissionSummary").value.trim(),
                            codeSnippet: document.getElementById("codeSnippet").value.trim(),
                            question: document.getElementById("question").value.trim()
                        };
                        const response = await fetch(`/api/learners/${encodedOpenId}/demo-feedback/current`, {
                            method: "POST",
                            headers: { "Content-Type": "application/json" },
                            body: JSON.stringify(payload)
                        });
                        const data = await readApi(response);
                        resultElement.textContent = pretty({
                            step: "submit-demo",
                            data
                        });
                        await refreshContext();
                    }

                    document.getElementById("setupButton").addEventListener("click", () => {
                        setupLearner().catch(error => resultElement.textContent = error.message);
                    });
                    document.getElementById("refreshButton").addEventListener("click", () => {
                        refreshContext().catch(error => resultElement.textContent = error.message);
                    });
                    document.getElementById("submitDemoButton").addEventListener("click", () => {
                        submitDemoFeedback().catch(error => resultElement.textContent = error.message);
                    });

                    setAiConfigLink();
                </script>
                </body>
                </html>
                """;
    }
}

package com.verilearn.ai.service.impl;

import com.verilearn.ai.service.AiProviderConfigPageService;
import org.springframework.stereotype.Service;

@Service
public class AiProviderConfigPageServiceImpl implements AiProviderConfigPageService {

    @Override
    public String buildPage(String feishuOpenId) {
        String safeOpenId = escape(feishuOpenId);
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>VeriLearn AI 安全配置</title>
                    <style>
                        body { font-family: "Microsoft YaHei", sans-serif; background: #f5f7fb; margin: 0; padding: 24px; color: #1f2a37; }
                        .container { max-width: 880px; margin: 0 auto; background: #ffffff; border-radius: 16px; padding: 24px; box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08); }
                        h1, h2 { margin-top: 0; }
                        .hint { background: #eef6ff; border-left: 4px solid #2f6fed; padding: 12px 16px; border-radius: 8px; margin-bottom: 20px; }
                        .danger { background: #fff8e8; border-left: 4px solid #d18b00; padding: 12px 16px; border-radius: 8px; margin-top: 20px; }
                        label { display: block; font-weight: 600; margin-bottom: 8px; }
                        input, select { width: 100%%; box-sizing: border-box; margin-bottom: 16px; padding: 12px 14px; border: 1px solid #d0d7e2; border-radius: 10px; font-size: 14px; }
                        button { background: #2f6fed; color: white; border: none; padding: 10px 16px; border-radius: 10px; font-size: 14px; cursor: pointer; }
                        button:hover { background: #245ad2; }
                        button.secondary { background: #f1f5f9; color: #1f2a37; border: 1px solid #d0d7e2; }
                        button.secondary:hover { background: #e2e8f0; }
                        pre { white-space: pre-wrap; word-break: break-word; background: #0f172a; color: #e2e8f0; padding: 16px; border-radius: 10px; overflow: auto; }
                        .config-list { display: grid; grid-template-columns: 1fr; gap: 12px; }
                        .config-item { border: 1px solid #d0d7e2; border-radius: 12px; padding: 16px; background: #fafcff; }
                        .config-item.active { border-color: #2f6fed; background: #eef6ff; }
                        .config-meta { color: #526071; margin-bottom: 12px; line-height: 1.7; }
                        .status { display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: 12px; margin-left: 8px; }
                        .status.active { background: #dbeafe; color: #1d4ed8; }
                        .status.inactive { background: #e5e7eb; color: #374151; }
                        .actions { display: flex; gap: 8px; flex-wrap: wrap; }
                    </style>
                </head>
                <body>
                <div class="container">
                    <h1>VeriLearn AI 安全配置页</h1>
                    <div class="hint">
                        当前用户 OpenId：<strong id="openId">%s</strong><br/>
                        这是本地可演示版的 AI 安全配置入口。用户仍然从飞书进入，但敏感配置不会走普通聊天消息。
                    </div>

                    <form id="configForm">
                        <label for="providerType">提供方</label>
                        <select id="providerType" name="providerType">
                            <option value="DEEPSEEK">DEEPSEEK</option>
                            <option value="OPENAI">OPENAI</option>
                            <option value="OPENAI_COMPATIBLE">OPENAI_COMPATIBLE</option>
                        </select>

                        <label for="baseUrl">Base URL</label>
                        <input id="baseUrl" name="baseUrl" value="https://api.deepseek.com" />

                        <label for="modelName">模型名称</label>
                        <input id="modelName" name="modelName" value="deepseek-chat" />

                        <label for="apiKey">API Key</label>
                        <input id="apiKey" name="apiKey" type="password" placeholder="输入你的 API Key" />

                        <label for="activateNow">保存后立刻启用</label>
                        <select id="activateNow" name="activateNow">
                            <option value="true">true</option>
                            <option value="false">false</option>
                        </select>

                        <button type="submit">保存 AI 配置</button>
                    </form>

                    <h2>当前配置</h2>
                    <pre id="currentConfig">加载中...</pre>

                    <h2>已保存配置</h2>
                    <div id="configList" class="config-list"></div>

                    <div class="danger">
                        安全提示：这里输入的 API Key 会由后端加密后保存。这个页面用于本地演示和开发测试；正式环境仍应避免在不受控场景里暴露密钥。
                    </div>
                </div>

                <script>
                    const openId = document.getElementById("openId").textContent;
                    const currentConfigElement = document.getElementById("currentConfig");
                    const configListElement = document.getElementById("configList");
                    const form = document.getElementById("configForm");

                    function escapeHtml(value) {
                        return String(value ?? "")
                            .replaceAll("&", "&amp;")
                            .replaceAll("<", "&lt;")
                            .replaceAll(">", "&gt;")
                            .replaceAll("\\"", "&quot;")
                            .replaceAll("'", "&#39;");
                    }

                    async function readJson(response) {
                        const data = await response.json();
                        if (!response.ok || data.code !== 0) {
                            throw new Error(data.message || "请求失败");
                        }
                        return data.data;
                    }

                    async function loadCurrent() {
                        const response = await fetch(`/api/learners/${openId}/ai-provider-configs/current`);
                        const data = await readJson(response);
                        currentConfigElement.textContent = JSON.stringify(data, null, 2);
                    }

                    async function activateConfig(configId) {
                        const response = await fetch(`/api/learners/${openId}/ai-provider-configs/${configId}/activate`, {
                            method: "POST"
                        });
                        const data = await readJson(response);
                        alert(`已切换到 ${data.providerType} / ${data.modelName ?? "未配置模型"}`);
                        await loadCurrent();
                        await loadList();
                    }

                    async function loadList() {
                        const response = await fetch(`/api/learners/${openId}/ai-provider-configs`);
                        const configs = await readJson(response);
                        if (!configs || configs.length === 0) {
                            configListElement.innerHTML = "<div class='config-item'>当前还没有自定义模型配置。</div>";
                            return;
                        }

                        configListElement.innerHTML = configs.map(config => {
                            const configIdLabel = config.configId == null ? "系统默认" : `配置ID ${config.configId}`;
                            const activeClass = config.active ? "active" : "";
                            const statusClass = config.active ? "active" : "inactive";
                            const statusText = config.active ? "当前使用中" : "可切换";
                            const actionHtml = config.active || config.configId == null
                                ? ""
                                : `<button class="secondary" data-config-id="${config.configId}">切换到此配置</button>`;

                            return `
                                <div class="config-item ${activeClass}">
                                    <div class="config-meta">
                                        <strong>${escapeHtml(configIdLabel)}</strong>
                                        <span class="status ${statusClass}">${escapeHtml(statusText)}</span><br/>
                                        提供方：${escapeHtml(config.providerType)}<br/>
                                        模型：${escapeHtml(config.modelName ?? "未配置模型")}<br/>
                                        Base URL：${escapeHtml(config.baseUrl ?? "未配置")}<br/>
                                        密钥：${escapeHtml(config.apiKeyMasked ?? "未配置")}<br/>
                                        来源：${escapeHtml(config.sourceType ?? "UNKNOWN")}
                                    </div>
                                    <div class="actions">${actionHtml}</div>
                                </div>
                            `;
                        }).join("");

                        document.querySelectorAll("[data-config-id]").forEach(button => {
                            button.addEventListener("click", async () => {
                                const configId = button.getAttribute("data-config-id");
                                await activateConfig(configId);
                            });
                        });
                    }

                    form.addEventListener("submit", async (event) => {
                        event.preventDefault();
                        const payload = {
                            providerType: document.getElementById("providerType").value,
                            baseUrl: document.getElementById("baseUrl").value,
                            modelName: document.getElementById("modelName").value,
                            apiKey: document.getElementById("apiKey").value,
                            activateNow: document.getElementById("activateNow").value === "true"
                        };

                        try {
                            const response = await fetch(`/api/learners/${openId}/ai-provider-configs`, {
                                method: "POST",
                                headers: { "Content-Type": "application/json" },
                                body: JSON.stringify(payload)
                            });
                            const data = await readJson(response);
                            alert(`保存成功：${data.providerType} / ${data.modelName ?? "未配置模型"}`);
                            document.getElementById("apiKey").value = "";
                            await loadCurrent();
                            await loadList();
                        } catch (error) {
                            alert(error.message);
                        }
                    });

                    loadCurrent().catch(error => currentConfigElement.textContent = error.message);
                    loadList().catch(error => configListElement.innerHTML = `<div class='config-item'>${escapeHtml(error.message)}</div>`);
                </script>
                </body>
                </html>
                """.formatted(safeOpenId);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

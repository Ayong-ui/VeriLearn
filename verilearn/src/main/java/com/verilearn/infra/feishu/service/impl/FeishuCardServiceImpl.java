package com.verilearn.infra.feishu.service.impl;

import com.verilearn.ai.dto.AiProviderConfigResponse;
import com.verilearn.ai.service.AiProviderConfigService;
import com.verilearn.chapter.dto.ChapterSummaryResponse;
import com.verilearn.infra.feishu.dto.FeishuCardActionRequest;
import com.verilearn.infra.feishu.dto.FeishuCardActionResponse;
import com.verilearn.infra.feishu.dto.FeishuCardPreviewResponse;
import com.verilearn.infra.feishu.service.FeishuCardService;
import com.verilearn.progress.dto.ProgressResponse;
import com.verilearn.task.dto.TaskResponse;
import com.verilearn.validation.dto.ValidationItemResponse;
import com.verilearn.workflow.dto.LearnerCurrentContextResponse;
import com.verilearn.workflow.dto.LearnerDashboardResponse;
import com.verilearn.workflow.dto.LearnerMaterialReference;
import com.verilearn.workflow.service.LearnerWorkflowService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FeishuCardServiceImpl implements FeishuCardService {

    private static final String ACTION_REFRESH_TODAY = "REFRESH_TODAY_TASK";
    private static final String ACTION_SHOW_PROGRESS = "SHOW_PROGRESS";
    private static final String ACTION_SHOW_DASHBOARD = "SHOW_DASHBOARD";
    private static final String ACTION_SHOW_CURRENT_CONTEXT = "SHOW_CURRENT_CONTEXT";
    private static final String ACTION_SHOW_AI_PROVIDER = "SHOW_AI_PROVIDER";
    private static final String ACTION_ACTIVATE_AI_PROVIDER = "ACTIVATE_AI_PROVIDER";

    private final LearnerWorkflowService learnerWorkflowService;
    private final AiProviderConfigService aiProviderConfigService;

    public FeishuCardServiceImpl(
            LearnerWorkflowService learnerWorkflowService,
            AiProviderConfigService aiProviderConfigService
    ) {
        this.learnerWorkflowService = learnerWorkflowService;
        this.aiProviderConfigService = aiProviderConfigService;
    }

    @Override
    public FeishuCardPreviewResponse buildTodayTaskCard(String openId) {
        TaskResponse task = learnerWorkflowService.generateTodayTask(openId);
        FeishuCardPreviewResponse response = new FeishuCardPreviewResponse();
        response.setCardType("TODAY_TASK");
        response.setOpenId(openId);
        response.setCard(buildTodayTaskCardPayload(openId, task));
        return response;
    }

    @Override
    public FeishuCardPreviewResponse buildDashboardCard(String openId) {
        LearnerDashboardResponse dashboard = learnerWorkflowService.getDashboard(openId);
        FeishuCardPreviewResponse response = new FeishuCardPreviewResponse();
        response.setCardType("DASHBOARD");
        response.setOpenId(openId);
        response.setCard(buildDashboardCardPayload(openId, dashboard));
        return response;
    }

    @Override
    public FeishuCardPreviewResponse buildCurrentContextCard(String openId) {
        LearnerCurrentContextResponse currentContext = learnerWorkflowService.getCurrentContext(openId);
        FeishuCardPreviewResponse response = new FeishuCardPreviewResponse();
        response.setCardType("CURRENT_CONTEXT");
        response.setOpenId(openId);
        response.setCard(buildCurrentContextCardPayload(openId, currentContext));
        return response;
    }

    @Override
    public FeishuCardPreviewResponse buildAiProviderCard(String openId) {
        AiProviderConfigResponse currentConfig = aiProviderConfigService.getCurrentConfig(openId);
        List<AiProviderConfigResponse> configList = aiProviderConfigService.listConfigs(openId);
        FeishuCardPreviewResponse response = new FeishuCardPreviewResponse();
        response.setCardType("AI_PROVIDER");
        response.setOpenId(openId);
        response.setCard(buildAiProviderCardPayload(openId, currentConfig, configList));
        return response;
    }

    @Override
    public FeishuCardActionResponse handleCardAction(FeishuCardActionRequest request) {
        String openId = extractOpenId(request);
        String action = extractActionName(request);

        return switch (action) {
            case ACTION_REFRESH_TODAY -> successAction(
                    action,
                    openId,
                    "已刷新今日任务卡片",
                    buildTodayTaskCardPayload(openId, learnerWorkflowService.generateTodayTask(openId))
            );
            case ACTION_SHOW_PROGRESS -> {
                LearnerDashboardResponse dashboard = learnerWorkflowService.getDashboard(openId);
                yield successAction(
                        action,
                        openId,
                        "已切换为进度总览卡片",
                        buildProgressOnlyCardPayload(openId, dashboard.getProgress())
                );
            }
            case ACTION_SHOW_DASHBOARD -> {
                LearnerDashboardResponse dashboard = learnerWorkflowService.getDashboard(openId);
                yield successAction(
                        action,
                        openId,
                        "已切换为学习总览卡片",
                        buildDashboardCardPayload(openId, dashboard)
                );
            }
            case ACTION_SHOW_CURRENT_CONTEXT -> {
                LearnerCurrentContextResponse currentContext = learnerWorkflowService.getCurrentContext(openId);
                yield successAction(
                        action,
                        openId,
                        "已切换为当前学习上下文卡片",
                        buildCurrentContextCardPayload(openId, currentContext)
                );
            }
            case ACTION_SHOW_AI_PROVIDER -> {
                AiProviderConfigResponse currentConfig = aiProviderConfigService.getCurrentConfig(openId);
                List<AiProviderConfigResponse> configList = aiProviderConfigService.listConfigs(openId);
                yield successAction(
                        action,
                        openId,
                        "已切换为 AI 模型配置卡片",
                        buildAiProviderCardPayload(openId, currentConfig, configList)
                );
            }
            case ACTION_ACTIVATE_AI_PROVIDER -> {
                Long configId = extractConfigId(request);
                AiProviderConfigResponse activatedConfig = aiProviderConfigService.activateConfig(openId, configId);
                List<AiProviderConfigResponse> configList = aiProviderConfigService.listConfigs(openId);
                yield successAction(
                        action,
                        openId,
                        "已切换到 " + activatedConfig.getProviderType() + " / " + defaultText(activatedConfig.getModelName(), "未配置模型"),
                        buildAiProviderCardPayload(openId, activatedConfig, configList)
                );
            }
            default -> successAction(
                    action,
                    openId,
                    "暂不支持该卡片动作",
                    buildSimpleTipCard("功能待实现", "这个卡片动作还没有接入业务流程。")
            );
        };
    }

    private Map<String, Object> buildTodayTaskCardPayload(String openId, TaskResponse task) {
        List<Map<String, Object>> elements = new ArrayList<>();
        elements.add(markdown("""
                **今日任务**：%s
                **当前章节**：%s
                **当前步骤**：%s
                **任务状态**：%s
                **理论文档**：%s
                **理论内容入口**：%s
                **Demo 文档**：%s
                **Demo 内容入口**：%s
                """.formatted(
                defaultText(task.getGoalText(), "暂无任务"),
                defaultText(task.getChapterTitle(), "未分配章节"),
                defaultText(task.getStepType(), "READ_THEORY"),
                defaultText(task.getStatus(), "PENDING"),
                defaultText(task.getTheoryFilePath(), "理论材料待生成"),
                defaultText(task.getTheoryViewUrl(), "理论查看入口待生成"),
                defaultText(task.getDemoFilePath(), "Demo 任务待生成"),
                defaultText(task.getDemoViewUrl(), "Demo 查看入口待生成")
        )));

        if (task.getValidationItems() != null && !task.getValidationItems().isEmpty()) {
            StringBuilder builder = new StringBuilder("**验证项预览**\n");
            for (ValidationItemResponse item : task.getValidationItems()) {
                builder.append("- ")
                        .append(item.getItemType())
                        .append("：")
                        .append(item.getQuestionText())
                        .append("\n");
            }
            elements.add(markdown(builder.toString().trim()));
        }

        elements.add(markdown("""
                **Demo 提交提示**
                完成 Demo 后，可以直接发送：
                `/submit-demo 我完成了今天的 Demo，并能说明关键原理`
                """));

        elements.add(actionRow(List.of(
                button("刷新今日任务", ACTION_REFRESH_TODAY, task.getTaskId(), null, "primary"),
                button("查看当前上下文", ACTION_SHOW_CURRENT_CONTEXT, task.getTaskId(), null, "default"),
                button("查看 AI 配置", ACTION_SHOW_AI_PROVIDER, task.getTaskId(), null, "default")
        )));
        return card("VeriLearn 今日任务", openId, elements);
    }

    private Map<String, Object> buildDashboardCardPayload(String openId, LearnerDashboardResponse dashboard) {
        List<Map<String, Object>> elements = new ArrayList<>();
        elements.add(markdown("""
                **学习主题**：%s
                **今日任务**：%s
                **章节总数**：%d
                **待复习章节**：%d
                """.formatted(
                defaultText(dashboard.getTopic(), "未设置主题"),
                dashboard.getTodayTask() == null ? "今日还没有生成任务" : defaultText(dashboard.getTodayTask().getGoalText(), "今日还没有生成任务"),
                dashboard.getChapterCount(),
                dashboard.getPendingReviewCount()
        )));

        if (dashboard.getCurrentChapter() != null) {
            elements.add(markdown("""
                    **当前章节**：第 %d 章 %s
                    **章节状态**：%s
                    """.formatted(
                    dashboard.getCurrentChapter().getChapterNo(),
                    dashboard.getCurrentChapter().getTitle(),
                    dashboard.getCurrentChapter().getStatus()
            )));
        }

        if (dashboard.getCurrentMaterials() != null && !dashboard.getCurrentMaterials().isEmpty()) {
            elements.add(markdown(buildMaterialOverview("**当前材料入口**", dashboard.getCurrentMaterials())));
        }

        if (dashboard.getChapters() != null && !dashboard.getChapters().isEmpty()) {
            StringBuilder chapterBuilder = new StringBuilder("**章节概览**\n");
            for (int i = 0; i < Math.min(3, dashboard.getChapters().size()); i++) {
                ChapterSummaryResponse chapter = dashboard.getChapters().get(i);
                chapterBuilder.append("- 第 ")
                        .append(chapter.getChapterNo())
                        .append(" 章：")
                        .append(chapter.getTitle())
                        .append("（")
                        .append(chapter.getStatus())
                        .append("）\n");
            }
            elements.add(markdown(chapterBuilder.toString().trim()));
        }

        elements.add(actionRow(List.of(
                button("查看今日任务", ACTION_REFRESH_TODAY, null, null, "primary"),
                button("查看当前上下文", ACTION_SHOW_CURRENT_CONTEXT, null, null, "default"),
                button("查看 AI 配置", ACTION_SHOW_AI_PROVIDER, null, null, "default")
        )));
        return card("VeriLearn 学习总览", openId, elements);
    }

    private Map<String, Object> buildCurrentContextCardPayload(String openId, LearnerCurrentContextResponse currentContext) {
        List<Map<String, Object>> elements = new ArrayList<>();
        elements.add(markdown("""
                **学习主题**：%s
                **目标状态**：%s
                **今日任务**：%s
                """.formatted(
                defaultText(currentContext.getTopic(), "未设置主题"),
                defaultText(currentContext.getGoalStatus(), "ACTIVE"),
                currentContext.getTodayTask() == null ? "今日还没有生成任务" : defaultText(currentContext.getTodayTask().getGoalText(), "任务待生成")
        )));

        if (currentContext.getCurrentChapter() != null) {
            elements.add(markdown("""
                    **当前章节**：第 %d 章 %s
                    **章节状态**：%s
                    """.formatted(
                    currentContext.getCurrentChapter().getChapterNo(),
                    currentContext.getCurrentChapter().getTitle(),
                    currentContext.getCurrentChapter().getStatus()
            )));
        }

        if (currentContext.getCurrentMaterials() != null && !currentContext.getCurrentMaterials().isEmpty()) {
            elements.add(markdown(buildMaterialOverview("**当前学习材料**", currentContext.getCurrentMaterials())));
        }

        if (currentContext.getEvaluationViewUrl() != null || currentContext.getNextStepViewUrl() != null) {
            elements.add(markdown("""
                    **评估与下一步**
                    - 评估报告：%s
                    - 下一步建议：%s
                    """.formatted(
                    defaultText(currentContext.getEvaluationViewUrl(), "评估报告待生成"),
                    defaultText(currentContext.getNextStepViewUrl(), "下一步建议待生成")
            )));
        }

        elements.add(actionRow(List.of(
                button("查看今日任务", ACTION_REFRESH_TODAY, null, null, "primary"),
                button("查看进度", ACTION_SHOW_PROGRESS, null, null, "default"),
                button("查看 AI 配置", ACTION_SHOW_AI_PROVIDER, null, null, "default")
        )));
        return card("VeriLearn 当前学习上下文", openId, elements);
    }

    private Map<String, Object> buildAiProviderCardPayload(
            String openId,
            AiProviderConfigResponse currentConfig,
            List<AiProviderConfigResponse> configList
    ) {
        List<Map<String, Object>> elements = new ArrayList<>();
        elements.add(markdown("""
                **当前 AI 配置**
                - 提供方：%s
                - 模型：%s
                - Base URL：%s
                - 来源：%s
                - 密钥：%s
                """.formatted(
                defaultText(currentConfig.getProviderType(), "未配置"),
                defaultText(currentConfig.getModelName(), "未配置模型"),
                defaultText(currentConfig.getBaseUrl(), "未配置"),
                defaultText(currentConfig.getSourceType(), "UNKNOWN"),
                defaultText(currentConfig.getApiKeyMasked(), "未配置")
        )));

        StringBuilder configBuilder = new StringBuilder("**可用模型列表**\n");
        List<Map<String, Object>> switchButtons = new ArrayList<>();
        for (AiProviderConfigResponse config : configList) {
            configBuilder.append("- ");
            if (config.getConfigId() == null) {
                configBuilder.append("系统默认");
            } else {
                configBuilder.append("配置ID ").append(config.getConfigId());
            }
            configBuilder.append("：")
                    .append(defaultText(config.getProviderType(), "UNKNOWN"))
                    .append(" / ")
                    .append(defaultText(config.getModelName(), "未配置模型"))
                    .append("（")
                    .append(config.isActive() ? "当前使用中" : "可切换")
                    .append("）\n");

            if (!config.isActive() && config.getConfigId() != null) {
                switchButtons.add(button(
                        "切换到 " + config.getProviderType(),
                        ACTION_ACTIVATE_AI_PROVIDER,
                        null,
                        config.getConfigId(),
                        "default"
                ));
            }
        }
        elements.add(markdown(configBuilder.toString().trim()));
        elements.add(markdown("""
                **安全说明**
                - 正式方案不会在聊天框里明文发送 API Key
                - 当前后端已支持安全保存模型配置
                - 新增配置时，请走安全配置入口
                - 安全配置页：%s
                """.formatted(buildAiConfigPagePath(openId))));

        if (!switchButtons.isEmpty()) {
            elements.add(actionRow(switchButtons));
        }

        elements.add(actionRow(List.of(
                button("查看当前上下文", ACTION_SHOW_CURRENT_CONTEXT, null, null, "primary"),
                button("查看今日任务", ACTION_REFRESH_TODAY, null, null, "default"),
                button("查看总览", ACTION_SHOW_DASHBOARD, null, null, "default")
        )));
        return card("VeriLearn AI 模型配置", openId, elements);
    }

    private Map<String, Object> buildProgressOnlyCardPayload(String openId, ProgressResponse progress) {
        List<Map<String, Object>> elements = new ArrayList<>();
        elements.add(markdown("""
                **当前主题**：%s
                **知识点**：总计 %d，进行中 %d，已通过 %d，待重试 %d
                **章节**：总计 %d，进行中 %d，已完成 %d，待复习 %d
                """.formatted(
                progress.getTopic(),
                progress.getTotalNodes(),
                progress.getInProgressNodes(),
                progress.getPassedNodes(),
                progress.getNeedsRetryNodes(),
                progress.getTotalChapters(),
                progress.getInProgressChapters(),
                progress.getCompletedChapters(),
                progress.getPendingReviewChapters()
        )));
        elements.add(actionRow(List.of(
                button("返回今日任务", ACTION_REFRESH_TODAY, null, null, "primary"),
                button("查看当前上下文", ACTION_SHOW_CURRENT_CONTEXT, null, null, "default"),
                button("查看 AI 配置", ACTION_SHOW_AI_PROVIDER, null, null, "default")
        )));
        return card("VeriLearn 进度总览", openId, elements);
    }

    private String buildMaterialOverview(String title, List<LearnerMaterialReference> materials) {
        StringBuilder builder = new StringBuilder(title).append("\n");
        for (LearnerMaterialReference material : materials) {
            builder.append("- ")
                    .append(defaultText(material.getDisplayName(), material.getMaterialType()))
                    .append("：")
                    .append(defaultText(material.getViewUrl(), "查看入口待生成"))
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private String buildAiConfigPagePath(String openId) {
        return "/ai/provider-config-page?openId=" + openId;
    }

    private Map<String, Object> buildSimpleTipCard(String title, String content) {
        return card(title, "", List.of(markdown(content)));
    }

    private Map<String, Object> card(String title, String openId, List<Map<String, Object>> elements) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("schema", "2.0");
        card.put("config", Map.of("wide_screen_mode", true));
        card.put("header", Map.of(
                "title", Map.of("tag", "plain_text", "content", title),
                "subtitle", Map.of("tag", "plain_text", "content", openId == null || openId.isBlank() ? "本地预览" : openId)
        ));
        card.put("body", Map.of("elements", elements));
        return card;
    }

    private Map<String, Object> markdown(String content) {
        return Map.of("tag", "markdown", "content", content);
    }

    private Map<String, Object> actionRow(List<Map<String, Object>> actions) {
        return Map.of("tag", "action", "actions", actions);
    }

    private Map<String, Object> button(String text, String action, Long taskId, Long configId, String type) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("action", action);
        if (taskId != null) {
            value.put("task_id", taskId);
        }
        if (configId != null) {
            value.put("config_id", configId);
        }
        return Map.of(
                "tag", "button",
                "text", Map.of("tag", "plain_text", "content", text),
                "type", type,
                "value", value
        );
    }

    private FeishuCardActionResponse successAction(String action, String openId, String toastText, Map<String, Object> card) {
        FeishuCardActionResponse response = new FeishuCardActionResponse();
        response.setAction(action);
        response.setOpenId(openId);
        response.setToastText(toastText);
        response.setCard(card);
        return response;
    }

    private String extractOpenId(FeishuCardActionRequest request) {
        if (request.getOpenId() != null && !request.getOpenId().isBlank()) {
            return request.getOpenId();
        }
        if (request.getOperator() != null && request.getOperator().getOpenId() != null && !request.getOperator().getOpenId().isBlank()) {
            return request.getOperator().getOpenId();
        }
        throw new IllegalArgumentException("feishu card callback open id is missing");
    }

    private String extractActionName(FeishuCardActionRequest request) {
        if (request.getAction() == null || request.getAction().getValue() == null) {
            throw new IllegalArgumentException("feishu card callback action is missing");
        }
        String actionName = request.getAction().getValue().getActionName();
        if (actionName == null || actionName.isBlank()) {
            throw new IllegalArgumentException("feishu card callback action is missing");
        }
        return actionName;
    }

    private Long extractConfigId(FeishuCardActionRequest request) {
        if (request.getAction() == null || request.getAction().getValue() == null || request.getAction().getValue().getConfigId() == null) {
            throw new IllegalArgumentException("feishu card callback config id is missing");
        }
        return request.getAction().getValue().getConfigId();
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}

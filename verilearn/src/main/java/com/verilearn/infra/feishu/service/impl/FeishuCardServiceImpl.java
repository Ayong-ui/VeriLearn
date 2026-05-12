package com.verilearn.infra.feishu.service.impl;

import com.verilearn.chapter.dto.ChapterSummaryResponse;
import com.verilearn.infra.feishu.dto.FeishuCardActionRequest;
import com.verilearn.infra.feishu.dto.FeishuCardActionResponse;
import com.verilearn.infra.feishu.dto.FeishuCardPreviewResponse;
import com.verilearn.infra.feishu.service.FeishuCardService;
import com.verilearn.progress.dto.ProgressResponse;
import com.verilearn.task.dto.TaskResponse;
import com.verilearn.validation.dto.ValidationItemResponse;
import com.verilearn.workflow.dto.LearnerDashboardResponse;
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

    private final LearnerWorkflowService learnerWorkflowService;

    public FeishuCardServiceImpl(LearnerWorkflowService learnerWorkflowService) {
        this.learnerWorkflowService = learnerWorkflowService;
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
            default -> successAction(action, openId, "暂不支持该卡片动作", buildSimpleTipCard("功能待实现", "这个卡片动作还没有接入业务流程。"));
        };
    }

    private Map<String, Object> buildTodayTaskCardPayload(String openId, TaskResponse task) {
        List<Map<String, Object>> elements = new ArrayList<>();
        elements.add(markdown("""
                **今日任务**：%s
                **当前章节**：%s
                **当前步骤**：%s
                **任务状态**：%s
                **理论文件**：%s
                **Demo 文件**：%s
                """.formatted(
                defaultText(task.getGoalText(), "暂无任务"),
                defaultText(task.getChapterTitle(), "未分配章节"),
                defaultText(task.getStepType(), "READ_THEORY"),
                defaultText(task.getStatus(), "PENDING"),
                defaultText(task.getTheoryFilePath(), "理论材料待生成"),
                defaultText(task.getDemoFilePath(), "Demo 任务待生成")
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

        elements.add(actionRow(List.of(
                button("刷新今日任务", ACTION_REFRESH_TODAY, task.getTaskId(), "primary"),
                button("查看进度", ACTION_SHOW_PROGRESS, task.getTaskId(), "default"),
                button("查看总览", ACTION_SHOW_DASHBOARD, task.getTaskId(), "default")
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
                dashboard.getTopic(),
                dashboard.getTodayTask() == null ? "今日还没有生成任务" : dashboard.getTodayTask().getGoalText(),
                dashboard.getChapterCount(),
                dashboard.getPendingReviewCount()
        )));

        if (dashboard.getTodayTask() != null) {
            elements.add(markdown("""
                    **理论文件**：%s
                    **Demo 文件**：%s
                    """.formatted(
                    defaultText(dashboard.getTodayTask().getTheoryFilePath(), "理论材料待生成"),
                    defaultText(dashboard.getTodayTask().getDemoFilePath(), "Demo 任务待生成")
            )));
        }

        if (dashboard.getChapters() != null && !dashboard.getChapters().isEmpty()) {
            StringBuilder chapterBuilder = new StringBuilder("**章节概览**\n");
            for (int i = 0; i < Math.min(3, dashboard.getChapters().size()); i++) {
                ChapterSummaryResponse chapter = dashboard.getChapters().get(i);
                chapterBuilder.append("- 第")
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
                button("查看今日任务", ACTION_REFRESH_TODAY, null, "primary"),
                button("查看进度", ACTION_SHOW_PROGRESS, null, "default")
        )));
        return card("VeriLearn 学习总览", openId, elements);
    }

    private Map<String, Object> buildProgressOnlyCardPayload(String openId, ProgressResponse progress) {
        List<Map<String, Object>> elements = new ArrayList<>();
        elements.add(markdown("""
                **当前主题**：%s
                **知识点**：总计 %d，进行中 %d，通过 %d，待重试 %d
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
                button("返回今日任务", ACTION_REFRESH_TODAY, null, "primary"),
                button("查看总览", ACTION_SHOW_DASHBOARD, null, "default")
        )));
        return card("VeriLearn 进度概览", openId, elements);
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
        return Map.of(
                "tag", "markdown",
                "content", content
        );
    }

    private Map<String, Object> actionRow(List<Map<String, Object>> actions) {
        return Map.of(
                "tag", "action",
                "actions", actions
        );
    }

    private Map<String, Object> button(String text, String action, Long taskId, String type) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("action", action);
        if (taskId != null) {
            value.put("task_id", taskId);
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

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}

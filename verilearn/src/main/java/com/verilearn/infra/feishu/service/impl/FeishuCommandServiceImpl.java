package com.verilearn.infra.feishu.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verilearn.chapter.dto.ChapterSummaryResponse;
import com.verilearn.infra.feishu.config.FeishuProperties;
import com.verilearn.infra.feishu.dto.FeishuCommandResponse;
import com.verilearn.infra.feishu.dto.FeishuEventRequest;
import com.verilearn.infra.feishu.service.FeishuCommandService;
import com.verilearn.progress.dto.ProgressResponse;
import com.verilearn.task.dto.TaskResponse;
import com.verilearn.workflow.dto.LearnerDashboardResponse;
import com.verilearn.workflow.dto.LearnerSetupRequest;
import com.verilearn.workflow.dto.LearnerSetupResponse;
import com.verilearn.workflow.service.LearnerWorkflowService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeishuCommandServiceImpl implements FeishuCommandService {

    private final ObjectMapper objectMapper;
    private final FeishuProperties feishuProperties;
    private final LearnerWorkflowService learnerWorkflowService;

    public FeishuCommandServiceImpl(
            ObjectMapper objectMapper,
            FeishuProperties feishuProperties,
            LearnerWorkflowService learnerWorkflowService
    ) {
        this.objectMapper = objectMapper;
        this.feishuProperties = feishuProperties;
        this.learnerWorkflowService = learnerWorkflowService;
    }

    @Override
    public void verifyTokenIfNecessary(FeishuEventRequest request) {
        String configuredToken = feishuProperties.getVerificationToken();
        if (configuredToken == null || configuredToken.isBlank()) {
            return;
        }

        String requestToken = request.getToken();
        if ((requestToken == null || requestToken.isBlank()) && request.getHeader() != null) {
            requestToken = request.getHeader().getToken();
        }

        if (!configuredToken.equals(requestToken)) {
            throw new IllegalArgumentException("invalid feishu verification token");
        }
    }

    @Override
    public FeishuCommandResponse handleCommand(FeishuEventRequest request) {
        String openId = extractOpenId(request);
        String commandLine = extractCommandText(request);
        String normalizedCommand = commandLine == null ? "" : commandLine.trim();

        if (normalizedCommand.isBlank()) {
            return failure(openId, "", "未识别到命令内容，请发送 /start 学习主题、/today、/progress 或 /dashboard。");
        }

        String[] parts = normalizedCommand.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String argument = parts.length > 1 ? parts[1].trim() : "";

        return switch (command) {
            case "/start" -> handleStart(openId, argument);
            case "/today" -> handleToday(openId);
            case "/progress" -> handleProgress(openId);
            case "/dashboard" -> handleDashboard(openId);
            default -> failure(openId, command, "暂不支持该命令，请使用 /start、/today、/progress 或 /dashboard。");
        };
    }

    private FeishuCommandResponse handleStart(String openId, String argument) {
        if (argument.isBlank()) {
            return failure(openId, "/start", "请在 /start 后面带上学习主题，例如：/start Java 后端");
        }

        try {
            LearnerSetupRequest request = new LearnerSetupRequest();
            request.setFeishuOpenId(openId);
            request.setTopic(argument);
            LearnerSetupResponse response = learnerWorkflowService.setupLearner(request);
            return success(
                    openId,
                    "/start",
                    """
                            已为你初始化学习目标：%s
                            默认每天学习时长：%d 分钟
                            已初始化知识点：%d 个
                            已初始化章节：%d 个
                            接下来发送 /today 查看今天的任务。
                            """.formatted(
                            response.getTopic(),
                            response.getDailyMinutes(),
                            response.getInitializedNodeCount(),
                            response.getChapterCount()
                    ).trim()
            );
        } catch (IllegalArgumentException exception) {
            return failure(openId, "/start", exception.getMessage());
        }
    }

    private FeishuCommandResponse handleToday(String openId) {
        try {
            TaskResponse task = learnerWorkflowService.generateTodayTask(openId);
            String theoryPath = defaultText(task.getTheoryFilePath(), "理论材料待生成");
            String demoPath = defaultText(task.getDemoFilePath(), "Demo 任务待生成");
            return success(
                    openId,
                    "/today",
                    """
                            今日任务：%s
                            当前章节：%s
                            当前步骤：%s
                            任务状态：%s
                            理论文件：%s
                            Demo 文件：%s
                            验证项数量：%d
                            """.formatted(
                            task.getGoalText(),
                            defaultText(task.getChapterTitle(), "未分配章节"),
                            defaultText(task.getStepType(), "READ_THEORY"),
                            task.getStatus(),
                            theoryPath,
                            demoPath,
                            task.getValidationItems() == null ? 0 : task.getValidationItems().size()
                    ).trim()
            );
        } catch (IllegalArgumentException exception) {
            return failure(openId, "/today", exception.getMessage());
        }
    }

    private FeishuCommandResponse handleProgress(String openId) {
        try {
            ProgressResponse progress = learnerWorkflowService.getProgress(openId);
            return success(
                    openId,
                    "/progress",
                    """
                            当前主题：%s
                            知识点：总计 %d，进行中 %d，通过 %d，待重试 %d
                            章节：总计 %d，进行中 %d，已完成 %d，待复习 %d
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
                    ).trim()
            );
        } catch (IllegalArgumentException exception) {
            return failure(openId, "/progress", exception.getMessage());
        }
    }

    private FeishuCommandResponse handleDashboard(String openId) {
        try {
            LearnerDashboardResponse dashboard = learnerWorkflowService.getDashboard(openId);
            List<ChapterSummaryResponse> chapters = dashboard.getChapters();
            String currentChapter = chapters == null || chapters.isEmpty() ? "暂无章节" : chapters.get(0).getTitle();
            return success(
                    openId,
                    "/dashboard",
                    """
                            当前主题：%s
                            今日任务：%s
                            章节总数：%d
                            待复习章节：%d
                            当前看到的章节：%s
                            """.formatted(
                            dashboard.getTopic(),
                            dashboard.getTodayTask() == null ? "今日还没有生成任务" : dashboard.getTodayTask().getGoalText(),
                            dashboard.getChapterCount(),
                            dashboard.getPendingReviewCount(),
                            currentChapter
                    ).trim()
            );
        } catch (IllegalArgumentException exception) {
            return failure(openId, "/dashboard", exception.getMessage());
        }
    }

    private String extractOpenId(FeishuEventRequest request) {
        if (request.getEvent() == null || request.getEvent().getSender() == null || request.getEvent().getSender().getSenderId() == null) {
            throw new IllegalArgumentException("feishu sender open id is missing");
        }
        String openId = request.getEvent().getSender().getSenderId().getOpenId();
        if (openId == null || openId.isBlank()) {
            throw new IllegalArgumentException("feishu sender open id is missing");
        }
        return openId;
    }

    private String extractCommandText(FeishuEventRequest request) {
        if (request.getEvent() == null || request.getEvent().getMessage() == null) {
            throw new IllegalArgumentException("feishu message is missing");
        }
        String content = request.getEvent().getMessage().getContent();
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("feishu message content is empty");
        }

        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode textNode = root.get("text");
            return textNode == null ? content : textNode.asText();
        } catch (Exception ignored) {
            return content;
        }
    }

    private FeishuCommandResponse success(String openId, String command, String replyText) {
        FeishuCommandResponse response = new FeishuCommandResponse();
        response.setSuccess(true);
        response.setOpenId(openId);
        response.setCommand(command);
        response.setReplyText(replyText);
        return response;
    }

    private FeishuCommandResponse failure(String openId, String command, String replyText) {
        FeishuCommandResponse response = new FeishuCommandResponse();
        response.setSuccess(false);
        response.setOpenId(openId);
        response.setCommand(command);
        response.setReplyText(replyText);
        return response;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}

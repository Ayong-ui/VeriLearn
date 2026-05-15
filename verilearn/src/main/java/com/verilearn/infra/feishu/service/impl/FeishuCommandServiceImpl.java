package com.verilearn.infra.feishu.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verilearn.ai.dto.AiProviderConfigResponse;
import com.verilearn.ai.service.AiProviderConfigService;
import com.verilearn.chapter.dto.ChapterDemoEvaluationResponse;
import com.verilearn.chapter.dto.ChapterSummaryResponse;
import com.verilearn.infra.feishu.config.FeishuProperties;
import com.verilearn.infra.feishu.dto.FeishuCommandResponse;
import com.verilearn.infra.feishu.dto.FeishuEventRequest;
import com.verilearn.infra.feishu.service.FeishuCommandService;
import com.verilearn.progress.dto.ProgressResponse;
import com.verilearn.task.dto.TaskResponse;
import com.verilearn.workflow.dto.LearnerDashboardResponse;
import com.verilearn.workflow.dto.LearnerDemoSubmissionRequest;
import com.verilearn.workflow.dto.LearnerMaterialReference;
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
    private final AiProviderConfigService aiProviderConfigService;

    public FeishuCommandServiceImpl(
            ObjectMapper objectMapper,
            FeishuProperties feishuProperties,
            LearnerWorkflowService learnerWorkflowService,
            AiProviderConfigService aiProviderConfigService
    ) {
        this.objectMapper = objectMapper;
        this.feishuProperties = feishuProperties;
        this.learnerWorkflowService = learnerWorkflowService;
        this.aiProviderConfigService = aiProviderConfigService;
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

        if (requestToken == null || requestToken.isBlank()) {
            return;
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
            return failure(openId, "", "没有识别到命令内容，请发送 /start 学习主题、/today、/progress、/dashboard、/submit-demo 或 /ai。");
        }

        String[] parts = normalizedCommand.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String argument = parts.length > 1 ? parts[1].trim() : "";

        return switch (command) {
            case "/start" -> handleStart(openId, argument);
            case "/today" -> handleToday(openId);
            case "/progress" -> handleProgress(openId);
            case "/dashboard" -> handleDashboard(openId);
            case "/submit-demo" -> handleSubmitDemo(openId, argument);
            case "/ai" -> handleAiCommand(openId, argument);
            default -> failure(openId, command, "暂不支持该命令，请使用 /start、/today、/progress、/dashboard、/submit-demo 或 /ai。");
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
                            默认每日学习时长：%d 分钟
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
            return success(
                    openId,
                    "/today",
                    """
                            今日任务：%s
                            当前章节：%s
                            当前步骤：%s
                            任务状态：%s
                            理论文档：%s
                            理论内容入口：%s
                            Demo 文档：%s
                            Demo 内容入口：%s
                            验证项数量：%d

                            完成 Demo 后可以直接发送：
                            /submit-demo 我完成了今天的 Demo，并能说明关键原理
                            """.formatted(
                            task.getGoalText(),
                            defaultText(task.getChapterTitle(), "未分配章节"),
                            defaultText(task.getStepType(), "READ_THEORY"),
                            task.getStatus(),
                            defaultText(task.getTheoryFilePath(), "理论文档待生成"),
                            defaultText(task.getTheoryViewUrl(), "理论查看入口待生成"),
                            defaultText(task.getDemoFilePath(), "Demo 文档待生成"),
                            defaultText(task.getDemoViewUrl(), "Demo 查看入口待生成"),
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
                            知识点：总计 %d，进行中 %d，已通过 %d，待重试 %d
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
                            当前材料入口：
                            %s
                            """.formatted(
                            dashboard.getTopic(),
                            dashboard.getTodayTask() == null ? "今日还没有生成任务" : dashboard.getTodayTask().getGoalText(),
                            dashboard.getChapterCount(),
                            dashboard.getPendingReviewCount(),
                            currentChapter,
                            buildMaterialOverview(dashboard.getCurrentMaterials())
                    ).trim()
            );
        } catch (IllegalArgumentException exception) {
            return failure(openId, "/dashboard", exception.getMessage());
        }
    }

    private FeishuCommandResponse handleSubmitDemo(String openId, String argument) {
        if (argument.isBlank()) {
            return failure(openId, "/submit-demo", "请发送 /submit-demo 你的完成说明。也兼容旧格式：/submit-demo 章节ID 你的完成说明");
        }

        Long chapterId = null;
        String submissionSummary = argument.trim();
        String[] parts = argument.split("\\s+", 2);
        if (parts.length >= 2) {
            try {
                chapterId = Long.parseLong(parts[0]);
                submissionSummary = parts[1].trim();
            } catch (NumberFormatException ignored) {
                submissionSummary = argument.trim();
            }
        }

        if (submissionSummary.isBlank()) {
            return failure(openId, "/submit-demo", "请在 /submit-demo 后补充你的完成说明。");
        }

        try {
            LearnerDemoSubmissionRequest request = new LearnerDemoSubmissionRequest();
            request.setSubmissionSummary(submissionSummary);
            ChapterDemoEvaluationResponse response = chapterId == null
                    ? learnerWorkflowService.evaluateCurrentDemoSubmission(openId, request)
                    : learnerWorkflowService.evaluateCurrentDemoSubmission(openId, chapterId, request);
            return success(
                    openId,
                    "/submit-demo",
                    """
                            Demo 评估已完成
                            %s
                            掌握度：%s
                            章节状态：%s
                            评估报告：%s
                            下一步建议：%s
                            """.formatted(
                            chapterId == null ? "已按当前学习上下文自动定位章节" : "章节ID：" + chapterId,
                            defaultText(response.getUnderstandingLevel(), "UNKNOWN"),
                            defaultText(response.getChapterStatus(), "IN_PROGRESS"),
                            defaultText(response.getEvaluationViewUrl(), "评估报告待生成"),
                            defaultText(response.getNextStepViewUrl(), "下一步建议待生成")
                    ).trim()
            );
        } catch (IllegalArgumentException exception) {
            return failure(openId, "/submit-demo", exception.getMessage());
        }
    }

    private FeishuCommandResponse handleAiCommand(String openId, String argument) {
        String normalizedArgument = argument == null ? "" : argument.trim();
        if (normalizedArgument.isBlank() || "current".equalsIgnoreCase(normalizedArgument)) {
            return handleAiCurrent(openId);
        }

        String[] parts = normalizedArgument.split("\\s+");
        String subCommand = parts[0].toLowerCase();
        return switch (subCommand) {
            case "current" -> handleAiCurrent(openId);
            case "switch" -> {
                if (parts.length < 2) {
                    yield failure(openId, "/ai", "请使用 /ai switch 配置ID。例如：/ai switch 2");
                }
                yield handleAiSwitch(openId, parts[1]);
            }
            case "config", "help" -> success(
                    openId,
                    "/ai",
                    """
                            AI 配置说明
                            1. 发送 /ai current 查看当前模型
                            2. 发送 /ai switch 配置ID 切换到已保存模型
                            3. 如需新增模型配置，请打开安全配置页：
                               %s
                            4. 正式产品会通过飞书安全入口录入密钥，不建议在聊天消息里直接发送 API Key
                            """.formatted(buildAiConfigPagePath(openId)).trim()
            );
            default -> failure(openId, "/ai", "不支持的 AI 命令，请使用 /ai current、/ai switch 配置ID 或 /ai config。");
        };
    }

    private FeishuCommandResponse handleAiCurrent(String openId) {
        try {
            AiProviderConfigResponse currentConfig = aiProviderConfigService.getCurrentConfig(openId);
            return success(
                    openId,
                    "/ai",
                    """
                            当前 AI 配置
                            提供方：%s
                            模型：%s
                            Base URL：%s
                            状态：%s
                            来源：%s
                            密钥：%s
                            %s
                            """.formatted(
                            currentConfig.getProviderType(),
                            defaultText(currentConfig.getModelName(), "未配置"),
                            defaultText(currentConfig.getBaseUrl(), "未配置"),
                            currentConfig.getStatus(),
                            currentConfig.getSourceType(),
                            defaultText(currentConfig.getApiKeyMasked(), "未配置"),
                            currentConfig.getConfigId() == null
                                    ? "当前正在使用系统默认模型。"
                                    : "如需切换到其他已保存模型，请发送 /ai switch 配置ID"
                    ).trim()
            );
        } catch (IllegalArgumentException exception) {
            return failure(openId, "/ai", exception.getMessage());
        }
    }

    private FeishuCommandResponse handleAiSwitch(String openId, String configIdText) {
        try {
            Long configId = Long.parseLong(configIdText);
            AiProviderConfigResponse activatedConfig = aiProviderConfigService.activateConfig(openId, configId);
            return success(
                    openId,
                    "/ai",
                    """
                            已切换当前 AI 模型
                            配置ID：%d
                            提供方：%s
                            模型：%s
                            Base URL：%s
                            密钥：%s
                            """.formatted(
                            activatedConfig.getConfigId(),
                            activatedConfig.getProviderType(),
                            defaultText(activatedConfig.getModelName(), "未配置"),
                            defaultText(activatedConfig.getBaseUrl(), "未配置"),
                            defaultText(activatedConfig.getApiKeyMasked(), "未配置")
                    ).trim()
            );
        } catch (NumberFormatException exception) {
            return failure(openId, "/ai", "配置ID 必须是数字，请使用 /ai switch 配置ID。");
        } catch (IllegalArgumentException exception) {
            return failure(openId, "/ai", exception.getMessage());
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

    private String buildMaterialOverview(List<LearnerMaterialReference> materials) {
        if (materials == null || materials.isEmpty()) {
            return "暂无材料";
        }
        StringBuilder builder = new StringBuilder();
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
}

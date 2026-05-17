package com.verilearn.infra.feishu.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verilearn.ai.dto.AiProviderConfigResponse;
import com.verilearn.ai.service.AiProviderConfigService;
import com.verilearn.chapter.config.StorageProperties;
import com.verilearn.chapter.dto.ChapterDemoEvaluationResponse;
import com.verilearn.chapter.dto.ChapterSummaryResponse;
import com.verilearn.infra.feishu.config.FeishuProperties;
import com.verilearn.infra.feishu.dto.FeishuCommandResponse;
import com.verilearn.infra.feishu.dto.FeishuEventRequest;
import com.verilearn.infra.feishu.service.FeishuCommandService;
import com.verilearn.progress.dto.ProgressResponse;
import com.verilearn.task.dto.TaskResponse;
import com.verilearn.workflow.dto.LearningRouteContentResponse;
import com.verilearn.workflow.dto.LearnerCurrentContextResponse;
import com.verilearn.workflow.dto.LearnerDashboardResponse;
import com.verilearn.workflow.dto.LearnerDemoSubmissionRequest;
import com.verilearn.workflow.dto.LearnerMaterialReference;
import com.verilearn.workflow.dto.LearnerSetupRequest;
import com.verilearn.workflow.dto.LearnerSetupResponse;
import com.verilearn.workflow.dto.TopicAnalysisResult;
import com.verilearn.workflow.dto.TopicOptionSelection;
import com.verilearn.workflow.service.LearnerWorkflowService;
import com.verilearn.workflow.service.PendingTopicSelectionService;
import com.verilearn.workflow.service.TopicValidationService;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class FeishuCommandServiceImpl implements FeishuCommandService {

    private static final String DEFAULT_AI_CONFIG_PATH_TEMPLATE = "/ai/provider-config-page?openId=%s";
    private static final int ROUTE_OVERVIEW_WINDOW_SIZE = 4;

    private final ObjectMapper objectMapper;
    private final FeishuProperties feishuProperties;
    private final LearnerWorkflowService learnerWorkflowService;
    private final AiProviderConfigService aiProviderConfigService;
    private final TopicValidationService topicValidationService;
    private final PendingTopicSelectionService pendingTopicSelectionService;
    private final StorageProperties storageProperties;

    public FeishuCommandServiceImpl(
            ObjectMapper objectMapper,
            FeishuProperties feishuProperties,
            LearnerWorkflowService learnerWorkflowService,
            AiProviderConfigService aiProviderConfigService,
            TopicValidationService topicValidationService,
            PendingTopicSelectionService pendingTopicSelectionService,
            StorageProperties storageProperties
    ) {
        this.objectMapper = objectMapper;
        this.feishuProperties = feishuProperties;
        this.learnerWorkflowService = learnerWorkflowService;
        this.aiProviderConfigService = aiProviderConfigService;
        this.topicValidationService = topicValidationService;
        this.pendingTopicSelectionService = pendingTopicSelectionService;
        this.storageProperties = storageProperties;
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
        String rawText = extractCommandText(request);
        String text = rawText == null ? "" : rawText.trim();

        if (text.isBlank()) {
            return failure(openId, "", "没有识别到命令内容。请发送 /start、/today、/progress、/dashboard、/submit-demo、/clear、/paths 或 /ai。");
        }

        TopicOptionSelection pendingSelection = pendingTopicSelectionService.get(openId);
        if (pendingSelection != null && !text.startsWith("/")) {
            if (text.matches("\\d+")) {
                return handlePendingTopicSelection(openId, pendingSelection, text);
            }
            return failure(openId, text, buildPendingSelectionHint(pendingSelection));
        }

        if ("我完成了".equals(text)) {
            return handleSubmitDemo(openId, "我完成了");
        }

        String[] parts = text.split("\\s+", 2);
        String command = parts[0].toLowerCase(Locale.ROOT);
        String argument = parts.length > 1 ? parts[1].trim() : "";

        return switch (command) {
            case "/start" -> handleStart(openId, argument);
            case "/today" -> handleToday(openId);
            case "/progress" -> handleProgress(openId);
            case "/dashboard" -> handleDashboard(openId);
            case "/submit-demo" -> handleSubmitDemo(openId, argument);
            case "/clear" -> handleClearRouteCommand(openId, argument);
            case "/paths" -> handlePaths(openId);
            case "/ai" -> handleAiCommand(openId, argument);
            default -> failure(openId, command, "暂不支持这个命令。请使用 /start、/today、/progress、/dashboard、/submit-demo、/clear、/paths 或 /ai。");
        };
    }

    private FeishuCommandResponse handlePendingTopicSelection(String openId, TopicOptionSelection pendingSelection, String text) {
        int selectedIndex = Integer.parseInt(text) - 1;
        if (selectedIndex < 0 || selectedIndex >= pendingSelection.getOptions().size()) {
            return failure(openId, text, buildPendingSelectionHint(pendingSelection));
        }

        pendingTopicSelectionService.consume(openId);
        String selectedTopic = pendingSelection.getOptions().get(selectedIndex);
        return startTopic(openId, selectedTopic);
    }

    private FeishuCommandResponse handleStart(String openId, String argument) {
        if (argument.isBlank()) {
            return failure(openId, "/start", "请在 /start 后面输入学习主题，例如：/start Linux 或 /start Spring Boot Controller");
        }

        pendingTopicSelectionService.clear(openId);
        TopicAnalysisResult analysis = topicValidationService.analyzeTopic(argument);
        if (analysis.getKind() == TopicAnalysisResult.TopicKind.REJECT) {
            return failure(openId, "/start", analysis.getMessage());
        }
        if (analysis.getKind() == TopicAnalysisResult.TopicKind.REQUIRE_OPTIONS) {
            try {
                List<String> options = learnerWorkflowService.generateTopicOptions(openId, argument);
                TopicOptionSelection selection = new TopicOptionSelection();
                selection.setOriginalTopic(argument);
                selection.setOptions(options);
                selection.setCreatedAt(LocalDateTime.now());
                pendingTopicSelectionService.save(openId, selection);
                return success(openId, "/start", buildTopicOptionsReply(argument, options));
            } catch (IllegalArgumentException | IllegalStateException exception) {
                return failure(openId, "/start", sanitizeWorkflowMessage(exception.getMessage()));
            }
        }
        return startTopic(openId, argument);
    }

    private FeishuCommandResponse startTopic(String openId, String topic) {
        LearnerDashboardResponse activeDashboard = getActiveDashboardOrNull(openId);
        if (activeDashboard != null && "ACTIVE".equalsIgnoreCase(activeDashboard.getGoalStatus())) {
            String currentChapterTitle = activeDashboard.getCurrentChapter() == null
                    ? "未定位到当前章节"
                    : defaultText(activeDashboard.getCurrentChapter().getTitle(), "未定位到当前章节");
            String currentStep = activeDashboard.getTodayTask() == null
                    ? "未定位到当前步骤"
                    : toReadableStepType(defaultText(activeDashboard.getTodayTask().getStepType(), "READ_THEORY"));
            return failure(
                    openId,
                    "/start",
                    """
                            你当前还有未完成的学习任务：
                            当前主题：%s
                            当前章节：%s
                            当前步骤：%s

                            请先发送 /today 继续当前任务。
                            如果你确认要放弃当前方向，再使用 /clear %s。
                            """.formatted(activeDashboard.getTopic(), currentChapterTitle, currentStep, activeDashboard.getTopic()).trim()
            );
        }

        try {
            LearnerSetupRequest request = new LearnerSetupRequest();
            request.setFeishuOpenId(openId);
            request.setTopic(topic);
            LearnerSetupResponse response = learnerWorkflowService.setupLearner(request);
            return success(
                    openId,
                    "/start",
                    """
                            已为你初始化学习主题：%s
                            默认每日学习时长：%d 分钟
                            已生成知识点：%d 个
                            已生成章节：%d 章

                            接下来请发送 /today 查看今天的学习任务。
                            """.formatted(
                            response.getTopic(),
                            response.getDailyMinutes(),
                            response.getInitializedNodeCount(),
                            response.getChapterCount()
                    ).trim()
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(openId, "/start", sanitizeWorkflowMessage(exception.getMessage()));
        }
    }

    private FeishuCommandResponse handleToday(String openId) {
        try {
            TaskResponse task = learnerWorkflowService.generateTodayTask(openId);
            LearnerDashboardResponse dashboard = learnerWorkflowService.getDashboard(openId);
            int currentChapterNo = task.getCurrentChapterNo() == null ? dashboard.getCurrentChapterNo() : task.getCurrentChapterNo();
            int totalChapterCount = task.getTotalChapterCount() == null || task.getTotalChapterCount() == 0
                    ? dashboard.getTotalChapterCount()
                    : task.getTotalChapterCount();
            return success(openId, "/today", buildTodayReply(task, dashboard, currentChapterNo, totalChapterCount));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(openId, "/today", sanitizeWorkflowMessage(exception.getMessage()));
        }
    }

    private FeishuCommandResponse handleProgress(String openId) {
        try {
            ProgressResponse progress = learnerWorkflowService.getProgress(openId);
            return success(openId, "/progress", buildProgressReply(progress));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(openId, "/progress", sanitizeWorkflowMessage(exception.getMessage()));
        }
    }

    private FeishuCommandResponse handleDashboard(String openId) {
        try {
            LearnerDashboardResponse dashboard = learnerWorkflowService.getDashboard(openId);
            return success(openId, "/dashboard", buildDashboardReply(dashboard));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(openId, "/dashboard", sanitizeWorkflowMessage(exception.getMessage()));
        }
    }

    private FeishuCommandResponse handleSubmitDemo(String openId, String argument) {
        String submissionSummary = argument == null || argument.isBlank() ? "我完成了" : argument.trim();
        try {
            LearnerDemoSubmissionRequest request = new LearnerDemoSubmissionRequest();
            request.setSubmissionSummary(submissionSummary);
            ChapterDemoEvaluationResponse response = learnerWorkflowService.evaluateCurrentDemoSubmission(openId, request);
            return success(
                    openId,
                    "/submit-demo",
                    """
                            Demo 评估已完成
                            掌握程度：%s
                            章节状态：%s

                            查看评估报告：
                            %s

                            查看下一步建议：
                            %s
                            """.formatted(
                            defaultText(response.getUnderstandingLevel(), "UNKNOWN"),
                            toReadableStatus(defaultText(response.getChapterStatus(), "IN_PROGRESS")),
                            defaultText(toAbsoluteUrl(response.getEvaluationViewUrl()), "评估报告暂未生成"),
                            defaultText(toAbsoluteUrl(response.getNextStepViewUrl()), "下一步建议暂未生成")
                    ).trim()
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(openId, "/submit-demo", sanitizeWorkflowMessage(exception.getMessage()));
        }
    }

    private FeishuCommandResponse handleClearRouteCommand(String openId, String argument) {
        if (argument == null || argument.isBlank()) {
            return failure(openId, "/clear", "请使用 /clear 主题，例如：/clear MySQL");
        }

        LearnerDashboardResponse activeDashboard = getActiveDashboardOrNull(openId);
        if (activeDashboard == null || !"ACTIVE".equalsIgnoreCase(activeDashboard.getGoalStatus())) {
            return failure(openId, "/clear", "当前没有可清理的进行中学习路线。");
        }
        if (!normalize(activeDashboard.getTopic()).equals(normalize(argument))) {
            return failure(openId, "/clear", "当前进行中的学习路线不是 " + argument + "。");
        }

        try {
            learnerWorkflowService.clearLearningRoute(openId, argument);
            pendingTopicSelectionService.clear(openId);
            return success(
                    openId,
                    "/clear",
                    """
                            已清理学习路线：%s
                            该主题关联的任务、章节、材料、验证记录和 learning-space 文件都已删除。

                            现在你可以重新发送 /start 新主题，开启新的学习路线。
                            """.formatted(argument).trim()
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(openId, "/clear", sanitizeWorkflowMessage(exception.getMessage()));
        }
    }

    private FeishuCommandResponse handlePaths(String openId) {
        try {
            LearningRouteContentResponse route = learnerWorkflowService.getLearningRoute(openId);
            LearnerCurrentContextResponse context = learnerWorkflowService.getCurrentContext(openId);
            TaskResponse todayTask = context.getTodayTask();
            return success(
                    openId,
                    "/paths",
                    """
                            当前学习路线文件：
                            - learning-route.md：%s
                            - theory.md：%s
                            - demo-task.md：%s
                            - evaluation-report.md：%s
                            - next-step.md：%s
                            """.formatted(
                            defaultText(route.getAbsoluteFilePath(), "暂无"),
                            toAbsoluteLearningSpacePath(todayTask == null ? null : todayTask.getTheoryFilePath()),
                            toAbsoluteLearningSpacePath(todayTask == null ? null : todayTask.getDemoFilePath()),
                            toAbsoluteLearningSpacePath(context.getEvaluationFilePath()),
                            toAbsoluteLearningSpacePath(context.getNextStepFilePath())
                    ).trim()
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(openId, "/paths", sanitizeWorkflowMessage(exception.getMessage()));
        }
    }

    private FeishuCommandResponse handleAiCommand(String openId, String argument) {
        String normalizedArgument = argument == null ? "" : argument.trim();
        if (normalizedArgument.isBlank() || "current".equalsIgnoreCase(normalizedArgument)) {
            return handleAiCurrent(openId);
        }
        if ("config".equalsIgnoreCase(normalizedArgument) || "help".equalsIgnoreCase(normalizedArgument)) {
            return success(
                    openId,
                    "/ai",
                    """
                            AI 配置说明
                            1. 发送 /ai current 查看当前模型
                            2. 发送 /ai switch 配置ID 切换到已保存模型
                            3. 如果要新增模型配置，请打开安全配置页面：
                               %s
                            4. 不建议在聊天消息里直接发送 API Key
                            """.formatted(buildAiConfigPageUrl(openId)).trim()
            );
        }

        String[] parts = normalizedArgument.split("\\s+");
        String subCommand = parts[0].toLowerCase(Locale.ROOT);
        return switch (subCommand) {
            case "current" -> handleAiCurrent(openId);
            case "switch" -> {
                if (parts.length < 2) {
                    yield failure(openId, "/ai", "请使用 /ai switch 配置ID，例如：/ai switch 2");
                }
                yield handleAiSwitch(openId, parts[1]);
            }
            default -> failure(openId, "/ai", "不支持这个 AI 命令，请使用 /ai current、/ai switch 配置ID 或 /ai config。");
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
                            defaultText(currentConfig.getStatus(), "UNKNOWN"),
                            defaultText(currentConfig.getSourceType(), "UNKNOWN"),
                            defaultText(currentConfig.getApiKeyMasked(), "未配置"),
                            currentConfig.getConfigId() == null
                                    ? "当前正在使用系统默认模型。"
                                    : "如需切换到其他已保存模型，请发送 /ai switch 配置ID。"
                    ).trim()
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(openId, "/ai", sanitizeWorkflowMessage(exception.getMessage()));
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
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(openId, "/ai", sanitizeWorkflowMessage(exception.getMessage()));
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

    private String buildTodayReply(
            TaskResponse task,
            LearnerDashboardResponse dashboard,
            int currentChapterNo,
            int totalChapterCount
    ) {
        int validationItemCount = task.getValidationItems() == null ? 0 : task.getValidationItems().size();
        return """
                今日任务：%s
                学习主题：%s
                当前进度：第 %d / %d 章
                当前章节：%s
                当前步骤：%s
                任务状态：%s

                路线概览：
                %s

                资料入口：
                - 学习路线：%s
                - 先看理论：%s
                - 再做 Demo：%s

                验证信息：
                - 本次验证项：%d 个

                完成 Demo 后直接发送：
                /submit-demo 我完成了
                """.formatted(
                defaultText(task.getGoalText(), "今日学习任务"),
                defaultText(dashboard.getTopic(), "未设置学习主题"),
                currentChapterNo,
                totalChapterCount,
                defaultText(task.getChapterTitle(), "未分配章节"),
                toReadableStepType(defaultText(task.getStepType(), "READ_THEORY")),
                toReadableStatus(defaultText(task.getStatus(), "PENDING")),
                buildRouteOverview(dashboard.getChapters(), currentChapterNo),
                defaultText(toAbsoluteUrl(task.getRouteViewUrl()), "学习路线暂未生成"),
                defaultText(toAbsoluteUrl(task.getTheoryViewUrl()), "理论文档暂未生成"),
                defaultText(toAbsoluteUrl(task.getDemoViewUrl()), "Demo 文档暂未生成"),
                validationItemCount
        ).trim();
    }

    private String buildProgressReply(ProgressResponse progress) {
        return """
                当前主题：%s
                路线状态：%s
                学习目标：%s

                知识点进度：
                - 总数：%d
                - 进行中：%d
                - 已掌握：%d
                - 待重试：%d

                章节进度：
                - 总章数：%d
                - 进行中：%d
                - 已完成：%d
                - 待复习：%d
                """.formatted(
                defaultText(progress.getTopic(), "未设置学习主题"),
                toReadableStatus(defaultText(progress.getGoalStatus(), "IN_PROGRESS")),
                defaultText(progress.getTargetLevel(), "intern"),
                progress.getTotalNodes(),
                progress.getInProgressNodes(),
                progress.getPassedNodes(),
                progress.getNeedsRetryNodes(),
                progress.getTotalChapters(),
                progress.getInProgressChapters(),
                progress.getCompletedChapters(),
                progress.getPendingReviewChapters()
        ).trim();
    }

    private String buildDashboardReply(LearnerDashboardResponse dashboard) {
        return """
                当前主题：%s
                路线状态：%s
                当前进度：第 %d / %d 章
                待复习章节：%d

                路线入口：
                %s

                当前材料：
                %s
                """.formatted(
                defaultText(dashboard.getTopic(), "未设置学习主题"),
                toReadableStatus(defaultText(dashboard.getGoalStatus(), "IN_PROGRESS")),
                dashboard.getCurrentChapterNo(),
                dashboard.getTotalChapterCount(),
                dashboard.getPendingReviewCount(),
                defaultText(toAbsoluteUrl(dashboard.getRouteViewUrl()), "学习路线暂未生成"),
                buildMaterialOverview(dashboard.getCurrentMaterials())
        ).trim();
    }

    private String buildTopicOptionsReply(String originalTopic, List<String> options) {
        StringBuilder builder = new StringBuilder();
        builder.append("“").append(originalTopic).append("”范围较大，请先选择一个更具体的学习方向：\n");
        for (int i = 0; i < options.size(); i++) {
            builder.append(i + 1).append(". ").append(options.get(i)).append("\n");
        }
        builder.append("\n请直接回复数字 1-").append(options.size()).append(" 继续。");
        return builder.toString().trim();
    }

    private String buildPendingSelectionHint(TopicOptionSelection selection) {
        return "当前正在等待你为主题“" + selection.getOriginalTopic() + "”选择子方向，请直接回复 1-" + selection.getOptions().size() + "。";
    }

    private String buildRouteOverview(List<ChapterSummaryResponse> chapters, int currentChapterNo) {
        if (chapters == null || chapters.isEmpty()) {
            return "暂无学习路线";
        }

        List<ChapterSummaryResponse> orderedChapters = new ArrayList<>(chapters);
        orderedChapters.sort(Comparator.comparing(
                chapter -> chapter.getChapterNo() == null ? Integer.MAX_VALUE : chapter.getChapterNo()
        ));

        int currentIndex = resolveCurrentChapterIndex(orderedChapters, currentChapterNo);
        int startIndex = Math.max(0, currentIndex - 1);
        int endIndex = Math.min(orderedChapters.size(), startIndex + ROUTE_OVERVIEW_WINDOW_SIZE);
        if (endIndex - startIndex < ROUTE_OVERVIEW_WINDOW_SIZE) {
            startIndex = Math.max(0, endIndex - ROUTE_OVERVIEW_WINDOW_SIZE);
        }

        StringBuilder builder = new StringBuilder();
        if (startIndex > 0) {
            builder.append("...前面还有 ").append(startIndex).append(" 章").append("\n");
        }

        for (int i = startIndex; i < endIndex; i++) {
            ChapterSummaryResponse chapter = orderedChapters.get(i);
            boolean isCurrentChapter = chapter.getChapterNo() != null && chapter.getChapterNo() == currentChapterNo;
            builder.append(isCurrentChapter ? "-> " : "   ")
                    .append(chapter.getChapterNo())
                    .append(". ")
                    .append(defaultText(chapter.getTitle(), "未命名章节"))
                    .append("（")
                    .append(toReadableStatus(chapter.getStatus()))
                    .append("）")
                    .append("\n");
        }

        if (endIndex < orderedChapters.size()) {
            builder.append("...后面还有 ").append(orderedChapters.size() - endIndex).append(" 章");
        }
        return builder.toString().trim();
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
                    .append(defaultText(toAbsoluteUrl(material.getViewUrl()), "查看入口待生成"))
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private String toReadableStatus(String status) {
        if (status == null || status.isBlank()) {
            return "未知";
        }
        return switch (status) {
            case "NOT_STARTED" -> "未开始";
            case "IN_PROGRESS" -> "进行中";
            case "COMPLETED" -> "已完成";
            case "PENDING" -> "待开始";
            case "PENDING_REVIEW" -> "待复习";
            default -> status;
        };
    }

    private String toReadableStepType(String stepType) {
        if (stepType == null || stepType.isBlank()) {
            return "先看理论";
        }
        return switch (stepType) {
            case "READ_THEORY" -> "先看理论";
            case "RUN_DEMO" -> "再做 Demo";
            case "SUBMIT_DEMO", "SUBMIT_VALIDATION" -> "提交结果";
            case "REVIEW_SUMMARY" -> "查看评估";
            default -> stepType;
        };
    }

    private int resolveCurrentChapterIndex(List<ChapterSummaryResponse> chapters, int currentChapterNo) {
        for (int i = 0; i < chapters.size(); i++) {
            Integer chapterNo = chapters.get(i).getChapterNo();
            if (chapterNo != null && chapterNo == currentChapterNo) {
                return i;
            }
        }
        for (int i = 0; i < chapters.size(); i++) {
            if ("IN_PROGRESS".equals(chapters.get(i).getStatus())) {
                return i;
            }
        }
        return 0;
    }

    private String buildAiConfigPageUrl(String openId) {
        return toAbsoluteUrl(DEFAULT_AI_CONFIG_PATH_TEMPLATE.formatted(openId));
    }

    private String toAbsoluteUrl(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) {
            return null;
        }
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            return pathOrUrl;
        }
        String publicBaseUrl = feishuProperties.getPublicBaseUrl();
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return pathOrUrl;
        }

        String normalizedBase = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        String normalizedPath = pathOrUrl.startsWith("/") ? pathOrUrl : "/" + pathOrUrl;
        return normalizedBase + normalizedPath;
    }

    private String toAbsoluteLearningSpacePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return "暂无";
        }
        Path root = Paths.get(storageProperties.getLearningSpaceRoot());
        return root.resolve(relativePath).normalize().toAbsolutePath().toString();
    }

    private LearnerDashboardResponse getActiveDashboardOrNull(String openId) {
        try {
            return learnerWorkflowService.getDashboard(openId);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return null;
        }
    }

    private String sanitizeWorkflowMessage(String message) {
        if (message == null || message.isBlank()) {
            return "操作失败，请稍后重试。";
        }
        if (message.contains("current demo step not found")) {
            return "当前还没有进入 Demo 步骤，请先按今日任务完成前置学习。";
        }
        if (message.contains("knowledge node not found")) {
            return "旧任务数据已失效，请重新发送 /today 生成新的学习任务。";
        }
        return message;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}

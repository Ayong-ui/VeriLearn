package com.verilearn.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.verilearn.chapter.dto.ChapterBootstrapResponse;
import com.verilearn.chapter.dto.ChapterDetailResponse;
import com.verilearn.chapter.dto.ChapterDemoEvaluationRequest;
import com.verilearn.chapter.dto.ChapterDemoEvaluationResponse;
import com.verilearn.chapter.dto.ChapterMaterialResponse;
import com.verilearn.chapter.dto.ChapterSummaryResponse;
import com.verilearn.chapter.entity.ChapterMaterial;
import com.verilearn.chapter.entity.ChapterReviewRecord;
import com.verilearn.chapter.entity.ChapterStep;
import com.verilearn.chapter.entity.LearningChapter;
import com.verilearn.chapter.mapper.ChapterMaterialMapper;
import com.verilearn.chapter.mapper.ChapterReviewRecordMapper;
import com.verilearn.chapter.mapper.ChapterStepMapper;
import com.verilearn.chapter.mapper.LearningChapterMapper;
import com.verilearn.chapter.model.StepType;
import com.verilearn.chapter.service.ChapterService;
import com.verilearn.goal.dto.GoalResponse;
import com.verilearn.goal.model.GoalStatus;
import com.verilearn.goal.dto.GoalUpsertRequest;
import com.verilearn.goal.entity.LearningGoal;
import com.verilearn.goal.mapper.LearningGoalMapper;
import com.verilearn.goal.service.GoalService;
import com.verilearn.knowledge.dto.KnowledgeNodeBatchUpsertRequest;
import com.verilearn.knowledge.dto.KnowledgeNodeConfirmResponse;
import com.verilearn.knowledge.dto.KnowledgeNodeDraftRequest;
import com.verilearn.knowledge.dto.KnowledgeNodeResponse;
import com.verilearn.knowledge.entity.KnowledgeNode;
import com.verilearn.knowledge.mapper.KnowledgeNodeMapper;
import com.verilearn.knowledge.service.KnowledgeService;
import com.verilearn.progress.dto.ProgressResponse;
import com.verilearn.progress.service.ProgressService;
import com.verilearn.task.dto.GenerateTaskRequest;
import com.verilearn.task.dto.TaskResponse;
import com.verilearn.task.entity.DailyTask;
import com.verilearn.task.mapper.DailyTaskMapper;
import com.verilearn.task.service.TaskService;
import com.verilearn.user.entity.LearnerUser;
import com.verilearn.user.mapper.LearnerUserMapper;
import com.verilearn.validation.entity.DiversionRecord;
import com.verilearn.validation.entity.ValidationItem;
import com.verilearn.validation.entity.ValidationSubmission;
import com.verilearn.validation.mapper.DiversionRecordMapper;
import com.verilearn.validation.mapper.ValidationItemMapper;
import com.verilearn.validation.mapper.ValidationSubmissionMapper;
import com.verilearn.workflow.dto.LearningRouteChapter;
import com.verilearn.workflow.dto.LearningRouteContentResponse;
import com.verilearn.workflow.dto.LearningRoutePlan;
import com.verilearn.workflow.dto.LearnerCurrentContextResponse;
import com.verilearn.workflow.dto.LearnerDashboardResponse;
import com.verilearn.workflow.dto.LearnerDemoSubmissionRequest;
import com.verilearn.workflow.dto.LearnerMaterialReference;
import com.verilearn.workflow.dto.LearnerSetupRequest;
import com.verilearn.workflow.dto.LearnerSetupResponse;
import com.verilearn.workflow.service.LearnerWorkflowService;
import com.verilearn.workflow.service.LearningRouteService;
import com.verilearn.workflow.service.TopicValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LearnerWorkflowServiceImpl implements LearnerWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(LearnerWorkflowServiceImpl.class);

    private static final String DEFAULT_TARGET_LEVEL = "intern";
    private static final Integer DEFAULT_DAILY_MINUTES = 120;
    private static final String ROUTE_CONTENT_URL_TEMPLATE = "/api/learners/%s/learning-route";
    private static final String ROUTE_VIEW_URL_TEMPLATE = "/learning-routes/%s/view";
    private static final String MATERIAL_CONTENT_URL_TEMPLATE = "/api/materials/%d/content";
    private static final String MATERIAL_VIEW_URL_TEMPLATE = "/materials/%d/view";

    private final GoalService goalService;
    private final ChapterService chapterService;
    private final KnowledgeService knowledgeService;
    private final TaskService taskService;
    private final ProgressService progressService;
    private final LearnerUserMapper learnerUserMapper;
    private final LearningGoalMapper learningGoalMapper;
    private final TopicValidationService topicValidationService;
    private final LearningRouteService learningRouteService;
    private final DailyTaskMapper dailyTaskMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final LearningChapterMapper learningChapterMapper;
    private final ChapterStepMapper chapterStepMapper;
    private final ChapterMaterialMapper chapterMaterialMapper;
    private final ChapterReviewRecordMapper chapterReviewRecordMapper;
    private final ValidationItemMapper validationItemMapper;
    private final ValidationSubmissionMapper validationSubmissionMapper;
    private final DiversionRecordMapper diversionRecordMapper;

    public LearnerWorkflowServiceImpl(
            GoalService goalService,
            ChapterService chapterService,
            KnowledgeService knowledgeService,
            TaskService taskService,
            ProgressService progressService,
            LearnerUserMapper learnerUserMapper,
            LearningGoalMapper learningGoalMapper,
            TopicValidationService topicValidationService,
            LearningRouteService learningRouteService,
            DailyTaskMapper dailyTaskMapper,
            KnowledgeNodeMapper knowledgeNodeMapper,
            LearningChapterMapper learningChapterMapper,
            ChapterStepMapper chapterStepMapper,
            ChapterMaterialMapper chapterMaterialMapper,
            ChapterReviewRecordMapper chapterReviewRecordMapper,
            ValidationItemMapper validationItemMapper,
            ValidationSubmissionMapper validationSubmissionMapper,
            DiversionRecordMapper diversionRecordMapper
    ) {
        this.goalService = goalService;
        this.chapterService = chapterService;
        this.knowledgeService = knowledgeService;
        this.taskService = taskService;
        this.progressService = progressService;
        this.learnerUserMapper = learnerUserMapper;
        this.learningGoalMapper = learningGoalMapper;
        this.topicValidationService = topicValidationService;
        this.learningRouteService = learningRouteService;
        this.dailyTaskMapper = dailyTaskMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.learningChapterMapper = learningChapterMapper;
        this.chapterStepMapper = chapterStepMapper;
        this.chapterMaterialMapper = chapterMaterialMapper;
        this.chapterReviewRecordMapper = chapterReviewRecordMapper;
        this.validationItemMapper = validationItemMapper;
        this.validationSubmissionMapper = validationSubmissionMapper;
        this.diversionRecordMapper = diversionRecordMapper;
    }

    @Override
    @Transactional
    public LearnerSetupResponse setupLearner(LearnerSetupRequest request) {
        topicValidationService.validateTopicOrThrow(request.getTopic());
        ensureNoBlockingActiveGoal(request.getFeishuOpenId());

        LearnerUser learnerUser = getOrCreateLearnerByOpenId(request.getFeishuOpenId());
        LearningRoutePlan routePlan = learningRouteService.generateLearningRoute(
                learnerUser.getId(),
                request.getTopic(),
                effectiveTargetLevel(request.getTargetLevel())
        );

        GoalResponse goal = goalService.saveGoal(toGoalRequest(request));
        KnowledgeNodeBatchUpsertRequest nodeRequest = new KnowledgeNodeBatchUpsertRequest();
        nodeRequest.setNodes(buildDraftNodes(routePlan.getChapters()));
        knowledgeService.replaceKnowledgeNodes(goal.getGoalId(), nodeRequest);

        KnowledgeNodeConfirmResponse confirmResponse = knowledgeService.confirmKnowledgeNodes(goal.getGoalId());
        List<KnowledgeNodeResponse> savedNodes = knowledgeService.listKnowledgeNodes(goal.getGoalId());
        ChapterBootstrapResponse chapterBootstrapResponse = chapterService.bootstrapChapters(goal.getGoalId(), routePlan.getChapters());
        learningRouteService.createOrUpdateRouteFile(goal.getTopic(), routePlan.getMarkdownContent());
        pregenerateFirstChapterIfNecessary(goal.getGoalId());

        LearnerSetupResponse response = new LearnerSetupResponse();
        response.setUserId(goal.getUserId());
        response.setGoalId(goal.getGoalId());
        response.setFeishuOpenId(goal.getFeishuOpenId());
        response.setTopic(goal.getTopic());
        response.setTargetLevel(goal.getTargetLevel());
        response.setDailyMinutes(goal.getDailyMinutes());
        response.setGoalStatus(goal.getStatus());
        response.setInitializedNodeCount(confirmResponse.getInitializedCount());
        response.setChapterCount(chapterBootstrapResponse.getChapterCount());
        response.setKnowledgeNodes(savedNodes);
        return response;
    }

    @Override
    public TaskResponse generateTodayTask(String feishuOpenId) {
        LearningGoal goal = getActiveGoalByOpenId(feishuOpenId);
        GenerateTaskRequest request = new GenerateTaskRequest();
        request.setGoalId(goal.getId());
        TaskResponse task = taskService.generateTask(request);
        return enrichTaskResponse(feishuOpenId, goal, task);
    }

    @Override
    public ProgressResponse getProgress(String feishuOpenId) {
        LearnerUser learnerUser = getLearnerByOpenId(feishuOpenId);
        return progressService.getProgress(learnerUser.getId());
    }

    @Override
    public List<ChapterSummaryResponse> listChapters(String feishuOpenId) {
        LearningGoal goal = getActiveGoalByOpenId(feishuOpenId);
        return chapterService.listChaptersByGoalId(goal.getId());
    }

    @Override
    public LearnerDashboardResponse getDashboard(String feishuOpenId) {
        LearnerUser learnerUser = getLearnerByOpenId(feishuOpenId);
        LearningGoal goal = getActiveGoalByOpenId(feishuOpenId);
        ProgressResponse progress = progressService.getProgress(learnerUser.getId());
        List<ChapterSummaryResponse> chapters = chapterService.listChaptersByGoalId(goal.getId());
        List<ChapterSummaryResponse> pendingReviews = chapterService.listPendingReviewsByGoalId(goal.getId());
        TaskResponse todayTask = findTodayTaskForGoal(feishuOpenId, goal, learnerUser.getId());
        ChapterDetailResponse currentChapter = resolveCurrentChapter(goal.getId(), todayTask);
        LearningRouteContentResponse route = getLearningRoute(feishuOpenId);

        LearnerDashboardResponse response = new LearnerDashboardResponse();
        response.setUserId(learnerUser.getId());
        response.setGoalId(goal.getId());
        response.setFeishuOpenId(feishuOpenId);
        response.setTopic(goal.getTopic());
        response.setTargetLevel(goal.getTargetLevel());
        response.setDailyMinutes(goal.getDailyMinutes());
        response.setGoalStatus(goal.getStatus());
        response.setTodayTask(todayTask);
        response.setProgress(progress);
        response.setRouteFilePath(route.getFilePath());
        response.setRouteAbsolutePath(route.getAbsoluteFilePath());
        response.setRouteContentUrl(route.getContentUrl());
        response.setRouteViewUrl(route.getViewUrl());
        response.setCurrentChapterNo(currentChapter == null || currentChapter.getChapterNo() == null ? 0 : currentChapter.getChapterNo());
        response.setTotalChapterCount(chapters.size());
        response.setChapterCount(chapters.size());
        response.setPendingReviewCount(pendingReviews.size());
        response.setCurrentChapter(currentChapter);
        response.setCurrentMaterials(buildCurrentMaterials(currentChapter));
        response.setChapters(chapters);
        response.setPendingReviews(pendingReviews);
        return response;
    }

    @Override
    public LearnerCurrentContextResponse getCurrentContext(String feishuOpenId) {
        LearnerUser learnerUser = getLearnerByOpenId(feishuOpenId);
        LearningGoal goal = getActiveGoalByOpenId(feishuOpenId);
        TaskResponse todayTask = findTodayTaskForGoal(feishuOpenId, goal, learnerUser.getId());
        ChapterDetailResponse currentChapter = resolveCurrentChapter(goal.getId(), todayTask);
        LearnerMaterialReference evaluationMaterial = extractMaterial(currentChapter, "EVALUATION_REPORT");
        LearnerMaterialReference nextStepMaterial = extractMaterial(currentChapter, "NEXT_STEP_NOTE");
        LearningRouteContentResponse route = getLearningRoute(feishuOpenId);

        LearnerCurrentContextResponse response = new LearnerCurrentContextResponse();
        response.setUserId(learnerUser.getId());
        response.setGoalId(goal.getId());
        response.setFeishuOpenId(feishuOpenId);
        response.setTopic(goal.getTopic());
        response.setGoalStatus(goal.getStatus());
        response.setRouteFilePath(route.getFilePath());
        response.setRouteAbsolutePath(route.getAbsoluteFilePath());
        response.setRouteContentUrl(route.getContentUrl());
        response.setRouteViewUrl(route.getViewUrl());
        response.setTodayTask(todayTask);
        response.setCurrentChapter(currentChapter);
        if (evaluationMaterial != null) {
            response.setEvaluationMaterialId(evaluationMaterial.getMaterialId());
            response.setEvaluationFilePath(evaluationMaterial.getFilePath());
            response.setEvaluationContentUrl(MATERIAL_CONTENT_URL_TEMPLATE.formatted(evaluationMaterial.getMaterialId()));
            response.setEvaluationViewUrl(MATERIAL_VIEW_URL_TEMPLATE.formatted(evaluationMaterial.getMaterialId()));
        }
        if (nextStepMaterial != null) {
            response.setNextStepMaterialId(nextStepMaterial.getMaterialId());
            response.setNextStepFilePath(nextStepMaterial.getFilePath());
            response.setNextStepContentUrl(MATERIAL_CONTENT_URL_TEMPLATE.formatted(nextStepMaterial.getMaterialId()));
            response.setNextStepViewUrl(MATERIAL_VIEW_URL_TEMPLATE.formatted(nextStepMaterial.getMaterialId()));
        }
        response.setCurrentMaterials(buildCurrentMaterials(currentChapter));
        return response;
    }

    @Override
    public LearningRouteContentResponse getLearningRoute(String feishuOpenId) {
        LearningGoal goal = getActiveGoalByOpenId(feishuOpenId);
        return learningRouteService.buildRouteContentResponse(
                goal.getTopic(),
                ROUTE_CONTENT_URL_TEMPLATE.formatted(feishuOpenId),
                ROUTE_VIEW_URL_TEMPLATE.formatted(feishuOpenId)
        );
    }

    @Override
    public String getLearningRouteViewHtml(String feishuOpenId) {
        LearningRouteContentResponse route = getLearningRoute(feishuOpenId);
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <style>
                        body { font-family: "Segoe UI", "Microsoft YaHei", sans-serif; margin: 0; background: #f5f7fb; color: #1f2937; }
                        .container { max-width: 980px; margin: 40px auto; padding: 0 20px; }
                        .header, .content { background: #ffffff; border: 1px solid #dbe4f0; border-radius: 18px; padding: 24px 28px; box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06); }
                        .content { margin-top: 24px; }
                        .badge { display: inline-block; padding: 6px 12px; border-radius: 999px; background: #dbeafe; color: #1d4ed8; font-size: 13px; font-weight: 600; }
                        h1 { margin: 16px 0 10px; font-size: 30px; }
                        .meta { margin: 6px 0; color: #4b5563; font-size: 14px; }
                        a { text-decoration: none; color: #ffffff; background: #2563eb; padding: 10px 14px; border-radius: 10px; font-size: 14px; display: inline-block; margin-top: 18px; }
                        pre { margin: 0; white-space: pre-wrap; word-break: break-word; line-height: 1.72; font-size: 15px; font-family: "JetBrains Mono", "Cascadia Code", "Consolas", "Microsoft YaHei", monospace; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <section class="header">
                            <span class="badge">学习路线</span>
                            <h1>%s</h1>
                            <div class="meta">文件路径：%s</div>
                            <div class="meta">绝对路径：%s</div>
                            <a href="%s">查看 JSON 内容接口</a>
                        </section>
                        <section class="content">
                            <pre>%s</pre>
                        </section>
                    </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(route.getTopic()),
                escapeHtml(route.getTopic()),
                escapeHtml(route.getFilePath()),
                escapeHtml(route.getAbsoluteFilePath()),
                route.getContentUrl(),
                escapeHtml(route.getContentText())
        );
    }

    @Override
    public List<String> generateTopicOptions(String feishuOpenId, String topic) {
        LearnerUser learnerUser = getOrCreateLearnerByOpenId(feishuOpenId);
        return learningRouteService.generateTopicOptions(learnerUser.getId(), topic);
    }

    @Override
    @Transactional
    public void clearLearningRoute(String feishuOpenId, String topic) {
        LearningGoal goal = getActiveGoalOrNull(feishuOpenId);
        if (goal == null) {
            throw new IllegalArgumentException("当前没有可清理的进行中学习路线。");
        }
        if (!normalizeTopic(goal.getTopic()).equals(normalizeTopic(topic))) {
            throw new IllegalArgumentException("当前进行中的学习路线不是 " + topic + "。");
        }

        List<LearningChapter> chapters = learningChapterMapper.selectList(
                new LambdaQueryWrapper<LearningChapter>()
                        .eq(LearningChapter::getGoalId, goal.getId())
        );
        List<Long> chapterIds = chapters.stream().map(LearningChapter::getId).toList();
        List<Long> taskIds = filterTaskIdsByGoal(goal.getId(), goal.getUserId());

        if (!taskIds.isEmpty()) {
            validationSubmissionMapper.delete(new LambdaQueryWrapper<ValidationSubmission>().in(ValidationSubmission::getTaskId, taskIds));
            validationItemMapper.delete(new LambdaQueryWrapper<ValidationItem>().in(ValidationItem::getTaskId, taskIds));
            diversionRecordMapper.delete(new LambdaQueryWrapper<DiversionRecord>().in(DiversionRecord::getTaskId, taskIds));
            dailyTaskMapper.delete(new LambdaQueryWrapper<DailyTask>().in(DailyTask::getId, taskIds));
        }

        if (!chapterIds.isEmpty()) {
            chapterStepMapper.delete(new LambdaQueryWrapper<ChapterStep>().in(ChapterStep::getChapterId, chapterIds));
            chapterMaterialMapper.delete(new LambdaQueryWrapper<ChapterMaterial>().in(ChapterMaterial::getChapterId, chapterIds));
            chapterReviewRecordMapper.delete(new LambdaQueryWrapper<ChapterReviewRecord>().in(ChapterReviewRecord::getChapterId, chapterIds));
            learningChapterMapper.delete(new LambdaQueryWrapper<LearningChapter>().in(LearningChapter::getId, chapterIds));
        }

        knowledgeNodeMapper.delete(new LambdaQueryWrapper<KnowledgeNode>().eq(KnowledgeNode::getGoalId, goal.getId()));
        learningGoalMapper.deleteById(goal.getId());
        try {
            learningRouteService.deleteRouteDirectory(goal.getTopic());
        } catch (RuntimeException exception) {
            log.warn("failed to delete learning route directory after clearing route: topic={}", goal.getTopic(), exception);
        }
    }

    @Override
    @Transactional
    public ChapterDemoEvaluationResponse evaluateDemoSubmission(String feishuOpenId, Long chapterId, ChapterDemoEvaluationRequest request) {
        LearningGoal goal = getActiveGoalByOpenId(feishuOpenId);
        ensureChapterBelongsToGoal(goal.getId(), chapterId);
        ChapterDemoEvaluationResponse response = chapterService.evaluateDemoSubmission(chapterId, request);
        pregenerateNextChapterIfNecessary(goal.getId(), chapterId);
        refreshGoalStatusIfCompleted(goal.getId());
        return response;
    }

    @Override
    @Transactional
    public ChapterDemoEvaluationResponse evaluateCurrentDemoSubmission(String feishuOpenId, LearnerDemoSubmissionRequest request) {
        LearningGoal goal = getActiveGoalByOpenId(feishuOpenId);
        LearnerUser learnerUser = getLearnerByOpenId(feishuOpenId);
        TaskResponse todayTask = taskService.findTaskByUserAndDate(learnerUser.getId(), LocalDate.now());
        ChapterDetailResponse currentChapter = resolveCurrentChapter(goal.getId(), todayTask);
        if (currentChapter == null) {
            throw new IllegalArgumentException("current learning chapter not found");
        }
        return evaluateCurrentDemoSubmission(feishuOpenId, currentChapter.getChapterId(), request);
    }

    @Override
    @Transactional
    public ChapterDemoEvaluationResponse evaluateCurrentDemoSubmission(String feishuOpenId, Long chapterId, LearnerDemoSubmissionRequest request) {
        LearningGoal goal = getActiveGoalByOpenId(feishuOpenId);
        ensureChapterBelongsToGoal(goal.getId(), chapterId);
        ChapterDetailResponse chapterDetail = chapterService.getChapterDetail(chapterId);
        if (chapterDetail.getSteps() == null) {
            throw new IllegalArgumentException("current demo step not found");
        }
        Long currentDemoStepId = chapterDetail.getSteps().stream()
                .filter(step -> StepType.RUN_DEMO.name().equals(step.getStepType()))
                .filter(step -> "IN_PROGRESS".equals(step.getStatus()) || "FAILED".equals(step.getStatus()))
                .map(step -> step.getId())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("current demo step not found"));

        ChapterDemoEvaluationRequest evaluationRequest = new ChapterDemoEvaluationRequest();
        evaluationRequest.setStepId(currentDemoStepId);
        evaluationRequest.setSubmissionSummary(request.getSubmissionSummary());
        evaluationRequest.setCodeSnippet(request.getCodeSnippet());
        evaluationRequest.setQuestion(request.getQuestion());
        ChapterDemoEvaluationResponse response = chapterService.evaluateDemoSubmission(chapterId, evaluationRequest);
        pregenerateNextChapterIfNecessary(goal.getId(), chapterId);
        refreshGoalStatusIfCompleted(goal.getId());
        return response;
    }

    private GoalUpsertRequest toGoalRequest(LearnerSetupRequest request) {
        GoalUpsertRequest goalRequest = new GoalUpsertRequest();
        goalRequest.setFeishuOpenId(request.getFeishuOpenId());
        goalRequest.setTopic(request.getTopic());
        goalRequest.setTargetLevel(effectiveTargetLevel(request.getTargetLevel()));
        goalRequest.setDailyMinutes(request.getDailyMinutes() == null ? DEFAULT_DAILY_MINUTES : request.getDailyMinutes());
        return goalRequest;
    }

    private String effectiveTargetLevel(String targetLevel) {
        return targetLevel == null || targetLevel.isBlank() ? DEFAULT_TARGET_LEVEL : targetLevel;
    }

    private List<KnowledgeNodeDraftRequest> buildDraftNodes(List<LearningRouteChapter> routeChapters) {
        List<KnowledgeNodeDraftRequest> nodes = new ArrayList<>();
        for (int i = 0; i < routeChapters.size(); i++) {
            LearningRouteChapter routeChapter = routeChapters.get(i);
            KnowledgeNodeDraftRequest node = new KnowledgeNodeDraftRequest();
            node.setNodeName(routeChapter.getTitle());
            node.setSequenceNo(i + 1);
            nodes.add(node);
        }
        return nodes;
    }

    private LearnerUser getOrCreateLearnerByOpenId(String feishuOpenId) {
        LearnerUser learnerUser = learnerUserMapper.selectOne(
                new LambdaQueryWrapper<LearnerUser>()
                        .eq(LearnerUser::getFeishuOpenId, feishuOpenId)
                        .orderByDesc(LearnerUser::getId)
                        .last("LIMIT 1")
        );
        if (learnerUser != null) {
            return learnerUser;
        }

        LocalDateTime now = LocalDateTime.now();
        LearnerUser newUser = new LearnerUser();
        newUser.setFeishuOpenId(feishuOpenId);
        newUser.setCreatedAt(now);
        newUser.setUpdatedAt(now);
        learnerUserMapper.insert(newUser);
        return newUser;
    }

    private LearnerUser getLearnerByOpenId(String feishuOpenId) {
        LearnerUser learnerUser = learnerUserMapper.selectOne(
                new LambdaQueryWrapper<LearnerUser>()
                        .eq(LearnerUser::getFeishuOpenId, feishuOpenId)
                        .orderByDesc(LearnerUser::getId)
                        .last("LIMIT 1")
        );
        if (learnerUser == null) {
            throw new IllegalArgumentException("learner not found");
        }
        return learnerUser;
    }

    private LearningGoal getActiveGoalByOpenId(String feishuOpenId) {
        LearningGoal goal = getActiveGoalOrNull(feishuOpenId);
        if (goal == null) {
            throw new IllegalArgumentException("当前没有进行中的学习路线，请先发送 /start 学习主题。");
        }
        return goal;
    }

    private LearningGoal getActiveGoalOrNull(String feishuOpenId) {
        LearnerUser learnerUser = learnerUserMapper.selectOne(
                new LambdaQueryWrapper<LearnerUser>()
                        .eq(LearnerUser::getFeishuOpenId, feishuOpenId)
                        .orderByDesc(LearnerUser::getId)
                        .last("LIMIT 1")
        );
        if (learnerUser == null) {
            return null;
        }
        LearningGoal goal = learningGoalMapper.selectOne(
                new LambdaQueryWrapper<LearningGoal>()
                        .eq(LearningGoal::getUserId, learnerUser.getId())
                        .eq(LearningGoal::getStatus, GoalStatus.ACTIVE.name())
                        .orderByDesc(LearningGoal::getId)
                        .last("LIMIT 1")
        );
        if (goal == null) {
            return null;
        }
        refreshGoalStatusIfCompleted(goal.getId());
        LearningGoal refreshed = learningGoalMapper.selectById(goal.getId());
        if (refreshed == null || !GoalStatus.ACTIVE.name().equals(refreshed.getStatus())) {
            return null;
        }
        return refreshed;
    }

    private void ensureNoBlockingActiveGoal(String feishuOpenId) {
        LearningGoal activeGoal = getActiveGoalOrNull(feishuOpenId);
        if (activeGoal == null) {
            return;
        }
        if (!GoalStatus.ACTIVE.name().equals(activeGoal.getStatus())) {
            return;
        }

        LearnerUser learnerUser = getLearnerByOpenId(feishuOpenId);
        TaskResponse todayTask = taskService.findTaskByUserAndDate(learnerUser.getId(), LocalDate.now());
        ChapterDetailResponse currentChapter = resolveCurrentChapter(activeGoal.getId(), todayTask);
        String readableCurrentChapterTitle = currentChapter == null ? "未定位到当前章节" : currentChapter.getTitle();
        String readableCurrentStep = currentChapter == null ? "未定位到当前步骤" : currentChapter.getSteps().stream()
                .filter(step -> "IN_PROGRESS".equals(step.getStatus()))
                .map(step -> switch (step.getStepType()) {
                    case "READ_THEORY" -> "先看理论";
                    case "RUN_DEMO" -> "再做 Demo";
                    case "SUBMIT_DEMO", "SUBMIT_VALIDATION" -> "提交结果";
                    case "REVIEW_SUMMARY" -> "查看评估";
                    default -> step.getStepType();
                })
                .findFirst()
                .orElse("未定位到当前步骤");
        if (activeGoal.getTopic() != null) {
            throw new IllegalArgumentException("""
                    你当前还有未完成的学习任务：
                    当前主题：%s
                    当前章节：%s
                    当前步骤：%s

                    请先发送 /today 继续当前任务。
                    如果你确认要放弃当前方向，再使用 /clear %s。
                    """.formatted(activeGoal.getTopic(), readableCurrentChapterTitle, readableCurrentStep, activeGoal.getTopic()).trim());
        }
    }

    private void ensureChapterBelongsToGoal(Long goalId, Long chapterId) {
        ChapterDetailResponse chapterDetail = chapterService.getChapterDetail(chapterId);
        if (!goalId.equals(chapterDetail.getGoalId())) {
            throw new IllegalArgumentException("chapter does not belong to current learner goal");
        }
    }

    private ChapterDetailResponse resolveCurrentChapter(Long goalId, TaskResponse todayTask) {
        if (todayTask != null
                && todayTask.getGoalId() != null
                && goalId.equals(todayTask.getGoalId())
                && todayTask.getChapterId() != null) {
            return chapterService.getChapterDetail(todayTask.getChapterId());
        }
        List<ChapterSummaryResponse> chapters = chapterService.listChaptersByGoalId(goalId);
        return chapters.stream()
                .filter(chapter -> "IN_PROGRESS".equals(chapter.getStatus()))
                .findFirst()
                .map(ChapterSummaryResponse::getChapterId)
                .map(chapterService::getChapterDetail)
                .orElse(null);
    }

    private LearnerMaterialReference extractMaterial(ChapterDetailResponse chapterDetail, String materialType) {
        if (chapterDetail == null || chapterDetail.getMaterials() == null) {
            return null;
        }
        return chapterDetail.getMaterials().stream()
                .filter(material -> materialType.equals(material.getMaterialType()))
                .findFirst()
                .map(this::toMaterialReference)
                .orElse(null);
    }

    private LearnerMaterialReference toMaterialReference(ChapterMaterialResponse material) {
        return new LearnerMaterialReference(
                material.getId(),
                material.getMaterialType(),
                resolveDisplayName(material.getMaterialType()),
                material.getFilePath(),
                MATERIAL_CONTENT_URL_TEMPLATE.formatted(material.getId()),
                MATERIAL_VIEW_URL_TEMPLATE.formatted(material.getId())
        );
    }

    private List<LearnerMaterialReference> buildCurrentMaterials(ChapterDetailResponse chapterDetail) {
        if (chapterDetail == null || chapterDetail.getMaterials() == null) {
            return List.of();
        }
        return chapterDetail.getMaterials().stream()
                .map(this::toMaterialReference)
                .sorted(Comparator.comparingInt(reference -> materialPriority(reference.getMaterialType())))
                .toList();
    }

    private String resolveDisplayName(String materialType) {
        return switch (materialType) {
            case "THEORY_DOC" -> "理论文档";
            case "DEMO_GUIDE" -> "Demo 任务";
            case "EVALUATION_REPORT" -> "评估报告";
            case "NEXT_STEP_NOTE" -> "下一步建议";
            default -> materialType;
        };
    }

    private int materialPriority(String materialType) {
        return switch (materialType) {
            case "THEORY_DOC" -> 1;
            case "DEMO_GUIDE" -> 2;
            case "EVALUATION_REPORT" -> 3;
            case "NEXT_STEP_NOTE" -> 4;
            default -> 99;
        };
    }

    private void pregenerateFirstChapterIfNecessary(Long goalId) {
        List<ChapterSummaryResponse> chapters = chapterService.listChaptersByGoalId(goalId);
        if (chapters.isEmpty()) {
            return;
        }
        ChapterSummaryResponse firstChapter = chapters.stream()
                .min(Comparator.comparing(ChapterSummaryResponse::getChapterNo))
                .orElse(null);
        if (firstChapter == null) {
            return;
        }
        if (hasCoreMaterials(firstChapter.getChapterId())) {
            return;
        }
        chapterService.generateMaterials(firstChapter.getChapterId());
    }

    private void pregenerateNextChapterIfNecessary(Long goalId, Long currentChapterId) {
        List<ChapterSummaryResponse> chapters = chapterService.listChaptersByGoalId(goalId);
        ChapterSummaryResponse currentChapter = chapters.stream()
                .filter(chapter -> chapter.getChapterId().equals(currentChapterId))
                .findFirst()
                .orElse(null);
        if (currentChapter == null) {
            return;
        }

        ChapterSummaryResponse nextChapter = chapters.stream()
                .filter(chapter -> chapter.getChapterNo() > currentChapter.getChapterNo())
                .min(Comparator.comparing(ChapterSummaryResponse::getChapterNo))
                .orElse(null);
        if (nextChapter == null || hasCoreMaterials(nextChapter.getChapterId())) {
            return;
        }

        try {
            chapterService.generateMaterials(nextChapter.getChapterId());
        } catch (RuntimeException exception) {
            log.warn("failed to pregenerate next chapter materials: goalId={}, currentChapterId={}, nextChapterId={}",
                    goalId, currentChapterId, nextChapter.getChapterId(), exception);
        }
    }

    private boolean hasCoreMaterials(Long chapterId) {
        ChapterDetailResponse chapterDetail = chapterService.getChapterDetail(chapterId);
        if (chapterDetail.getMaterials() == null) {
            return false;
        }
        boolean hasTheory = chapterDetail.getMaterials().stream()
                .anyMatch(material -> "THEORY_DOC".equals(material.getMaterialType())
                        && material.getFilePath() != null
                        && !material.getFilePath().isBlank());
        boolean hasDemo = chapterDetail.getMaterials().stream()
                .anyMatch(material -> "DEMO_GUIDE".equals(material.getMaterialType())
                        && material.getFilePath() != null
                        && !material.getFilePath().isBlank());
        return hasTheory && hasDemo;
    }

    private void refreshGoalStatusIfCompleted(Long goalId) {
        LearningGoal goal = learningGoalMapper.selectById(goalId);
        if (goal == null || !GoalStatus.ACTIVE.name().equals(goal.getStatus())) {
            return;
        }
        List<ChapterSummaryResponse> chapters = chapterService.listChaptersByGoalId(goalId);
        if (!chapters.isEmpty() && chapters.stream().allMatch(chapter -> "COMPLETED".equals(chapter.getStatus()))) {
            goal.setStatus(GoalStatus.COMPLETED.name());
            goal.setUpdatedAt(LocalDateTime.now());
            learningGoalMapper.updateById(goal);
        }
    }

    private List<Long> filterTaskIdsByGoal(Long goalId, Long userId) {
        List<DailyTask> tasks = dailyTaskMapper.selectList(
                new LambdaQueryWrapper<DailyTask>()
                        .eq(DailyTask::getUserId, userId)
        );
        if (tasks.isEmpty()) {
            return List.of();
        }

        Set<Long> nodeIds = tasks.stream()
                .map(DailyTask::getNodeId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> chapterIds = tasks.stream()
                .map(DailyTask::getChapterId).filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, KnowledgeNode> nodeMap = nodeIds.isEmpty() ? Map.of()
                : knowledgeNodeMapper.selectBatchIds(nodeIds).stream()
                        .collect(Collectors.toMap(KnowledgeNode::getId, Function.identity()));
        Map<Long, LearningChapter> chapterMap = chapterIds.isEmpty() ? Map.of()
                : learningChapterMapper.selectBatchIds(chapterIds).stream()
                        .collect(Collectors.toMap(LearningChapter::getId, Function.identity()));

        return tasks.stream()
                .filter(task -> {
                    KnowledgeNode node = task.getNodeId() == null ? null : nodeMap.get(task.getNodeId());
                    if (node != null && goalId.equals(node.getGoalId())) {
                        return true;
                    }
                    LearningChapter chapter = task.getChapterId() == null ? null : chapterMap.get(task.getChapterId());
                    return chapter != null && goalId.equals(chapter.getGoalId());
                })
                .map(DailyTask::getId)
                .toList();
    }

    private String normalizeTopic(String topic) {
        return topic == null ? "" : topic.trim().toLowerCase(Locale.ROOT);
    }

    private TaskResponse findTodayTaskForGoal(String feishuOpenId, LearningGoal goal, Long userId) {
        TaskResponse task = taskService.findTaskByUserAndDate(userId, LocalDate.now());
        if (task == null || !goal.getId().equals(task.getGoalId())) {
            return null;
        }
        return enrichTaskResponse(feishuOpenId, goal, task);
    }

    private TaskResponse enrichTaskResponse(String feishuOpenId, LearningGoal goal, TaskResponse task) {
        if (task == null) {
            return null;
        }
        task.setRouteContentUrl(ROUTE_CONTENT_URL_TEMPLATE.formatted(feishuOpenId));
        task.setRouteViewUrl(ROUTE_VIEW_URL_TEMPLATE.formatted(feishuOpenId));
        if (task.getRouteFilePath() == null || task.getRouteFilePath().isBlank()) {
            task.setRouteFilePath(learningRouteService.buildRouteRelativePath(goal.getTopic()));
        }
        if (task.getRouteAbsolutePath() == null || task.getRouteAbsolutePath().isBlank()) {
            task.setRouteAbsolutePath(learningRouteService.resolveAbsolutePath(task.getRouteFilePath()));
        }
        if (task.getCurrentChapterNo() == null || task.getTotalChapterCount() == null || task.getTotalChapterCount() == 0) {
            List<ChapterSummaryResponse> chapters = chapterService.listChaptersByGoalId(goal.getId());
            task.setTotalChapterCount(chapters.size());
            if (task.getChapterId() != null) {
                chapters.stream()
                        .filter(chapter -> task.getChapterId().equals(chapter.getChapterId()))
                        .findFirst()
                        .ifPresent(chapter -> task.setCurrentChapterNo(chapter.getChapterNo()));
            }
        }
        return task;
    }

    @Override
    @Transactional
    public String completeReviews(String feishuOpenId, String chapterNoText) {
        LearningGoal goal = getActiveGoalByOpenId(feishuOpenId);
        List<ChapterSummaryResponse> pendingReviews = chapterService.listPendingReviewsByGoalId(goal.getId());
        if (pendingReviews.isEmpty()) {
            return "当前没有待复习的章节。";
        }

        Integer targetChapterNo = null;
        if (chapterNoText != null && !chapterNoText.isBlank()) {
            try {
                targetChapterNo = Integer.parseInt(chapterNoText.trim());
            } catch (NumberFormatException exception) {
                return "章序号必须是数字，例如：/review 1";
            }
        }

        int completedCount = 0;
        StringBuilder reviewedTitles = new StringBuilder();
        for (ChapterSummaryResponse chapter : pendingReviews) {
            if (targetChapterNo != null && !targetChapterNo.equals(chapter.getChapterNo())) {
                continue;
            }
            chapterService.completeReview(chapter.getChapterId());
            reviewedTitles.append("  - 第 ").append(chapter.getChapterNo()).append(" 章 ").append(chapter.getTitle()).append("\n");
            completedCount++;
        }

        if (completedCount == 0) {
            return "未找到章序号为 " + targetChapterNo + " 的待复习章节。";
        }
        return "已完成 " + completedCount + " 个章节的复习：\n" + reviewedTitles;
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

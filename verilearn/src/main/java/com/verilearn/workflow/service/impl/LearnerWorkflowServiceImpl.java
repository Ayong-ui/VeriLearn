package com.verilearn.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.verilearn.chapter.dto.ChapterBootstrapResponse;
import com.verilearn.chapter.dto.ChapterDetailResponse;
import com.verilearn.chapter.dto.ChapterDemoEvaluationRequest;
import com.verilearn.chapter.dto.ChapterDemoEvaluationResponse;
import com.verilearn.chapter.dto.ChapterMaterialResponse;
import com.verilearn.chapter.dto.ChapterSummaryResponse;
import com.verilearn.chapter.service.ChapterService;
import com.verilearn.goal.dto.GoalResponse;
import com.verilearn.goal.dto.GoalUpsertRequest;
import com.verilearn.goal.entity.LearningGoal;
import com.verilearn.goal.mapper.LearningGoalMapper;
import com.verilearn.goal.service.GoalService;
import com.verilearn.knowledge.dto.KnowledgeNodeBatchUpsertRequest;
import com.verilearn.knowledge.dto.KnowledgeNodeConfirmResponse;
import com.verilearn.knowledge.dto.KnowledgeNodeDraftRequest;
import com.verilearn.knowledge.dto.KnowledgeNodeResponse;
import com.verilearn.knowledge.service.KnowledgeService;
import com.verilearn.progress.dto.ProgressResponse;
import com.verilearn.progress.service.ProgressService;
import com.verilearn.task.dto.GenerateTaskRequest;
import com.verilearn.task.dto.TaskResponse;
import com.verilearn.task.service.TaskService;
import com.verilearn.user.entity.LearnerUser;
import com.verilearn.user.mapper.LearnerUserMapper;
import com.verilearn.workflow.dto.LearnerSetupRequest;
import com.verilearn.workflow.dto.LearnerCurrentContextResponse;
import com.verilearn.workflow.dto.LearnerDemoSubmissionRequest;
import com.verilearn.workflow.dto.LearnerMaterialReference;
import com.verilearn.workflow.dto.LearnerDashboardResponse;
import com.verilearn.workflow.dto.LearnerSetupResponse;
import com.verilearn.workflow.service.LearnerWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class LearnerWorkflowServiceImpl implements LearnerWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(LearnerWorkflowServiceImpl.class);

    private static final String DEFAULT_TARGET_LEVEL = "intern";
    private static final Integer DEFAULT_DAILY_MINUTES = 120;
    private static final String MATERIAL_CONTENT_URL_TEMPLATE = "/api/materials/%d/content";
    private static final String MATERIAL_VIEW_URL_TEMPLATE = "/materials/%d/view";

    private final GoalService goalService;
    private final ChapterService chapterService;
    private final KnowledgeService knowledgeService;
    private final TaskService taskService;
    private final ProgressService progressService;
    private final LearnerUserMapper learnerUserMapper;
    private final LearningGoalMapper learningGoalMapper;

    public LearnerWorkflowServiceImpl(
            GoalService goalService,
            ChapterService chapterService,
            KnowledgeService knowledgeService,
            TaskService taskService,
            ProgressService progressService,
            LearnerUserMapper learnerUserMapper,
            LearningGoalMapper learningGoalMapper
    ) {
        this.goalService = goalService;
        this.chapterService = chapterService;
        this.knowledgeService = knowledgeService;
        this.taskService = taskService;
        this.progressService = progressService;
        this.learnerUserMapper = learnerUserMapper;
        this.learningGoalMapper = learningGoalMapper;
    }

    @Override
    @Transactional
    public LearnerSetupResponse setupLearner(LearnerSetupRequest request) {
        GoalResponse goal = goalService.saveGoal(toGoalRequest(request));

        KnowledgeNodeBatchUpsertRequest nodeRequest = new KnowledgeNodeBatchUpsertRequest();
        nodeRequest.setNodes(buildDraftNodes(request));
        knowledgeService.replaceKnowledgeNodes(goal.getGoalId(), nodeRequest);

        KnowledgeNodeConfirmResponse confirmResponse = knowledgeService.confirmKnowledgeNodes(goal.getGoalId());
        List<KnowledgeNodeResponse> savedNodes = knowledgeService.listKnowledgeNodes(goal.getGoalId());
        ChapterBootstrapResponse chapterBootstrapResponse = chapterService.bootstrapChapters(goal.getGoalId());
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
        LearningGoal goal = getLatestGoalByOpenId(feishuOpenId);
        GenerateTaskRequest request = new GenerateTaskRequest();
        request.setGoalId(goal.getId());
        return taskService.generateTask(request);
    }

    @Override
    public ProgressResponse getProgress(String feishuOpenId) {
        LearnerUser learnerUser = getLearnerByOpenId(feishuOpenId);
        return progressService.getProgress(learnerUser.getId());
    }

    @Override
    public List<ChapterSummaryResponse> listChapters(String feishuOpenId) {
        LearningGoal goal = getLatestGoalByOpenId(feishuOpenId);
        return chapterService.listChaptersByGoalId(goal.getId());
    }

    @Override
    public LearnerDashboardResponse getDashboard(String feishuOpenId) {
        LearnerUser learnerUser = getLearnerByOpenId(feishuOpenId);
        LearningGoal goal = getLatestGoalByOpenId(feishuOpenId);
        ProgressResponse progress = progressService.getProgress(learnerUser.getId());
        List<ChapterSummaryResponse> chapters = chapterService.listChaptersByGoalId(goal.getId());
        List<ChapterSummaryResponse> pendingReviews = chapterService.listPendingReviewsByGoalId(goal.getId());
        TaskResponse todayTask = taskService.findTaskByUserAndDate(learnerUser.getId(), LocalDate.now());
        ChapterDetailResponse currentChapter = resolveCurrentChapter(goal.getId(), todayTask);

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
        LearningGoal goal = getLatestGoalByOpenId(feishuOpenId);
        TaskResponse todayTask = taskService.findTaskByUserAndDate(learnerUser.getId(), LocalDate.now());
        ChapterDetailResponse currentChapter = resolveCurrentChapter(goal.getId(), todayTask);
        LearnerMaterialReference evaluationMaterial = extractMaterial(currentChapter, "EVALUATION_REPORT");
        LearnerMaterialReference nextStepMaterial = extractMaterial(currentChapter, "NEXT_STEP_NOTE");

        LearnerCurrentContextResponse response = new LearnerCurrentContextResponse();
        response.setUserId(learnerUser.getId());
        response.setGoalId(goal.getId());
        response.setFeishuOpenId(feishuOpenId);
        response.setTopic(goal.getTopic());
        response.setGoalStatus(goal.getStatus());
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
    @Transactional
    public ChapterDemoEvaluationResponse evaluateDemoSubmission(String feishuOpenId, Long chapterId, ChapterDemoEvaluationRequest request) {
        LearningGoal goal = getLatestGoalByOpenId(feishuOpenId);
        ensureChapterBelongsToGoal(goal.getId(), chapterId);
        ChapterDemoEvaluationResponse response = chapterService.evaluateDemoSubmission(chapterId, request);
        pregenerateNextChapterIfNecessary(goal.getId(), chapterId);
        return response;
    }

    @Override
    @Transactional
    public ChapterDemoEvaluationResponse evaluateCurrentDemoSubmission(String feishuOpenId, LearnerDemoSubmissionRequest request) {
        LearningGoal goal = getLatestGoalByOpenId(feishuOpenId);
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
        LearningGoal goal = getLatestGoalByOpenId(feishuOpenId);
        ensureChapterBelongsToGoal(goal.getId(), chapterId);
        ChapterDetailResponse chapterDetail = chapterService.getChapterDetail(chapterId);
        Long currentDemoStepId = chapterDetail.getSteps().stream()
                .filter(step -> "RUN_DEMO".equals(step.getStepType()))
                .filter(step -> "IN_PROGRESS".equals(step.getStatus()))
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
        return response;
    }

    private GoalUpsertRequest toGoalRequest(LearnerSetupRequest request) {
        GoalUpsertRequest goalRequest = new GoalUpsertRequest();
        goalRequest.setFeishuOpenId(request.getFeishuOpenId());
        goalRequest.setTopic(request.getTopic());
        goalRequest.setTargetLevel(
                request.getTargetLevel() == null || request.getTargetLevel().isBlank()
                        ? DEFAULT_TARGET_LEVEL
                        : request.getTargetLevel()
        );
        goalRequest.setDailyMinutes(request.getDailyMinutes() == null ? DEFAULT_DAILY_MINUTES : request.getDailyMinutes());
        return goalRequest;
    }

    private List<KnowledgeNodeDraftRequest> buildDraftNodes(LearnerSetupRequest request) {
        List<String> nodeNames = request.getNodeNames();
        if (nodeNames == null || nodeNames.isEmpty()) {
            nodeNames = buildDefaultNodeNames(request.getTopic());
        }

        List<KnowledgeNodeDraftRequest> nodes = new ArrayList<>();
        for (int i = 0; i < nodeNames.size(); i++) {
            KnowledgeNodeDraftRequest node = new KnowledgeNodeDraftRequest();
            node.setNodeName(nodeNames.get(i));
            node.setSequenceNo(i + 1);
            nodes.add(node);
        }
        return nodes;
    }

    private List<String> buildDefaultNodeNames(String topic) {
        return List.of(
                topic + " fundamentals",
                topic + " environment setup",
                topic + " first practical example",
                topic + " real usage scenario"
        );
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

    private LearningGoal getLatestGoalByOpenId(String feishuOpenId) {
        LearnerUser learnerUser = getLearnerByOpenId(feishuOpenId);
        LearningGoal goal = learningGoalMapper.selectOne(
                new LambdaQueryWrapper<LearningGoal>()
                        .eq(LearningGoal::getUserId, learnerUser.getId())
                        .orderByDesc(LearningGoal::getId)
                        .last("LIMIT 1")
        );
        if (goal == null) {
            throw new IllegalArgumentException("goal not found");
        }
        return goal;
    }

    private void ensureChapterBelongsToGoal(Long goalId, Long chapterId) {
        ChapterDetailResponse chapterDetail = chapterService.getChapterDetail(chapterId);
        if (!goalId.equals(chapterDetail.getGoalId())) {
            throw new IllegalArgumentException("chapter does not belong to current learner goal");
        }
    }

    private ChapterDetailResponse resolveCurrentChapter(Long goalId, TaskResponse todayTask) {
        if (todayTask != null && todayTask.getChapterId() != null) {
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
            case "THEORY_DOC" -> "\u7406\u8bba\u6587\u6863";
            case "DEMO_GUIDE" -> "Demo \u4efb\u52a1";
            case "EVALUATION_REPORT" -> "\u8bc4\u4f30\u62a5\u544a";
            case "NEXT_STEP_NOTE" -> "\u4e0b\u4e00\u6b65\u5efa\u8bae";
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
        try {
            chapterService.generateMaterials(firstChapter.getChapterId());
        } catch (RuntimeException exception) {
            log.warn("failed to prepare first chapter materials: goalId={}, chapterId={}", goalId, firstChapter.getChapterId(), exception);
        }
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
}

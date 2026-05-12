package com.verilearn.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.verilearn.chapter.entity.ChapterMaterial;
import com.verilearn.chapter.entity.ChapterStep;
import com.verilearn.chapter.entity.LearningChapter;
import com.verilearn.chapter.mapper.ChapterMaterialMapper;
import com.verilearn.chapter.mapper.ChapterStepMapper;
import com.verilearn.chapter.mapper.LearningChapterMapper;
import com.verilearn.goal.entity.LearningGoal;
import com.verilearn.goal.mapper.LearningGoalMapper;
import com.verilearn.knowledge.entity.KnowledgeNode;
import com.verilearn.knowledge.mapper.KnowledgeNodeMapper;
import com.verilearn.task.dto.GenerateTaskRequest;
import com.verilearn.task.dto.TaskResponse;
import com.verilearn.task.entity.DailyTask;
import com.verilearn.task.mapper.DailyTaskMapper;
import com.verilearn.task.service.TaskService;
import com.verilearn.validation.service.ValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    private static final String PENDING_STATUS = "PENDING";
    private static final String NOT_STARTED_STATUS = "NOT_STARTED";
    private static final String IN_PROGRESS_STATUS = "IN_PROGRESS";
    private static final String NEEDS_RETRY_STATUS = "NEEDS_RETRY";
    private static final String COMPLETED_STATUS = "COMPLETED";
    private static final String MATERIAL_THEORY_DOC = "THEORY_DOC";
    private static final String MATERIAL_DEMO_GUIDE = "DEMO_GUIDE";

    private final LearningGoalMapper learningGoalMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final DailyTaskMapper dailyTaskMapper;
    private final LearningChapterMapper learningChapterMapper;
    private final ChapterStepMapper chapterStepMapper;
    private final ChapterMaterialMapper chapterMaterialMapper;
    private final ValidationService validationService;

    public TaskServiceImpl(
            LearningGoalMapper learningGoalMapper,
            KnowledgeNodeMapper knowledgeNodeMapper,
            DailyTaskMapper dailyTaskMapper,
            LearningChapterMapper learningChapterMapper,
            ChapterStepMapper chapterStepMapper,
            ChapterMaterialMapper chapterMaterialMapper,
            ValidationService validationService
    ) {
        this.learningGoalMapper = learningGoalMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.dailyTaskMapper = dailyTaskMapper;
        this.learningChapterMapper = learningChapterMapper;
        this.chapterStepMapper = chapterStepMapper;
        this.chapterMaterialMapper = chapterMaterialMapper;
        this.validationService = validationService;
    }

    @Override
    @Transactional
    public TaskResponse generateTask(GenerateTaskRequest request) {
        LearningGoal goal = learningGoalMapper.selectById(request.getGoalId());
        if (goal == null) {
            throw new IllegalArgumentException("goal not found");
        }

        LocalDate taskDate = request.getTaskDate() == null ? LocalDate.now() : request.getTaskDate();
        DailyTask existingTask = dailyTaskMapper.selectOne(
                new LambdaQueryWrapper<DailyTask>()
                        .eq(DailyTask::getUserId, goal.getUserId())
                        .eq(DailyTask::getTaskDate, taskDate)
                        .orderByDesc(DailyTask::getId)
                        .last("LIMIT 1")
        );
        if (existingTask != null) {
            return toTaskResponse(existingTask);
        }

        ChapterTaskContext chapterTaskContext = resolveChapterTaskContext(goal.getId());
        if (chapterTaskContext != null) {
            return createChapterTask(goal, taskDate, chapterTaskContext);
        }

        return createKnowledgeNodeTask(goal, taskDate);
    }

    @Override
    public TaskResponse findTaskByUserAndDate(Long userId, LocalDate taskDate) {
        DailyTask task = dailyTaskMapper.selectOne(
                new LambdaQueryWrapper<DailyTask>()
                        .eq(DailyTask::getUserId, userId)
                        .eq(DailyTask::getTaskDate, taskDate)
                        .orderByDesc(DailyTask::getId)
                        .last("LIMIT 1")
        );
        return task == null ? null : toTaskResponse(task);
    }

    private ChapterTaskContext resolveChapterTaskContext(Long goalId) {
        List<LearningChapter> chapters = learningChapterMapper.selectList(
                new LambdaQueryWrapper<LearningChapter>()
                        .eq(LearningChapter::getGoalId, goalId)
                        .orderByAsc(LearningChapter::getChapterNo)
                        .orderByAsc(LearningChapter::getId)
        );
        if (chapters.isEmpty()) {
            return null;
        }

        List<Long> nodeIds = chapters.stream()
                .map(LearningChapter::getNodeId)
                .filter(nodeId -> nodeId != null)
                .distinct()
                .toList();
        if (nodeIds.isEmpty()) {
            return null;
        }

        Map<Long, KnowledgeNode> nodeMap = knowledgeNodeMapper.selectBatchIds(nodeIds).stream()
                .collect(Collectors.toMap(KnowledgeNode::getId, Function.identity()));

        List<LearningChapter> sortedChapters = chapters.stream()
                .filter(chapter -> !COMPLETED_STATUS.equals(chapter.getStatus()))
                .filter(chapter -> nodeMap.containsKey(chapter.getNodeId()))
                .sorted(Comparator
                        .comparingInt((LearningChapter chapter) -> priority(nodeMap.get(chapter.getNodeId()).getStatus()))
                        .thenComparingInt(this::chapterPriority)
                        .thenComparing(LearningChapter::getChapterNo)
                        .thenComparing(LearningChapter::getId))
                .toList();

        for (LearningChapter chapter : sortedChapters) {
            KnowledgeNode node = nodeMap.get(chapter.getNodeId());
            if (node == null || priority(node.getStatus()) == Integer.MAX_VALUE) {
                continue;
            }

            ChapterStep step = resolveActiveStep(chapter);
            if (step != null) {
                return new ChapterTaskContext(chapter, step, node);
            }
        }
        return null;
    }

    private ChapterStep resolveActiveStep(LearningChapter chapter) {
        List<ChapterStep> steps = chapterStepMapper.selectList(
                new LambdaQueryWrapper<ChapterStep>()
                        .eq(ChapterStep::getChapterId, chapter.getId())
                        .orderByAsc(ChapterStep::getStepOrder)
                        .orderByAsc(ChapterStep::getId)
        );
        if (steps.isEmpty()) {
            return null;
        }

        ChapterStep inProgressStep = steps.stream()
                .filter(step -> IN_PROGRESS_STATUS.equals(step.getStatus()))
                .min(Comparator.comparing(ChapterStep::getStepOrder).thenComparing(ChapterStep::getId))
                .orElse(null);
        if (inProgressStep != null) {
            return inProgressStep;
        }

        ChapterStep nextStep = steps.stream()
                .filter(step -> NOT_STARTED_STATUS.equals(step.getStatus()))
                .min(Comparator.comparing(ChapterStep::getStepOrder).thenComparing(ChapterStep::getId))
                .orElse(null);
        if (nextStep != null) {
            LocalDateTime now = LocalDateTime.now();
            nextStep.setStatus(IN_PROGRESS_STATUS);
            nextStep.setUpdatedAt(now);
            chapterStepMapper.updateById(nextStep);

            if (NOT_STARTED_STATUS.equals(chapter.getStatus())) {
                chapter.setStatus(IN_PROGRESS_STATUS);
                chapter.setUpdatedAt(now);
                learningChapterMapper.updateById(chapter);
            }
        }
        return nextStep;
    }

    private TaskResponse createChapterTask(LearningGoal goal, LocalDate taskDate, ChapterTaskContext context) {
        LocalDateTime now = LocalDateTime.now();
        DailyTask task = new DailyTask();
        task.setUserId(goal.getUserId());
        task.setNodeId(context.node().getId());
        task.setChapterId(context.chapter().getId());
        task.setTaskDate(taskDate);
        task.setStepType(context.step().getStepType());
        task.setStepOrder(context.step().getStepOrder());
        task.setGoalText("Chapter " + context.chapter().getChapterNo() + ": " + context.step().getTitle());
        task.setStudyMaterial(resolveStudyMaterial(context.chapter().getId(), context.step()));
        task.setStatus(PENDING_STATUS);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        dailyTaskMapper.insert(task);

        if (NOT_STARTED_STATUS.equals(context.node().getStatus())) {
            context.node().setStatus(IN_PROGRESS_STATUS);
            context.node().setUpdatedAt(now);
            knowledgeNodeMapper.updateById(context.node());
        }

        return toTaskResponse(task, context.node(), context.chapter());
    }

    private String resolveStudyMaterial(Long chapterId, ChapterStep step) {
        if ("SUBMIT_FEEDBACK".equals(step.getStepType())) {
            return step.getInstructionText();
        }

        String expectedMaterialType = "RUN_DEMO".equals(step.getStepType()) ? MATERIAL_DEMO_GUIDE : MATERIAL_THEORY_DOC;
        ChapterMaterial material = chapterMaterialMapper.selectOne(
                new LambdaQueryWrapper<ChapterMaterial>()
                        .eq(ChapterMaterial::getChapterId, chapterId)
                        .eq(ChapterMaterial::getMaterialType, expectedMaterialType)
                        .last("LIMIT 1")
        );
        if (material != null && material.getContentText() != null && !material.getContentText().isBlank()) {
            return material.getContentText();
        }
        return step.getInstructionText();
    }

    private TaskResponse createKnowledgeNodeTask(LearningGoal goal, LocalDate taskDate) {
        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getGoalId, goal.getId())
                        .orderByAsc(KnowledgeNode::getSequenceNo)
                        .orderByAsc(KnowledgeNode::getId)
        );
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("knowledge nodes not found");
        }

        Optional<KnowledgeNode> candidateOptional = nodes.stream()
                .filter(node -> priority(node.getStatus()) < Integer.MAX_VALUE)
                .min(Comparator
                        .comparingInt((KnowledgeNode node) -> priority(node.getStatus()))
                        .thenComparing(KnowledgeNode::getSequenceNo)
                        .thenComparing(KnowledgeNode::getId));

        if (candidateOptional.isEmpty()) {
            throw new IllegalArgumentException("no available knowledge node for task generation");
        }

        KnowledgeNode candidate = candidateOptional.get();
        LocalDateTime now = LocalDateTime.now();

        DailyTask task = new DailyTask();
        task.setUserId(goal.getUserId());
        task.setNodeId(candidate.getId());
        task.setTaskDate(taskDate);
        task.setGoalText("Study knowledge node: " + candidate.getNodeName());
        task.setStudyMaterial("Review this knowledge node and prepare for validation.");
        task.setStatus(PENDING_STATUS);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        dailyTaskMapper.insert(task);

        candidate.setStatus(IN_PROGRESS_STATUS);
        candidate.setUpdatedAt(now);
        knowledgeNodeMapper.updateById(candidate);

        return toTaskResponse(task, candidate, null);
    }

    private int chapterPriority(LearningChapter chapter) {
        if (IN_PROGRESS_STATUS.equals(chapter.getStatus())) {
            return 0;
        }
        if (NOT_STARTED_STATUS.equals(chapter.getStatus())) {
            return 1;
        }
        return Integer.MAX_VALUE;
    }

    private int priority(String status) {
        if (NEEDS_RETRY_STATUS.equals(status)) {
            return 0;
        }
        if (IN_PROGRESS_STATUS.equals(status)) {
            return 1;
        }
        if (NOT_STARTED_STATUS.equals(status)) {
            return 2;
        }
        return Integer.MAX_VALUE;
    }

    private TaskResponse toTaskResponse(DailyTask task) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(task.getNodeId());
        LearningChapter chapter = task.getChapterId() == null ? null : learningChapterMapper.selectById(task.getChapterId());
        return toTaskResponse(task, node, chapter);
    }

    private TaskResponse toTaskResponse(DailyTask task, KnowledgeNode node, LearningChapter chapter) {
        if (node == null) {
            throw new IllegalArgumentException("knowledge node not found");
        }
        TaskResponse response = new TaskResponse();
        response.setTaskId(task.getId());
        response.setUserId(task.getUserId());
        response.setGoalId(node.getGoalId());
        response.setNodeId(node.getId());
        response.setNodeName(node.getNodeName());
        response.setChapterId(task.getChapterId());
        response.setChapterTitle(chapter == null ? null : chapter.getTitle());
        applyMaterialLinks(response, chapter);
        response.setTaskDate(task.getTaskDate());
        response.setStepType(task.getStepType());
        response.setStepOrder(task.getStepOrder());
        response.setGoalText(task.getGoalText());
        response.setStudyMaterial(task.getStudyMaterial());
        response.setStatus(task.getStatus());
        response.setValidationItems(validationService.initializeValidationItems(task.getId(), node.getId(), node.getNodeName(), task.getStepType()));
        return response;
    }

    private void applyMaterialLinks(TaskResponse response, LearningChapter chapter) {
        if (chapter == null) {
            return;
        }

        List<ChapterMaterial> materials = chapterMaterialMapper.selectList(
                new LambdaQueryWrapper<ChapterMaterial>()
                        .eq(ChapterMaterial::getChapterId, chapter.getId())
        );
        if (materials.isEmpty()) {
            return;
        }

        materials.stream()
                .filter(material -> MATERIAL_THEORY_DOC.equals(material.getMaterialType()))
                .findFirst()
                .ifPresent(material -> {
                    response.setTheoryMaterialId(material.getId());
                    response.setTheoryFilePath(material.getFilePath());
                    response.setTheoryContentUrl("/api/materials/" + material.getId() + "/content");
                });

        materials.stream()
                .filter(material -> MATERIAL_DEMO_GUIDE.equals(material.getMaterialType()))
                .findFirst()
                .ifPresent(material -> {
                    response.setDemoMaterialId(material.getId());
                    response.setDemoFilePath(material.getFilePath());
                    response.setDemoContentUrl("/api/materials/" + material.getId() + "/content");
                });
    }

    private record ChapterTaskContext(LearningChapter chapter, ChapterStep step, KnowledgeNode node) {
    }
}

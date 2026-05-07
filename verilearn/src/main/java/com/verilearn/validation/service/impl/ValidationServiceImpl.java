package com.verilearn.validation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.verilearn.ai.dto.AiValidationItemDraft;
import com.verilearn.ai.service.AiValidationService;
import com.verilearn.chapter.entity.ChapterReviewRecord;
import com.verilearn.chapter.entity.ChapterStep;
import com.verilearn.chapter.entity.LearningChapter;
import com.verilearn.chapter.mapper.ChapterReviewRecordMapper;
import com.verilearn.chapter.mapper.ChapterStepMapper;
import com.verilearn.chapter.mapper.LearningChapterMapper;
import com.verilearn.knowledge.entity.KnowledgeNode;
import com.verilearn.knowledge.mapper.KnowledgeNodeMapper;
import com.verilearn.goal.entity.LearningGoal;
import com.verilearn.goal.mapper.LearningGoalMapper;
import com.verilearn.task.entity.DailyTask;
import com.verilearn.task.mapper.DailyTaskMapper;
import com.verilearn.validation.dto.TaskSubmissionItemRequest;
import com.verilearn.validation.dto.TaskSubmitRequest;
import com.verilearn.validation.dto.TaskSubmitResponse;
import com.verilearn.validation.dto.ValidationItemResponse;
import com.verilearn.validation.entity.DiversionRecord;
import com.verilearn.validation.entity.ValidationItem;
import com.verilearn.validation.entity.ValidationSubmission;
import com.verilearn.validation.mapper.DiversionRecordMapper;
import com.verilearn.validation.mapper.ValidationItemMapper;
import com.verilearn.validation.mapper.ValidationSubmissionMapper;
import com.verilearn.validation.service.ValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ValidationServiceImpl implements ValidationService {

    private static final String TASK_PENDING = "PENDING";
    private static final String TASK_COMPLETED = "COMPLETED";
    private static final String NODE_IN_PROGRESS = "IN_PROGRESS";
    private static final String NODE_PASSED = "PASSED";
    private static final String NODE_NEEDS_RETRY = "NEEDS_RETRY";
    private static final String ITEM_VALIDATED = "VALIDATED";
    private static final String RESULT_ADVANCE = "ADVANCE";
    private static final String RESULT_REVIEW = "REVIEW";
    private static final String RESULT_RETRY = "RETRY";
    private static final String RESULT_APPEND_VALIDATION = "APPEND_VALIDATION";
    private static final String STEP_IN_PROGRESS = "IN_PROGRESS";
    private static final String STEP_COMPLETED = "COMPLETED";
    private static final String CHAPTER_IN_PROGRESS = "IN_PROGRESS";
    private static final String CHAPTER_COMPLETED = "COMPLETED";
    private static final String REVIEW_PENDING = "PENDING";

    private final DailyTaskMapper dailyTaskMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final LearningGoalMapper learningGoalMapper;
    private final ValidationItemMapper validationItemMapper;
    private final ValidationSubmissionMapper validationSubmissionMapper;
    private final DiversionRecordMapper diversionRecordMapper;
    private final LearningChapterMapper learningChapterMapper;
    private final ChapterStepMapper chapterStepMapper;
    private final ChapterReviewRecordMapper chapterReviewRecordMapper;
    private final AiValidationService aiValidationService;

    public ValidationServiceImpl(
            DailyTaskMapper dailyTaskMapper,
            KnowledgeNodeMapper knowledgeNodeMapper,
            LearningGoalMapper learningGoalMapper,
            ValidationItemMapper validationItemMapper,
            ValidationSubmissionMapper validationSubmissionMapper,
            DiversionRecordMapper diversionRecordMapper,
            LearningChapterMapper learningChapterMapper,
            ChapterStepMapper chapterStepMapper,
            ChapterReviewRecordMapper chapterReviewRecordMapper,
            AiValidationService aiValidationService
    ) {
        this.dailyTaskMapper = dailyTaskMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.learningGoalMapper = learningGoalMapper;
        this.validationItemMapper = validationItemMapper;
        this.validationSubmissionMapper = validationSubmissionMapper;
        this.diversionRecordMapper = diversionRecordMapper;
        this.learningChapterMapper = learningChapterMapper;
        this.chapterStepMapper = chapterStepMapper;
        this.chapterReviewRecordMapper = chapterReviewRecordMapper;
        this.aiValidationService = aiValidationService;
    }

    @Override
    @Transactional
    public List<ValidationItemResponse> initializeValidationItems(Long taskId, Long nodeId, String nodeName, String stepType) {
        List<ValidationItemResponse> existingItems = listValidationItems(taskId);
        if (!existingItems.isEmpty()) {
            return existingItems;
        }

        LocalDateTime now = LocalDateTime.now();
        List<ValidationItem> createdItems = new ArrayList<>();

        if ("RUN_DEMO".equals(stepType)) {
            createdItems.add(buildItem(
                    taskId,
                    nodeId,
                    1,
                    "DEMO_RESULT",
                    "BASIC",
                    "Describe what happened when you ran the demo for " + nodeName + ".",
                    "Should mention the actual output or behavior and whether it matched the expectation.",
                    now
            ));
            createdItems.add(buildItem(
                    taskId,
                    nodeId,
                    1,
                    "DEMO_ANALYSIS",
                    "BASIC",
                    "Explain one thing you learned from the demo for " + nodeName + ".",
                    "Should connect the demo result to the underlying concept of " + nodeName + ".",
                    now
            ));
        } else if ("SUBMIT_FEEDBACK".equals(stepType)) {
            createdItems.add(buildItem(
                    taskId,
                    nodeId,
                    1,
                    "SUMMARY",
                    "BASIC",
                    "Summarize the key idea of " + nodeName + " and what you can now do with it.",
                    "Should explain the key idea clearly and mention one usable application or takeaway.",
                    now
            ));
            createdItems.add(buildItem(
                    taskId,
                    nodeId,
                    1,
                    "REFLECTION",
                    "BASIC",
                    "Point out one part of " + nodeName + " that still feels weak or worth reviewing.",
                    "Should honestly describe a weak point or explain why review may not be necessary.",
                    now
            ));
        } else {
            createdItems.add(buildItem(
                    taskId,
                    nodeId,
                    1,
                    "CONCEPT",
                    "BASIC",
                    "Explain the core idea of " + nodeName + " in your own words.",
                    "Should mention what " + nodeName + " is used for and its main purpose.",
                    now
            ));
            createdItems.add(buildItem(
                    taskId,
                    nodeId,
                    1,
                    "APPLICATION",
                    "BASIC",
                    "Give one concrete usage scenario or example for " + nodeName + ".",
                    "Should provide a practical example showing where " + nodeName + " is applied.",
                    now
            ));
        }

        for (ValidationItem item : createdItems) {
            validationItemMapper.insert(item);
        }
        return toResponseList(createdItems);
    }

    @Override
    public List<ValidationItemResponse> listValidationItems(Long taskId) {
        List<ValidationItem> items = validationItemMapper.selectList(
                new LambdaQueryWrapper<ValidationItem>()
                        .eq(ValidationItem::getTaskId, taskId)
                        .orderByAsc(ValidationItem::getRoundNo)
                        .orderByAsc(ValidationItem::getId)
        );
        return toResponseList(items);
    }

    @Override
    @Transactional
    public List<ValidationItemResponse> regenerateValidationItems(Long taskId) {
        DailyTask task = dailyTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("task not found");
        }
        long submissionCount = validationSubmissionMapper.selectCount(
                new LambdaQueryWrapper<ValidationSubmission>()
                        .eq(ValidationSubmission::getTaskId, taskId)
        );
        if (submissionCount > 0) {
            throw new IllegalArgumentException("validation items cannot be regenerated after submission");
        }

        KnowledgeNode node = knowledgeNodeMapper.selectById(task.getNodeId());
        if (node == null) {
            throw new IllegalArgumentException("knowledge node not found");
        }
        LearningGoal goal = learningGoalMapper.selectById(node.getGoalId());
        if (goal == null) {
            throw new IllegalArgumentException("goal not found");
        }

        LearningChapter chapter = task.getChapterId() == null ? null : learningChapterMapper.selectById(task.getChapterId());
        String chapterTitle = chapter == null ? node.getNodeName() : chapter.getTitle();

        validationItemMapper.delete(new LambdaQueryWrapper<ValidationItem>().eq(ValidationItem::getTaskId, taskId));

        List<AiValidationItemDraft> generatedItems = aiValidationService.generateValidationItems(
                goal.getTopic(),
                chapterTitle,
                node.getNodeName(),
                task.getStepType()
        );

        LocalDateTime now = LocalDateTime.now();
        List<ValidationItem> createdItems = new ArrayList<>();
        for (AiValidationItemDraft generatedItem : generatedItems) {
            ValidationItem item = buildItem(
                    taskId,
                    node.getId(),
                    1,
                    generatedItem.getItemType(),
                    generatedItem.getDifficultyLevel(),
                    generatedItem.getQuestionText(),
                    generatedItem.getAnswerKey(),
                    now
            );
            validationItemMapper.insert(item);
            createdItems.add(item);
        }
        return toResponseList(createdItems);
    }

    @Override
    @Transactional
    public TaskSubmitResponse submitTask(Long taskId, TaskSubmitRequest request) {
        DailyTask task = dailyTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("task not found");
        }
        if (TASK_COMPLETED.equals(task.getStatus())) {
            throw new IllegalArgumentException("task already completed");
        }

        KnowledgeNode node = knowledgeNodeMapper.selectById(task.getNodeId());
        if (node == null) {
            throw new IllegalArgumentException("knowledge node not found");
        }

        List<ValidationItem> pendingItems = findPendingItems(taskId);
        if (pendingItems.isEmpty()) {
            throw new IllegalArgumentException("no pending validation items");
        }

        Set<Long> pendingItemIds = new HashSet<>();
        for (ValidationItem pendingItem : pendingItems) {
            pendingItemIds.add(pendingItem.getId());
        }

        Set<Long> submittedItemIds = new HashSet<>();
        for (TaskSubmissionItemRequest itemRequest : request.getSubmissions()) {
            submittedItemIds.add(itemRequest.getItemId());
            if (!pendingItemIds.contains(itemRequest.getItemId())) {
                throw new IllegalArgumentException("submission contains invalid validation item");
            }
        }

        if (submittedItemIds.size() != pendingItemIds.size() || !submittedItemIds.containsAll(pendingItemIds)) {
            throw new IllegalArgumentException("all pending validation items must be submitted together");
        }

        LocalDateTime now = LocalDateTime.now();
        for (TaskSubmissionItemRequest itemRequest : request.getSubmissions()) {
            ValidationSubmission submission = new ValidationSubmission();
            submission.setTaskId(taskId);
            submission.setItemId(itemRequest.getItemId());
            submission.setSubmittedAnswer(itemRequest.getSubmittedAnswer());
            submission.setIsCorrect(itemRequest.getCorrect());
            submission.setSubmittedAt(now);
            validationSubmissionMapper.insert(submission);
        }

        int currentRound = pendingItems.get(0).getRoundNo();
        int correctCount = 0;
        for (TaskSubmissionItemRequest itemRequest : request.getSubmissions()) {
            if (Boolean.TRUE.equals(itemRequest.getCorrect())) {
                correctCount++;
            }
        }

        if (currentRound == 1 && correctCount > 0 && correctCount < pendingItems.size()) {
            List<ValidationItemResponse> nextItems = appendFollowUpValidationItem(task, node);
            recordDiversion(taskId, RESULT_APPEND_VALIDATION, "Initial evidence was mixed, so one follow-up validation item was added.");

            TaskSubmitResponse response = new TaskSubmitResponse();
            response.setTaskId(taskId);
            response.setTaskStatus(task.getStatus());
            response.setNodeStatus(node.getStatus());
            response.setResultCode(RESULT_APPEND_VALIDATION);
            response.setReasonText("Evidence is not sufficient yet. Complete the follow-up validation item.");
            response.setFinalized(false);
            response.setNextValidationItems(nextItems);
            return response;
        }

        List<ValidationSubmission> allSubmissions = validationSubmissionMapper.selectList(
                new LambdaQueryWrapper<ValidationSubmission>()
                        .eq(ValidationSubmission::getTaskId, taskId)
        );

        int totalCount = allSubmissions.size();
        int totalCorrectCount = 0;
        for (ValidationSubmission submission : allSubmissions) {
            if (Boolean.TRUE.equals(submission.getIsCorrect())) {
                totalCorrectCount++;
            }
        }

        TaskSubmitResponse response = new TaskSubmitResponse();
        response.setTaskId(taskId);
        response.setFinalized(true);
        response.setNextValidationItems(List.of());

        if (totalCorrectCount == totalCount) {
            finalizeTaskAndLearningState(task, node, RESULT_ADVANCE, now);
            recordDiversion(taskId, RESULT_ADVANCE, "All validation items were answered correctly.");
            response.setTaskStatus(TASK_COMPLETED);
            response.setNodeStatus(node.getStatus());
            response.setResultCode(RESULT_ADVANCE);
            response.setReasonText("The learner passed this task and can move forward.");
            return response;
        }

        double accuracy = totalCount == 0 ? 0D : (double) totalCorrectCount / totalCount;
        if (accuracy >= 0.67D) {
            finalizeTaskAndLearningState(task, node, RESULT_REVIEW, now);
            recordDiversion(taskId, RESULT_REVIEW, "The learner passed after additional validation, but review is recommended.");
            response.setTaskStatus(TASK_COMPLETED);
            response.setNodeStatus(node.getStatus());
            response.setResultCode(RESULT_REVIEW);
            response.setReasonText("The learner can continue, but this task should be reviewed later.");
            return response;
        }

        finalizeTaskAndLearningState(task, node, RESULT_RETRY, now);
        recordDiversion(taskId, RESULT_RETRY, "The learner did not meet the minimum validation threshold.");
        response.setTaskStatus(TASK_COMPLETED);
        response.setNodeStatus(node.getStatus());
        response.setResultCode(RESULT_RETRY);
        response.setReasonText("The learner should retry this task before moving on.");
        return response;
    }

    private List<ValidationItem> findPendingItems(Long taskId) {
        List<ValidationItem> items = validationItemMapper.selectList(
                new LambdaQueryWrapper<ValidationItem>()
                        .eq(ValidationItem::getTaskId, taskId)
        );
        if (items.isEmpty()) {
            return List.of();
        }

        Set<Long> submittedItemIds = new HashSet<>();
        for (ValidationSubmission submission : validationSubmissionMapper.selectList(
                new LambdaQueryWrapper<ValidationSubmission>()
                        .eq(ValidationSubmission::getTaskId, taskId)
        )) {
            submittedItemIds.add(submission.getItemId());
        }

        List<ValidationItem> pendingItems = new ArrayList<>();
        for (ValidationItem item : items) {
            if (!submittedItemIds.contains(item.getId())) {
                pendingItems.add(item);
            }
        }
        pendingItems.sort(Comparator.comparing(ValidationItem::getRoundNo).thenComparing(ValidationItem::getId));
        if (pendingItems.isEmpty()) {
            return List.of();
        }

        Integer currentRound = pendingItems.get(0).getRoundNo();
        List<ValidationItem> currentRoundItems = new ArrayList<>();
        for (ValidationItem pendingItem : pendingItems) {
            if (currentRound.equals(pendingItem.getRoundNo())) {
                currentRoundItems.add(pendingItem);
            }
        }
        return currentRoundItems;
    }

    private List<ValidationItemResponse> appendFollowUpValidationItem(DailyTask task, KnowledgeNode node) {
        LocalDateTime now = LocalDateTime.now();
        String questionText = "Re-explain " + node.getNodeName() + " and correct the part you were unsure about.";
        String answerKey = "Should clearly correct the earlier misunderstanding and restate the key idea accurately.";
        if ("RUN_DEMO".equals(task.getStepType())) {
            questionText = "Re-run or re-check the demo for " + node.getNodeName() + " and explain what you missed the first time.";
            answerKey = "Should explain the corrected demo observation and connect it back to the concept.";
        } else if ("SUBMIT_FEEDBACK".equals(task.getStepType())) {
            questionText = "Summarize " + node.getNodeName() + " again and explain what still needs reinforcement.";
            answerKey = "Should summarize the core idea correctly and point out a concrete weak spot.";
        }

        ValidationItem followUpItem = buildItem(
                task.getId(),
                node.getId(),
                2,
                "FOLLOW_UP",
                "MEDIUM",
                questionText,
                answerKey,
                now
        );
        validationItemMapper.insert(followUpItem);
        return toResponseList(List.of(followUpItem));
    }

    private ValidationItem buildItem(
            Long taskId,
            Long nodeId,
            Integer roundNo,
            String itemType,
            String difficultyLevel,
            String questionText,
            String answerKey,
            LocalDateTime createdAt
    ) {
        ValidationItem item = new ValidationItem();
        item.setTaskId(taskId);
        item.setNodeId(nodeId);
        item.setRoundNo(roundNo);
        item.setItemType(itemType);
        item.setDifficultyLevel(difficultyLevel);
        item.setQuestionText(questionText);
        item.setAnswerKey(answerKey);
        item.setQualityStatus(ITEM_VALIDATED);
        item.setCreatedAt(createdAt);
        return item;
    }

    private void finalizeTaskAndLearningState(DailyTask task, KnowledgeNode node, String resultCode, LocalDateTime now) {
        task.setStatus(TASK_COMPLETED);
        task.setUpdatedAt(now);
        dailyTaskMapper.updateById(task);

        if (task.getChapterId() != null && task.getStepType() != null && task.getStepOrder() != null) {
            updateChapterDrivenState(task, node, resultCode, now);
            knowledgeNodeMapper.updateById(node);
            return;
        }

        if (RESULT_ADVANCE.equals(resultCode) || RESULT_REVIEW.equals(resultCode)) {
            node.setStatus(NODE_PASSED);
        } else {
            node.setStatus(NODE_NEEDS_RETRY);
        }
        node.setUpdatedAt(now);
        knowledgeNodeMapper.updateById(node);
    }

    private void updateChapterDrivenState(DailyTask task, KnowledgeNode node, String resultCode, LocalDateTime now) {
        LearningChapter chapter = learningChapterMapper.selectById(task.getChapterId());
        if (chapter == null) {
            throw new IllegalArgumentException("chapter not found");
        }

        ChapterStep currentStep = chapterStepMapper.selectOne(
                new LambdaQueryWrapper<ChapterStep>()
                        .eq(ChapterStep::getChapterId, task.getChapterId())
                        .eq(ChapterStep::getStepOrder, task.getStepOrder())
                        .last("LIMIT 1")
        );
        if (currentStep == null) {
            throw new IllegalArgumentException("chapter step not found");
        }

        if (RESULT_RETRY.equals(resultCode)) {
            chapter.setStatus(CHAPTER_IN_PROGRESS);
            chapter.setUpdatedAt(now);
            learningChapterMapper.updateById(chapter);

            currentStep.setStatus(STEP_IN_PROGRESS);
            currentStep.setUpdatedAt(now);
            chapterStepMapper.updateById(currentStep);

            node.setStatus(NODE_NEEDS_RETRY);
            node.setUpdatedAt(now);
            return;
        }

        currentStep.setStatus(STEP_COMPLETED);
        currentStep.setUpdatedAt(now);
        chapterStepMapper.updateById(currentStep);

        ChapterStep nextStep = chapterStepMapper.selectOne(
                new LambdaQueryWrapper<ChapterStep>()
                        .eq(ChapterStep::getChapterId, task.getChapterId())
                        .eq(ChapterStep::getStatus, "NOT_STARTED")
                        .orderByAsc(ChapterStep::getStepOrder)
                        .orderByAsc(ChapterStep::getId)
                        .last("LIMIT 1")
        );

        if (nextStep != null) {
            nextStep.setStatus(STEP_IN_PROGRESS);
            nextStep.setUpdatedAt(now);
            chapterStepMapper.updateById(nextStep);

            chapter.setStatus(CHAPTER_IN_PROGRESS);
            chapter.setUpdatedAt(now);
            learningChapterMapper.updateById(chapter);

            node.setStatus(NODE_IN_PROGRESS);
            node.setUpdatedAt(now);
            return;
        }

        chapter.setStatus(CHAPTER_COMPLETED);
        chapter.setUpdatedAt(now);
        learningChapterMapper.updateById(chapter);
        updateChapterReviewToPending(task.getChapterId(), now);

        node.setStatus(NODE_PASSED);
        node.setUpdatedAt(now);
    }

    private void updateChapterReviewToPending(Long chapterId, LocalDateTime now) {
        ChapterReviewRecord reviewRecord = chapterReviewRecordMapper.selectOne(
                new LambdaQueryWrapper<ChapterReviewRecord>()
                        .eq(ChapterReviewRecord::getChapterId, chapterId)
                        .last("LIMIT 1")
        );
        if (reviewRecord == null) {
            throw new IllegalArgumentException("chapter review record not found");
        }
        reviewRecord.setReviewStatus(REVIEW_PENDING);
        reviewRecord.setUpdatedAt(now);
        chapterReviewRecordMapper.updateById(reviewRecord);
    }

    private void recordDiversion(Long taskId, String resultCode, String reasonText) {
        DiversionRecord record = new DiversionRecord();
        record.setTaskId(taskId);
        record.setResultCode(resultCode);
        record.setReasonText(reasonText);
        record.setCreatedAt(LocalDateTime.now());
        diversionRecordMapper.insert(record);
    }

    private List<ValidationItemResponse> toResponseList(List<ValidationItem> items) {
        List<ValidationItemResponse> responses = new ArrayList<>();
        for (ValidationItem item : items) {
            ValidationItemResponse response = new ValidationItemResponse();
            response.setItemId(item.getId());
            response.setRoundNo(item.getRoundNo());
            response.setItemType(item.getItemType());
            response.setDifficultyLevel(item.getDifficultyLevel());
            response.setQuestionText(item.getQuestionText());
            response.setQualityStatus(item.getQualityStatus());
            responses.add(response);
        }
        return responses;
    }
}

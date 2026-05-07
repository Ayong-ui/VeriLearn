package com.verilearn.chapter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.verilearn.ai.dto.AiChapterMaterialResult;
import com.verilearn.ai.service.AiMaterialService;
import com.verilearn.chapter.dto.ChapterBootstrapResponse;
import com.verilearn.chapter.dto.ChapterDetailResponse;
import com.verilearn.chapter.dto.ChapterMaterialResponse;
import com.verilearn.chapter.dto.ChapterStepResponse;
import com.verilearn.chapter.dto.ChapterStepSubmitRequest;
import com.verilearn.chapter.dto.ChapterStepSubmitResponse;
import com.verilearn.chapter.dto.ChapterSummaryResponse;
import com.verilearn.chapter.entity.ChapterMaterial;
import com.verilearn.chapter.entity.ChapterReviewRecord;
import com.verilearn.chapter.entity.ChapterStep;
import com.verilearn.chapter.entity.LearningChapter;
import com.verilearn.chapter.mapper.ChapterMaterialMapper;
import com.verilearn.chapter.mapper.ChapterReviewRecordMapper;
import com.verilearn.chapter.mapper.ChapterStepMapper;
import com.verilearn.chapter.mapper.LearningChapterMapper;
import com.verilearn.chapter.service.ChapterService;
import com.verilearn.goal.entity.LearningGoal;
import com.verilearn.goal.mapper.LearningGoalMapper;
import com.verilearn.knowledge.entity.KnowledgeNode;
import com.verilearn.knowledge.mapper.KnowledgeNodeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ChapterServiceImpl implements ChapterService {

    private static final String CHAPTER_NOT_STARTED = "NOT_STARTED";
    private static final String CHAPTER_IN_PROGRESS = "IN_PROGRESS";
    private static final String CHAPTER_COMPLETED = "COMPLETED";
    private static final String STEP_NOT_STARTED = "NOT_STARTED";
    private static final String STEP_IN_PROGRESS = "IN_PROGRESS";
    private static final String STEP_COMPLETED = "COMPLETED";
    private static final String REVIEW_NOT_READY = "NOT_READY";
    private static final String REVIEW_PENDING = "PENDING";
    private static final String REVIEWED = "REVIEWED";

    private final LearningGoalMapper learningGoalMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final LearningChapterMapper learningChapterMapper;
    private final ChapterStepMapper chapterStepMapper;
    private final ChapterMaterialMapper chapterMaterialMapper;
    private final ChapterReviewRecordMapper chapterReviewRecordMapper;
    private final AiMaterialService aiMaterialService;

    public ChapterServiceImpl(
            LearningGoalMapper learningGoalMapper,
            KnowledgeNodeMapper knowledgeNodeMapper,
            LearningChapterMapper learningChapterMapper,
            ChapterStepMapper chapterStepMapper,
            ChapterMaterialMapper chapterMaterialMapper,
            ChapterReviewRecordMapper chapterReviewRecordMapper,
            AiMaterialService aiMaterialService
    ) {
        this.learningGoalMapper = learningGoalMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.learningChapterMapper = learningChapterMapper;
        this.chapterStepMapper = chapterStepMapper;
        this.chapterMaterialMapper = chapterMaterialMapper;
        this.chapterReviewRecordMapper = chapterReviewRecordMapper;
        this.aiMaterialService = aiMaterialService;
    }

    @Override
    @Transactional
    public ChapterBootstrapResponse bootstrapChapters(Long goalId) {
        LearningGoal goal = getGoalOrThrow(goalId);
        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getGoalId, goalId)
                        .orderByAsc(KnowledgeNode::getSequenceNo)
                        .orderByAsc(KnowledgeNode::getId)
        );
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("knowledge nodes not found");
        }

        deleteExistingChapters(goalId);

        LocalDateTime now = LocalDateTime.now();
        int chapterNo = 1;
        for (KnowledgeNode node : nodes) {
            LearningChapter chapter = new LearningChapter();
            chapter.setGoalId(goal.getId());
            chapter.setNodeId(node.getId());
            chapter.setChapterNo(chapterNo++);
            chapter.setTitle(node.getNodeName());
            chapter.setSummary("Learn the theory, complete the demo, and submit feedback for " + node.getNodeName() + ".");
            chapter.setStatus(CHAPTER_NOT_STARTED);
            chapter.setCreatedAt(now);
            chapter.setUpdatedAt(now);
            learningChapterMapper.insert(chapter);

            createDefaultSteps(chapter, node.getNodeName(), now);
            createDefaultMaterials(chapter, node.getNodeName(), now);
            createReviewRecord(chapter.getId(), now);
        }

        ChapterBootstrapResponse response = new ChapterBootstrapResponse();
        response.setGoalId(goalId);
        response.setChapterCount(nodes.size());
        return response;
    }

    @Override
    public List<ChapterSummaryResponse> listChaptersByGoalId(Long goalId) {
        getGoalOrThrow(goalId);
        List<LearningChapter> chapters = learningChapterMapper.selectList(
                new LambdaQueryWrapper<LearningChapter>()
                        .eq(LearningChapter::getGoalId, goalId)
                        .orderByAsc(LearningChapter::getChapterNo)
                        .orderByAsc(LearningChapter::getId)
        );
        List<ChapterSummaryResponse> responses = new ArrayList<>();
        for (LearningChapter chapter : chapters) {
            ChapterSummaryResponse response = new ChapterSummaryResponse();
            response.setChapterId(chapter.getId());
            response.setGoalId(chapter.getGoalId());
            response.setChapterNo(chapter.getChapterNo());
            response.setTitle(chapter.getTitle());
            response.setSummary(chapter.getSummary());
            response.setStatus(chapter.getStatus());
            response.setCurrentStepType(findCurrentStepType(chapter.getId()));
            response.setReviewStatus(getReviewStatus(chapter.getId()));
            responses.add(response);
        }
        return responses;
    }

    @Override
    public ChapterDetailResponse getChapterDetail(Long chapterId) {
        LearningChapter chapter = getChapterOrThrow(chapterId);
        return toDetailResponse(chapter);
    }

    @Override
    @Transactional
    public ChapterDetailResponse startChapter(Long chapterId) {
        LearningChapter chapter = getChapterOrThrow(chapterId);
        List<ChapterStep> steps = listSteps(chapterId);
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("chapter steps not found");
        }

        LocalDateTime now = LocalDateTime.now();
        if (CHAPTER_NOT_STARTED.equals(chapter.getStatus())) {
            chapter.setStatus(CHAPTER_IN_PROGRESS);
            chapter.setUpdatedAt(now);
            learningChapterMapper.updateById(chapter);
        }

        boolean hasInProgress = steps.stream().anyMatch(step -> STEP_IN_PROGRESS.equals(step.getStatus()));
        if (!hasInProgress) {
            ChapterStep firstPendingStep = steps.stream()
                    .filter(step -> STEP_NOT_STARTED.equals(step.getStatus()))
                    .min(Comparator.comparing(ChapterStep::getStepOrder))
                    .orElse(null);
            if (firstPendingStep != null) {
                firstPendingStep.setStatus(STEP_IN_PROGRESS);
                firstPendingStep.setUpdatedAt(now);
                chapterStepMapper.updateById(firstPendingStep);
            }
        }

        return toDetailResponse(getChapterOrThrow(chapterId));
    }

    @Override
    @Transactional
    public ChapterStepSubmitResponse submitStep(Long chapterId, ChapterStepSubmitRequest request) {
        LearningChapter chapter = getChapterOrThrow(chapterId);
        ChapterStep step = chapterStepMapper.selectById(request.getStepId());
        if (step == null || !chapterId.equals(step.getChapterId())) {
            throw new IllegalArgumentException("chapter step not found");
        }
        if (!STEP_IN_PROGRESS.equals(step.getStatus())) {
            throw new IllegalArgumentException("chapter step is not in progress");
        }

        LocalDateTime now = LocalDateTime.now();
        step.setStatus(STEP_COMPLETED);
        step.setFeedbackNote(request.getFeedbackNote());
        step.setUpdatedAt(now);
        chapterStepMapper.updateById(step);

        if (Boolean.TRUE.equals(request.getNeedsReview())) {
            updateReviewStatus(chapterId, REVIEW_PENDING, null, now);
        }

        ChapterStep nextStep = listSteps(chapterId).stream()
                .filter(item -> STEP_NOT_STARTED.equals(item.getStatus()))
                .min(Comparator.comparing(ChapterStep::getStepOrder))
                .orElse(null);

        ChapterStepSubmitResponse response = new ChapterStepSubmitResponse();
        response.setChapterId(chapterId);
        response.setCompletedStepId(step.getId());

        if (nextStep != null) {
            nextStep.setStatus(STEP_IN_PROGRESS);
            nextStep.setUpdatedAt(now);
            chapterStepMapper.updateById(nextStep);

            chapter.setStatus(CHAPTER_IN_PROGRESS);
            chapter.setUpdatedAt(now);
            learningChapterMapper.updateById(chapter);

            response.setNextStepId(nextStep.getId());
            response.setNextStepType(nextStep.getStepType());
            response.setChapterStatus(CHAPTER_IN_PROGRESS);
            response.setReviewStatus(getReviewStatus(chapterId));
            return response;
        }

        chapter.setStatus(CHAPTER_COMPLETED);
        chapter.setUpdatedAt(now);
        learningChapterMapper.updateById(chapter);
        updateReviewStatus(chapterId, REVIEW_PENDING, null, now);

        response.setChapterStatus(CHAPTER_COMPLETED);
        response.setReviewStatus(REVIEW_PENDING);
        return response;
    }

    @Override
    @Transactional
    public ChapterDetailResponse completeReview(Long chapterId) {
        LearningChapter chapter = getChapterOrThrow(chapterId);
        if (!CHAPTER_COMPLETED.equals(chapter.getStatus())) {
            throw new IllegalArgumentException("chapter must be completed before review");
        }
        updateReviewStatus(chapterId, REVIEWED, LocalDateTime.now(), LocalDateTime.now());
        return toDetailResponse(chapter);
    }

    @Override
    public List<ChapterSummaryResponse> listPendingReviewsByGoalId(Long goalId) {
        getGoalOrThrow(goalId);
        List<ChapterSummaryResponse> allChapters = listChaptersByGoalId(goalId);
        List<ChapterSummaryResponse> pending = new ArrayList<>();
        for (ChapterSummaryResponse chapter : allChapters) {
            if (REVIEW_PENDING.equals(chapter.getReviewStatus())) {
                pending.add(chapter);
            }
        }
        return pending;
    }

    @Override
    @Transactional
    public ChapterDetailResponse generateMaterials(Long chapterId) {
        LearningChapter chapter = getChapterOrThrow(chapterId);
        LearningGoal goal = getGoalOrThrow(chapter.getGoalId());

        AiChapterMaterialResult materialResult = aiMaterialService.generateChapterMaterials(
                goal.getTopic(),
                chapter.getTitle(),
                findCurrentStepType(chapterId)
        );

        List<ChapterMaterial> materials = listMaterials(chapterId);
        if (materials.isEmpty()) {
            throw new IllegalArgumentException("chapter materials not found");
        }

        LocalDateTime now = LocalDateTime.now();
        updateMaterial(materials, "THEORY_DOC", materialResult.getTheoryContent(), materialResult.isGeneratedByAi() ? "AI_GENERATED" : "TEMPLATE_READY", now);
        updateMaterial(materials, "DEMO_GUIDE", materialResult.getDemoGuideContent(), materialResult.isGeneratedByAi() ? "AI_GENERATED" : "TEMPLATE_READY", now);

        if (materialResult.getSummary() != null && !materialResult.getSummary().isBlank()) {
            chapter.setSummary(materialResult.getSummary());
            chapter.setUpdatedAt(now);
            learningChapterMapper.updateById(chapter);
        }

        return toDetailResponse(getChapterOrThrow(chapterId));
    }

    private void createDefaultSteps(LearningChapter chapter, String chapterTitle, LocalDateTime now) {
        insertStep(chapter.getId(), 1, "READ_THEORY", "Read the theory for " + chapterTitle,
                "Study the theory content and understand the core idea.", now);
        insertStep(chapter.getId(), 2, "RUN_DEMO", "Complete the demo for " + chapterTitle,
                "Run the demo or exercise and compare the result with the expected behavior.", now);
        insertStep(chapter.getId(), 3, "SUBMIT_FEEDBACK", "Submit your feedback for " + chapterTitle,
                "Describe what you understood, what was unclear, and whether review is needed.", now);
    }

    private void createDefaultMaterials(LearningChapter chapter, String chapterTitle, LocalDateTime now) {
        insertMaterial(chapter.getId(), "THEORY_DOC", null,
                "# " + chapterTitle + "\n\nExplain what this chapter is, why it matters, and the key idea to remember.", now);
        insertMaterial(chapter.getId(), "DEMO_GUIDE", null,
                "# Demo for " + chapterTitle + "\n\n1. Read the theory.\n2. Run the demo.\n3. Compare the output.\n4. Summarize the result.", now);
    }

    private void createReviewRecord(Long chapterId, LocalDateTime now) {
        ChapterReviewRecord review = new ChapterReviewRecord();
        review.setChapterId(chapterId);
        review.setReviewStatus(REVIEW_NOT_READY);
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        chapterReviewRecordMapper.insert(review);
    }

    private void insertStep(Long chapterId, int stepOrder, String stepType, String title, String instructionText, LocalDateTime now) {
        ChapterStep step = new ChapterStep();
        step.setChapterId(chapterId);
        step.setStepOrder(stepOrder);
        step.setStepType(stepType);
        step.setTitle(title);
        step.setInstructionText(instructionText);
        step.setStatus(STEP_NOT_STARTED);
        step.setCreatedAt(now);
        step.setUpdatedAt(now);
        chapterStepMapper.insert(step);
    }

    private void insertMaterial(Long chapterId, String materialType, String filePath, String contentText, LocalDateTime now) {
        ChapterMaterial material = new ChapterMaterial();
        material.setChapterId(chapterId);
        material.setMaterialType(materialType);
        material.setFilePath(filePath);
        material.setContentText(contentText);
        material.setStatus("READY");
        material.setCreatedAt(now);
        material.setUpdatedAt(now);
        chapterMaterialMapper.insert(material);
    }

    private void updateMaterial(List<ChapterMaterial> materials, String materialType, String contentText, String status, LocalDateTime now) {
        ChapterMaterial material = materials.stream()
                .filter(item -> materialType.equals(item.getMaterialType()))
                .findFirst()
                .orElse(null);
        if (material == null) {
            return;
        }
        material.setContentText(contentText);
        material.setStatus(status);
        material.setUpdatedAt(now);
        chapterMaterialMapper.updateById(material);
    }

    private void deleteExistingChapters(Long goalId) {
        List<LearningChapter> existingChapters = learningChapterMapper.selectList(
                new LambdaQueryWrapper<LearningChapter>()
                        .eq(LearningChapter::getGoalId, goalId)
        );
        if (existingChapters.isEmpty()) {
            return;
        }

        List<Long> chapterIds = existingChapters.stream().map(LearningChapter::getId).toList();
        chapterStepMapper.delete(new LambdaQueryWrapper<ChapterStep>().in(ChapterStep::getChapterId, chapterIds));
        chapterMaterialMapper.delete(new LambdaQueryWrapper<ChapterMaterial>().in(ChapterMaterial::getChapterId, chapterIds));
        chapterReviewRecordMapper.delete(new LambdaQueryWrapper<ChapterReviewRecord>().in(ChapterReviewRecord::getChapterId, chapterIds));
        learningChapterMapper.delete(new LambdaQueryWrapper<LearningChapter>().eq(LearningChapter::getGoalId, goalId));
    }

    private LearningGoal getGoalOrThrow(Long goalId) {
        LearningGoal goal = learningGoalMapper.selectById(goalId);
        if (goal == null) {
            throw new IllegalArgumentException("goal not found");
        }
        return goal;
    }

    private LearningChapter getChapterOrThrow(Long chapterId) {
        LearningChapter chapter = learningChapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new IllegalArgumentException("chapter not found");
        }
        return chapter;
    }

    private List<ChapterStep> listSteps(Long chapterId) {
        return chapterStepMapper.selectList(
                new LambdaQueryWrapper<ChapterStep>()
                        .eq(ChapterStep::getChapterId, chapterId)
                        .orderByAsc(ChapterStep::getStepOrder)
                        .orderByAsc(ChapterStep::getId)
        );
    }

    private List<ChapterMaterial> listMaterials(Long chapterId) {
        return chapterMaterialMapper.selectList(
                new LambdaQueryWrapper<ChapterMaterial>()
                        .eq(ChapterMaterial::getChapterId, chapterId)
                        .orderByAsc(ChapterMaterial::getId)
        );
    }

    private String findCurrentStepType(Long chapterId) {
        return listSteps(chapterId).stream()
                .filter(step -> STEP_IN_PROGRESS.equals(step.getStatus()))
                .min(Comparator.comparing(ChapterStep::getStepOrder))
                .map(ChapterStep::getStepType)
                .orElse(null);
    }

    private String getReviewStatus(Long chapterId) {
        ChapterReviewRecord review = chapterReviewRecordMapper.selectOne(
                new LambdaQueryWrapper<ChapterReviewRecord>()
                        .eq(ChapterReviewRecord::getChapterId, chapterId)
                        .last("LIMIT 1")
        );
        return review == null ? null : review.getReviewStatus();
    }

    private void updateReviewStatus(Long chapterId, String reviewStatus, LocalDateTime lastReviewedAt, LocalDateTime now) {
        ChapterReviewRecord review = chapterReviewRecordMapper.selectOne(
                new LambdaQueryWrapper<ChapterReviewRecord>()
                        .eq(ChapterReviewRecord::getChapterId, chapterId)
                        .last("LIMIT 1")
        );
        if (review == null) {
            throw new IllegalArgumentException("chapter review record not found");
        }
        review.setReviewStatus(reviewStatus);
        if (lastReviewedAt != null) {
            review.setLastReviewedAt(lastReviewedAt);
        }
        review.setUpdatedAt(now);
        chapterReviewRecordMapper.updateById(review);
    }

    private ChapterDetailResponse toDetailResponse(LearningChapter chapter) {
        ChapterDetailResponse response = new ChapterDetailResponse();
        response.setChapterId(chapter.getId());
        response.setGoalId(chapter.getGoalId());
        response.setChapterNo(chapter.getChapterNo());
        response.setTitle(chapter.getTitle());
        response.setSummary(chapter.getSummary());
        response.setStatus(chapter.getStatus());
        response.setReviewStatus(getReviewStatus(chapter.getId()));
        response.setSteps(toStepResponses(listSteps(chapter.getId())));
        response.setMaterials(toMaterialResponses(listMaterials(chapter.getId())));
        return response;
    }

    private List<ChapterStepResponse> toStepResponses(List<ChapterStep> steps) {
        List<ChapterStepResponse> responses = new ArrayList<>();
        for (ChapterStep step : steps) {
            ChapterStepResponse response = new ChapterStepResponse();
            response.setId(step.getId());
            response.setStepOrder(step.getStepOrder());
            response.setStepType(step.getStepType());
            response.setTitle(step.getTitle());
            response.setInstructionText(step.getInstructionText());
            response.setStatus(step.getStatus());
            response.setFeedbackNote(step.getFeedbackNote());
            responses.add(response);
        }
        return responses;
    }

    private List<ChapterMaterialResponse> toMaterialResponses(List<ChapterMaterial> materials) {
        List<ChapterMaterialResponse> responses = new ArrayList<>();
        for (ChapterMaterial material : materials) {
            ChapterMaterialResponse response = new ChapterMaterialResponse();
            response.setId(material.getId());
            response.setMaterialType(material.getMaterialType());
            response.setFilePath(material.getFilePath());
            response.setContentText(material.getContentText());
            response.setStatus(material.getStatus());
            responses.add(response);
        }
        return responses;
    }
}

package com.verilearn.progress.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.verilearn.chapter.entity.ChapterReviewRecord;
import com.verilearn.chapter.entity.LearningChapter;
import com.verilearn.chapter.mapper.ChapterReviewRecordMapper;
import com.verilearn.chapter.mapper.LearningChapterMapper;
import com.verilearn.goal.entity.LearningGoal;
import com.verilearn.goal.mapper.LearningGoalMapper;
import com.verilearn.knowledge.entity.KnowledgeNode;
import com.verilearn.knowledge.mapper.KnowledgeNodeMapper;
import com.verilearn.progress.dto.ProgressResponse;
import com.verilearn.progress.dto.RecentTaskResponse;
import com.verilearn.progress.service.ProgressService;
import com.verilearn.task.entity.DailyTask;
import com.verilearn.task.mapper.DailyTaskMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProgressServiceImpl implements ProgressService {

    private static final String GOAL_ACTIVE = "ACTIVE";

    private final LearningGoalMapper learningGoalMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final DailyTaskMapper dailyTaskMapper;
    private final LearningChapterMapper learningChapterMapper;
    private final ChapterReviewRecordMapper chapterReviewRecordMapper;

    public ProgressServiceImpl(
            LearningGoalMapper learningGoalMapper,
            KnowledgeNodeMapper knowledgeNodeMapper,
            DailyTaskMapper dailyTaskMapper,
            LearningChapterMapper learningChapterMapper,
            ChapterReviewRecordMapper chapterReviewRecordMapper
    ) {
        this.learningGoalMapper = learningGoalMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.dailyTaskMapper = dailyTaskMapper;
        this.learningChapterMapper = learningChapterMapper;
        this.chapterReviewRecordMapper = chapterReviewRecordMapper;
    }

    @Override
    public ProgressResponse getProgress(Long userId) {
        LearningGoal goal = learningGoalMapper.selectOne(
                new LambdaQueryWrapper<LearningGoal>()
                        .eq(LearningGoal::getUserId, userId)
                        .eq(LearningGoal::getStatus, GOAL_ACTIVE)
                        .orderByDesc(LearningGoal::getId)
                        .last("LIMIT 1")
        );
        if (goal == null) {
            goal = learningGoalMapper.selectOne(
                    new LambdaQueryWrapper<LearningGoal>()
                            .eq(LearningGoal::getUserId, userId)
                            .orderByDesc(LearningGoal::getId)
                            .last("LIMIT 1")
            );
        }
        if (goal == null) {
            throw new IllegalArgumentException("goal not found");
        }

        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getGoalId, goal.getId())
                        .orderByAsc(KnowledgeNode::getSequenceNo)
                        .orderByAsc(KnowledgeNode::getId)
        );

        ProgressResponse response = new ProgressResponse();
        response.setUserId(userId);
        response.setGoalId(goal.getId());
        response.setTopic(goal.getTopic());
        response.setTargetLevel(goal.getTargetLevel());
        response.setDailyMinutes(goal.getDailyMinutes());
        response.setGoalStatus(goal.getStatus());
        response.setTotalNodes(nodes.size());

        for (KnowledgeNode node : nodes) {
            String status = node.getStatus();
            if ("NOT_STARTED".equals(status)) {
                response.setNotStartedNodes(response.getNotStartedNodes() + 1);
            } else if ("IN_PROGRESS".equals(status)) {
                response.setInProgressNodes(response.getInProgressNodes() + 1);
            } else if ("PASSED".equals(status)) {
                response.setPassedNodes(response.getPassedNodes() + 1);
            } else if ("NEEDS_RETRY".equals(status)) {
                response.setNeedsRetryNodes(response.getNeedsRetryNodes() + 1);
            }
        }

        List<LearningChapter> chapters = learningChapterMapper.selectList(
                new LambdaQueryWrapper<LearningChapter>()
                        .eq(LearningChapter::getGoalId, goal.getId())
                        .orderByAsc(LearningChapter::getChapterNo)
                        .orderByAsc(LearningChapter::getId)
        );
        response.setTotalChapters(chapters.size());
        for (LearningChapter chapter : chapters) {
            if ("COMPLETED".equals(chapter.getStatus())) {
                response.setCompletedChapters(response.getCompletedChapters() + 1);
            } else if ("IN_PROGRESS".equals(chapter.getStatus())) {
                response.setInProgressChapters(response.getInProgressChapters() + 1);
            }
        }

        if (!chapters.isEmpty()) {
            List<Long> chapterIds = chapters.stream().map(LearningChapter::getId).toList();
            List<ChapterReviewRecord> reviews = chapterReviewRecordMapper.selectList(
                    new LambdaQueryWrapper<ChapterReviewRecord>()
                            .in(ChapterReviewRecord::getChapterId, chapterIds)
            );
            for (ChapterReviewRecord review : reviews) {
                if ("PENDING".equals(review.getReviewStatus())) {
                    response.setPendingReviewChapters(response.getPendingReviewChapters() + 1);
                }
            }
        }

        List<DailyTask> recentTasks = dailyTaskMapper.selectList(
                new LambdaQueryWrapper<DailyTask>()
                        .eq(DailyTask::getUserId, userId)
                        .orderByDesc(DailyTask::getTaskDate)
                        .orderByDesc(DailyTask::getId)
                        .last("LIMIT 5")
        );

        Map<Long, KnowledgeNode> nodeMap = new HashMap<>();
        Map<Long, LearningChapter> chapterMap = new HashMap<>();
        if (!recentTasks.isEmpty()) {
            List<Long> nodeIds = recentTasks.stream().map(DailyTask::getNodeId).distinct().toList();
            for (KnowledgeNode node : knowledgeNodeMapper.selectBatchIds(nodeIds)) {
                nodeMap.put(node.getId(), node);
            }

            List<Long> chapterIds = recentTasks.stream()
                    .map(DailyTask::getChapterId)
                    .filter(chapterId -> chapterId != null)
                    .distinct()
                    .toList();
            if (!chapterIds.isEmpty()) {
                for (LearningChapter chapter : learningChapterMapper.selectBatchIds(chapterIds)) {
                    chapterMap.put(chapter.getId(), chapter);
                }
            }
        }

        List<RecentTaskResponse> taskResponses = new ArrayList<>();
        for (DailyTask task : recentTasks) {
            RecentTaskResponse taskResponse = new RecentTaskResponse();
            taskResponse.setTaskId(task.getId());
            taskResponse.setNodeId(task.getNodeId());
            KnowledgeNode node = nodeMap.get(task.getNodeId());
            LearningChapter chapter = task.getChapterId() == null ? null : chapterMap.get(task.getChapterId());
            taskResponse.setNodeName(node == null ? null : node.getNodeName());
            taskResponse.setChapterId(task.getChapterId());
            taskResponse.setChapterTitle(chapter == null ? null : chapter.getTitle());
            taskResponse.setTaskDate(task.getTaskDate());
            taskResponse.setStepType(task.getStepType());
            taskResponse.setStepOrder(task.getStepOrder());
            taskResponse.setGoalText(task.getGoalText());
            taskResponse.setStatus(task.getStatus());
            taskResponses.add(taskResponse);
        }
        response.setRecentTasks(taskResponses);

        return response;
    }
}

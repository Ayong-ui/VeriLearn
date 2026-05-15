package com.verilearn.workflow.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.verilearn.chapter.dto.ChapterSummaryResponse;
import com.verilearn.chapter.service.ChapterService;
import com.verilearn.goal.entity.LearningGoal;
import com.verilearn.goal.mapper.LearningGoalMapper;
import com.verilearn.infra.feishu.service.FeishuMessagingService;
import com.verilearn.user.entity.LearnerUser;
import com.verilearn.user.mapper.LearnerUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LearnerReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(LearnerReminderScheduler.class);
    private static final String GOAL_ACTIVE = "ACTIVE";
    private static final String CHAPTER_COMPLETED = "COMPLETED";

    private final LearningGoalMapper learningGoalMapper;
    private final LearnerUserMapper learnerUserMapper;
    private final ChapterService chapterService;
    private final FeishuMessagingService feishuMessagingService;

    public LearnerReminderScheduler(
            LearningGoalMapper learningGoalMapper,
            LearnerUserMapper learnerUserMapper,
            ChapterService chapterService,
            FeishuMessagingService feishuMessagingService
    ) {
        this.learningGoalMapper = learningGoalMapper;
        this.learnerUserMapper = learnerUserMapper;
        this.chapterService = chapterService;
        this.feishuMessagingService = feishuMessagingService;
    }

    @Scheduled(cron = "${verilearn.schedule.daily-task-cron}")
    public void sendDailyLearningReminders() {
        for (LearningGoal goal : listLatestActiveGoals()) {
            if (!hasActionableLearning(goal.getId())) {
                continue;
            }
            LearnerUser learnerUser = learnerUserMapper.selectById(goal.getUserId());
            if (learnerUser == null || learnerUser.getFeishuOpenId() == null || learnerUser.getFeishuOpenId().isBlank()) {
                continue;
            }
            feishuMessagingService.sendTextMessage(
                    learnerUser.getFeishuOpenId(),
                    """
                            现在可以开始今天的学习了。
                            当前主题：%s
                            直接发送 /today 查看今天的学习任务与材料入口。
                            """.formatted(goal.getTopic()).trim()
            );
        }
    }

    @Scheduled(cron = "${verilearn.schedule.review-reminder-cron}")
    public void sendPendingReviewReminders() {
        for (LearningGoal goal : listLatestActiveGoals()) {
            List<ChapterSummaryResponse> pendingReviews = chapterService.listPendingReviewsByGoalId(goal.getId());
            if (pendingReviews.isEmpty()) {
                continue;
            }
            LearnerUser learnerUser = learnerUserMapper.selectById(goal.getUserId());
            if (learnerUser == null || learnerUser.getFeishuOpenId() == null || learnerUser.getFeishuOpenId().isBlank()) {
                continue;
            }
            feishuMessagingService.sendTextMessage(
                    learnerUser.getFeishuOpenId(),
                    """
                            你有待复习的章节，建议今晚处理。
                            当前主题：%s
                            待复习章节数：%d
                            直接发送 /dashboard 查看复习入口和当前上下文。
                            """.formatted(goal.getTopic(), pendingReviews.size()).trim()
            );
        }
    }

    private List<LearningGoal> listLatestActiveGoals() {
        List<LearningGoal> activeGoals = learningGoalMapper.selectList(
                new LambdaQueryWrapper<LearningGoal>()
                        .eq(LearningGoal::getStatus, GOAL_ACTIVE)
                        .orderByDesc(LearningGoal::getId)
        );
        Map<Long, LearningGoal> latestByUser = new LinkedHashMap<>();
        for (LearningGoal goal : activeGoals) {
            latestByUser.putIfAbsent(goal.getUserId(), goal);
        }
        return latestByUser.values().stream()
                .sorted(Comparator.comparing(LearningGoal::getId))
                .toList();
    }

    private boolean hasActionableLearning(Long goalId) {
        try {
            return chapterService.listChaptersByGoalId(goalId).stream()
                    .anyMatch(chapter -> !CHAPTER_COMPLETED.equals(chapter.getStatus()));
        } catch (RuntimeException exception) {
            log.warn("failed to inspect actionable learning chapters: goalId={}", goalId, exception);
            return false;
        }
    }
}

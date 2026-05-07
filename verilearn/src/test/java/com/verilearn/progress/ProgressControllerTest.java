package com.verilearn.progress;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.verilearn.chapter.entity.ChapterReviewRecord;
import com.verilearn.chapter.entity.LearningChapter;
import com.verilearn.chapter.mapper.ChapterReviewRecordMapper;
import com.verilearn.chapter.mapper.LearningChapterMapper;
import com.verilearn.goal.entity.LearningGoal;
import com.verilearn.goal.mapper.LearningGoalMapper;
import com.verilearn.knowledge.entity.KnowledgeNode;
import com.verilearn.knowledge.mapper.KnowledgeNodeMapper;
import com.verilearn.task.entity.DailyTask;
import com.verilearn.task.mapper.DailyTaskMapper;
import com.verilearn.user.entity.LearnerUser;
import com.verilearn.user.mapper.LearnerUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProgressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LearnerUserMapper learnerUserMapper;

    @Autowired
    private LearningGoalMapper learningGoalMapper;

    @Autowired
    private KnowledgeNodeMapper knowledgeNodeMapper;

    @Autowired
    private DailyTaskMapper dailyTaskMapper;

    @Autowired
    private LearningChapterMapper learningChapterMapper;

    @Autowired
    private ChapterReviewRecordMapper chapterReviewRecordMapper;

    @Test
    void shouldReturnProgressSummary() throws Exception {
        LearnerUser user = new LearnerUser();
        user.setFeishuOpenId("progress-user");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        learnerUserMapper.insert(user);

        LearningGoal goal = new LearningGoal();
        goal.setUserId(user.getId());
        goal.setTopic("Java backend");
        goal.setTargetLevel("intern");
        goal.setDailyMinutes(180);
        goal.setStatus("ACTIVE");
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());
        learningGoalMapper.insert(goal);

        insertNode(user.getId(), goal.getId(), "Java basics", 1, "IN_PROGRESS");
        insertNode(user.getId(), goal.getId(), "Spring MVC", 2, "NOT_STARTED");
        insertNode(user.getId(), goal.getId(), "HTTP", 3, "PASSED");
        insertChapter(goal.getId(), 1, "Java basics", "IN_PROGRESS", "NOT_READY");
        insertChapter(goal.getId(), 2, "Spring MVC", "COMPLETED", "PENDING");
        insertChapter(goal.getId(), 3, "HTTP", "NOT_STARTED", "NOT_READY");

        DailyTask task = new DailyTask();
        task.setUserId(user.getId());
        task.setNodeId(knowledgeNodeMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getGoalId, goal.getId())
                        .eq(KnowledgeNode::getNodeName, "Java basics")
                        .last("LIMIT 1")
        ).getId());
        task.setTaskDate(LocalDate.parse("2026-04-27"));
        task.setGoalText("Study knowledge node: Java basics");
        task.setStudyMaterial("Review Java basics.");
        task.setStatus("PENDING");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        dailyTaskMapper.insert(task);

        mockMvc.perform(get("/api/progress/{userId}", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(user.getId()))
                .andExpect(jsonPath("$.data.topic").value("Java backend"))
                .andExpect(jsonPath("$.data.totalNodes").value(3))
                .andExpect(jsonPath("$.data.totalChapters").value(3))
                .andExpect(jsonPath("$.data.completedChapters").value(1))
                .andExpect(jsonPath("$.data.inProgressChapters").value(1))
                .andExpect(jsonPath("$.data.pendingReviewChapters").value(1))
                .andExpect(jsonPath("$.data.notStartedNodes").value(1))
                .andExpect(jsonPath("$.data.inProgressNodes").value(1))
                .andExpect(jsonPath("$.data.passedNodes").value(1))
                .andExpect(jsonPath("$.data.recentTasks.length()").value(1))
                .andExpect(jsonPath("$.data.recentTasks[0].nodeName").value("Java basics"));
    }

    private void insertNode(Long userId, Long goalId, String nodeName, int sequenceNo, String status) {
        KnowledgeNode node = new KnowledgeNode();
        node.setUserId(userId);
        node.setGoalId(goalId);
        node.setNodeName(nodeName);
        node.setSequenceNo(sequenceNo);
        node.setStatus(status);
        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());
        knowledgeNodeMapper.insert(node);
    }

    private void insertChapter(Long goalId, int chapterNo, String title, String status, String reviewStatus) {
        KnowledgeNode node = knowledgeNodeMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getGoalId, goalId)
                        .eq(KnowledgeNode::getNodeName, title)
                        .last("LIMIT 1")
        );
        LearningChapter chapter = new LearningChapter();
        chapter.setGoalId(goalId);
        chapter.setNodeId(node == null ? null : node.getId());
        chapter.setChapterNo(chapterNo);
        chapter.setTitle(title);
        chapter.setSummary("Summary for " + title);
        chapter.setStatus(status);
        chapter.setCreatedAt(LocalDateTime.now());
        chapter.setUpdatedAt(LocalDateTime.now());
        learningChapterMapper.insert(chapter);

        ChapterReviewRecord reviewRecord = new ChapterReviewRecord();
        reviewRecord.setChapterId(chapter.getId());
        reviewRecord.setReviewStatus(reviewStatus);
        reviewRecord.setCreatedAt(LocalDateTime.now());
        reviewRecord.setUpdatedAt(LocalDateTime.now());
        chapterReviewRecordMapper.insert(reviewRecord);
    }
}

package com.verilearn.validation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.verilearn.ai.dto.AiValidationItemDraft;
import com.verilearn.ai.service.AiValidationService;
import com.verilearn.goal.entity.LearningGoal;
import com.verilearn.goal.mapper.LearningGoalMapper;
import com.verilearn.knowledge.entity.KnowledgeNode;
import com.verilearn.knowledge.mapper.KnowledgeNodeMapper;
import com.verilearn.task.entity.DailyTask;
import com.verilearn.task.mapper.DailyTaskMapper;
import com.verilearn.user.entity.LearnerUser;
import com.verilearn.user.mapper.LearnerUserMapper;
import com.verilearn.validation.entity.DiversionRecord;
import com.verilearn.validation.entity.ValidationItem;
import com.verilearn.validation.mapper.DiversionRecordMapper;
import com.verilearn.validation.mapper.ValidationItemMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ValidationFlowTest {

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
    private ValidationItemMapper validationItemMapper;

    @Autowired
    private DiversionRecordMapper diversionRecordMapper;

    @MockBean
    private AiValidationService aiValidationService;

    @Test
    void shouldAppendFollowUpValidationWhenInitialEvidenceIsMixed() throws Exception {
        Long taskId = createTaskWithItems();
        List<ValidationItem> roundOneItems = validationItemMapper.selectList(
                new LambdaQueryWrapper<ValidationItem>()
                        .eq(ValidationItem::getTaskId, taskId)
                        .orderByAsc(ValidationItem::getId)
        );
        assertEquals(2, roundOneItems.size());

        mockMvc.perform(post("/api/tasks/{taskId}/submit", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "submissions": [
                                    {
                                      "itemId": %d,
                                      "submittedAnswer": "concept answer",
                                      "correct": true
                                    },
                                    {
                                      "itemId": %d,
                                      "submittedAnswer": "wrong example",
                                      "correct": false
                                    }
                                  ]
                                }
                                """.formatted(roundOneItems.get(0).getId(), roundOneItems.get(1).getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.finalized").value(false))
                .andExpect(jsonPath("$.data.resultCode").value("APPEND_VALIDATION"))
                .andExpect(jsonPath("$.data.nextValidationItems.length()").value(1));

        DailyTask task = dailyTaskMapper.selectById(taskId);
        assertNotNull(task);
        assertEquals("PENDING", task.getStatus());

        KnowledgeNode node = knowledgeNodeMapper.selectById(task.getNodeId());
        assertNotNull(node);
        assertEquals("IN_PROGRESS", node.getStatus());

        List<ValidationItem> allItems = validationItemMapper.selectList(
                new LambdaQueryWrapper<ValidationItem>()
                        .eq(ValidationItem::getTaskId, taskId)
                        .orderByAsc(ValidationItem::getRoundNo)
                        .orderByAsc(ValidationItem::getId)
        );
        assertEquals(3, allItems.size());
        assertEquals(2, allItems.get(2).getRoundNo());
    }

    @Test
    void shouldFinalizeTaskAsAdvanceWhenAllAnswersAreCorrect() throws Exception {
        Long taskId = createTaskWithItems();
        List<ValidationItem> roundOneItems = validationItemMapper.selectList(
                new LambdaQueryWrapper<ValidationItem>()
                        .eq(ValidationItem::getTaskId, taskId)
                        .orderByAsc(ValidationItem::getId)
        );

        mockMvc.perform(post("/api/tasks/{taskId}/submit", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "submissions": [
                                    {
                                      "itemId": %d,
                                      "submittedAnswer": "concept answer",
                                      "correct": true
                                    },
                                    {
                                      "itemId": %d,
                                      "submittedAnswer": "example answer",
                                      "correct": true
                                    }
                                  ]
                                }
                                """.formatted(roundOneItems.get(0).getId(), roundOneItems.get(1).getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finalized").value(true))
                .andExpect(jsonPath("$.data.resultCode").value("ADVANCE"))
                .andExpect(jsonPath("$.data.taskStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.nodeStatus").value("PASSED"));

        DailyTask task = dailyTaskMapper.selectById(taskId);
        assertNotNull(task);
        assertEquals("COMPLETED", task.getStatus());

        KnowledgeNode node = knowledgeNodeMapper.selectById(task.getNodeId());
        assertNotNull(node);
        assertEquals("PASSED", node.getStatus());

        DiversionRecord record = diversionRecordMapper.selectOne(
                new LambdaQueryWrapper<DiversionRecord>()
                        .eq(DiversionRecord::getTaskId, taskId)
                        .last("LIMIT 1")
        );
        assertNotNull(record);
        assertEquals("ADVANCE", record.getResultCode());
    }

    @Test
    void shouldFinalizeTaskAsRetryWhenAnswersRemainBelowThreshold() throws Exception {
        Long taskId = createTaskWithItems();
        List<ValidationItem> roundOneItems = validationItemMapper.selectList(
                new LambdaQueryWrapper<ValidationItem>()
                        .eq(ValidationItem::getTaskId, taskId)
                        .orderByAsc(ValidationItem::getId)
        );

        mockMvc.perform(post("/api/tasks/{taskId}/submit", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "submissions": [
                                    {
                                      "itemId": %d,
                                      "submittedAnswer": "concept answer",
                                      "correct": true
                                    },
                                    {
                                      "itemId": %d,
                                      "submittedAnswer": "wrong example",
                                      "correct": false
                                    }
                                  ]
                                }
                                """.formatted(roundOneItems.get(0).getId(), roundOneItems.get(1).getId())))
                .andExpect(status().isOk());

        ValidationItem followUpItem = validationItemMapper.selectOne(
                new LambdaQueryWrapper<ValidationItem>()
                        .eq(ValidationItem::getTaskId, taskId)
                        .eq(ValidationItem::getRoundNo, 2)
                        .last("LIMIT 1")
        );
        assertNotNull(followUpItem);

        mockMvc.perform(post("/api/tasks/{taskId}/submit", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "submissions": [
                                    {
                                      "itemId": %d,
                                      "submittedAnswer": "still wrong",
                                      "correct": false
                                    }
                                  ]
                                }
                                """.formatted(followUpItem.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finalized").value(true))
                .andExpect(jsonPath("$.data.resultCode").value("RETRY"))
                .andExpect(jsonPath("$.data.nodeStatus").value("NEEDS_RETRY"));

        DailyTask task = dailyTaskMapper.selectById(taskId);
        assertNotNull(task);
        assertEquals("COMPLETED", task.getStatus());

        KnowledgeNode node = knowledgeNodeMapper.selectById(task.getNodeId());
        assertNotNull(node);
        assertEquals("NEEDS_RETRY", node.getStatus());
    }

    @Test
    void shouldRegenerateValidationItemsByAiBeforeSubmission() throws Exception {
        Long taskId = createTaskWithItems();
        when(aiValidationService.generateValidationItems(anyString(), anyString(), anyString(), isNull()))
                .thenReturn(List.of(
                        buildDraft("CONCEPT", "BASIC", "请解释依赖注入是什么。", "应说明控制反转与对象装配。"),
                        buildDraft("APPLICATION", "MEDIUM", "请给出依赖注入在 Spring Boot 中的使用场景。", "应举出 Controller、Service 或配置注入场景。")
                ));

        mockMvc.perform(post("/api/tasks/{taskId}/validation-items/generate", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].itemType").value("CONCEPT"))
                .andExpect(jsonPath("$.data[0].questionText").value("请解释依赖注入是什么。"))
                .andExpect(jsonPath("$.data[1].itemType").value("APPLICATION"))
                .andExpect(jsonPath("$.data[1].difficultyLevel").value("MEDIUM"));

        List<ValidationItem> savedItems = validationItemMapper.selectList(
                new LambdaQueryWrapper<ValidationItem>()
                        .eq(ValidationItem::getTaskId, taskId)
                        .orderByAsc(ValidationItem::getId)
        );
        assertEquals(2, savedItems.size());
        assertEquals("请解释依赖注入是什么。", savedItems.get(0).getQuestionText());
        assertEquals("APPLICATION", savedItems.get(1).getItemType());
    }

    private Long createTaskWithItems() {
        LearnerUser user = new LearnerUser();
        user.setFeishuOpenId("validation-user-" + System.nanoTime());
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

        KnowledgeNode node = new KnowledgeNode();
        node.setUserId(user.getId());
        node.setGoalId(goal.getId());
        node.setNodeName("Spring Boot validation");
        node.setSequenceNo(1);
        node.setStatus("IN_PROGRESS");
        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());
        knowledgeNodeMapper.insert(node);

        DailyTask task = new DailyTask();
        task.setUserId(user.getId());
        task.setNodeId(node.getId());
        task.setTaskDate(java.time.LocalDate.of(2026, 4, 29));
        task.setGoalText("Study knowledge node: Spring Boot validation");
        task.setStudyMaterial("Review validation.");
        task.setStatus("PENDING");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        dailyTaskMapper.insert(task);

        createValidationItem(task.getId(), node.getId(), 1, "CONCEPT");
        createValidationItem(task.getId(), node.getId(), 1, "APPLICATION");
        return task.getId();
    }

    private void createValidationItem(Long taskId, Long nodeId, int roundNo, String itemType) {
        ValidationItem item = new ValidationItem();
        item.setTaskId(taskId);
        item.setNodeId(nodeId);
        item.setRoundNo(roundNo);
        item.setItemType(itemType);
        item.setDifficultyLevel("BASIC");
        item.setQuestionText("Question for " + itemType);
        item.setAnswerKey("Answer for " + itemType);
        item.setQualityStatus("VALIDATED");
        item.setCreatedAt(LocalDateTime.now());
        validationItemMapper.insert(item);
    }

    private AiValidationItemDraft buildDraft(String itemType, String difficulty, String question, String answer) {
        AiValidationItemDraft draft = new AiValidationItemDraft();
        draft.setItemType(itemType);
        draft.setDifficultyLevel(difficulty);
        draft.setQuestionText(question);
        draft.setAnswerKey(answer);
        return draft;
    }
}

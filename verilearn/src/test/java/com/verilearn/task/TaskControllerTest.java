package com.verilearn.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskControllerTest {

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

    @Test
    void shouldGenerateDailyTaskAndReuseSameTaskForSameDay() throws Exception {
        Long goalId = createGoalWithNodes();

        mockMvc.perform(post("/api/tasks/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goalId": %d,
                                  "taskDate": "2026-04-27"
                                }
                                """.formatted(goalId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.goalId").value(goalId))
                .andExpect(jsonPath("$.data.nodeName").value("Spring Boot basics"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.validationItems.length()").value(2));

        DailyTask savedTask = dailyTaskMapper.selectOne(
                new LambdaQueryWrapper<DailyTask>()
                        .eq(DailyTask::getTaskDate, LocalDate.parse("2026-04-27"))
                        .last("LIMIT 1")
        );
        assertNotNull(savedTask);

        KnowledgeNode assignedNode = knowledgeNodeMapper.selectById(savedTask.getNodeId());
        assertNotNull(assignedNode);
        assertEquals("IN_PROGRESS", assignedNode.getStatus());

        mockMvc.perform(post("/api/tasks/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goalId": %d,
                                  "taskDate": "2026-04-27"
                                }
                                """.formatted(goalId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(savedTask.getId()))
                .andExpect(jsonPath("$.data.nodeName").value("Spring Boot basics"));
    }

    private Long createGoalWithNodes() {
        LearnerUser user = new LearnerUser();
        user.setFeishuOpenId("task-generate-user");
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        learnerUserMapper.insert(user);

        LearningGoal goal = new LearningGoal();
        goal.setUserId(user.getId());
        goal.setTopic("Java backend");
        goal.setTargetLevel("intern");
        goal.setDailyMinutes(180);
        goal.setStatus("ACTIVE");
        goal.setCreatedAt(java.time.LocalDateTime.now());
        goal.setUpdatedAt(java.time.LocalDateTime.now());
        learningGoalMapper.insert(goal);

        createKnowledgeNode(user.getId(), goal.getId(), "Spring Boot basics", 1, "NOT_STARTED");
        createKnowledgeNode(user.getId(), goal.getId(), "REST API", 2, "NOT_STARTED");
        return goal.getId();
    }

    private void createKnowledgeNode(Long userId, Long goalId, String nodeName, int sequenceNo, String status) {
        KnowledgeNode node = new KnowledgeNode();
        node.setUserId(userId);
        node.setGoalId(goalId);
        node.setNodeName(nodeName);
        node.setSequenceNo(sequenceNo);
        node.setStatus(status);
        node.setCreatedAt(java.time.LocalDateTime.now());
        node.setUpdatedAt(java.time.LocalDateTime.now());
        knowledgeNodeMapper.insert(node);
    }
}

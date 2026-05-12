package com.verilearn.workflow;

import com.verilearn.chapter.JsonPathHelper;
import com.verilearn.ai.dto.AiDemoEvaluationResult;
import com.verilearn.ai.service.AiEvaluationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.verilearn.goal.entity.LearningGoal;
import com.verilearn.goal.mapper.LearningGoalMapper;
import com.verilearn.knowledge.entity.KnowledgeNode;
import com.verilearn.knowledge.mapper.KnowledgeNodeMapper;
import com.verilearn.user.entity.LearnerUser;
import com.verilearn.user.mapper.LearnerUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LearnerWorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LearnerUserMapper learnerUserMapper;

    @Autowired
    private LearningGoalMapper learningGoalMapper;

    @Autowired
    private KnowledgeNodeMapper knowledgeNodeMapper;

    @MockBean
    private AiEvaluationService aiEvaluationService;

    @Test
    void shouldSetupLearnerAndCreateDefaultKnowledgeNodes() throws Exception {
        String openId = "workflow-setup-user";

        mockMvc.perform(post("/api/learners/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feishuOpenId": "workflow-setup-user",
                                  "topic": "Spring Boot",
                                  "targetLevel": "intern",
                                  "dailyMinutes": 90
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("learner setup completed successfully"))
                .andExpect(jsonPath("$.data.feishuOpenId").value(openId))
                .andExpect(jsonPath("$.data.goalStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.initializedNodeCount").value(4))
                .andExpect(jsonPath("$.data.chapterCount").value(4))
                .andExpect(jsonPath("$.data.knowledgeNodes.length()").value(4))
                .andExpect(jsonPath("$.data.knowledgeNodes[0].status").value("NOT_STARTED"));

        LearnerUser user = learnerUserMapper.selectOne(
                new LambdaQueryWrapper<LearnerUser>()
                        .eq(LearnerUser::getFeishuOpenId, openId)
                        .last("LIMIT 1")
        );
        assertNotNull(user);

        LearningGoal goal = learningGoalMapper.selectOne(
                new LambdaQueryWrapper<LearningGoal>()
                        .eq(LearningGoal::getUserId, user.getId())
                        .last("LIMIT 1")
        );
        assertNotNull(goal);

        Long nodeCount = knowledgeNodeMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getGoalId, goal.getId())
        );
        assertEquals(4L, nodeCount);
    }

    @Test
    void shouldGenerateTaskAndQueryProgressByFeishuOpenId() throws Exception {
        String openId = "workflow-task-user";

        mockMvc.perform(post("/api/learners/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feishuOpenId": "workflow-task-user",
                                  "topic": "Java backend",
                                  "targetLevel": "intern",
                                  "dailyMinutes": 120,
                                  "nodeNames": [
                                    "Java basics",
                                    "Spring Boot basics",
                                    "REST API design"
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        String todayTaskResponse = mockMvc.perform(get("/api/learners/{feishuOpenId}/today-task", openId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.chapterTitle").value("Java basics"))
                .andExpect(jsonPath("$.data.stepType").value("READ_THEORY"))
                .andExpect(jsonPath("$.data.validationItems.length()").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long taskId = JsonPathHelper.readLong(todayTaskResponse, "$.data.taskId");
        Long firstItemId = JsonPathHelper.readLong(todayTaskResponse, "$.data.validationItems[0].itemId");
        Long secondItemId = JsonPathHelper.readLong(todayTaskResponse, "$.data.validationItems[1].itemId");

        mockMvc.perform(post("/api/tasks/{taskId}/submit", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "submissions": [
                                    {
                                      "itemId": %d,
                                      "submittedAnswer": "understood",
                                      "correct": true
                                    },
                                    {
                                      "itemId": %d,
                                      "submittedAnswer": "usable example",
                                      "correct": true
                                    }
                                  ]
                                }
                                """.formatted(firstItemId, secondItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finalized").value(true))
                .andExpect(jsonPath("$.data.resultCode").value("ADVANCE"))
                .andExpect(jsonPath("$.data.nodeStatus").value("IN_PROGRESS"));

        mockMvc.perform(get("/api/learners/{feishuOpenId}/progress", openId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.topic").value("Java backend"))
                .andExpect(jsonPath("$.data.totalNodes").value(3))
                .andExpect(jsonPath("$.data.totalChapters").value(3))
                .andExpect(jsonPath("$.data.inProgressNodes").value(1))
                .andExpect(jsonPath("$.data.inProgressChapters").value(1))
                .andExpect(jsonPath("$.data.recentTasks.length()").value(1));

        mockMvc.perform(get("/api/learners/{feishuOpenId}/chapters", openId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].chapterNo").value(1))
                .andExpect(jsonPath("$.data[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data[0].currentStepType").value("RUN_DEMO"));

        mockMvc.perform(get("/api/learners/{feishuOpenId}/dashboard", openId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.feishuOpenId").value(openId))
                .andExpect(jsonPath("$.data.topic").value("Java backend"))
                .andExpect(jsonPath("$.data.chapterCount").value(3))
                .andExpect(jsonPath("$.data.pendingReviewCount").value(0))
                .andExpect(jsonPath("$.data.todayTask.taskId").value(taskId))
                .andExpect(jsonPath("$.data.todayTask.stepType").value("READ_THEORY"))
                .andExpect(jsonPath("$.data.progress.totalChapters").value(3))
                .andExpect(jsonPath("$.data.chapters.length()").value(3))
                .andExpect(jsonPath("$.data.pendingReviews.length()").value(0));
    }

    @Test
    void shouldQueryCurrentContextAfterDemoEvaluation() throws Exception {
        given(aiEvaluationService.evaluateDemoSubmission(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .willReturn(mockEvaluationResult());

        String openId = "workflow-context-user";

        mockMvc.perform(post("/api/learners/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feishuOpenId": "workflow-context-user",
                                  "topic": "Spring Boot",
                                  "targetLevel": "intern",
                                  "dailyMinutes": 100,
                                  "nodeNames": [
                                    "Spring basics"
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        String taskResponse = mockMvc.perform(get("/api/learners/{feishuOpenId}/today-task", openId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long taskId = JsonPathHelper.readLong(taskResponse, "$.data.taskId");
        Long chapterId = JsonPathHelper.readLong(taskResponse, "$.data.chapterId");
        Long firstItemId = JsonPathHelper.readLong(taskResponse, "$.data.validationItems[0].itemId");
        Long secondItemId = JsonPathHelper.readLong(taskResponse, "$.data.validationItems[1].itemId");

        mockMvc.perform(post("/api/tasks/{taskId}/submit", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "submissions": [
                                    {
                                      "itemId": %d,
                                      "submittedAnswer": "understood",
                                      "correct": true
                                    },
                                    {
                                      "itemId": %d,
                                      "submittedAnswer": "working example",
                                      "correct": true
                                    }
                                  ]
                                }
                                """.formatted(firstItemId, secondItemId)))
                .andExpect(status().isOk());

        String chapterDetail = mockMvc.perform(get("/api/chapters/{chapterId}", chapterId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long demoStepId = JsonPathHelper.readLong(chapterDetail, "$.data.steps[1].id");

        mockMvc.perform(post("/api/chapters/{chapterId}/demo-evaluations", chapterId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stepId": %d,
                                  "submissionSummary": "I finished the ping endpoint and can explain why the controller is scanned.",
                                  "codeSnippet": "@RestController class PingController {}",
                                  "question": "Why does Spring Boot auto scan this package?"
                                }
                                """.formatted(demoStepId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/learners/{feishuOpenId}/current-context", openId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.feishuOpenId").value(openId))
                .andExpect(jsonPath("$.data.topic").value("Spring Boot"))
                .andExpect(jsonPath("$.data.todayTask.taskId").value(taskId))
                .andExpect(jsonPath("$.data.currentChapter.chapterId").value(chapterId))
                .andExpect(jsonPath("$.data.currentChapter.steps[2].stepType").value("SUBMIT_FEEDBACK"))
                .andExpect(jsonPath("$.data.evaluationFilePath").value(org.hamcrest.Matchers.endsWith("evaluation-report.md")))
                .andExpect(jsonPath("$.data.nextStepFilePath").value(org.hamcrest.Matchers.endsWith("next-step.md")));
    }

    private AiDemoEvaluationResult mockEvaluationResult() {
        AiDemoEvaluationResult result = new AiDemoEvaluationResult();
        result.setUnderstandingLevel("HIGH");
        result.setEvaluationMarkdown("""
                # Demo evaluation
                You completed the exercise well.
                """);
        result.setNextStepMarkdown("""
                # Next step
                Move on to the feedback summary step.
                """);
        result.setShouldReview(false);
        result.setProvider("mock");
        return result;
    }
}

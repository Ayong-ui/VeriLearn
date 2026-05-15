package com.verilearn.workflow.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.verilearn.ai.dto.AiDemoEvaluationResult;
import com.verilearn.ai.service.AiEvaluationService;
import com.verilearn.goal.entity.LearningGoal;
import com.verilearn.goal.mapper.LearningGoalMapper;
import com.verilearn.infra.feishu.service.FeishuMessagingService;
import com.verilearn.user.entity.LearnerUser;
import com.verilearn.user.mapper.LearnerUserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LearnerReminderSchedulerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LearnerReminderScheduler learnerReminderScheduler;

    @Autowired
    private LearnerUserMapper learnerUserMapper;

    @Autowired
    private LearningGoalMapper learningGoalMapper;

    @MockBean
    private FeishuMessagingService feishuMessagingService;

    @MockBean
    private AiEvaluationService aiEvaluationService;

    @Test
    void shouldSendLearningReminderForActiveGoal() throws Exception {
        String openId = "scheduler-learning-user";

        mockMvc.perform(post("/api/learners/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feishuOpenId": "scheduler-learning-user",
                                  "topic": "Java backend",
                                  "targetLevel": "intern",
                                  "dailyMinutes": 120,
                                  "nodeNames": [
                                    "Java basics"
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        learnerReminderScheduler.sendDailyLearningReminders();

        verify(feishuMessagingService).sendTextMessage(
                ArgumentMatchers.eq(openId),
                ArgumentMatchers.contains("/today")
        );
    }

    @Test
    void shouldSendReviewReminderWhenPendingReviewExists() throws Exception {
        given(aiEvaluationService.evaluateDemoSubmission(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .willReturn(mockEvaluationResult());

        String openId = "scheduler-review-user";

        mockMvc.perform(post("/api/learners/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feishuOpenId": "scheduler-review-user",
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

        Long taskId = com.verilearn.chapter.JsonPathHelper.readLong(taskResponse, "$.data.taskId");
        Long firstItemId = com.verilearn.chapter.JsonPathHelper.readLong(taskResponse, "$.data.validationItems[0].itemId");
        Long secondItemId = com.verilearn.chapter.JsonPathHelper.readLong(taskResponse, "$.data.validationItems[1].itemId");

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

        mockMvc.perform(post("/api/learners/{feishuOpenId}/demo-feedback/current", openId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "submissionSummary": "I finished the demo and can explain the key idea.",
                                  "codeSnippet": "@RestController class PingController {}",
                                  "question": "Why does Spring Boot discover this controller?"
                                }
                                """))
                .andExpect(status().isOk());

        learnerReminderScheduler.sendPendingReviewReminders();

        verify(feishuMessagingService).sendTextMessage(
                ArgumentMatchers.eq(openId),
                ArgumentMatchers.contains("/dashboard")
        );
    }

    @Test
    void shouldNotSendReviewReminderWhenNoPendingReviewExists() throws Exception {
        String openId = "scheduler-no-review-user";

        mockMvc.perform(post("/api/learners/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feishuOpenId": "scheduler-no-review-user",
                                  "topic": "REST API",
                                  "targetLevel": "intern",
                                  "dailyMinutes": 80,
                                  "nodeNames": [
                                    "REST basics"
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        learnerReminderScheduler.sendPendingReviewReminders();

        LearnerUser learnerUser = learnerUserMapper.selectOne(
                new LambdaQueryWrapper<LearnerUser>()
                        .eq(LearnerUser::getFeishuOpenId, openId)
                        .last("LIMIT 1")
        );
        LearningGoal goal = learningGoalMapper.selectOne(
                new LambdaQueryWrapper<LearningGoal>()
                        .eq(LearningGoal::getUserId, learnerUser.getId())
                        .last("LIMIT 1")
        );

        verify(feishuMessagingService, org.mockito.Mockito.never()).sendTextMessage(
                ArgumentMatchers.eq(openId),
                ArgumentMatchers.contains("/dashboard")
        );
        org.junit.jupiter.api.Assertions.assertNotNull(goal);
    }

    private AiDemoEvaluationResult mockEvaluationResult() {
        AiDemoEvaluationResult result = new AiDemoEvaluationResult();
        result.setUnderstandingLevel("HIGH");
        result.setEvaluationMarkdown("# Demo evaluation\nYou completed the exercise well.");
        result.setNextStepMarkdown("# Next step\nMove on to review.");
        result.setShouldReview(true);
        result.setProvider("mock");
        return result;
    }
}

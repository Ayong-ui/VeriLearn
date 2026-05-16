package com.verilearn.workflow.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.verilearn.ai.dto.AiChapterMaterialResult;
import com.verilearn.ai.dto.AiDemoEvaluationResult;
import com.verilearn.ai.service.AiEvaluationService;
import com.verilearn.ai.service.AiMaterialService;
import com.verilearn.goal.entity.LearningGoal;
import com.verilearn.goal.mapper.LearningGoalMapper;
import com.verilearn.infra.feishu.service.FeishuMessagingService;
import com.verilearn.user.entity.LearnerUser;
import com.verilearn.user.mapper.LearnerUserMapper;
import com.verilearn.workflow.dto.LearningRouteChapter;
import com.verilearn.workflow.dto.LearningRouteContentResponse;
import com.verilearn.workflow.dto.LearningRoutePlan;
import com.verilearn.workflow.service.LearningRouteService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @MockBean
    private AiMaterialService aiMaterialService;

    @MockBean
    private LearningRouteService learningRouteService;

    @Test
    void shouldSendLearningReminderForActiveGoal() throws Exception {
        String openId = "scheduler-learning-user";
        mockLearningRouteService("Java backend");
        given(aiMaterialService.generateChapterMaterials(anyLong(), anyString(), anyString(), org.mockito.ArgumentMatchers.<String>nullable(String.class)))
                .willReturn(mockMaterialResult());

        mockMvc.perform(post("/api/learners/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feishuOpenId": "scheduler-learning-user",
                                  "topic": "Java backend",
                                  "targetLevel": "intern",
                                  "dailyMinutes": 120
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
        String openId = "scheduler-review-user";
        mockLearningRouteService("Spring Boot");
        given(aiMaterialService.generateChapterMaterials(anyLong(), anyString(), anyString(), org.mockito.ArgumentMatchers.<String>nullable(String.class)))
                .willReturn(mockMaterialResult());
        given(aiEvaluationService.evaluateDemoSubmission(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .willReturn(mockEvaluationResult());

        mockMvc.perform(post("/api/learners/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feishuOpenId": "scheduler-review-user",
                                  "topic": "Spring Boot",
                                  "targetLevel": "intern",
                                  "dailyMinutes": 100
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
        mockLearningRouteService("REST API");
        given(aiMaterialService.generateChapterMaterials(anyLong(), anyString(), anyString(), org.mockito.ArgumentMatchers.<String>nullable(String.class)))
                .willReturn(mockMaterialResult());

        mockMvc.perform(post("/api/learners/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feishuOpenId": "scheduler-no-review-user",
                                  "topic": "REST API",
                                  "targetLevel": "intern",
                                  "dailyMinutes": 80
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
                        .orderByDesc(LearningGoal::getId)
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

    private AiChapterMaterialResult mockMaterialResult() {
        AiChapterMaterialResult result = new AiChapterMaterialResult();
        result.setSummary("首章材料已生成");
        result.setTheoryContent("# Java backend\n\n这是测试理论内容。");
        result.setDemoGuideContent("# Demo Task\n\n这是测试 Demo 任务。");
        result.setGeneratedByAi(true);
        result.setProvider("mock");
        return result;
    }

    private void mockLearningRouteService(String topic) {
        List<LearningRouteChapter> chapters = List.of(
                chapter(1, topic + " 核心概念", "理解基础概念"),
                chapter(2, topic + " 入门实践", "完成基础实践"),
                chapter(3, topic + " 常见场景", "理解常见场景"),
                chapter(4, topic + " 综合应用", "完成综合应用")
        );
        given(learningRouteService.generateLearningRoute(anyLong(), anyString(), anyString()))
                .willAnswer(invocation -> {
                    String actualTopic = invocation.getArgument(1, String.class);
                    LearningRoutePlan plan = new LearningRoutePlan();
                    plan.setTopic(actualTopic);
                    plan.setOverview(actualTopic + " 的自学路线");
                    plan.setChapters(chapters);
                    plan.setMarkdownContent(buildRouteMarkdown(actualTopic, chapters));
                    return plan;
                });
        given(learningRouteService.createOrUpdateRouteFile(anyString(), anyString()))
                .willAnswer(invocation -> slugify(invocation.getArgument(0, String.class)) + "/learning-route.md");
        given(learningRouteService.buildRouteRelativePath(anyString()))
                .willAnswer(invocation -> slugify(invocation.getArgument(0, String.class)) + "/learning-route.md");
        given(learningRouteService.resolveAbsolutePath(anyString()))
                .willAnswer(invocation -> "D:/mock-learning-space/" + invocation.getArgument(0, String.class));
        given(learningRouteService.buildRouteContentResponse(anyString(), anyString(), anyString()))
                .willAnswer(invocation -> {
                    String actualTopic = invocation.getArgument(0, String.class);
                    LearningRouteContentResponse response = new LearningRouteContentResponse();
                    response.setTopic(actualTopic);
                    response.setFilePath(slugify(actualTopic) + "/learning-route.md");
                    response.setAbsoluteFilePath("D:/mock-learning-space/" + slugify(actualTopic) + "/learning-route.md");
                    response.setContentUrl(invocation.getArgument(1, String.class));
                    response.setViewUrl(invocation.getArgument(2, String.class));
                    response.setContentText(buildRouteMarkdown(actualTopic, chapters));
                    return response;
                });
        given(learningRouteService.ensureDemoAnswerTemplate(anyString()))
                .willAnswer(invocation -> invocation.getArgument(0, String.class));
        given(learningRouteService.extractDemoAnswerSections(anyString()))
                .willReturn("""
                        ## 我的完成记录
                        - 我完成了本章 Demo。
                        ## 我的回答
                        - 我能解释关键原理。
                        ## 我的自检结果
                        - 已完成
                        """);
    }

    private LearningRouteChapter chapter(int chapterNo, String title, String summary) {
        LearningRouteChapter chapter = new LearningRouteChapter();
        chapter.setChapterNo(chapterNo);
        chapter.setTitle(title);
        chapter.setSummary(summary);
        return chapter;
    }

    private String buildRouteMarkdown(String topic, List<LearningRouteChapter> chapters) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 学习路线：").append(topic).append("\n\n");
        builder.append("## 章节安排\n");
        for (LearningRouteChapter chapter : chapters) {
            builder.append(chapter.getChapterNo())
                    .append(". ")
                    .append(chapter.getTitle())
                    .append(" | ")
                    .append(chapter.getSummary())
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private String slugify(String input) {
        return input.toLowerCase().replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-").replaceAll("^-|-$", "");
    }
}

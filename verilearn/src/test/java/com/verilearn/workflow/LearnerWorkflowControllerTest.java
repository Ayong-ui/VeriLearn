package com.verilearn.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.verilearn.ai.dto.AiChapterMaterialResult;
import com.verilearn.ai.dto.AiDemoEvaluationResult;
import com.verilearn.ai.service.AiEvaluationService;
import com.verilearn.ai.service.AiMaterialService;
import com.verilearn.chapter.JsonPathHelper;
import com.verilearn.chapter.entity.ChapterMaterial;
import com.verilearn.chapter.entity.LearningChapter;
import com.verilearn.chapter.mapper.ChapterMaterialMapper;
import com.verilearn.chapter.mapper.LearningChapterMapper;
import com.verilearn.goal.entity.LearningGoal;
import com.verilearn.goal.mapper.LearningGoalMapper;
import com.verilearn.knowledge.entity.KnowledgeNode;
import com.verilearn.knowledge.mapper.KnowledgeNodeMapper;
import com.verilearn.user.entity.LearnerUser;
import com.verilearn.user.mapper.LearnerUserMapper;
import com.verilearn.workflow.dto.LearningRouteChapter;
import com.verilearn.workflow.dto.LearningRouteContentResponse;
import com.verilearn.workflow.dto.LearningRoutePlan;
import com.verilearn.workflow.service.LearningRouteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @Autowired
    private LearningChapterMapper learningChapterMapper;

    @Autowired
    private ChapterMaterialMapper chapterMaterialMapper;

    @MockBean
    private AiEvaluationService aiEvaluationService;

    @MockBean
    private AiMaterialService aiMaterialService;

    @MockBean
    private LearningRouteService learningRouteService;

    @Test
    void shouldSetupLearnerAndCreateDefaultKnowledgeNodes() throws Exception {
        String openId = "workflow-setup-user";
        mockLearningRouteService("Spring Boot", List.of(
                chapter(1, "Spring Boot 核心概念", "理解 Spring Boot 的定位与启动方式"),
                chapter(2, "Spring Boot 配置管理", "掌握配置文件与环境隔离"),
                chapter(3, "Spring Boot Web 开发", "理解 Controller 与请求处理"),
                chapter(4, "Spring Boot 项目实践", "完成最小可运行示例")
        ));
        given(aiMaterialService.generateChapterMaterials(anyLong(), anyString(), anyString(), org.mockito.ArgumentMatchers.<String>nullable(String.class)))
                .willReturn(mockMaterialResult());

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
                        .orderByDesc(LearningGoal::getId)
                        .last("LIMIT 1")
        );
        assertNotNull(goal);

        Long nodeCount = knowledgeNodeMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getGoalId, goal.getId())
        );
        assertEquals(4L, nodeCount);

        List<LearningChapter> chapters = learningChapterMapper.selectList(
                new LambdaQueryWrapper<LearningChapter>()
                        .eq(LearningChapter::getGoalId, goal.getId())
                        .orderByAsc(LearningChapter::getChapterNo)
        );
        assertEquals(4, chapters.size());
        assertEquals("Spring Boot 核心概念", chapters.get(0).getTitle());

        Long firstChapterMaterialCount = chapterMaterialMapper.selectCount(
                new LambdaQueryWrapper<ChapterMaterial>()
                        .eq(ChapterMaterial::getChapterId, chapters.get(0).getId())
        );
        Long secondChapterMaterialCount = chapterMaterialMapper.selectCount(
                new LambdaQueryWrapper<ChapterMaterial>()
                        .eq(ChapterMaterial::getChapterId, chapters.get(1).getId())
        );
        assertEquals(2L, firstChapterMaterialCount);
        assertEquals(0L, secondChapterMaterialCount);
    }

    @Test
    void shouldGenerateTaskAndQueryProgressByFeishuOpenId() throws Exception {
        String openId = "workflow-task-user";
        mockLearningRouteService("Java backend", List.of(
                chapter(1, "Java 后端 核心概念", "理解后端分层与请求处理"),
                chapter(2, "Java 后端 数据访问", "掌握数据库与持久层"),
                chapter(3, "Java 后端 接口设计", "理解 RESTful API 设计"),
                chapter(4, "Java 后端 项目实践", "完成最小后端项目")
        ));
        given(aiMaterialService.generateChapterMaterials(anyLong(), anyString(), anyString(), org.mockito.ArgumentMatchers.<String>nullable(String.class)))
                .willReturn(mockMaterialResult());
        given(aiEvaluationService.evaluateDemoSubmission(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .willReturn(mockEvaluationResult());

        mockMvc.perform(post("/api/learners/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feishuOpenId": "workflow-task-user",
                                  "topic": "Java backend",
                                  "targetLevel": "intern",
                                  "dailyMinutes": 120
                                }
                                """))
                .andExpect(status().isOk());

        String todayTaskResponse = mockMvc.perform(post("/api/learners/{feishuOpenId}/today-task", openId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.chapterTitle").value("Java 后端 核心概念"))
                .andExpect(jsonPath("$.data.stepType").value("RUN_DEMO"))
                .andExpect(jsonPath("$.data.theoryContentUrl").value(org.hamcrest.Matchers.endsWith("/content")))
                .andExpect(jsonPath("$.data.theoryViewUrl").value(org.hamcrest.Matchers.endsWith("/view")))
                .andExpect(jsonPath("$.data.demoContentUrl").value(org.hamcrest.Matchers.endsWith("/content")))
                .andExpect(jsonPath("$.data.demoViewUrl").value(org.hamcrest.Matchers.endsWith("/view")))
                .andExpect(jsonPath("$.data.routeFilePath").value(org.hamcrest.Matchers.endsWith("learning-route.md")))
                .andExpect(jsonPath("$.data.currentChapterNo").value(1))
                .andExpect(jsonPath("$.data.totalChapterCount").value(4))
                .andExpect(jsonPath("$.data.validationItems.length()").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long taskId = JsonPathHelper.readLong(todayTaskResponse, "$.data.taskId");
        Long chapterId = JsonPathHelper.readLong(todayTaskResponse, "$.data.chapterId");

        mockMvc.perform(post("/api/learners/{feishuOpenId}/demo-feedback/current", openId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "submissionSummary": "I completed the demo and understood the key concept.",
                                  "codeSnippet": "@RestController class DemoController {}",
                                  "question": "How does Spring Boot discover controllers?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.understandingLevel").value("HIGH"));

        mockMvc.perform(get("/api/learners/{feishuOpenId}/progress", openId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.topic").value("Java backend"))
                .andExpect(jsonPath("$.data.totalNodes").value(4))
                .andExpect(jsonPath("$.data.totalChapters").value(4))
                .andExpect(jsonPath("$.data.passedNodes").value(1))
                .andExpect(jsonPath("$.data.completedChapters").value(1))
                .andExpect(jsonPath("$.data.recentTasks.length()").value(1));

        mockMvc.perform(get("/api/learners/{feishuOpenId}/chapters", openId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].chapterNo").value(1))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.data[0].currentStepType").doesNotExist());

        mockMvc.perform(get("/api/learners/{feishuOpenId}/dashboard", openId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.feishuOpenId").value(openId))
                .andExpect(jsonPath("$.data.topic").value("Java backend"))
                .andExpect(jsonPath("$.data.chapterCount").value(4))
                .andExpect(jsonPath("$.data.pendingReviewCount").value(1))
                .andExpect(jsonPath("$.data.todayTask.taskId").value(taskId))
                .andExpect(jsonPath("$.data.todayTask.stepType").value("RUN_DEMO"))
                .andExpect(jsonPath("$.data.routeFilePath").value(org.hamcrest.Matchers.endsWith("learning-route.md")))
                .andExpect(jsonPath("$.data.routeViewUrl").value(org.hamcrest.Matchers.endsWith("/view")))
                .andExpect(jsonPath("$.data.progress.totalChapters").value(4))
                .andExpect(jsonPath("$.data.currentChapter.chapterId").exists())
                .andExpect(jsonPath("$.data.currentMaterials.length()").value(4))
                .andExpect(jsonPath("$.data.currentMaterials[0].materialType").value("THEORY_DOC"))
                .andExpect(jsonPath("$.data.currentMaterials[1].materialType").value("DEMO_GUIDE"))
                .andExpect(jsonPath("$.data.chapters.length()").value(4))
                .andExpect(jsonPath("$.data.pendingReviews.length()").value(1));
    }

    @Test
    void shouldQueryCurrentContextAfterDemoEvaluation() throws Exception {
        mockLearningRouteService("Spring Boot", List.of(
                chapter(1, "Spring Boot 核心概念", "理解 Spring Boot 核心概念"),
                chapter(2, "Spring Boot Web 开发", "理解 Web 开发"),
                chapter(3, "Spring Boot 数据访问", "理解数据访问"),
                chapter(4, "Spring Boot 项目实践", "完成项目实践")
        ));
        given(aiMaterialService.generateChapterMaterials(anyLong(), anyString(), anyString(), org.mockito.ArgumentMatchers.<String>nullable(String.class)))
                .willReturn(mockMaterialResult());
        given(aiEvaluationService.evaluateDemoSubmission(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .willReturn(mockEvaluationResult());

        String openId = "workflow-context-user";

        mockMvc.perform(post("/api/learners/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feishuOpenId": "workflow-context-user",
                                  "topic": "Spring Boot",
                                  "targetLevel": "intern",
                                  "dailyMinutes": 100
                                }
                                """))
                .andExpect(status().isOk());

        String taskResponse = mockMvc.perform(post("/api/learners/{feishuOpenId}/today-task", openId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long taskId = JsonPathHelper.readLong(taskResponse, "$.data.taskId");
        Long chapterId = JsonPathHelper.readLong(taskResponse, "$.data.chapterId");

        mockMvc.perform(post("/api/learners/{feishuOpenId}/demo-feedback/current", openId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "submissionSummary": "I finished the ping endpoint and can explain why the controller is scanned.",
                                  "codeSnippet": "@RestController class PingController {}",
                                  "question": "Why does Spring Boot auto scan this package?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.understandingLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.evaluationContentUrl").value(org.hamcrest.Matchers.endsWith("/content")))
                .andExpect(jsonPath("$.data.evaluationViewUrl").value(org.hamcrest.Matchers.endsWith("/view")))
                .andExpect(jsonPath("$.data.nextStepContentUrl").value(org.hamcrest.Matchers.endsWith("/content")));

        mockMvc.perform(get("/api/learners/{feishuOpenId}/current-context", openId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.feishuOpenId").value(openId))
                .andExpect(jsonPath("$.data.topic").value("Spring Boot"))
                .andExpect(jsonPath("$.data.todayTask.taskId").value(taskId))
                .andExpect(jsonPath("$.data.currentChapter.chapterId").value(chapterId))
                .andExpect(jsonPath("$.data.currentChapter.steps[0].stepType").value("RUN_DEMO"))
                .andExpect(jsonPath("$.data.currentMaterials.length()").value(4))
                .andExpect(jsonPath("$.data.currentMaterials[0].materialType").value("THEORY_DOC"))
                .andExpect(jsonPath("$.data.currentMaterials[0].contentUrl").value(org.hamcrest.Matchers.endsWith("/content")))
                .andExpect(jsonPath("$.data.currentMaterials[0].viewUrl").value(org.hamcrest.Matchers.endsWith("/view")))
                .andExpect(jsonPath("$.data.currentMaterials[1].materialType").value("DEMO_GUIDE"))
                .andExpect(jsonPath("$.data.currentMaterials[2].materialType").value("EVALUATION_REPORT"))
                .andExpect(jsonPath("$.data.currentMaterials[3].materialType").value("NEXT_STEP_NOTE"))
                .andExpect(jsonPath("$.data.routeFilePath").value(org.hamcrest.Matchers.endsWith("learning-route.md")))
                .andExpect(jsonPath("$.data.routeViewUrl").value(org.hamcrest.Matchers.endsWith("/view")))
                .andExpect(jsonPath("$.data.evaluationFilePath").value(org.hamcrest.Matchers.endsWith("evaluation-report.md")))
                .andExpect(jsonPath("$.data.evaluationContentUrl").value(org.hamcrest.Matchers.endsWith("/content")))
                .andExpect(jsonPath("$.data.evaluationViewUrl").value(org.hamcrest.Matchers.endsWith("/view")))
                .andExpect(jsonPath("$.data.nextStepFilePath").value(org.hamcrest.Matchers.endsWith("next-step.md")))
                .andExpect(jsonPath("$.data.nextStepContentUrl").value(org.hamcrest.Matchers.endsWith("/content")))
                .andExpect(jsonPath("$.data.nextStepViewUrl").value(org.hamcrest.Matchers.endsWith("/view")));
    }

    @Test
    void shouldPregenerateNextChapterAfterCurrentDemoEvaluation() throws Exception {
        mockLearningRouteService("Spring Boot", List.of(
                chapter(1, "Spring Boot 核心概念", "理解 Spring Boot 核心概念"),
                chapter(2, "Spring MVC 基础", "理解 Spring MVC 基础")
        ));
        given(aiEvaluationService.evaluateDemoSubmission(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .willReturn(mockEvaluationResult());
        given(aiMaterialService.generateChapterMaterials(anyLong(), anyString(), anyString(), org.mockito.ArgumentMatchers.<String>nullable(String.class)))
                .willReturn(mockMaterialResult());

        String openId = "workflow-pregenerate-user";

        mockMvc.perform(post("/api/learners/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feishuOpenId": "workflow-pregenerate-user",
                                  "topic": "Spring Boot",
                                  "targetLevel": "intern",
                                  "dailyMinutes": 100
                                }
                                """))
                .andExpect(status().isOk());

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
        List<LearningChapter> chapters = learningChapterMapper.selectList(
                new LambdaQueryWrapper<LearningChapter>()
                        .eq(LearningChapter::getGoalId, goal.getId())
                        .orderByAsc(LearningChapter::getChapterNo)
        );

        assertEquals(2L, chapterMaterialMapper.selectCount(
                new LambdaQueryWrapper<ChapterMaterial>().eq(ChapterMaterial::getChapterId, chapters.get(0).getId())
        ));
        assertEquals(0L, chapterMaterialMapper.selectCount(
                new LambdaQueryWrapper<ChapterMaterial>().eq(ChapterMaterial::getChapterId, chapters.get(1).getId())
        ));

        String taskResponse = mockMvc.perform(post("/api/learners/{feishuOpenId}/today-task", openId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long taskId = JsonPathHelper.readLong(taskResponse, "$.data.taskId");

        mockMvc.perform(post("/api/learners/{feishuOpenId}/demo-feedback/current", openId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "submissionSummary": "I finished the first demo and can explain the controller flow.",
                                  "codeSnippet": "@RestController class PingController {}",
                                  "question": "Why is this controller mapped automatically?"
                                }
                                """))
                .andExpect(status().isOk());

        assertEquals(2L, chapterMaterialMapper.selectCount(
                new LambdaQueryWrapper<ChapterMaterial>().eq(ChapterMaterial::getChapterId, chapters.get(1).getId())
        ));
    }

    @Test
    void shouldRejectAmbiguousTopicBeforeCreatingLearnerData() throws Exception {
        String openId = "workflow-ambiguous-user";

        mockMvc.perform(post("/api/learners/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feishuOpenId": "workflow-ambiguous-user",
                                  "topic": "数学"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("子方向")));

        LearnerUser user = learnerUserMapper.selectOne(
                new LambdaQueryWrapper<LearnerUser>()
                        .eq(LearnerUser::getFeishuOpenId, openId)
                        .last("LIMIT 1")
        );
        org.junit.jupiter.api.Assertions.assertNull(user);
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

    private AiChapterMaterialResult mockMaterialResult() {
        AiChapterMaterialResult result = new AiChapterMaterialResult();
        result.setSummary("下一章已准备完成");
        result.setTheoryContent("# Spring MVC\n\n这是预生成的理论内容。");
        result.setDemoGuideContent("# Demo Guide\n\n这是预生成的 Demo 内容。");
        result.setGeneratedByAi(true);
        result.setProvider("mock");
        return result;
    }

    private void mockLearningRouteService(String topic, List<LearningRouteChapter> chapters) {
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
                    String contentUrl = invocation.getArgument(1, String.class);
                    String viewUrl = invocation.getArgument(2, String.class);
                    LearningRouteContentResponse response = new LearningRouteContentResponse();
                    response.setTopic(actualTopic);
                    response.setFilePath(slugify(actualTopic) + "/learning-route.md");
                    response.setAbsoluteFilePath("D:/mock-learning-space/" + slugify(actualTopic) + "/learning-route.md");
                    response.setContentUrl(contentUrl);
                    response.setViewUrl(viewUrl);
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

package com.verilearn.infra.feishu;

import com.verilearn.ai.dto.AiProviderConfigResponse;
import com.verilearn.ai.service.AiProviderConfigService;
import com.verilearn.chapter.dto.ChapterDemoEvaluationResponse;
import com.verilearn.chapter.dto.ChapterSummaryResponse;
import com.verilearn.infra.feishu.service.FeishuMessagingService;
import com.verilearn.progress.dto.ProgressResponse;
import com.verilearn.task.dto.TaskResponse;
import com.verilearn.workflow.dto.LearnerDashboardResponse;
import com.verilearn.workflow.dto.LearnerMaterialReference;
import com.verilearn.workflow.dto.LearnerSetupResponse;
import com.verilearn.workflow.service.LearnerWorkflowService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FeishuEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LearnerWorkflowService learnerWorkflowService;

    @MockBean
    private FeishuMessagingService feishuMessagingService;

    @MockBean
    private AiProviderConfigService aiProviderConfigService;

    @Test
    void shouldReturnChallengeForUrlVerification() throws Exception {
        mockMvc.perform(post("/api/feishu/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "url_verification",
                                  "challenge": "challenge-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challenge").value("challenge-token"));
    }

    @Test
    void shouldRejectStartCommandWithoutTopic() throws Exception {
        mockMvc.perform(post("/api/feishu/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageEvent("ou_test_start_empty", "/start")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(false))
                .andExpect(jsonPath("$.data.command").value("/start"))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("请在 /start 后面输入学习主题")));
    }

    @Test
    void shouldHandleStartCommand() throws Exception {
        LearnerSetupResponse response = new LearnerSetupResponse();
        response.setTopic("Java 后端");
        response.setDailyMinutes(120);
        response.setInitializedNodeCount(4);
        response.setChapterCount(4);
        when(learnerWorkflowService.setupLearner(any())).thenReturn(response);

        mockMvc.perform(post("/api/feishu/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageEvent("ou_test_start", "/start Java 后端")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.command").value("/start"))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("已为你初始化学习主题：Java 后端")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("已生成知识点：4 个")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("/today")));

        verify(feishuMessagingService).sendTextMessage(eq("ou_test_start"), contains("Java 后端"));
    }

    @Test
    void shouldBlockStartWhenActiveRouteExists() throws Exception {
        LearnerDashboardResponse dashboard = new LearnerDashboardResponse();
        dashboard.setTopic("MySQL");
        dashboard.setGoalStatus("ACTIVE");
        TaskResponse todayTask = new TaskResponse();
        todayTask.setStepType("RUN_DEMO");
        dashboard.setTodayTask(todayTask);
        var currentChapter = new com.verilearn.chapter.dto.ChapterDetailResponse();
        currentChapter.setTitle("索引基础");
        dashboard.setCurrentChapter(currentChapter);
        when(learnerWorkflowService.getDashboard(eq("ou_test_start_blocked"))).thenReturn(dashboard);

        mockMvc.perform(post("/api/feishu/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageEvent("ou_test_start_blocked", "/start Redis")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(false))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("你当前还有未完成的学习任务")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("当前主题：MySQL")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("请先发送 /today")));
    }

    @Test
    void shouldHandleTodayCommand() throws Exception {
        TaskResponse taskResponse = new TaskResponse();
        taskResponse.setGoalText("学习 Spring Boot fundamentals");
        taskResponse.setChapterTitle("Spring Boot fundamentals");
        taskResponse.setCurrentChapterNo(1);
        taskResponse.setTotalChapterCount(4);
        taskResponse.setStepType("READ_THEORY");
        taskResponse.setStatus("PENDING");
        taskResponse.setRouteViewUrl("http://localhost:8080/learning-routes/ou_test_today/view");
        taskResponse.setTheoryViewUrl("/materials/101/view");
        taskResponse.setDemoViewUrl("/materials/102/view");
        taskResponse.setValidationItems(List.of());

        LearnerDashboardResponse dashboard = new LearnerDashboardResponse();
        dashboard.setTopic("Spring Boot fundamentals");
        dashboard.setCurrentChapterNo(1);
        dashboard.setTotalChapterCount(4);
        dashboard.setChapters(List.of(
                chapter(1, "Spring Boot fundamentals", "IN_PROGRESS"),
                chapter(2, "Spring MVC basics", "NOT_STARTED")
        ));
        when(learnerWorkflowService.generateTodayTask(eq("ou_test_today"))).thenReturn(taskResponse);
        when(learnerWorkflowService.getDashboard(eq("ou_test_today"))).thenReturn(dashboard);

        mockMvc.perform(post("/api/feishu/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageEvent("ou_test_today", "/today")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.command").value("/today"))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("今日任务")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("路线概览")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("完成 Demo")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("/submit-demo 我完成了")));
    }

    @Test
    void shouldHandleProgressCommand() throws Exception {
        ProgressResponse progressResponse = new ProgressResponse();
        progressResponse.setTopic("Java 后端");
        progressResponse.setGoalStatus("IN_PROGRESS");
        progressResponse.setTargetLevel("intern");
        progressResponse.setTotalNodes(4);
        progressResponse.setInProgressNodes(1);
        progressResponse.setPassedNodes(2);
        progressResponse.setNeedsRetryNodes(1);
        progressResponse.setTotalChapters(4);
        progressResponse.setInProgressChapters(1);
        progressResponse.setCompletedChapters(2);
        progressResponse.setPendingReviewChapters(1);
        when(learnerWorkflowService.getProgress(eq("ou_test_progress"))).thenReturn(progressResponse);

        mockMvc.perform(post("/api/feishu/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageEvent("ou_test_progress", "/progress")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.command").value("/progress"))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("当前主题：Java 后端")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("知识点进度")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("章节进度")));
    }

    @Test
    void shouldHandleDashboardCommand() throws Exception {
        LearnerDashboardResponse dashboard = new LearnerDashboardResponse();
        dashboard.setTopic("Java 后端");
        dashboard.setGoalStatus("IN_PROGRESS");
        dashboard.setCurrentChapterNo(1);
        dashboard.setTotalChapterCount(4);
        dashboard.setPendingReviewCount(1);
        dashboard.setRouteViewUrl("/learning-routes/ou_test_dashboard/view");
        dashboard.setCurrentMaterials(List.of(
                new LearnerMaterialReference(101L, "THEORY_DOC", "理论文档", "path/theory.md", "/api/materials/101/content", "/materials/101/view"),
                new LearnerMaterialReference(102L, "DEMO_GUIDE", "Demo 任务", "path/demo-task.md", "/api/materials/102/content", "/materials/102/view")
        ));
        when(learnerWorkflowService.getDashboard(eq("ou_test_dashboard"))).thenReturn(dashboard);

        mockMvc.perform(post("/api/feishu/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageEvent("ou_test_dashboard", "/dashboard")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.command").value("/dashboard"))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("当前主题：Java 后端")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("路线入口")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("当前材料")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("http://localhost:8080/materials/101/view")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("http://localhost:8080/materials/102/view")));
    }

    @Test
    void shouldHandleSubmitDemoCommandWithCurrentContext() throws Exception {
        ChapterDemoEvaluationResponse response = new ChapterDemoEvaluationResponse();
        response.setUnderstandingLevel("HIGH");
        response.setChapterStatus("IN_PROGRESS");
        response.setEvaluationViewUrl("/materials/201/view");
        response.setNextStepViewUrl("/materials/202/view");
        when(learnerWorkflowService.evaluateCurrentDemoSubmission(eq("ou_test_submit_demo"), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/feishu/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageEvent("ou_test_submit_demo", "/submit-demo 我完成了 ping 接口，并理解了包扫描")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.command").value("/submit-demo"))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("Demo 评估已完成")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("掌握程度：HIGH")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("http://localhost:8080/materials/201/view")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("http://localhost:8080/materials/202/view")));
    }

    @Test
    void shouldHandleAiCurrentCommand() throws Exception {
        AiProviderConfigResponse response = new AiProviderConfigResponse();
        response.setProviderType("DEEPSEEK");
        response.setModelName("deepseek-chat");
        response.setBaseUrl("https://api.deepseek.com");
        response.setStatus("ACTIVE");
        response.setSourceType("SYSTEM_DEFAULT");
        response.setApiKeyMasked("sk-xxxx***yyyy");
        when(aiProviderConfigService.getCurrentConfig(eq("ou_test_ai_current"))).thenReturn(response);

        mockMvc.perform(post("/api/feishu/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageEvent("ou_test_ai_current", "/ai current")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.command").value("/ai"))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("当前 AI 配置")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("DEEPSEEK")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("deepseek-chat")));
    }

    @Test
    void shouldHandleAiSwitchCommand() throws Exception {
        AiProviderConfigResponse response = new AiProviderConfigResponse();
        response.setConfigId(2L);
        response.setProviderType("OPENAI");
        response.setModelName("gpt-4.1-mini");
        response.setBaseUrl("https://api.openai.com/v1");
        response.setApiKeyMasked("sk-open***mini");
        when(aiProviderConfigService.activateConfig(eq("ou_test_ai_switch"), eq(2L))).thenReturn(response);

        mockMvc.perform(post("/api/feishu/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageEvent("ou_test_ai_switch", "/ai switch 2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("已切换当前 AI 模型")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("OPENAI")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("gpt-4.1-mini")));
    }

    @Test
    void shouldHandleAiSwitchCommandWithInvalidConfigId() throws Exception {
        mockMvc.perform(post("/api/feishu/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageEvent("ou_test_ai_switch_invalid", "/ai switch abc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(false))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("配置ID 必须是数字")));
    }

    @Test
    void shouldHandleAiConfigCommand() throws Exception {
        mockMvc.perform(post("/api/feishu/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageEvent("ou_test_ai_config", "/ai config")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("AI 配置说明")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("http://localhost:8080/ai/provider-config-page?openId=ou_test_ai_config")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("API Key")));
    }

    private String messageEvent(String openId, String text) {
        return """
                {
                  "header": {
                    "event_type": "im.message.receive_v1"
                  },
                  "event": {
                    "sender": {
                      "sender_id": {
                        "open_id": "%s"
                      }
                    },
                    "message": {
                      "message_type": "text",
                      "content": "{\\"text\\":\\"%s\\"}"
                    }
                  }
                }
                """.formatted(openId, escapeForMessageContent(text));
    }

    private String escapeForMessageContent(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private ChapterSummaryResponse chapter(int chapterNo, String title, String status) {
        ChapterSummaryResponse response = new ChapterSummaryResponse();
        response.setChapterNo(chapterNo);
        response.setTitle(title);
        response.setStatus(status);
        return response;
    }
}

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
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("Java 后端")));

        verify(feishuMessagingService).sendTextMessage(eq("ou_test_start"), contains("Java 后端"));
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
        taskResponse.setTheoryFilePath("spring-boot/01-spring-boot-fundamentals/user/theory/theory.md");
        taskResponse.setTheoryContentUrl("/api/materials/101/content");
        taskResponse.setTheoryViewUrl("/materials/101/view");
        taskResponse.setDemoFilePath("spring-boot/01-spring-boot-fundamentals/user/demo/demo-task.md");
        taskResponse.setDemoContentUrl("/api/materials/102/content");
        taskResponse.setDemoViewUrl("/materials/102/view");
        taskResponse.setValidationItems(List.of());

        LearnerDashboardResponse dashboard = new LearnerDashboardResponse();
        dashboard.setTopic("Spring Boot fundamentals");
        dashboard.setChapterCount(4);
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
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("学习 Spring Boot fundamentals")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("先看理论")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("http://localhost:8080/materials/101/view")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("再做 Demo")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("http://localhost:8080/materials/102/view")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("/submit-demo")));
    }

    @Test
    void shouldHandleProgressCommand() throws Exception {
        ProgressResponse progressResponse = new ProgressResponse();
        progressResponse.setTopic("Java 后端");
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
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("Java 后端")));
    }

    @Test
    void shouldHandleDashboardCommand() throws Exception {
        LearnerDashboardResponse dashboard = new LearnerDashboardResponse();
        dashboard.setTopic("Java 后端");
        dashboard.setChapterCount(4);
        dashboard.setPendingReviewCount(1);
        TaskResponse taskResponse = new TaskResponse();
        taskResponse.setGoalText("学习 Spring Boot fundamentals");
        dashboard.setTodayTask(taskResponse);
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
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("Java 后端")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("http://localhost:8080/materials/101/view")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("http://localhost:8080/materials/102/view")));
    }

    @Test
    void shouldHandleSubmitDemoCommandWithCurrentContext() throws Exception {
        ChapterDemoEvaluationResponse response = new ChapterDemoEvaluationResponse();
        response.setUnderstandingLevel("HIGH");
        response.setChapterStatus("IN_PROGRESS");
        response.setEvaluationContentUrl("/api/materials/201/content");
        response.setEvaluationViewUrl("/materials/201/view");
        response.setNextStepContentUrl("/api/materials/202/content");
        response.setNextStepViewUrl("/materials/202/view");
        when(learnerWorkflowService.evaluateCurrentDemoSubmission(eq("ou_test_submit_demo"), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/feishu/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageEvent("ou_test_submit_demo", "/submit-demo 我完成了 ping 接口，并理解了包扫描")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.command").value("/submit-demo"))
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
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("OPENAI")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("gpt-4.1-mini")));
    }

    @Test
    void shouldHandleAiConfigCommand() throws Exception {
        mockMvc.perform(post("/api/feishu/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageEvent("ou_test_ai_config", "/ai config")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
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

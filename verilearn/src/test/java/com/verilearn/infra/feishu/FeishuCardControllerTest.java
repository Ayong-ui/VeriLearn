package com.verilearn.infra.feishu;

import com.verilearn.ai.dto.AiProviderConfigResponse;
import com.verilearn.ai.service.AiProviderConfigService;
import com.verilearn.chapter.dto.ChapterDetailResponse;
import com.verilearn.progress.dto.ProgressResponse;
import com.verilearn.task.dto.TaskResponse;
import com.verilearn.validation.dto.ValidationItemResponse;
import com.verilearn.workflow.dto.LearnerCurrentContextResponse;
import com.verilearn.workflow.dto.LearnerDashboardResponse;
import com.verilearn.workflow.dto.LearnerMaterialReference;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FeishuCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LearnerWorkflowService learnerWorkflowService;

    @MockBean
    private AiProviderConfigService aiProviderConfigService;

    @Test
    void shouldPreviewTodayTaskCard() throws Exception {
        TaskResponse task = new TaskResponse();
        task.setTaskId(1001L);
        task.setGoalText("学习 Spring Boot fundamentals");
        task.setChapterTitle("Spring Boot fundamentals");
        task.setStepType("RUN_DEMO");
        task.setStatus("PENDING");
        task.setTheoryFilePath("spring-boot/01-spring-boot-fundamentals/user/theory/theory.md");
        task.setTheoryContentUrl("/api/materials/101/content");
        task.setTheoryViewUrl("/materials/101/view");
        task.setDemoFilePath("spring-boot/01-spring-boot-fundamentals/user/demo/demo-task.md");
        task.setDemoContentUrl("/api/materials/102/content");
        task.setDemoViewUrl("/materials/102/view");
        ValidationItemResponse item = new ValidationItemResponse();
        item.setItemType("CONCEPT");
        item.setQuestionText("请解释依赖注入的核心作用。");
        task.setValidationItems(List.of(item));
        when(learnerWorkflowService.generateTodayTask(eq("ou_card_today"))).thenReturn(task);

        mockMvc.perform(get("/api/feishu/cards/learners/ou_card_today/today-task"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.cardType").value("TODAY_TASK"))
                .andExpect(jsonPath("$.data.card.header.title.content").value("VeriLearn 今日任务"))
                .andExpect(jsonPath("$.data.card.body.elements[0].content").value(Matchers.containsString("学习 Spring Boot fundamentals")))
                .andExpect(jsonPath("$.data.card.body.elements[0].content").value(Matchers.containsString("/materials/101/view")))
                .andExpect(jsonPath("$.data.card.body.elements[0].content").value(Matchers.containsString("/materials/102/view")))
                .andExpect(jsonPath("$.data.card.body.elements[2].content").value(Matchers.containsString("/submit-demo 我完成了今天的 Demo")))
                .andExpect(jsonPath("$.data.card.body.elements[3].actions[2].value.action").value("SHOW_AI_PROVIDER"));
    }

    @Test
    void shouldPreviewDashboardCardWithMaterialLinks() throws Exception {
        LearnerDashboardResponse dashboard = new LearnerDashboardResponse();
        dashboard.setTopic("Java 后端");
        dashboard.setChapterCount(4);
        dashboard.setPendingReviewCount(1);
        TaskResponse task = new TaskResponse();
        task.setGoalText("学习 Spring Boot fundamentals");
        dashboard.setTodayTask(task);
        ChapterDetailResponse currentChapter = new ChapterDetailResponse();
        currentChapter.setChapterNo(1);
        currentChapter.setTitle("Spring Boot fundamentals");
        currentChapter.setStatus("IN_PROGRESS");
        dashboard.setCurrentChapter(currentChapter);
        dashboard.setCurrentMaterials(List.of(
                new LearnerMaterialReference(101L, "THEORY_DOC", "理论文档", "path/theory.md", "/api/materials/101/content", "/materials/101/view"),
                new LearnerMaterialReference(102L, "DEMO_GUIDE", "Demo 任务", "path/demo.md", "/api/materials/102/content", "/materials/102/view")
        ));
        when(learnerWorkflowService.getDashboard(eq("ou_card_dashboard"))).thenReturn(dashboard);

        mockMvc.perform(get("/api/feishu/cards/learners/ou_card_dashboard/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.cardType").value("DASHBOARD"))
                .andExpect(jsonPath("$.data.card.header.title.content").value("VeriLearn 学习总览"))
                .andExpect(jsonPath("$.data.card.body.elements[1].content").value(Matchers.containsString("第 1 章 Spring Boot fundamentals")))
                .andExpect(jsonPath("$.data.card.body.elements[2].content").value(Matchers.containsString("/materials/101/view")))
                .andExpect(jsonPath("$.data.card.body.elements[2].content").value(Matchers.containsString("/materials/102/view")))
                .andExpect(jsonPath("$.data.card.body.elements[3].actions[2].value.action").value("SHOW_AI_PROVIDER"));
    }

    @Test
    void shouldPreviewCurrentContextCard() throws Exception {
        LearnerCurrentContextResponse context = new LearnerCurrentContextResponse();
        context.setTopic("Java 后端");
        context.setGoalStatus("ACTIVE");
        TaskResponse task = new TaskResponse();
        task.setGoalText("学习 Spring Boot fundamentals");
        context.setTodayTask(task);
        ChapterDetailResponse currentChapter = new ChapterDetailResponse();
        currentChapter.setChapterNo(1);
        currentChapter.setTitle("Spring Boot fundamentals");
        currentChapter.setStatus("IN_PROGRESS");
        context.setCurrentChapter(currentChapter);
        context.setCurrentMaterials(List.of(
                new LearnerMaterialReference(101L, "THEORY_DOC", "理论文档", "path/theory.md", "/api/materials/101/content", "/materials/101/view"),
                new LearnerMaterialReference(201L, "EVALUATION_REPORT", "评估报告", "path/evaluation-report.md", "/api/materials/201/content", "/materials/201/view"),
                new LearnerMaterialReference(202L, "NEXT_STEP_NOTE", "下一步建议", "path/next-step.md", "/api/materials/202/content", "/materials/202/view")
        ));
        context.setEvaluationContentUrl("/api/materials/201/content");
        context.setEvaluationViewUrl("/materials/201/view");
        context.setNextStepContentUrl("/api/materials/202/content");
        context.setNextStepViewUrl("/materials/202/view");
        when(learnerWorkflowService.getCurrentContext(eq("ou_card_context"))).thenReturn(context);

        mockMvc.perform(get("/api/feishu/cards/learners/ou_card_context/current-context"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.cardType").value("CURRENT_CONTEXT"))
                .andExpect(jsonPath("$.data.card.header.title.content").value("VeriLearn 当前学习上下文"))
                .andExpect(jsonPath("$.data.card.body.elements[2].content").value(Matchers.containsString("/materials/101/view")))
                .andExpect(jsonPath("$.data.card.body.elements[3].content").value(Matchers.containsString("/materials/201/view")))
                .andExpect(jsonPath("$.data.card.body.elements[3].content").value(Matchers.containsString("/materials/202/view")));
    }

    @Test
    void shouldPreviewAiProviderCard() throws Exception {
        AiProviderConfigResponse currentConfig = new AiProviderConfigResponse();
        currentConfig.setConfigId(2L);
        currentConfig.setProviderType("OPENAI");
        currentConfig.setModelName("gpt-4.1-mini");
        currentConfig.setBaseUrl("https://api.openai.com/v1");
        currentConfig.setApiKeyMasked("sk-open***mini");
        currentConfig.setSourceType("USER_CONFIG");
        currentConfig.setActive(true);

        AiProviderConfigResponse deepseekConfig = new AiProviderConfigResponse();
        deepseekConfig.setConfigId(1L);
        deepseekConfig.setProviderType("DEEPSEEK");
        deepseekConfig.setModelName("deepseek-chat");
        deepseekConfig.setBaseUrl("https://api.deepseek.com");
        deepseekConfig.setApiKeyMasked("sk-deep***chat");
        deepseekConfig.setActive(false);

        when(aiProviderConfigService.getCurrentConfig(eq("ou_card_ai"))).thenReturn(currentConfig);
        when(aiProviderConfigService.listConfigs(eq("ou_card_ai"))).thenReturn(List.of(currentConfig, deepseekConfig));

        mockMvc.perform(get("/api/feishu/cards/learners/ou_card_ai/ai-provider"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.cardType").value("AI_PROVIDER"))
                .andExpect(jsonPath("$.data.card.header.title.content").value("VeriLearn AI 模型配置"))
                .andExpect(jsonPath("$.data.card.body.elements[0].content").value(Matchers.containsString("提供方：OPENAI")))
                .andExpect(jsonPath("$.data.card.body.elements[1].content").value(Matchers.containsString("配置ID 1：DEEPSEEK / deepseek-chat（可切换）")))
                .andExpect(jsonPath("$.data.card.body.elements[2].content").value(Matchers.containsString("/ai/provider-config-page?openId=ou_card_ai")))
                .andExpect(jsonPath("$.data.card.body.elements[3].actions[0].value.action").value("ACTIVATE_AI_PROVIDER"))
                .andExpect(jsonPath("$.data.card.body.elements[3].actions[0].value.config_id").value(1));
    }

    @Test
    void shouldHandleShowProgressCardAction() throws Exception {
        LearnerDashboardResponse dashboard = new LearnerDashboardResponse();
        ProgressResponse progress = new ProgressResponse();
        progress.setTopic("Java 后端");
        progress.setTotalNodes(4);
        progress.setInProgressNodes(1);
        progress.setPassedNodes(2);
        progress.setNeedsRetryNodes(1);
        progress.setTotalChapters(4);
        progress.setInProgressChapters(1);
        progress.setCompletedChapters(2);
        progress.setPendingReviewChapters(1);
        dashboard.setProgress(progress);
        when(learnerWorkflowService.getDashboard(eq("ou_card_action"))).thenReturn(dashboard);

        mockMvc.perform(post("/api/feishu/cards/callbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "open_id": "ou_card_action",
                                  "action": {
                                    "value": {
                                      "action": "SHOW_PROGRESS"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.action").value("SHOW_PROGRESS"))
                .andExpect(jsonPath("$.data.toastText").value("已切换为进度总览卡片"))
                .andExpect(jsonPath("$.data.card.header.title.content").value("VeriLearn 进度总览"))
                .andExpect(jsonPath("$.data.card.body.elements[0].content").value(Matchers.containsString("Java 后端")));
    }

    @Test
    void shouldHandleShowCurrentContextCardAction() throws Exception {
        LearnerCurrentContextResponse context = new LearnerCurrentContextResponse();
        context.setTopic("Java 后端");
        context.setGoalStatus("ACTIVE");
        context.setCurrentMaterials(List.of(
                new LearnerMaterialReference(301L, "THEORY_DOC", "理论文档", "path/theory.md", "/api/materials/301/content", "/materials/301/view")
        ));
        when(learnerWorkflowService.getCurrentContext(eq("ou_card_context_action"))).thenReturn(context);

        mockMvc.perform(post("/api/feishu/cards/callbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "open_id": "ou_card_context_action",
                                  "action": {
                                    "value": {
                                      "action": "SHOW_CURRENT_CONTEXT"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.action").value("SHOW_CURRENT_CONTEXT"))
                .andExpect(jsonPath("$.data.toastText").value("已切换为当前学习上下文卡片"))
                .andExpect(jsonPath("$.data.card.header.title.content").value("VeriLearn 当前学习上下文"))
                .andExpect(jsonPath("$.data.card.body.elements[1].content").value(Matchers.containsString("/materials/301/view")));
    }

    @Test
    void shouldHandleActivateAiProviderCardAction() throws Exception {
        AiProviderConfigResponse activatedConfig = new AiProviderConfigResponse();
        activatedConfig.setConfigId(3L);
        activatedConfig.setProviderType("OPENAI");
        activatedConfig.setModelName("gpt-4.1-mini");
        activatedConfig.setBaseUrl("https://api.openai.com/v1");
        activatedConfig.setApiKeyMasked("sk-open***mini");
        activatedConfig.setSourceType("USER_CONFIG");
        activatedConfig.setActive(true);

        AiProviderConfigResponse deepseekConfig = new AiProviderConfigResponse();
        deepseekConfig.setConfigId(1L);
        deepseekConfig.setProviderType("DEEPSEEK");
        deepseekConfig.setModelName("deepseek-chat");
        deepseekConfig.setBaseUrl("https://api.deepseek.com");
        deepseekConfig.setApiKeyMasked("sk-deep***chat");
        deepseekConfig.setActive(false);

        when(aiProviderConfigService.activateConfig(eq("ou_card_ai_action"), eq(3L))).thenReturn(activatedConfig);
        when(aiProviderConfigService.listConfigs(eq("ou_card_ai_action"))).thenReturn(List.of(activatedConfig, deepseekConfig));

        mockMvc.perform(post("/api/feishu/cards/callbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "open_id": "ou_card_ai_action",
                                  "action": {
                                    "value": {
                                      "action": "ACTIVATE_AI_PROVIDER",
                                      "config_id": 3
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.action").value("ACTIVATE_AI_PROVIDER"))
                .andExpect(jsonPath("$.data.toastText").value(Matchers.containsString("已切换到 OPENAI")))
                .andExpect(jsonPath("$.data.card.header.title.content").value("VeriLearn AI 模型配置"))
                .andExpect(jsonPath("$.data.card.body.elements[0].content").value(Matchers.containsString("提供方：OPENAI")));
    }
}

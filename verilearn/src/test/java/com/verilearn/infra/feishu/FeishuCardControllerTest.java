package com.verilearn.infra.feishu;

import com.verilearn.progress.dto.ProgressResponse;
import com.verilearn.task.dto.TaskResponse;
import com.verilearn.validation.dto.ValidationItemResponse;
import com.verilearn.workflow.dto.LearnerDashboardResponse;
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

    @Test
    void shouldPreviewTodayTaskCard() throws Exception {
        TaskResponse task = new TaskResponse();
        task.setTaskId(1001L);
        task.setGoalText("学习 Spring Boot fundamentals");
        task.setChapterTitle("Spring Boot fundamentals");
        task.setStepType("READ_THEORY");
        task.setStatus("PENDING");
        task.setTheoryFilePath("spring-boot/01-spring-boot-fundamentals/user/theory/theory.md");
        task.setDemoFilePath("spring-boot/01-spring-boot-fundamentals/user/demo/demo-task.md");
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
                .andExpect(jsonPath("$.data.card.body.elements[0].content").value(Matchers.containsString("theory.md")))
                .andExpect(jsonPath("$.data.card.body.elements[0].content").value(Matchers.containsString("demo-task.md")))
                .andExpect(jsonPath("$.data.card.body.elements[2].actions[0].value.action").value("REFRESH_TODAY_TASK"));
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
                .andExpect(jsonPath("$.data.card.header.title.content").value("VeriLearn 进度概览"))
                .andExpect(jsonPath("$.data.card.body.elements[0].content").value(Matchers.containsString("Java 后端")));
    }
}

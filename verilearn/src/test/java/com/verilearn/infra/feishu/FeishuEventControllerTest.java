package com.verilearn.infra.feishu;

import com.verilearn.infra.feishu.service.FeishuMessagingService;
import com.verilearn.progress.dto.ProgressResponse;
import com.verilearn.task.dto.TaskResponse;
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
                        .content("""
                                {
                                  "header": {
                                    "event_type": "im.message.receive_v1"
                                  },
                                  "event": {
                                    "sender": {
                                      "sender_id": {
                                        "open_id": "ou_test_start"
                                      }
                                    },
                                    "message": {
                                      "message_type": "text",
                                      "content": "{\\"text\\":\\"/start Java 后端\\"}"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.command").value("/start"))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("已为你初始化学习目标：Java 后端")));

        verify(feishuMessagingService).sendTextMessage(eq("ou_test_start"), contains("已为你初始化学习目标：Java 后端"));
    }

    @Test
    void shouldHandleTodayCommand() throws Exception {
        TaskResponse taskResponse = new TaskResponse();
        taskResponse.setGoalText("学习 Spring Boot fundamentals");
        taskResponse.setChapterTitle("Spring Boot fundamentals");
        taskResponse.setStepType("READ_THEORY");
        taskResponse.setStatus("PENDING");
        taskResponse.setTheoryFilePath("spring-boot/01-spring-boot-fundamentals/user/theory/theory.md");
        taskResponse.setDemoFilePath("spring-boot/01-spring-boot-fundamentals/user/demo/demo-task.md");
        taskResponse.setValidationItems(List.of());
        when(learnerWorkflowService.generateTodayTask(eq("ou_test_today"))).thenReturn(taskResponse);

        mockMvc.perform(post("/api/feishu/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "header": {
                                    "event_type": "im.message.receive_v1"
                                  },
                                  "event": {
                                    "sender": {
                                      "sender_id": {
                                        "open_id": "ou_test_today"
                                      }
                                    },
                                    "message": {
                                      "message_type": "text",
                                      "content": "{\\"text\\":\\"/today\\"}"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.command").value("/today"))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("今日任务：学习 Spring Boot fundamentals")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("theory.md")))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("demo-task.md")));
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
                        .content("""
                                {
                                  "header": {
                                    "event_type": "im.message.receive_v1"
                                  },
                                  "event": {
                                    "sender": {
                                      "sender_id": {
                                        "open_id": "ou_test_progress"
                                      }
                                    },
                                    "message": {
                                      "message_type": "text",
                                      "content": "{\\"text\\":\\"/progress\\"}"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.command").value("/progress"))
                .andExpect(jsonPath("$.data.replyText").value(Matchers.containsString("当前主题：Java 后端")));
    }
}

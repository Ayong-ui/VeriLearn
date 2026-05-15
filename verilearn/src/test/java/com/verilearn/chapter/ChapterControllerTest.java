package com.verilearn.chapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.verilearn.ai.dto.AiDemoEvaluationResult;
import com.verilearn.ai.service.AiEvaluationService;
import com.verilearn.ai.dto.AiChapterMaterialResult;
import com.verilearn.ai.service.AiMaterialService;
import com.verilearn.chapter.entity.LearningChapter;
import com.verilearn.chapter.mapper.LearningChapterMapper;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChapterControllerTest {

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

    @MockBean
    private AiMaterialService aiMaterialService;

    @MockBean
    private AiEvaluationService aiEvaluationService;

    @Test
    void shouldBootstrapStartAndCompleteChapterWorkflow() throws Exception {
        Long goalId = createGoalWithNodes();
        mockAiMaterialService();

        mockMvc.perform(post("/api/goals/{goalId}/chapters/bootstrap", goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.goalId").value(goalId))
                .andExpect(jsonPath("$.data.chapterCount").value(2));

        LearningChapter chapter = learningChapterMapper.selectOne(
                new LambdaQueryWrapper<LearningChapter>()
                        .eq(LearningChapter::getGoalId, goalId)
                        .eq(LearningChapter::getChapterNo, 1)
                        .last("LIMIT 1")
        );
        assertNotNull(chapter);

        mockMvc.perform(post("/api/chapters/{chapterId}/start", chapter.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.steps[0].status").value("IN_PROGRESS"));

        String chapterDetail = mockMvc.perform(get("/api/chapters/{chapterId}", chapter.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.materials.length()").value(2))
                .andExpect(jsonPath("$.data.materials[0].filePath").value(org.hamcrest.Matchers.endsWith(".md")))
                .andExpect(jsonPath("$.data.steps.length()").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long firstMaterialId = JsonPathHelper.readLong(chapterDetail, "$.data.materials[0].id");
        Long firstStepId = JsonPathHelper.readLong(chapterDetail, "$.data.steps[0].id");
        Long secondStepId = JsonPathHelper.readLong(chapterDetail, "$.data.steps[1].id");
        Long thirdStepId = JsonPathHelper.readLong(chapterDetail, "$.data.steps[2].id");

        mockMvc.perform(get("/api/materials/{materialId}/content", firstMaterialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.materialId").value(firstMaterialId))
                .andExpect(jsonPath("$.data.filePath").value(org.hamcrest.Matchers.endsWith(".md")))
                .andExpect(jsonPath("$.data.viewUrl").value("/materials/" + firstMaterialId + "/view"))
                .andExpect(jsonPath("$.data.contentText").value(org.hamcrest.Matchers.containsString("Spring basics")));

        mockMvc.perform(get("/materials/{materialId}/view", firstMaterialId))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("/api/materials/" + firstMaterialId + "/content")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("theory.md")));

        mockMvc.perform(post("/api/chapters/{chapterId}/steps/submit", chapter.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stepId": %d,
                                  "feedbackNote": "Theory understood.",
                                  "needsReview": false
                                }
                                """.formatted(firstStepId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextStepId").value(secondStepId))
                .andExpect(jsonPath("$.data.nextStepType").value("RUN_DEMO"));

        mockMvc.perform(post("/api/chapters/{chapterId}/steps/submit", chapter.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stepId": %d,
                                  "feedbackNote": "Demo completed but review would help.",
                                  "needsReview": true
                                }
                                """.formatted(secondStepId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextStepId").value(thirdStepId))
                .andExpect(jsonPath("$.data.reviewStatus").value("PENDING"));

        mockMvc.perform(post("/api/chapters/{chapterId}/steps/submit", chapter.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stepId": %d,
                                  "feedbackNote": "Chapter completed.",
                                  "needsReview": false
                                }
                                """.formatted(thirdStepId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chapterStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.reviewStatus").value("PENDING"));

        mockMvc.perform(get("/api/goals/{goalId}/chapters/reviews/pending", goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].chapterId").value(chapter.getId()));

        mockMvc.perform(post("/api/chapters/{chapterId}/review/complete", chapter.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value("REVIEWED"));
    }

    @Test
    void shouldGenerateChapterMaterials() throws Exception {
        Long goalId = createGoalWithNodes();
        mockAiMaterialService();

        mockMvc.perform(post("/api/goals/{goalId}/chapters/bootstrap", goalId))
                .andExpect(status().isOk());

        LearningChapter chapter = learningChapterMapper.selectOne(
                new LambdaQueryWrapper<LearningChapter>()
                        .eq(LearningChapter::getGoalId, goalId)
                        .eq(LearningChapter::getChapterNo, 1)
                        .last("LIMIT 1")
        );
        assertNotNull(chapter);

        mockMvc.perform(post("/api/chapters/{chapterId}/materials/generate", chapter.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.summary").value("AI summary for chapter"))
                .andExpect(jsonPath("$.data.materials[0].status").value("AI_GENERATED"))
                .andExpect(jsonPath("$.data.materials[0].filePath").value(org.hamcrest.Matchers.endsWith(".md")))
                .andExpect(jsonPath("$.data.materials[0].contentText").exists())
                .andExpect(jsonPath("$.data.materials[1].status").value("AI_GENERATED"))
                .andExpect(jsonPath("$.data.materials[1].contentText").exists());
    }

    @Test
    void shouldEvaluateDemoSubmissionAndCreateEvaluationFiles() throws Exception {
        Long goalId = createGoalWithNodes();
        mockAiMaterialService();
        mockAiEvaluationService();

        mockMvc.perform(post("/api/goals/{goalId}/chapters/bootstrap", goalId))
                .andExpect(status().isOk());

        LearningChapter chapter = learningChapterMapper.selectOne(
                new LambdaQueryWrapper<LearningChapter>()
                        .eq(LearningChapter::getGoalId, goalId)
                        .eq(LearningChapter::getChapterNo, 1)
                        .last("LIMIT 1")
        );
        assertNotNull(chapter);

        mockMvc.perform(post("/api/chapters/{chapterId}/start", chapter.getId()))
                .andExpect(status().isOk());

        String chapterDetail = mockMvc.perform(get("/api/chapters/{chapterId}", chapter.getId()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long firstStepId = JsonPathHelper.readLong(chapterDetail, "$.data.steps[0].id");
        Long secondStepId = JsonPathHelper.readLong(chapterDetail, "$.data.steps[1].id");

        mockMvc.perform(post("/api/chapters/{chapterId}/steps/submit", chapter.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stepId": %d,
                                  "feedbackNote": "Theory understood.",
                                  "needsReview": false
                                }
                                """.formatted(firstStepId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextStepId").value(secondStepId))
                .andExpect(jsonPath("$.data.nextStepType").value("RUN_DEMO"));

        mockMvc.perform(post("/api/chapters/{chapterId}/demo-evaluations", chapter.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stepId": %d,
                                  "submissionSummary": "I created the controller and explained the request mapping.",
                                  "codeSnippet": "@RestController public class DemoController {}",
                                  "question": "Why is @RestController better here?"
                                }
                                """.formatted(secondStepId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.chapterId").value(chapter.getId()))
                .andExpect(jsonPath("$.data.completedStepId").value(secondStepId))
                .andExpect(jsonPath("$.data.understandingLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.evaluationFilePath").value(org.hamcrest.Matchers.endsWith("evaluation-report.md")))
                .andExpect(jsonPath("$.data.evaluationViewUrl").value(org.hamcrest.Matchers.endsWith("/view")))
                .andExpect(jsonPath("$.data.nextStepFilePath").value(org.hamcrest.Matchers.endsWith("next-step.md")))
                .andExpect(jsonPath("$.data.nextStepViewUrl").value(org.hamcrest.Matchers.endsWith("/view")))
                .andExpect(jsonPath("$.data.nextStepType").value("SUBMIT_FEEDBACK"))
                .andExpect(jsonPath("$.data.chapterStatus").value("IN_PROGRESS"));
    }

    private Long createGoalWithNodes() {
        LearnerUser user = new LearnerUser();
        user.setFeishuOpenId("chapter-user-" + System.nanoTime());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        learnerUserMapper.insert(user);

        LearningGoal goal = new LearningGoal();
        goal.setUserId(user.getId());
        goal.setTopic("Spring Boot");
        goal.setTargetLevel("intern");
        goal.setDailyMinutes(100);
        goal.setStatus("ACTIVE");
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());
        learningGoalMapper.insert(goal);

        insertNode(user.getId(), goal.getId(), "Spring basics", 1);
        insertNode(user.getId(), goal.getId(), "Controller design", 2);
        return goal.getId();
    }

    private void insertNode(Long userId, Long goalId, String nodeName, int sequenceNo) {
        KnowledgeNode node = new KnowledgeNode();
        node.setUserId(userId);
        node.setGoalId(goalId);
        node.setNodeName(nodeName);
        node.setSequenceNo(sequenceNo);
        node.setStatus("NOT_STARTED");
        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());
        knowledgeNodeMapper.insert(node);
    }

    private void mockAiMaterialService() {
        AiChapterMaterialResult result = new AiChapterMaterialResult();
        result.setSummary("AI summary for chapter");
        result.setTheoryContent("AI theory content");
        result.setDemoGuideContent("AI demo content");
        result.setGeneratedByAi(true);
        result.setProvider("deepseek");
        when(aiMaterialService.generateChapterMaterials(org.mockito.ArgumentMatchers.anyLong(), any(), any(), any())).thenReturn(result);
    }

    private void mockAiEvaluationService() {
        AiDemoEvaluationResult result = new AiDemoEvaluationResult();
        result.setUnderstandingLevel("HIGH");
        result.setEvaluationMarkdown("# Demo 评估报告\n\n这次 Demo 完成度较高。");
        result.setNextStepMarkdown("# 下一步建议\n\n进入提交反馈步骤，并总结你已经掌握的内容。");
        result.setShouldReview(false);
        result.setGeneratedByAi(true);
        result.setProvider("deepseek");
        when(aiEvaluationService.evaluateDemoSubmission(org.mockito.ArgumentMatchers.anyLong(), any(), any(), any(), any(), any(), any())).thenReturn(result);
    }
}

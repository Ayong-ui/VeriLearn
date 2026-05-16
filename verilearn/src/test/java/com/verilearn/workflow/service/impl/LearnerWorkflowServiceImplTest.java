package com.verilearn.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.verilearn.ai.dto.AiChapterMaterialResult;
import com.verilearn.ai.service.AiEvaluationService;
import com.verilearn.ai.service.AiMaterialService;
import com.verilearn.ai.service.AiRoutingService;
import com.verilearn.chapter.entity.ChapterMaterial;
import com.verilearn.chapter.entity.LearningChapter;
import com.verilearn.chapter.mapper.ChapterMaterialMapper;
import com.verilearn.chapter.mapper.LearningChapterMapper;
import com.verilearn.goal.entity.LearningGoal;
import com.verilearn.goal.mapper.LearningGoalMapper;
import com.verilearn.knowledge.entity.KnowledgeNode;
import com.verilearn.knowledge.mapper.KnowledgeNodeMapper;
import com.verilearn.task.dto.TaskResponse;
import com.verilearn.task.entity.DailyTask;
import com.verilearn.task.mapper.DailyTaskMapper;
import com.verilearn.user.entity.LearnerUser;
import com.verilearn.user.mapper.LearnerUserMapper;
import com.verilearn.validation.entity.ValidationItem;
import com.verilearn.validation.mapper.ValidationItemMapper;
import com.verilearn.workflow.dto.LearnerSetupRequest;
import com.verilearn.workflow.dto.LearnerSetupResponse;
import com.verilearn.workflow.service.LearnerWorkflowService;
import com.verilearn.workflow.service.LearningRouteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Transactional
class LearnerWorkflowServiceImplTest {

    private static final String LINUX_TOPIC = "Linux Route Cleanup Test";
    private static final String MYSQL_TOPIC = "MySQL Route Cleanup Test";

    @Autowired
    private LearnerWorkflowService learnerWorkflowService;

    @Autowired
    private LearningRouteService learningRouteService;

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

    @Autowired
    private DailyTaskMapper dailyTaskMapper;

    @Autowired
    private ValidationItemMapper validationItemMapper;

    @MockBean
    private AiRoutingService aiRoutingService;

    @MockBean
    private AiMaterialService aiMaterialService;

    @MockBean
    private AiEvaluationService aiEvaluationService;

    @AfterEach
    void cleanupRouteDirectories() {
        learningRouteService.deleteRouteDirectory(LINUX_TOPIC);
        learningRouteService.deleteRouteDirectory(MYSQL_TOPIC);
    }

    @Test
    void shouldClearOnlySpecifiedActiveRouteWithoutAffectingCompletedRoute() throws Exception {
        mockAiRouteAndMaterials();
        String openId = "clear-route-user";

        LearnerSetupResponse linuxSetup = learnerWorkflowService.setupLearner(setupRequest(openId, LINUX_TOPIC));
        markGoalCompleted(linuxSetup.getGoalId());

        LearnerUser learnerUser = findLearner(openId);
        String linuxRouteAbsolutePath = learningRouteService.resolveAbsolutePath(
                learningRouteService.buildRouteRelativePath(LINUX_TOPIC)
        );
        assertTrue(Files.exists(Path.of(linuxRouteAbsolutePath)));

        LearnerSetupResponse mysqlSetup = learnerWorkflowService.setupLearner(setupRequest(openId, MYSQL_TOPIC));
        LearningGoal mysqlGoal = learningGoalMapper.selectById(mysqlSetup.getGoalId());
        List<LearningChapter> mysqlChapters = learningChapterMapper.selectList(
                new LambdaQueryWrapper<LearningChapter>()
                        .eq(LearningChapter::getGoalId, mysqlGoal.getId())
        );
        List<Long> mysqlChapterIds = mysqlChapters.stream().map(LearningChapter::getId).toList();

        TaskResponse mysqlTask = learnerWorkflowService.generateTodayTask(openId);
        assertNotNull(mysqlTask);
        assertNotNull(mysqlTask.getTaskId());

        Long validationItemCountBeforeClear = validationItemMapper.selectCount(
                new LambdaQueryWrapper<ValidationItem>()
                        .eq(ValidationItem::getTaskId, mysqlTask.getTaskId())
        );
        assertTrue(validationItemCountBeforeClear > 0);

        String mysqlRouteAbsolutePath = learningRouteService.resolveAbsolutePath(
                learningRouteService.buildRouteRelativePath(MYSQL_TOPIC)
        );
        assertTrue(Files.exists(Path.of(mysqlRouteAbsolutePath)));

        learnerWorkflowService.clearLearningRoute(openId, MYSQL_TOPIC);

        LearningGoal linuxGoal = learningGoalMapper.selectById(linuxSetup.getGoalId());
        assertNotNull(linuxGoal);
        assertEquals("COMPLETED", linuxGoal.getStatus());
        assertTrue(Files.exists(Path.of(linuxRouteAbsolutePath)));

        assertNull(learningGoalMapper.selectById(mysqlSetup.getGoalId()));
        assertEquals(0L, knowledgeNodeMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getGoalId, mysqlGoal.getId())
        ));
        assertEquals(0L, learningChapterMapper.selectCount(
                new LambdaQueryWrapper<LearningChapter>()
                        .eq(LearningChapter::getGoalId, mysqlGoal.getId())
        ));
        assertEquals(0L, chapterMaterialMapper.selectCount(
                new LambdaQueryWrapper<ChapterMaterial>()
                        .in(!mysqlChapterIds.isEmpty(), ChapterMaterial::getChapterId, mysqlChapterIds)
        ));
        assertEquals(0L, dailyTaskMapper.selectCount(
                new LambdaQueryWrapper<DailyTask>()
                        .eq(DailyTask::getUserId, learnerUser.getId())
        ));
        assertEquals(0L, validationItemMapper.selectCount(
                new LambdaQueryWrapper<ValidationItem>()
                        .eq(ValidationItem::getTaskId, mysqlTask.getTaskId())
        ));
        assertFalse(Files.exists(Path.of(mysqlRouteAbsolutePath)));
    }

    private void mockAiRouteAndMaterials() {
        given(aiRoutingService.chatForUser(anyLong(), anyString(), anyString()))
                .willAnswer(invocation -> buildRouteResponse(extractTopic(invocation.getArgument(2, String.class))));
        given(aiMaterialService.generateChapterMaterials(anyLong(), anyString(), anyString(), org.mockito.ArgumentMatchers.<String>nullable(String.class)))
                .willReturn(mockMaterialResult());
    }

    private LearnerSetupRequest setupRequest(String openId, String topic) {
        LearnerSetupRequest request = new LearnerSetupRequest();
        request.setFeishuOpenId(openId);
        request.setTopic(topic);
        request.setTargetLevel("intern");
        request.setDailyMinutes(120);
        return request;
    }

    private void markGoalCompleted(Long goalId) {
        LearningGoal goal = learningGoalMapper.selectById(goalId);
        goal.setStatus("COMPLETED");
        goal.setUpdatedAt(LocalDateTime.now());
        learningGoalMapper.updateById(goal);
    }

    private LearnerUser findLearner(String openId) {
        LearnerUser learnerUser = learnerUserMapper.selectOne(
                new LambdaQueryWrapper<LearnerUser>()
                        .eq(LearnerUser::getFeishuOpenId, openId)
                        .orderByDesc(LearnerUser::getId)
                        .last("LIMIT 1")
        );
        assertNotNull(learnerUser);
        return learnerUser;
    }

    private String extractTopic(String prompt) {
        int topicStart = prompt.indexOf("Topic:");
        if (topicStart < 0) {
            return "Unknown Topic";
        }
        int lineEnd = prompt.indexOf('\n', topicStart);
        if (lineEnd < 0) {
            lineEnd = prompt.length();
        }
        return prompt.substring(topicStart + "Topic:".length(), lineEnd).trim();
    }

    private String buildRouteResponse(String topic) {
        return """
                [OVERVIEW]
                这是一条围绕 %s 的 4 章自学路线，帮助学习者从核心概念逐步过渡到综合应用。
                [CHAPTERS]
                1. %s 核心概念 | 先理解基本定义、常见术语和整体作用
                2. %s 基础操作 | 熟悉最常见的日常使用方法和关键步骤
                3. %s 场景实践 | 能结合简单场景完成一次基础练习
                4. %s 综合应用 | 能总结经验并形成可复用的实践方式
                """.formatted(topic, topic, topic, topic, topic).trim();
    }

    private AiChapterMaterialResult mockMaterialResult() {
        AiChapterMaterialResult result = new AiChapterMaterialResult();
        result.setSummary("材料生成完成");
        result.setTheoryContent("""
                ## 概念定义
                这是一个可用于测试的理论文档。

                ## 使用场景
                适用于当前章节的入门学习。

                ## 核心方法
                按步骤完成学习任务。

                ## 原理说明
                通过分阶段练习理解关键机制。

                ## 常见误区
                不要跳过基础概念。
                """);
        result.setDemoGuideContent("""
                ## 练习目标
                完成当前章节的演示练习。

                ## 练习步骤
                1. 阅读理论文档
                2. 按步骤完成练习

                ## 预期结果
                能说明本章关键概念。

                ## 复盘问题
                - 你理解了什么
                - 哪些地方还不确定
                """);
        result.setGeneratedByAi(true);
        result.setProvider("mock");
        return result;
    }
}

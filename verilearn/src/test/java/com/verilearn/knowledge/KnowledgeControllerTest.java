package com.verilearn.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class KnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LearnerUserMapper learnerUserMapper;

    @Autowired
    private LearningGoalMapper learningGoalMapper;

    @Autowired
    private KnowledgeNodeMapper knowledgeNodeMapper;

    @Test
    void shouldReplaceAndQueryKnowledgeNodes() throws Exception {
        Long goalId = createGoal("knowledge-replace-user");

        mockMvc.perform(post("/api/goals/{goalId}/knowledge-nodes", goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodes": [
                                    {
                                      "nodeName": "Java syntax",
                                      "sequenceNo": 2
                                    },
                                    {
                                      "nodeName": "Spring Boot basics",
                                      "sequenceNo": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].status").value("DRAFT"));

        mockMvc.perform(post("/api/goals/{goalId}/knowledge-nodes", goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodes": [
                                    {
                                      "nodeName": "Collections",
                                      "sequenceNo": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].nodeName").value("Collections"));

        mockMvc.perform(get("/api/goals/{goalId}/knowledge-nodes", goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].nodeName").value("Collections"))
                .andExpect(jsonPath("$.data[0].sequenceNo").value(1));

        List<KnowledgeNode> savedNodes = knowledgeNodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getGoalId, goalId)
        );
        assertEquals(1, savedNodes.size());
        assertEquals("Collections", savedNodes.get(0).getNodeName());
    }

    @Test
    void shouldConfirmKnowledgeNodesAndInitializeStatus() throws Exception {
        Long goalId = createGoal("knowledge-confirm-user");
        mockMvc.perform(post("/api/goals/{goalId}/knowledge-nodes", goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodes": [
                                    {
                                      "nodeName": "Java syntax",
                                      "sequenceNo": 1
                                    },
                                    {
                                      "nodeName": "OOP",
                                      "sequenceNo": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/goals/{goalId}/knowledge-nodes/confirm", goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.goalId").value(goalId))
                .andExpect(jsonPath("$.data.initializedCount").value(2))
                .andExpect(jsonPath("$.data.status").value("NOT_STARTED"));

        List<KnowledgeNode> savedNodes = knowledgeNodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getGoalId, goalId)
                        .orderByAsc(KnowledgeNode::getSequenceNo)
        );
        assertEquals(2, savedNodes.size());
        assertEquals("NOT_STARTED", savedNodes.get(0).getStatus());
        assertEquals("NOT_STARTED", savedNodes.get(1).getStatus());
    }

    private Long createGoal(String feishuOpenId) {
        LearnerUser user = new LearnerUser();
        user.setFeishuOpenId(feishuOpenId);
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        learnerUserMapper.insert(user);

        LearnerUser savedUser = learnerUserMapper.selectOne(
                new LambdaQueryWrapper<LearnerUser>()
                        .eq(LearnerUser::getFeishuOpenId, feishuOpenId)
                        .last("LIMIT 1")
        );
        assertNotNull(savedUser);

        LearningGoal goal = new LearningGoal();
        goal.setUserId(savedUser.getId());
        goal.setTopic("Java backend");
        goal.setTargetLevel("intern");
        goal.setDailyMinutes(180);
        goal.setStatus("ACTIVE");
        goal.setCreatedAt(java.time.LocalDateTime.now());
        goal.setUpdatedAt(java.time.LocalDateTime.now());
        learningGoalMapper.insert(goal);
        return goal.getId();
    }
}

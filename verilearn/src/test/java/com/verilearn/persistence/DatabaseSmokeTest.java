package com.verilearn.persistence;

import com.verilearn.goal.entity.LearningGoal;
import com.verilearn.goal.mapper.LearningGoalMapper;
import com.verilearn.knowledge.entity.KnowledgeNode;
import com.verilearn.knowledge.mapper.KnowledgeNodeMapper;
import com.verilearn.user.entity.LearnerUser;
import com.verilearn.user.mapper.LearnerUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class DatabaseSmokeTest {

    @Autowired
    private LearnerUserMapper learnerUserMapper;

    @Autowired
    private LearningGoalMapper learningGoalMapper;

    @Autowired
    private KnowledgeNodeMapper knowledgeNodeMapper;

    @Test
    void shouldInsertAndReadCoreEntities() {
        LocalDateTime now = LocalDateTime.now();

        LearnerUser learnerUser = new LearnerUser();
        learnerUser.setFeishuOpenId("day3-" + UUID.randomUUID());
        learnerUser.setCreatedAt(now);
        learnerUser.setUpdatedAt(now);
        learnerUserMapper.insert(learnerUser);

        LearningGoal learningGoal = new LearningGoal();
        learningGoal.setUserId(learnerUser.getId());
        learningGoal.setTopic("Java backend");
        learningGoal.setTargetLevel("intern");
        learningGoal.setDailyMinutes(180);
        learningGoal.setStatus("ACTIVE");
        learningGoal.setCreatedAt(now);
        learningGoal.setUpdatedAt(now);
        learningGoalMapper.insert(learningGoal);

        KnowledgeNode knowledgeNode = new KnowledgeNode();
        knowledgeNode.setUserId(learnerUser.getId());
        knowledgeNode.setGoalId(learningGoal.getId());
        knowledgeNode.setParentId(null);
        knowledgeNode.setNodeName("Spring Boot basics");
        knowledgeNode.setSequenceNo(1);
        knowledgeNode.setStatus("NOT_STARTED");
        knowledgeNode.setCreatedAt(now);
        knowledgeNode.setUpdatedAt(now);
        knowledgeNodeMapper.insert(knowledgeNode);

        LearnerUser savedUser = learnerUserMapper.selectById(learnerUser.getId());
        LearningGoal savedGoal = learningGoalMapper.selectById(learningGoal.getId());
        KnowledgeNode savedNode = knowledgeNodeMapper.selectById(knowledgeNode.getId());

        assertNotNull(savedUser);
        assertNotNull(savedGoal);
        assertNotNull(savedNode);
        assertEquals(learnerUser.getId(), savedGoal.getUserId());
        assertEquals(learningGoal.getId(), savedNode.getGoalId());
    }
}

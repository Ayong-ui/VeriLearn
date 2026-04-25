package com.verilearn.goal;

import com.verilearn.goal.entity.LearningGoal;
import com.verilearn.goal.mapper.LearningGoalMapper;
import com.verilearn.user.entity.LearnerUser;
import com.verilearn.user.mapper.LearnerUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LearnerUserMapper learnerUserMapper;

    @Autowired
    private LearningGoalMapper learningGoalMapper;

    @Test
    void shouldCreateUserAndGoal() throws Exception {
        String feishuOpenId = "goal-create-user";

        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feishuOpenId": "goal-create-user",
                                  "topic": "Java backend",
                                  "targetLevel": "intern",
                                  "dailyMinutes": 180
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("goal saved successfully"))
                .andExpect(jsonPath("$.data.feishuOpenId").value(feishuOpenId))
                .andExpect(jsonPath("$.data.topic").value("Java backend"))
                .andExpect(jsonPath("$.data.targetLevel").value("intern"))
                .andExpect(jsonPath("$.data.dailyMinutes").value(180))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        LearnerUser savedUser = learnerUserMapper.selectById(
                learnerUserMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LearnerUser>()
                                .eq(LearnerUser::getFeishuOpenId, feishuOpenId)
                                .last("LIMIT 1")
                ).getId()
        );
        assertNotNull(savedUser);

        LearningGoal savedGoal = learningGoalMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LearningGoal>()
                        .eq(LearningGoal::getUserId, savedUser.getId())
                        .last("LIMIT 1")
        );
        assertNotNull(savedGoal);
        assertEquals("Java backend", savedGoal.getTopic());
        assertEquals("intern", savedGoal.getTargetLevel());
        assertEquals(180, savedGoal.getDailyMinutes());
    }

    @Test
    void shouldUpdateExistingGoalForSameUser() throws Exception {
        String feishuOpenId = "goal-update-user";

        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feishuOpenId": "goal-update-user",
                                  "topic": "Java basics",
                                  "targetLevel": "junior",
                                  "dailyMinutes": 120
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feishuOpenId": "goal-update-user",
                                  "topic": "Spring Boot",
                                  "targetLevel": "intern",
                                  "dailyMinutes": 200
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topic").value("Spring Boot"))
                .andExpect(jsonPath("$.data.targetLevel").value("intern"))
                .andExpect(jsonPath("$.data.dailyMinutes").value(200));

        LearnerUser savedUser = learnerUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LearnerUser>()
                        .eq(LearnerUser::getFeishuOpenId, feishuOpenId)
                        .last("LIMIT 1")
        );
        assertNotNull(savedUser);

        LearningGoal savedGoal = learningGoalMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LearningGoal>()
                        .eq(LearningGoal::getUserId, savedUser.getId())
                        .last("LIMIT 1")
        );
        assertNotNull(savedGoal);
        assertEquals("Spring Boot", savedGoal.getTopic());
        assertEquals("intern", savedGoal.getTargetLevel());
        assertEquals(200, savedGoal.getDailyMinutes());
    }
}

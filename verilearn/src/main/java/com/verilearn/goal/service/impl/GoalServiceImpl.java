package com.verilearn.goal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.verilearn.goal.dto.GoalResponse;
import com.verilearn.goal.dto.GoalUpsertRequest;
import com.verilearn.goal.entity.LearningGoal;
import com.verilearn.goal.mapper.LearningGoalMapper;
import com.verilearn.goal.service.GoalService;
import com.verilearn.user.entity.LearnerUser;
import com.verilearn.user.mapper.LearnerUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class GoalServiceImpl implements GoalService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final LearnerUserMapper learnerUserMapper;
    private final LearningGoalMapper learningGoalMapper;

    public GoalServiceImpl(LearnerUserMapper learnerUserMapper, LearningGoalMapper learningGoalMapper) {
        this.learnerUserMapper = learnerUserMapper;
        this.learningGoalMapper = learningGoalMapper;
    }

    @Override
    @Transactional
    public GoalResponse saveGoal(GoalUpsertRequest request) {
        LocalDateTime now = LocalDateTime.now();

        LearnerUser learnerUser = learnerUserMapper.selectOne(
                new LambdaQueryWrapper<LearnerUser>()
                        .eq(LearnerUser::getFeishuOpenId, request.getFeishuOpenId())
                        .last("LIMIT 1")
        );

        if (learnerUser == null) {
            learnerUser = new LearnerUser();
            learnerUser.setFeishuOpenId(request.getFeishuOpenId());
            learnerUser.setCreatedAt(now);
            learnerUser.setUpdatedAt(now);
            learnerUserMapper.insert(learnerUser);
        } else {
            learnerUser.setUpdatedAt(now);
            learnerUserMapper.updateById(learnerUser);
        }

        LearningGoal learningGoal = learningGoalMapper.selectOne(
                new LambdaQueryWrapper<LearningGoal>()
                        .eq(LearningGoal::getUserId, learnerUser.getId())
                        .last("LIMIT 1")
        );

        if (learningGoal == null) {
            learningGoal = new LearningGoal();
            learningGoal.setUserId(learnerUser.getId());
            learningGoal.setCreatedAt(now);
        }

        learningGoal.setTopic(request.getTopic());
        learningGoal.setTargetLevel(request.getTargetLevel());
        learningGoal.setDailyMinutes(request.getDailyMinutes());
        learningGoal.setStatus(ACTIVE_STATUS);
        learningGoal.setUpdatedAt(now);

        if (learningGoal.getId() == null) {
            learningGoalMapper.insert(learningGoal);
        } else {
            learningGoalMapper.updateById(learningGoal);
        }

        GoalResponse response = new GoalResponse();
        response.setUserId(learnerUser.getId());
        response.setGoalId(learningGoal.getId());
        response.setFeishuOpenId(learnerUser.getFeishuOpenId());
        response.setTopic(learningGoal.getTopic());
        response.setTargetLevel(learningGoal.getTargetLevel());
        response.setDailyMinutes(learningGoal.getDailyMinutes());
        response.setStatus(learningGoal.getStatus());
        return response;
    }
}

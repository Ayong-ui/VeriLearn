package com.verilearn.goal.service;

import com.verilearn.goal.dto.GoalResponse;
import com.verilearn.goal.dto.GoalUpsertRequest;

public interface GoalService {

    GoalResponse saveGoal(GoalUpsertRequest request);
}

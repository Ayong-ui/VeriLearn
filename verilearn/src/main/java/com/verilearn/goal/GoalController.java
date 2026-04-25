package com.verilearn.goal;

import com.verilearn.common.ApiResponse;
import com.verilearn.goal.dto.GoalResponse;
import com.verilearn.goal.dto.GoalUpsertRequest;
import com.verilearn.goal.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public ApiResponse<GoalResponse> saveGoal(@Valid @RequestBody GoalUpsertRequest request) {
        return ApiResponse.success("goal saved successfully", goalService.saveGoal(request));
    }
}

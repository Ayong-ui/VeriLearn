package com.verilearn.task.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class GenerateTaskRequest {

    @NotNull(message = "goalId cannot be null")
    private Long goalId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate taskDate;

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public LocalDate getTaskDate() {
        return taskDate;
    }

    public void setTaskDate(LocalDate taskDate) {
        this.taskDate = taskDate;
    }
}

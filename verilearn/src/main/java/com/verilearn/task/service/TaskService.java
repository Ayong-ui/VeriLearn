package com.verilearn.task.service;

import com.verilearn.task.dto.GenerateTaskRequest;
import com.verilearn.task.dto.TaskResponse;

import java.time.LocalDate;

public interface TaskService {

    TaskResponse generateTask(GenerateTaskRequest request);

    TaskResponse findTaskByUserAndDate(Long userId, LocalDate taskDate);
}

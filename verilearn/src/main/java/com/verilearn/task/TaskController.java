package com.verilearn.task;

import com.verilearn.common.ApiResponse;
import com.verilearn.task.dto.GenerateTaskRequest;
import com.verilearn.task.dto.TaskResponse;
import com.verilearn.task.service.TaskService;
import com.verilearn.validation.dto.TaskSubmitRequest;
import com.verilearn.validation.dto.TaskSubmitResponse;
import com.verilearn.validation.dto.ValidationItemResponse;
import com.verilearn.validation.service.ValidationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final ValidationService validationService;

    public TaskController(TaskService taskService, ValidationService validationService) {
        this.taskService = taskService;
        this.validationService = validationService;
    }

    @PostMapping("/generate")
    public ApiResponse<TaskResponse> generateTask(@Valid @RequestBody GenerateTaskRequest request) {
        return ApiResponse.success("daily task generated successfully", taskService.generateTask(request));
    }

    @PostMapping("/{taskId}/submit")
    public ApiResponse<TaskSubmitResponse> submitTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskSubmitRequest request
    ) {
        return ApiResponse.success("task submitted successfully", validationService.submitTask(taskId, request));
    }

    @PostMapping("/{taskId}/validation-items/generate")
    public ApiResponse<List<ValidationItemResponse>> regenerateValidationItems(@PathVariable Long taskId) {
        return ApiResponse.success("validation items generated successfully", validationService.regenerateValidationItems(taskId));
    }
}

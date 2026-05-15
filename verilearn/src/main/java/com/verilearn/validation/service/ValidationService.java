package com.verilearn.validation.service;

import com.verilearn.validation.dto.TaskSubmitRequest;
import com.verilearn.validation.dto.TaskSubmitResponse;
import com.verilearn.validation.dto.ValidationItemResponse;

import java.util.List;

public interface ValidationService {

    List<ValidationItemResponse> initializeValidationItems(Long taskId, Long nodeId, Long goalId, String nodeName, String stepType);

    List<ValidationItemResponse> listValidationItems(Long taskId);

    List<ValidationItemResponse> regenerateValidationItems(Long taskId);

    TaskSubmitResponse submitTask(Long taskId, TaskSubmitRequest request);
}

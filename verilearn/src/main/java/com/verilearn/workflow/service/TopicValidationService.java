package com.verilearn.workflow.service;

import com.verilearn.workflow.dto.TopicAnalysisResult;

public interface TopicValidationService {

    TopicAnalysisResult analyzeTopic(String topic);

    void validateTopicOrThrow(String topic);
}

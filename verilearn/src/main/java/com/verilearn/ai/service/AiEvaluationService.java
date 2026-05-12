package com.verilearn.ai.service;

import com.verilearn.ai.dto.AiDemoEvaluationResult;

public interface AiEvaluationService {

    AiDemoEvaluationResult evaluateDemoSubmission(
            String topic,
            String chapterTitle,
            String demoGuide,
            String submissionSummary,
            String codeSnippet,
            String question
    );
}

package com.verilearn.chapter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ChapterDemoEvaluationRequest {

    @NotNull(message = "stepId cannot be null")
    private Long stepId;

    @NotBlank(message = "submissionSummary cannot be blank")
    @Size(max = 2000, message = "submissionSummary is too long")
    private String submissionSummary;

    @Size(max = 4000, message = "codeSnippet is too long")
    private String codeSnippet;

    @Size(max = 1000, message = "question is too long")
    private String question;

    public Long getStepId() {
        return stepId;
    }

    public void setStepId(Long stepId) {
        this.stepId = stepId;
    }

    public String getSubmissionSummary() {
        return submissionSummary;
    }

    public void setSubmissionSummary(String submissionSummary) {
        this.submissionSummary = submissionSummary;
    }

    public String getCodeSnippet() {
        return codeSnippet;
    }

    public void setCodeSnippet(String codeSnippet) {
        this.codeSnippet = codeSnippet;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}

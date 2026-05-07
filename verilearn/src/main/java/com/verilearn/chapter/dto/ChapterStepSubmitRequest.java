package com.verilearn.chapter.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ChapterStepSubmitRequest {

    @NotNull(message = "stepId cannot be null")
    private Long stepId;

    @Size(max = 1000, message = "feedbackNote is too long")
    private String feedbackNote;

    private Boolean needsReview;

    public Long getStepId() {
        return stepId;
    }

    public void setStepId(Long stepId) {
        this.stepId = stepId;
    }

    public String getFeedbackNote() {
        return feedbackNote;
    }

    public void setFeedbackNote(String feedbackNote) {
        this.feedbackNote = feedbackNote;
    }

    public Boolean getNeedsReview() {
        return needsReview;
    }

    public void setNeedsReview(Boolean needsReview) {
        this.needsReview = needsReview;
    }
}

package com.verilearn.validation.dto;

import jakarta.validation.constraints.NotNull;

public class TaskSubmissionItemRequest {

    @NotNull(message = "itemId cannot be null")
    private Long itemId;

    private String submittedAnswer;

    @NotNull(message = "correct cannot be null")
    private Boolean correct;

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getSubmittedAnswer() {
        return submittedAnswer;
    }

    public void setSubmittedAnswer(String submittedAnswer) {
        this.submittedAnswer = submittedAnswer;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }
}

package com.verilearn.validation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class TaskSubmitRequest {

    @Valid
    @NotEmpty(message = "submissions cannot be empty")
    private List<TaskSubmissionItemRequest> submissions;

    public List<TaskSubmissionItemRequest> getSubmissions() {
        return submissions;
    }

    public void setSubmissions(List<TaskSubmissionItemRequest> submissions) {
        this.submissions = submissions;
    }
}

package com.verilearn.workflow.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TopicOptionSelection {

    private String originalTopic;
    private List<String> options = new ArrayList<>();
    private LocalDateTime createdAt;

    public String getOriginalTopic() {
        return originalTopic;
    }

    public void setOriginalTopic(String originalTopic) {
        this.originalTopic = originalTopic;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

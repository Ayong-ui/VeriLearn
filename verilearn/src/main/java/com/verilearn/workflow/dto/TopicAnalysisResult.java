package com.verilearn.workflow.dto;

public class TopicAnalysisResult {

    public enum TopicKind {
        REJECT,
        REQUIRE_OPTIONS,
        ACCEPT
    }

    private TopicKind kind;
    private String normalizedTopic;
    private String message;

    public TopicKind getKind() {
        return kind;
    }

    public void setKind(TopicKind kind) {
        this.kind = kind;
    }

    public String getNormalizedTopic() {
        return normalizedTopic;
    }

    public void setNormalizedTopic(String normalizedTopic) {
        this.normalizedTopic = normalizedTopic;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

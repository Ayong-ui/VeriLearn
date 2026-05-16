package com.verilearn.workflow.dto;

import java.util.ArrayList;
import java.util.List;

public class LearningRoutePlan {

    private String topic;
    private String overview;
    private String markdownContent;
    private List<LearningRouteChapter> chapters = new ArrayList<>();

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getMarkdownContent() {
        return markdownContent;
    }

    public void setMarkdownContent(String markdownContent) {
        this.markdownContent = markdownContent;
    }

    public List<LearningRouteChapter> getChapters() {
        return chapters;
    }

    public void setChapters(List<LearningRouteChapter> chapters) {
        this.chapters = chapters;
    }
}

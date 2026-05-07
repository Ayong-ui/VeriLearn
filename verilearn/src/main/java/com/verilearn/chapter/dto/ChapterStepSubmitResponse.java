package com.verilearn.chapter.dto;

public class ChapterStepSubmitResponse {

    private Long chapterId;
    private Long completedStepId;
    private Long nextStepId;
    private String nextStepType;
    private String chapterStatus;
    private String reviewStatus;

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public Long getCompletedStepId() {
        return completedStepId;
    }

    public void setCompletedStepId(Long completedStepId) {
        this.completedStepId = completedStepId;
    }

    public Long getNextStepId() {
        return nextStepId;
    }

    public void setNextStepId(Long nextStepId) {
        this.nextStepId = nextStepId;
    }

    public String getNextStepType() {
        return nextStepType;
    }

    public void setNextStepType(String nextStepType) {
        this.nextStepType = nextStepType;
    }

    public String getChapterStatus() {
        return chapterStatus;
    }

    public void setChapterStatus(String chapterStatus) {
        this.chapterStatus = chapterStatus;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }
}

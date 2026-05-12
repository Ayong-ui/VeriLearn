package com.verilearn.chapter.dto;

public class ChapterDemoEvaluationResponse {

    private Long chapterId;
    private Long completedStepId;
    private Long nextStepId;
    private String nextStepType;
    private String chapterStatus;
    private String reviewStatus;
    private String understandingLevel;
    private Long evaluationMaterialId;
    private String evaluationFilePath;
    private Long nextStepMaterialId;
    private String nextStepFilePath;

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

    public String getUnderstandingLevel() {
        return understandingLevel;
    }

    public void setUnderstandingLevel(String understandingLevel) {
        this.understandingLevel = understandingLevel;
    }

    public Long getEvaluationMaterialId() {
        return evaluationMaterialId;
    }

    public void setEvaluationMaterialId(Long evaluationMaterialId) {
        this.evaluationMaterialId = evaluationMaterialId;
    }

    public String getEvaluationFilePath() {
        return evaluationFilePath;
    }

    public void setEvaluationFilePath(String evaluationFilePath) {
        this.evaluationFilePath = evaluationFilePath;
    }

    public Long getNextStepMaterialId() {
        return nextStepMaterialId;
    }

    public void setNextStepMaterialId(Long nextStepMaterialId) {
        this.nextStepMaterialId = nextStepMaterialId;
    }

    public String getNextStepFilePath() {
        return nextStepFilePath;
    }

    public void setNextStepFilePath(String nextStepFilePath) {
        this.nextStepFilePath = nextStepFilePath;
    }
}

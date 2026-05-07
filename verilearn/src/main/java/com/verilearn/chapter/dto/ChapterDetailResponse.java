package com.verilearn.chapter.dto;

import java.util.List;

public class ChapterDetailResponse {

    private Long chapterId;
    private Long goalId;
    private Integer chapterNo;
    private String title;
    private String summary;
    private String status;
    private String reviewStatus;
    private List<ChapterMaterialResponse> materials;
    private List<ChapterStepResponse> steps;

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public Integer getChapterNo() {
        return chapterNo;
    }

    public void setChapterNo(Integer chapterNo) {
        this.chapterNo = chapterNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public List<ChapterMaterialResponse> getMaterials() {
        return materials;
    }

    public void setMaterials(List<ChapterMaterialResponse> materials) {
        this.materials = materials;
    }

    public List<ChapterStepResponse> getSteps() {
        return steps;
    }

    public void setSteps(List<ChapterStepResponse> steps) {
        this.steps = steps;
    }
}

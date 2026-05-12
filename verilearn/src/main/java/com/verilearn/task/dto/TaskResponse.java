package com.verilearn.task.dto;

import com.verilearn.validation.dto.ValidationItemResponse;

import java.time.LocalDate;
import java.util.List;

public class TaskResponse {

    private Long taskId;
    private Long userId;
    private Long goalId;
    private Long nodeId;
    private String nodeName;
    private Long chapterId;
    private String chapterTitle;
    private Long theoryMaterialId;
    private String theoryFilePath;
    private Long demoMaterialId;
    private String demoFilePath;
    private LocalDate taskDate;
    private String stepType;
    private Integer stepOrder;
    private String goalText;
    private String studyMaterial;
    private String status;
    private List<ValidationItemResponse> validationItems;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public Long getTheoryMaterialId() {
        return theoryMaterialId;
    }

    public void setTheoryMaterialId(Long theoryMaterialId) {
        this.theoryMaterialId = theoryMaterialId;
    }

    public String getTheoryFilePath() {
        return theoryFilePath;
    }

    public void setTheoryFilePath(String theoryFilePath) {
        this.theoryFilePath = theoryFilePath;
    }

    public Long getDemoMaterialId() {
        return demoMaterialId;
    }

    public void setDemoMaterialId(Long demoMaterialId) {
        this.demoMaterialId = demoMaterialId;
    }

    public String getDemoFilePath() {
        return demoFilePath;
    }

    public void setDemoFilePath(String demoFilePath) {
        this.demoFilePath = demoFilePath;
    }

    public LocalDate getTaskDate() {
        return taskDate;
    }

    public void setTaskDate(LocalDate taskDate) {
        this.taskDate = taskDate;
    }

    public String getStepType() {
        return stepType;
    }

    public void setStepType(String stepType) {
        this.stepType = stepType;
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(Integer stepOrder) {
        this.stepOrder = stepOrder;
    }

    public String getGoalText() {
        return goalText;
    }

    public void setGoalText(String goalText) {
        this.goalText = goalText;
    }

    public String getStudyMaterial() {
        return studyMaterial;
    }

    public void setStudyMaterial(String studyMaterial) {
        this.studyMaterial = studyMaterial;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ValidationItemResponse> getValidationItems() {
        return validationItems;
    }

    public void setValidationItems(List<ValidationItemResponse> validationItems) {
        this.validationItems = validationItems;
    }
}

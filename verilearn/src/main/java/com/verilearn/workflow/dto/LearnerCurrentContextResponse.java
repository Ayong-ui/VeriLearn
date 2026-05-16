package com.verilearn.workflow.dto;

import com.verilearn.chapter.dto.ChapterDetailResponse;
import com.verilearn.task.dto.TaskResponse;

import java.util.List;

public class LearnerCurrentContextResponse {

    private Long userId;
    private Long goalId;
    private String feishuOpenId;
    private String topic;
    private String goalStatus;
    private String routeFilePath;
    private String routeAbsolutePath;
    private String routeContentUrl;
    private String routeViewUrl;
    private TaskResponse todayTask;
    private ChapterDetailResponse currentChapter;
    private Long evaluationMaterialId;
    private String evaluationFilePath;
    private String evaluationContentUrl;
    private String evaluationViewUrl;
    private Long nextStepMaterialId;
    private String nextStepFilePath;
    private String nextStepContentUrl;
    private String nextStepViewUrl;
    private List<LearnerMaterialReference> currentMaterials;

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

    public String getFeishuOpenId() {
        return feishuOpenId;
    }

    public void setFeishuOpenId(String feishuOpenId) {
        this.feishuOpenId = feishuOpenId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getGoalStatus() {
        return goalStatus;
    }

    public void setGoalStatus(String goalStatus) {
        this.goalStatus = goalStatus;
    }

    public String getRouteFilePath() {
        return routeFilePath;
    }

    public void setRouteFilePath(String routeFilePath) {
        this.routeFilePath = routeFilePath;
    }

    public String getRouteAbsolutePath() {
        return routeAbsolutePath;
    }

    public void setRouteAbsolutePath(String routeAbsolutePath) {
        this.routeAbsolutePath = routeAbsolutePath;
    }

    public String getRouteContentUrl() {
        return routeContentUrl;
    }

    public void setRouteContentUrl(String routeContentUrl) {
        this.routeContentUrl = routeContentUrl;
    }

    public String getRouteViewUrl() {
        return routeViewUrl;
    }

    public void setRouteViewUrl(String routeViewUrl) {
        this.routeViewUrl = routeViewUrl;
    }

    public TaskResponse getTodayTask() {
        return todayTask;
    }

    public void setTodayTask(TaskResponse todayTask) {
        this.todayTask = todayTask;
    }

    public ChapterDetailResponse getCurrentChapter() {
        return currentChapter;
    }

    public void setCurrentChapter(ChapterDetailResponse currentChapter) {
        this.currentChapter = currentChapter;
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

    public String getEvaluationContentUrl() {
        return evaluationContentUrl;
    }

    public void setEvaluationContentUrl(String evaluationContentUrl) {
        this.evaluationContentUrl = evaluationContentUrl;
    }

    public String getEvaluationViewUrl() {
        return evaluationViewUrl;
    }

    public void setEvaluationViewUrl(String evaluationViewUrl) {
        this.evaluationViewUrl = evaluationViewUrl;
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

    public String getNextStepContentUrl() {
        return nextStepContentUrl;
    }

    public void setNextStepContentUrl(String nextStepContentUrl) {
        this.nextStepContentUrl = nextStepContentUrl;
    }

    public String getNextStepViewUrl() {
        return nextStepViewUrl;
    }

    public void setNextStepViewUrl(String nextStepViewUrl) {
        this.nextStepViewUrl = nextStepViewUrl;
    }

    public List<LearnerMaterialReference> getCurrentMaterials() {
        return currentMaterials;
    }

    public void setCurrentMaterials(List<LearnerMaterialReference> currentMaterials) {
        this.currentMaterials = currentMaterials;
    }
}

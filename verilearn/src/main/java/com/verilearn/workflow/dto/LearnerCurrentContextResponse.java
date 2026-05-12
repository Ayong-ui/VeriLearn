package com.verilearn.workflow.dto;

import com.verilearn.chapter.dto.ChapterDetailResponse;
import com.verilearn.task.dto.TaskResponse;

public class LearnerCurrentContextResponse {

    private Long userId;
    private Long goalId;
    private String feishuOpenId;
    private String topic;
    private String goalStatus;
    private TaskResponse todayTask;
    private ChapterDetailResponse currentChapter;
    private Long evaluationMaterialId;
    private String evaluationFilePath;
    private Long nextStepMaterialId;
    private String nextStepFilePath;

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

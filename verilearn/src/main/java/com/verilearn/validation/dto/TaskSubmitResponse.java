package com.verilearn.validation.dto;

import java.util.List;

public class TaskSubmitResponse {

    private Long taskId;
    private String taskStatus;
    private String nodeStatus;
    private String resultCode;
    private String reasonText;
    private boolean finalized;
    private List<ValidationItemResponse> nextValidationItems;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }

    public String getNodeStatus() {
        return nodeStatus;
    }

    public void setNodeStatus(String nodeStatus) {
        this.nodeStatus = nodeStatus;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getReasonText() {
        return reasonText;
    }

    public void setReasonText(String reasonText) {
        this.reasonText = reasonText;
    }

    public boolean isFinalized() {
        return finalized;
    }

    public void setFinalized(boolean finalized) {
        this.finalized = finalized;
    }

    public List<ValidationItemResponse> getNextValidationItems() {
        return nextValidationItems;
    }

    public void setNextValidationItems(List<ValidationItemResponse> nextValidationItems) {
        this.nextValidationItems = nextValidationItems;
    }
}

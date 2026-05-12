package com.verilearn.workflow.dto;

public class LearnerMaterialReference {

    private Long materialId;
    private String materialType;
    private String displayName;
    private String filePath;
    private String contentUrl;

    public LearnerMaterialReference() {
    }

    public LearnerMaterialReference(Long materialId, String materialType, String displayName, String filePath, String contentUrl) {
        this.materialId = materialId;
        this.materialType = materialType;
        this.displayName = displayName;
        this.filePath = filePath;
        this.contentUrl = contentUrl;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }
}

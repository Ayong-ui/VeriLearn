package com.verilearn.workflow.dto;

public class LearnerMaterialReference {

    private Long materialId;
    private String filePath;

    public LearnerMaterialReference() {
    }

    public LearnerMaterialReference(Long materialId, String filePath) {
        this.materialId = materialId;
        this.filePath = filePath;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}

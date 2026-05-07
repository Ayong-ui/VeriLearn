package com.verilearn.knowledge.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class KnowledgeNodeDraftRequest {

    @NotBlank(message = "nodeName cannot be blank")
    @Size(max = 100, message = "nodeName is too long")
    private String nodeName;

    private Long parentId;

    @NotNull(message = "sequenceNo cannot be null")
    @Min(value = 1, message = "sequenceNo must be at least 1")
    private Integer sequenceNo;

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Integer sequenceNo) {
        this.sequenceNo = sequenceNo;
    }
}

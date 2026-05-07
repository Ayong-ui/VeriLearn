package com.verilearn.knowledge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class KnowledgeNodeBatchUpsertRequest {

    @Valid
    @NotEmpty(message = "nodes cannot be empty")
    private List<KnowledgeNodeDraftRequest> nodes;

    public List<KnowledgeNodeDraftRequest> getNodes() {
        return nodes;
    }

    public void setNodes(List<KnowledgeNodeDraftRequest> nodes) {
        this.nodes = nodes;
    }
}

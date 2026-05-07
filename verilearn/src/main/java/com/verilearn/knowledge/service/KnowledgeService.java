package com.verilearn.knowledge.service;

import com.verilearn.knowledge.dto.KnowledgeNodeBatchUpsertRequest;
import com.verilearn.knowledge.dto.KnowledgeNodeConfirmResponse;
import com.verilearn.knowledge.dto.KnowledgeNodeResponse;

import java.util.List;

public interface KnowledgeService {

    List<KnowledgeNodeResponse> replaceKnowledgeNodes(Long goalId, KnowledgeNodeBatchUpsertRequest request);

    KnowledgeNodeConfirmResponse confirmKnowledgeNodes(Long goalId);

    List<KnowledgeNodeResponse> listKnowledgeNodes(Long goalId);
}

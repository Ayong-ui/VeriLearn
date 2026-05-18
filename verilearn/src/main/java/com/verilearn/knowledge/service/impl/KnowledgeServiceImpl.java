package com.verilearn.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.verilearn.goal.entity.LearningGoal;
import com.verilearn.goal.mapper.LearningGoalMapper;
import com.verilearn.knowledge.dto.KnowledgeNodeBatchUpsertRequest;
import com.verilearn.knowledge.dto.KnowledgeNodeConfirmResponse;
import com.verilearn.knowledge.dto.KnowledgeNodeDraftRequest;
import com.verilearn.knowledge.dto.KnowledgeNodeResponse;
import com.verilearn.knowledge.entity.KnowledgeNode;
import com.verilearn.knowledge.mapper.KnowledgeNodeMapper;
import com.verilearn.knowledge.model.NodeStatus;
import com.verilearn.knowledge.service.KnowledgeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final String DRAFT_STATUS = "DRAFT";

    private final LearningGoalMapper learningGoalMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;

    public KnowledgeServiceImpl(LearningGoalMapper learningGoalMapper, KnowledgeNodeMapper knowledgeNodeMapper) {
        this.learningGoalMapper = learningGoalMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
    }

    @Override
    @Transactional
    public List<KnowledgeNodeResponse> replaceKnowledgeNodes(Long goalId, KnowledgeNodeBatchUpsertRequest request) {
        LearningGoal goal = getGoalOrThrow(goalId);
        LocalDateTime now = LocalDateTime.now();

        knowledgeNodeMapper.delete(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getGoalId, goalId)
        );

        List<KnowledgeNode> savedNodes = new ArrayList<>();
        for (KnowledgeNodeDraftRequest item : request.getNodes()) {
            KnowledgeNode node = new KnowledgeNode();
            node.setUserId(goal.getUserId());
            node.setGoalId(goalId);
            node.setParentId(item.getParentId());
            node.setNodeName(item.getNodeName());
            node.setSequenceNo(item.getSequenceNo());
            node.setStatus(DRAFT_STATUS);
            node.setCreatedAt(now);
            node.setUpdatedAt(now);
            knowledgeNodeMapper.insert(node);
            savedNodes.add(node);
        }

        return toResponseList(savedNodes);
    }

    @Override
    @Transactional
    public KnowledgeNodeConfirmResponse confirmKnowledgeNodes(Long goalId) {
        getGoalOrThrow(goalId);
        List<KnowledgeNode> nodes = listNodesByGoalId(goalId);
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("knowledge nodes not found");
        }

        int initializedCount = 0;
        LocalDateTime now = LocalDateTime.now();
        for (KnowledgeNode node : nodes) {
            if (DRAFT_STATUS.equals(node.getStatus())) {
                node.setStatus(NodeStatus.NOT_STARTED.name());
                node.setUpdatedAt(now);
                knowledgeNodeMapper.updateById(node);
                initializedCount++;
            }
        }

        KnowledgeNodeConfirmResponse response = new KnowledgeNodeConfirmResponse();
        response.setGoalId(goalId);
        response.setInitializedCount(initializedCount);
        response.setStatus(NodeStatus.NOT_STARTED.name());
        return response;
    }

    @Override
    public List<KnowledgeNodeResponse> listKnowledgeNodes(Long goalId) {
        getGoalOrThrow(goalId);
        return toResponseList(listNodesByGoalId(goalId));
    }

    private LearningGoal getGoalOrThrow(Long goalId) {
        LearningGoal goal = learningGoalMapper.selectById(goalId);
        if (goal == null) {
            throw new IllegalArgumentException("goal not found");
        }
        return goal;
    }

    private List<KnowledgeNode> listNodesByGoalId(Long goalId) {
        return knowledgeNodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getGoalId, goalId)
                        .orderByAsc(KnowledgeNode::getSequenceNo)
                        .orderByAsc(KnowledgeNode::getId)
        );
    }

    private List<KnowledgeNodeResponse> toResponseList(List<KnowledgeNode> nodes) {
        List<KnowledgeNodeResponse> responses = new ArrayList<>();
        for (KnowledgeNode node : nodes) {
            KnowledgeNodeResponse response = new KnowledgeNodeResponse();
            response.setId(node.getId());
            response.setUserId(node.getUserId());
            response.setGoalId(node.getGoalId());
            response.setParentId(node.getParentId());
            response.setNodeName(node.getNodeName());
            response.setSequenceNo(node.getSequenceNo());
            response.setStatus(node.getStatus());
            responses.add(response);
        }
        return responses;
    }
}

package com.verilearn.workflow.service.impl;

import com.verilearn.workflow.dto.TopicOptionSelection;
import com.verilearn.workflow.service.PendingTopicSelectionService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PendingTopicSelectionServiceImpl implements PendingTopicSelectionService {

    private final Map<String, TopicOptionSelection> pendingSelections = new ConcurrentHashMap<>();

    @Override
    public void save(String feishuOpenId, TopicOptionSelection selection) {
        pendingSelections.put(feishuOpenId, selection);
    }

    @Override
    public TopicOptionSelection get(String feishuOpenId) {
        return pendingSelections.get(feishuOpenId);
    }

    @Override
    public TopicOptionSelection consume(String feishuOpenId) {
        return pendingSelections.remove(feishuOpenId);
    }

    @Override
    public void clear(String feishuOpenId) {
        pendingSelections.remove(feishuOpenId);
    }
}

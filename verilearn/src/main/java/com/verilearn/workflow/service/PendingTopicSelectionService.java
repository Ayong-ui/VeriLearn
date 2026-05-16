package com.verilearn.workflow.service;

import com.verilearn.workflow.dto.TopicOptionSelection;

public interface PendingTopicSelectionService {

    void save(String feishuOpenId, TopicOptionSelection selection);

    TopicOptionSelection get(String feishuOpenId);

    TopicOptionSelection consume(String feishuOpenId);

    void clear(String feishuOpenId);
}

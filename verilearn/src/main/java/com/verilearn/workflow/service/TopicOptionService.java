package com.verilearn.workflow.service;

import java.util.List;

public interface TopicOptionService {

    List<String> generateOptions(Long userId, String broadTopic);
}

package com.verilearn.ai.service;

import com.verilearn.ai.dto.AiValidationItemDraft;

import java.util.List;

public interface AiValidationService {

    List<AiValidationItemDraft> generateValidationItems(Long userId, String topic, String chapterTitle, String nodeName, String stepType);
}

package com.verilearn.ai.service;

import com.verilearn.ai.dto.AiChapterMaterialResult;

public interface AiMaterialService {

    AiChapterMaterialResult generateChapterMaterials(Long userId, String topic, String chapterTitle, String currentStepType);
}

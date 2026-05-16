package com.verilearn.workflow.service;

import com.verilearn.workflow.dto.LearningRouteContentResponse;
import com.verilearn.workflow.dto.LearningRoutePlan;

import java.util.List;

public interface LearningRouteService {

    LearningRoutePlan generateLearningRoute(Long userId, String topic, String targetLevel);

    List<String> generateTopicOptions(Long userId, String topic);

    String createOrUpdateRouteFile(String topic, String markdownContent);

    String readRouteContent(String relativeFilePath);

    String buildRouteRelativePath(String topic);

    String resolveAbsolutePath(String relativeFilePath);

    void deleteRouteDirectory(String topic);

    String extractDemoAnswerSections(String markdownContent);

    String ensureDemoAnswerTemplate(String markdownContent);

    LearningRouteContentResponse buildRouteContentResponse(String topic, String contentUrl, String viewUrl);
}

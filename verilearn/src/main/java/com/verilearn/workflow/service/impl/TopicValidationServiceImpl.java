package com.verilearn.workflow.service.impl;

import com.verilearn.workflow.dto.TopicAnalysisResult;
import com.verilearn.workflow.service.TopicValidationService;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class TopicValidationServiceImpl implements TopicValidationService {

    private static final Set<String> REJECT_TOPICS = Set.of(
            "技术", "开发", "学习", "提升能力",
            "technology", "development", "study", "skill", "skills"
    );

    private static final Set<String> REQUIRE_OPTIONS_TOPICS = Set.of(
            "数学", "英语", "数据库",
            "math", "english", "database", "databases"
    );

    private static final String REJECTION_MESSAGE = """
            当前学习主题过于宽泛，请输入一个更具体的学科或技术主题。
            例如：
            - Linux
            - MySQL
            - Spring Boot Controller
            - 概率统计入门
            """.trim();

    private static final String OPTIONS_MESSAGE = "当前主题范围较大，请先从系统给出的子方向中选择一个更具体的学习主题。";

    @Override
    public TopicAnalysisResult analyzeTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic cannot be blank");
        }

        String normalized = normalize(topic);
        TopicAnalysisResult result = new TopicAnalysisResult();
        result.setNormalizedTopic(normalized);

        if (REJECT_TOPICS.contains(normalized)) {
            result.setKind(TopicAnalysisResult.TopicKind.REJECT);
            result.setMessage(REJECTION_MESSAGE);
            return result;
        }

        if (REQUIRE_OPTIONS_TOPICS.contains(normalized)) {
            result.setKind(TopicAnalysisResult.TopicKind.REQUIRE_OPTIONS);
            result.setMessage(OPTIONS_MESSAGE);
            return result;
        }

        result.setKind(TopicAnalysisResult.TopicKind.ACCEPT);
        return result;
    }

    @Override
    public void validateTopicOrThrow(String topic) {
        TopicAnalysisResult result = analyzeTopic(topic);
        if (result.getKind() == TopicAnalysisResult.TopicKind.ACCEPT) {
            return;
        }
        throw new IllegalArgumentException(result.getMessage());
    }

    private String normalize(String topic) {
        return topic.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}

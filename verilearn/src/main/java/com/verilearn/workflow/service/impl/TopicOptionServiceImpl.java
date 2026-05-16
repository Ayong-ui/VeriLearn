package com.verilearn.workflow.service.impl;

import com.verilearn.ai.service.AiRoutingService;
import com.verilearn.workflow.service.TopicOptionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TopicOptionServiceImpl implements TopicOptionService {

    private static final Map<String, List<String>> FALLBACK_OPTIONS = new LinkedHashMap<>();

    static {
        FALLBACK_OPTIONS.put("数学", List.of("高等数学", "线性代数", "概率统计", "程序员常用数学基础"));
        FALLBACK_OPTIONS.put("英语", List.of("技术英语阅读", "英语语法基础", "口语表达基础", "面试英语"));
        FALLBACK_OPTIONS.put("数据库", List.of("MySQL 基础", "数据库设计基础", "SQL 查询进阶", "事务与索引"));
    }

    private final AiRoutingService aiRoutingService;

    public TopicOptionServiceImpl(AiRoutingService aiRoutingService) {
        this.aiRoutingService = aiRoutingService;
    }

    @Override
    public List<String> generateOptions(Long userId, String broadTopic) {
        String content = aiRoutingService.chatForUser(
                userId,
                "You suggest 4 concise Chinese subtopics for one broad learning domain.",
                """
                        请为一个跨度较大的学习主题生成 4 个更具体的子方向。
                        Topic: %s

                        输出格式：
                        [OPTIONS]
                        1. 方向A
                        2. 方向B
                        3. 方向C
                        4. 方向D

                        要求：
                        1. 必须是中文
                        2. 每个方向都应该足够具体，适合作为正式学习主题
                        3. 不要输出解释，不要输出 FAQ
                        """.formatted(broadTopic)
        );

        List<String> parsed = parseOptions(content);
        if (!parsed.isEmpty()) {
            return parsed;
        }
        return FALLBACK_OPTIONS.getOrDefault(normalize(broadTopic), List.of(
                broadTopic + " 基础概念",
                broadTopic + " 核心方法",
                broadTopic + " 入门应用",
                broadTopic + " 综合实践"
        ));
    }

    private List<String> parseOptions(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        int markerIndex = content.indexOf("[OPTIONS]");
        String body = markerIndex >= 0 ? content.substring(markerIndex + "[OPTIONS]".length()) : content;
        String[] lines = body.split("\\R");
        List<String> options = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            String normalized = trimmed.replaceFirst("^[-*]\\s*", "")
                    .replaceFirst("^\\d+[.)、]\\s*", "")
                    .trim();
            if (!normalized.isBlank()) {
                options.add(normalized);
            }
            if (options.size() >= 5) {
                break;
            }
        }
        return options.size() >= 3 ? options : List.of();
    }

    private String normalize(String topic) {
        return topic == null ? "" : topic.trim().toLowerCase(Locale.ROOT);
    }
}

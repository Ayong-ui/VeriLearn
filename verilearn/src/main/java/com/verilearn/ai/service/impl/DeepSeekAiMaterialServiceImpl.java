package com.verilearn.ai.service.impl;

import com.verilearn.ai.dto.AiChapterMaterialResult;
import com.verilearn.ai.exception.AiGenerationException;
import com.verilearn.ai.service.AiMaterialService;
import com.verilearn.ai.service.AiRoutingService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class DeepSeekAiMaterialServiceImpl implements AiMaterialService {

    private static final String PROVIDER = "deepseek";
    private static final Set<String> STOP_WORDS = Set.of(
            "核心", "概念", "基础", "入门", "练习", "综合", "应用",
            "fundamentals", "basics", "example", "examples", "demo", "guide",
            "theory", "chapter", "read", "step", "scenario", "setup"
    );
    private static final Set<String> BAD_PATTERNS = Set.of(
            "faq", "常见问题", "markdown 简明指南", "200个常见问题", "请查收为您生成"
    );

    private final AiRoutingService aiRoutingService;

    public DeepSeekAiMaterialServiceImpl(AiRoutingService aiRoutingService) {
        this.aiRoutingService = aiRoutingService;
    }

    @Override
    public AiChapterMaterialResult generateChapterMaterials(
            Long userId,
            String topic,
            String chapterTitle,
            String currentStepType
    ) {
        String content = aiRoutingService.chatForUser(
                userId,
                "You generate concise Chinese self-study materials for one clearly scoped learning topic.",
                buildPrompt(topic, chapterTitle, currentStepType)
        );
        if (content == null || content.isBlank()) {
            throw new AiGenerationException("学习材料生成失败，请稍后重试或检查 AI 配置。");
        }
        return parseGeneratedContent(content, topic, chapterTitle, currentStepType);
    }

    private String buildPrompt(String topic, String chapterTitle, String currentStepType) {
        return """
                你要为“明确且具体”的学习主题生成中文自学材料。
                Topic: %s
                Chapter: %s
                Current step: %s

                Hard requirements:
                1. Topic 和 Chapter 必须强相关，不能偷换成其他学科或其他技术主题。
                2. 不要输出 FAQ、教程总览、学习建议汇总。
                3. 输出内容必须适合作为 theory.md 和 demo-task.md。
                4. Demo 必须是 Markdown 任务单，不要生成额外源代码文件。

                Output format:
                [SUMMARY]
                用中文写一段 80 字以内的摘要。

                [THEORY]
                用中文覆盖：
                1. 这个知识点是什么
                2. 它适用于什么场景
                3. 一个最小示例或最小步骤
                4. 为什么这样设计 / 工作原理
                5. 一个常见误区

                [DEMO]
                用中文提供 Markdown 任务单：
                1. 练习目标
                2. 练习步骤
                3. 预期结果
                4. 学完后需要回答的问题
                """.formatted(
                defaultText(topic),
                defaultText(chapterTitle),
                defaultText(currentStepType, "READ_THEORY")
        );
    }

    private AiChapterMaterialResult parseGeneratedContent(
            String content,
            String topic,
            String chapterTitle,
            String currentStepType
    ) {
        String summary = extractSection(content, "[SUMMARY]", "[THEORY]");
        String theory = extractSection(content, "[THEORY]", "[DEMO]");
        String demo = extractSection(content, "[DEMO]", null);

        if (summary == null || summary.isBlank() || theory == null || theory.isBlank() || demo == null || demo.isBlank()) {
            throw new AiGenerationException("学习材料生成结果缺少必要结构，请稍后重试。");
        }
        if (!isUsableMaterialContent(summary, theory, demo, topic, chapterTitle)) {
            throw new AiGenerationException("学习材料生成结果不符合要求，系统已拒绝写入。");
        }

        AiChapterMaterialResult result = new AiChapterMaterialResult();
        result.setSummary(summary.trim());
        result.setTheoryContent(theory.trim());
        result.setDemoGuideContent(demo.trim());
        result.setGeneratedByAi(true);
        result.setProvider(PROVIDER);
        return result;
    }

    private String extractSection(String content, String startMarker, String endMarker) {
        int startIndex = content.indexOf(startMarker);
        if (startIndex < 0) {
            return null;
        }
        startIndex += startMarker.length();
        int endIndex = endMarker == null ? content.length() : content.indexOf(endMarker, startIndex);
        if (endIndex < 0) {
            endIndex = content.length();
        }
        return content.substring(startIndex, endIndex).trim();
    }

    private boolean isUsableMaterialContent(String summary, String theory, String demo, String topic, String chapterTitle) {
        String combined = (summary + "\n" + theory + "\n" + demo).toLowerCase(Locale.ROOT);
        for (String badPattern : BAD_PATTERNS) {
            if (combined.contains(badPattern)) {
                return false;
            }
        }

        Set<String> keywords = buildKeywords(topic, chapterTitle);
        if (keywords.isEmpty()) {
            return true;
        }
        for (String keyword : keywords) {
            if (combined.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private Set<String> buildKeywords(String topic, String chapterTitle) {
        Set<String> keywords = new HashSet<>();
        addKeywords(keywords, topic);
        addKeywords(keywords, chapterTitle);
        return keywords;
    }

    private void addKeywords(Set<String> keywords, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        Arrays.stream(text.trim().split("[^\\p{IsHan}A-Za-z0-9]+"))
                .map(token -> token.toLowerCase(Locale.ROOT).trim())
                .filter(token -> token.length() >= 2)
                .filter(token -> !STOP_WORDS.contains(token))
                .forEach(keywords::add);
    }

    private String defaultText(String value) {
        return defaultText(value, "unknown");
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}

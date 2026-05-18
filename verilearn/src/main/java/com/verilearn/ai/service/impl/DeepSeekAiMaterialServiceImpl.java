package com.verilearn.ai.service.impl;

import com.verilearn.ai.dto.AiChapterMaterialResult;
import com.verilearn.ai.exception.AiGenerationException;
import com.verilearn.ai.service.AiMaterialService;
import com.verilearn.ai.service.AiRoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class DeepSeekAiMaterialServiceImpl implements AiMaterialService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekAiMaterialServiceImpl.class);

    private static final String PROVIDER = "deepseek";
    private static final double TEMPERATURE = 0.7;

    private static final Set<String> STOP_WORDS = Set.of(
            "核心", "概念", "基础", "入门", "练习", "综合", "应用",
            "fundamentals", "basics", "example", "examples", "demo", "guide",
            "theory", "chapter", "read", "step", "scenario", "setup"
    );
    private static final Set<String> BAD_PATTERNS = Set.of(
            "faq", "常见问题", "markdown 简明指南", "200个常见问题", "请查收为您生成"
    );

    private static final String SYSTEM_PROMPT = """
            你是一位资深的中文技术课程设计师，专门为自学者编写高质量、结构化的学习材料。

            你的写作标准：
            1. 每个概念都附带一个最小可运行的代码示例或具体操作步骤，避免纯理论堆砌。
            2. 内容按「是什么 → 为什么 → 怎么用」的逻辑组织，层层递进。
            3. 用具体的场景和案例说明抽象概念，让读者读完就能动手实践。
            4. 语言简洁但信息密度高，适合有基本编程基础的学习者。
            5. 避免空泛的模板句式（如「本章主要介绍……」「通过学习本章你将掌握……」），直接进入正题。
            6. 对于常见误区，给出错误示例和正确示例的对比。
            7. 所有输出使用中文 Markdown，代码注释也用中文。
            8. 聚焦在当前章节范围内，不要扩展到无关主题。
            """;

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
                SYSTEM_PROMPT,
                buildPrompt(topic, chapterTitle, currentStepType),
                TEMPERATURE
        );
        if (content == null || content.isBlank()) {
            throw new AiGenerationException("学习材料生成失败，请稍后重试或检查 AI 配置。");
        }
        return parseGeneratedContent(content, topic, chapterTitle, currentStepType);
    }

    private String buildPrompt(String topic, String chapterTitle, String currentStepType) {
        return """
                请为以下学习主题和章节生成自学材料。

                学习主题：%s
                当前章节：%s
                当前步骤：%s

                输出格式（严格按以下三个部分输出）：

                [SUMMARY]
                用 80 字以内概括本章要讲什么、学完能做什么。

                [THEORY]
                覆盖以下内容（每部分用小标题）：
                1. 这个知识点解决什么问题（场景引入）
                2. 核心概念和原理（用类比或图解思维描述）
                3. 最小可用示例（代码或具体步骤）
                4. 工作原理简述
                5. 常见误区和正确做法对比

                [DEMO]
                一份 Markdown 任务单：
                1. 练习目标（一句话说清要达成什么）
                2. 练习步骤（具体的、可直接执行的操作）
                3. 预期结果（做完后应该看到什么）
                4. 学完后需要回答的问题（2-3 个理解检查题）
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
        if (hasBadPatterns(summary, theory, demo)) {
            log.warn("ai generated material contains bad patterns, retrying: topic={}, chapter={}", topic, chapterTitle);
            throw new AiGenerationException("学习材料生成结果不符合要求，系统已拒绝写入。");
        }
        if (!isOnTopic(summary, theory, demo, topic, chapterTitle)) {
            log.warn("ai generated material appears off-topic, accepting with warning: topic={}, chapter={}", topic, chapterTitle);
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

    private boolean hasBadPatterns(String summary, String theory, String demo) {
        String combined = (summary + "\n" + theory + "\n" + demo).toLowerCase(Locale.ROOT);
        for (String badPattern : BAD_PATTERNS) {
            if (combined.contains(badPattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOnTopic(String summary, String theory, String demo, String topic, String chapterTitle) {
        String combined = (summary + "\n" + theory + "\n" + demo).toLowerCase(Locale.ROOT);
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

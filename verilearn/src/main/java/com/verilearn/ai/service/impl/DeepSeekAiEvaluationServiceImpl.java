package com.verilearn.ai.service.impl;

import com.verilearn.ai.dto.AiDemoEvaluationResult;
import com.verilearn.ai.exception.AiGenerationException;
import com.verilearn.ai.service.AiEvaluationService;
import com.verilearn.ai.service.AiRoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class DeepSeekAiEvaluationServiceImpl implements AiEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekAiEvaluationServiceImpl.class);

    private static final String PROVIDER = "deepseek";
    private static final double TEMPERATURE = 0.5;
    private static final Set<String> BAD_PATTERNS = Set.of("faq", "常见问题", "markdown 简明指南", "200个常见问题");

    private static final String SYSTEM_PROMPT = """
            你是一位耐心的中文编程导师，专门评估自学者的 Demo 练习提交。

            你的评估原则：
            1. 以鼓励为主，先肯定学习者做对的部分，再指出可以改进的地方。
            2. 评估标准要与章节主题和 Demo 指南对齐，不随意拔高要求。
            3. 指出不足时要给出具体的改进方向，而不是笼统的「需要加强」。
            4. 理解程度判定标准：
               - HIGH：能准确解释核心概念，Demo 结果正确，能举一反三
               - MEDIUM：基本理解正确但某些细节不够清晰，或 Demo 有小偏差
               - LOW：核心概念理解错误，Demo 结果明显不对
            5. 所有输出使用中文。
            """;

    private final AiRoutingService aiRoutingService;

    public DeepSeekAiEvaluationServiceImpl(AiRoutingService aiRoutingService) {
        this.aiRoutingService = aiRoutingService;
    }

    @Override
    public AiDemoEvaluationResult evaluateDemoSubmission(
            Long userId,
            String topic,
            String chapterTitle,
            String demoGuide,
            String submissionSummary,
            String codeSnippet,
            String question
    ) {
        String content = aiRoutingService.chatForUser(
                userId,
                SYSTEM_PROMPT,
                buildPrompt(topic, chapterTitle, demoGuide, submissionSummary, codeSnippet, question),
                TEMPERATURE
        );
        if (content == null || content.isBlank()) {
            throw new AiGenerationException("学习评估生成失败，请稍后重试或检查 AI 配置。");
        }
        return parseContent(content, chapterTitle, submissionSummary, question);
    }

    private String buildPrompt(
            String topic,
            String chapterTitle,
            String demoGuide,
            String submissionSummary,
            String codeSnippet,
            String question
    ) {
        return """
                请评估以下自学者的 Demo 提交。

                学习主题：%s
                章节标题：%s

                Demo 指南：
                %s

                学习者完成记录：
                %s

                学习者代码片段：
                %s

                学习者提出的问题：
                %s

                请严格按以下三个部分输出：

                [LEVEL]
                从 HIGH / MEDIUM / LOW 中选择一个。

                [EVALUATION]
                用中文 Markdown 说明：
                1. 完成情况（Demo 任务是否完成，预期结果是否符合）
                2. 理解正确的部分（具体指出哪些概念掌握得好）
                3. 可以改进的地方（指出具体问题，给出改进建议）
                4. 是否建议复习本章

                [NEXT_STEP]
                用中文给出下一步行动建议：
                - 如果掌握良好：建议进入下一章，并给出一句话预告
                - 如果部分掌握：给出一个小的补充练习
                - 如果掌握不足：建议重新学习本章的哪个具体部分
                """.formatted(
                defaultText(topic),
                defaultText(chapterTitle),
                defaultText(demoGuide),
                defaultText(submissionSummary),
                defaultText(codeSnippet),
                defaultText(question)
        );
    }

    private AiDemoEvaluationResult parseContent(String content, String chapterTitle, String submissionSummary, String question) {
        String level = extractSection(content, "[LEVEL]", "[EVALUATION]");
        String evaluation = extractSection(content, "[EVALUATION]", "[NEXT_STEP]");
        String nextStep = extractSection(content, "[NEXT_STEP]", null);
        if (evaluation == null || evaluation.isBlank() || nextStep == null || nextStep.isBlank()) {
            throw new AiGenerationException("学习评估结果缺少必要结构，请稍后重试。");
        }
        if (hasBadPatterns(level, evaluation, nextStep)) {
            log.warn("ai generated evaluation contains bad patterns, retrying");
            throw new AiGenerationException("学习评估结果不符合要求，系统已拒绝写入。");
        }

        AiDemoEvaluationResult result = new AiDemoEvaluationResult();
        result.setUnderstandingLevel(normalizeLevel(level));
        result.setEvaluationMarkdown(evaluation.trim());
        result.setNextStepMarkdown(nextStep.trim());
        result.setShouldReview("LOW".equals(result.getUnderstandingLevel()));
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

    private String normalizeLevel(String level) {
        if (level == null || level.isBlank()) {
            return "MEDIUM";
        }
        String normalized = level.trim().toUpperCase();
        return switch (normalized) {
            case "HIGH", "MEDIUM", "LOW" -> normalized;
            default -> "MEDIUM";
        };
    }

    private boolean hasBadPatterns(String level, String evaluation, String nextStep) {
        String normalizedLevel = normalizeLevel(level);
        String combined = (normalizedLevel + "\n" + evaluation + "\n" + nextStep).toLowerCase(Locale.ROOT);
        for (String badPattern : BAD_PATTERNS) {
            if (combined.contains(badPattern)) {
                return true;
            }
        }
        return !("HIGH".equals(normalizedLevel) || "MEDIUM".equals(normalizedLevel) || "LOW".equals(normalizedLevel));
    }

    private String defaultText(String value) {
        return value == null || value.isBlank() ? "（未提供）" : value.trim();
    }
}

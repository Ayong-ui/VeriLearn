package com.verilearn.ai.service.impl;

import com.verilearn.ai.dto.AiDemoEvaluationResult;
import com.verilearn.ai.exception.AiGenerationException;
import com.verilearn.ai.service.AiEvaluationService;
import com.verilearn.ai.service.AiRoutingService;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class DeepSeekAiEvaluationServiceImpl implements AiEvaluationService {

    private static final String PROVIDER = "deepseek";
    private static final Set<String> BAD_PATTERNS = Set.of("faq", "常见问题", "markdown 简明指南", "200个常见问题");

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
                "You evaluate Chinese self-study demo submissions and return structured results.",
                buildPrompt(topic, chapterTitle, demoGuide, submissionSummary, codeSnippet, question)
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
                请评估一次中文自学任务提交，并且严格按指定结构输出。
                Topic: %s
                Chapter: %s
                Demo guide:
                %s

                Learner summary:
                %s

                Learner code snippet:
                %s

                Learner question:
                %s

                Output format:
                Hard requirements:
                1. 必须与 Topic / Chapter / Demo guide 相关，不能跑题。
                2. 不要输出 FAQ、教程总览或无关知识。
                3. 只能输出以下三个部分。

                [LEVEL]
                One of HIGH / MEDIUM / LOW
                
                [EVALUATION]
                用中文 Markdown 说明：
                1. Completion status
                2. What the learner understands
                3. Weak points
                4. Whether review is suggested

                [NEXT_STEP]
                用中文 Markdown 给出下一步建议：
                - continue
                - smaller follow-up practice
                - review this chapter first
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
        if (!isUsableEvaluationContent(level, evaluation, nextStep)) {
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

    private boolean isUsableEvaluationContent(String level, String evaluation, String nextStep) {
        String normalizedLevel = normalizeLevel(level);
        String combined = (normalizedLevel + "\n" + evaluation + "\n" + nextStep).toLowerCase(Locale.ROOT);
        for (String badPattern : BAD_PATTERNS) {
            if (combined.contains(badPattern)) {
                return false;
            }
        }
        return "HIGH".equals(normalizedLevel) || "MEDIUM".equals(normalizedLevel) || "LOW".equals(normalizedLevel);
    }

    private String defaultText(String value) {
        return value == null || value.isBlank() ? "none" : value.trim();
    }
}

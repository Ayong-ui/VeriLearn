package com.verilearn.ai.service.impl;

import com.verilearn.ai.dto.AiDemoEvaluationResult;
import com.verilearn.ai.service.AiEvaluationService;
import com.verilearn.ai.service.DeepSeekChatClient;
import org.springframework.stereotype.Service;

@Service
public class DeepSeekAiEvaluationServiceImpl implements AiEvaluationService {

    private static final String PROVIDER = "deepseek";

    private final DeepSeekChatClient deepSeekChatClient;

    public DeepSeekAiEvaluationServiceImpl(DeepSeekChatClient deepSeekChatClient) {
        this.deepSeekChatClient = deepSeekChatClient;
    }

    @Override
    public AiDemoEvaluationResult evaluateDemoSubmission(
            String topic,
            String chapterTitle,
            String demoGuide,
            String submissionSummary,
            String codeSnippet,
            String question
    ) {
        String prompt = buildPrompt(topic, chapterTitle, demoGuide, submissionSummary, codeSnippet, question);
        String content = deepSeekChatClient.chat(
                "You are a Java backend mentor. Evaluate demo submissions in Chinese and return structured sections.",
                prompt
        );
        if (content == null || content.isBlank()) {
            return fallback(chapterTitle, submissionSummary, question);
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
                请基于下面的 Java 后端学习场景，评估用户对 Demo 的掌握情况，并严格按固定分段输出。

                学习主题：%s
                当前章节：%s
                Demo 指南：
                %s

                用户提交说明：
                %s

                用户代码片段：
                %s

                用户问题：
                %s

                输出格式必须严格如下：
                [LEVEL]
                只输出一个：HIGH / MEDIUM / LOW

                [EVALUATION]
                用中文输出 Markdown，包含：
                1. 本次完成情况
                2. 用户已经掌握了什么
                3. 还存在哪些薄弱点
                4. 是否建议复习

                [NEXT_STEP]
                用中文输出 Markdown，告诉用户下一步应该做什么：
                - 继续下一步
                - 先补一个更小练习
                - 先复习本章
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
            return fallback(chapterTitle, submissionSummary, question);
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

    private AiDemoEvaluationResult fallback(String chapterTitle, String submissionSummary, String question) {
        AiDemoEvaluationResult result = new AiDemoEvaluationResult();
        result.setUnderstandingLevel("MEDIUM");
        result.setEvaluationMarkdown("""
                # Demo 评估报告

                - 当前章节：%s
                - 用户提交说明：%s
                - 用户问题：%s

                ## 评估结论
                你已经完成了一轮 Demo 提交，但目前仍建议你继续补充自己的理解说明，并把关键代码路径解释清楚。

                ## 当前掌握情况
                - 已经进入了实操阶段
                - 已经能描述本次 Demo 的完成结果

                ## 仍需加强
                - 需要更明确地说清楚为什么这样实现
                - 需要结合项目代码解释关键类或关键步骤

                ## 是否建议复习
                当前先不直接回退复习，建议先看下一步建议并补一轮更具体的说明。
                """.formatted(defaultText(chapterTitle), defaultText(submissionSummary), defaultText(question)));
        result.setNextStepMarkdown("""
                # 下一步建议

                1. 回到当前章节的 Demo 指南，逐条对照自己是否都完成。
                2. 用自己的话回答：这个 Demo 解决了什么问题。
                3. 找到一段最关键的代码，说明它为什么放在这里。
                4. 完成后再提交一轮反馈，或继续进入章节的下一步。
                """);
        result.setShouldReview(false);
        result.setGeneratedByAi(false);
        result.setProvider("template");
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

    private String defaultText(String value) {
        return value == null || value.isBlank() ? "无" : value.trim();
    }
}

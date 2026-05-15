package com.verilearn.ai.service.impl;

import com.verilearn.ai.dto.AiDemoEvaluationResult;
import com.verilearn.ai.service.AiEvaluationService;
import com.verilearn.ai.service.AiRoutingService;
import org.springframework.stereotype.Service;

@Service
public class DeepSeekAiEvaluationServiceImpl implements AiEvaluationService {

    private static final String PROVIDER = "deepseek";

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
                "You evaluate Java backend self-study demo submissions.",
                buildPrompt(topic, chapterTitle, demoGuide, submissionSummary, codeSnippet, question)
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
                Evaluate a Java backend self-study demo submission and answer in Chinese.
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
                [LEVEL]
                One of HIGH / MEDIUM / LOW

                [EVALUATION]
                Markdown in Chinese:
                1. Completion status
                2. What the learner understands
                3. Weak points
                4. Whether review is suggested

                [NEXT_STEP]
                Markdown in Chinese:
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
                # Demo Evaluation

                - Chapter: %s
                - Learner summary: %s
                - Learner question: %s

                ## Conclusion
                One round of demo feedback has been submitted. A clearer explanation of the key code path is still recommended.

                ## Current understanding
                - The learner has entered the hands-on stage.
                - The learner can already describe the demo result.

                ## Weak points
                - The reason behind the implementation is not fully clear yet.
                - The learner should connect the explanation to actual project code.
                """.formatted(defaultText(chapterTitle), defaultText(submissionSummary), defaultText(question)));
        result.setNextStepMarkdown("""
                # Next Step

                1. Re-check the current demo guide item by item.
                2. Explain what problem this demo solves.
                3. Point out the most important code section and explain why it is there.
                4. Submit one more round of feedback or move to the next chapter step.
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
        return value == null || value.isBlank() ? "none" : value.trim();
    }
}

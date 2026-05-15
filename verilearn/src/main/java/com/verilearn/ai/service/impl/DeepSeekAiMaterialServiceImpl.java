package com.verilearn.ai.service.impl;

import com.verilearn.ai.dto.AiChapterMaterialResult;
import com.verilearn.ai.service.AiMaterialService;
import com.verilearn.ai.service.AiRoutingService;
import org.springframework.stereotype.Service;

@Service
public class DeepSeekAiMaterialServiceImpl implements AiMaterialService {

    private static final String PROVIDER = "deepseek";

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
                "You generate concise study materials for Java backend self-learning.",
                buildPrompt(topic, chapterTitle, currentStepType)
        );
        if (content == null || content.isBlank()) {
            return fallback(topic, chapterTitle, currentStepType);
        }
        return parseGeneratedContent(content, topic, chapterTitle, currentStepType);
    }

    private String buildPrompt(String topic, String chapterTitle, String currentStepType) {
        return """
                Generate Chinese self-study material for one Java backend chapter.
                Topic: %s
                Chapter: %s
                Current step: %s

                Output format:
                [SUMMARY]
                One short summary within 80 Chinese characters.

                [THEORY]
                Explain:
                1. What this concept is
                2. Why it matters
                3. One minimal example
                4. One common mistake

                [DEMO]
                Provide:
                1. Demo goal
                2. Demo steps
                3. Expected result
                4. Questions for the learner after finishing
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

        if (theory == null || theory.isBlank() || demo == null || demo.isBlank()) {
            return fallback(topic, chapterTitle, currentStepType);
        }

        AiChapterMaterialResult result = new AiChapterMaterialResult();
        result.setSummary(summary == null || summary.isBlank()
                ? "Complete theory study and a minimal demo for " + chapterTitle + "."
                : summary.trim());
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

    private AiChapterMaterialResult fallback(
            String topic,
            String chapterTitle,
            String currentStepType
    ) {
        AiChapterMaterialResult result = new AiChapterMaterialResult();
        result.setSummary("Study theory, finish the demo, and submit feedback for " + chapterTitle + ".");
        result.setTheoryContent("""
                # %s

                ## What it is
                %s is an important chapter in the %s learning path.

                ## Why it matters
                Understanding this chapter helps connect later API design, task flow, validation, and project code.

                ## Minimal example
                Explain the core role of %s in your own words, then find one related place in the current project.

                ## Common mistake
                Do not memorize the concept only. Connect it to the actual VeriLearn code.
                """.formatted(
                chapterTitle,
                chapterTitle,
                defaultText(topic, "current topic"),
                chapterTitle
        ));
        result.setDemoGuideContent("""
                # Demo Guide

                ## Goal
                Finish one minimal practice task related to %s.

                ## Steps
                1. Read the theory.
                2. Find related code, API, or configuration in this project.
                3. Run it once or verify the current step manually.

                ## Expected result
                You should be able to explain what problem this chapter solves and where it appears in the project.

                ## Questions after finishing
                1. What is the core idea of this chapter?
                2. Why does it appear in the current step: %s?
                3. Which code section shows its real usage?
                """.formatted(
                chapterTitle,
                defaultText(currentStepType, "READ_THEORY")
        ));
        result.setGeneratedByAi(false);
        result.setProvider("template");
        return result;
    }

    private String defaultText(String value) {
        return defaultText(value, "unknown");
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}

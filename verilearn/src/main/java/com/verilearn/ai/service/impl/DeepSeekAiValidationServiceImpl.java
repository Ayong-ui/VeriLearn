package com.verilearn.ai.service.impl;

import com.verilearn.ai.dto.AiValidationItemDraft;
import com.verilearn.ai.service.AiRoutingService;
import com.verilearn.ai.service.AiValidationService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeepSeekAiValidationServiceImpl implements AiValidationService {

    private static final double TEMPERATURE = 0.5;

    private static final String SYSTEM_PROMPT = """
            你是一位中文技术自学的考核设计者，专门为学习章节生成简短的验证题目。

            你的出题原则：
            1. 题目必须紧贴当前章节的知识点，考察理解而非记忆。
            2. 每道题要求学习者用自己的话解释或举例，而不是选择/填空。
            3. 题目语言清晰直接，不使用「请简述」「请说明」等冗余开头。
            4. 答案要点给出核心判断标准，不是标准答案。
            """;

    private final AiRoutingService aiRoutingService;

    public DeepSeekAiValidationServiceImpl(AiRoutingService aiRoutingService) {
        this.aiRoutingService = aiRoutingService;
    }

    @Override
    public List<AiValidationItemDraft> generateValidationItems(Long userId, String topic, String chapterTitle, String nodeName, String stepType) {
        String content = aiRoutingService.chatForUser(
                userId,
                SYSTEM_PROMPT,
                buildPrompt(topic, chapterTitle, nodeName, stepType),
                TEMPERATURE
        );
        if (content == null || content.isBlank()) {
            return fallback(nodeName, stepType);
        }

        List<AiValidationItemDraft> parsed = parse(content, stepType);
        return parsed.isEmpty() ? fallback(nodeName, stepType) : parsed;
    }

    private String buildPrompt(String topic, String chapterTitle, String nodeName, String stepType) {
        return """
                为以下自学章节生成 2 个中文验证题目。

                学习主题：%s
                章节：%s
                知识点：%s
                学习步骤类型：%s

                输出格式（严格按照以下模板，每个题目以 [ITEM] 开头）：

                [ITEM]
                type=题目类型
                difficulty=难度
                question=题目文字（要求用自己的话解释或举例）
                answer=判断要点（列出回答应该覆盖的关键点）

                [ITEM]
                type=题目类型
                difficulty=难度
                question=题目文字
                answer=判断要点
                """.formatted(
                topic,
                chapterTitle,
                nodeName,
                stepType == null ? "READ_THEORY" : stepType
        );
    }

    private List<AiValidationItemDraft> parse(String content, String stepType) {
        String[] sections = content.split("\\[ITEM\\]");
        List<AiValidationItemDraft> items = new ArrayList<>();
        for (String section : sections) {
            String trimmed = section.trim();
            if (trimmed.isBlank()) {
                continue;
            }

            AiValidationItemDraft item = new AiValidationItemDraft();
            for (String line : trimmed.split("\\R")) {
                String current = line.trim();
                if (current.startsWith("type=")) {
                    item.setItemType(current.substring("type=".length()).trim());
                } else if (current.startsWith("difficulty=")) {
                    item.setDifficultyLevel(current.substring("difficulty=".length()).trim());
                } else if (current.startsWith("question=")) {
                    item.setQuestionText(current.substring("question=".length()).trim());
                } else if (current.startsWith("answer=")) {
                    item.setAnswerKey(current.substring("answer=".length()).trim());
                }
            }

            if (item.getQuestionText() != null && !item.getQuestionText().isBlank()) {
                if (item.getItemType() == null || item.getItemType().isBlank()) {
                    item.setItemType(defaultItemType(stepType, items.size()));
                }
                if (item.getDifficultyLevel() == null || item.getDifficultyLevel().isBlank()) {
                    item.setDifficultyLevel("BASIC");
                }
                if (item.getAnswerKey() == null || item.getAnswerKey().isBlank()) {
                    item.setAnswerKey("Explain the key point clearly.");
                }
                items.add(item);
            }
        }
        return items;
    }

    private List<AiValidationItemDraft> fallback(String nodeName, String stepType) {
        List<AiValidationItemDraft> items = new ArrayList<>();
        items.add(buildFallbackItem(
                defaultItemType(stepType, 0),
                "BASIC",
                "Explain the core role of " + nodeName + " in your own words.",
                "Should explain what problem it solves and why it is needed."
        ));
        items.add(buildFallbackItem(
                defaultItemType(stepType, 1),
                "BASIC",
                fallbackSecondQuestion(nodeName, stepType),
                "Should provide one concrete example, observation, or summary."
        ));
        return items;
    }

    private String fallbackSecondQuestion(String nodeName, String stepType) {
        if ("RUN_DEMO".equals(stepType)) {
            return "Describe one concrete demo behavior related to " + nodeName + " and explain why it happened.";
        }
        if ("SUBMIT_FEEDBACK".equals(stepType)) {
            return "Summarize which part of " + nodeName + " still feels weak and how you will review it.";
        }
        return "Give one practical usage scenario for " + nodeName + ".";
    }

    private String defaultItemType(String stepType, int index) {
        if ("RUN_DEMO".equals(stepType)) {
            return index == 0 ? "DEMO_RESULT" : "DEMO_ANALYSIS";
        }
        if ("SUBMIT_FEEDBACK".equals(stepType)) {
            return index == 0 ? "SUMMARY" : "REFLECTION";
        }
        return index == 0 ? "CONCEPT" : "APPLICATION";
    }

    private AiValidationItemDraft buildFallbackItem(String itemType, String difficulty, String question, String answer) {
        AiValidationItemDraft item = new AiValidationItemDraft();
        item.setItemType(itemType);
        item.setDifficultyLevel(difficulty);
        item.setQuestionText(question);
        item.setAnswerKey(answer);
        return item;
    }
}

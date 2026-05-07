package com.verilearn.ai.service.impl;

import com.verilearn.ai.dto.AiValidationItemDraft;
import com.verilearn.ai.service.AiValidationService;
import com.verilearn.ai.service.DeepSeekChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeepSeekAiValidationServiceImpl implements AiValidationService {

    private final DeepSeekChatClient deepSeekChatClient;

    public DeepSeekAiValidationServiceImpl(DeepSeekChatClient deepSeekChatClient) {
        this.deepSeekChatClient = deepSeekChatClient;
    }

    @Override
    public List<AiValidationItemDraft> generateValidationItems(String topic, String chapterTitle, String nodeName, String stepType) {
        String content = deepSeekChatClient.chat(
                "You are a Java backend learning coach. Output concise Chinese validation items.",
                buildPrompt(topic, chapterTitle, nodeName, stepType)
        );
        if (content == null || content.isBlank()) {
            return fallback(nodeName, stepType);
        }

        List<AiValidationItemDraft> parsed = parse(content, nodeName, stepType);
        return parsed.isEmpty() ? fallback(nodeName, stepType) : parsed;
    }

    private String buildPrompt(String topic, String chapterTitle, String nodeName, String stepType) {
        return """
                请为一个 Java 后端学习章节生成 2 道中文验证题。

                学习主题：%s
                当前章节：%s
                知识点：%s
                当前步骤：%s

                你必须严格按下面格式输出：
                [ITEM]
                type=题目类型
                difficulty=难度
                question=题目内容
                answer=答案要点
                [ITEM]
                type=题目类型
                difficulty=难度
                question=题目内容
                answer=答案要点
                """.formatted(topic, chapterTitle, nodeName, stepType == null ? "READ_THEORY" : stepType);
    }

    private List<AiValidationItemDraft> parse(String content, String nodeName, String stepType) {
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
                    item.setAnswerKey("请围绕 " + nodeName + " 给出关键解释。");
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
                "请用自己的话解释 " + nodeName + " 的核心作用。",
                "应说明 " + nodeName + " 解决了什么问题，以及为什么需要它。"
        ));
        items.add(buildFallbackItem(
                defaultItemType(stepType, 1),
                "BASIC",
                fallbackSecondQuestion(nodeName, stepType),
                "应结合当前章节给出具体例子、现象或总结。"
        ));
        return items;
    }

    private String fallbackSecondQuestion(String nodeName, String stepType) {
        if ("RUN_DEMO".equals(stepType)) {
            return "请结合 Demo 描述 " + nodeName + " 的一个实际现象，并说明它为什么会这样。";
        }
        if ("SUBMIT_FEEDBACK".equals(stepType)) {
            return "请总结 " + nodeName + " 中你最容易混淆的部分，并说明你准备如何复习。";
        }
        return "请给出 " + nodeName + " 的一个实际使用场景。";
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

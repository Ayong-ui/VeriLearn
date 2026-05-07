package com.verilearn.ai.service.impl;

import com.verilearn.ai.dto.AiChapterMaterialResult;
import com.verilearn.ai.service.AiMaterialService;
import com.verilearn.ai.service.DeepSeekChatClient;
import org.springframework.stereotype.Service;

@Service
public class DeepSeekAiMaterialServiceImpl implements AiMaterialService {

    private static final String PROVIDER = "deepseek";

    private final DeepSeekChatClient deepSeekChatClient;

    public DeepSeekAiMaterialServiceImpl(DeepSeekChatClient deepSeekChatClient) {
        this.deepSeekChatClient = deepSeekChatClient;
    }

    @Override
    public AiChapterMaterialResult generateChapterMaterials(String topic, String chapterTitle, String currentStepType) {
        String prompt = buildPrompt(topic, chapterTitle, currentStepType);
        String content = deepSeekChatClient.chat(
                "You are a Java backend learning coach. Output concise, structured Chinese learning materials.",
                prompt
        );
        if (content == null || content.isBlank()) {
            return fallback(topic, chapterTitle, currentStepType);
        }
        return parseGeneratedContent(content, topic, chapterTitle, currentStepType);
    }

    private String buildPrompt(String topic, String chapterTitle, String currentStepType) {
        return """
                请围绕下面的 Java 后端学习章节，生成一份可直接用于学习的中文章节材料。

                学习主题：%s
                当前章节：%s
                当前步骤：%s

                你必须严格按下面格式输出：
                [SUMMARY]
                用不超过 80 字总结这一章的学习重点。
                [THEORY]
                用中文讲清：
                1. 这个知识点是什么
                2. 为什么需要它
                3. 一个最小示例
                4. 一个常见误区
                [DEMO]
                用中文给出 Demo / 练习指南，包括：
                1. 练习目标
                2. 练习步骤
                3. 预期现象
                4. 完成后要回答的问题
                """.formatted(topic, chapterTitle, currentStepType == null ? "READ_THEORY" : currentStepType);
    }

    private AiChapterMaterialResult parseGeneratedContent(String content, String topic, String chapterTitle, String currentStepType) {
        String summary = extractSection(content, "[SUMMARY]", "[THEORY]");
        String theory = extractSection(content, "[THEORY]", "[DEMO]");
        String demo = extractSection(content, "[DEMO]", null);

        if (theory == null || theory.isBlank() || demo == null || demo.isBlank()) {
            return fallback(topic, chapterTitle, currentStepType);
        }

        AiChapterMaterialResult result = new AiChapterMaterialResult();
        result.setSummary(summary == null || summary.isBlank() ? "AI generated summary for " + chapterTitle : summary.trim());
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

    private AiChapterMaterialResult fallback(String topic, String chapterTitle, String currentStepType) {
        AiChapterMaterialResult result = new AiChapterMaterialResult();
        result.setSummary("围绕 " + chapterTitle + " 完成理论学习、Demo 练习和反馈总结。");
        result.setTheoryContent("""
                # %s

                1. 这是什么
                %s 是 %s 学习路径中的一个关键章节，重点是先理解核心概念和使用场景。

                2. 为什么需要它
                学会这个章节后，你会更容易把后续的接口设计、任务推进或验证逻辑串起来。

                3. 最小示例
                请先用你自己的话解释 %s 的核心作用，再结合当前项目找一个对应位置。

                4. 常见误区
                不要只背概念，要把它和当前 VeriLearn 项目里的真实代码联系起来。
                """.formatted(chapterTitle, chapterTitle, topic == null || topic.isBlank() ? "当前主题" : topic, chapterTitle));
        result.setDemoGuideContent("""
                # Demo / 练习指南

                1. 练习目标
                完成一条和 %s 对应的最小学习验证路径。

                2. 练习步骤
                - 先阅读理论内容
                - 找到项目里和这个章节相关的代码或接口
                - 运行一次，或者手动验证当前步骤

                3. 预期现象
                你应该能说清这个章节解决了什么问题，以及它在当前项目中的落点。

                4. 完成后要回答的问题
                - 这个章节的核心概念是什么？
                - 它为什么会出现在当前步骤：%s？
                - 你在哪段代码里看到了它的实际应用？
                """.formatted(chapterTitle, currentStepType == null ? "READ_THEORY" : currentStepType));
        result.setGeneratedByAi(false);
        result.setProvider("template");
        return result;
    }
}

package com.verilearn.workflow.service.impl;

import com.verilearn.ai.exception.AiGenerationException;
import com.verilearn.ai.service.AiRoutingService;
import com.verilearn.chapter.config.StorageProperties;
import com.verilearn.workflow.dto.LearningRouteChapter;
import com.verilearn.workflow.dto.LearningRouteContentResponse;
import com.verilearn.workflow.dto.LearningRoutePlan;
import com.verilearn.workflow.service.LearningRouteService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class LearningRouteServiceImpl implements LearningRouteService {

    private static final String ROUTE_FILE_NAME = "learning-route.md";
    private static final String ANSWER_SECTION_MARKER = "## 我的完成记录";

    private final AiRoutingService aiRoutingService;
    private final StorageProperties storageProperties;

    public LearningRouteServiceImpl(AiRoutingService aiRoutingService, StorageProperties storageProperties) {
        this.aiRoutingService = aiRoutingService;
        this.storageProperties = storageProperties;
    }

    @Override
    public LearningRoutePlan generateLearningRoute(Long userId, String topic, String targetLevel) {
        String content = aiRoutingService.chatForUser(
                userId,
                "You are a Chinese curriculum designer for a self-study system.",
                """
                        请为以下学习主题设计一条结构清晰的中文学习路线。
                        Topic: %s
                        Target level: %s

                        输出格式：
                        [OVERVIEW]
                        用 80 字以内概括这条学习路线的整体目标。

                        [CHAPTERS]
                        1. 章节标题 | 本章学习目标
                        2. 章节标题 | 本章学习目标
                        3. 章节标题 | 本章学习目标
                        4. 章节标题 | 本章学习目标

                        要求：
                        1. 至少输出 4 章，最多 6 章。
                        2. 章节标题必须具体，不能只写“核心概念/基础方法/综合应用”这类空泛模板。
                        3. 每章学习目标必须明确、可执行、适合自学。
                        4. 不要输出 FAQ，不要输出无关学科内容。
                        """.formatted(topic, defaultText(targetLevel, "beginner"))
        );
        if (content == null || content.isBlank()) {
            throw new AiGenerationException("学习路线生成失败，请稍后重试或检查 AI 配置。");
        }
        return parseRoute(content, topic);
    }

    @Override
    public List<String> generateTopicOptions(Long userId, String topic) {
        String content = aiRoutingService.chatForUser(
                userId,
                "You generate concrete Chinese study subtopics for a broad topic.",
                """
                        用户输入了一个范围较大的学习主题，请给出 3 到 5 个更具体的子方向。
                        Topic: %s

                        输出格式：
                        1. 子方向
                        2. 子方向
                        3. 子方向

                        要求：
                        1. 每个子方向都必须是可以直接开始学习的具体主题。
                        2. 不要输出解释性段落，只输出编号选项。
                        3. 不要重复。
                        """.formatted(topic)
        );
        if (content == null || content.isBlank()) {
            throw new AiGenerationException("学习方向选项生成失败，请稍后重试。");
        }

        List<String> options = parseOptions(content);
        if (options.size() < 3) {
            throw new AiGenerationException("学习方向选项生成结果不符合要求，系统已拒绝写入。");
        }
        return options;
    }

    @Override
    public String createOrUpdateRouteFile(String topic, String markdownContent) {
        String relativePath = buildRouteRelativePath(topic);
        Path absolutePath = resolvePath(relativePath);
        try {
            Files.createDirectories(absolutePath.getParent());
            Files.writeString(absolutePath, markdownContent == null ? "" : markdownContent, StandardCharsets.UTF_8);
            return relativePath.replace('\\', '/');
        } catch (IOException exception) {
            throw new IllegalStateException("failed to write learning route file", exception);
        }
    }

    @Override
    public String readRouteContent(String relativeFilePath) {
        Path absolutePath = resolvePath(relativeFilePath);
        if (!Files.exists(absolutePath)) {
            throw new IllegalArgumentException("learning route file not found");
        }
        try {
            return Files.readString(absolutePath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read learning route file", exception);
        }
    }

    @Override
    public String buildRouteRelativePath(String topic) {
        return slugify(topic) + "/" + ROUTE_FILE_NAME;
    }

    @Override
    public String resolveAbsolutePath(String relativeFilePath) {
        return resolvePath(relativeFilePath).toAbsolutePath().toString();
    }

    @Override
    public void deleteRouteDirectory(String topic) {
        Path directory = resolvePath(slugify(topic));
        if (!Files.exists(directory)) {
            return;
        }
        try {
            Files.walk(directory)
                    .sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new IllegalStateException("failed to delete learning route directory", exception);
                        }
                    });
        } catch (IOException exception) {
            throw new IllegalStateException("failed to delete learning route directory", exception);
        }
    }

    @Override
    public String extractDemoAnswerSections(String markdownContent) {
        if (markdownContent == null || markdownContent.isBlank()) {
            return "";
        }
        int markerIndex = markdownContent.indexOf(ANSWER_SECTION_MARKER);
        if (markerIndex < 0) {
            return "";
        }
        return markdownContent.substring(markerIndex).trim();
    }

    @Override
    public String ensureDemoAnswerTemplate(String markdownContent) {
        String normalized = markdownContent == null ? "" : markdownContent.trim();
        if (normalized.contains(ANSWER_SECTION_MARKER)) {
            return normalized;
        }
        return normalized + """


                ---

                ## 我的完成记录
                - 请在这里说明你完成了什么。

                ## 我的回答
                - 请在这里回答本章要求解释的概念题或理解题。

                ## 我的自检结果
                - 我是否完成了任务：
                - 我是否达到了预期结果：
                - 我还不确定的地方：
                """;
    }

    @Override
    public LearningRouteContentResponse buildRouteContentResponse(String topic, String contentUrl, String viewUrl) {
        String filePath = buildRouteRelativePath(topic);
        LearningRouteContentResponse response = new LearningRouteContentResponse();
        response.setTopic(topic);
        response.setFilePath(filePath);
        response.setAbsoluteFilePath(resolveAbsolutePath(filePath));
        response.setContentUrl(contentUrl);
        response.setViewUrl(viewUrl);
        response.setContentText(readRouteContent(filePath));
        return response;
    }

    private LearningRoutePlan parseRoute(String content, String topic) {
        String overview = extractSection(content, "[OVERVIEW]", "[CHAPTERS]");
        String chapterBody = extractSection(content, "[CHAPTERS]", null);
        List<LearningRouteChapter> chapters = parseChapters(chapterBody);
        if (overview == null || overview.isBlank() || chapters.size() < 4) {
            throw new AiGenerationException("学习路线生成结果不符合要求，系统已拒绝写入。");
        }

        LearningRoutePlan plan = new LearningRoutePlan();
        plan.setTopic(topic);
        plan.setOverview(overview.trim());
        plan.setChapters(chapters);
        plan.setMarkdownContent(buildRouteMarkdown(topic, overview, chapters));
        return plan;
    }

    private List<String> parseOptions(String content) {
        String[] lines = content.split("\\R");
        Set<String> options = new LinkedHashSet<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            String normalized = trimmed.replaceFirst("^\\d+[.)、]\\s*", "").trim();
            if (!normalized.isBlank()) {
                options.add(normalized);
            }
            if (options.size() >= 5) {
                break;
            }
        }
        return new ArrayList<>(options);
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

    private List<LearningRouteChapter> parseChapters(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        String[] lines = body.split("\\R");
        List<LearningRouteChapter> chapters = new ArrayList<>();
        int chapterNo = 1;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            String normalized = trimmed.replaceFirst("^\\d+[.)、]\\s*", "").trim();
            String[] parts = normalized.split("\\s*\\|\\s*", 2);
            if (parts.length < 2) {
                parts = normalized.split("\\s*-\\s*", 2);
            }
            if (parts.length < 2) {
                continue;
            }
            LearningRouteChapter chapter = new LearningRouteChapter();
            chapter.setChapterNo(chapterNo++);
            chapter.setTitle(parts[0].trim());
            chapter.setSummary(parts[1].trim());
            chapters.add(chapter);
            if (chapters.size() >= 6) {
                break;
            }
        }
        return chapters;
    }

    private String buildRouteMarkdown(String topic, String overview, List<LearningRouteChapter> chapters) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 学习路线：").append(topic).append("\n\n");
        builder.append("## 路线说明\n");
        builder.append(overview.trim()).append("\n\n");
        builder.append("## 章节安排\n");
        for (LearningRouteChapter chapter : chapters) {
            builder.append(chapter.getChapterNo())
                    .append(". **")
                    .append(chapter.getTitle())
                    .append("**：")
                    .append(chapter.getSummary())
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private Path resolvePath(String relativePath) {
        return Paths.get(storageProperties.getLearningSpaceRoot()).resolve(relativePath).normalize();
    }

    private String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "untitled";
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        return normalized.isBlank() ? "untitled" : normalized;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}

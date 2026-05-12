package com.verilearn.chapter.service.impl;

import com.verilearn.chapter.config.StorageProperties;
import com.verilearn.chapter.service.ChapterMaterialStorageService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ChapterMaterialStorageServiceImpl implements ChapterMaterialStorageService {

    private final StorageProperties storageProperties;

    public ChapterMaterialStorageServiceImpl(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public String createOrUpdateMaterialFile(
            String topic,
            Integer chapterNo,
            String chapterTitle,
            String materialType,
            String markdownContent
    ) {
        String relativePath = buildRelativePath(topic, chapterNo, chapterTitle, materialType);
        Path absolutePath = resolveAbsolutePath(relativePath);
        try {
            Files.createDirectories(absolutePath.getParent());
            Files.writeString(absolutePath, markdownContent == null ? "" : markdownContent, StandardCharsets.UTF_8);
            return relativePath.replace('\\', '/');
        } catch (IOException exception) {
            throw new IllegalStateException("failed to write chapter material file", exception);
        }
    }

    @Override
    public String readMaterialContent(String relativeFilePath, String fallbackContent) {
        if (relativeFilePath == null || relativeFilePath.isBlank()) {
            return fallbackContent;
        }

        Path absolutePath = resolveAbsolutePath(relativeFilePath);
        if (!Files.exists(absolutePath)) {
            return fallbackContent;
        }

        try {
            return Files.readString(absolutePath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read chapter material file", exception);
        }
    }

    private Path resolveAbsolutePath(String relativePath) {
        return Paths.get(storageProperties.getLearningSpaceRoot()).resolve(relativePath).normalize();
    }

    private String buildRelativePath(String topic, Integer chapterNo, String chapterTitle, String materialType) {
        String topicDir = slugify(topic);
        String chapterDir = String.format("%02d-%s", chapterNo == null ? 0 : chapterNo, slugify(chapterTitle));
        String materialFileName = switch (materialType) {
            case "THEORY_DOC" -> "user/theory/theory.md";
            case "DEMO_GUIDE" -> "user/demo/demo-task.md";
            case "EVALUATION_REPORT" -> "user/summary/evaluation-report.md";
            case "NEXT_STEP_NOTE" -> "user/summary/next-step.md";
            default -> "user/notes/material.md";
        };
        return topicDir + "/" + chapterDir + "/" + materialFileName;
    }

    private String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "untitled";
        }

        String normalized = input.trim().toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        return normalized.isBlank() ? "untitled" : normalized;
    }
}

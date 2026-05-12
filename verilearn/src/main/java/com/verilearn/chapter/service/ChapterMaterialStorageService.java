package com.verilearn.chapter.service;

public interface ChapterMaterialStorageService {

    String createOrUpdateMaterialFile(
            String topic,
            Integer chapterNo,
            String chapterTitle,
            String materialType,
            String markdownContent
    );

    String readMaterialContent(String relativeFilePath, String fallbackContent);
}

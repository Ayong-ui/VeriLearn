package com.verilearn.chapter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "verilearn.storage")
public class StorageProperties {

    private String learningSpaceRoot;

    public String getLearningSpaceRoot() {
        return learningSpaceRoot;
    }

    public void setLearningSpaceRoot(String learningSpaceRoot) {
        this.learningSpaceRoot = learningSpaceRoot;
    }
}

package com.verilearn.ai.dto;

public class AiChapterMaterialResult {

    private String theoryContent;
    private String demoGuideContent;
    private String summary;
    private boolean generatedByAi;
    private String provider;

    public String getTheoryContent() {
        return theoryContent;
    }

    public void setTheoryContent(String theoryContent) {
        this.theoryContent = theoryContent;
    }

    public String getDemoGuideContent() {
        return demoGuideContent;
    }

    public void setDemoGuideContent(String demoGuideContent) {
        this.demoGuideContent = demoGuideContent;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public boolean isGeneratedByAi() {
        return generatedByAi;
    }

    public void setGeneratedByAi(boolean generatedByAi) {
        this.generatedByAi = generatedByAi;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}

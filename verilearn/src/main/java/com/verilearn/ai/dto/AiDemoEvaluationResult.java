package com.verilearn.ai.dto;

public class AiDemoEvaluationResult {

    private String understandingLevel;
    private String evaluationMarkdown;
    private String nextStepMarkdown;
    private boolean shouldReview;
    private boolean generatedByAi;
    private String provider;

    public String getUnderstandingLevel() {
        return understandingLevel;
    }

    public void setUnderstandingLevel(String understandingLevel) {
        this.understandingLevel = understandingLevel;
    }

    public String getEvaluationMarkdown() {
        return evaluationMarkdown;
    }

    public void setEvaluationMarkdown(String evaluationMarkdown) {
        this.evaluationMarkdown = evaluationMarkdown;
    }

    public String getNextStepMarkdown() {
        return nextStepMarkdown;
    }

    public void setNextStepMarkdown(String nextStepMarkdown) {
        this.nextStepMarkdown = nextStepMarkdown;
    }

    public boolean isShouldReview() {
        return shouldReview;
    }

    public void setShouldReview(boolean shouldReview) {
        this.shouldReview = shouldReview;
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

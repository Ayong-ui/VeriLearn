package com.verilearn.ai.service;

public interface AiRoutingService {

    String chatForUser(Long userId, String systemPrompt, String userPrompt);

    String chatForUser(Long userId, String systemPrompt, String userPrompt, double temperature);
}

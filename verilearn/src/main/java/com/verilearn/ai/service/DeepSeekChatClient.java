package com.verilearn.ai.service;

public interface DeepSeekChatClient {

    String chat(String systemPrompt, String userPrompt);
}

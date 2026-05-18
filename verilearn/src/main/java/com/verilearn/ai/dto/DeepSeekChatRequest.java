package com.verilearn.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeepSeekChatRequest(
        String model,
        List<Message> messages,
        Double temperature,
        @JsonProperty("max_tokens") Integer maxTokens
) {
    public record Message(String role, String content) {
    }
}

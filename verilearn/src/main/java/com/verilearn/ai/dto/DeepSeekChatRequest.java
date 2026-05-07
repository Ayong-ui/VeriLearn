package com.verilearn.ai.dto;

import java.util.List;

public record DeepSeekChatRequest(
        String model,
        List<Message> messages,
        Double temperature
) {
    public record Message(String role, String content) {
    }
}

package com.verilearn.ai.dto;

import java.util.List;

public record DeepSeekChatResponse(
        List<Choice> choices
) {
    public record Choice(Message message) {
    }

    public record Message(String role, String content) {
    }
}

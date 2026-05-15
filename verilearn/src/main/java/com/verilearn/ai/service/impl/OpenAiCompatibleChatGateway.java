package com.verilearn.ai.service.impl;

import com.verilearn.ai.dto.DeepSeekChatRequest;
import com.verilearn.ai.dto.DeepSeekChatResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class OpenAiCompatibleChatGateway {

    private final RestClient restClient;

    public OpenAiCompatibleChatGateway() {
        this.restClient = RestClient.builder().build();
    }

    public String chat(String baseUrl, String apiKey, String model, String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank() || baseUrl == null || baseUrl.isBlank() || model == null || model.isBlank()) {
            return null;
        }

        try {
            DeepSeekChatResponse response = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(new DeepSeekChatRequest(
                            model,
                            List.of(
                                    new DeepSeekChatRequest.Message("system", systemPrompt),
                                    new DeepSeekChatRequest.Message("user", userPrompt)
                            ),
                            0.4
                    ))
                    .retrieve()
                    .body(DeepSeekChatResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                return null;
            }
            DeepSeekChatResponse.Choice choice = response.choices().get(0);
            if (choice == null || choice.message() == null) {
                return null;
            }
            return choice.message().content();
        } catch (Exception ignored) {
            return null;
        }
    }
}

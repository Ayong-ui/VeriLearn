package com.verilearn.ai.service.impl;

import com.verilearn.ai.config.AiProperties;
import com.verilearn.ai.dto.DeepSeekChatRequest;
import com.verilearn.ai.dto.DeepSeekChatResponse;
import com.verilearn.ai.service.DeepSeekChatClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class DeepSeekChatClientImpl implements DeepSeekChatClient {

    private final AiProperties aiProperties;
    private final RestClient restClient;

    public DeepSeekChatClientImpl(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            return null;
        }

        try {
            DeepSeekChatResponse response = restClient.post()
                    .uri(aiProperties.getBaseUrl() + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .body(new DeepSeekChatRequest(
                            aiProperties.getModel(),
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

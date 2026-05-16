package com.verilearn.ai.service.impl;

import com.verilearn.ai.dto.DeepSeekChatRequest;
import com.verilearn.ai.dto.DeepSeekChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class OpenAiCompatibleChatGateway {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleChatGateway.class);

    private final RestClient restClient;

    public OpenAiCompatibleChatGateway() {
        this.restClient = RestClient.builder().build();
    }

    public String chat(String baseUrl, String apiKey, String model, String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank() || baseUrl == null || baseUrl.isBlank() || model == null || model.isBlank()) {
            log.warn("skip ai request because config is incomplete: baseUrlConfigured={}, apiKeyConfigured={}, modelConfigured={}",
                    baseUrl != null && !baseUrl.isBlank(),
                    apiKey != null && !apiKey.isBlank(),
                    model != null && !model.isBlank());
            return null;
        }

        long startedAt = System.currentTimeMillis();
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
                log.warn("ai request returned empty choices: model={}, baseUrl={}, elapsedMs={}",
                        model, baseUrl, System.currentTimeMillis() - startedAt);
                return null;
            }
            DeepSeekChatResponse.Choice choice = response.choices().get(0);
            if (choice == null || choice.message() == null) {
                log.warn("ai request returned empty message: model={}, baseUrl={}, elapsedMs={}",
                        model, baseUrl, System.currentTimeMillis() - startedAt);
                return null;
            }
            log.info("ai request completed successfully: model={}, baseUrl={}, elapsedMs={}",
                    model, baseUrl, System.currentTimeMillis() - startedAt);
            return choice.message().content();
        } catch (Exception exception) {
            log.error("ai request failed: model={}, baseUrl={}, elapsedMs={}, error={}",
                    model, baseUrl, System.currentTimeMillis() - startedAt, exception.getMessage(), exception);
            return null;
        }
    }
}

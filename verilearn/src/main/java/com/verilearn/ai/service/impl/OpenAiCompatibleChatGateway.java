package com.verilearn.ai.service.impl;

import com.verilearn.ai.dto.DeepSeekChatRequest;
import com.verilearn.ai.dto.DeepSeekChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class OpenAiCompatibleChatGateway {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleChatGateway.class);
    private static final int DEFAULT_MAX_TOKENS = 4096;
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_BASE_DELAY_MS = 1000;

    private final RestClient restClient;

    public OpenAiCompatibleChatGateway() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);  // 连接超时 10 秒
        factory.setReadTimeout(120_000);    // 读取超时 120 秒（AI 生成需要较长时间）
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    public String chat(String baseUrl, String apiKey, String model,
                       String systemPrompt, String userPrompt,
                       double temperature) {
        return chat(baseUrl, apiKey, model, systemPrompt, userPrompt, temperature, DEFAULT_MAX_TOKENS);
    }

    public String chat(String baseUrl, String apiKey, String model,
                       String systemPrompt, String userPrompt,
                       double temperature, int maxTokens) {
        if (apiKey == null || apiKey.isBlank() || baseUrl == null || baseUrl.isBlank() || model == null || model.isBlank()) {
            log.warn("skip ai request because config is incomplete: baseUrlConfigured={}, apiKeyConfigured={}, modelConfigured={}",
                    baseUrl != null && !baseUrl.isBlank(),
                    apiKey != null && !apiKey.isBlank(),
                    model != null && !model.isBlank());
            return null;
        }

        long startedAt = System.currentTimeMillis();
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
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
                                temperature,
                                maxTokens
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
                log.info("ai request completed successfully: model={}, baseUrl={}, elapsedMs={}, attempt={}",
                        model, baseUrl, System.currentTimeMillis() - startedAt, attempt);
                return choice.message().content();
            } catch (Exception exception) {
                long elapsedMs = System.currentTimeMillis() - startedAt;
                boolean retriable = isRetriable(exception);
                if (retriable && attempt < MAX_RETRIES) {
                    long delayMs = RETRY_BASE_DELAY_MS * (1L << attempt); // 1s, 2s
                    log.warn("ai request failed, will retry: model={}, baseUrl={}, elapsedMs={}, attempt={}/{}, retryInMs={}, error={}",
                            model, baseUrl, elapsedMs, attempt + 1, MAX_RETRIES, delayMs, exception.getMessage());
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        log.error("retry interrupted: model={}, baseUrl={}", model, baseUrl);
                        return null;
                    }
                } else {
                    log.error("ai request failed: model={}, baseUrl={}, elapsedMs={}, attempt={}/{}, retriable={}, error={}",
                            model, baseUrl, elapsedMs, attempt + 1, MAX_RETRIES + 1, retriable,
                            exception.getMessage(), exception);
                    return null;
                }
            }
        }
        return null;
    }

    private boolean isRetriable(Exception exception) {
        // I/O errors (connect timeout, read timeout, connection reset) — transient
        if (exception instanceof ResourceAccessException) {
            return true;
        }
        // 5xx server errors (except 501 Not Implemented) — transient overload
        if (exception instanceof HttpServerErrorException hsee) {
            return hsee.getStatusCode().value() != 501;
        }
        return false;
    }
}

package com.verilearn.ai.service.impl;

import com.verilearn.ai.dto.ResolvedAiProviderConfig;
import com.verilearn.ai.service.AiProviderConfigService;
import com.verilearn.ai.service.AiRoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiRoutingServiceImpl implements AiRoutingService {

    private static final Logger log = LoggerFactory.getLogger(AiRoutingServiceImpl.class);

    private final AiProviderConfigService aiProviderConfigService;
    private final OpenAiCompatibleChatGateway openAiCompatibleChatGateway;

    public AiRoutingServiceImpl(
            AiProviderConfigService aiProviderConfigService,
            OpenAiCompatibleChatGateway openAiCompatibleChatGateway
    ) {
        this.aiProviderConfigService = aiProviderConfigService;
        this.openAiCompatibleChatGateway = openAiCompatibleChatGateway;
    }

    @Override
    public String chatForUser(Long userId, String systemPrompt, String userPrompt) {
        return chatForUser(userId, systemPrompt, userPrompt, 0.7);
    }

    @Override
    public String chatForUser(Long userId, String systemPrompt, String userPrompt, double temperature) {
        ResolvedAiProviderConfig config = aiProviderConfigService.resolveConfigByUserId(userId);
        log.info("routing ai request: userId={}, sourceType={}, providerType={}, modelName={}, baseUrl={}, temperature={}",
                userId, config.getSourceType(), config.getProviderType(), config.getModelName(), config.getBaseUrl(), temperature);
        String response = openAiCompatibleChatGateway.chat(
                config.getBaseUrl(),
                config.getApiKey(),
                config.getModelName(),
                systemPrompt,
                userPrompt,
                temperature
        );
        if (response == null || response.isBlank()) {
            log.warn("ai request returned empty content: userId={}, sourceType={}, providerType={}, modelName={}",
                    userId, config.getSourceType(), config.getProviderType(), config.getModelName());
        }
        return response;
    }
}

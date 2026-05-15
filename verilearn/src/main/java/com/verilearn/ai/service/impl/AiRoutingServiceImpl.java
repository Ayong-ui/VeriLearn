package com.verilearn.ai.service.impl;

import com.verilearn.ai.dto.ResolvedAiProviderConfig;
import com.verilearn.ai.service.AiProviderConfigService;
import com.verilearn.ai.service.AiRoutingService;
import org.springframework.stereotype.Service;

@Service
public class AiRoutingServiceImpl implements AiRoutingService {

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
        ResolvedAiProviderConfig config = aiProviderConfigService.resolveConfigByUserId(userId);
        return openAiCompatibleChatGateway.chat(
                config.getBaseUrl(),
                config.getApiKey(),
                config.getModelName(),
                systemPrompt,
                userPrompt
        );
    }
}

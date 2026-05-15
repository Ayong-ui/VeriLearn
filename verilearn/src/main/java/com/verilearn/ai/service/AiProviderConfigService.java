package com.verilearn.ai.service;

import com.verilearn.ai.dto.AiProviderConfigResponse;
import com.verilearn.ai.dto.AiProviderConfigUpsertRequest;
import com.verilearn.ai.dto.ResolvedAiProviderConfig;

import java.util.List;

public interface AiProviderConfigService {

    AiProviderConfigResponse saveConfig(String feishuOpenId, AiProviderConfigUpsertRequest request);

    List<AiProviderConfigResponse> listConfigs(String feishuOpenId);

    AiProviderConfigResponse getCurrentConfig(String feishuOpenId);

    AiProviderConfigResponse activateConfig(String feishuOpenId, Long configId);

    ResolvedAiProviderConfig resolveConfigByUserId(Long userId);
}

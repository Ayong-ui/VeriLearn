package com.verilearn.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.verilearn.ai.config.AiProperties;
import com.verilearn.ai.dto.AiProviderConfigResponse;
import com.verilearn.ai.dto.AiProviderConfigUpsertRequest;
import com.verilearn.ai.dto.ResolvedAiProviderConfig;
import com.verilearn.ai.entity.LearnerAiProviderConfig;
import com.verilearn.ai.mapper.LearnerAiProviderConfigMapper;
import com.verilearn.ai.model.AiProviderType;
import com.verilearn.ai.service.AiProviderConfigService;
import com.verilearn.ai.service.AiSecretCryptoService;
import com.verilearn.user.entity.LearnerUser;
import com.verilearn.user.mapper.LearnerUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiProviderConfigServiceImpl implements AiProviderConfigService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String SOURCE_SYSTEM_DEFAULT = "SYSTEM_DEFAULT";
    private static final String SOURCE_USER_CONFIG = "USER_CONFIG";

    private final LearnerAiProviderConfigMapper learnerAiProviderConfigMapper;
    private final LearnerUserMapper learnerUserMapper;
    private final AiSecretCryptoService aiSecretCryptoService;
    private final AiProperties aiProperties;

    public AiProviderConfigServiceImpl(
            LearnerAiProviderConfigMapper learnerAiProviderConfigMapper,
            LearnerUserMapper learnerUserMapper,
            AiSecretCryptoService aiSecretCryptoService,
            AiProperties aiProperties
    ) {
        this.learnerAiProviderConfigMapper = learnerAiProviderConfigMapper;
        this.learnerUserMapper = learnerUserMapper;
        this.aiSecretCryptoService = aiSecretCryptoService;
        this.aiProperties = aiProperties;
    }

    @Override
    @Transactional
    public AiProviderConfigResponse saveConfig(String feishuOpenId, AiProviderConfigUpsertRequest request) {
        LearnerUser learnerUser = getLearnerByOpenId(feishuOpenId);
        AiProviderType providerType = AiProviderType.resolveOrDefault(request.getProviderType());
        LocalDateTime now = LocalDateTime.now();

        LearnerAiProviderConfig config = new LearnerAiProviderConfig();
        config.setUserId(learnerUser.getId());
        config.setProviderType(providerType.name());
        config.setBaseUrl(request.getBaseUrl().trim());
        config.setModelName(request.getModelName().trim());
        config.setApiKeyCiphertext(aiSecretCryptoService.encrypt(request.getApiKey().trim()));
        config.setApiKeyMasked(aiSecretCryptoService.mask(request.getApiKey()));
        config.setStatus(STATUS_ACTIVE);
        config.setIsActive(Boolean.TRUE.equals(request.getActivateNow()) || request.getActivateNow() == null);
        config.setCreatedAt(now);
        config.setUpdatedAt(now);

        learnerAiProviderConfigMapper.insert(config);

        if (Boolean.TRUE.equals(config.getIsActive())) {
            deactivateOtherConfigs(learnerUser.getId(), config.getId(), now);
        }

        return toResponse(config, SOURCE_USER_CONFIG);
    }

    @Override
    public List<AiProviderConfigResponse> listConfigs(String feishuOpenId) {
        LearnerUser learnerUser = getLearnerByOpenId(feishuOpenId);
        List<LearnerAiProviderConfig> configs = learnerAiProviderConfigMapper.selectList(
                new LambdaQueryWrapper<LearnerAiProviderConfig>()
                        .eq(LearnerAiProviderConfig::getUserId, learnerUser.getId())
                        .eq(LearnerAiProviderConfig::getStatus, STATUS_ACTIVE)
                        .orderByDesc(LearnerAiProviderConfig::getIsActive)
                        .orderByDesc(LearnerAiProviderConfig::getId)
        );

        List<AiProviderConfigResponse> responses = new ArrayList<>();
        if (configs.isEmpty()) {
            responses.add(buildSystemDefaultResponse(learnerUser.getId()));
            return responses;
        }

        for (LearnerAiProviderConfig config : configs) {
            responses.add(toResponse(config, SOURCE_USER_CONFIG));
        }
        return responses;
    }

    @Override
    public AiProviderConfigResponse getCurrentConfig(String feishuOpenId) {
        LearnerUser learnerUser = getLearnerByOpenId(feishuOpenId);
        LearnerAiProviderConfig activeConfig = findActiveConfig(learnerUser.getId());
        if (activeConfig != null) {
            return toResponse(activeConfig, SOURCE_USER_CONFIG);
        }
        return buildSystemDefaultResponse(learnerUser.getId());
    }

    @Override
    @Transactional
    public AiProviderConfigResponse activateConfig(String feishuOpenId, Long configId) {
        LearnerUser learnerUser = getLearnerByOpenId(feishuOpenId);
        LearnerAiProviderConfig config = learnerAiProviderConfigMapper.selectById(configId);
        if (config == null || !learnerUser.getId().equals(config.getUserId())) {
            throw new IllegalArgumentException("ai provider config not found");
        }

        LocalDateTime now = LocalDateTime.now();
        deactivateOtherConfigs(learnerUser.getId(), config.getId(), now);
        config.setIsActive(true);
        config.setStatus(STATUS_ACTIVE);
        config.setUpdatedAt(now);
        learnerAiProviderConfigMapper.updateById(config);
        return toResponse(config, SOURCE_USER_CONFIG);
    }

    @Override
    public ResolvedAiProviderConfig resolveConfigByUserId(Long userId) {
        LearnerAiProviderConfig activeConfig = findActiveConfig(userId);
        if (activeConfig != null) {
            ResolvedAiProviderConfig resolvedConfig = new ResolvedAiProviderConfig();
            resolvedConfig.setUserId(userId);
            resolvedConfig.setProviderType(activeConfig.getProviderType());
            resolvedConfig.setBaseUrl(activeConfig.getBaseUrl());
            resolvedConfig.setModelName(activeConfig.getModelName());
            resolvedConfig.setApiKey(aiSecretCryptoService.decrypt(activeConfig.getApiKeyCiphertext()));
            resolvedConfig.setSourceType(SOURCE_USER_CONFIG);
            return resolvedConfig;
        }

        ResolvedAiProviderConfig fallbackConfig = new ResolvedAiProviderConfig();
        fallbackConfig.setUserId(userId);
        fallbackConfig.setProviderType(AiProviderType.resolveOrDefault(aiProperties.getProviderType()).name());
        fallbackConfig.setBaseUrl(aiProperties.getBaseUrl());
        fallbackConfig.setModelName(aiProperties.getModel());
        fallbackConfig.setApiKey(aiProperties.getApiKey());
        fallbackConfig.setSourceType(SOURCE_SYSTEM_DEFAULT);
        return fallbackConfig;
    }

    private LearnerUser getLearnerByOpenId(String feishuOpenId) {
        LearnerUser learnerUser = learnerUserMapper.selectOne(
                new LambdaQueryWrapper<LearnerUser>()
                        .eq(LearnerUser::getFeishuOpenId, feishuOpenId)
                        .orderByDesc(LearnerUser::getId)
                        .last("LIMIT 1")
        );
        if (learnerUser == null) {
            throw new IllegalArgumentException("learner not found");
        }
        return learnerUser;
    }

    private LearnerAiProviderConfig findActiveConfig(Long userId) {
        return learnerAiProviderConfigMapper.selectOne(
                new LambdaQueryWrapper<LearnerAiProviderConfig>()
                        .eq(LearnerAiProviderConfig::getUserId, userId)
                        .eq(LearnerAiProviderConfig::getStatus, STATUS_ACTIVE)
                        .eq(LearnerAiProviderConfig::getIsActive, true)
                        .orderByDesc(LearnerAiProviderConfig::getId)
                        .last("LIMIT 1")
        );
    }

    private void deactivateOtherConfigs(Long userId, Long keepConfigId, LocalDateTime now) {
        learnerAiProviderConfigMapper.selectList(
                        new LambdaQueryWrapper<LearnerAiProviderConfig>()
                                .eq(LearnerAiProviderConfig::getUserId, userId)
                                .eq(LearnerAiProviderConfig::getStatus, STATUS_ACTIVE))
                .stream()
                .filter(config -> !config.getId().equals(keepConfigId))
                .forEach(config -> {
                    config.setIsActive(false);
                    config.setUpdatedAt(now);
                    learnerAiProviderConfigMapper.updateById(config);
                });
    }

    private AiProviderConfigResponse buildSystemDefaultResponse(Long userId) {
        AiProviderConfigResponse response = new AiProviderConfigResponse();
        response.setUserId(userId);
        response.setProviderType(AiProviderType.resolveOrDefault(aiProperties.getProviderType()).name());
        response.setBaseUrl(aiProperties.getBaseUrl());
        response.setModelName(aiProperties.getModel());
        response.setApiKeyMasked(aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()
                ? "NOT_CONFIGURED"
                : aiSecretCryptoService.mask(aiProperties.getApiKey()));
        response.setStatus(STATUS_ACTIVE);
        response.setActive(true);
        response.setSourceType(SOURCE_SYSTEM_DEFAULT);
        return response;
    }

    private AiProviderConfigResponse toResponse(LearnerAiProviderConfig config, String sourceType) {
        AiProviderConfigResponse response = new AiProviderConfigResponse();
        response.setConfigId(config.getId());
        response.setUserId(config.getUserId());
        response.setProviderType(config.getProviderType());
        response.setBaseUrl(config.getBaseUrl());
        response.setModelName(config.getModelName());
        response.setApiKeyMasked(config.getApiKeyMasked());
        response.setStatus(config.getStatus());
        response.setActive(Boolean.TRUE.equals(config.getIsActive()));
        response.setSourceType(sourceType);
        return response;
    }
}

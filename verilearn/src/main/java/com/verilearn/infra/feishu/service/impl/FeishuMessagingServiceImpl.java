package com.verilearn.infra.feishu.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verilearn.infra.feishu.config.FeishuProperties;
import com.verilearn.infra.feishu.dto.FeishuCardPreviewResponse;
import com.verilearn.infra.feishu.dto.FeishuSendMessageResponse;
import com.verilearn.infra.feishu.dto.FeishuTenantAccessTokenResponse;
import com.verilearn.infra.feishu.service.FeishuMessagingService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class FeishuMessagingServiceImpl implements FeishuMessagingService {

    private final FeishuProperties feishuProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public FeishuMessagingServiceImpl(FeishuProperties feishuProperties, ObjectMapper objectMapper) {
        this.feishuProperties = feishuProperties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public void sendTextMessage(String openId, String text) {
        if (!isMessagingConfigured() || isBlank(openId) || isBlank(text)) {
            return;
        }
        sendMessage(openId, "text", writeJson(Map.of("text", text)));
    }

    @Override
    public void sendInteractiveCard(String openId, FeishuCardPreviewResponse cardPreviewResponse) {
        if (!isMessagingConfigured() || isBlank(openId) || cardPreviewResponse == null || cardPreviewResponse.getCard() == null) {
            return;
        }
        sendMessage(openId, "interactive", writeJson(Map.of("card", cardPreviewResponse.getCard())));
    }

    private void sendMessage(String openId, String messageType, String content) {
        String tenantAccessToken = getTenantAccessToken();
        if (tenantAccessToken == null) {
            return;
        }

        FeishuSendMessageResponse response = restClient.post()
                .uri(feishuProperties.getBaseUrl() + "/im/v1/messages?receive_id_type=open_id")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tenantAccessToken)
                .body(Map.of(
                        "receive_id", openId,
                        "msg_type", messageType,
                        "content", content
                ))
                .retrieve()
                .body(FeishuSendMessageResponse.class);

        if (response == null || response.getCode() != 0) {
            throw new IllegalStateException("failed to send feishu message");
        }
    }

    private String getTenantAccessToken() {
        FeishuTenantAccessTokenResponse response = restClient.post()
                .uri(feishuProperties.getBaseUrl() + "/auth/v3/tenant_access_token/internal")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "app_id", feishuProperties.getAppId(),
                        "app_secret", feishuProperties.getAppSecret()
                ))
                .retrieve()
                .body(FeishuTenantAccessTokenResponse.class);

        if (response == null || response.getCode() != 0) {
            return null;
        }
        return response.getTenantAccessToken();
    }

    private boolean isMessagingConfigured() {
        return !isBlank(feishuProperties.getBaseUrl())
                && !isBlank(feishuProperties.getAppId())
                && !isBlank(feishuProperties.getAppSecret());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String writeJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize feishu message content", exception);
        }
    }
}

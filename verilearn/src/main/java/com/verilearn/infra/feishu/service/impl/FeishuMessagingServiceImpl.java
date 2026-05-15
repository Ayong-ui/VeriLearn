package com.verilearn.infra.feishu.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verilearn.infra.feishu.config.FeishuProperties;
import com.verilearn.infra.feishu.dto.FeishuCardPreviewResponse;
import com.verilearn.infra.feishu.dto.FeishuReplyTarget;
import com.verilearn.infra.feishu.dto.FeishuSendMessageResponse;
import com.verilearn.infra.feishu.dto.FeishuTenantAccessTokenResponse;
import com.verilearn.infra.feishu.service.FeishuMessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class FeishuMessagingServiceImpl implements FeishuMessagingService {

    private static final Logger log = LoggerFactory.getLogger(FeishuMessagingServiceImpl.class);

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
        sendTextMessage(FeishuReplyTarget.openId(openId), text);
    }

    @Override
    public void sendTextMessage(FeishuReplyTarget replyTarget, String text) {
        if (!isMessagingConfigured() || replyTarget == null || isBlank(replyTarget.getReceiveId()) || isBlank(text)) {
            return;
        }
        sendMessage(replyTarget, "text", writeJson(Map.of("text", text)));
    }

    @Override
    public void sendInteractiveCard(String openId, FeishuCardPreviewResponse cardPreviewResponse) {
        if (!isMessagingConfigured() || isBlank(openId) || cardPreviewResponse == null || cardPreviewResponse.getCard() == null) {
            return;
        }
        sendMessage(FeishuReplyTarget.openId(openId), "interactive", writeJson(Map.of("card", cardPreviewResponse.getCard())));
    }

    private void sendMessage(FeishuReplyTarget replyTarget, String messageType, String content) {
        String tenantAccessToken = getTenantAccessToken();
        if (tenantAccessToken == null) {
            return;
        }

        FeishuSendMessageResponse response = restClient.post()
                .uri(feishuProperties.getBaseUrl() + "/im/v1/messages?receive_id_type=" + replyTarget.getReceiveIdType())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + tenantAccessToken)
                .body(Map.of(
                        "receive_id", replyTarget.getReceiveId(),
                        "msg_type", messageType,
                        "content", content
                ))
                .retrieve()
                .body(FeishuSendMessageResponse.class);

        if (response == null || response.getCode() != 0) {
            log.error("failed to send feishu message: receiveIdType={}, receiveId={}, messageType={}, responseCode={}, responseMessage={}",
                    replyTarget.getReceiveIdType(),
                    replyTarget.getReceiveId(),
                    messageType,
                    response == null ? null : response.getCode(),
                    response == null ? null : response.getMsg());
            throw new IllegalStateException("failed to send feishu message");
        }

        log.info("sent feishu message successfully: receiveIdType={}, receiveId={}, messageType={}",
                replyTarget.getReceiveIdType(),
                replyTarget.getReceiveId(),
                messageType);
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
            log.error("failed to get feishu tenant access token: responseCode={}, responseMessage={}",
                    response == null ? null : response.getCode(),
                    response == null ? null : response.getMsg());
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

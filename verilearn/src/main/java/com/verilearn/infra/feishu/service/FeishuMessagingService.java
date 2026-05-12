package com.verilearn.infra.feishu.service;

import com.verilearn.infra.feishu.dto.FeishuCardPreviewResponse;

public interface FeishuMessagingService {

    void sendTextMessage(String openId, String text);

    void sendInteractiveCard(String openId, FeishuCardPreviewResponse cardPreviewResponse);
}

package com.verilearn.infra.feishu.service;

import com.verilearn.infra.feishu.dto.FeishuCardPreviewResponse;
import com.verilearn.infra.feishu.dto.FeishuReplyTarget;

public interface FeishuMessagingService {

    void sendTextMessage(String openId, String text);

    void sendTextMessage(FeishuReplyTarget replyTarget, String text);

    void sendInteractiveCard(String openId, FeishuCardPreviewResponse cardPreviewResponse);
}

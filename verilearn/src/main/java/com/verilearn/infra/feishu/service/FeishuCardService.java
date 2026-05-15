package com.verilearn.infra.feishu.service;

import com.verilearn.infra.feishu.dto.FeishuCardActionRequest;
import com.verilearn.infra.feishu.dto.FeishuCardActionResponse;
import com.verilearn.infra.feishu.dto.FeishuCardPreviewResponse;

public interface FeishuCardService {

    FeishuCardPreviewResponse buildTodayTaskCard(String openId);

    FeishuCardPreviewResponse buildDashboardCard(String openId);

    FeishuCardPreviewResponse buildCurrentContextCard(String openId);

    FeishuCardPreviewResponse buildAiProviderCard(String openId);

    FeishuCardActionResponse handleCardAction(FeishuCardActionRequest request);
}

package com.verilearn.infra.feishu.service;

import com.lark.oapi.event.cardcallback.model.P2CardActionTrigger;
import com.lark.oapi.event.cardcallback.model.P2CardActionTriggerResponse;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;

public interface FeishuLongConnectionBridgeService {

    void handleMessageEvent(P2MessageReceiveV1 event);

    P2CardActionTriggerResponse handleCardActionEvent(P2CardActionTrigger event);
}

package com.verilearn.infra.feishu;

import com.lark.oapi.event.cardcallback.model.CallBackAction;
import com.lark.oapi.event.cardcallback.model.CallBackOperator;
import com.lark.oapi.event.cardcallback.model.P2CardActionTrigger;
import com.lark.oapi.event.cardcallback.model.P2CardActionTriggerData;
import com.lark.oapi.event.cardcallback.model.P2CardActionTriggerResponse;
import com.lark.oapi.service.im.v1.model.EventMessage;
import com.lark.oapi.service.im.v1.model.EventSender;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1Data;
import com.lark.oapi.service.im.v1.model.UserId;
import com.verilearn.infra.feishu.config.FeishuProperties;
import com.verilearn.infra.feishu.dto.FeishuCardActionResponse;
import com.verilearn.infra.feishu.dto.FeishuCommandResponse;
import com.verilearn.infra.feishu.dto.FeishuReplyTarget;
import com.verilearn.infra.feishu.service.FeishuCardService;
import com.verilearn.infra.feishu.service.FeishuCommandService;
import com.verilearn.infra.feishu.service.FeishuMessagingService;
import com.verilearn.infra.feishu.service.impl.FeishuLongConnectionBridgeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeishuLongConnectionBridgeServiceTest {

    @Mock
    private FeishuCommandService feishuCommandService;

    @Mock
    private FeishuCardService feishuCardService;

    @Mock
    private FeishuMessagingService feishuMessagingService;

    private final FeishuProperties feishuProperties = new FeishuProperties();

    @Test
    void shouldBridgeLongConnectionMessageToCommandService() {
        feishuProperties.setVerificationToken("bridge-token");
        FeishuLongConnectionBridgeServiceImpl bridgeService = new FeishuLongConnectionBridgeServiceImpl(
                feishuProperties,
                feishuCommandService,
                feishuCardService,
                feishuMessagingService
        );

        FeishuCommandResponse commandResponse = new FeishuCommandResponse();
        commandResponse.setOpenId("ou_bridge_message");
        commandResponse.setReplyText("今日任务已生成");
        when(feishuCommandService.handleCommand(any())).thenReturn(commandResponse);

        bridgeService.handleMessageEvent(buildTextMessageEvent("ou_bridge_message", "p2p", "{\"text\":\"/today\"}"));

        ArgumentCaptor<com.verilearn.infra.feishu.dto.FeishuEventRequest> requestCaptor =
                ArgumentCaptor.forClass(com.verilearn.infra.feishu.dto.FeishuEventRequest.class);
        verify(feishuCommandService).handleCommand(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getHeader().getEventType()).isEqualTo("im.message.receive_v1");
        assertThat(requestCaptor.getValue().getEvent().getSender().getSenderId().getOpenId()).isEqualTo("ou_bridge_message");
        assertThat(requestCaptor.getValue().getEvent().getMessage().getContent()).isEqualTo("{\"text\":\"/today\"}");

        ArgumentCaptor<FeishuReplyTarget> targetCaptor = ArgumentCaptor.forClass(FeishuReplyTarget.class);
        ArgumentCaptor<String> replyCaptor = ArgumentCaptor.forClass(String.class);
        verify(feishuMessagingService).sendTextMessage(targetCaptor.capture(), replyCaptor.capture());
        assertThat(targetCaptor.getValue().getReceiveIdType()).isEqualTo("open_id");
        assertThat(targetCaptor.getValue().getReceiveId()).isEqualTo("ou_bridge_message");
        assertThat(replyCaptor.getValue()).isEqualTo("今日任务已生成");
    }

    @Test
    void shouldGuideUserToSingleChatWhenMessageComesFromGroup() {
        feishuProperties.setVerificationToken("bridge-token");
        FeishuLongConnectionBridgeServiceImpl bridgeService = new FeishuLongConnectionBridgeServiceImpl(
                feishuProperties,
                feishuCommandService,
                feishuCardService,
                feishuMessagingService
        );

        bridgeService.handleMessageEvent(buildTextMessageEvent("ou_group_user", "group", "{\"text\":\"@VeriLearn /today\"}"));

        ArgumentCaptor<FeishuReplyTarget> targetCaptor = ArgumentCaptor.forClass(FeishuReplyTarget.class);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(feishuMessagingService).sendTextMessage(targetCaptor.capture(), textCaptor.capture());
        assertThat(targetCaptor.getValue().getReceiveIdType()).isEqualTo("open_id");
        assertThat(targetCaptor.getValue().getReceiveId()).isEqualTo("ou_group_user");
        assertThat(textCaptor.getValue()).contains("当前版本仅支持与 VeriLearn 机器人单聊");
    }

    @Test
    void shouldBridgeCardCallbackToExistingCardService() {
        FeishuLongConnectionBridgeServiceImpl bridgeService = new FeishuLongConnectionBridgeServiceImpl(
                feishuProperties,
                feishuCommandService,
                feishuCardService,
                feishuMessagingService
        );

        FeishuCardActionResponse actionResponse = new FeishuCardActionResponse();
        actionResponse.setToastText("已切换到学习总览");
        actionResponse.setCard(Map.of("schema", "2.0", "header", Map.of("title", Map.of("content", "VeriLearn"))));
        when(feishuCardService.handleCardAction(any())).thenReturn(actionResponse);

        P2CardActionTriggerResponse sdkResponse = bridgeService.handleCardActionEvent(buildCardActionEvent("ou_bridge_card", "SHOW_DASHBOARD"));

        ArgumentCaptor<com.verilearn.infra.feishu.dto.FeishuCardActionRequest> requestCaptor =
                ArgumentCaptor.forClass(com.verilearn.infra.feishu.dto.FeishuCardActionRequest.class);
        verify(feishuCardService).handleCardAction(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getOpenId()).isEqualTo("ou_bridge_card");
        assertThat(requestCaptor.getValue().getAction().getValue().getActionName()).isEqualTo("SHOW_DASHBOARD");
        assertThat(sdkResponse.getToast()).isNotNull();
        assertThat(sdkResponse.getToast().getContent()).isEqualTo("已切换到学习总览");
        assertThat(sdkResponse.getCard()).isNotNull();
        assertThat(sdkResponse.getCard().getType()).isEqualTo("raw");
    }

    private P2MessageReceiveV1 buildTextMessageEvent(String openId, String chatType, String content) {
        UserId userId = new UserId();
        userId.setOpenId(openId);

        EventSender sender = new EventSender();
        sender.setSenderId(userId);

        EventMessage message = new EventMessage();
        message.setChatType(chatType);
        message.setMessageType("text");
        message.setContent(content);

        P2MessageReceiveV1Data data = new P2MessageReceiveV1Data();
        data.setSender(sender);
        data.setMessage(message);

        P2MessageReceiveV1 event = new P2MessageReceiveV1();
        event.setEvent(data);
        return event;
    }

    private P2CardActionTrigger buildCardActionEvent(String openId, String actionName) {
        CallBackOperator operator = new CallBackOperator();
        operator.setOpenId(openId);

        CallBackAction action = new CallBackAction();
        action.setValue(Map.of("action", actionName));

        P2CardActionTriggerData data = new P2CardActionTriggerData();
        data.setOperator(operator);
        data.setAction(action);

        P2CardActionTrigger event = new P2CardActionTrigger();
        event.setEvent(data);
        return event;
    }
}

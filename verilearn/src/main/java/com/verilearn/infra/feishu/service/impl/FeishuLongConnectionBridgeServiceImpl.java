package com.verilearn.infra.feishu.service.impl;

import com.lark.oapi.event.cardcallback.model.CallBackCard;
import com.lark.oapi.event.cardcallback.model.CallBackToast;
import com.lark.oapi.event.cardcallback.model.P2CardActionTrigger;
import com.lark.oapi.event.cardcallback.model.P2CardActionTriggerData;
import com.lark.oapi.event.cardcallback.model.P2CardActionTriggerResponse;
import com.lark.oapi.service.im.v1.model.EventMessage;
import com.lark.oapi.service.im.v1.model.EventSender;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1Data;
import com.verilearn.infra.feishu.config.FeishuProperties;
import com.verilearn.infra.feishu.dto.FeishuCardActionRequest;
import com.verilearn.infra.feishu.dto.FeishuCardActionResponse;
import com.verilearn.infra.feishu.dto.FeishuCommandResponse;
import com.verilearn.infra.feishu.dto.FeishuEventRequest;
import com.verilearn.infra.feishu.dto.FeishuReplyTarget;
import com.verilearn.infra.feishu.service.FeishuCardService;
import com.verilearn.infra.feishu.service.FeishuCommandService;
import com.verilearn.infra.feishu.service.FeishuLongConnectionBridgeService;
import com.verilearn.infra.feishu.service.FeishuMessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FeishuLongConnectionBridgeServiceImpl implements FeishuLongConnectionBridgeService {

    private static final Logger log = LoggerFactory.getLogger(FeishuLongConnectionBridgeServiceImpl.class);
    private static final String CHAT_TYPE_P2P = "p2p";
    private static final String NON_TEXT_GUIDE = "当前仅支持文本命令消息。发送 /help 查看可用命令。";
    private static final String NON_P2P_GUIDE = """
            当前版本仅支持与 VeriLearn 机器人单聊。
            请在机器人单聊窗口中发送 /help 查看可用命令。
            """.trim();

    private final FeishuProperties feishuProperties;
    private final FeishuCommandService feishuCommandService;
    private final FeishuCardService feishuCardService;
    private final FeishuMessagingService feishuMessagingService;
    private final Set<String> processedMessageIds = ConcurrentHashMap.newKeySet();

    public FeishuLongConnectionBridgeServiceImpl(
            FeishuProperties feishuProperties,
            FeishuCommandService feishuCommandService,
            FeishuCardService feishuCardService,
            FeishuMessagingService feishuMessagingService
    ) {
        this.feishuProperties = feishuProperties;
        this.feishuCommandService = feishuCommandService;
        this.feishuCardService = feishuCardService;
        this.feishuMessagingService = feishuMessagingService;
    }

    @Override
    public void handleMessageEvent(P2MessageReceiveV1 event) {
        P2MessageReceiveV1Data eventData = event == null ? null : event.getEvent();
        EventSender sender = eventData == null ? null : eventData.getSender();
        EventMessage message = eventData == null ? null : eventData.getMessage();
        String openId = sender == null || sender.getSenderId() == null ? null : sender.getSenderId().getOpenId();
        String chatType = message == null ? null : message.getChatType();
        String messageType = message == null ? null : message.getMessageType();
        String messageId = message == null ? null : message.getMessageId();
        String content = message == null ? null : message.getContent();

        log.info("received feishu message event: messageId={}, openId={}, chatType={}, messageType={}, content={}",
                messageId,
                openId,
                chatType,
                messageType,
                content);

        if (openId == null || openId.isBlank() || message == null) {
            log.warn("ignore feishu message event because sender or message is missing: messageId={}", messageId);
            return;
        }

        FeishuReplyTarget replyTarget = FeishuReplyTarget.openId(openId);
        if (!CHAT_TYPE_P2P.equalsIgnoreCase(chatType)) {
            log.info("ignore non-p2p feishu message event: openId={}, chatType={}", openId, chatType);
            feishuMessagingService.sendTextMessage(replyTarget, NON_P2P_GUIDE);
            return;
        }

        if (!"text".equalsIgnoreCase(message.getMessageType())) {
            feishuMessagingService.sendTextMessage(replyTarget, NON_TEXT_GUIDE);
            return;
        }

        if (messageId != null && !messageId.isBlank() && !processedMessageIds.add(messageId)) {
            log.info("ignore duplicate feishu message event: messageId={}", messageId);
            return;
        }
        if (processedMessageIds.size() > 10_000) {
            processedMessageIds.clear();
        }

        FeishuEventRequest request = new FeishuEventRequest();
        request.setToken(feishuProperties.getVerificationToken());

        FeishuEventRequest.FeishuEventHeader header = new FeishuEventRequest.FeishuEventHeader();
        header.setEventType("im.message.receive_v1");
        header.setToken(feishuProperties.getVerificationToken());
        request.setHeader(header);

        FeishuEventRequest.FeishuSenderId senderId = new FeishuEventRequest.FeishuSenderId();
        senderId.setOpenId(openId);
        FeishuEventRequest.FeishuSender requestSender = new FeishuEventRequest.FeishuSender();
        requestSender.setSenderId(senderId);

        FeishuEventRequest.FeishuMessage requestMessage = new FeishuEventRequest.FeishuMessage();
        requestMessage.setMessageType(message.getMessageType());
        requestMessage.setContent(message.getContent());

        FeishuEventRequest.FeishuMessageEvent requestEvent = new FeishuEventRequest.FeishuMessageEvent();
        requestEvent.setSender(requestSender);
        requestEvent.setMessage(requestMessage);
        request.setEvent(requestEvent);

        FeishuCommandResponse response = feishuCommandService.handleCommand(request);
        log.info("feishu command handled successfully: messageId={}, openId={}, replyLength={}",
                messageId,
                openId,
                response.getReplyText() == null ? 0 : response.getReplyText().length());
        feishuMessagingService.sendTextMessage(replyTarget, response.getReplyText());
    }

    @Override
    public P2CardActionTriggerResponse handleCardActionEvent(P2CardActionTrigger event) {
        P2CardActionTriggerData eventData = event == null ? null : event.getEvent();
        FeishuCardActionRequest request = new FeishuCardActionRequest();
        if (eventData != null) {
            request.setOpenId(eventData.getOperator() == null ? null : eventData.getOperator().getOpenId());

            FeishuCardActionRequest.FeishuCardOperator operator = new FeishuCardActionRequest.FeishuCardOperator();
            operator.setOpenId(eventData.getOperator() == null ? null : eventData.getOperator().getOpenId());
            request.setOperator(operator);

            FeishuCardActionRequest.FeishuCardAction action = new FeishuCardActionRequest.FeishuCardAction();
            FeishuCardActionRequest.FeishuCardActionValue value = buildActionValue(eventData);
            action.setValue(value);
            request.setAction(action);
        }

        FeishuCardActionResponse actionResponse = feishuCardService.handleCardAction(request);
        P2CardActionTriggerResponse sdkResponse = new P2CardActionTriggerResponse();
        if (actionResponse.getToastText() != null && !actionResponse.getToastText().isBlank()) {
            CallBackToast toast = new CallBackToast();
            toast.setType("success");
            toast.setContent(actionResponse.getToastText());
            sdkResponse.setToast(toast);
        }
        if (actionResponse.getCard() != null && !actionResponse.getCard().isEmpty()) {
            CallBackCard card = new CallBackCard();
            card.setType("raw");
            card.setData(actionResponse.getCard());
            sdkResponse.setCard(card);
        }
        return sdkResponse;
    }

    private FeishuCardActionRequest.FeishuCardActionValue buildActionValue(P2CardActionTriggerData eventData) {
        FeishuCardActionRequest.FeishuCardActionValue value = new FeishuCardActionRequest.FeishuCardActionValue();
        Map<String, Object> actionValue = eventData.getAction() == null ? null : eventData.getAction().getValue();
        if (actionValue == null || actionValue.isEmpty()) {
            return value;
        }

        value.setActionName(asString(actionValue.get("action")));
        value.setTaskId(asLong(actionValue.get("task_id")));
        value.setConfigId(asLong(actionValue.get("config_id")));
        return value;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}

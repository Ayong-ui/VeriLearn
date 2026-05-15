package com.verilearn.infra.feishu;

import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.event.cardcallback.P2CardActionTriggerHandler;
import com.lark.oapi.event.cardcallback.model.P2CardActionTrigger;
import com.lark.oapi.event.cardcallback.model.P2CardActionTriggerResponse;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.P2ChatAccessEventBotP2pChatEnteredV1;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.ws.Client;
import com.verilearn.infra.feishu.config.FeishuProperties;
import com.verilearn.infra.feishu.service.FeishuLongConnectionBridgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class FeishuLongConnectionStarter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FeishuLongConnectionStarter.class);

    private final FeishuProperties feishuProperties;
    private final FeishuLongConnectionBridgeService bridgeService;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "verilearn-feishu-long-connection");
        thread.setDaemon(true);
        return thread;
    });

    public FeishuLongConnectionStarter(
            FeishuProperties feishuProperties,
            FeishuLongConnectionBridgeService bridgeService
    ) {
        this.feishuProperties = feishuProperties;
        this.bridgeService = bridgeService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!shouldStart()) {
            log.info("skip feishu long connection startup: mode={}, enabled={}",
                    feishuProperties.getMode(),
                    feishuProperties.isLongConnectionEnabled());
            return;
        }
        if (!started.compareAndSet(false, true)) {
            return;
        }

        EventDispatcher dispatcher = EventDispatcher
                .newBuilder(defaultString(feishuProperties.getVerificationToken()), defaultString(feishuProperties.getEncryptKey()))
                .onP2ChatAccessEventBotP2pChatEnteredV1(new ImService.P2ChatAccessEventBotP2pChatEnteredV1Handler() {
                    @Override
                    public void handle(P2ChatAccessEventBotP2pChatEnteredV1 event) {
                        String openId = event == null
                                || event.getEvent() == null
                                || event.getEvent().getOperatorId() == null
                                ? null
                                : event.getEvent().getOperatorId().getOpenId();
                        log.info("received feishu bot p2p chat entered event: openId={}", openId);
                    }
                })
                .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                    @Override
                    public void handle(P2MessageReceiveV1 event) {
                        bridgeService.handleMessageEvent(event);
                    }
                })
                .onP2CardActionTrigger(new P2CardActionTriggerHandler() {
                    @Override
                    public P2CardActionTriggerResponse handle(P2CardActionTrigger event) {
                        return bridgeService.handleCardActionEvent(event);
                    }
                })
                .build();

        Client client = new Client.Builder(feishuProperties.getAppId(), feishuProperties.getAppSecret())
                .eventHandler(dispatcher)
                .autoReconnect(Boolean.TRUE)
                .build();

        executor.submit(() -> {
            log.info("starting feishu long connection client");
            client.start();
        });
    }

    private boolean shouldStart() {
        return feishuProperties.isLongConnectionEnabled()
                && feishuProperties.isLongConnectionMode()
                && isConfigured(feishuProperties.getAppId())
                && isConfigured(feishuProperties.getAppSecret());
    }

    private boolean isConfigured(String value) {
        return value != null && !value.isBlank();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}

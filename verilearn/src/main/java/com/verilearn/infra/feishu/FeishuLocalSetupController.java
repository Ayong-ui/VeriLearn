package com.verilearn.infra.feishu;

import com.verilearn.common.ApiResponse;
import com.verilearn.infra.feishu.config.FeishuProperties;
import com.verilearn.infra.feishu.dto.FeishuLocalSetupResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/feishu")
public class FeishuLocalSetupController {

    private static final String CALLBACK_PATH = "/api/feishu/events";

    private final FeishuProperties feishuProperties;

    public FeishuLocalSetupController(FeishuProperties feishuProperties) {
        this.feishuProperties = feishuProperties;
    }

    @GetMapping("/local-setup")
    public ApiResponse<FeishuLocalSetupResponse> getLocalSetup() {
        FeishuLocalSetupResponse response = new FeishuLocalSetupResponse();
        response.setCallbackPath(CALLBACK_PATH);
        response.setCallbackUrlTemplate("{PUBLIC_BASE_URL}" + CALLBACK_PATH);
        response.setAiConfigPageTemplate("{PUBLIC_BASE_URL}/ai/provider-config-page?openId={feishuOpenId}");
        response.setMode(feishuProperties.getMode());
        response.setAppIdConfigured(isConfigured(feishuProperties.getAppId()));
        response.setAppSecretConfigured(isConfigured(feishuProperties.getAppSecret()));
        response.setVerificationTokenConfigured(isConfigured(feishuProperties.getVerificationToken()));
        response.setSendMessageEnabled(response.isAppIdConfigured() && response.isAppSecretConfigured());
        response.setInboundVerificationEnabled(response.isVerificationTokenConfigured());
        response.setLongConnectionEnabled(feishuProperties.isLongConnectionEnabled() && feishuProperties.isLongConnectionMode());
        response.setLongConnectionReady(response.isLongConnectionEnabled() && response.isSendMessageEnabled());
        response.setSingleChatOnly(true);
        response.setSingleChatReady(response.isLongConnectionReady());
        response.setNextSteps(buildNextSteps(response));
        return ApiResponse.success("feishu local setup queried successfully", response);
    }

    private List<String> buildNextSteps(FeishuLocalSetupResponse response) {
        List<String> nextSteps = new ArrayList<>();
        if (!response.isAppIdConfigured()) {
            nextSteps.add("先配置 VERILEARN_FEISHU_APP_ID。");
        }
        if (!response.isAppSecretConfigured()) {
            nextSteps.add("先配置 VERILEARN_FEISHU_APP_SECRET。");
        }
        if (!response.isVerificationTokenConfigured()) {
            nextSteps.add("先配置 VERILEARN_FEISHU_VERIFICATION_TOKEN，并确保它和飞书后台填写的一致。");
        }
        if (!response.isSendMessageEnabled()) {
            nextSteps.add("当前缺少真实消息发送能力，机器人只能做本地演示，不能真正回消息给飞书用户。");
        }
        if (response.isLongConnectionEnabled()) {
            if (response.isLongConnectionReady()) {
                nextSteps.add("当前已启用飞书长连接模式，单聊收发消息已具备启动条件。");
                nextSteps.add("请直接在飞书中与 VeriLearn 机器人单聊，发送 /start、/today、/submit-demo 或 /ai current。");
                nextSteps.add("当前版本不支持群聊学习流程；如果先在群里 @ 机器人，系统会私聊引导你切到单聊。");
            } else {
                nextSteps.add("如需使用飞书长连接模式，请确保 APP_ID、APP_SECRET 和长连接开关都已配置。");
            }
        } else {
            nextSteps.add("如需使用 HTTP 回调模式，准备一个公网地址，把 {PUBLIC_BASE_URL}/api/feishu/events 填到飞书事件订阅里。");
            nextSteps.add("飞书后台至少订阅 im.message.receive_v1 事件。");
        }
        nextSteps.add("如需演示安全模型切换，把 {PUBLIC_BASE_URL}/ai/provider-config-page?openId={feishuOpenId} 挂到飞书卡片按钮或说明文案里。");
        return nextSteps;
    }

    private boolean isConfigured(String value) {
        return value != null && !value.isBlank();
    }
}

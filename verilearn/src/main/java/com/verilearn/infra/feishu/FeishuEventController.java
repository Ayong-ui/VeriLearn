package com.verilearn.infra.feishu;

import com.verilearn.common.ApiResponse;
import com.verilearn.infra.feishu.dto.FeishuChallengeResponse;
import com.verilearn.infra.feishu.dto.FeishuCommandResponse;
import com.verilearn.infra.feishu.dto.FeishuEventRequest;
import com.verilearn.infra.feishu.service.FeishuCommandService;
import com.verilearn.infra.feishu.service.FeishuMessagingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feishu")
public class FeishuEventController {

    private final FeishuCommandService feishuCommandService;
    private final FeishuMessagingService feishuMessagingService;

    public FeishuEventController(
            FeishuCommandService feishuCommandService,
            FeishuMessagingService feishuMessagingService
    ) {
        this.feishuCommandService = feishuCommandService;
        this.feishuMessagingService = feishuMessagingService;
    }

    @PostMapping("/events")
    public Object handleEvent(@RequestBody FeishuEventRequest request) {
        feishuCommandService.verifyTokenIfNecessary(request);
        if ("url_verification".equals(request.getType())) {
            return new FeishuChallengeResponse(request.getChallenge());
        }

        FeishuCommandResponse response = feishuCommandService.handleCommand(request);
        if (request.getEvent() != null && request.getEvent().getMessage() != null) {
            feishuMessagingService.sendTextMessage(response.getOpenId(), response.getReplyText());
        }
        return ApiResponse.success("feishu command handled successfully", response);
    }
}

package com.verilearn.infra.feishu;

import com.verilearn.common.ApiResponse;
import com.verilearn.infra.feishu.dto.FeishuCardActionRequest;
import com.verilearn.infra.feishu.dto.FeishuCardActionResponse;
import com.verilearn.infra.feishu.dto.FeishuCardPreviewResponse;
import com.verilearn.infra.feishu.service.FeishuCardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feishu/cards")
public class FeishuCardController {

    private final FeishuCardService feishuCardService;

    public FeishuCardController(FeishuCardService feishuCardService) {
        this.feishuCardService = feishuCardService;
    }

    @GetMapping("/learners/{openId}/today-task")
    public ApiResponse<FeishuCardPreviewResponse> previewTodayTaskCard(@PathVariable String openId) {
        return ApiResponse.success("today task card generated successfully", feishuCardService.buildTodayTaskCard(openId));
    }

    @GetMapping("/learners/{openId}/dashboard")
    public ApiResponse<FeishuCardPreviewResponse> previewDashboardCard(@PathVariable String openId) {
        return ApiResponse.success("dashboard card generated successfully", feishuCardService.buildDashboardCard(openId));
    }

    @GetMapping("/learners/{openId}/current-context")
    public ApiResponse<FeishuCardPreviewResponse> previewCurrentContextCard(@PathVariable String openId) {
        return ApiResponse.success("current context card generated successfully", feishuCardService.buildCurrentContextCard(openId));
    }

    @GetMapping("/learners/{openId}/ai-provider")
    public ApiResponse<FeishuCardPreviewResponse> previewAiProviderCard(@PathVariable String openId) {
        return ApiResponse.success("ai provider card generated successfully", feishuCardService.buildAiProviderCard(openId));
    }

    @PostMapping("/callbacks")
    public ApiResponse<FeishuCardActionResponse> handleCardCallback(@RequestBody FeishuCardActionRequest request) {
        return ApiResponse.success("feishu card callback handled successfully", feishuCardService.handleCardAction(request));
    }
}

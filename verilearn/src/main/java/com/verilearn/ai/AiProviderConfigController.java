package com.verilearn.ai;

import com.verilearn.ai.dto.AiProviderConfigResponse;
import com.verilearn.ai.dto.AiProviderConfigUpsertRequest;
import com.verilearn.ai.service.AiProviderConfigService;
import com.verilearn.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/learners/{feishuOpenId}/ai-provider-configs")
public class AiProviderConfigController {

    private final AiProviderConfigService aiProviderConfigService;

    public AiProviderConfigController(AiProviderConfigService aiProviderConfigService) {
        this.aiProviderConfigService = aiProviderConfigService;
    }

    @PostMapping
    public ApiResponse<AiProviderConfigResponse> saveConfig(
            @PathVariable String feishuOpenId,
            @Valid @RequestBody AiProviderConfigUpsertRequest request
    ) {
        return ApiResponse.success(
                "ai provider config saved successfully",
                aiProviderConfigService.saveConfig(feishuOpenId, request)
        );
    }

    @GetMapping
    public ApiResponse<List<AiProviderConfigResponse>> listConfigs(@PathVariable String feishuOpenId) {
        return ApiResponse.success(
                "ai provider configs queried successfully",
                aiProviderConfigService.listConfigs(feishuOpenId)
        );
    }

    @GetMapping("/current")
    public ApiResponse<AiProviderConfigResponse> getCurrentConfig(@PathVariable String feishuOpenId) {
        return ApiResponse.success(
                "current ai provider config queried successfully",
                aiProviderConfigService.getCurrentConfig(feishuOpenId)
        );
    }

    @PostMapping("/{configId}/activate")
    public ApiResponse<AiProviderConfigResponse> activateConfig(
            @PathVariable String feishuOpenId,
            @PathVariable Long configId
    ) {
        return ApiResponse.success(
                "ai provider config activated successfully",
                aiProviderConfigService.activateConfig(feishuOpenId, configId)
        );
    }
}

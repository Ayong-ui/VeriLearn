package com.verilearn.ai;

import com.verilearn.ai.service.AiProviderConfigPageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiProviderConfigPageController {

    private final AiProviderConfigPageService aiProviderConfigPageService;

    public AiProviderConfigPageController(AiProviderConfigPageService aiProviderConfigPageService) {
        this.aiProviderConfigPageService = aiProviderConfigPageService;
    }

    @GetMapping(value = "/ai/provider-config-page", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> renderPage(@RequestParam String openId) {
        return ResponseEntity.ok(aiProviderConfigPageService.buildPage(openId));
    }
}

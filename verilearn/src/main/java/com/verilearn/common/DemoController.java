package com.verilearn.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DemoController {

    @GetMapping("/api/demo")
    public ApiResponse<Map<String, String>> demo() {
        return ApiResponse.success(
                "demo endpoint is ready",
                Map.of("module", "common", "status", "ready")
        );
    }

    @GetMapping("/api/demo/error")
    public ApiResponse<Void> error() {
        throw new IllegalStateException("demo exception for global handler");
    }
}

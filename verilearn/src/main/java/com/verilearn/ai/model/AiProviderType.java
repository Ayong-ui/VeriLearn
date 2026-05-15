package com.verilearn.ai.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum AiProviderType {

    DEEPSEEK,
    OPENAI,
    OPENAI_COMPATIBLE;

    public static AiProviderType resolveOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return DEEPSEEK;
        }
        return Arrays.stream(values())
                .filter(type -> type.name().equals(value.trim().toUpperCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported providerType, supported values: " + supportedValues()));
    }

    public static String supportedValues() {
        return Arrays.stream(values())
                .map(AiProviderType::name)
                .collect(Collectors.joining(", "));
    }
}

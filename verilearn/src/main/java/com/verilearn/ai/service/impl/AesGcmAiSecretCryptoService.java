package com.verilearn.ai.service.impl;

import com.verilearn.ai.config.AiSecurityProperties;
import com.verilearn.ai.service.AiSecretCryptoService;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AesGcmAiSecretCryptoService implements AiSecretCryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final AiSecurityProperties aiSecurityProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmAiSecretCryptoService(AiSecurityProperties aiSecurityProperties) {
        this.aiSecurityProperties = aiSecurityProperties;
    }

    @Override
    public String encrypt(String plainText) {
        requireMasterKey();
        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException("apiKey cannot be blank");
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, buildSecretKey(), new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to encrypt api key", exception);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        requireMasterKey();
        if (cipherText == null || cipherText.isBlank()) {
            throw new IllegalArgumentException("encrypted api key cannot be blank");
        }

        try {
            String[] parts = cipherText.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("invalid encrypted api key format");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, buildSecretKey(), new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] plainBytes = cipher.doFinal(encrypted);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to decrypt api key", exception);
        }
    }

    @Override
    public String mask(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "NOT_CONFIGURED";
        }
        String trimmed = apiKey.trim();
        if (trimmed.length() <= 8) {
            return trimmed.charAt(0) + "***" + trimmed.charAt(trimmed.length() - 1);
        }
        return trimmed.substring(0, 4) + "***" + trimmed.substring(trimmed.length() - 4);
    }

    private SecretKeySpec buildSecretKey() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(aiSecurityProperties.getMasterKey().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }

    private void requireMasterKey() {
        if (aiSecurityProperties.getMasterKey() == null || aiSecurityProperties.getMasterKey().isBlank()) {
            throw new IllegalStateException("VERILEARN_SECRET_MASTER_KEY is not configured");
        }
    }
}

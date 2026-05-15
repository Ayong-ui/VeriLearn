package com.verilearn.ai.service;

public interface AiSecretCryptoService {

    String encrypt(String plainText);

    String decrypt(String cipherText);

    String mask(String apiKey);
}

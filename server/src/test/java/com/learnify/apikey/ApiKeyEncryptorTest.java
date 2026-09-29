package com.learnify.apikey;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiKeyEncryptorTest {

    private final ApiKeyEncryptor encryptor = new ApiKeyEncryptor("test-secret-value");

    @Test
    void encryptsAndDecryptsRoundTrip() {
        String plainText = "sk-my-real-gemini-key";

        String encrypted = encryptor.encrypt(plainText);

        assertThat(encrypted).isNotEqualTo(plainText);
        assertThat(encryptor.decrypt(encrypted)).isEqualTo(plainText);
    }

    @Test
    void sameInputEncryptsDifferentlyEachTime() {
        String encryptedOnce = encryptor.encrypt("same-key");
        String encryptedTwice = encryptor.encrypt("same-key");

        assertThat(encryptedOnce).isNotEqualTo(encryptedTwice);
    }
}

package com.learnify.controller;

import com.learnify.apikey.ApiKeyEncryptor;
import com.learnify.dto.request.SetApiKeyRequest;
import com.learnify.dto.response.ApiKeyStatusResponse;
import com.learnify.entity.UserApiKey;
import com.learnify.repository.UserApiKeyRepository;
import com.learnify.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/api-key")
public class ApiKeyController {

    private static final String DEFAULT_PROVIDER = "gemini";

    private final UserApiKeyRepository userApiKeyRepository;
    private final ApiKeyEncryptor encryptor;
    private final CurrentUserProvider currentUserProvider;

    public ApiKeyController(
        UserApiKeyRepository userApiKeyRepository,
        ApiKeyEncryptor encryptor,
        CurrentUserProvider currentUserProvider
    ) {
        this.userApiKeyRepository = userApiKeyRepository;
        this.encryptor = encryptor;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ApiKeyStatusResponse status() {
        boolean configured = userApiKeyRepository.findByUserId(currentUserProvider.currentUserId()).isPresent();
        return new ApiKeyStatusResponse(configured);
    }

    @PutMapping
    @Transactional
    public ApiKeyStatusResponse set(@Valid @RequestBody SetApiKeyRequest request) {
        var ownerId = currentUserProvider.currentUserId();
        UserApiKey key = userApiKeyRepository.findByUserId(ownerId).orElseGet(UserApiKey::new);
        key.setUserId(ownerId);
        key.setProvider(DEFAULT_PROVIDER);
        key.setEncryptedKey(encryptor.encrypt(request.apiKey()));
        userApiKeyRepository.save(key);
        return new ApiKeyStatusResponse(true);
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<Void> delete() {
        userApiKeyRepository.deleteByUserId(currentUserProvider.currentUserId());
        return ResponseEntity.noContent().build();
    }
}

package com.learnify.controller;

import com.learnify.api.ApiKeysApi;
import com.learnify.api.model.ApiKeyStatusResponse;
import com.learnify.api.model.SetApiKeyRequest;
import com.learnify.apikey.ApiKeyEncryptor;
import com.learnify.entity.UserApiKey;
import com.learnify.repository.UserApiKeyRepository;
import com.learnify.security.CurrentUserProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ApiKeyController implements ApiKeysApi {

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

    @Override
    public ResponseEntity<ApiKeyStatusResponse> getApiKeyStatus() {
        boolean configured = userApiKeyRepository.findByUserId(currentUserProvider.currentUserId()).isPresent();
        return ResponseEntity.ok(new ApiKeyStatusResponse(configured));
    }

    @Override
    @Transactional
    public ResponseEntity<ApiKeyStatusResponse> setApiKey(SetApiKeyRequest setApiKeyRequest) {
        var ownerId = currentUserProvider.currentUserId();
        UserApiKey key = userApiKeyRepository.findByUserId(ownerId).orElseGet(UserApiKey::new);
        key.setUserId(ownerId);
        key.setProvider(DEFAULT_PROVIDER);
        key.setEncryptedKey(encryptor.encrypt(setApiKeyRequest.getApiKey()));
        userApiKeyRepository.save(key);
        return ResponseEntity.ok(new ApiKeyStatusResponse(true));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteApiKey() {
        userApiKeyRepository.deleteByUserId(currentUserProvider.currentUserId());
        return ResponseEntity.noContent().build();
    }
}

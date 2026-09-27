package com.learnify.apikey;

import com.learnify.repository.UserApiKeyRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyResolver {

    public enum Source {
        USER,
        DEFAULT
    }

    public record ResolvedKey(String value, Source source) {
    }

    private final UserApiKeyRepository userApiKeyRepository;
    private final ApiKeyEncryptor encryptor;
    private final String defaultApiKey;

    public ApiKeyResolver(
        UserApiKeyRepository userApiKeyRepository,
        ApiKeyEncryptor encryptor,
        @Value("${gemini.api-key}") String defaultApiKey
    ) {
        this.userApiKeyRepository = userApiKeyRepository;
        this.encryptor = encryptor;
        this.defaultApiKey = defaultApiKey;
    }

    public ResolvedKey resolve(UUID userId) {
        return userApiKeyRepository.findByUserId(userId)
            .map(key -> new ResolvedKey(encryptor.decrypt(key.getEncryptedKey()), Source.USER))
            .orElse(new ResolvedKey(defaultApiKey, Source.DEFAULT));
    }

    public String defaultKey() {
        return defaultApiKey;
    }
}

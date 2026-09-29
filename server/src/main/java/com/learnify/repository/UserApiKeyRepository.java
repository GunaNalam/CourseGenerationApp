package com.learnify.repository;

import com.learnify.entity.UserApiKey;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserApiKeyRepository extends JpaRepository<UserApiKey, UUID> {

    Optional<UserApiKey> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}

package com.learnify.repository;

import com.learnify.entity.GenerationRequestLog;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationRequestLogRepository extends JpaRepository<GenerationRequestLog, UUID> {

    long countByUserIdAndCreatedAtAfter(UUID userId, Instant after);
}

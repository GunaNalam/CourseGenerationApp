package com.learnify.repository;

import com.learnify.entity.Module;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleRepository extends JpaRepository<Module, UUID> {

    Optional<Module> findByIdAndCourse_OwnerId(UUID id, UUID ownerId);

    List<Module> findAllByCourse_IdOrderByOrderIndex(UUID courseId);
}

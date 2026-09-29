package com.learnify.repository;

import com.learnify.entity.Course;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    Optional<Course> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Course> findAllByOwnerId(UUID ownerId);
}

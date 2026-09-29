package com.learnify.repository;

import com.learnify.entity.Lesson;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    Optional<Lesson> findByIdAndModule_Course_OwnerId(UUID id, UUID ownerId);

    List<Lesson> findAllByModule_IdOrderByOrderIndex(UUID moduleId);

    /**
     * All lessons for every module in a course, in one round trip — used by the
     * course tree endpoint instead of one query per module (each extra round trip
     * costs real latency against a remote DB).
     */
    @Query("""
        SELECT l FROM Lesson l
        JOIN FETCH l.module m
        WHERE m.course.id = :courseId
        ORDER BY m.orderIndex, l.orderIndex
        """)
    List<Lesson> findAllByCourseIdOrderByModuleAndLesson(UUID courseId);
}

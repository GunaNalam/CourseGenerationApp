package com.learnify.service;

import com.learnify.dto.request.CreateCourseRequest;
import com.learnify.entity.Course;
import com.learnify.entity.Lesson;
import com.learnify.entity.Module;
import com.learnify.exception.NotFoundException;
import com.learnify.repository.CourseRepository;
import com.learnify.repository.LessonRepository;
import com.learnify.repository.ModuleRepository;
import com.learnify.security.CurrentUserProvider;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final CurrentUserProvider currentUserProvider;

    public CourseService(
        CourseRepository courseRepository,
        ModuleRepository moduleRepository,
        LessonRepository lessonRepository,
        CurrentUserProvider currentUserProvider
    ) {
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public Course create(CreateCourseRequest request) {
        Course course = new Course();
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setOwnerId(currentUserProvider.currentUserId());
        if (request.tags() != null) {
            course.setTags(request.tags());
        }
        return courseRepository.save(course);
    }

    public Course getOwnedOrThrow(UUID courseId) {
        return courseRepository.findByIdAndOwnerId(courseId, currentUserProvider.currentUserId())
            .orElseThrow(() -> new NotFoundException("Course not found: " + courseId));
    }

    public List<Course> listMine() {
        return courseRepository.findAllByOwnerId(currentUserProvider.currentUserId());
    }

    /**
     * Course + all modules + all lessons in ONE transaction — every Spring Data
     * repository call is transactional on its own by default (its own begin/commit
     * round trip to the DB), which is invisible on a local Postgres but very much
     * not against a remote one. Composing the whole read here means one begin,
     * three queries, one commit, instead of three separate transactions plus a
     * redundant ownership re-check that CourseController.tree() used to trigger by
     * going through ModuleService.listByCourse (which re-verifies course ownership
     * that had already just been checked).
     */
    @Transactional(readOnly = true)
    public CourseTree getTree(UUID courseId) {
        Course course = getOwnedOrThrow(courseId);
        List<Module> modules = moduleRepository.findAllByCourse_IdOrderByOrderIndex(courseId);
        List<Lesson> lessons = lessonRepository.findAllByCourseIdOrderByModuleAndLesson(courseId);
        return new CourseTree(course, modules, lessons);
    }

    public record CourseTree(Course course, List<Module> modules, List<Lesson> lessons) {
    }
}

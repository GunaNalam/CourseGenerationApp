package com.learnify.service;

import com.learnify.dto.response.CourseExportResponse;
import com.learnify.entity.Course;
import com.learnify.entity.Lesson;
import com.learnify.entity.Module;
import com.learnify.repository.LessonRepository;
import com.learnify.repository.ModuleRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CourseExportService {

    private final CourseService courseService;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;

    public CourseExportService(CourseService courseService, ModuleRepository moduleRepository, LessonRepository lessonRepository) {
        this.courseService = courseService;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
    }

    public CourseExportResponse export(UUID courseId) {
        Course course = courseService.getOwnedOrThrow(courseId);

        var moduleExports = moduleRepository.findAllByCourse_IdOrderByOrderIndex(courseId).stream()
            .map(this::toModuleExport)
            .toList();

        return new CourseExportResponse(course.getTitle(), course.getDescription(), course.getTags(), moduleExports);
    }

    private CourseExportResponse.ModuleExport toModuleExport(Module module) {
        var lessonExports = lessonRepository.findAllByModule_IdOrderByOrderIndex(module.getId()).stream()
            .map(this::toLessonExport)
            .toList();
        return new CourseExportResponse.ModuleExport(module.getTitle(), module.getOrderIndex(), lessonExports);
    }

    private CourseExportResponse.LessonExport toLessonExport(Lesson lesson) {
        return new CourseExportResponse.LessonExport(
            lesson.getTitle(), lesson.getOrderIndex(), lesson.getObjectives(), lesson.getContent(), lesson.isEnriched()
        );
    }
}

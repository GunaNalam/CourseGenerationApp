package com.learnify.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnify.api.model.ContentBlock;
import com.learnify.api.model.CourseExportResponse;
import com.learnify.api.model.CourseExportResponseLessonExport;
import com.learnify.api.model.CourseExportResponseModuleExport;
import com.learnify.entity.Course;
import com.learnify.entity.Lesson;
import com.learnify.entity.Module;
import com.learnify.repository.LessonRepository;
import com.learnify.repository.ModuleRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CourseExportService {

    private final CourseService courseService;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final ObjectMapper objectMapper;

    public CourseExportService(
        CourseService courseService,
        ModuleRepository moduleRepository,
        LessonRepository lessonRepository,
        ObjectMapper objectMapper
    ) {
        this.courseService = courseService;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.objectMapper = objectMapper;
    }

    public CourseExportResponse export(UUID courseId) {
        Course course = courseService.getOwnedOrThrow(courseId);

        var moduleExports = moduleRepository.findAllByCourse_IdOrderByOrderIndex(courseId).stream()
            .map(this::toModuleExport)
            .toList();

        return new CourseExportResponse(course.getTitle(), course.getDescription(), course.getTags(), moduleExports);
    }

    private CourseExportResponseModuleExport toModuleExport(Module module) {
        var lessonExports = lessonRepository.findAllByModule_IdOrderByOrderIndex(module.getId()).stream()
            .map(this::toLessonExport)
            .toList();
        return new CourseExportResponseModuleExport(module.getTitle(), module.getOrderIndex(), lessonExports);
    }

    private CourseExportResponseLessonExport toLessonExport(Lesson lesson) {
        var content = lesson.getContent().stream().map(this::toContentBlock).toList();
        return new CourseExportResponseLessonExport(
            lesson.getTitle(), lesson.getOrderIndex(), lesson.getObjectives(), content, lesson.isEnriched()
        );
    }

    private ContentBlock toContentBlock(Map<String, Object> block) {
        return objectMapper.convertValue(block, ContentBlock.class);
    }
}

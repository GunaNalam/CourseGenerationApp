package com.learnify.controller;

import com.learnify.api.CoursesApi;
import com.learnify.api.model.CourseExportResponse;
import com.learnify.api.model.CourseResponse;
import com.learnify.api.model.CourseTreeResponse;
import com.learnify.api.model.CreateCourseRequest;
import com.learnify.api.model.LessonSummaryResponse;
import com.learnify.api.model.ModuleWithLessonsResponse;
import com.learnify.entity.Course;
import com.learnify.entity.Lesson;
import com.learnify.service.CourseExportService;
import com.learnify.service.CourseService;
import com.learnify.service.CourseService.CourseTree;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CourseController implements CoursesApi {

    private final CourseService courseService;
    private final CourseExportService courseExportService;

    public CourseController(CourseService courseService, CourseExportService courseExportService) {
        this.courseService = courseService;
        this.courseExportService = courseExportService;
    }

    @Override
    public ResponseEntity<CourseResponse> createCourse(CreateCourseRequest createCourseRequest) {
        Course course = courseService.create(createCourseRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(course));
    }

    @Override
    public ResponseEntity<CourseResponse> getCourse(UUID id) {
        return ResponseEntity.ok(toResponse(courseService.getOwnedOrThrow(id)));
    }

    @Override
    public ResponseEntity<List<CourseResponse>> listMyCourses() {
        var responses = courseService.listMine().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<CourseExportResponse> exportCourse(UUID id) {
        return ResponseEntity.ok(courseExportService.export(id));
    }

    @Override
    public ResponseEntity<CourseTreeResponse> getCourseTree(UUID id) {
        CourseTree tree = courseService.getTree(id);

        Map<UUID, List<LessonSummaryResponse>> lessonsByModuleId = tree.lessons().stream()
            .collect(Collectors.groupingBy(
                lesson -> lesson.getModule().getId(),
                Collectors.mapping(this::toSummary, Collectors.toList())
            ));

        List<ModuleWithLessonsResponse> moduleResponses = tree.modules().stream()
            .map(module -> new ModuleWithLessonsResponse(
                module.getId(), module.getTitle(), module.getOrderIndex(),
                lessonsByModuleId.getOrDefault(module.getId(), List.of())
            ))
            .toList();

        Course course = tree.course();
        var response = new CourseTreeResponse(
            course.getId(), course.getTitle(), course.getDescription(), course.getTags(),
            course.getCreatedAt().atOffset(ZoneOffset.UTC), moduleResponses
        );
        return ResponseEntity.ok(response);
    }

    private CourseResponse toResponse(Course course) {
        return new CourseResponse(
            course.getId(), course.getTitle(), course.getDescription(), course.getTags(),
            course.getCreatedAt().atOffset(ZoneOffset.UTC)
        );
    }

    private LessonSummaryResponse toSummary(Lesson lesson) {
        return new LessonSummaryResponse(
            lesson.getId(), lesson.getTitle(), lesson.getOrderIndex(),
            lesson.isEnriched(), lesson.isCompleted(), lesson.isBookmarked()
        );
    }
}

package com.learnify.controller;

import com.learnify.dto.request.CreateCourseRequest;
import com.learnify.dto.response.CourseExportResponse;
import com.learnify.dto.response.CourseResponse;
import com.learnify.dto.response.CourseTreeResponse;
import com.learnify.dto.response.LessonSummaryResponse;
import com.learnify.dto.response.ModuleWithLessonsResponse;
import com.learnify.service.CourseExportService;
import com.learnify.service.CourseService;
import com.learnify.service.CourseService.CourseTree;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService courseService;
    private final CourseExportService courseExportService;

    public CourseController(CourseService courseService, CourseExportService courseExportService) {
        this.courseService = courseService;
        this.courseExportService = courseExportService;
    }

    @PostMapping
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CreateCourseRequest request) {
        CourseResponse response = CourseResponse.from(courseService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public CourseResponse get(@PathVariable UUID id) {
        return CourseResponse.from(courseService.getOwnedOrThrow(id));
    }

    @GetMapping
    public List<CourseResponse> listMine() {
        return courseService.listMine().stream().map(CourseResponse::from).toList();
    }

    @GetMapping("/{id}/export")
    public CourseExportResponse export(@PathVariable UUID id) {
        return courseExportService.export(id);
    }

    @GetMapping("/{id}/tree")
    public CourseTreeResponse tree(@PathVariable UUID id) {
        CourseTree tree = courseService.getTree(id);

        Map<UUID, List<LessonSummaryResponse>> lessonsByModuleId = tree.lessons().stream()
            .collect(Collectors.groupingBy(
                lesson -> lesson.getModule().getId(),
                Collectors.mapping(LessonSummaryResponse::from, Collectors.toList())
            ));

        List<ModuleWithLessonsResponse> moduleResponses = tree.modules().stream()
            .map(module -> ModuleWithLessonsResponse.from(
                module,
                lessonsByModuleId.getOrDefault(module.getId(), List.of())
            ))
            .toList();

        return CourseTreeResponse.from(tree.course(), moduleResponses);
    }
}

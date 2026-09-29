package com.learnify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnify.api.LessonsApi;
import com.learnify.api.model.ContentBlock;
import com.learnify.api.model.CreateLessonRequest;
import com.learnify.api.model.LessonResponse;
import com.learnify.api.model.LessonSummaryResponse;
import com.learnify.api.model.UpdateLessonStateRequest;
import com.learnify.entity.Lesson;
import com.learnify.service.LessonAudioService;
import com.learnify.service.LessonService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class LessonController implements LessonsApi {

    private final LessonService lessonService;
    private final LessonAudioService lessonAudioService;
    private final ObjectMapper objectMapper;

    public LessonController(LessonService lessonService, LessonAudioService lessonAudioService, ObjectMapper objectMapper) {
        this.lessonService = lessonService;
        this.lessonAudioService = lessonAudioService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResponseEntity<LessonResponse> createLesson(UUID courseId, UUID moduleId, CreateLessonRequest createLessonRequest) {
        Lesson lesson = lessonService.create(moduleId, createLessonRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(lesson));
    }

    @Override
    public ResponseEntity<List<LessonSummaryResponse>> listLessons(UUID courseId, UUID moduleId) {
        var responses = lessonService.listByModule(moduleId).stream().map(this::toSummary).toList();
        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<LessonResponse> getLesson(UUID courseId, UUID moduleId, UUID lessonId) {
        return ResponseEntity.ok(toResponse(lessonService.getOwnedOrThrow(lessonId)));
    }

    @Override
    public ResponseEntity<LessonResponse> updateLessonState(
        UUID courseId, UUID moduleId, UUID lessonId, UpdateLessonStateRequest updateLessonStateRequest
    ) {
        var updated = lessonService.updateState(
            lessonId, updateLessonStateRequest.getCompleted(), updateLessonStateRequest.getBookmarked()
        );
        return ResponseEntity.ok(toResponse(updated));
    }

    @Override
    public ResponseEntity<Resource> getLessonAudio(UUID courseId, UUID moduleId, UUID lessonId) {
        byte[] wav = lessonAudioService.generateHinglishAudio(lessonId);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("audio/wav"))
            .body(new ByteArrayResource(wav));
    }

    private LessonResponse toResponse(Lesson lesson) {
        List<ContentBlock> content = lesson.getContent().stream().map(this::toContentBlock).toList();
        return new LessonResponse(
            lesson.getId(), lesson.getTitle(), lesson.getOrderIndex(), lesson.getObjectives(), content,
            lesson.isEnriched(), lesson.isCompleted(), lesson.isBookmarked()
        );
    }

    private LessonSummaryResponse toSummary(Lesson lesson) {
        return new LessonSummaryResponse(
            lesson.getId(), lesson.getTitle(), lesson.getOrderIndex(),
            lesson.isEnriched(), lesson.isCompleted(), lesson.isBookmarked()
        );
    }

    private ContentBlock toContentBlock(Map<String, Object> map) {
        return objectMapper.convertValue(map, ContentBlock.class);
    }
}

package com.learnify.controller;

import com.learnify.dto.request.CreateLessonRequest;
import com.learnify.dto.request.UpdateLessonStateRequest;
import com.learnify.dto.response.LessonResponse;
import com.learnify.dto.response.LessonSummaryResponse;
import com.learnify.service.LessonAudioService;
import com.learnify.service.LessonService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/modules/{moduleId}/lessons")
public class LessonController {

    private final LessonService lessonService;
    private final LessonAudioService lessonAudioService;

    public LessonController(LessonService lessonService, LessonAudioService lessonAudioService) {
        this.lessonService = lessonService;
        this.lessonAudioService = lessonAudioService;
    }

    @PostMapping
    public ResponseEntity<LessonResponse> create(
        @PathVariable UUID courseId,
        @PathVariable UUID moduleId,
        @Valid @RequestBody CreateLessonRequest request
    ) {
        LessonResponse response = LessonResponse.from(lessonService.create(moduleId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<LessonSummaryResponse> list(@PathVariable UUID courseId, @PathVariable UUID moduleId) {
        return lessonService.listByModule(moduleId).stream().map(LessonSummaryResponse::from).toList();
    }

    @GetMapping("/{lessonId}")
    public LessonResponse get(
        @PathVariable UUID courseId,
        @PathVariable UUID moduleId,
        @PathVariable UUID lessonId
    ) {
        return LessonResponse.from(lessonService.getOwnedOrThrow(lessonId));
    }

    @PatchMapping("/{lessonId}")
    public LessonResponse updateState(
        @PathVariable UUID courseId,
        @PathVariable UUID moduleId,
        @PathVariable UUID lessonId,
        @RequestBody UpdateLessonStateRequest request
    ) {
        return LessonResponse.from(lessonService.updateState(lessonId, request.completed(), request.bookmarked()));
    }

    @GetMapping(value = "/{lessonId}/audio", produces = "audio/wav")
    public ResponseEntity<byte[]> hinglishAudio(
        @PathVariable UUID courseId,
        @PathVariable UUID moduleId,
        @PathVariable UUID lessonId
    ) {
        byte[] wav = lessonAudioService.generateHinglishAudio(lessonId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("audio/wav")).body(wav);
    }
}

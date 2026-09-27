package com.learnify.service;

import com.learnify.dto.request.CreateLessonRequest;
import com.learnify.entity.Lesson;
import com.learnify.entity.Module;
import com.learnify.exception.NotFoundException;
import com.learnify.repository.LessonRepository;
import com.learnify.security.CurrentUserProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ModuleService moduleService;
    private final CurrentUserProvider currentUserProvider;

    public LessonService(
        LessonRepository lessonRepository,
        ModuleService moduleService,
        CurrentUserProvider currentUserProvider
    ) {
        this.lessonRepository = lessonRepository;
        this.moduleService = moduleService;
        this.currentUserProvider = currentUserProvider;
    }

    public Lesson create(UUID moduleId, CreateLessonRequest request) {
        Module module = moduleService.getOwnedOrThrow(moduleId);
        Lesson lesson = new Lesson();
        lesson.setTitle(request.title());
        lesson.setOrderIndex(request.orderIndex());
        lesson.setModule(module);
        if (request.objectives() != null) {
            lesson.setObjectives(new ArrayList<>(request.objectives()));
        }
        if (request.content() != null) {
            lesson.setContent(new ArrayList<>(request.content()));
        }
        return lessonRepository.save(lesson);
    }

    public Lesson getOwnedOrThrow(UUID lessonId) {
        return lessonRepository.findByIdAndModule_Course_OwnerId(lessonId, currentUserProvider.currentUserId())
            .orElseThrow(() -> new NotFoundException("Lesson not found: " + lessonId));
    }

    public List<Lesson> listByModule(UUID moduleId) {
        moduleService.getOwnedOrThrow(moduleId);
        return lessonRepository.findAllByModule_IdOrderByOrderIndex(moduleId);
    }

    public Lesson updateState(UUID lessonId, Boolean completed, Boolean bookmarked) {
        Lesson lesson = getOwnedOrThrow(lessonId);
        if (completed != null) {
            lesson.setCompleted(completed);
        }
        if (bookmarked != null) {
            lesson.setBookmarked(bookmarked);
        }
        return lessonRepository.save(lesson);
    }

    public void cacheHinglishAudio(Lesson lesson, String hinglishText, byte[] hinglishAudio) {
        lesson.setHinglishText(hinglishText);
        lesson.setHinglishAudio(hinglishAudio);
        lessonRepository.save(lesson);
    }
}

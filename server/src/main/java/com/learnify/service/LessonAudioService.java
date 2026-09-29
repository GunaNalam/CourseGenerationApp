package com.learnify.service;

import com.learnify.apikey.ApiKeyResolver;
import com.learnify.entity.Lesson;
import com.learnify.integration.gemini.GeminiTtsClient;
import com.learnify.integration.gemini.TranslationClient;
import com.learnify.security.CurrentUserProvider;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LessonAudioService {

    private static final String DEFAULT_VOICE = "Kore";

    private final LessonService lessonService;
    private final TranslationClient translationClient;
    private final GeminiTtsClient ttsClient;
    private final ApiKeyResolver apiKeyResolver;
    private final CurrentUserProvider currentUserProvider;

    public LessonAudioService(
        LessonService lessonService,
        TranslationClient translationClient,
        GeminiTtsClient ttsClient,
        ApiKeyResolver apiKeyResolver,
        CurrentUserProvider currentUserProvider
    ) {
        this.lessonService = lessonService;
        this.translationClient = translationClient;
        this.ttsClient = ttsClient;
        this.apiKeyResolver = apiKeyResolver;
        this.currentUserProvider = currentUserProvider;
    }

    public byte[] generateHinglishAudio(UUID lessonId) {
        Lesson lesson = lessonService.getOwnedOrThrow(lessonId);

        byte[] cachedAudio = lesson.getHinglishAudio();
        if (cachedAudio != null) {
            return cachedAudio;
        }

        String englishText = extractPlainText(lesson.getContent());
        String apiKey = apiKeyResolver.resolve(currentUserProvider.currentUserId()).value();

        String hinglishText = translationClient.translateToHinglish(englishText, apiKey);
        byte[] audio = ttsClient.synthesizeSpeech(hinglishText, DEFAULT_VOICE, apiKey);

        lessonService.cacheHinglishAudio(lesson, hinglishText, audio);
        return audio;
    }

    private String extractPlainText(List<Map<String, Object>> content) {
        StringBuilder builder = new StringBuilder();
        for (Map<String, Object> block : content) {
            Object type = block.get("type");
            if ("heading".equals(type) || "paragraph".equals(type)) {
                Object text = block.get("text");
                if (text != null) {
                    builder.append(text).append(". ");
                }
            }
        }
        return builder.toString();
    }
}

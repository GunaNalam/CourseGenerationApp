package com.learnify.integration.gemini;

import com.learnify.ai.GeminiClient;
import org.springframework.stereotype.Component;

/**
 * Reuses GeminiClient's plain-text generation (already proven for course/lesson
 * generation) with a translation-specific prompt — no separate HTTP client needed for
 * this half of Milestone 10.
 */
@Component
public class TranslationClient {

    private final GeminiClient geminiClient;

    public TranslationClient(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public String translateToHinglish(String englishText, String apiKey) {
        String prompt = """
            Translate the following English lesson text into Hinglish (Hindi written in \
            Latin/Roman script, naturally mixed with common English words, the way it's \
            actually spoken) so a student more comfortable in Hindi than English can follow \
            along. Keep any code, variable names, or technical terms unchanged. Return ONLY \
            the translated text, no preamble, no notes.

            """ + englishText;
        return geminiClient.generate(prompt, apiKey);
    }
}

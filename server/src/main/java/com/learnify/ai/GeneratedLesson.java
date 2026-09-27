package com.learnify.ai;

import java.util.List;
import java.util.Map;

public record GeneratedLesson(
    String title,
    List<String> objectives,
    List<Map<String, Object>> content
) {
}

package com.learnify.ai;

import java.util.List;

public record GeneratedCourse(
    String title,
    String description,
    List<String> tags,
    List<GeneratedModule> modules
) {
}

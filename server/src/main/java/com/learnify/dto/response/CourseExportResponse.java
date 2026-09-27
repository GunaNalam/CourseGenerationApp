package com.learnify.dto.response;

import java.util.List;

public record CourseExportResponse(
    String title,
    String description,
    List<String> tags,
    List<ModuleExport> modules
) {

    public record ModuleExport(String title, int orderIndex, List<LessonExport> lessons) {
    }

    public record LessonExport(
        String title,
        int orderIndex,
        List<String> objectives,
        List<java.util.Map<String, Object>> content,
        boolean isEnriched
    ) {
    }
}

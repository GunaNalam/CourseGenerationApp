package com.learnify.dto.request;

/**
 * Partial update — only non-null fields are applied, so the frontend can toggle
 * "completed" and "bookmarked" independently with the same endpoint.
 */
public record UpdateLessonStateRequest(Boolean completed, Boolean bookmarked) {
}

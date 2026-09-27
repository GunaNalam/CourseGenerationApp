package com.learnify.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JsonResponseValidator {

    private final ObjectMapper objectMapper;

    public JsonResponseValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public GeneratedCourse parseCourse(String raw) {
        JsonNode root = parse(raw);
        requireText(root, "title");

        JsonNode modulesNode = root.path("modules");
        if (!modulesNode.isArray() || modulesNode.isEmpty()) {
            throw new AiGenerationException("AI response missing a non-empty 'modules' array");
        }

        List<GeneratedModule> modules = new ArrayList<>();
        for (JsonNode moduleNode : modulesNode) {
            requireText(moduleNode, "title");
            JsonNode lessonsNode = moduleNode.path("lessons");
            if (!lessonsNode.isArray() || lessonsNode.isEmpty()) {
                throw new AiGenerationException("AI response module missing a non-empty 'lessons' array");
            }
            List<String> lessons = new ArrayList<>();
            lessonsNode.forEach(lesson -> lessons.add(lesson.asText()));
            modules.add(new GeneratedModule(moduleNode.path("title").asText(), lessons));
        }

        List<String> tags = new ArrayList<>();
        root.path("tags").forEach(tag -> tags.add(tag.asText()));

        return new GeneratedCourse(root.path("title").asText(), root.path("description").asText(""), tags, modules);
    }

    public GeneratedLesson parseLesson(String raw) {
        JsonNode root = parse(raw);
        requireText(root, "title");

        JsonNode contentNode = root.path("content");
        if (!contentNode.isArray() || contentNode.isEmpty()) {
            throw new AiGenerationException("AI response missing a non-empty 'content' array");
        }

        List<Map<String, Object>> content = new ArrayList<>();
        for (JsonNode block : contentNode) {
            String type = block.path("type").asText(null);
            if (!BlockType.isValid(type)) {
                throw new AiGenerationException("AI response content block has invalid or missing 'type': " + type);
            }
            content.add(objectMapper.convertValue(block, new TypeReference<Map<String, Object>>() {
            }));
        }

        List<String> objectives = new ArrayList<>();
        root.path("objectives").forEach(objective -> objectives.add(objective.asText()));

        return new GeneratedLesson(root.path("title").asText(), objectives, content);
    }

    private JsonNode parse(String raw) {
        try {
            return objectMapper.readTree(stripMarkdownFence(raw));
        } catch (JsonProcessingException e) {
            throw new AiGenerationException("AI response was not valid JSON", e);
        }
    }

    private void requireText(JsonNode node, String field) {
        if (!node.hasNonNull(field) || node.path(field).asText().isBlank()) {
            throw new AiGenerationException("AI response missing required field: " + field);
        }
    }

    private String stripMarkdownFence(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}

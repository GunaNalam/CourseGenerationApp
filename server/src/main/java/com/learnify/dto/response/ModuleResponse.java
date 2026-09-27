package com.learnify.dto.response;

import com.learnify.entity.Module;
import java.util.UUID;

public record ModuleResponse(
    UUID id,
    String title,
    int orderIndex
) {

    public static ModuleResponse from(Module module) {
        return new ModuleResponse(module.getId(), module.getTitle(), module.getOrderIndex());
    }
}

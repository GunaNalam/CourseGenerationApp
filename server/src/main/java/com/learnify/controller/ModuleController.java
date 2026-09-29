package com.learnify.controller;

import com.learnify.api.ModulesApi;
import com.learnify.api.model.CreateModuleRequest;
import com.learnify.api.model.ModuleResponse;
import com.learnify.entity.Module;
import com.learnify.service.ModuleService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ModuleController implements ModulesApi {

    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @Override
    public ResponseEntity<ModuleResponse> createModule(UUID courseId, CreateModuleRequest createModuleRequest) {
        Module module = moduleService.create(courseId, createModuleRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(module));
    }

    @Override
    public ResponseEntity<List<ModuleResponse>> listModules(UUID courseId) {
        var responses = moduleService.listByCourse(courseId).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(responses);
    }

    private ModuleResponse toResponse(Module module) {
        return new ModuleResponse(module.getId(), module.getTitle(), module.getOrderIndex());
    }
}

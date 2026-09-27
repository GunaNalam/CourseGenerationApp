package com.learnify.controller;

import com.learnify.dto.request.CreateModuleRequest;
import com.learnify.dto.response.ModuleResponse;
import com.learnify.service.ModuleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/modules")
public class ModuleController {

    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @PostMapping
    public ResponseEntity<ModuleResponse> create(
        @PathVariable UUID courseId,
        @Valid @RequestBody CreateModuleRequest request
    ) {
        ModuleResponse response = ModuleResponse.from(moduleService.create(courseId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<ModuleResponse> list(@PathVariable UUID courseId) {
        return moduleService.listByCourse(courseId).stream().map(ModuleResponse::from).toList();
    }
}

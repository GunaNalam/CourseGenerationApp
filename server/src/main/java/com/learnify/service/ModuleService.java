package com.learnify.service;

import com.learnify.dto.request.CreateModuleRequest;
import com.learnify.entity.Course;
import com.learnify.entity.Module;
import com.learnify.exception.NotFoundException;
import com.learnify.repository.ModuleRepository;
import com.learnify.security.CurrentUserProvider;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final CourseService courseService;
    private final CurrentUserProvider currentUserProvider;

    public ModuleService(
        ModuleRepository moduleRepository,
        CourseService courseService,
        CurrentUserProvider currentUserProvider
    ) {
        this.moduleRepository = moduleRepository;
        this.courseService = courseService;
        this.currentUserProvider = currentUserProvider;
    }

    public Module create(UUID courseId, CreateModuleRequest request) {
        Course course = courseService.getOwnedOrThrow(courseId);
        Module module = new Module();
        module.setTitle(request.title());
        module.setOrderIndex(request.orderIndex());
        module.setCourse(course);
        return moduleRepository.save(module);
    }

    public Module getOwnedOrThrow(UUID moduleId) {
        return moduleRepository.findByIdAndCourse_OwnerId(moduleId, currentUserProvider.currentUserId())
            .orElseThrow(() -> new NotFoundException("Module not found: " + moduleId));
    }

    public List<Module> listByCourse(UUID courseId) {
        courseService.getOwnedOrThrow(courseId);
        return moduleRepository.findAllByCourse_IdOrderByOrderIndex(courseId);
    }
}

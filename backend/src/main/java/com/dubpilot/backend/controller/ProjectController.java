package com.dubpilot.backend.controller;

import com.dubpilot.backend.dto.ProjectProgressUpdateRequest;
import com.dubpilot.backend.dto.ProjectStatusResponse;
import com.dubpilot.backend.dto.ProjectSummaryResponse;
import com.dubpilot.backend.dto.ProjectUpdateRequest;
import com.dubpilot.backend.entity.Episode;
import com.dubpilot.backend.entity.ProductionNote;
import com.dubpilot.backend.entity.Project;
import com.dubpilot.backend.entity.ProjectStage;
import com.dubpilot.backend.repository.EpisodeRepository;
import com.dubpilot.backend.repository.ProductionNoteRepository;
import com.dubpilot.backend.repository.ProjectRepository;
import com.dubpilot.backend.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final EpisodeRepository episodeRepository;
    private final ProductionNoteRepository noteRepository;

    public ProjectController(ProjectRepository projectRepository,
                             ProjectService projectService,
                             EpisodeRepository episodeRepository,
                             ProductionNoteRepository noteRepository) {
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.episodeRepository = episodeRepository;
        this.noteRepository = noteRepository;
    }

    @GetMapping
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable Long id) {
        return projectRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Project> createProject(@Valid @RequestBody Project project) {
        Project saved = projectRepository.save(project);
        return ResponseEntity.status(201).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id) {
        Optional<Project> projectOptional = projectRepository.findById(id);
        if (projectOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<Episode> episodes = episodeRepository.findByProjectId(id);
        List<ProductionNote> notes = noteRepository.findByProjectIdOrderByCreatedAtDesc(id);

        if (!episodes.isEmpty() || !notes.isEmpty()) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "Cannot delete this project because it has " + episodes.size()
                            + " episode(s) and " + notes.size() + " production note(s) linked to it. "
                            + "Deleting projects with production history is not supported, to protect historical data. "
                            + "Remove or reassign the related episodes and notes first if you really need to delete it."));
        }

        projectRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ProjectStatusResponse> getProjectStatus(@PathVariable Long id) {
        return projectService.getProjectStatus(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<?> updateProjectProgress(@PathVariable Long id,
                                                   @Valid @RequestBody ProjectProgressUpdateRequest request) {
        Optional<Project> projectOptional = projectRepository.findById(id);
        if (projectOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ProjectStage newStage;
        try {
            newStage = ProjectStage.valueOf(request.getStage().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid stage value: " + request.getStage()));
        }

        Project project = projectOptional.get();
        project.setProgressPercentage(request.getProgressPercentage());
        project.setStage(newStage);

        Project updated = projectRepository.save(project);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/upcoming")
    public List<Project> getUpcomingProjects() {
        return projectService.getUpcomingProjects();
    }

    @GetMapping("/active")
    public List<Project> getActiveProjects() {
        return projectService.getActiveProjects();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(@PathVariable Long id, @Valid @RequestBody ProjectUpdateRequest request) {
        Optional<Project> projectOptional = projectRepository.findById(id);
        if (projectOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Project project = projectOptional.get();

        if (request.getStage() != null) {
            ProjectStage newStage;
            try {
                newStage = ProjectStage.valueOf(request.getStage().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid stage value: " + request.getStage()));
            }
            project.setStage(newStage);
        }

        if (request.getProgressPercentage() != null) {
            project.setProgressPercentage(request.getProgressPercentage());
        }

        if (request.getDeadline() != null) {
            project.setDeadline(request.getDeadline());
        }

        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }

        if (request.getClientName() != null) {
            project.setClientName(request.getClientName());
        }

        Project updated = projectRepository.save(project);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/delayed")
    public List<Project> getDelayedProjects() {
        return projectService.getDelayedProjects();
    }

    @GetMapping("/completed")
    public List<Project> getCompletedProjects() {
        return projectService.getCompletedProjects();
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<ProjectSummaryResponse> getProjectSummary(@PathVariable Long id) {
        return projectService.getProjectSummary(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
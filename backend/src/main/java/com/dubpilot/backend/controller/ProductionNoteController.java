package com.dubpilot.backend.controller;

import com.dubpilot.backend.dto.NoteCreateRequest;
import com.dubpilot.backend.dto.NoteResponse;
import com.dubpilot.backend.entity.Episode;
import com.dubpilot.backend.entity.ProductionNote;
import com.dubpilot.backend.entity.Project;
import com.dubpilot.backend.repository.EpisodeRepository;
import com.dubpilot.backend.repository.ProductionNoteRepository;
import com.dubpilot.backend.repository.ProjectRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notes")
@CrossOrigin(origins = {"http://localhost:5173", "https://aozene-ai.vercel.app", "https://aozene-oktxti0i7-harsha-d199.vercel.app", "https://aozene-pt8vv9zy3-harsha-d199.vercel.app"})
public class ProductionNoteController {

    private final ProductionNoteRepository noteRepository;
    private final ProjectRepository projectRepository;
    private final EpisodeRepository episodeRepository;

    public ProductionNoteController(ProductionNoteRepository noteRepository,
                                    ProjectRepository projectRepository,
                                    EpisodeRepository episodeRepository) {
        this.noteRepository = noteRepository;
        this.projectRepository = projectRepository;
        this.episodeRepository = episodeRepository;
    }

    @PostMapping
    public ResponseEntity<?> createNote(@Valid @RequestBody NoteCreateRequest request) {
        Optional<Project> projectOptional = projectRepository.findById(request.getProjectId());
        if (projectOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Project project = projectOptional.get();

        Episode episode = null;
        if (request.getEpisodeId() != null) {
            Optional<Episode> episodeOptional = episodeRepository.findById(request.getEpisodeId());
            if (episodeOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            episode = episodeOptional.get();

            if (episode.getProject() == null || !episode.getProject().getId().equals(project.getId())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "The supplied episode does not belong to the supplied project."));
            }
        }

        ProductionNote note = new ProductionNote();
        note.setProject(project);
        note.setEpisode(episode);
        note.setNoteText(request.getNoteText());

        ProductionNote saved = noteRepository.save(note);
        return ResponseEntity.status(201).body(toResponse(saved));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<?> getNotesForProject(@PathVariable Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            return ResponseEntity.notFound().build();
        }
        List<NoteResponse> notes = noteRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(notes);
    }

    @GetMapping("/episode/{episodeId}")
    public ResponseEntity<?> getNotesForEpisode(@PathVariable Long episodeId) {
        if (!episodeRepository.existsById(episodeId)) {
            return ResponseEntity.notFound().build();
        }
        List<NoteResponse> notes = noteRepository.findByEpisodeIdOrderByCreatedAtDesc(episodeId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(notes);
    }

    @GetMapping("/recent")
    public List<NoteResponse> getRecentNotes() {
        return noteRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
        if (!noteRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        noteRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    private NoteResponse toResponse(ProductionNote note) {
        Long episodeId = note.getEpisode() != null ? note.getEpisode().getId() : null;
        return new NoteResponse(
                note.getId(),
                note.getProject().getId(),
                episodeId,
                note.getNoteText(),
                note.getCreatedAt());
    }
}
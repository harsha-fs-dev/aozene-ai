package com.dubpilot.backend.controller;

import com.dubpilot.backend.dto.EpisodeCreateRequest;
import com.dubpilot.backend.entity.Episode;
import com.dubpilot.backend.entity.Project;
import com.dubpilot.backend.entity.ProductionNote;
import com.dubpilot.backend.entity.RecordingSession;
import com.dubpilot.backend.repository.EpisodeRepository;
import com.dubpilot.backend.repository.ProductionNoteRepository;
import com.dubpilot.backend.repository.ProjectRepository;
import com.dubpilot.backend.repository.RecordingSessionRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/episodes")
@CrossOrigin(origins = "http://localhost:5173")
public class EpisodeController {

    private final EpisodeRepository episodeRepository;
    private final ProjectRepository projectRepository;
    private final RecordingSessionRepository sessionRepository;
    private final ProductionNoteRepository noteRepository;

    public EpisodeController(EpisodeRepository episodeRepository,
                             ProjectRepository projectRepository,
                             RecordingSessionRepository sessionRepository,
                             ProductionNoteRepository noteRepository) {
        this.episodeRepository = episodeRepository;
        this.projectRepository = projectRepository;
        this.sessionRepository = sessionRepository;
        this.noteRepository = noteRepository;
    }

    @GetMapping
    public List<Episode> getAllEpisodes() {
        return episodeRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Episode> getEpisodeById(@PathVariable Long id) {
        return episodeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<?> getEpisodesByProject(@PathVariable Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            return ResponseEntity.notFound().build();
        }
        List<Episode> episodes = episodeRepository.findByProjectId(projectId);
        return ResponseEntity.ok(episodes);
    }

    @PostMapping
    public ResponseEntity<?> createEpisode(@Valid @RequestBody EpisodeCreateRequest request) {
        Optional<Project> projectOptional = projectRepository.findById(request.getProjectId());
        if (projectOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        boolean duplicate = episodeRepository.existsByProjectIdAndEpisodeNumber(
                request.getProjectId(), request.getEpisodeNumber());
        if (duplicate) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "Episode " + request.getEpisodeNumber()
                            + " already exists for this project."));
        }

        Episode episode = new Episode();
        episode.setTitle(request.getTitle());
        episode.setEpisodeNumber(request.getEpisodeNumber());
        episode.setDescription(request.getDescription());
        episode.setProject(projectOptional.get());

        Episode saved = episodeRepository.save(episode);
        return ResponseEntity.status(201).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEpisode(@PathVariable Long id) {
        Optional<Episode> episodeOptional = episodeRepository.findById(id);
        if (episodeOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<RecordingSession> sessions = sessionRepository.findByEpisodeId(id);
        List<ProductionNote> notes = noteRepository.findByEpisodeIdOrderByCreatedAtDesc(id);

        if (!sessions.isEmpty() || !notes.isEmpty()) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "Cannot delete this episode because it has " + sessions.size()
                            + " recording session(s) and " + notes.size() + " production note(s) linked to it. "
                            + "Deleting episodes with production history is not supported, to protect historical data."));
        }

        episodeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
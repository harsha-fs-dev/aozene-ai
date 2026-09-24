package com.dubpilot.backend.controller;

import com.dubpilot.backend.dto.AvailabilityResult;
import com.dubpilot.backend.dto.SessionCreateRequest;
import com.dubpilot.backend.dto.SessionStatusUpdateRequest;
import com.dubpilot.backend.entity.Artist;
import com.dubpilot.backend.entity.Episode;
import com.dubpilot.backend.entity.Project;
import com.dubpilot.backend.entity.RecordingSession;
import com.dubpilot.backend.entity.SessionStatus;
import com.dubpilot.backend.repository.ArtistRepository;
import com.dubpilot.backend.repository.EpisodeRepository;
import com.dubpilot.backend.repository.ProjectRepository;
import com.dubpilot.backend.repository.RecordingSessionRepository;
import com.dubpilot.backend.service.SchedulingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = {"http://localhost:5173", "https://aozene-ai.vercel.app", "https://aozene-oktxti0i7-harsha-d199.vercel.app"})
public class RecordingSessionController {

    private final RecordingSessionRepository sessionRepository;
    private final EpisodeRepository episodeRepository;
    private final ArtistRepository artistRepository;
    private final ProjectRepository projectRepository;
    private final SchedulingService schedulingService;

    public RecordingSessionController(RecordingSessionRepository sessionRepository,
                                      EpisodeRepository episodeRepository,
                                      ArtistRepository artistRepository,
                                      ProjectRepository projectRepository,
                                      SchedulingService schedulingService) {
        this.sessionRepository = sessionRepository;
        this.episodeRepository = episodeRepository;
        this.artistRepository = artistRepository;
        this.projectRepository = projectRepository;
        this.schedulingService = schedulingService;
    }

    @GetMapping
    public List<RecordingSession> getAllSessions() {
        return sessionRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecordingSession> getSessionById(@PathVariable Long id) {
        return sessionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createSession(@Valid @RequestBody SessionCreateRequest request) {

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "startTime must be before endTime"));
        }

        Optional<Episode> episodeOptional = episodeRepository.findById(request.getEpisodeId());
        if (episodeOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Episode episode = episodeOptional.get();

        Optional<Artist> artistOptional = artistRepository.findById(request.getArtistId());
        if (artistOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Optional stricter check: if the caller also supplied projectId (e.g. the voice
        // agent, which always knows which project it's talking about), verify the project
        // exists and that the episode actually belongs to it. Existing callers that omit
        // projectId are unaffected.
        if (request.getProjectId() != null) {
            Optional<Project> projectOptional = projectRepository.findById(request.getProjectId());
            if (projectOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            if (episode.getProject() == null || !episode.getProject().getId().equals(request.getProjectId())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "The supplied episode does not belong to the supplied project."));
            }
        }

        AvailabilityResult availabilityResult = schedulingService.checkAvailability(
                request.getArtistId(),
                request.getSessionDate(),
                request.getStartTime(),
                request.getEndTime());

        if (!availabilityResult.isAvailable()) {
            return ResponseEntity.status(409).body(availabilityResult);
        }

        RecordingSession session = new RecordingSession();
        session.setEpisode(episode);
        session.setArtist(artistOptional.get());
        session.setSessionDate(request.getSessionDate());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setNotes(request.getNotes());
        session.setStatus(SessionStatus.AWAITING_CONFIRMATION);
        session.setCreatedAt(LocalDateTime.now());

        RecordingSession saved = sessionRepository.save(session);
        return ResponseEntity.status(201).body(saved);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RecordingSession> updateSessionStatus(@PathVariable Long id,
                                                                @Valid @RequestBody SessionStatusUpdateRequest request) {
        Optional<RecordingSession> sessionOptional = sessionRepository.findById(id);
        if (sessionOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SessionStatus newStatus;
        try {
            newStatus = SessionStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        RecordingSession session = sessionOptional.get();
        session.setStatus(newStatus);

        RecordingSession updated = sessionRepository.save(session);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<?> confirmSession(@PathVariable Long id) {
        Optional<RecordingSession> sessionOptional = sessionRepository.findById(id);
        if (sessionOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RecordingSession session = sessionOptional.get();

        if (session.getStatus() != SessionStatus.AWAITING_CONFIRMATION) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "Only sessions with status AWAITING_CONFIRMATION can be confirmed. Current status: "
                            + session.getStatus()));
        }

        session.setStatus(SessionStatus.CONFIRMED);
        RecordingSession updated = sessionRepository.save(session);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/decline")
    public ResponseEntity<?> declineSession(@PathVariable Long id) {
        Optional<RecordingSession> sessionOptional = sessionRepository.findById(id);
        if (sessionOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RecordingSession session = sessionOptional.get();

        if (session.getStatus() != SessionStatus.AWAITING_CONFIRMATION) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "Only sessions with status AWAITING_CONFIRMATION can be declined. Current status: "
                            + session.getStatus()));
        }

        session.setStatus(SessionStatus.DECLINED);
        RecordingSession updated = sessionRepository.save(session);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{sessionId}/replace-artist/{artistId}")
    public ResponseEntity<?> replaceArtist(@PathVariable Long sessionId, @PathVariable Long artistId) {
        Optional<RecordingSession> sessionOptional = sessionRepository.findById(sessionId);
        if (sessionOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<Artist> artistOptional = artistRepository.findById(artistId);
        if (artistOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RecordingSession session = sessionOptional.get();
        Artist newArtist = artistOptional.get();

        if (!newArtist.isActive()) {
            return ResponseEntity.status(409).body(Map.of("error", "Replacement artist is not active."));
        }

        if (session.getStatus() != SessionStatus.DECLINED
                && session.getStatus() != SessionStatus.AWAITING_CONFIRMATION) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "Replacement is only allowed when session status is DECLINED or AWAITING_CONFIRMATION. Current status: "
                            + session.getStatus()));
        }

        AvailabilityResult availabilityResult = schedulingService.checkAvailability(
                artistId, session.getSessionDate(), session.getStartTime(), session.getEndTime());

        if (!availabilityResult.isAvailable()) {
            return ResponseEntity.status(409).body(availabilityResult);
        }

        session.setArtist(newArtist);
        session.setStatus(SessionStatus.AWAITING_CONFIRMATION);
        RecordingSession updated = sessionRepository.save(session);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/artist/{artistId}/pending")
    public ResponseEntity<?> getPendingSessionsForArtist(@PathVariable Long artistId) {
        Optional<Artist> artistOptional = artistRepository.findById(artistId);
        if (artistOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<RecordingSession> pending =
                sessionRepository.findByArtistIdAndStatus(artistId, SessionStatus.AWAITING_CONFIRMATION);
        return ResponseEntity.ok(pending);
    }

    @GetMapping("/upcoming")
    public List<RecordingSession> getUpcomingSessions() {
        List<SessionStatus> excluded = List.of(SessionStatus.DECLINED, SessionStatus.CANCELLED, SessionStatus.COMPLETED);

        return sessionRepository.findAll().stream()
                .filter(s -> !excluded.contains(s.getStatus()))
                .sorted(Comparator.comparing(RecordingSession::getSessionDate)
                        .thenComparing(RecordingSession::getStartTime))
                .toList();
    }

    @GetMapping("/pending-confirmations")
    public List<RecordingSession> getPendingConfirmations() {
        return sessionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SessionStatus.AWAITING_CONFIRMATION)
                .sorted(Comparator.comparing(RecordingSession::getSessionDate)
                        .thenComparing(RecordingSession::getStartTime))
                .toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        if (!sessionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        sessionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelSession(@PathVariable Long id) {
        Optional<RecordingSession> sessionOptional = sessionRepository.findById(id);
        if (sessionOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RecordingSession session = sessionOptional.get();

        if (session.getStatus() == SessionStatus.CANCELLED
                || session.getStatus() == SessionStatus.DECLINED
                || session.getStatus() == SessionStatus.COMPLETED) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "Only AWAITING_CONFIRMATION or CONFIRMED sessions can be cancelled. Current status: "
                            + session.getStatus()));
        }

        session.setStatus(SessionStatus.CANCELLED);
        RecordingSession updated = sessionRepository.save(session);
        return ResponseEntity.ok(updated);
    }
}
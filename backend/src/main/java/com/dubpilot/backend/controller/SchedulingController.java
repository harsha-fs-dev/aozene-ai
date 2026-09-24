package com.dubpilot.backend.controller;

import com.dubpilot.backend.dto.AvailabilityResult;
import com.dubpilot.backend.dto.ReplacementArtistResponse;
import com.dubpilot.backend.entity.Episode;
import com.dubpilot.backend.entity.Project;
import com.dubpilot.backend.entity.RecordingSession;
import com.dubpilot.backend.repository.EpisodeRepository;
import com.dubpilot.backend.repository.ProjectRepository;
import com.dubpilot.backend.repository.RecordingSessionRepository;
import com.dubpilot.backend.service.SchedulingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/scheduling")
@CrossOrigin(origins = {"http://localhost:5173", "https://aozene-ai.vercel.app", "https://aozene-oktxti0i7-harsha-d199.vercel.app", "https://aozene-pt8vv9zy3-harsha-d199.vercel.app"})
public class SchedulingController {

    private final SchedulingService schedulingService;
    private final RecordingSessionRepository sessionRepository;
    private final ProjectRepository projectRepository;
    private final EpisodeRepository episodeRepository;

    public SchedulingController(SchedulingService schedulingService,
                                RecordingSessionRepository sessionRepository,
                                ProjectRepository projectRepository,
                                EpisodeRepository episodeRepository) {
        this.schedulingService = schedulingService;
        this.sessionRepository = sessionRepository;
        this.projectRepository = projectRepository;
        this.episodeRepository = episodeRepository;
    }

    @GetMapping("/check-availability")
    public AvailabilityResult checkAvailability(
            @RequestParam Long artistId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {

        return schedulingService.checkAvailability(artistId, sessionDate, startTime, endTime);
    }

    @GetMapping("/replacement-artists")
    public ResponseEntity<?> getReplacementArtists(@RequestParam Long sessionId) {
        Optional<RecordingSession> sessionOptional = sessionRepository.findById(sessionId);
        if (sessionOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RecordingSession session = sessionOptional.get();
        if (session.getArtist() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Session has no assigned artist."));
        }

        List<ReplacementArtistResponse> replacements = schedulingService.findReplacementArtists(session);
        return ResponseEntity.ok(replacements);
    }

    @GetMapping("/available-artists")
    public ResponseEntity<?> getAvailableArtists(
            @RequestParam Long projectId,
            @RequestParam Long episodeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime,
            @RequestParam(required = false) String specialization) {

        Optional<Project> projectOptional = projectRepository.findById(projectId);
        if (projectOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<Episode> episodeOptional = episodeRepository.findById(episodeId);
        if (episodeOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Episode episode = episodeOptional.get();

        if (episode.getProject() == null || !episode.getProject().getId().equals(projectId)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "The supplied episode does not belong to the supplied project."));
        }

        if (!startDateTime.isBefore(endDateTime)) {
            return ResponseEntity.badRequest().body(Map.of("error", "startDateTime must be before endDateTime"));
        }
        if (!startDateTime.toLocalDate().equals(endDateTime.toLocalDate())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "startDateTime and endDateTime must fall on the same date."));
        }

        List<ReplacementArtistResponse> results = schedulingService.searchAvailableArtists(
                startDateTime.toLocalDate(), startDateTime.toLocalTime(), endDateTime.toLocalTime(), specialization);

        return ResponseEntity.ok(results);
    }
}
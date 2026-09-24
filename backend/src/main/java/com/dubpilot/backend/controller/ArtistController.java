package com.dubpilot.backend.controller;

import com.dubpilot.backend.dto.ArtistUpdateRequest;
import com.dubpilot.backend.dto.AvailabilityUpsertRequest;
import com.dubpilot.backend.entity.Artist;
import com.dubpilot.backend.entity.ArtistAvailability;
import com.dubpilot.backend.entity.AvailabilityStatus;
import com.dubpilot.backend.entity.RecordingSession;
import com.dubpilot.backend.repository.ArtistAvailabilityRepository;
import com.dubpilot.backend.repository.ArtistRepository;
import com.dubpilot.backend.repository.RecordingSessionRepository;
import com.dubpilot.backend.service.SchedulingService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/artists")
@CrossOrigin(origins = {"http://localhost:5173", "https://aozene-ai.vercel.app", "https://aozene-oktxti0i7-harsha-d199.vercel.app", "https://aozene-pt8vv9zy3-harsha-d199.vercel.app"})
public class ArtistController {

    private final ArtistRepository artistRepository;
    private final ArtistAvailabilityRepository availabilityRepository;
    private final RecordingSessionRepository sessionRepository;
    private final SchedulingService schedulingService;

    public ArtistController(ArtistRepository artistRepository,
                            ArtistAvailabilityRepository availabilityRepository,
                            RecordingSessionRepository sessionRepository,
                            SchedulingService schedulingService) {
        this.artistRepository = artistRepository;
        this.availabilityRepository = availabilityRepository;
        this.sessionRepository = sessionRepository;
        this.schedulingService = schedulingService;
    }

    @GetMapping
    public List<Artist> getAllArtists() {
        return artistRepository.findAll();
    }

    @GetMapping("/active")
    public List<Artist> getActiveArtists() {
        return artistRepository.findAll().stream()
                .filter(Artist::isActive)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Artist> getArtistById(@PathVariable Long id) {
        return artistRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createArtist(@Valid @RequestBody Artist artist) {
        try {
            Artist saved = artistRepository.save(artist);
            return ResponseEntity.status(201).body(saved);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body(Map.of("error", "An artist with this email already exists."));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateArtist(@PathVariable Long id, @RequestBody ArtistUpdateRequest request) {
        Optional<Artist> artistOptional = artistRepository.findById(id);
        if (artistOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Artist artist = artistOptional.get();

        if (request.getName() != null) {
            artist.setName(request.getName());
        }
        if (request.getPhone() != null) {
            artist.setPhone(request.getPhone());
        }
        if (request.getSpecialization() != null) {
            artist.setSpecialization(request.getSpecialization());
        }
        if (request.getActive() != null) {
            artist.setActive(request.getActive());
        }

        Artist updated = artistRepository.save(artist);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateArtist(@PathVariable Long id) {
        Optional<Artist> artistOptional = artistRepository.findById(id);
        if (artistOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Artist artist = artistOptional.get();
        artist.setActive(false);
        Artist updated = artistRepository.save(artist);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<?> activateArtist(@PathVariable Long id) {
        Optional<Artist> artistOptional = artistRepository.findById(id);
        if (artistOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Artist artist = artistOptional.get();
        artist.setActive(true);
        Artist updated = artistRepository.save(artist);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteArtist(@PathVariable Long id) {
        Optional<Artist> artistOptional = artistRepository.findById(id);
        if (artistOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<RecordingSession> sessions = sessionRepository.findByArtistId(id);
        if (!sessions.isEmpty()) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "Cannot permanently delete this artist because they have " + sessions.size()
                            + " recording session(s) in the system, which is production history. "
                            + "Use PUT /api/artists/" + id + "/deactivate instead to stop them being selected for new scheduling."));
        }

        // No session history - safe to remove. Cascade-delete their availability
        // records first, since those are just scheduling input, not outcome history.
        List<ArtistAvailability> availabilityRecords = availabilityRepository.findByArtistIdOrderByIdDesc(id);
        availabilityRepository.deleteAll(availabilityRecords);

        artistRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{artistId}/availability")
    public ResponseEntity<?> addAvailability(@PathVariable Long artistId,
                                             @Valid @RequestBody AvailabilityUpsertRequest request) {
        Optional<Artist> artistOptional = artistRepository.findById(artistId);
        if (artistOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (!request.getStartDateTime().isBefore(request.getEndDateTime())) {
            return ResponseEntity.badRequest().body(Map.of("error", "startDateTime must be before endDateTime"));
        }

        if (!request.getStartDateTime().toLocalDate().equals(request.getEndDateTime().toLocalDate())) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "startDateTime and endDateTime must fall on the same date."));
        }

        AvailabilityStatus status;
        try {
            status = AvailabilityStatus.valueOf(request.getAvailabilityStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Invalid availabilityStatus: " + request.getAvailabilityStatus()));
        }

        ArtistAvailability availability = new ArtistAvailability();
        availability.setArtist(artistOptional.get());
        availability.setDate(request.getStartDateTime().toLocalDate());
        availability.setStartTime(request.getStartDateTime().toLocalTime());
        availability.setEndTime(request.getEndDateTime().toLocalTime());
        availability.setAvailabilityStatus(status);

        ArtistAvailability saved = availabilityRepository.save(availability);
        return ResponseEntity.status(201).body(saved);
    }

    @GetMapping("/{artistId}/availability")
    public ResponseEntity<?> getAvailability(@PathVariable Long artistId) {
        if (!artistRepository.existsById(artistId)) {
            return ResponseEntity.notFound().build();
        }
        List<ArtistAvailability> records = availabilityRepository.findByArtistIdOrderByIdDesc(artistId);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/available")
    public ResponseEntity<?> getAvailableArtists(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        if (!start.isBefore(end)) {
            return ResponseEntity.badRequest().body(Map.of("error", "start must be before end"));
        }
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            return ResponseEntity.badRequest().body(Map.of("error", "start and end must fall on the same date."));
        }

        List<Artist> available = schedulingService.findAvailableArtists(
                start.toLocalDate(), start.toLocalTime(), end.toLocalTime());
        return ResponseEntity.ok(available);
    }
}
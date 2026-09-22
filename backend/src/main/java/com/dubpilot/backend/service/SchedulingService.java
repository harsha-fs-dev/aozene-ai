package com.dubpilot.backend.service;

import com.dubpilot.backend.dto.AvailabilityResult;
import com.dubpilot.backend.dto.ReplacementArtistResponse;
import com.dubpilot.backend.entity.Artist;
import com.dubpilot.backend.entity.AvailabilityStatus;
import com.dubpilot.backend.entity.RecordingSession;
import com.dubpilot.backend.entity.SessionStatus;
import com.dubpilot.backend.repository.ArtistAvailabilityRepository;
import com.dubpilot.backend.repository.ArtistRepository;
import com.dubpilot.backend.repository.RecordingSessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class SchedulingService {

    private final ArtistRepository artistRepository;
    private final ArtistAvailabilityRepository availabilityRepository;
    private final RecordingSessionRepository sessionRepository;

    public SchedulingService(ArtistRepository artistRepository,
                             ArtistAvailabilityRepository availabilityRepository,
                             RecordingSessionRepository sessionRepository) {
        this.artistRepository = artistRepository;
        this.availabilityRepository = availabilityRepository;
        this.sessionRepository = sessionRepository;
    }

    public AvailabilityResult checkAvailability(Long artistId, LocalDate sessionDate,
                                                LocalTime startTime, LocalTime endTime) {

        Optional<Artist> artistOptional = artistRepository.findById(artistId);
        if (artistOptional.isEmpty()) {
            return new AvailabilityResult(false, "Artist not found.");
        }

        Artist artist = artistOptional.get();
        if (!artist.isActive()) {
            return new AvailabilityResult(false, "Artist is not active.");
        }

        if (!hasCoveringAvailability(artistId, sessionDate, startTime, endTime)) {
            return new AvailabilityResult(false,
                    "Artist does not have recorded availability for the requested time.");
        }

        Optional<RecordingSession> conflict = findConflictingSession(artistId, sessionDate, startTime, endTime);
        if (conflict.isPresent()) {
            RecordingSession existing = conflict.get();
            String statusWord = existing.getStatus() == SessionStatus.CONFIRMED ? "confirmed" : "pending";
            return new AvailabilityResult(false, "Artist already has a " + statusWord + " recording from "
                    + existing.getStartTime() + " to " + existing.getEndTime() + ".");
        }

        return new AvailabilityResult(true, "Artist is available for the requested time.");
    }

    /**
     * Finds other active artists who appear available, according to the studio's recorded
     * schedule, to take over the given session.
     */
    public List<ReplacementArtistResponse> findReplacementArtists(RecordingSession session) {
        Long currentArtistId = session.getArtist() != null ? session.getArtist().getId() : null;
        LocalDate date = session.getSessionDate();
        LocalTime start = session.getStartTime();
        LocalTime end = session.getEndTime();
        String targetSpecialization = session.getArtist() != null ? session.getArtist().getSpecialization() : null;

        List<Artist> candidates = artistRepository.findAll().stream()
                .filter(Artist::isActive)
                .filter(a -> currentArtistId == null || !a.getId().equals(currentArtistId))
                .toList();

        List<ReplacementArtistResponse> results = new ArrayList<>();

        for (Artist artist : candidates) {
            boolean covering = hasCoveringAvailability(artist.getId(), date, start, end);
            boolean conflict = covering && hasConflictingSession(artist.getId(), date, start, end);
            boolean available = covering && !conflict;

            if (available) {
                results.add(new ReplacementArtistResponse(
                        artist.getId(),
                        artist.getName(),
                        artist.getEmail(),
                        artist.getSpecialization(),
                        true,
                        "Available according to the studio schedule."));
            }
        }

        results.sort((a, b) -> {
            if (targetSpecialization != null) {
                boolean aMatch = targetSpecialization.equalsIgnoreCase(a.getSpecialization());
                boolean bMatch = targetSpecialization.equalsIgnoreCase(b.getSpecialization());
                if (aMatch != bMatch) {
                    return aMatch ? -1 : 1;
                }
            }
            return a.getArtistName().compareToIgnoreCase(b.getArtistName());
        });

        return results;
    }

    /**
     * Returns active artists who have AVAILABLE recorded availability covering the given
     * window and no conflicting recording session. Used by the artist-facing
     * GET /api/artists/available endpoint. Reuses the same checks as everything else.
     */
    public List<Artist> findAvailableArtists(LocalDate date, LocalTime startTime, LocalTime endTime) {
        return artistRepository.findAll().stream()
                .filter(Artist::isActive)
                .filter(artist -> hasCoveringAvailability(artist.getId(), date, startTime, endTime))
                .filter(artist -> !hasConflictingSession(artist.getId(), date, startTime, endTime))
                .toList();
    }

    /**
     * Searches for suitable artists for a specific project/episode recording request,
     * with an optional specialization filter. Reuses the exact same availability and
     * conflict checks as checkAvailability and findReplacementArtists.
     */
    public List<ReplacementArtistResponse> searchAvailableArtists(LocalDate date, LocalTime startTime,
                                                                  LocalTime endTime, String specialization) {
        List<Artist> candidates = artistRepository.findAll().stream()
                .filter(Artist::isActive)
                .filter(a -> specialization == null || specialization.isBlank()
                        || specialization.equalsIgnoreCase(a.getSpecialization()))
                .toList();

        List<ReplacementArtistResponse> results = new ArrayList<>();

        for (Artist artist : candidates) {
            boolean covering = hasCoveringAvailability(artist.getId(), date, startTime, endTime);
            boolean conflict = covering && hasConflictingSession(artist.getId(), date, startTime, endTime);
            boolean available = covering && !conflict;

            if (available) {
                results.add(new ReplacementArtistResponse(
                        artist.getId(),
                        artist.getName(),
                        artist.getEmail(),
                        artist.getSpecialization(),
                        true,
                        "Available according to the studio schedule."));
            }
        }

        results.sort(Comparator.comparing(ReplacementArtistResponse::getArtistName, String.CASE_INSENSITIVE_ORDER));
        return results;
    }

    private boolean hasCoveringAvailability(Long artistId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return availabilityRepository.findAll().stream()
                .filter(a -> a.getArtist().getId().equals(artistId))
                .filter(a -> a.getDate().equals(date))
                .filter(a -> a.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE)
                .anyMatch(a -> !a.getStartTime().isAfter(startTime) && !a.getEndTime().isBefore(endTime));
    }

    private Optional<RecordingSession> findConflictingSession(Long artistId, LocalDate date,
                                                              LocalTime startTime, LocalTime endTime) {
        return sessionRepository.findAll().stream()
                .filter(s -> s.getArtist().getId().equals(artistId))
                .filter(s -> s.getSessionDate().equals(date))
                .filter(s -> s.getStatus() == SessionStatus.CONFIRMED
                        || s.getStatus() == SessionStatus.AWAITING_CONFIRMATION)
                .filter(s -> s.getStartTime().isBefore(endTime) && s.getEndTime().isAfter(startTime))
                .findFirst();
    }

    private boolean hasConflictingSession(Long artistId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return findConflictingSession(artistId, date, startTime, endTime).isPresent();
    }
}
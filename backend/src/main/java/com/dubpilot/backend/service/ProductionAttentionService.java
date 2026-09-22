package com.dubpilot.backend.service;

import com.dubpilot.backend.dto.AttentionItem;
import com.dubpilot.backend.entity.ProductionNote;
import com.dubpilot.backend.entity.Project;
import com.dubpilot.backend.entity.ProjectStage;
import com.dubpilot.backend.entity.RecordingSession;
import com.dubpilot.backend.entity.SessionStatus;
import com.dubpilot.backend.repository.ProductionNoteRepository;
import com.dubpilot.backend.repository.ProjectRepository;
import com.dubpilot.backend.repository.RecordingSessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class ProductionAttentionService {

    private static final Map<String, Integer> PRIORITY_ORDER = Map.of(
            "HIGH", 0,
            "MEDIUM", 1,
            "LOW", 2
    );

    private static final int MAX_UPCOMING_ITEMS = 5;
    private static final int MAX_RECENT_NOTE_ITEMS = 5;
    private static final int MAX_DECLINED_ITEMS = 5;
    private static final int APPROACHING_DEADLINE_WINDOW_DAYS = 7;

    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final RecordingSessionRepository sessionRepository;
    private final ProductionNoteRepository noteRepository;

    public ProductionAttentionService(ProjectService projectService,
                                      ProjectRepository projectRepository,
                                      RecordingSessionRepository sessionRepository,
                                      ProductionNoteRepository noteRepository) {
        this.projectService = projectService;
        this.projectRepository = projectRepository;
        this.sessionRepository = sessionRepository;
        this.noteRepository = noteRepository;
    }

    public List<AttentionItem> getAttentionItems() {
        List<AttentionItem> items = new ArrayList<>();

        items.addAll(buildDelayedProjectItems());
        items.addAll(buildPendingConfirmationItems());
        items.addAll(buildDeclinedSessionItems());
        items.addAll(buildApproachingDeadlineItems());
        items.addAll(buildUpcomingSessionItems());
        items.addAll(buildRecentNoteItems());

        // Stable sort: HIGH first, then MEDIUM, then LOW. Items of the same priority
        // keep the order they were added in above.
        items.sort(Comparator.comparing(item -> PRIORITY_ORDER.getOrDefault(item.getPriority(), 99)));

        return items;
    }

    private List<AttentionItem> buildDelayedProjectItems() {
        List<AttentionItem> items = new ArrayList<>();
        for (Project project : projectService.getDelayedProjects()) {
            Integer progress = project.getProgressPercentage();
            String progressText = progress != null ? progress + "%" : "an unrecorded";

            items.add(new AttentionItem(
                    "DELAYED_PROJECT",
                    "HIGH",
                    project.getName() + " is delayed",
                    "Project is currently at " + progressText + " completion."));
        }
        return items;
    }

    private List<AttentionItem> buildPendingConfirmationItems() {
        List<AttentionItem> items = new ArrayList<>();

        List<RecordingSession> pending = sessionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SessionStatus.AWAITING_CONFIRMATION)
                .toList();

        for (RecordingSession session : pending) {
            String artistName = session.getArtist() != null ? session.getArtist().getName() : "The artist";
            String episodeTitle = session.getEpisode() != null ? session.getEpisode().getTitle() : "the episode";

            items.add(new AttentionItem(
                    "PENDING_CONFIRMATION",
                    "MEDIUM",
                    "Artist confirmation pending",
                    artistName + " has not confirmed the recording session for " + episodeTitle + "."));
        }
        return items;
    }

    /**
     * New: recently declined recording requests. These represent a real gap in the
     * schedule the manager should know about, so they're flagged HIGH - consistent with
     * the priority rule you specified, and additive to the existing categories above.
     */
    private List<AttentionItem> buildDeclinedSessionItems() {
        List<RecordingSession> declined = sessionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SessionStatus.DECLINED)
                .sorted(Comparator.comparing(RecordingSession::getCreatedAt).reversed())
                .limit(MAX_DECLINED_ITEMS)
                .toList();

        List<AttentionItem> items = new ArrayList<>();
        for (RecordingSession session : declined) {
            String artistName = session.getArtist() != null ? session.getArtist().getName() : "The artist";
            String episodeTitle = session.getEpisode() != null ? session.getEpisode().getTitle() : "the episode";
            String projectName = (session.getEpisode() != null && session.getEpisode().getProject() != null)
                    ? session.getEpisode().getProject().getName() : "the project";

            items.add(new AttentionItem(
                    "DECLINED_SESSION",
                    "HIGH",
                    "Recording request declined",
                    artistName + " declined the recording request for " + projectName + " " + episodeTitle
                            + " on " + session.getSessionDate() + "."));
        }
        return items;
    }

    /**
     * New: projects with a real recorded deadline within the next 7 days that are not
     * already COMPLETED or DELAYED (those are covered by their own categories above, so
     * this avoids double-flagging the same project). Purely a date comparison against
     * real data - no invented urgency.
     */
    private List<AttentionItem> buildApproachingDeadlineItems() {
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(APPROACHING_DEADLINE_WINDOW_DAYS);

        List<Project> candidates = projectRepository.findAll().stream()
                .filter(p -> p.getDeadline() != null)
                .filter(p -> ProjectService.classify(p) != ProjectService.ProjectClassification.COMPLETED
                        && ProjectService.classify(p) != ProjectService.ProjectClassification.DELAYED)                .filter(p -> p.getProgressPercentage() == null || p.getProgressPercentage() < 100)
                .filter(p -> !p.getDeadline().isBefore(today) && !p.getDeadline().isAfter(windowEnd))
                .toList();

        List<AttentionItem> items = new ArrayList<>();
        for (Project project : candidates) {
            long daysLeft = ChronoUnit.DAYS.between(today, project.getDeadline());
            String progressText = project.getProgressPercentage() != null
                    ? project.getProgressPercentage() + "%" : "an unrecorded";

            items.add(new AttentionItem(
                    "APPROACHING_DEADLINE",
                    "MEDIUM",
                    project.getName() + "'s deadline is approaching",
                    "Deadline is " + project.getDeadline() + " (" + daysLeft + " day"
                            + (daysLeft == 1 ? "" : "s") + " away), currently at " + progressText + " completion."));
        }
        return items;
    }

    private List<AttentionItem> buildUpcomingSessionItems() {
        List<SessionStatus> excluded = ProjectService.excludedFromUpcoming();

        List<RecordingSession> upcoming = sessionRepository.findAll().stream()
                .filter(s -> !excluded.contains(s.getStatus()))
                .sorted(Comparator.comparing(RecordingSession::getSessionDate)
                        .thenComparing(RecordingSession::getStartTime))
                .limit(MAX_UPCOMING_ITEMS)
                .toList();

        List<AttentionItem> items = new ArrayList<>();
        for (RecordingSession session : upcoming) {
            String projectName = (session.getEpisode() != null && session.getEpisode().getProject() != null)
                    ? session.getEpisode().getProject().getName() : "Project";
            String episodeTitle = session.getEpisode() != null ? session.getEpisode().getTitle() : "Episode";
            String artistName = session.getArtist() != null ? session.getArtist().getName() : "an artist";

            items.add(new AttentionItem(
                    "UPCOMING_SESSION",
                    "MEDIUM",
                    "Recording session coming up",
                    projectName + " " + episodeTitle + " with " + artistName + " at " + session.getStartTime() + "."));
        }
        return items;
    }

    private List<AttentionItem> buildRecentNoteItems() {
        List<ProductionNote> recentNotes = noteRepository.findTop10ByOrderByCreatedAtDesc();

        List<AttentionItem> items = new ArrayList<>();
        int count = 0;
        for (ProductionNote note : recentNotes) {
            if (count >= MAX_RECENT_NOTE_ITEMS) {
                break;
            }
            String projectName = note.getProject() != null ? note.getProject().getName() : "Project";

            items.add(new AttentionItem(
                    "RECENT_NOTE",
                    "LOW",
                    "Recent production note",
                    projectName + ": " + note.getNoteText()));
            count++;
        }
        return items;
    }
}
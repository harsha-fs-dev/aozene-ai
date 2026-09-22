package com.dubpilot.backend.service;

import com.dubpilot.backend.dto.DashboardSummaryResponse;
import com.dubpilot.backend.dto.ProjectStatusResponse;
import com.dubpilot.backend.dto.ProjectSummaryResponse;
import com.dubpilot.backend.entity.Episode;
import com.dubpilot.backend.entity.Project;
import com.dubpilot.backend.entity.ProjectStage;
import com.dubpilot.backend.entity.RecordingSession;
import com.dubpilot.backend.entity.SessionStatus;
import com.dubpilot.backend.repository.EpisodeRepository;
import com.dubpilot.backend.repository.ProjectRepository;
import com.dubpilot.backend.repository.RecordingSessionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {

    private static final List<SessionStatus> EXCLUDED_FROM_UPCOMING =
            List.of(SessionStatus.DECLINED, SessionStatus.CANCELLED, SessionStatus.COMPLETED);

    private final ProjectRepository projectRepository;
    private final EpisodeRepository episodeRepository;
    private final RecordingSessionRepository sessionRepository;

    public ProjectService(ProjectRepository projectRepository,
                          EpisodeRepository episodeRepository,
                          RecordingSessionRepository sessionRepository) {
        this.projectRepository = projectRepository;
        this.episodeRepository = episodeRepository;
        this.sessionRepository = sessionRepository;
    }

    public static ProjectClassification classify(Project project) {
        ProjectStage stage = project.getStage();
        Integer progress = project.getProgressPercentage();
        boolean fullProgress = progress != null && progress == 100;

        if (stage == ProjectStage.UPCOMING) {
            return ProjectClassification.UPCOMING;
        }
        if (stage == ProjectStage.DELAYED) {
            return ProjectClassification.DELAYED;
        }
        if (stage == ProjectStage.COMPLETED && fullProgress) {
            return ProjectClassification.COMPLETED;
        }
        return ProjectClassification.ACTIVE;
    }

    public enum ProjectClassification {
        UPCOMING, DELAYED, COMPLETED, ACTIVE
    }

    public Optional<ProjectStatusResponse> getProjectStatus(Long projectId) {
        return projectRepository.findById(projectId).map(project -> new ProjectStatusResponse(
                project.getId(),
                project.getName(),
                project.getStage().name(),
                project.getProgressPercentage(),
                project.getDeadline(),
                determineStatus(project)));
    }

    private String determineStatus(Project project) {
        return switch (classify(project)) {
            case DELAYED -> "DELAYED";
            case COMPLETED -> "COMPLETED";
            case UPCOMING, ACTIVE -> "ON_TRACK";
        };
    }

    public List<Project> getDelayedProjects() {
        return projectRepository.findAll().stream()
                .filter(p -> classify(p) == ProjectClassification.DELAYED)
                .toList();
    }

    public List<Project> getUpcomingProjects() {
        return projectRepository.findAll().stream()
                .filter(p -> classify(p) == ProjectClassification.UPCOMING)
                .toList();
    }

    public List<Project> getCompletedProjects() {
        return projectRepository.findAll().stream()
                .filter(p -> classify(p) == ProjectClassification.COMPLETED)
                .toList();
    }

    public List<Project> getActiveProjects() {
        return projectRepository.findAll().stream()
                .filter(p -> classify(p) == ProjectClassification.ACTIVE)
                .toList();
    }

    public Optional<ProjectSummaryResponse> getProjectSummary(Long projectId) {
        return projectRepository.findById(projectId).map(project -> {
            List<Episode> episodes = episodeRepository.findByProjectId(projectId);
            long totalEpisodes = episodes.size();
            long completedEpisodes = episodes.stream().filter(Episode::isCompleted).count();

            List<RecordingSession> projectSessions = sessionRepository.findAll().stream()
                    .filter(s -> s.getEpisode() != null
                            && s.getEpisode().getProject() != null
                            && s.getEpisode().getProject().getId().equals(projectId))
                    .toList();

            long upcomingSessions = projectSessions.stream()
                    .filter(s -> !EXCLUDED_FROM_UPCOMING.contains(s.getStatus()))
                    .count();

            long pendingConfirmations = projectSessions.stream()
                    .filter(s -> s.getStatus() == SessionStatus.AWAITING_CONFIRMATION)
                    .count();

            return new ProjectSummaryResponse(
                    project.getId(),
                    project.getName(),
                    project.getStage().name(),
                    project.getProgressPercentage(),
                    totalEpisodes,
                    completedEpisodes,
                    upcomingSessions,
                    pendingConfirmations,
                    project.getDeadline());
        });
    }

    public DashboardSummaryResponse computeDashboardSummary() {
        long totalProjects = projectRepository.count();

        long delayedProjects = getDelayedProjects().size();
        long completedProjects = getCompletedProjects().size();
        long activeProjects = getActiveProjects().size();

        List<RecordingSession> allSessions = sessionRepository.findAll();

        long pendingConfirmations = allSessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.AWAITING_CONFIRMATION)
                .count();

        long upcomingSessions = allSessions.stream()
                .filter(s -> !EXCLUDED_FROM_UPCOMING.contains(s.getStatus()))
                .count();

        return new DashboardSummaryResponse(
                totalProjects, activeProjects, completedProjects, delayedProjects,
                pendingConfirmations, upcomingSessions);
    }

    public static List<SessionStatus> excludedFromUpcoming() {
        return EXCLUDED_FROM_UPCOMING;
    }
}
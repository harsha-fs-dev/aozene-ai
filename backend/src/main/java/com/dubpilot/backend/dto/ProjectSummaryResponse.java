package com.dubpilot.backend.dto;

import java.time.LocalDate;

public class ProjectSummaryResponse {

    private Long projectId;
    private String projectName;
    private String stage;
    private Integer progressPercentage;
    private long totalEpisodes;
    private long completedEpisodes;
    private long upcomingSessions;
    private long pendingConfirmations;
    private LocalDate deadline;

    public ProjectSummaryResponse() {
    }

    public ProjectSummaryResponse(Long projectId, String projectName, String stage, Integer progressPercentage,
                                  long totalEpisodes, long completedEpisodes, long upcomingSessions,
                                  long pendingConfirmations, LocalDate deadline) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.stage = stage;
        this.progressPercentage = progressPercentage;
        this.totalEpisodes = totalEpisodes;
        this.completedEpisodes = completedEpisodes;
        this.upcomingSessions = upcomingSessions;
        this.pendingConfirmations = pendingConfirmations;
        this.deadline = deadline;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public Integer getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public long getTotalEpisodes() {
        return totalEpisodes;
    }

    public void setTotalEpisodes(long totalEpisodes) {
        this.totalEpisodes = totalEpisodes;
    }

    public long getCompletedEpisodes() {
        return completedEpisodes;
    }

    public void setCompletedEpisodes(long completedEpisodes) {
        this.completedEpisodes = completedEpisodes;
    }

    public long getUpcomingSessions() {
        return upcomingSessions;
    }

    public void setUpcomingSessions(long upcomingSessions) {
        this.upcomingSessions = upcomingSessions;
    }

    public long getPendingConfirmations() {
        return pendingConfirmations;
    }

    public void setPendingConfirmations(long pendingConfirmations) {
        this.pendingConfirmations = pendingConfirmations;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }
}
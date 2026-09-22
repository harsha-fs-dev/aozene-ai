package com.dubpilot.backend.dto;

public class DashboardSummaryResponse {

    private long totalProjects;
    private long activeProjects;
    private long completedProjects;
    private long delayedProjects;
    private long pendingConfirmations;
    private long upcomingSessions;

    public DashboardSummaryResponse() {
    }

    public DashboardSummaryResponse(long totalProjects, long activeProjects, long completedProjects,
                                    long delayedProjects, long pendingConfirmations, long upcomingSessions) {
        this.totalProjects = totalProjects;
        this.activeProjects = activeProjects;
        this.completedProjects = completedProjects;
        this.delayedProjects = delayedProjects;
        this.pendingConfirmations = pendingConfirmations;
        this.upcomingSessions = upcomingSessions;
    }

    public long getTotalProjects() {
        return totalProjects;
    }

    public void setTotalProjects(long totalProjects) {
        this.totalProjects = totalProjects;
    }

    public long getActiveProjects() {
        return activeProjects;
    }

    public void setActiveProjects(long activeProjects) {
        this.activeProjects = activeProjects;
    }

    public long getCompletedProjects() {
        return completedProjects;
    }

    public void setCompletedProjects(long completedProjects) {
        this.completedProjects = completedProjects;
    }

    public long getDelayedProjects() {
        return delayedProjects;
    }

    public void setDelayedProjects(long delayedProjects) {
        this.delayedProjects = delayedProjects;
    }

    public long getPendingConfirmations() {
        return pendingConfirmations;
    }

    public void setPendingConfirmations(long pendingConfirmations) {
        this.pendingConfirmations = pendingConfirmations;
    }

    public long getUpcomingSessions() {
        return upcomingSessions;
    }

    public void setUpcomingSessions(long upcomingSessions) {
        this.upcomingSessions = upcomingSessions;
    }
}
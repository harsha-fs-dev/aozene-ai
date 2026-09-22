package com.dubpilot.backend.dto;

import java.time.LocalDate;

public class ProjectStatusResponse {

    private Long projectId;
    private String projectName;
    private String stage;
    private Integer progressPercentage;
    private LocalDate deadline;
    private String status;

    public ProjectStatusResponse() {
    }

    public ProjectStatusResponse(Long projectId, String projectName, String stage,
                                 Integer progressPercentage, LocalDate deadline, String status) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.stage = stage;
        this.progressPercentage = progressPercentage;
        this.deadline = deadline;
        this.status = status;
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

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
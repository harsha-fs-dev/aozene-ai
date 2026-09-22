package com.dubpilot.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProjectProgressUpdateRequest {

    @NotNull(message = "progressPercentage is required")
    @Min(value = 0, message = "progressPercentage cannot be below 0")
    @Max(value = 100, message = "progressPercentage cannot be above 100")
    private Integer progressPercentage;

    @NotBlank(message = "stage is required")
    private String stage;

    public ProjectProgressUpdateRequest() {
    }

    public Integer getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }
}
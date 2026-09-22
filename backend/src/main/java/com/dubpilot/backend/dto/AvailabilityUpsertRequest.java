package com.dubpilot.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class AvailabilityUpsertRequest {

    @NotNull(message = "startDateTime is required")
    private LocalDateTime startDateTime;

    @NotNull(message = "endDateTime is required")
    private LocalDateTime endDateTime;

    @NotBlank(message = "availabilityStatus is required")
    private String availabilityStatus;

    public AvailabilityUpsertRequest() {
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }
}
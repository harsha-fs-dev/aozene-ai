package com.dubpilot.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class SessionStatusUpdateRequest {

    @NotBlank(message = "status is required")
    private String status;

    public SessionStatusUpdateRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
package com.dubpilot.backend.dto;

public class AvailabilityResult {

    private boolean available;
    private String reason;

    public AvailabilityResult() {
    }

    public AvailabilityResult(boolean available, String reason) {
        this.available = available;
        this.reason = reason;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
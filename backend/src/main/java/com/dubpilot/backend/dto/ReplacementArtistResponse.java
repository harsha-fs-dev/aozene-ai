package com.dubpilot.backend.dto;

public class ReplacementArtistResponse {

    private Long artistId;
    private String artistName;
    private String email;
    private String specialization;
    private boolean available;
    private String reason;

    public ReplacementArtistResponse() {
    }

    public ReplacementArtistResponse(Long artistId, String artistName, String email,
                                     String specialization, boolean available, String reason) {
        this.artistId = artistId;
        this.artistName = artistName;
        this.email = email;
        this.specialization = specialization;
        this.available = available;
        this.reason = reason;
    }

    public Long getArtistId() {
        return artistId;
    }

    public void setArtistId(Long artistId) {
        this.artistId = artistId;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
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
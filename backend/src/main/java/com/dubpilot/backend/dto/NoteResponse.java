package com.dubpilot.backend.dto;

import java.time.LocalDateTime;

public class NoteResponse {

    private Long id;
    private Long projectId;
    private Long episodeId;
    private String noteText;
    private LocalDateTime createdAt;

    public NoteResponse() {
    }

    public NoteResponse(Long id, Long projectId, Long episodeId, String noteText, LocalDateTime createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.episodeId = episodeId;
        this.noteText = noteText;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(Long episodeId) {
        this.episodeId = episodeId;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
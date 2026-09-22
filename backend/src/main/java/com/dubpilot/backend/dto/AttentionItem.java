package com.dubpilot.backend.dto;

public class AttentionItem {

    private String type;
    private String priority;
    private String title;
    private String description;

    public AttentionItem() {
    }

    public AttentionItem(String type, String priority, String title, String description) {
        this.type = type;
        this.priority = priority;
        this.title = title;
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
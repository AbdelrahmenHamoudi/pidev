package org.example.Entites.user;

import java.time.LocalDateTime;

public class AdminNotification {
    private int id;
    private String title;
    private String message;
    private String type; // INFO, SUCCESS, WARNING, ERROR
    private String priority; // LOW, MEDIUM, HIGH, URGENT
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String actionLink;
    private String actionText;
    private String createdBy;

    public AdminNotification() {}

    public AdminNotification(String title, String message, String type, String priority) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.priority = priority;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getActionLink() { return actionLink; }
    public void setActionLink(String actionLink) { this.actionLink = actionLink; }

    public String getActionText() { return actionText; }
    public void setActionText(String actionText) { this.actionText = actionText; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
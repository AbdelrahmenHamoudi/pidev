package org.example.Entites.user;

import java.time.LocalDateTime;

public class AdminActionLog {
    private int id;
    private int adminId;
    private String adminName;
    private String actionType; // CREATE, UPDATE, DELETE, LOGIN, LOGOUT, SUSPEND, ACTIVATE, EXPORT, VIEW
    private String targetType; // USER, HEBERGEMENT, ACTIVITE, etc.
    private int targetId;
    private String targetDescription;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;

    public AdminActionLog() {}

    public AdminActionLog(int adminId, String adminName, String actionType, String targetType, String details) {
        this.adminId = adminId;
        this.adminName = adminName;
        this.actionType = actionType;
        this.targetType = targetType;
        this.details = details;
        this.createdAt = LocalDateTime.now();
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAdminId() { return adminId; }
    public void setAdminId(int adminId) { this.adminId = adminId; }

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public int getTargetId() { return targetId; }
    public void setTargetId(int targetId) { this.targetId = targetId; }

    public String getTargetDescription() { return targetDescription; }
    public void setTargetDescription(String targetDescription) { this.targetDescription = targetDescription; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
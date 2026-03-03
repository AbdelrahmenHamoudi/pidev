package org.example.Entites.user;

import java.time.LocalDateTime;
import java.util.List;

public class User2FA {
    private int id;
    private int userId;
    private String secretKey;
    private boolean enabled;
    private LocalDateTime createdAt;
    private List<String> backupCodes;

    // Constructeurs
    public User2FA() {}

    public User2FA(int userId, String secretKey) {
        this.userId = userId;
        this.secretKey = secretKey;
        this.enabled = false;
        this.createdAt = LocalDateTime.now();
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<String> getBackupCodes() { return backupCodes; }
    public void setBackupCodes(List<String> backupCodes) { this.backupCodes = backupCodes; }
}
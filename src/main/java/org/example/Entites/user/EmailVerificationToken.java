package org.example.Entites.user;

import java.time.LocalDateTime;

public class EmailVerificationToken {
    private int id;
    private int userId;
    private String token;
    private String code;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean verified;

    public EmailVerificationToken() {}

    public EmailVerificationToken(int userId, String code) {
        this.userId = userId;
        this.code = code;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusHours(24);
        this.verified = false;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public boolean isValid() {
        return !verified && expiresAt.isAfter(LocalDateTime.now());
    }
}
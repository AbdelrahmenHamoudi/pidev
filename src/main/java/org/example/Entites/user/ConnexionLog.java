package org.example.Entites.user;

import java.time.LocalDateTime;

public class ConnexionLog {
    private int id;
    private int userId;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private String ipAddress;
    private String deviceInfo;
    private boolean success;
    private String failureReason;

    public ConnexionLog() {}

    public ConnexionLog(int userId, String ipAddress, String deviceInfo, boolean success) {
        this.userId = userId;
        this.loginTime = LocalDateTime.now();
        this.ipAddress = ipAddress;
        this.deviceInfo = deviceInfo;
        this.success = success;
    }


    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }

    public LocalDateTime getLogoutTime() { return logoutTime; }
    public void setLogoutTime(LocalDateTime logoutTime) { this.logoutTime = logoutTime; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
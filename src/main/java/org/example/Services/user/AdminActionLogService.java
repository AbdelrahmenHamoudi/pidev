package org.example.Services.user;

import org.example.Entites.user.AdminActionLog;
import org.example.Utils.MyBD;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminActionLogService {

    private Connection conn;

    public AdminActionLogService() {
        conn = MyBD.getInstance().getConnection();
    }

    // Enregistrer une action admin
    public void logAction(AdminActionLog log) {
        String sql = "INSERT INTO admin_action_logs (admin_id, admin_name, action_type, target_type, target_id, target_description, details, ip_address, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, log.getAdminId());
            pstmt.setString(2, log.getAdminName());
            pstmt.setString(3, log.getActionType());
            pstmt.setString(4, log.getTargetType());
            pstmt.setObject(5, log.getTargetId() > 0 ? log.getTargetId() : null);
            pstmt.setString(6, log.getTargetDescription());
            pstmt.setString(7, log.getDetails());
            pstmt.setString(8, log.getIpAddress());
            pstmt.setTimestamp(9, Timestamp.valueOf(log.getCreatedAt()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur log action: " + e.getMessage());
        }
    }

    // Récupérer tous les logs
    public List<AdminActionLog> getAllLogs(int limit) {
        List<AdminActionLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM admin_action_logs ORDER BY created_at DESC LIMIT ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération logs: " + e.getMessage());
        }

        return logs;
    }

    // Récupérer les logs par type d'action
    public List<AdminActionLog> getLogsByActionType(String actionType, int limit) {
        List<AdminActionLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM admin_action_logs WHERE action_type = ? ORDER BY created_at DESC LIMIT ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, actionType);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération logs par type: " + e.getMessage());
        }

        return logs;
    }

    // Récupérer les logs par admin
    public List<AdminActionLog> getLogsByAdmin(int adminId, int limit) {
        List<AdminActionLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM admin_action_logs WHERE admin_id = ? ORDER BY created_at DESC LIMIT ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, adminId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération logs par admin: " + e.getMessage());
        }

        return logs;
    }

    // Récupérer les logs par cible
    public List<AdminActionLog> getLogsByTarget(String targetType, int targetId, int limit) {
        List<AdminActionLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM admin_action_logs WHERE target_type = ? AND target_id = ? ORDER BY created_at DESC LIMIT ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, targetType);
            pstmt.setInt(2, targetId);
            pstmt.setInt(3, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération logs par cible: " + e.getMessage());
        }

        return logs;
    }

    // Mapping ResultSet -> AdminActionLog
    private AdminActionLog mapResultSetToLog(ResultSet rs) throws SQLException {
        AdminActionLog log = new AdminActionLog();
        log.setId(rs.getInt("id"));
        log.setAdminId(rs.getInt("admin_id"));
        log.setAdminName(rs.getString("admin_name"));
        log.setActionType(rs.getString("action_type"));
        log.setTargetType(rs.getString("target_type"));
        log.setTargetId(rs.getInt("target_id"));
        log.setTargetDescription(rs.getString("target_description"));
        log.setDetails(rs.getString("details"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return log;
    }
}
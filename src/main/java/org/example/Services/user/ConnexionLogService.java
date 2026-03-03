package org.example.Services.user;

import org.example.Entites.user.ConnexionLog;
import org.example.Entites.user.LocationInfo;
import org.example.Services.user.APIservices.GeolocationService;
import org.example.Utils.MyBD;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConnexionLogService {

    private Connection conn;

    public ConnexionLogService() {
        conn = MyBD.getInstance().getConnection();
    }

    /**
     * Enregistre une connexion avec localisation
     */
    public void logConnexion(int userId, String ipAddress, String deviceInfo, boolean success, String failureReason) {
        // Récupérer la localisation via ipapi
        LocationInfo location = GeolocationService.getLocationByIP(ipAddress);

        String country = location != null ? location.getCountryName() : null;
        String city = location != null ? location.getCity() : null;
        double latitude = location != null ? location.getLatitude() : 0;
        double longitude = location != null ? location.getLongitude() : 0;

        String sql = "INSERT INTO connexion_logs (user_id, login_time, ip_address, device_info, success, failure_reason, country, city, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setString(3, ipAddress);
            pstmt.setString(4, deviceInfo);
            pstmt.setBoolean(5, success);
            pstmt.setString(6, failureReason);
            pstmt.setString(7, country);
            pstmt.setString(8, city);
            pstmt.setDouble(9, latitude);
            pstmt.setDouble(10, longitude);
            pstmt.executeUpdate();

            System.out.println("✅ Log de connexion enregistré avec localisation: " +
                    (country != null ? country : "Inconnue") + ", " +
                    (city != null ? city : "Inconnue"));

        } catch (SQLException e) {
            System.err.println("❌ Erreur log connexion: " + e.getMessage());
        }
    }

    /**
     * Enregistre une déconnexion
     */
    public void logLogout(int userId) {
        String sql = "UPDATE connexion_logs SET logout_time = ? WHERE user_id = ? AND logout_time IS NULL ORDER BY login_time DESC LIMIT 1";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur log déconnexion: " + e.getMessage());
        }
    }

    /**
     * Récupère les logs d'un utilisateur
     */
    public List<ConnexionLog> getUserLogs(int userId, int limit) {
        List<ConnexionLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM connexion_logs WHERE user_id = ? ORDER BY login_time DESC LIMIT ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération logs: " + e.getMessage());
        }

        return logs;
    }

    /**
     * Récupère tous les logs (pour admin)
     */
    public List<ConnexionLog> getAllLogs(int limit) {
        List<ConnexionLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM connexion_logs ORDER BY login_time DESC LIMIT ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération tous logs: " + e.getMessage());
        }

        return logs;
    }

    /**
     * Statistiques: nombre de connexions aujourd'hui
     */
    public int getTodayConnexions() {
        String sql = "SELECT COUNT(*) FROM connexion_logs WHERE DATE(login_time) = CURDATE()";

        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur stats aujourd'hui: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Statistiques: connexions par pays
     */
    public List<Object[]> getCountryStats() {
        List<Object[]> stats = new ArrayList<>();
        String sql = "SELECT country, COUNT(*) as count FROM connexion_logs WHERE country IS NOT NULL AND country != '' GROUP BY country ORDER BY count DESC";

        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                stats.add(new Object[]{
                        rs.getString("country"),
                        rs.getInt("count")
                });
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur stats pays: " + e.getMessage());
        }

        return stats;
    }

    /**
     * Statistiques: connexions par ville
     */
    public List<Object[]> getCityStats() {
        List<Object[]> stats = new ArrayList<>();
        String sql = "SELECT city, country, COUNT(*) as count FROM connexion_logs WHERE city IS NOT NULL AND city != '' GROUP BY city, country ORDER BY count DESC LIMIT 10";

        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                stats.add(new Object[]{
                        rs.getString("city"),
                        rs.getString("country"),
                        rs.getInt("count")
                });
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur stats villes: " + e.getMessage());
        }

        return stats;
    }

    /**
     * Statistiques: connexions par jour (7 derniers jours)
     */
    public List<Object[]> getDailyStats() {
        List<Object[]> stats = new ArrayList<>();
        String sql = "SELECT DATE(login_time) as date, COUNT(*) as count, " +
                "SUM(CASE WHEN success = true THEN 1 ELSE 0 END) as success " +
                "FROM connexion_logs " +
                "WHERE login_time >= NOW() - INTERVAL 7 DAY " +
                "GROUP BY DATE(login_time) " +
                "ORDER BY date DESC";

        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                stats.add(new Object[]{
                        rs.getDate("date"),
                        rs.getInt("count"),
                        rs.getInt("success")
                });
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur stats journalières: " + e.getMessage());
        }

        return stats;
    }

    /**
     * Mapping ResultSet -> ConnexionLog
     */
    private ConnexionLog mapResultSetToLog(ResultSet rs) throws SQLException {
        ConnexionLog log = new ConnexionLog();
        log.setId(rs.getInt("id"));
        log.setUserId(rs.getInt("user_id"));
        log.setLoginTime(rs.getTimestamp("login_time").toLocalDateTime());

        Timestamp logoutTs = rs.getTimestamp("logout_time");
        if (logoutTs != null) {
            log.setLogoutTime(logoutTs.toLocalDateTime());
        }

        log.setIpAddress(rs.getString("ip_address"));
        log.setDeviceInfo(rs.getString("device_info"));
        log.setSuccess(rs.getBoolean("success"));
        log.setFailureReason(rs.getString("failure_reason"));

        // Champs de localisation
        log.setCountry(rs.getString("country"));
        log.setCity(rs.getString("city"));
        log.setLatitude(rs.getDouble("latitude"));
        log.setLongitude(rs.getDouble("longitude"));

        return log;
    }
}
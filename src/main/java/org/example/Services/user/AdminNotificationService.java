package org.example.Services.user;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.Entites.user.AdminNotification;
import org.example.Utils.MyBD;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminNotificationService {

    private Connection conn;
    private static final ObservableList<AdminNotification> liveNotifications = FXCollections.observableArrayList();

    public AdminNotificationService() {
        conn = MyBD.getInstance().getConnection();
    }

    // Créer une notification
    public void createNotification(AdminNotification notification) {
        String sql = "INSERT INTO admin_notifications (title, message, type, priority, is_read, created_at, expires_at, action_link, action_text, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, notification.getTitle());
            pstmt.setString(2, notification.getMessage());
            pstmt.setString(3, notification.getType());
            pstmt.setString(4, notification.getPriority());
            pstmt.setBoolean(5, notification.isRead());
            pstmt.setTimestamp(6, Timestamp.valueOf(notification.getCreatedAt()));
            pstmt.setTimestamp(7, notification.getExpiresAt() != null ? Timestamp.valueOf(notification.getExpiresAt()) : null);
            pstmt.setString(8, notification.getActionLink());
            pstmt.setString(9, notification.getActionText());
            pstmt.setString(10, notification.getCreatedBy());
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                notification.setId(rs.getInt(1));
            }

            liveNotifications.add(0, notification);

        } catch (SQLException e) {
            System.err.println("❌ Erreur création notification: " + e.getMessage());
        }
    }

    // Récupérer les notifications non lues
    public List<AdminNotification> getUnreadNotifications() {
        List<AdminNotification> list = new ArrayList<>();
        String sql = "SELECT * FROM admin_notifications WHERE is_read = false AND (expires_at IS NULL OR expires_at > NOW()) ORDER BY priority DESC, created_at DESC";

        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                list.add(mapResultSetToNotification(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération notifications: " + e.getMessage());
        }

        return list;
    }

    // Récupérer toutes les notifications
    public List<AdminNotification> getAllNotifications(int limit) {
        List<AdminNotification> list = new ArrayList<>();
        String sql = "SELECT * FROM admin_notifications ORDER BY created_at DESC LIMIT ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToNotification(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération toutes notifications: " + e.getMessage());
        }

        return list;
    }

    // Marquer comme lue
    public void markAsRead(int notificationId) {
        String sql = "UPDATE admin_notifications SET is_read = true WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, notificationId);
            pstmt.executeUpdate();

            liveNotifications.stream()
                    .filter(n -> n.getId() == notificationId)
                    .findFirst()
                    .ifPresent(n -> n.setRead(true));

        } catch (SQLException e) {
            System.err.println("❌ Erreur marquage notification: " + e.getMessage());
        }
    }

    // Marquer toutes comme lues
    public void markAllAsRead() {
        String sql = "UPDATE admin_notifications SET is_read = true";

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            liveNotifications.forEach(n -> n.setRead(true));
        } catch (SQLException e) {
            System.err.println("❌ Erreur marquage toutes notifications: " + e.getMessage());
        }
    }

    // Supprimer une notification
    public void deleteNotification(int notificationId) {
        String sql = "DELETE FROM admin_notifications WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, notificationId);
            pstmt.executeUpdate();
            liveNotifications.removeIf(n -> n.getId() == notificationId);
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression notification: " + e.getMessage());
        }
    }

    // Notifications automatiques
    public void notifyNewUser(String adminName, String userName) {
        AdminNotification notif = new AdminNotification(
                "👤 Nouvel utilisateur",
                userName + " vient de s'inscrire sur la plateforme.",
                "INFO",
                "MEDIUM"
        );
        notif.setCreatedBy(adminName);
        notif.setActionLink("/user/back/users.fxml");
        notif.setActionText("Voir l'utilisateur");
        createNotification(notif);
    }

    public void notifyUserSuspended(String adminName, String userName) {
        AdminNotification notif = new AdminNotification(
                "🚫 Utilisateur suspendu",
                userName + " a été suspendu par " + adminName,
                "WARNING",
                "HIGH"
        );
        notif.setCreatedBy(adminName);
        createNotification(notif);
    }

    public void notifyUserActivated(String adminName, String userName) {
        AdminNotification notif = new AdminNotification(
                "✅ Utilisateur activé",
                userName + " a été activé par " + adminName,
                "SUCCESS",
                "MEDIUM"
        );
        notif.setCreatedBy(adminName);
        createNotification(notif);
    }

    public void notifyNewReservation(String adminName, String userName, String hebergement) {
        AdminNotification notif = new AdminNotification(
                "📅 Nouvelle réservation",
                userName + " a réservé " + hebergement,
                "INFO",
                "LOW"
        );
        notif.setCreatedBy(adminName);
        createNotification(notif);
    }

    public void notifySystemAlert(String title, String message, String priority) {
        AdminNotification notif = new AdminNotification(
                title,
                message,
                "ERROR",
                priority
        );
        notif.setCreatedBy("Système");
        createNotification(notif);
    }

    // Mapping ResultSet -> Notification
    private AdminNotification mapResultSetToNotification(ResultSet rs) throws SQLException {
        AdminNotification n = new AdminNotification();
        n.setId(rs.getInt("id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(rs.getString("type"));
        n.setPriority(rs.getString("priority"));
        n.setRead(rs.getBoolean("is_read"));
        n.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp expires = rs.getTimestamp("expires_at");
        if (expires != null) {
            n.setExpiresAt(expires.toLocalDateTime());
        }
        n.setActionLink(rs.getString("action_link"));
        n.setActionText(rs.getString("action_text"));
        n.setCreatedBy(rs.getString("created_by"));
        return n;
    }

    public static ObservableList<AdminNotification> getLiveNotifications() {
        return liveNotifications;
    }
}
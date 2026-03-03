package org.example.Services.hebergement;

import org.example.Entites.hebergement.Notification;
import org.example.Utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationCRUD {

    private Connection con;

    public NotificationCRUD() {
        con = MyBD.getInstance().getConnection();
    }

    public void ajouter(Notification notif) throws SQLException {
        String sql = "INSERT INTO notification (user_id, message, lue) VALUES (?, ?, false)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, notif.getUserId());
            ps.setString(2, notif.getMessage());
            ps.executeUpdate();
        }
    }

    public List<Notification> getByUser(int userId) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notification WHERE user_id = ? ORDER BY date_creation DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Notification n = new Notification(userId, rs.getString("message"));
                n.setId(rs.getInt("id"));
                n.setLue(rs.getBoolean("lue"));
                n.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
                list.add(n);
            }
        }
        return list;
    }

    public int compterNonLues(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notification WHERE user_id = ? AND lue = false";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public void marquerToutesLues(int userId) throws SQLException {
        String sql = "UPDATE notification SET lue = true WHERE user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}
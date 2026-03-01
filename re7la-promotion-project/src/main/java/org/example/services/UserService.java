package org.example.services;

import org.example.models.User;
import org.example.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ✅ FIXED: Aligned with re7la_3a9 `users` table.
 *
 * Column mapping changes:
 *   - Table: `user` → `users`
 *   - `email` → `e_mail`
 *   - `password` → `mot_de_pass`
 *   - No `created_at` in re7la_3a9 users table → removed from extract
 *   - Added `role` and `status` fields
 */
public class UserService {

    public List<User> getAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, nom, prenom, e_mail, mot_de_pass, role, status FROM users ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) users.add(extractUser(rs));
        } catch (SQLException e) {
            System.err.println("❌ [UserService] getAll : " + e.getMessage());
        }
        return users;
    }

    public Optional<User> getById(int id) {
        String sql = "SELECT id, nom, prenom, e_mail, mot_de_pass, role, status FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(extractUser(rs));
        } catch (SQLException e) {
            System.err.println("❌ [UserService] getById #" + id + " : " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<User> getByEmail(String email) {
        String sql = "SELECT id, nom, prenom, e_mail, mot_de_pass, role, status FROM users WHERE e_mail = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(extractUser(rs));
        } catch (SQLException e) {
            System.err.println("❌ [UserService] getByEmail : " + e.getMessage());
        }
        return Optional.empty();
    }

    private User extractUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("e_mail"));
        u.setPassword(rs.getString("mot_de_pass"));
        return u;
    }
}
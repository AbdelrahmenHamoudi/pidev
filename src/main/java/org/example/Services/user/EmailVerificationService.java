package org.example.Services.user;

import org.example.Services.user.APIservices.EmailService;
import org.example.Entites.user.EmailVerificationToken;
import org.example.Entites.user.User;
import org.example.Utils.MyBD;

import java.sql.*;
import java.time.LocalDateTime;

public class EmailVerificationService {

    private Connection conn;
    private final UserCRUD userCRUD = new UserCRUD();

    public EmailVerificationService() {
        conn = MyBD.getInstance().getConnection();
    }

    /**
     * Crée un token de vérification, envoie l'email et RETOURNE le code
     */
    public String sendVerificationEmail(int userId) {
        try {
            User user = userCRUD.getUserById(userId);
            if (user == null) return null;

            String code = EmailService.generateVerificationCode();

            // Supprimer les anciens tokens
            deleteOldTokens(userId);

            // Créer le nouveau token
            String sql = "INSERT INTO email_verification_tokens (user_id, code, created_at, expires_at) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, code);
                pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now().plusHours(24)));
                pstmt.executeUpdate();
            }

            // Envoyer l'email
            EmailService.sendVerificationEmail(user.getE_mail(), user.getPrenom(), code);

            // ✅ RETOURNER LE CODE
            return code;

        } catch (SQLException e) {
            System.err.println("❌ Erreur envoi vérification email: " + e.getMessage());
            return null;
        }
    }

    /**
     * Vérifie le code
     */
    public boolean verifyCode(int userId, String code) {
        String sql = "SELECT * FROM email_verification_tokens WHERE user_id = ? AND code = ? AND verified = false AND expires_at > NOW() ORDER BY created_at DESC LIMIT 1";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, code);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Marquer comme vérifié
                String updateSql = "UPDATE email_verification_tokens SET verified = true WHERE id = ?";
                try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                    updatePstmt.setInt(1, rs.getInt("id"));
                    updatePstmt.executeUpdate();
                }

                // Mettre à jour l'utilisateur (email vérifié)
                String updateUserSql = "UPDATE users SET email_verified = true WHERE id = ?";
                try (PreparedStatement updateUserPstmt = conn.prepareStatement(updateUserSql)) {
                    updateUserPstmt.setInt(1, userId);
                    updateUserPstmt.executeUpdate();
                }

                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur vérification code: " + e.getMessage());
        }

        return false;
    }

    /**
     * Supprime les anciens tokens
     */
    private void deleteOldTokens(int userId) {
        String sql = "DELETE FROM email_verification_tokens WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression anciens tokens: " + e.getMessage());
        }
    }
}
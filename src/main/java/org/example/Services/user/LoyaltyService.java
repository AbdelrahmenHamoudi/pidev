package org.example.Services.user;

import org.example.Entites.user.UserStats;
import org.example.Utils.MyBD;

import java.sql.*;

public class LoyaltyService {

    private Connection conn;
    private final UserCRUD userCRUD = new UserCRUD();

    public LoyaltyService() {
        conn = MyBD.getInstance().getConnection();
    }

    /**
     * Récupère les statistiques d'un utilisateur
     */
    public UserStats getUserStats(int userId) {
        UserStats stats = new UserStats();
        stats.setUserId(userId);

        String sql = "SELECT * FROM user_stats WHERE user_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                stats.setTotalPoints(rs.getInt("total_points"));
                stats.setCurrentLevel(rs.getInt("current_level"));
                stats.setExperiencePoints(rs.getInt("experience_points"));
                stats.setTotalReservations(rs.getInt("total_reservations"));
                stats.setTotalReviews(rs.getInt("total_reviews"));
                stats.setTotalSpent(rs.getDouble("total_spent"));
                stats.setRank(calculateRank(stats.getTotalPoints()));
                stats.setNextLevelPoints(calculateNextLevelPoints(stats.getCurrentLevel()));
            } else {
                // Créer des stats par défaut
                initializeUserStats(userId);
                stats.setTotalPoints(0);
                stats.setCurrentLevel(1);
                stats.setExperiencePoints(0);
                stats.setTotalReservations(0);
                stats.setTotalReviews(0);
                stats.setTotalSpent(0);
                stats.setRank("Novice");
                stats.setNextLevelPoints(100);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération des stats: " + e.getMessage());
        }

        return stats;
    }

    /**
     * Initialise les statistiques d'un nouvel utilisateur
     */
    private void initializeUserStats(int userId) {
        String sql = "INSERT INTO user_stats (user_id, total_points, current_level, experience_points, total_reservations, total_reviews, total_spent) VALUES (?, 0, 1, 0, 0, 0, 0)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            System.out.println("✅ Stats initialisées pour l'utilisateur " + userId);
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'initialisation des stats: " + e.getMessage());
        }
    }

    /**
     * Ajoute des points à un utilisateur
     */
    public void addPoints(int userId, int points, String reason) {
        String sql = "UPDATE user_stats SET total_points = total_points + ?, experience_points = experience_points + ? WHERE user_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, points);
            pstmt.setInt(2, points);
            pstmt.setInt(3, userId);
            int affected = pstmt.executeUpdate();

            if (affected > 0) {
                System.out.println("✅ " + points + " points ajoutés à l'utilisateur " + userId);

                // Vérifier si le niveau doit augmenter
                checkLevelUp(userId);

                // Enregistrer la transaction de points
                logPointsTransaction(userId, points, reason);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'ajout de points: " + e.getMessage());
        }
    }

    /**
     * Vérifie si l'utilisateur doit monter de niveau
     */
    private void checkLevelUp(int userId) {
        String sql = "SELECT current_level, experience_points FROM user_stats WHERE user_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int currentLevel = rs.getInt("current_level");
                int exp = rs.getInt("experience_points");
                int nextLevelExp = calculateNextLevelPoints(currentLevel);
                boolean leveledUp = false;

                while (exp >= nextLevelExp) {
                    currentLevel++;
                    exp -= nextLevelExp;
                    nextLevelExp = calculateNextLevelPoints(currentLevel);
                    leveledUp = true;
                }

                if (leveledUp) {
                    // Mettre à jour le niveau
                    String updateSql = "UPDATE user_stats SET current_level = ?, experience_points = ? WHERE user_id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, currentLevel);
                        updateStmt.setInt(2, exp);
                        updateStmt.setInt(3, userId);
                        updateStmt.executeUpdate();
                        System.out.println("🎉 L'utilisateur " + userId + " est passé au niveau " + currentLevel);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la vérification du niveau: " + e.getMessage());
        }
    }

    /**
     * Calcule les points nécessaires pour le prochain niveau
     */
    private int calculateNextLevelPoints(int level) {
        return level * 100; // Niveau 1: 100, Niveau 2: 200, etc.
    }

    /**
     * Calcule le rang en fonction des points
     */
    private String calculateRank(int points) {
        if (points < 100) return "Novice";
        if (points < 500) return "Bronze";
        if (points < 1000) return "Argent";
        if (points < 5000) return "Or";
        if (points < 10000) return "Platine";
        return "Diamant";
    }

    /**
     * Enregistre une transaction de points
     */
    private void logPointsTransaction(int userId, int points, String reason) {
        String sql = "INSERT INTO points_transactions (user_id, points, reason, transaction_date) VALUES (?, ?, ?, NOW())";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, points);
            pstmt.setString(3, reason);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'enregistrement de la transaction: " + e.getMessage());
        }
    }

    /**
     * Ajoute des points pour une réservation
     */
    public void addReservationPoints(int userId, double amount) {
        int points = (int) (amount / 10); // 1 point pour 10 DT
        addPoints(userId, points, "Réservation d'hébergement");

        // Mettre à jour le compteur de réservations et le total dépensé
        String sql = "UPDATE user_stats SET total_reservations = total_reservations + 1, total_spent = total_spent + ? WHERE user_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la mise à jour des réservations: " + e.getMessage());
        }
    }

    /**
     * Ajoute des points pour un avis
     */
    public void addReviewPoints(int userId) {
        addPoints(userId, 50, "Avis laissé");

        String sql = "UPDATE user_stats SET total_reviews = total_reviews + 1 WHERE user_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la mise à jour des avis: " + e.getMessage());
        }
    }

    /**
     * Récupère le classement des utilisateurs par points
     */
    public ResultSet getLeaderboard(int limit) {
        String sql = "SELECT u.id, u.nom, u.prenom, s.total_points " +
                "FROM users u " +
                "JOIN user_stats s ON u.id = s.user_id " +
                "ORDER BY s.total_points DESC " +
                "LIMIT ?";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, limit);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération du classement: " + e.getMessage());
            return null;
        }
    }
}
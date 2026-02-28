package org.example.services;

import org.example.models.ReservationPromo;
import org.example.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationPromoService {

    public List<ReservationPromo> getAll() {
        List<ReservationPromo> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservation_promo ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) reservations.add(extractFromResultSet(rs));
        } catch (SQLException e) {
            System.err.println("Erreur getAll réservations"); e.printStackTrace();
        }
        return reservations;
    }

    public List<ReservationPromo> getByUserId(int userId) {
        List<ReservationPromo> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservation_promo WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) reservations.add(extractFromResultSet(rs));
        } catch (SQLException e) {
            System.err.println("Erreur getByUserId #" + userId); e.printStackTrace();
        }
        return reservations;
    }

    public Optional<ReservationPromo> getById(int id) {
        String sql = "SELECT * FROM reservation_promo WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return Optional.of(extractFromResultSet(rs));
        } catch (SQLException e) {
            System.err.println("Erreur getById #" + id); e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Ajouter une réservation + incrémenter nb_reservations dans promotion
     */
    public ReservationPromo add(ReservationPromo reservation) {
        String sql = "INSERT INTO reservation_promo (user_id, promotion_id, date_debut_reservation, " +
                "date_fin_reservation, nb_jours, prix_par_jour, prix_original, " +
                "reduction_appliquee, montant_total) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, reservation.getUserId());
            pstmt.setInt(2, reservation.getPromotionId());
            pstmt.setDate(3, reservation.getDateDebutReservation());
            pstmt.setDate(4, reservation.getDateFinReservation());
            pstmt.setInt(5, reservation.getNbJours());
            pstmt.setFloat(6, reservation.getPrixParJour());
            pstmt.setFloat(7, reservation.getPrixOriginal());
            pstmt.setFloat(8, reservation.getReductionAppliquee());
            pstmt.setFloat(9, reservation.getMontantTotal());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    reservation.setId(generatedKeys.getInt(1));
                }

                // ✅ FIX — incrémenter nb_reservations dans la table promotion
                incrementReservations(reservation.getPromotionId());

                System.out.println("✅ Réservation créée: ID=" + reservation.getId()
                        + " · nb_reservations incrémenté pour promo #" + reservation.getPromotionId());
                return reservation;
            }

        } catch (SQLException e) {
            System.err.println("Erreur add réservation"); e.printStackTrace();
        }
        return null;
    }

    public boolean update(ReservationPromo reservation) {
        String sql = "UPDATE reservation_promo SET date_debut_reservation = ?, " +
                "date_fin_reservation = ?, nb_jours = ?, prix_par_jour = ?, " +
                "prix_original = ?, reduction_appliquee = ?, montant_total = ? " +
                "WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, reservation.getDateDebutReservation());
            pstmt.setDate(2, reservation.getDateFinReservation());
            pstmt.setInt(3, reservation.getNbJours());
            pstmt.setFloat(4, reservation.getPrixParJour());
            pstmt.setFloat(5, reservation.getPrixOriginal());
            pstmt.setFloat(6, reservation.getReductionAppliquee());
            pstmt.setFloat(7, reservation.getMontantTotal());
            pstmt.setInt(8, reservation.getId());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("✅ Réservation modifiée: ID=" + reservation.getId());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur update réservation"); e.printStackTrace();
        }
        return false;
    }

    /**
     * Supprimer une réservation + décrémenter nb_reservations
     */
    public boolean delete(int id) {
        // Récupérer le promotionId avant suppression
        int promotionId = -1;
        Optional<ReservationPromo> existing = getById(id);
        if (existing.isPresent()) promotionId = existing.get().getPromotionId();

        String sql = "DELETE FROM reservation_promo WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                // ✅ FIX — décrémenter nb_reservations (sans passer sous 0)
                if (promotionId > 0) decrementReservations(promotionId);
                System.out.println("✅ Réservation supprimée: ID=" + id);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur delete réservation"); e.printStackTrace();
        }
        return false;
    }

    public boolean canDelete(ReservationPromo reservation) {
        if (reservation.getCreatedAt() == null) return false;
        LocalDateTime createdAt = reservation.getCreatedAt().toLocalDateTime();
        long hoursSinceCreation = ChronoUnit.HOURS.between(createdAt, LocalDateTime.now());
        return hoursSinceCreation < 24;
    }

    // ════════════════════════════════════════════════════
    // HELPERS — incrément / décrément nb_reservations
    // ════════════════════════════════════════════════════

    private void incrementReservations(int promotionId) {
        String sql = "UPDATE promotion SET nb_reservations = nb_reservations + 1 WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, promotionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur incrementReservations promo #" + promotionId); e.printStackTrace();
        }
    }

    private void decrementReservations(int promotionId) {
        String sql = "UPDATE promotion SET nb_reservations = GREATEST(0, nb_reservations - 1) WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, promotionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur decrementReservations promo #" + promotionId); e.printStackTrace();
        }
    }

    private ReservationPromo extractFromResultSet(ResultSet rs) throws SQLException {
        return new ReservationPromo(
                rs.getInt("id"), rs.getInt("user_id"), rs.getInt("promotion_id"),
                rs.getDate("date_debut_reservation"), rs.getDate("date_fin_reservation"),
                rs.getInt("nb_jours"), rs.getFloat("prix_par_jour"),
                rs.getFloat("prix_original"), rs.getFloat("reduction_appliquee"),
                rs.getFloat("montant_total"), rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at"));
    }
}
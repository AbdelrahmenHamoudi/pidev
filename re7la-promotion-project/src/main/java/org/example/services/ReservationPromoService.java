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

    /**
     * Récupérer toutes les réservations
     */
    public List<ReservationPromo> getAll() {
        List<ReservationPromo> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservation_promo ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                reservations.add(extractFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des réservations");
            e.printStackTrace();
        }

        return reservations;
    }

    /**
     * Récupérer les réservations d'un user
     */
    public List<ReservationPromo> getByUserId(int userId) {
        List<ReservationPromo> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservation_promo WHERE user_id = ? ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                reservations.add(extractFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des réservations du user #" + userId);
            e.printStackTrace();
        }

        return reservations;
    }

    /**
     * Récupérer une réservation par ID
     */
    public Optional<ReservationPromo> getById(int id) {
        String sql = "SELECT * FROM reservation_promo WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(extractFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de la réservation #" + id);
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Ajouter une nouvelle réservation
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
            }

            System.out.println("✅ Réservation créée: ID=" + reservation.getId());
            return reservation;

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout de la réservation");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Mettre à jour une réservation
     */
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
            System.err.println("Erreur lors de la modification de la réservation");
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Supprimer une réservation
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM reservation_promo WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✅ Réservation supprimée: ID=" + id);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de la réservation");
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Vérifier si une réservation peut être annulée (< 24h)
     */
    public boolean canDelete(ReservationPromo reservation) {
        if (reservation.getCreatedAt() == null) {
            return false;
        }

        LocalDateTime createdAt = reservation.getCreatedAt().toLocalDateTime();
        LocalDateTime now = LocalDateTime.now();

        long hoursSinceCreation = ChronoUnit.HOURS.between(createdAt, now);

        return hoursSinceCreation < 24;
    }

    /**
     * Extraire une ReservationPromo depuis un ResultSet
     */
    private ReservationPromo extractFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        int promotionId = rs.getInt("promotion_id");
        Date dateDebut = rs.getDate("date_debut_reservation");
        Date dateFin = rs.getDate("date_fin_reservation");
        int nbJours = rs.getInt("nb_jours");
        float prixParJour = rs.getFloat("prix_par_jour");
        float prixOriginal = rs.getFloat("prix_original");
        float reduction = rs.getFloat("reduction_appliquee");
        float montantTotal = rs.getFloat("montant_total");
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");

        return new ReservationPromo(id, userId, promotionId, dateDebut, dateFin,
                nbJours, prixParJour, prixOriginal, reduction,
                montantTotal, createdAt, updatedAt);
    }
}

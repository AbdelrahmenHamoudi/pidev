package org.example.Services.promotion;

import org.example.Entites.promotion.Promotion;
import org.example.Entites.promotion.ReservationPromo;
import org.example.Entites.user.User;
import org.example.Utils.MyBD;
import org.example.Utils.UserSession;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationPromoService {

    private PromotionService promotionService;

    public ReservationPromoService() {
        this.promotionService = new PromotionService();
    }

    public List<ReservationPromo> getAll() {
        List<ReservationPromo> reservations = new ArrayList<>();
        String sql = "SELECT rp.*, u.id as user_id, u.nom, u.prenom, u.e_mail, " +
                "p.id as promo_id, p.name as promo_name " +
                "FROM reservation_promo rp " +
                "JOIN users u ON rp.user_id = u.id " +
                "JOIN promotion p ON rp.promotion_id = p.id " +
                "ORDER BY rp.created_at DESC";
        try (Connection conn = MyBD.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur getAll réservations");
            e.printStackTrace();
        }
        return reservations;
    }

    public List<ReservationPromo> getByUserId(int userId) {
        List<ReservationPromo> reservations = new ArrayList<>();
        String sql = "SELECT rp.*, u.id as user_id, u.nom, u.prenom, u.e_mail, " +
                "p.id as promo_id, p.name as promo_name " +
                "FROM reservation_promo rp " +
                "JOIN users u ON rp.user_id = u.id " +
                "JOIN promotion p ON rp.promotion_id = p.id " +
                "WHERE rp.user_id = ? " +
                "ORDER BY rp.created_at DESC";
        try (Connection conn = MyBD.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur getByUserId #" + userId);
            e.printStackTrace();
        }
        return reservations;
    }

    public List<ReservationPromo> getMesReservations() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            return new ArrayList<>();
        }
        return getByUserId(currentUser.getId());
    }

    public Optional<ReservationPromo> getById(int id) {
        String sql = "SELECT rp.*, u.id as user_id, u.nom, u.prenom, u.e_mail, " +
                "p.id as promo_id, p.name as promo_name " +
                "FROM reservation_promo rp " +
                "JOIN users u ON rp.user_id = u.id " +
                "JOIN promotion p ON rp.promotion_id = p.id " +
                "WHERE rp.id = ?";
        try (Connection conn = MyBD.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToReservation(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur getById #" + id);
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public ReservationPromo add(ReservationPromo reservation) {
        String sql = "INSERT INTO reservation_promo (user_id, promotion_id, date_debut_reservation, " +
                "date_fin_reservation, nb_jours, prix_par_jour, prix_original, " +
                "reduction_appliquee, montant_total) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = MyBD.getInstance().getConnection();
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

                incrementReservations(reservation.getPromotionId());

                System.out.println("✅ Réservation promo créée: ID=" + reservation.getId()
                        + " pour user #" + reservation.getUserId());
                return reservation;
            }

        } catch (SQLException e) {
            System.err.println("Erreur add réservation");
            e.printStackTrace();
        }
        return null;
    }

    public boolean update(ReservationPromo reservation) {
        String sql = "UPDATE reservation_promo SET date_debut_reservation = ?, " +
                "date_fin_reservation = ?, nb_jours = ?, prix_par_jour = ?, " +
                "prix_original = ?, reduction_appliquee = ?, montant_total = ? " +
                "WHERE id = ?";
        try (Connection conn = MyBD.getInstance().getConnection();
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
            System.err.println("Erreur update réservation");
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(int id) {
        int promotionId = -1;
        Optional<ReservationPromo> existing = getById(id);
        if (existing.isPresent()) {
            promotionId = existing.get().getPromotionId();
        }

        String sql = "DELETE FROM reservation_promo WHERE id = ?";
        try (Connection conn = MyBD.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                if (promotionId > 0) decrementReservations(promotionId);
                System.out.println("✅ Réservation supprimée: ID=" + id);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur delete réservation");
            e.printStackTrace();
        }
        return false;
    }

    public boolean canDelete(ReservationPromo reservation) {
        if (reservation.getCreatedAt() == null) return false;
        LocalDateTime createdAt = reservation.getCreatedAt().toLocalDateTime();
        long hoursSinceCreation = ChronoUnit.HOURS.between(createdAt, LocalDateTime.now());
        return hoursSinceCreation < 24;
    }

    private void incrementReservations(int promotionId) {
        String sql = "UPDATE promotion SET nb_reservations = nb_reservations + 1 WHERE id = ?";
        try (Connection conn = MyBD.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, promotionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur incrementReservations promo #" + promotionId);
            e.printStackTrace();
        }
    }

    private void decrementReservations(int promotionId) {
        String sql = "UPDATE promotion SET nb_reservations = GREATEST(0, nb_reservations - 1) WHERE id = ?";
        try (Connection conn = MyBD.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, promotionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur decrementReservations promo #" + promotionId);
            e.printStackTrace();
        }
    }

    private ReservationPromo mapResultSetToReservation(ResultSet rs) throws SQLException {
        // Créer l'utilisateur
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setE_mail(rs.getString("e_mail"));

        // Créer la promotion
        Promotion promotion = new Promotion();
        promotion.setId(rs.getInt("promo_id"));
        promotion.setName(rs.getString("promo_name"));

        ReservationPromo reservation = new ReservationPromo();
        reservation.setId(rs.getInt("id"));
        reservation.setUser(user);
        reservation.setPromotion(promotion);
        reservation.setDateDebutReservation(rs.getDate("date_debut_reservation"));
        reservation.setDateFinReservation(rs.getDate("date_fin_reservation"));
        reservation.setNbJours(rs.getInt("nb_jours"));
        reservation.setPrixParJour(rs.getFloat("prix_par_jour"));
        reservation.setPrixOriginal(rs.getFloat("prix_original"));
        reservation.setReductionAppliquee(rs.getFloat("reduction_appliquee"));
        reservation.setMontantTotal(rs.getFloat("montant_total"));
        reservation.setCreatedAt(rs.getTimestamp("created_at"));
        reservation.setUpdatedAt(rs.getTimestamp("updated_at"));

        return reservation;
    }
}
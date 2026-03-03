package org.example.Services.activite;

import org.example.Entites.activite.Activite;
import org.example.Entites.activite.Planning;
import org.example.Entites.activite.Reservation;
import org.example.Entites.activite.ReservationDetail;  // ✅ Importer la classe
import org.example.Entites.user.User;
import org.example.Utils.MyBD;
import org.example.Utils.UserSession;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationServiceImpl implements ReservationService {

    private Connection connection;
    private PlaningActiviteImpl planningService;

    public ReservationServiceImpl() {
        this.connection = MyBD.getInstance().getConnection();
        this.planningService = new PlaningActiviteImpl();
    }

    // ✅ NOUVELLE MÉTHODE : Récupère les réservations de l'utilisateur connecté
    public List<ReservationDetail> getMesReservations() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            System.err.println("❌ Aucun utilisateur connecté");
            return new ArrayList<>();
        }
        return getReservationsDetailsByUser(currentUser.getId());
    }

    // ✅ NOUVELLE MÉTHODE : Récupère les réservations d'un utilisateur spécifique
    public List<ReservationDetail> getReservationsDetailsByUser(int userId) {
        List<ReservationDetail> reservations = new ArrayList<>();

        String sql = """
            SELECT r.*, 
                   p.id_planning, p.date_planning, p.heure_debut, p.heure_fin, p.nb_places_restantes, p.etat as planning_etat,
                   a.id_activite, a.nomA, a.descriptionA, a.lieu, a.prix_par_personne, a.type, a.statut as activite_statut, a.image,
                   u.id as user_id, u.nom, u.prenom, u.e_mail, u.num_tel, u.role, u.status
            FROM reservation r
            JOIN planning p ON r.id_planning = p.id_planning
            JOIN activite a ON p.id_activite = a.id_activite
            JOIN users u ON r.id_user = u.id
            WHERE r.id_user = ?
            ORDER BY r.id_reservation DESC
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservationDetail(rs));
                }
            }
            System.out.println("✅ " + reservations.size() + " réservations chargées pour l'utilisateur #" + userId);
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération réservations: " + e.getMessage());
            e.printStackTrace();
        }
        return reservations;
    }

    // ✅ NOUVELLE MÉTHODE : Récupère toutes les réservations (admin)
    public List<ReservationDetail> getAllReservationsDetails() {
        List<ReservationDetail> reservations = new ArrayList<>();

        String sql = """
            SELECT r.*, 
                   p.id_planning, p.date_planning, p.heure_debut, p.heure_fin, p.nb_places_restantes, p.etat as planning_etat,
                   a.id_activite, a.nomA, a.descriptionA, a.lieu, a.prix_par_personne, a.type, a.statut as activite_statut, a.image,
                   u.id as user_id, u.nom, u.prenom, u.e_mail, u.num_tel, u.role, u.status
            FROM reservation r
            JOIN planning p ON r.id_planning = p.id_planning
            JOIN activite a ON p.id_activite = a.id_activite
            JOIN users u ON r.id_user = u.id
            ORDER BY r.id_reservation DESC
        """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                reservations.add(mapResultSetToReservationDetail(rs));
            }
            System.out.println("✅ " + reservations.size() + " réservations totales chargées");
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération toutes réservations: " + e.getMessage());
            e.printStackTrace();
        }
        return reservations;
    }

    // ✅ NOUVELLE MÉTHODE : Mapping vers ReservationDetail
    private ReservationDetail mapResultSetToReservationDetail(ResultSet rs) throws SQLException {
        ReservationDetail detail = new ReservationDetail();

        detail.id_reservation = rs.getInt("id_reservation");
        detail.id_user = rs.getInt("id_user");
        detail.id_planning = rs.getInt("id_planning");
        detail.statut = rs.getString("statut");
        detail.date_planning = rs.getDate("date_planning").toLocalDate();
        detail.heure_debut = rs.getTime("heure_debut").toLocalTime();
        detail.heure_fin = rs.getTime("heure_fin").toLocalTime();
        detail.nb_places = rs.getInt("nb_places_restantes");
        detail.id_activite = rs.getInt("id_activite");
        detail.nomActivite = rs.getString("nomA");
        detail.lieu = rs.getString("lieu");
        detail.type = rs.getString("type");
        detail.prix = rs.getFloat("prix_par_personne");

        return detail;
    }

    // ═══════════════════════════════════════════════════════════════
    //  MÉTHODES EXISTANTES (non modifiées)
    // ═══════════════════════════════════════════════════════════════

    @Override
    public boolean addReservation(Reservation reservation) {
        // ... votre code existant ...
        if (hasAlreadyReserved(reservation.getUser().getId(), reservation.getPlanning().getIdPlanning())) {
            System.out.println("⚠️ L'utilisateur a déjà réservé ce planning");
            return false;
        }

        String sql = "INSERT INTO reservation (id_user, id_planning, statut) VALUES (?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, reservation.getUser().getId());
            ps.setInt(2, reservation.getPlanning().getIdPlanning());
            ps.setString(3, reservation.getStatut() != null ? reservation.getStatut() : "CONFIRMEE");

            int affected = ps.executeUpdate();

            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        reservation.setIdReservation(rs.getInt(1));
                    }
                }
                planningService.reserverPlace(reservation.getPlanning().getIdPlanning());
                System.out.println("✅ Réservation ajoutée avec succès");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'ajout de la réservation: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean annulerReservation(int idReservation, int idUser) {
        String checkSql = "SELECT id_planning FROM reservation WHERE id_reservation = ? AND id_user = ? AND statut = 'CONFIRMEE'";

        try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
            ps.setInt(1, idReservation);
            ps.setInt(2, idUser);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int idPlanning = rs.getInt("id_planning");

                    String updateSql = "UPDATE reservation SET statut = 'ANNULEE' WHERE id_reservation = ?";
                    try (PreparedStatement ps2 = connection.prepareStatement(updateSql)) {
                        ps2.setInt(1, idReservation);
                        ps2.executeUpdate();
                    }

                    planningService.libererPlace(idPlanning);

                    System.out.println("✅ Réservation #" + idReservation + " annulée");
                    return true;
                } else {
                    System.out.println("⚠️ Réservation non trouvée ou ne vous appartient pas");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'annulation: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Reservation> getReservationsByUser(int idUser) {
        List<Reservation> reservations = new ArrayList<>();

        String sql = """
            SELECT r.*, 
                   p.id_planning, p.date_planning, p.heure_debut, p.heure_fin, p.nb_places_restantes, p.etat as planning_etat,
                   a.id_activite, a.nomA, a.descriptionA, a.lieu, a.prix_par_personne, a.type, a.statut as activite_statut, a.image,
                   u.id as user_id, u.nom, u.prenom, u.e_mail, u.num_tel, u.role, u.status
            FROM reservation r
            JOIN planning p ON r.id_planning = p.id_planning
            JOIN activite a ON p.id_activite = a.id_activite
            JOIN users u ON r.id_user = u.id
            WHERE r.id_user = ?
            ORDER BY r.id_reservation DESC
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }
            System.out.println("✅ " + reservations.size() + " réservations chargées pour l'utilisateur #" + idUser);
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération réservations: " + e.getMessage());
            e.printStackTrace();
        }
        return reservations;
    }

    @Override
    public Reservation getReservationById(int id) {
        String sql = """
            SELECT r.*, 
                   p.id_planning, p.date_planning, p.heure_debut, p.heure_fin, p.nb_places_restantes, p.etat as planning_etat,
                   a.id_activite, a.nomA, a.descriptionA, a.lieu, a.prix_par_personne, a.type, a.statut as activite_statut, a.image,
                   u.id as user_id, u.nom, u.prenom, u.e_mail, u.num_tel, u.role, u.status
            FROM reservation r
            JOIN planning p ON r.id_planning = p.id_planning
            JOIN activite a ON p.id_activite = a.id_activite
            JOIN users u ON r.id_user = u.id
            WHERE r.id_reservation = ?
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReservation(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération réservation #" + id + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean hasAlreadyReserved(int idUser, int idPlanning) {
        String sql = "SELECT 1 FROM reservation WHERE id_user = ? AND id_planning = ? AND statut = 'CONFIRMEE'";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            ps.setInt(2, idPlanning);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur vérification réservation: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Reservation> getAllReservations() {
        List<Reservation> reservations = new ArrayList<>();

        String sql = """
            SELECT r.*, 
                   p.id_planning, p.date_planning, p.heure_debut, p.heure_fin, p.nb_places_restantes, p.etat as planning_etat,
                   a.id_activite, a.nomA, a.descriptionA, a.lieu, a.prix_par_personne, a.type, a.statut as activite_statut, a.image,
                   u.id as user_id, u.nom, u.prenom, u.e_mail, u.num_tel, u.role, u.status
            FROM reservation r
            JOIN planning p ON r.id_planning = p.id_planning
            JOIN activite a ON p.id_activite = a.id_activite
            JOIN users u ON r.id_user = u.id
            ORDER BY r.id_reservation DESC
        """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
            System.out.println("✅ " + reservations.size() + " réservations totales chargées");
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération toutes réservations: " + e.getMessage());
            e.printStackTrace();
        }
        return reservations;
    }

    @Override
    public List<Reservation> getReservationsByPlanning(int idPlanning) {
        List<Reservation> reservations = new ArrayList<>();

        String sql = """
            SELECT r.*, 
                   p.id_planning, p.date_planning, p.heure_debut, p.heure_fin, p.nb_places_restantes, p.etat as planning_etat,
                   a.id_activite, a.nomA, a.descriptionA, a.lieu, a.prix_par_personne, a.type, a.statut as activite_statut, a.image,
                   u.id as user_id, u.nom, u.prenom, u.e_mail, u.num_tel, u.role, u.status
            FROM reservation r
            JOIN planning p ON r.id_planning = p.id_planning
            JOIN activite a ON p.id_activite = a.id_activite
            JOIN users u ON r.id_user = u.id
            WHERE r.id_planning = ?
            ORDER BY r.id_reservation DESC
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idPlanning);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération réservations par planning: " + e.getMessage());
            e.printStackTrace();
        }
        return reservations;
    }

    @Override
    public List<Reservation> getReservationsByStatut(String statut) {
        List<Reservation> reservations = new ArrayList<>();

        String sql = """
            SELECT r.*, 
                   p.id_planning, p.date_planning, p.heure_debut, p.heure_fin, p.nb_places_restantes, p.etat as planning_etat,
                   a.id_activite, a.nomA, a.descriptionA, a.lieu, a.prix_par_personne, a.type, a.statut as activite_statut, a.image,
                   u.id as user_id, u.nom, u.prenom, u.e_mail, u.num_tel, u.role, u.status
            FROM reservation r
            JOIN planning p ON r.id_planning = p.id_planning
            JOIN activite a ON p.id_activite = a.id_activite
            JOIN users u ON r.id_user = u.id
            WHERE r.statut = ?
            ORDER BY r.id_reservation DESC
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, statut);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération réservations par statut: " + e.getMessage());
            e.printStackTrace();
        }
        return reservations;
    }

    @Override
    public int countReservationsByUser(int idUser) {
        String sql = "SELECT COUNT(*) as total FROM reservation WHERE id_user = ? AND statut = 'CONFIRMEE'";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur comptage réservations: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public boolean deleteReservation(int idReservation) {
        String sql = "DELETE FROM reservation WHERE id_reservation = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            int affected = ps.executeUpdate();

            if (affected > 0) {
                System.out.println("✅ Réservation #" + idReservation + " supprimée définitivement");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression réservation #" + idReservation + ": " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Méthode utilitaire pour mapper un ResultSet vers un objet Reservation
     */
    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        // Créer l'utilisateur
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setE_mail(rs.getString("e_mail"));
        user.setNum_tel(rs.getString("num_tel"));

        // Créer l'activité
        Activite activite = new Activite();
        activite.setIdActivite(rs.getInt("id_activite"));
        activite.setNomA(rs.getString("nomA"));
        activite.setDescriptionA(rs.getString("descriptionA"));
        activite.setLieu(rs.getString("lieu"));
        activite.setPrixParPersonne(rs.getFloat("prix_par_personne"));
        activite.setType(rs.getString("type"));
        activite.setStatut(rs.getString("activite_statut"));
        activite.setImage(rs.getString("image"));

        // Créer le planning
        Planning planning = new Planning();
        planning.setIdPlanning(rs.getInt("id_planning"));
        planning.setIdActivite(rs.getInt("id_activite"));
        planning.setDatePlanning(rs.getDate("date_planning").toLocalDate());
        planning.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
        planning.setHeureFin(rs.getTime("heure_fin").toLocalTime());
        planning.setEtat(rs.getString("planning_etat"));
        planning.setNbPlacesRestantes(rs.getInt("nb_places_restantes"));
        planning.setActivite(activite);

        // Créer la réservation
        Reservation reservation = new Reservation();
        reservation.setIdReservation(rs.getInt("id_reservation"));
        reservation.setUser(user);
        reservation.setPlanning(planning);
        reservation.setStatut(rs.getString("statut"));

        return reservation;
    }
}
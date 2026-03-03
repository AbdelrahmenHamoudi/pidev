package org.example.Services.trajet;

import org.example.Entites.trajet.Trajet;
import org.example.Entites.user.User;
import org.example.Entites.trajet.Voiture;
import org.example.Entites.trajet.StatutVoiture;
import org.example.Services.CRUD;
import org.example.Services.user.UserCRUD;
import org.example.Utils.MyBD;
import org.example.Utils.Query;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TrajetCRUD implements CRUD<Trajet> {

    private final Connection conn;
    private final UserCRUD userCRUD = new UserCRUD();

    // ==================== SQL DE BASE (avec JOIN voiture + user) ====================
    // Adapte les noms de colonnes (prix_km, nb_places, avec_chauffeur, image)
    // si ta table voiture utilise des noms différents.
    private static final String SELECT_BASE =
            "SELECT t.*, " +
                    "u.nom, u.prenom, u.e_mail, u.num_tel, " +
                    "v.id_voiture AS v_id, v.marque, v.modele, v.prix_km, " +
                    "v.nb_places, v.avec_chauffeur, v.image " +
                    "FROM trajet t " +
                    "LEFT JOIN users   u ON t.id_utilisateur = u.id " +
                    "LEFT JOIN voiture v ON t.id_voiture     = v.id_voiture ";

    public TrajetCRUD() {
        conn = MyBD.getInstance().getConnection();
    }

    // ==================== AJOUTER ====================
    @Override
    public void ajouterh(Trajet trajet) throws SQLException {
        String sql = Query.addTrajetQuery;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (trajet.getIdVoiture() != null) {
                ps.setInt(1, trajet.getIdVoiture().getIdVoiture());
            } else {
                ps.setNull(1, Types.INTEGER);
            }

            if (trajet.getIdUser() != null) {
                ps.setInt(2, trajet.getIdUser().getId());
            } else {
                throw new SQLException("L'utilisateur est obligatoire pour créer un trajet");
            }

            ps.setString(3, trajet.getPointDepart());
            ps.setString(4, trajet.getPointArrivee());
            ps.setFloat(5, trajet.getDistanceKm());
            ps.setDate(6, trajet.getDateReservation());
            ps.setString(7, trajet.getStatut().name());
            ps.setInt(8, trajet.getNbPersonnes());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("L'ajout du trajet a échoué, aucune ligne affectée.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    trajet.setIdTrajet(rs.getInt(1));
                }
            }

            System.out.println("✅ Trajet ajouté avec ID: " + trajet.getIdTrajet() +
                    " pour l'utilisateur: " + trajet.getIdUser().getPrenom() + " " + trajet.getIdUser().getNom());
        }
    }

    // ==================== MODIFIER ====================
    @Override
    public void modifierh(Trajet trajet) throws SQLException {
        String sql = Query.updateTrajetQuery;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            if (trajet.getIdVoiture() != null) {
                ps.setInt(1, trajet.getIdVoiture().getIdVoiture());
            } else {
                ps.setNull(1, Types.INTEGER);
            }

            if (trajet.getIdUser() != null) {
                ps.setInt(2, trajet.getIdUser().getId());
            } else {
                throw new SQLException("L'utilisateur est obligatoire pour modifier un trajet");
            }

            ps.setString(3, trajet.getPointDepart());
            ps.setString(4, trajet.getPointArrivee());
            ps.setFloat(5, trajet.getDistanceKm());
            ps.setDate(6, trajet.getDateReservation());
            ps.setString(7, trajet.getStatut().name());
            ps.setInt(8, trajet.getNbPersonnes());
            ps.setInt(9, trajet.getIdTrajet());

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Trajet modifié (ID: " + trajet.getIdTrajet() + ")");
            } else {
                System.out.println("⚠️ Aucun trajet modifié (ID introuvable: " + trajet.getIdTrajet() + ")");
            }
        }
    }

    // ==================== SUPPRIMER ====================
    @Override
    public void supprimerh(Trajet trajet) throws SQLException {
        String sql = Query.deleteTrajetQuery;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trajet.getIdTrajet());

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Trajet supprimé (ID: " + trajet.getIdTrajet() + ")");
            } else {
                System.out.println("⚠️ Aucun trajet supprimé (ID introuvable: " + trajet.getIdTrajet() + ")");
            }
        }
    }

    // ==================== AFFICHER TOUS ====================
    @Override
    public List<Trajet> afficherh() throws SQLException {
        String sql = SELECT_BASE + "ORDER BY t.date_reservation DESC";
        List<Trajet> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToTrajetComplet(rs));
            }
        }

        System.out.println("📋 " + list.size() + " trajet(s) récupéré(s)");
        return list;
    }

    // ==================== MAPPING COMPLET (user + voiture) ====================
    private Trajet mapResultSetToTrajetComplet(ResultSet rs) throws SQLException {
        Trajet t = new Trajet();

        t.setIdTrajet(rs.getInt("id_trajet"));

        // ── Voiture complète ────────────────────────────────────────────────────
        int idVoiture = rs.getInt("id_voiture");
        if (!rs.wasNull()) {
            Voiture v = new Voiture();
            v.setIdVoiture(idVoiture);
            try { v.setMarque(rs.getString("marque")); }             catch (SQLException ignored) {}
            try { v.setModele(rs.getString("modele")); }             catch (SQLException ignored) {}
            try { v.setPrixKm(rs.getFloat("prix_km")); }             catch (SQLException ignored) {}
            try { v.setNb_places(rs.getInt("nb_places")); }          catch (SQLException ignored) {}
            try { v.setAvecChauffeur(rs.getBoolean("avec_chauffeur")); } catch (SQLException ignored) {}
            try { v.setImage(rs.getString("image")); }               catch (SQLException ignored) {}
            t.setIdVoiture(v);
        } else {
            t.setIdVoiture(null);
        }

        // ── Utilisateur ─────────────────────────────────────────────────────────
        int idUtilisateur = rs.getInt("id_utilisateur");
        if (!rs.wasNull()) {
            User u = new User();
            u.setId(idUtilisateur);
            try { u.setNom(rs.getString("nom")); }         catch (SQLException ignored) {}
            try { u.setPrenom(rs.getString("prenom")); }   catch (SQLException ignored) {}
            try { u.setE_mail(rs.getString("e_mail")); }   catch (SQLException ignored) {}
            try { u.setNum_tel(rs.getString("num_tel")); } catch (SQLException ignored) {}
            t.setIdUser(u);
        } else {
            t.setIdUser(null);
        }

        // ── Champs trajet ────────────────────────────────────────────────────────
        t.setPointDepart(rs.getString("point_depart"));
        t.setPointArrivee(rs.getString("point_arrivee"));
        t.setDistanceKm(rs.getFloat("distance_km"));
        t.setDateReservation(rs.getDate("date_reservation"));

        try {
            t.setStatut(StatutVoiture.valueOf(rs.getString("statut")));
        } catch (IllegalArgumentException | NullPointerException e) {
            t.setStatut(StatutVoiture.Disponible);
        }

        t.setNbPersonnes(rs.getInt("nb_personnes"));

        return t;
    }

    // ==================== MAPPING SIMPLE (sans JOIN — usage interne) ====================
    private Trajet mapResultSetToTrajet(ResultSet rs) throws SQLException {
        Trajet t = new Trajet();

        t.setIdTrajet(rs.getInt("id_trajet"));

        int idVoiture = rs.getInt("id_voiture");
        if (!rs.wasNull()) {
            Voiture v = new Voiture();
            v.setIdVoiture(idVoiture);
            t.setIdVoiture(v);
        } else {
            t.setIdVoiture(null);
        }

        int idUtilisateur = rs.getInt("id_utilisateur");
        if (!rs.wasNull()) {
            User u = new User();
            u.setId(idUtilisateur);
            t.setIdUser(u);
        } else {
            t.setIdUser(null);
        }

        t.setPointDepart(rs.getString("point_depart"));
        t.setPointArrivee(rs.getString("point_arrivee"));
        t.setDistanceKm(rs.getFloat("distance_km"));
        t.setDateReservation(rs.getDate("date_reservation"));

        try {
            t.setStatut(StatutVoiture.valueOf(rs.getString("statut")));
        } catch (IllegalArgumentException | NullPointerException e) {
            t.setStatut(StatutVoiture.Disponible);
        }

        t.setNbPersonnes(rs.getInt("nb_personnes"));

        return t;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Récupérer un trajet par ID (avec infos utilisateur + voiture complète)
     */
    public Trajet getTrajetById(int idTrajet) throws SQLException {
        String sql = SELECT_BASE + "WHERE t.id_trajet = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idTrajet);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTrajetComplet(rs);
                }
            }
        }

        return null;
    }

    /**
     * Récupérer les trajets disponibles (sans voiture assignée)
     */
    public List<Trajet> getTrajetsDisponibles() throws SQLException {
        String sql = SELECT_BASE +
                "WHERE t.id_voiture IS NULL " +
                "ORDER BY t.date_reservation ASC";
        List<Trajet> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToTrajetComplet(rs));
            }
        }

        return list;
    }

    /**
     * Récupérer les trajets d'un utilisateur spécifique
     */
    public List<Trajet> getTrajetsByUserId(int userId) throws SQLException {
        String sql = SELECT_BASE +
                "WHERE t.id_utilisateur = ? " +
                "ORDER BY t.date_reservation DESC";
        List<Trajet> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTrajetComplet(rs));
                }
            }
        }

        return list;
    }

    /**
     * Récupérer les trajets avec le nom du conducteur (INNER JOIN = uniquement les trajets avec user)
     */
    public List<Trajet> getTrajetsWithConducteur() throws SQLException {
        String sql =
                "SELECT t.*, " +
                        "u.nom, u.prenom, u.e_mail, u.num_tel, " +
                        "v.id_voiture AS v_id, v.marque, v.modele, v.prix_km, " +
                        "v.nb_places, v.avec_chauffeur, v.image " +
                        "FROM trajet t " +
                        "INNER JOIN users   u ON t.id_utilisateur = u.id " +
                        "LEFT  JOIN voiture v ON t.id_voiture     = v.id_voiture " +
                        "ORDER BY t.date_reservation DESC";

        List<Trajet> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToTrajetComplet(rs));
            }
        }

        System.out.println("📋 getTrajetsWithConducteur : " + list.size() + " trajet(s)");
        return list;
    }

    /**
     * Assigner une voiture à un trajet
     */
    public void assignerVoiture(int idTrajet, int idVoiture) throws SQLException {
        String sql = "UPDATE trajet SET id_voiture = ?, statut = ? WHERE id_trajet = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVoiture);
            ps.setString(2, StatutVoiture.Reserve.name());
            ps.setInt(3, idTrajet);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Voiture " + idVoiture + " assignée au trajet " + idTrajet);
            }
        }
    }

    /**
     * Rechercher des trajets par critères
     */
    public List<Trajet> rechercherTrajets(String pointDepart, String pointArrivee, Date date) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT t.*, " +
                        "u.nom, u.prenom, u.e_mail, u.num_tel, " +
                        "v.id_voiture AS v_id, v.marque, v.modele, v.prix_km, " +
                        "v.nb_places, v.avec_chauffeur, v.image " +
                        "FROM trajet t " +
                        "LEFT JOIN users   u ON t.id_utilisateur = u.id " +
                        "LEFT JOIN voiture v ON t.id_voiture     = v.id_voiture " +
                        "WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (pointDepart != null && !pointDepart.isEmpty()) {
            sql.append(" AND LOWER(t.point_depart) LIKE LOWER(?)");
            params.add("%" + pointDepart + "%");
        }

        if (pointArrivee != null && !pointArrivee.isEmpty()) {
            sql.append(" AND LOWER(t.point_arrivee) LIKE LOWER(?)");
            params.add("%" + pointArrivee + "%");
        }

        if (date != null) {
            sql.append(" AND DATE(t.date_reservation) = ?");
            params.add(date);
        }

        sql.append(" ORDER BY t.date_reservation ASC");

        List<Trajet> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTrajetComplet(rs));
                }
            }
        }

        return list;
    }

    /**
     * Compter le nombre de trajets par utilisateur
     */
    public int countTrajetsByUserId(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM trajet WHERE id_utilisateur = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    /**
     * Récupérer les statistiques d'un utilisateur (nombre de trajets, distance totale)
     */
    public Object[] getUserStats(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) as nb_trajets, SUM(distance_km) as distance_totale " +
                "FROM trajet WHERE id_utilisateur = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Object[]{
                            rs.getInt("nb_trajets"),
                            rs.getFloat("distance_totale")
                    };
                }
            }
        }

        return new Object[]{0, 0.0f};
    }
}
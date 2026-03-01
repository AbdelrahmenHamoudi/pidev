package org.example.services;

import org.example.models.Activite;
import org.example.models.Hebergement;
import org.example.models.Voiture;
import org.example.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ NEW: Replaces OffresStaticData entirely.
 *
 * Now queries re7la_3a9 directly:
 *   - hebergement table  → id_hebergement, titre, type_hebergement, prixParNuit
 *   - activite table     → idActivite, nomA, lieu, prixParPersonne
 *   - voiture table      → id_voiture, marque, modele, prix_KM, disponibilite
 *
 * Also provides price-fetcher methods used by PriceCalculatorService.
 */
 public class Offresservice {

    private static Offresservice instance;

    public static Offresservice getInstance() {
        if (instance == null) instance = new Offresservice();
        return instance;
    }

    // ════════════════════════════════════════════════════
    // HÉBERGEMENT
    // ════════════════════════════════════════════════════

    public List<Hebergement> getAllHebergements() {
        List<Hebergement> list = new ArrayList<>();
        String sql = "SELECT id_hebergement, titre, desc_hebergement, capacite, " +
                "type_hebergement, disponible_heberg, prixParNuit FROM hebergement ORDER BY titre";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(extractHebergement(rs));
        } catch (SQLException e) {
            System.err.println("❌ [OffresService] getAllHebergements : " + e.getMessage());
        }
        return list;
    }

    public Hebergement getHebergementById(int id) {
        String sql = "SELECT id_hebergement, titre, desc_hebergement, capacite, " +
                "type_hebergement, disponible_heberg, prixParNuit FROM hebergement WHERE id_hebergement = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return extractHebergement(rs);
        } catch (SQLException e) {
            System.err.println("❌ [OffresService] getHebergementById #" + id + " : " + e.getMessage());
        }
        return null;
    }

    /** Returns only prixParNuit for fast price calculation. */
    public float getPrixParNuit(int hebergementId) {
        String sql = "SELECT prixParNuit FROM hebergement WHERE id_hebergement = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, hebergementId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getFloat("prixParNuit");
        } catch (SQLException e) {
            System.err.println("❌ [OffresService] getPrixParNuit #" + hebergementId + " : " + e.getMessage());
        }
        return 0f;
    }

    private Hebergement extractHebergement(ResultSet rs) throws SQLException {
        return new Hebergement(
                rs.getInt("id_hebergement"),
                rs.getString("titre"),
                rs.getString("desc_hebergement"),
                rs.getInt("capacite"),
                rs.getString("type_hebergement"),
                rs.getBoolean("disponible_heberg"),
                rs.getFloat("prixParNuit")
        );
    }

    // ════════════════════════════════════════════════════
    // ACTIVITÉ
    // ════════════════════════════════════════════════════

    public List<Activite> getAllActivites() {
        List<Activite> list = new ArrayList<>();
        String sql = "SELECT idActivite, nomA, descriptionA, lieu, prixParPersonne, capaciteMax " +
                "FROM activite ORDER BY nomA";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(extractActivite(rs));
        } catch (SQLException e) {
            System.err.println("❌ [OffresService] getAllActivites : " + e.getMessage());
        }
        return list;
    }

    public Activite getActiviteById(int id) {
        String sql = "SELECT idActivite, nomA, descriptionA, lieu, prixParPersonne, capaciteMax " +
                "FROM activite WHERE idActivite = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return extractActivite(rs);
        } catch (SQLException e) {
            System.err.println("❌ [OffresService] getActiviteById #" + id + " : " + e.getMessage());
        }
        return null;
    }

    /** Returns only prixParPersonne for fast price calculation. */
    public float getPrixParPersonne(int activiteId) {
        String sql = "SELECT prixParPersonne FROM activite WHERE idActivite = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activiteId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getFloat("prixParPersonne");
        } catch (SQLException e) {
            System.err.println("❌ [OffresService] getPrixParPersonne #" + activiteId + " : " + e.getMessage());
        }
        return 0f;
    }

    private Activite extractActivite(ResultSet rs) throws SQLException {
        return new Activite(
                rs.getInt("idActivite"),
                rs.getString("nomA"),
                rs.getString("descriptionA"),
                rs.getString("lieu"),
                rs.getFloat("prixParPersonne"),
                rs.getInt("capaciteMax")
        );
    }

    // ════════════════════════════════════════════════════
    // VOITURE
    // ════════════════════════════════════════════════════

    public List<Voiture> getAllVoitures() {
        List<Voiture> list = new ArrayList<>();
        String sql = "SELECT id_voiture, marque, modele, immatriculation, prix_KM, " +
                "avec_chauffeur, disponibilite, description, nb_places " +
                "FROM voiture WHERE disponibilite = 1 ORDER BY marque";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(extractVoiture(rs));
        } catch (SQLException e) {
            System.err.println("❌ [OffresService] getAllVoitures : " + e.getMessage());
        }
        return list;
    }

    public Voiture getVoitureById(int id) {
        String sql = "SELECT id_voiture, marque, modele, immatriculation, prix_KM, " +
                "avec_chauffeur, disponibilite, description, nb_places " +
                "FROM voiture WHERE id_voiture = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return extractVoiture(rs);
        } catch (SQLException e) {
            System.err.println("❌ [OffresService] getVoitureById #" + id + " : " + e.getMessage());
        }
        return null;
    }

    /** Returns only prix_KM for fast price calculation. */
    public float getPrixKm(int voitureId) {
        String sql = "SELECT prix_KM FROM voiture WHERE id_voiture = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, voitureId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getFloat("prix_KM");
        } catch (SQLException e) {
            System.err.println("❌ [OffresService] getPrixKm #" + voitureId + " : " + e.getMessage());
        }
        return 0f;
    }

    private Voiture extractVoiture(ResultSet rs) throws SQLException {
        return new Voiture(
                rs.getInt("id_voiture"),
                rs.getString("marque"),
                rs.getString("modele"),
                rs.getString("immatriculation"),
                rs.getFloat("prix_KM"),
                rs.getBoolean("avec_chauffeur"),
                rs.getBoolean("disponibilite"),
                rs.getString("description"),
                rs.getInt("nb_places")
        );
    }

    // ════════════════════════════════════════════════════
    // DISPLAY HELPERS — for TableView "details" column
    // ════════════════════════════════════════════════════

    /** Human-readable name of any offer by type+id (used in reservation display) */
    public String getOfferName(org.example.models.TargetType type, int targetId) {
        return switch (type) {
            case HEBERGEMENT -> {
                Hebergement h = getHebergementById(targetId);
                yield h != null ? h.getTitre() : "Hébergement #" + targetId;
            }
            case ACTIVITE -> {
                Activite a = getActiviteById(targetId);
                yield a != null ? a.getNomA() : "Activité #" + targetId;
            }
            case VOITURE -> {
                Voiture v = getVoitureById(targetId);
                yield v != null ? v.getNom() : "Voiture #" + targetId;
            }
        };
    }
}

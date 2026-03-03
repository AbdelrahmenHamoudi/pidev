package org.example.Services.promotion;

import org.example.Entites.activite.Activite;
import org.example.Entites.hebergement.Hebergement;
import org.example.Entites.trajet.Voiture;
import org.example.Entites.promotion.TargetType;
import org.example.Utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
                "type_hebergement, disponible_heberg, prixParNuit, image " +
                "FROM hebergement ORDER BY titre";
        try (Connection conn = MyBD.getInstance().getConnection();
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
                "type_hebergement, disponible_heberg, prixParNuit, image " +
                "FROM hebergement WHERE id_hebergement = ?";
        try (Connection conn = MyBD.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return extractHebergement(rs);
        } catch (SQLException e) {
            System.err.println("❌ [OffresService] getHebergementById #" + id + " : " + e.getMessage());
        }
        return null;
    }

    public float getPrixParNuit(int hebergementId) {
        String sql = "SELECT prixParNuit FROM hebergement WHERE id_hebergement = ?";
        try (Connection conn = MyBD.getInstance().getConnection();
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
                rs.getFloat("prixParNuit"),
                rs.getString("image")  // ✅ Ajout du paramètre image manquant
        );
    }

    // ════════════════════════════════════════════════════
    // ACTIVITÉ
    // ════════════════════════════════════════════════════

    public List<Activite> getAllActivites() {
        List<Activite> list = new ArrayList<>();
        String sql = "SELECT id_activite, nomA, descriptionA, lieu, prix_par_personne, capacite_max, type, statut, image " +
                "FROM activite ORDER BY nomA";
        try (Connection conn = MyBD.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(extractActivite(rs));
        } catch (SQLException e) {
            System.err.println("❌ [OffresService] getAllActivites : " + e.getMessage());
        }
        return list;
    }

    public Activite getActiviteById(int id) {
        String sql = "SELECT id_activite, nomA, descriptionA, lieu, prix_par_personne, capacite_max, type, statut, image " +
                "FROM activite WHERE id_activite = ?";
        try (Connection conn = MyBD.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return extractActivite(rs);
        } catch (SQLException e) {
            System.err.println("❌ [OffresService] getActiviteById #" + id + " : " + e.getMessage());
        }
        return null;
    }

    public float getPrixParPersonne(int activiteId) {
        String sql = "SELECT prix_par_personne FROM activite WHERE id_activite = ?";
        try (Connection conn = MyBD.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activiteId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getFloat("prix_par_personne");
        } catch (SQLException e) {
            System.err.println("❌ [OffresService] getPrixParPersonne #" + activiteId + " : " + e.getMessage());
        }
        return 0f;
    }

    private Activite extractActivite(ResultSet rs) throws SQLException {
        return new Activite(
                rs.getInt("id_activite"),
                rs.getString("nomA"),
                rs.getString("descriptionA"),
                rs.getString("lieu"),
                rs.getFloat("prix_par_personne"),
                rs.getInt("capacite_max"),
                rs.getString("type"),
                rs.getString("statut"),
                rs.getString("image")
        );
    }

    // ════════════════════════════════════════════════════
    // VOITURE
    // ════════════════════════════════════════════════════

    public List<Voiture> getAllVoitures() {
        List<Voiture> list = new ArrayList<>();
        String sql = "SELECT id_voiture, marque, modele, immatriculation, prix_km, " +
                "avec_chauffeur, disponibilite, description, image, nb_places " +
                "FROM voiture WHERE disponibilite = 1 ORDER BY marque";
        try (Connection conn = MyBD.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(extractVoiture(rs));
        } catch (SQLException e) {
            System.err.println("❌ [OffresService] getAllVoitures : " + e.getMessage());
        }
        return list;
    }

    public Voiture getVoitureById(int id) {
        String sql = "SELECT id_voiture, marque, modele, immatriculation, prix_km, " +
                "avec_chauffeur, disponibilite, description, image, nb_places " +
                "FROM voiture WHERE id_voiture = ?";
        try (Connection conn = MyBD.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return extractVoiture(rs);
        } catch (SQLException e) {
            System.err.println("❌ [OffresService] getVoitureById #" + id + " : " + e.getMessage());
        }
        return null;
    }

    public float getPrixKm(int voitureId) {
        String sql = "SELECT prix_km FROM voiture WHERE id_voiture = ?";
        try (Connection conn = MyBD.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, voitureId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getFloat("prix_km");
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
                rs.getFloat("prix_km"),
                rs.getBoolean("avec_chauffeur"),
                rs.getBoolean("disponibilite"),
                rs.getString("description"),
                rs.getString("image"),      // ✅ Ajout du paramètre image
                rs.getInt("nb_places")
        );
    }

    public String getOfferName(TargetType type, int targetId) {
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
                yield v != null ? v.getMarque() + " " + v.getModele() : "Voiture #" + targetId;
            }
        };
    }
}
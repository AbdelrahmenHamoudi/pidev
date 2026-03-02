package org.example.Services.activite;

import org.example.Entites.activite.Planning;
import org.example.Utils.MyBD;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class PlaningActiviteImpl implements Planingactivite {

    private Connection connection;

    public PlaningActiviteImpl() {
        connection = MyBD.getInstance().getConnection();
    }

    @Override
    public List<Planning> getPlanningsByActivite(int id_activite) {
        List<Planning> plannings = new ArrayList<>();
        String query = "SELECT * FROM planning WHERE id_activite = ? ORDER BY date_planning, heure_debut";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id_activite);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    plannings.add(mapResultSetToPlanning(rs));
                }
            }
            System.out.println("✅ " + plannings.size() + " plannings pour activité #" + id_activite);
        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement plannings: " + e.getMessage());
            e.printStackTrace();
        }
        return plannings;
    }

    @Override
    public List<Planning> getAllPlannings() {
        List<Planning> plannings = new ArrayList<>();
        String query = "SELECT * FROM planning ORDER BY date_planning DESC, heure_debut";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                plannings.add(mapResultSetToPlanning(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement tous plannings: " + e.getMessage());
            e.printStackTrace();
        }
        return plannings;
    }

    @Override
    public Planning getPlanningById(int id) {
        String query = "SELECT * FROM planning WHERE id_planning = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPlanning(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération planning #" + id + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean addPlanning(Planning planning) {
        String query = "INSERT INTO planning (id_activite, date_planning, heure_debut, heure_fin, etat, nb_places_restantes) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, planning.getIdActivite());
            pstmt.setDate(2, Date.valueOf(planning.getDatePlanning()));
            pstmt.setTime(3, Time.valueOf(planning.getHeureDebut()));
            pstmt.setTime(4, Time.valueOf(planning.getHeureFin()));
            pstmt.setString(5, planning.getEtat() != null ? planning.getEtat() : "Disponible");
            pstmt.setInt(6, planning.getNbPlacesRestantes());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        planning.setIdPlanning(generatedKeys.getInt(1));
                    }
                }
                System.out.println("✅ Planning ajouté");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur ajout planning: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean updatePlanning(Planning planning) {
        String query = "UPDATE planning SET id_activite = ?, date_planning = ?, heure_debut = ?, " +
                "heure_fin = ?, etat = ?, nb_places_restantes = ? WHERE id_planning = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, planning.getIdActivite());
            pstmt.setDate(2, Date.valueOf(planning.getDatePlanning()));
            pstmt.setTime(3, Time.valueOf(planning.getHeureDebut()));
            pstmt.setTime(4, Time.valueOf(planning.getHeureFin()));
            pstmt.setString(5, planning.getEtat());
            pstmt.setInt(6, planning.getNbPlacesRestantes());
            pstmt.setInt(7, planning.getIdPlanning());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Planning #" + planning.getIdPlanning() + " mis à jour");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur mise à jour planning: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean deletePlanning(int id) {
        String query = "DELETE FROM planning WHERE id_planning = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Planning #" + id + " supprimé");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression planning #" + id + ": " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Planning> getAvailablePlannings(int id_activite) {
        List<Planning> plannings = new ArrayList<>();
        String query = "SELECT * FROM planning WHERE id_activite = ? AND nb_places_restantes > 0 " +
                "AND etat = 'Disponible' ORDER BY date_planning, heure_debut";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id_activite);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    plannings.add(mapResultSetToPlanning(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération plannings disponibles: " + e.getMessage());
            e.printStackTrace();
        }
        return plannings;
    }

    @Override
    public boolean reserverPlace(int idPlanning) {
        String query = "UPDATE planning SET nb_places_restantes = nb_places_restantes - 1 " +
                "WHERE id_planning = ? AND nb_places_restantes > 0";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idPlanning);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Place réservée planning #" + idPlanning);
                updateStatutIfComplet(idPlanning);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur réservation place: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean libererPlace(int idPlanning) {
        String query = "UPDATE planning SET nb_places_restantes = nb_places_restantes + 1 " +
                "WHERE id_planning = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idPlanning);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Place libérée planning #" + idPlanning);
                // Mettre à jour le statut si nécessaire
                String updateStatut = "UPDATE planning SET etat = 'Disponible' WHERE id_planning = ?";
                try (PreparedStatement ps2 = connection.prepareStatement(updateStatut)) {
                    ps2.setInt(1, idPlanning);
                    ps2.executeUpdate();
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur libération place: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public int countPlannings() {
        String query = "SELECT COUNT(*) as total FROM planning";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur comptage plannings: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int countPlanningsByActivite(int id_activite) {
        String query = "SELECT COUNT(*) as total FROM planning WHERE id_activite = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id_activite);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur comptage plannings par activité: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    private void updateStatutIfComplet(int idPlanning) {
        String query = "UPDATE planning SET etat = 'Complet' " +
                "WHERE id_planning = ? AND nb_places_restantes = 0";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idPlanning);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Planning mapResultSetToPlanning(ResultSet rs) throws SQLException {
        return new Planning(
                rs.getInt("id_planning"),
                rs.getInt("id_activite"),
                rs.getDate("date_planning").toLocalDate(),
                rs.getTime("heure_debut").toLocalTime(),
                rs.getTime("heure_fin").toLocalTime(),
                rs.getString("etat"),
                rs.getInt("nb_places_restantes")
        );
    }
}
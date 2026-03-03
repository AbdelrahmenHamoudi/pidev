package org.example.Services.activite;

import org.example.Entites.activite.Activite;
import org.example.Utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActiviteServiceImpl implements ActiviteService {
    private Connection connection;

    public ActiviteServiceImpl() {
        this.connection = MyBD.getInstance().getConnection();
    }

    @Override
    public List<Activite> getAllActivites() {
        List<Activite> activites = new ArrayList<>();
        String query = "SELECT * FROM activite ORDER BY id_activite DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Activite activite = new Activite(
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
                activites.add(activite);
            }
            System.out.println("✅ " + activites.size() + " activités chargées");

        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement activités: " + e.getMessage());
            e.printStackTrace();
        }
        return activites;
    }

    @Override
    public Activite getActiviteById(int id) {
        String query = "SELECT * FROM activite WHERE id_activite = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
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
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération activité #" + id + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean addActivite(Activite activite) {
        String query = "INSERT INTO activite (nomA, descriptionA, lieu, prix_par_personne, " +
                "capacite_max, type, statut, image) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, activite.getNomA());
            pstmt.setString(2, activite.getDescriptionA());
            pstmt.setString(3, activite.getLieu());
            pstmt.setFloat(4, activite.getPrixParPersonne());
            pstmt.setInt(5, activite.getCapaciteMax());
            pstmt.setString(6, activite.getType());
            pstmt.setString(7, activite.getStatut());
            pstmt.setString(8, activite.getImage());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        activite.setIdActivite(generatedKeys.getInt(1));
                    }
                }
                System.out.println("✅ Activité ajoutée : " + activite.getNomA());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur ajout activité: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean updateActivite(Activite activite) {
        String query = "UPDATE activite SET nomA = ?, descriptionA = ?, lieu = ?, " +
                "prix_par_personne = ?, capacite_max = ?, type = ?, statut = ?, image = ? " +
                "WHERE id_activite = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, activite.getNomA());
            pstmt.setString(2, activite.getDescriptionA());
            pstmt.setString(3, activite.getLieu());
            pstmt.setFloat(4, activite.getPrixParPersonne());
            pstmt.setInt(5, activite.getCapaciteMax());
            pstmt.setString(6, activite.getType());
            pstmt.setString(7, activite.getStatut());
            pstmt.setString(8, activite.getImage());
            pstmt.setInt(9, activite.getIdActivite());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Activité mise à jour : " + activite.getNomA());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur mise à jour activité: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean deleteActivite(int id) {
        String query = "DELETE FROM activite WHERE id_activite = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Activité #" + id + " supprimée");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression activité #" + id + ": " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Activite> searchActivites(String keyword) {
        List<Activite> activites = new ArrayList<>();
        String query = "SELECT * FROM activite WHERE nomA LIKE ? OR lieu LIKE ? OR descriptionA LIKE ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Activite activite = new Activite(
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
                    activites.add(activite);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur recherche activités: " + e.getMessage());
            e.printStackTrace();
        }
        return activites;
    }

    @Override
    public List<Activite> getActivitesByType(String type) {
        List<Activite> activites = new ArrayList<>();
        String query = "SELECT * FROM activite WHERE type = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, type);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Activite activite = new Activite(
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
                    activites.add(activite);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération activités par type: " + e.getMessage());
            e.printStackTrace();
        }
        return activites;
    }


    public List<Activite> getActivitesByLieu(String lieu) {
        List<Activite> activites = new ArrayList<>();
        String query = "SELECT * FROM activite WHERE lieu = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, lieu);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    activites.add(mapResultSetToActivite(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération activités par lieu: " + e.getMessage());
            e.printStackTrace();
        }
        return activites;
    }


    public List<Activite> getActivitesByStatut(String statut) {
        List<Activite> activites = new ArrayList<>();
        String query = "SELECT * FROM activite WHERE statut = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, statut);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    activites.add(mapResultSetToActivite(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération activités par statut: " + e.getMessage());
            e.printStackTrace();
        }
        return activites;
    }

    @Override
    public int countActivites() {
        String query = "SELECT COUNT(*) as total FROM activite";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur comptage activités: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    private Activite mapResultSetToActivite(ResultSet rs) throws SQLException {
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
}
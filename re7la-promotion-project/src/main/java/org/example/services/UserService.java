package org.example.services;

import org.example.models.User;
import org.example.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserService {

    /**
     * Récupérer tous les users
     */
    public List<User> getAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des users");
            e.printStackTrace();
        }

        return users;
    }

    /**
     * Récupérer un user par ID
     */
    public Optional<User> getById(int id) {
        String sql = "SELECT * FROM user WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(extractUserFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du user #" + id);
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Récupérer un user par email
     */
    public Optional<User> getByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(extractUserFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du user par email");
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Ajouter un nouveau user
     */
    public User add(User user) {
        String sql = "INSERT INTO user (nom, prenom, email, password) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getNom());
            pstmt.setString(2, user.getPrenom());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getPassword());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                }
            }

            System.out.println("✅ User ajouté: " + user.getFullName());
            return user;

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du user");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Mettre à jour un user
     */
    public boolean update(User user) {
        String sql = "UPDATE user SET nom = ?, prenom = ?, email = ?, password = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getNom());
            pstmt.setString(2, user.getPrenom());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getPassword());
            pstmt.setInt(5, user.getId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✅ User modifié: " + user.getFullName());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification du user");
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Supprimer un user
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM user WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✅ User supprimé (ID: " + id + ")");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du user");
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Extraire un User depuis un ResultSet
     */
    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String nom = rs.getString("nom");
        String prenom = rs.getString("prenom");
        String email = rs.getString("email");
        String password = rs.getString("password");
        Timestamp createdAt = rs.getTimestamp("created_at");

        return new User(id, nom, prenom, email, password, createdAt);
    }
}

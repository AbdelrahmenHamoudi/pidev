package org.example.Services.user;

import org.example.Controllers.user.customUserException;
import org.example.Entites.user.User;
import org.example.Entites.user.Role;
import org.example.Entites.user.Status;
import org.example.Utils.MyBD;
import org.example.Utils.Query;

import java.sql.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;




public class UserCRUD implements CRUDuser<User> {

    private Connection conn;

    public UserCRUD() {
        conn = MyBD.getInstance().getConnection();
    }

    @Override
    public void createUser(User user) throws SQLException {

        if (getUserByEmail(user.getE_mail()) != null) {
            throw new customUserException("Cet email est déjà utilisé !");
        }


        if (user.getRole() == null) user.setRole(Role.user);
        if (user.getStatus() == null) user.setStatus(Status.Unbanned);

        String req = Query.addUserQuery;
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getDate_naiss());
            ps.setString(4, user.getE_mail());
            ps.setString(5, user.getNum_tel());
            ps.setString(6, hashPassword(user.getMot_de_pass()));
            ps.setString(7, user.getImage());
            ps.setString(8, user.getRole().name());
            ps.setString(9, user.getStatus().name());

            ps.executeUpdate();
            System.out.println("Utilisateur ajouté !");
        }
    }

    @Override
    public void updateUser(User user) throws SQLException {
        String req = Query.updateUserQuery;
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setString(1, user.getNom());
        ps.setString(2, user.getPrenom());
        ps.setString(3, user.getDate_naiss());
        ps.setString(4, user.getE_mail());
        ps.setString(5, user.getNum_tel());
        ps.setString(6, user.getRole().name()); // Role
        ps.setString(7, user.getStatus().name()); // Status
        ps.setInt(8, user.getId());

        ps.executeUpdate();
        System.out.println("Utilisateur modifié !");
    }

    @Override
    public void deleteUser(User user) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(Query.deleteUserQuery);
        ps.setInt(1, user.getId());
        ps.executeUpdate();
        System.out.println("Utilisateur supprimé !");
    }

    @Override
    public void updateImageUser(User user) throws SQLException {
        String req = Query.updateImageQuery ;
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setString(1, user.getImage());
            ps.setInt(2, user.getId());
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Image utilisateur mise à jour pour l'ID: " + user.getId());
            } else {
                System.out.println("⚠️ Aucune mise à jour - utilisateur non trouvé");
            }
        }
    }

    @Override
    public void updatePassword(User user) throws SQLException {
        String req = Query.updatePasswordQuery;
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setString(1, hashPassword(user.getMot_de_pass())); // hash du mot de passe
        ps.setInt(2, user.getId());
        ps.executeUpdate();
        System.out.println("Mot de passe utilisateur mis à jour !");
    }

    public User getUserById(int id) {
        String query = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération de l'utilisateur par ID: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<User> getUserByName(String name) throws SQLException {
        String req = Query.getUserByName;
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setString(1, "%" + name + "%");
        ResultSet rs = ps.executeQuery();

        List<User> users = new ArrayList<>();
        while (rs.next()) {
            users.add(mapResultSetToUser(rs));
        }
        return users;
    }

    @Override
    public List<User> ShowUsers() throws SQLException {
        String req = Query.showUsers;
        PreparedStatement ps = conn.prepareStatement(req);
        ResultSet rs = ps.executeQuery();

        List<User> users = new ArrayList<>();
        while (rs.next()) {
            users.add(mapResultSetToUser(rs));
        }
        return users;
    }

    public User getUserByEmail(String email) {
        String query = Query.getUserByEmailQuery;
        User user = null;

        try (PreparedStatement pst = conn.prepareStatement(query)) {

            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                user = mapResultSetToUser(rs); // 🔥 utilisation du mapper
            }

        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération de l'utilisateur par email : " + e.getMessage());
        }

        return user;
    }



    @Override
    public User signIn(User user) throws SQLException {

        String req = Query.signIn;
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setString(1, user.getE_mail());
        ps.setString(2, hashPassword(user.getMot_de_pass()));

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
           // token algo impl
            return mapResultSetToUser(rs); // 🔥 retourne le vrai user
        } else {
            throw new customUserException("Email ou mot de passe incorrect !");
        }
    }


    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setDate_naiss(rs.getString("date_naiss"));
        u.setE_mail(rs.getString("e_mail"));
        u.setNum_tel(rs.getString("num_tel"));
        u.setMot_de_pass(rs.getString("mot_de_pass"));
        u.setImage(rs.getString("image"));
        u.setRole(Role.valueOf(rs.getString("role")));  // correspond exactement à l'enum
        u.setStatus(Status.valueOf(rs.getString("status")));   // si enum Status = Banned, Unbanned
        return u;
    }

    private static String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashed = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void markEmailAsVerified(int userId) throws SQLException {
        String sql = "UPDATE users SET email_verified = true WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            System.out.println("✅ Email marqué comme vérifié pour l'utilisateur ID: " );
        }
    }
}


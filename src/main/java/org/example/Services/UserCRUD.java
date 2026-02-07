package org.example.Services;

import org.example.Entites.User;
import org.example.Entites.Role;
import org.example.Utils.Query;
import java.sql.SQLException;
import java.util.List;
import java.sql.*;
import java.util.ArrayList;


public class UserCRUD implements CRUDuser<User> {
    Connection conn;
    @Override
    public void createUser(User user) throws SQLException {
        String req = Query.addUserQuery;
        PreparedStatement ps = conn.prepareStatement(req);

        ps.setString(1, user.getNom());
        ps.setString(2, user.getPrenom());
        ps.setString(3, user.getDate_naiss());
        ps.setString(4, user.getE_mail());
        ps.setString(5, user.getNum_tel());
        ps.setString(6, user.getMot_de_pass());
        ps.setString(7, user.getImage());
        ps.setString(8, user.getRole().name().toLowerCase());
        ps.setString(9, user.getStatus());

        ps.executeUpdate();
        System.out.println("Utilisateur ajouté !");
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
        ps.setString(6, user.getRole().name().toLowerCase());
        ps.setString(7, user.getStatus());
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
        String req = Query.updateImageQuery;
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setString(1, user.getImage());
        ps.setInt(2, user.getId());
        ps.executeUpdate();
        System.out.println("Image utilisateur mise à jour !");
    }

    @Override
    public void updatePassword(User user) throws SQLException {
        String req = Query.updatePasswordQuery;
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setString(1, user.getMot_de_pass());
        ps.setInt(2, user.getId());
        ps.executeUpdate();
        System.out.println("Mot de passe utilisateur mis à jour !");
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


    @Override
    public void signIn(User user) throws SQLException {
        String req = Query.signIn;
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setString(1, user.getE_mail());
        ps.setString(2, user.getMot_de_pass());
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            System.out.println("Connexion réussie pour : " + rs.getString("nom"));
        } else {
            System.out.println("Email ou mot de passe incorrect !");
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
        u.setRole(Role.valueOf(rs.getString("role").toUpperCase()));
        u.setStatus(rs.getString("status"));
        return u;
    }


}


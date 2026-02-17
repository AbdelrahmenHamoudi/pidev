package org.example.Services;

import org.example.Entites.Commentaire;
import org.example.Entites.Publication;
import org.example.Entites.User;
import org.example.Utils.MyBD;
import org.example.Utils.Query;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentaireCRUD implements CRUD<Commentaire> {
    private Connection con;

    public CommentaireCRUD() {
        con = MyBD.getInstance().getConnection();
    }
    @Override
    public void ajouterh(Commentaire c) throws SQLException {
        String req = Query.addCommentaireQuery;
        PreparedStatement ps = con.prepareStatement(req);
        ps.setInt(1, c.getIdCommentaire());
        ps.setInt(2, c.getId_utilisateur().getId()); // FK user
        ps.setInt(3, c.getPublication().getIdPublication()); // FK Publication
        ps.setString(4, c.getContenuC());
        ps.setString(5, c.getDateCreationC());
        ps.setBoolean(6, c.isStatutC());

        ps.executeUpdate();
        System.out.println(" Commentaire ajoutée !");
    }

    @Override
    public void modifierh(Commentaire c) throws SQLException {
        String req = Query.updateCommentaireQuery;
        PreparedStatement ps = con.prepareStatement(req);

        ps.setInt(1, c.getId_utilisateur().getId());
        ps.setInt(2, c.getPublication().getIdPublication());
        ps.setString(3, c.getContenuC());
        ps.setString(4, c.getDateCreationC());
        ps.setBoolean(5, c.isStatutC());
        ps.setInt(6, c.getIdCommentaire());

        ps.executeUpdate();
        System.out.println("✏ Commentaire modifiée !");
    }

    @Override
    public void supprimerh(Commentaire c) throws SQLException {
        String req = Query.deleteCommentaireQuery;
        PreparedStatement ps = con.prepareStatement(req);
        ps.setInt(1, c.getIdCommentaire());

        ps.executeUpdate();
        System.out.println("🗑 Commentaire supprimée !");
    }

    @Override
    public List<Commentaire> afficherh() throws SQLException {
        List<Commentaire> list = new ArrayList<>();
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(Query.showCommentaireQuery);

        while (rs.next()) {

            // Création objets liés (minimum)
            User u = new User();
            u.setId(rs.getInt("id_utilisateur"));

            Publication p = new Publication();
            p.setIdPublication(rs.getInt("idPublication"));

            Commentaire c = new Commentaire(
                    rs.getInt("idCommentaire"),
                    u,
                    p,
                    rs.getString("contenuC"),
                    rs.getString("dateCreationC"),
                    rs.getBoolean("statutC")
            );

            list.add(c);
        }

        return list;
    }
    public List<Commentaire> afficherParPublication(int idPublication) throws SQLException {
        List<Commentaire> list = new ArrayList<>();
        String sql = "SELECT * FROM commentaire WHERE idPublication = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, idPublication);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            // Création objets liés (minimum)
            User u = new User();
            u.setId(rs.getInt("id_utilisateur"));

            Publication p = new Publication();
            p.setIdPublication(rs.getInt("idPublication"));

            Commentaire c = new Commentaire(
                    rs.getInt("idCommentaire"),
                    u,
                    p,
                    rs.getString("contenuC"),
                    rs.getString("dateCreationC"),
                    rs.getBoolean("statutC")
            );

            list.add(c);
        }

        return list;
    }

}

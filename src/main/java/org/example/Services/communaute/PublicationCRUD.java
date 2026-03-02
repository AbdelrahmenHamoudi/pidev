package org.example.Services.communaute;

import org.example.Entites.communaute.Publication;
import org.example.Entites.communaute.StatutP;
import org.example.Entites.communaute.TypeCible;
import org.example.Services.CRUD;
import org.example.Utils.MyBD;
import org.example.Utils.Query;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PublicationCRUD implements CRUD<Publication> {
    private Connection con;

    public PublicationCRUD() {
        con = MyBD.getInstance().getConnection();
    }

    @Override
    public void ajouterh(Publication p) throws SQLException {

        String sql =  Query.addPublicationQuery;
        PreparedStatement ps = con.prepareStatement(sql);

        ps.setInt(1, p.getId_utilisateur().getId()); // FK user
        ps.setString(2, p.getImgURL());
        ps.setString(3, p.getTypeCible().toString());
        ps.setString(4, p.getDateCreation());
        ps.setString(5, p.getDateModif());
        ps.setString(6, p.getStatutP().toString());
        ps.setBoolean(7, p.isEstVerifie());
        ps.setString(8, p.getDescriptionP());

        ps.executeUpdate();
        System.out.println("✅ Publication ajoutée !");
    }


    @Override
    public void modifierh(Publication p) throws SQLException {

        String sql = Query.updatePublicationQuery;
        PreparedStatement ps = con.prepareStatement(sql);

        ps.setString(1, p.getImgURL());
        ps.setString(2, p.getTypeCible().toString());
        ps.setString(3, p.getDateCreation());
        ps.setString(4, p.getDateModif());
        ps.setString(5, p.getStatutP().toString());
        ps.setBoolean(6, p.isEstVerifie());
        ps.setString(7, p.getDescriptionP());
        ps.setInt(8, p.getIdPublication());

        ps.executeUpdate();
        System.out.println("✏ Publication modifiée !");
    }

    @Override
    public void supprimerh(Publication p) throws SQLException {

        String sql = Query.deletePublicationQuery;
        PreparedStatement ps = con.prepareStatement(sql);

        ps.setInt(1, p.getIdPublication());
        ps.executeUpdate();

        System.out.println("🗑 Publication supprimée !");
    }

    @Override
    public List<Publication> afficherh() throws SQLException {
        List<Publication> list = new ArrayList<>();
        String sql = Query.showPublicationQuery;

        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Publication p = new Publication(
                    rs.getInt("idPublication"),
                    rs.getString("ImgURL"),
                    TypeCible.valueOf(rs.getString("typeCible")),
                    rs.getString("dateCreation"),
                    rs.getString("dateModif"),
                    StatutP.valueOf(rs.getString("statutP")),
                    rs.getBoolean("estVerifie"),
                    rs.getString("DescriptionP")
            );



            list.add(p);
        }

        return list;
    }
        //return List.of();
}

package org.example.Services.hebergement;

import org.example.Entites.hebergement.Hebergement;
import org.example.Services.CRUD;
import org.example.Utils.MyBD;
import org.example.Utils.Query;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HebergementCRUD implements CRUD<Hebergement> {

    private Connection con;

    public HebergementCRUD() {
        con = MyBD.getInstance().getConnection();
    }

    @Override
    public void ajouterh(Hebergement h) throws SQLException {

        String sql = Query.addhebergementQuery;
        PreparedStatement ps = con.prepareStatement(sql);


        ps.setString(1, h.getTitre());
        ps.setString(2, h.getDesc_hebergement());
        ps.setInt(3, h.getCapacite());
        ps.setString(4, h.getType_hebergement());
        ps.setBoolean(5, h.isDisponible_heberg());
        ps.setFloat(6, h.getPrixParNuit());
        ps.setString(7, h.getImage());


        ps.executeUpdate();
        System.out.println("✅ Hebergement ajouté !");
    }

    @Override
    public void modifierh(Hebergement h) throws SQLException {

        String sql = Query.updatehebergementQuery;
        PreparedStatement ps = con.prepareStatement(sql);

        ps.setString(1, h.getTitre());
        ps.setString(2, h.getDesc_hebergement());
        ps.setInt(3, h.getCapacite());
        ps.setString(4, h.getType_hebergement());
        ps.setBoolean(5, h.isDisponible_heberg());
        ps.setFloat(6, h.getPrixParNuit());
        ps.setString(7, h.getImage());
        ps.setInt(8, h.getId_hebergement());

        ps.executeUpdate();
        System.out.println("✏ Hebergement modifié !");
    }

    @Override
    public void supprimerh(Hebergement h) throws SQLException {

        String sql = Query.deletehebergementQuery;
        PreparedStatement ps = con.prepareStatement(sql);

        ps.setInt(1, h.getId_hebergement());
        ps.executeUpdate();

        System.out.println("🗑 Hebergement supprimé !");
    }

    @Override
    public List<Hebergement> afficherh() throws SQLException {

        List<Hebergement> list = new ArrayList<>();
        String sql = Query.showhebergementQuery;

        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Hebergement h = new Hebergement(
                    rs.getInt("id_hebergement"),
                    rs.getString("titre"),
                    rs.getString("desc_hebergement"),
                    rs.getInt("capacite"),
                    rs.getString("type_hebergement"),
                    rs.getBoolean("disponible_heberg"),
                    rs.getFloat("prixParNuit"),
                    rs.getString("image")
            );

            list.add(h);
        }

        return list;
    }
}

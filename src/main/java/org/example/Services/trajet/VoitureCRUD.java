package org.example.Services.trajet;

import org.example.Entites.trajet.Voiture;
import org.example.Services.CRUD;
import org.example.Utils.MyBD;
import org.example.Utils.Query;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VoitureCRUD implements CRUD<Voiture> {

    Connection conn;
    public VoitureCRUD()
    {
        conn= MyBD.getInstance().getConnection();
    }

    @Override
    public void ajouterh(Voiture voiture) throws SQLException {


        PreparedStatement ps = conn.prepareStatement(
                Query.addVoitureQuery,
                Statement.RETURN_GENERATED_KEYS
        );

        ps.setString(1, voiture.getMarque());
        ps.setString(2, voiture.getModele());
        ps.setString(3, voiture.getImmatriculation());
        ps.setFloat(4, voiture.getPrixKm());
        ps.setBoolean(5, voiture.isAvecChauffeur());
        ps.setBoolean(6, voiture.isDisponibilite());
        ps.setString(7, voiture.getDescription());
        ps.setString(8, voiture.getImage());
        ps.setInt(9, voiture.getNb_places());

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            voiture.setIdVoiture(rs.getInt(1));
        }

        System.out.println("Voiture ajoutée ✅");
    }

    @Override
    public void modifierh(Voiture voiture) throws SQLException {

        PreparedStatement ps = conn.prepareStatement(Query.updateVoitureQuery);

        ps.setString(1, voiture.getMarque());
        ps.setString(2, voiture.getModele());
        ps.setString(3, voiture.getImmatriculation());
        ps.setFloat(4, voiture.getPrixKm());
        ps.setBoolean(5, voiture.isAvecChauffeur());
        ps.setBoolean(6, voiture.isDisponibilite());
        ps.setString(7, voiture.getDescription());
        ps.setString(8, voiture.getImage());
        ps.setInt(9, voiture.getNb_places());
        ps.setInt(10, voiture.getIdVoiture());

        ps.executeUpdate();

        System.out.println("Voiture modifiée ✅");
    }

    @Override
    public void supprimerh(Voiture voiture) throws SQLException {

        PreparedStatement ps = conn.prepareStatement(Query.deleteVoitureQuery);
        ps.setInt(1, voiture.getIdVoiture());
        ps.executeUpdate();

        System.out.println("Voiture supprimée ✅");
    }

    @Override
    public List<Voiture> afficherh() throws SQLException {
        PreparedStatement ps = conn.prepareStatement(Query.showVoiture);
        ResultSet rs = ps.executeQuery();

        List<Voiture> list = new ArrayList<>();

        while (rs.next()) {
            list.add(mapResultSetToVoiture(rs));
        }

        return list;

    }
    private Voiture mapResultSetToVoiture(ResultSet rs) throws SQLException {

        Voiture v = new Voiture();

        v.setIdVoiture(rs.getInt("id_Voiture"));
        v.setMarque(rs.getString("marque"));
        v.setModele(rs.getString("modele"));
        v.setImmatriculation(rs.getString("immatriculation"));
        v.setPrixKm(rs.getFloat("prix_KM"));
        v.setAvecChauffeur(rs.getBoolean("avec_chauffeur"));
        v.setDisponibilite(rs.getBoolean("disponibilite"));
        v.setDescription(rs.getString("description"));
        v.setImage(rs.getString("image"));
        v.setNb_places(rs.getInt("nb_places"));

        return v;
    }
}

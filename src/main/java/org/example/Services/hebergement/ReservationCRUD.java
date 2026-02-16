package org.example.Services.hebergement;

import org.example.Entites.hebergement.Hebergement;
import org.example.Entites.hebergement.Reservation;
import org.example.Services.CRUD;
import org.example.Utils.MyBD;
import org.example.Utils.Query;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationCRUD implements CRUD<Reservation> {

    private final Connection con;

    public ReservationCRUD() {
        con = MyBD.getInstance().getConnection();
    }

    @Override
    public void ajouterh(Reservation r) throws SQLException {
        String req = Query.addReservationQuery;
        PreparedStatement ps = con.prepareStatement(req);

        ps.setInt(1, r.getHebergement().getId_hebergement()); // ID de l'hébergement
        ps.setString(2, r.getDateDebutR());
        ps.setString(3, r.getDateFinR());
        ps.setString(4, r.getNomR());
        ps.setString(5, r.getPrenomR());
        ps.setInt(6, r.getTelR());
        ps.setString(7, r.getStatutR());

        ps.executeUpdate();
        System.out.println("✅ Réservation ajoutée !");
    }

    @Override
    public void modifierh(Reservation r) throws SQLException {
        String req = Query.updateReservationQuery;
        PreparedStatement ps = con.prepareStatement(req);

        ps.setInt(1, r.getHebergement().getId_hebergement());
        ps.setString(2, r.getDateDebutR());
        ps.setString(3, r.getDateFinR());
        ps.setString(4, r.getNomR());
        ps.setString(5, r.getPrenomR());
        ps.setInt(6, r.getTelR());
        ps.setString(7, r.getStatutR());
        ps.setInt(8, r.getId_reservation());

        ps.executeUpdate();
        System.out.println("✏️ Réservation modifiée !");
    }

    @Override
    public void supprimerh(Reservation r) throws SQLException {
        String req = Query.deleteReservationQuery;
        PreparedStatement ps = con.prepareStatement(req);
        ps.setInt(1, r.getId_reservation());
        ps.executeUpdate();
        System.out.println("🗑️ Réservation supprimée !");
    }

    @Override
    public List<Reservation> afficherh() throws SQLException {
        List<Reservation> list = new ArrayList<>();

        // Requête avec jointure pour récupérer toutes les informations
        String req = Query.showReservationQuery;

        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            // 1. Créer l'objet Hebergement avec toutes ses informations
            Hebergement hebergement = new Hebergement();
            hebergement.setId_hebergement(rs.getInt("id_hebergement"));
            hebergement.setTitre(rs.getString("titre"));
            hebergement.setDesc_hebergement(rs.getString("desc_hebergement"));
            hebergement.setCapacite(rs.getInt("capacite"));
            hebergement.setType_hebergement(rs.getString("type_hebergement"));
            hebergement.setDisponible_heberg(rs.getBoolean("disponible_heberg"));
            hebergement.setPrixParNuit(rs.getFloat("prixParNuit"));
            hebergement.setImage(rs.getString("image"));

            // 2. Créer l'objet Reservation avec l'hébergement
            Reservation reservation = new Reservation(
                    rs.getInt("id_reservation"),
                    hebergement,
                    rs.getString("dateDebutR"),
                    rs.getString("dateFinR"),
                    rs.getString("nomR"),
                    rs.getString("prenomR"),
                    rs.getInt("telR"),
                    rs.getString("statutR")
            );

            list.add(reservation);
        }

        return list;
    }

    // Méthode supplémentaire pour rechercher par hébergement
    public List<Reservation> rechercherParHebergement(int hebergementId) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String req = "SELECT r.*, h.* FROM reservation r " +
                "INNER JOIN hebergement h ON r.hebergement_id = h.id_hebergement " +
                "WHERE r.hebergement_id = ?";

        PreparedStatement ps = con.prepareStatement(req);
        ps.setInt(1, hebergementId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Hebergement hebergement = new Hebergement();
            hebergement.setId_hebergement(rs.getInt("id_hebergement"));
            hebergement.setTitre(rs.getString("titre"));
            // ... autres setters

            Reservation reservation = new Reservation(
                    rs.getInt("id_reservation"),
                    hebergement,
                    rs.getString("dateDebutR"),
                    rs.getString("dateFinR"),
                    rs.getString("nomR"),
                    rs.getString("prenomR"),
                    rs.getInt("telR"),
                    rs.getString("statutR")
            );

            list.add(reservation);
        }

        return list;
    }
}
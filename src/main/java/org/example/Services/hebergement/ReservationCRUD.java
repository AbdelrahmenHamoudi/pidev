package org.example.Services.hebergement;


import org.example.Entites.hebergement.Hebergement;
import org.example.Entites.hebergement.Reservation;
import org.example.Entites.user.User;
import org.example.Services.CRUD;
import org.example.Utils.MyBD;
import org.example.Utils.Query;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationCRUD implements CRUD<Reservation> {

    private Connection con;

    public ReservationCRUD() {
        con = MyBD.getInstance().getConnection();
    }

    @Override
    public void ajouterh(Reservation r) throws SQLException {

        String req = Query.addReservationQuery;
        PreparedStatement ps = con.prepareStatement(req);
        ps.setInt(1, r.getId_reservation());
        ps.setInt(2, r.getUser().getId()); // FK user
        ps.setInt(3, r.getHebergement().getId_hebergement()); // FK hebergement
        ps.setString(4, r.getDateDebutR());
        ps.setString(5, r.getDateFinR());
        ps.setFloat(6, r.getPrixTotalR());
        ps.setFloat(7, r.getPrixKM());
        ps.setString(8, r.getStatutR());

        ps.executeUpdate();
        System.out.println(" Réservation ajoutée !");
    }

    @Override
    public void modifierh(Reservation r) throws SQLException {

        String req = Query.updateReservationQuery;
        PreparedStatement ps = con.prepareStatement(req);

        ps.setInt(1, r.getUser().getId());
        ps.setInt(2, r.getHebergement().getId_hebergement());
        ps.setString(3, r.getDateDebutR());
        ps.setString(4, r.getDateFinR());
        ps.setFloat(5, r.getPrixTotalR());
        ps.setFloat(6, r.getPrixKM());
        ps.setString(7, r.getStatutR());
        ps.setInt(8, r.getId_reservation());

        ps.executeUpdate();
        System.out.println("✏ Réservation modifiée !");
    }

    @Override
    public void supprimerh(Reservation r) throws SQLException {

        String req = Query.deleteReservationQuery;
        PreparedStatement ps = con.prepareStatement(req);
        ps.setInt(1, r.getId_reservation());

        ps.executeUpdate();
        System.out.println("🗑 Réservation supprimée !");
    }

    @Override
    public List<Reservation> afficherh() throws SQLException {

        List<Reservation> list = new ArrayList<>();
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(Query.showReservationQuery);

        while (rs.next()) {

            // Création objets liés (minimum)
            User u = new User();
            u.setId(rs.getInt("id_user"));

            Hebergement h = new Hebergement();
            h.setId_hebergement(rs.getInt("id_hebergement"));

            Reservation r = new Reservation(
                    rs.getInt("id_reservation"),
                    h,
                    u,
                    rs.getString("dateDebutR"),
                    rs.getString("dateFinR"),
                    rs.getFloat("prixTotalR"),
                    rs.getFloat("prixKM"),
                    rs.getString("statutR")
            );

            list.add(r);
        }

        return list;
    }
}

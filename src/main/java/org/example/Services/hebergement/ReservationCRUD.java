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

        ps.setInt(1, r.getHebergement().getId_hebergement());
        ps.setInt(2, r.getUser().getId());
        ps.setString(3, r.getDateDebutR());
        ps.setString(4, r.getDateFinR());
        ps.setString(5, r.getStatutR());

        ps.executeUpdate();
        System.out.println("✅ Réservation ajoutée !");
    }

    @Override
    public void modifierh(Reservation r) throws SQLException {
        String req = Query.updateReservationQuery;
        PreparedStatement ps = con.prepareStatement(req);

        ps.setInt(1, r.getHebergement().getId_hebergement());
        ps.setInt(2, r.getUser().getId());
        ps.setString(3, r.getDateDebutR());
        ps.setString(4, r.getDateFinR());
        ps.setString(5, r.getStatutR());
        ps.setInt(6, r.getId_reservation());

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
        String req = "SELECT " +
                "r.id_reservation, " +
                "r.dateDebutR, " +
                "r.dateFinR, " +
                "r.statutR, " +
                "h.id_hebergement, " +
                "h.titre, " +
                "h.desc_hebergement, " +
                "h.capacite, " +
                "h.type_hebergement, " +
                "h.disponible_heberg, " +
                "h.prixParNuit, " +
                "h.image, " +
                "u.id as id_user, " +
                "u.nom, " +
                "u.prenom, " +
                "u.e_mail as email, " +
                "u.num_tel as tel " +
                "FROM reservation r " +
                "INNER JOIN hebergement h ON r.hebergement_id = h.id_hebergement " +
                "INNER JOIN users u ON r.user_id = u.id";

        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            User u = new User();
            u.setId(rs.getInt("id_user"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setE_mail(rs.getString("email"));
            u.setNum_tel(rs.getString("tel"));

            Hebergement h = new Hebergement();
            h.setId_hebergement(rs.getInt("h.id_hebergement"));
            h.setTitre(rs.getString("h.titre"));
            h.setDesc_hebergement(rs.getString("h.desc_hebergement"));
            h.setCapacite(rs.getInt("h.capacite"));
            h.setType_hebergement(rs.getString("h.type_hebergement"));
            h.setDisponible_heberg(rs.getBoolean("h.disponible_heberg"));
            h.setPrixParNuit(rs.getFloat("h.prixParNuit"));
            h.setImage(rs.getString("h.image"));

            Reservation r = new Reservation(
                    rs.getInt("r.id_reservation"),
                    h,
                    u,
                    rs.getString("r.dateDebutR"),
                    rs.getString("r.dateFinR"),
                    rs.getString("r.statutR")
            );

            list.add(r);
        }

        return list;
    }

    public List<Reservation> getReservationsByUser(int userId) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String req = "SELECT " +
                "r.id_reservation, " +
                "r.dateDebutR, " +
                "r.dateFinR, " +
                "r.statutR, " +
                "h.id_hebergement, " +
                "h.titre, " +
                "h.desc_hebergement, " +
                "h.capacite, " +
                "h.type_hebergement, " +
                "h.disponible_heberg, " +
                "h.prixParNuit, " +
                "h.image, " +
                "u.id as id_user, " +
                "u.nom, " +
                "u.prenom, " +
                "u.e_mail as email, " +
                "u.num_tel as tel " +
                "FROM reservation r " +
                "INNER JOIN hebergement h ON r.hebergement_id = h.id_hebergement " +
                "INNER JOIN users u ON r.user_id = u.id " +
                "WHERE r.user_id = ?";

        PreparedStatement ps = con.prepareStatement(req);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            User u = new User();
            u.setId(rs.getInt("id_user"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setE_mail(rs.getString("email"));
            u.setNum_tel(rs.getString("tel"));

            Hebergement h = new Hebergement();
            h.setId_hebergement(rs.getInt("h.id_hebergement"));
            h.setTitre(rs.getString("h.titre"));
            h.setDesc_hebergement(rs.getString("h.desc_hebergement"));
            h.setCapacite(rs.getInt("h.capacite"));
            h.setType_hebergement(rs.getString("h.type_hebergement"));
            h.setDisponible_heberg(rs.getBoolean("h.disponible_heberg"));
            h.setPrixParNuit(rs.getFloat("h.prixParNuit"));
            h.setImage(rs.getString("h.image"));

            Reservation r = new Reservation(
                    rs.getInt("r.id_reservation"),
                    h,
                    u,
                    rs.getString("r.dateDebutR"),
                    rs.getString("r.dateFinR"),
                    rs.getString("r.statutR")
            );

            list.add(r);
        }

        return list;
    }

    public List<Reservation> getReservationsByHebergement(int hebergementId) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String req = "SELECT " +
                "r.id_reservation, " +
                "r.dateDebutR, " +
                "r.dateFinR, " +
                "r.statutR, " +
                "h.id_hebergement, " +
                "h.titre, " +
                "h.desc_hebergement, " +
                "h.capacite, " +
                "h.type_hebergement, " +
                "h.disponible_heberg, " +
                "h.prixParNuit, " +
                "h.image, " +
                "u.id as id_user, " +
                "u.nom, " +
                "u.prenom, " +
                "u.e_mail as email, " +
                "u.num_tel as tel " +
                "FROM reservation r " +
                "INNER JOIN hebergement h ON r.hebergement_id = h.id_hebergement " +
                "INNER JOIN users u ON r.user_id = u.id " +
                "WHERE r.hebergement_id = ?";

        PreparedStatement ps = con.prepareStatement(req);
        ps.setInt(1, hebergementId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            User u = new User();
            u.setId(rs.getInt("id_user"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setE_mail(rs.getString("email"));
            u.setNum_tel(rs.getString("tel"));

            Hebergement h = new Hebergement();
            h.setId_hebergement(rs.getInt("h.id_hebergement"));
            h.setTitre(rs.getString("h.titre"));
            h.setDesc_hebergement(rs.getString("h.desc_hebergement"));
            h.setCapacite(rs.getInt("h.capacite"));
            h.setType_hebergement(rs.getString("h.type_hebergement"));
            h.setDisponible_heberg(rs.getBoolean("h.disponible_heberg"));
            h.setPrixParNuit(rs.getFloat("h.prixParNuit"));
            h.setImage(rs.getString("h.image"));

            Reservation r = new Reservation(
                    rs.getInt("r.id_reservation"),
                    h,
                    u,
                    rs.getString("r.dateDebutR"),
                    rs.getString("r.dateFinR"),
                    rs.getString("r.statutR")
            );

            list.add(r);
        }

        return list;
    }
}
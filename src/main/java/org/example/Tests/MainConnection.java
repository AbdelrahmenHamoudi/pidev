package org.example.Tests;

import org.example.Entites.hebergement.Hebergement;
import org.example.Entites.hebergement.Reservation;
import org.example.Entites.user.Role;
import org.example.Entites.user.Status;
import org.example.Entites.user.User;
import org.example.Services.hebergement.HebergementCRUD;
import org.example.Services.hebergement.ReservationCRUD;
import org.example.Services.user.UserCRUD;

import java.sql.SQLException;

public class MainConnection {

    public static void main(String[] args) {

        HebergementCRUD hebergementCRUD = new HebergementCRUD();
        ReservationCRUD reservationCRUD = new ReservationCRUD();
        UserCRUD userCRUD = new UserCRUD();

        Hebergement h1 = new Hebergement(
                "Maison vue sur mer",
                "Belle maison moderne avec vue panoramique sur la mer",
                6,
                "Maison",
                true,
                180.0f,
                "maison_mer.jpg"
        );


        User u1 = new User(
                "Ben Ali",
                "Iheb",
                "1999-05-14",
                "iheb.benali@gmail.com",
                "22123456",
                "pass123",
                Role.user,
                "avatar1.png",
                Status.Unbanned
        );

        try {
            hebergementCRUD.ajouterh(h1);
            userCRUD.createUser(u1);

            // ✅ CORRECTION : Respecter l'ordre des paramètres du constructeur
            Reservation r1 = new Reservation(
                    0,                    // id_reservation
                    h1,                   // hebergement
                                     // user
                    "2026-07-10",         // dateDebutR
                    "2026-07-20",         // dateFinR  (ajout de la date de fin)
                    "hmidi",              // nomR      (nom de famille)
                    "iheb",                // prenomR   (prénom)
                    50936876,              // telR      (téléphone)
                    "EN ATTENTE"           // statutR
            );

            reservationCRUD.ajouterh(r1);

            System.out.println("Données insérées avec succès ");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
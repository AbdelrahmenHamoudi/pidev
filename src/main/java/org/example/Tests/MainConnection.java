package org.example.Tests;

import org.example.Entites.Hebergement;
import org.example.Entites.Reservation;
import org.example.Entites.Role;
import org.example.Entites.User;
import org.example.Services.HebergementCRUD;
import org.example.Services.ReservationCRUD;

import java.sql.SQLException;

public class MainConnection {
    public static void main(String[] args) {
        HebergementCRUD hebergementCRUD = new HebergementCRUD();
        ReservationCRUD reservationCRUD = new ReservationCRUD();

        Hebergement h1 = new Hebergement(
                1,
                "Maison vue sur mer",
                "Belle maison moderne avec vue panoramique sur la mer",
                6,
                "Maison",
                true,
                180.0f,
                "maison_mer.jpg"
        );


        try {
            hebergementCRUD.ajouterh(h1);
            System.out.println("Hebergement ajouter ");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        User u1 = new User(
                1,
                "Trabelsi",
                "Sarra",
                "2001-08-23",
                "sarra.trabelsi@gmail.com",
                "98765432",
                "pass456",
                "sarra.png",
                Role.user,
                "active"
        );


        Reservation r1 = new Reservation(
                1,
                h1,
                u1,
                "2026-07-10",
                "2026-07-15",
                900.0f,
                40.0f,
                "EN ATTENTE"
        );

        try {
            reservationCRUD.ajouterh(r1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }
}

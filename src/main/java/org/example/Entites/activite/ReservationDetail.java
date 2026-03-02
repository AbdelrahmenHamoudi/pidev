package org.example.Entites.activite;

import org.example.Entites.user.User;
import org.example.Utils.UserSession;

import java.time.LocalDate;
import java.time.LocalTime;

public class ReservationDetail {
    public int id_reservation;
    public int id_user;
    public int id_planning;
    public String statut;
    public LocalDate date_planning;
    public LocalTime heure_debut;
    public LocalTime heure_fin;
    public int nb_places;
    public int id_activite;
    public String nomActivite;
    public String lieu;
    public String type;
    public float prix;

    /**
     * Récupère le nom de l'utilisateur à partir de son ID
     * Pour l'instant, retourne un format simple car on n'a pas les infos
     */
    public String getUserName() {
        // Option 1: Récupérer depuis UserSession si c'est l'utilisateur connecté
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getId() == id_user) {
            return currentUser.getPrenom() + " " + currentUser.getNom();
        }

        // Option 2: Retourner un format générique
        return "Utilisateur #" + id_user;
    }

    /**
     * Version améliorée qui pourrait faire une requête en BD
     * (à implémenter si nécessaire avec un service)
     */
    public String getUserNameDetailed() {
        // Idéalement, on aurait un UserService pour récupérer le nom
        // Pour l'instant, on retourne le format simple
        return "Utilisateur #" + id_user;
    }

    @Override
    public String toString() {
        return "Réservation #" + id_reservation + " - " + nomActivite + " - " + getUserName();
    }
}
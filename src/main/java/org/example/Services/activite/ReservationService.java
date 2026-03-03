package org.example.Services.activite;

import org.example.Entites.activite.Reservation;
import java.util.List;

public interface ReservationService {

    // Créer une réservation
    boolean addReservation(Reservation reservation);

    // Annuler une réservation (met le statut à "ANNULEE" et rend la place)
    boolean annulerReservation(int idReservation, int idUser);

    // Voir toutes les réservations d'un utilisateur
    List<Reservation> getReservationsByUser(int idUser);

    // Récupérer une réservation par ID
    Reservation getReservationById(int id);

    // Vérifier si l'utilisateur a déjà réservé ce planning
    boolean hasAlreadyReserved(int idUser, int idPlanning);

    // Récupérer toutes les réservations (pour admin)
    List<Reservation> getAllReservations();

    // Récupérer les réservations par planning
    List<Reservation> getReservationsByPlanning(int idPlanning);

    // Récupérer les réservations par statut
    List<Reservation> getReservationsByStatut(String statut);

    // Compter les réservations d'un utilisateur
    int countReservationsByUser(int idUser);

    // Supprimer définitivement une réservation (admin seulement)
    boolean deleteReservation(int idReservation);
}
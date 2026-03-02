package org.example.Entites.activite;

import org.example.Entites.user.User;

public class Reservation {
    private int id_reservation;
    private User user;           // L'utilisateur qui a réservé
    private Planning planning;    // Le planning réservé
    private String statut;        // 'CONFIRMEE' ou 'ANNULEE'

    // Constructeur vide
    public Reservation() {
    }

    // Constructeur complet
    public Reservation(int id_reservation, User user, Planning planning, String statut) {
        this.id_reservation = id_reservation;
        this.user = user;
        this.planning = planning;
        this.statut = statut;
    }

    // Constructeur sans ID (pour insertion)
    public Reservation(User user, Planning planning, String statut) {
        this.user = user;
        this.planning = planning;
        this.statut = statut;
    }

    // Getters et Setters
    public int getIdReservation() {
        return id_reservation;
    }

    public void setIdReservation(int id_reservation) {
        this.id_reservation = id_reservation;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Planning getPlanning() {
        return planning;
    }

    public void setPlanning(Planning planning) {
        this.planning = planning;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    // Méthodes utilitaires pour faciliter l'affichage
    public int getIdUser() {
        return user != null ? user.getId() : 0;
    }

    public int getIdPlanning() {
        return planning != null ? planning.getIdPlanning() : 0;
    }

    public String getNomUser() {
        return user != null ? user.getPrenom() + " " + user.getNom() : "Inconnu";
    }

    public String getNomActivite() {
        if (planning != null && planning.getActivite() != null) {
            return planning.getActivite().getNomA();
        }
        return "Inconnue";
    }

    public String getLieuActivite() {
        if (planning != null && planning.getActivite() != null) {
            return planning.getActivite().getLieu();
        }
        return "Inconnu";
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id_reservation +
                ", user=" + (user != null ? user.getPrenom() + " " + user.getNom() : "null") +
                ", activite=" + getNomActivite() +
                ", statut='" + statut + '\'' +
                '}';
    }
}
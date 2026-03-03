package org.example.Entites.trajet;

import org.example.Entites.user.User;

import java.sql.Date;

public class Trajet {

    private int idTrajet;
    private Voiture Voiture;
    private  User User;
    private String pointDepart;
    private String pointArrivee;
    private float distanceKm;
    private Date dateReservation;
    private StatutVoiture statut;
    private int nbPersonnes;

    public Trajet() {}




    public Trajet(int idTrajet, Voiture Voiture,User User, String pointDepart, String pointArrivee, float distanceKm, Date dateReservation, StatutVoiture statut, int nbPersonnes) {
        this.idTrajet = idTrajet;
        this.pointDepart = pointDepart;
        this.pointArrivee = pointArrivee;
        this.distanceKm = distanceKm;
        this.dateReservation = dateReservation;
        this.statut = statut;
        this.nbPersonnes = nbPersonnes;
    }

    public String getPointDepart() {
        return pointDepart;
    }

    public void setPointDepart(String pointDepart) {
        this.pointDepart = pointDepart;
    }

    public int getIdTrajet() {
        return idTrajet;
    }

    public void setIdTrajet(int idTrajet) {
        this.idTrajet = idTrajet;
    }

    public String getPointArrivee() {
        return pointArrivee;
    }

    public void setPointArrivee(String pointArrivee) {
        this.pointArrivee = pointArrivee;
    }

    public float getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(float distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Date getDateReservation() {
        return dateReservation;
    }

    public void setDateReservation(Date dateReservation) {
        this.dateReservation = dateReservation;
    }

    public StatutVoiture getStatut() {
        return statut;
    }

    public void setStatut(StatutVoiture statut) {
        this.statut = statut;
    }

    public Voiture getIdVoiture() {
        return Voiture;
    }

    public void setIdVoiture(Voiture idVoiture) {
        this.Voiture = idVoiture;
    }

    public User getIdUser() {
        return User;
    }

    public void setIdUser(User idUser) {
        this.User = idUser;
    }

    public int getNbPersonnes() {
        return nbPersonnes;
    }

    public void setNbPersonnes(int nbPersonnes) {
        this.nbPersonnes = nbPersonnes;
    }

    @Override
    public String toString() {
        return "Trajet{" +
                "idTrajet=" + idTrajet +
                ", idVoiture=" + Voiture +
                ", idUser=" + User +
                ", pointDepart='" + pointDepart + '\'' +
                ", pointArrivee='" + pointArrivee + '\'' +
                ", distanceKm=" + distanceKm +
                ", dateReservation=" + dateReservation +
                ", statut=" + statut +
                ", nbPersonnes=" + nbPersonnes +
                '}';
    }
}

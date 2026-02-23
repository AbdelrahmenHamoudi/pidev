package models;

import java.time.LocalDate;
import java.time.LocalTime;

public class Planning {
    private int idPlanning;
    private int idActivite;
    private LocalDate datePlanning;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private String etat;
    private int nbPlacesRestantes;
    private Activite activite;

    // Constructeur vide
    public Planning() {
    }

    // Constructeur complet
    public Planning(int idPlanning, int idActivite, LocalDate datePlanning, 
                   LocalTime heureDebut, LocalTime heureFin, String etat, int nbPlacesRestantes) {
        this.idPlanning = idPlanning;
        this.idActivite = idActivite;
        this.datePlanning = datePlanning;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.etat = etat;
        this.nbPlacesRestantes = nbPlacesRestantes;
    }

    // Constructeur sans ID (pour insertion)
    public Planning(int idActivite, LocalDate datePlanning, 
                   LocalTime heureDebut, LocalTime heureFin, String etat, int nbPlacesRestantes) {
        this.idActivite = idActivite;
        this.datePlanning = datePlanning;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.etat = etat;
        this.nbPlacesRestantes = nbPlacesRestantes;
    }

    // Getters et Setters
    public int getIdPlanning() {
        return idPlanning;
    }

    public void setIdPlanning(int idPlanning) {
        this.idPlanning = idPlanning;
    }

    public int getIdActivite() {
        return idActivite;
    }

    public void setIdActivite(int idActivite) {
        this.idActivite = idActivite;
    }

    public LocalDate getDatePlanning() {
        return datePlanning;
    }

    public void setDatePlanning(LocalDate datePlanning) {
        this.datePlanning = datePlanning;
    }

    public LocalTime getHeureDebut() {
        return heureDebut;
    }

    public void setHeureDebut(LocalTime heureDebut) {
        this.heureDebut = heureDebut;
    }

    public LocalTime getHeureFin() {
        return heureFin;
    }

    public void setHeureFin(LocalTime heureFin) {
        this.heureFin = heureFin;
    }

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public int getNbPlacesRestantes() {
        return nbPlacesRestantes;
    }

    public void setNbPlacesRestantes(int nbPlacesRestantes) {
        this.nbPlacesRestantes = nbPlacesRestantes;
    }

    public Activite getActivite() {
        return activite;
    }

    public void setActivite(Activite activite) {
        this.activite = activite;
    }

    @Override
    public String toString() {
        return "Planning{" +
                "idPlanning=" + idPlanning +
                ", idActivite=" + idActivite +
                ", datePlanning=" + datePlanning +
                ", heureDebut=" + heureDebut +
                ", heureFin=" + heureFin +
                ", etat='" + etat + '\'' +
                ", nbPlacesRestantes=" + nbPlacesRestantes +
                '}';
    }
}

package org.example.Entites.activite;

import java.time.LocalDate;
import java.time.LocalTime;

public class Planning {
    private int id_planning;
    private int id_activite;
    private LocalDate date_planning;
    private LocalTime heure_debut;
    private LocalTime heure_fin;
    private String etat;
    private int nb_places_restantes;
    private Activite activite;  // L'activité associée (optionnel)

    // Constructeur vide
    public Planning() {
    }

    // Constructeur complet
    public Planning(int id_planning, int id_activite, LocalDate date_planning,
                    LocalTime heure_debut, LocalTime heure_fin, String etat,
                    int nb_places_restantes) {
        this.id_planning = id_planning;
        this.id_activite = id_activite;
        this.date_planning = date_planning;
        this.heure_debut = heure_debut;
        this.heure_fin = heure_fin;
        this.etat = etat;
        this.nb_places_restantes = nb_places_restantes;
    }

    // Constructeur sans ID (pour insertion)
    public Planning(int id_activite, LocalDate date_planning,
                    LocalTime heure_debut, LocalTime heure_fin, String etat,
                    int nb_places_restantes) {
        this.id_activite = id_activite;
        this.date_planning = date_planning;
        this.heure_debut = heure_debut;
        this.heure_fin = heure_fin;
        this.etat = etat;
        this.nb_places_restantes = nb_places_restantes;
    }

    // Getters et Setters
    public int getIdPlanning() {
        return id_planning;
    }

    public void setIdPlanning(int id_planning) {
        this.id_planning = id_planning;
    }

    public int getIdActivite() {
        return id_activite;
    }

    public void setIdActivite(int id_activite) {
        this.id_activite = id_activite;
    }

    public LocalDate getDatePlanning() {
        return date_planning;
    }

    public void setDatePlanning(LocalDate date_planning) {
        this.date_planning = date_planning;
    }

    public LocalTime getHeureDebut() {
        return heure_debut;
    }

    public void setHeureDebut(LocalTime heure_debut) {
        this.heure_debut = heure_debut;
    }

    public LocalTime getHeureFin() {
        return heure_fin;
    }

    public void setHeureFin(LocalTime heure_fin) {
        this.heure_fin = heure_fin;
    }

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public int getNbPlacesRestantes() {
        return nb_places_restantes;
    }

    public void setNbPlacesRestantes(int nb_places_restantes) {
        this.nb_places_restantes = nb_places_restantes;
    }

    public Activite getActivite() {
        return activite;
    }

    public void setActivite(Activite activite) {
        this.activite = activite;
        if (activite != null) {
            this.id_activite = activite.getIdActivite();
        }
    }

    @Override
    public String toString() {
        return "Planning{" +
                "id=" + id_planning +
                ", date=" + date_planning +
                ", heure=" + heure_debut + "-" + heure_fin +
                ", etat='" + etat + '\'' +
                ", places=" + nb_places_restantes +
                '}';
    }
}
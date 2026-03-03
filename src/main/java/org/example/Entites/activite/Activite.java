package org.example.Entites.activite;

import java.util.ArrayList;
import java.util.List;

public class Activite {
    private int id_activite;
    private String nomA;
    private String descriptionA;
    private String lieu;
    private float prix_par_personne;
    private int capacite_max;
    private String type;
    private String statut;
    private String image;
    private List<Planning> plannings;  // Liste des plannings associés

    // Constructeur vide
    public Activite() {
        this.plannings = new ArrayList<>();
    }

    // Constructeur complet
    public Activite(int id_activite, String nomA, String descriptionA, String lieu,
                    float prix_par_personne, int capacite_max, String type,
                    String statut, String image) {
        this.id_activite = id_activite;
        this.nomA = nomA;
        this.descriptionA = descriptionA;
        this.lieu = lieu;
        this.prix_par_personne = prix_par_personne;
        this.capacite_max = capacite_max;
        this.type = type;
        this.statut = statut;
        this.image = image;
        this.plannings = new ArrayList<>();
    }

    // Constructeur sans ID (pour insertion)
    public Activite(String nomA, String descriptionA, String lieu,
                    float prix_par_personne, int capacite_max, String type,
                    String statut, String image) {
        this.nomA = nomA;
        this.descriptionA = descriptionA;
        this.lieu = lieu;
        this.prix_par_personne = prix_par_personne;
        this.capacite_max = capacite_max;
        this.type = type;
        this.statut = statut;
        this.image = image;
        this.plannings = new ArrayList<>();
    }

    // Getters et Setters
    public int getIdActivite() {
        return id_activite;
    }

    public void setIdActivite(int id_activite) {
        this.id_activite = id_activite;
    }

    public String getNomA() {
        return nomA;
    }

    public void setNomA(String nomA) {
        this.nomA = nomA;
    }

    public String getDescriptionA() {
        return descriptionA;
    }

    public void setDescriptionA(String descriptionA) {
        this.descriptionA = descriptionA;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public float getPrixParPersonne() {
        return prix_par_personne;
    }

    public void setPrixParPersonne(float prix_par_personne) {
        this.prix_par_personne = prix_par_personne;
    }

    public int getCapaciteMax() {
        return capacite_max;
    }

    public void setCapaciteMax(int capacite_max) {
        this.capacite_max = capacite_max;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<Planning> getPlannings() {
        return plannings;
    }

    public void setPlannings(List<Planning> plannings) {
        this.plannings = plannings;
    }

    public void addPlanning(Planning planning) {
        this.plannings.add(planning);
    }

    @Override
    public String toString() {
        return "Activite{" +
                "id=" + id_activite +
                ", nom='" + nomA + '\'' +
                ", lieu='" + lieu + '\'' +
                ", prix=" + prix_par_personne +
                ", type='" + type + '\'' +
                ", statut='" + statut + '\'' +
                '}';
    }
}
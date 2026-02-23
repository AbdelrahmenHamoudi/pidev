package models;

import java.util.ArrayList;
import java.util.List;

public class Activite {
    private int idActivite;
    private String nomA;
    private String descriptionA;
    private String lieu;
    private float prixParPersonne;
    private int capaciteMax;
    private String type;
    private String statut;
    private String image;
    private List<Planning> plannings;

    // Constructeur vide
    public Activite() {
        this.plannings = new ArrayList<>();
    }

    // Constructeur complet
    public Activite(int idActivite, String nomA, String descriptionA, String lieu, 
                   float prixParPersonne, int capaciteMax, String type, String statut, String image) {
        this.idActivite = idActivite;
        this.nomA = nomA;
        this.descriptionA = descriptionA;
        this.lieu = lieu;
        this.prixParPersonne = prixParPersonne;
        this.capaciteMax = capaciteMax;
        this.type = type;
        this.statut = statut;
        this.image = image;
        this.plannings = new ArrayList<>();
    }

    // Constructeur sans ID (pour insertion)
    public Activite(String nomA, String descriptionA, String lieu, 
                   float prixParPersonne, int capaciteMax, String type, String statut, String image) {
        this.nomA = nomA;
        this.descriptionA = descriptionA;
        this.lieu = lieu;
        this.prixParPersonne = prixParPersonne;
        this.capaciteMax = capaciteMax;
        this.type = type;
        this.statut = statut;
        this.image = image;
        this.plannings = new ArrayList<>();
    }

    // Getters et Setters
    public int getIdActivite() {
        return idActivite;
    }

    public void setIdActivite(int idActivite) {
        this.idActivite = idActivite;
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
        return prixParPersonne;
    }

    public void setPrixParPersonne(float prixParPersonne) {
        this.prixParPersonne = prixParPersonne;
    }

    public int getCapaciteMax() {
        return capaciteMax;
    }

    public void setCapaciteMax(int capaciteMax) {
        this.capaciteMax = capaciteMax;
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
                "idActivite=" + idActivite +
                ", nomA='" + nomA + '\'' +
                ", lieu='" + lieu + '\'' +
                ", prixParPersonne=" + prixParPersonne +
                ", type='" + type + '\'' +
                ", statut='" + statut + '\'' +
                '}';
    }
}

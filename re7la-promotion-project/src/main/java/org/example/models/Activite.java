package org.example.models;

public class Activite {
    private int id;
    private String nom;
    private String lieu;
    private String type;

    public Activite() {
    }

    public Activite(int id, String nom, String lieu, String type) {
        this.id = id;
        this.nom = nom;
        this.lieu = lieu;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return nom + " - " + lieu + " (" + type + ")";
    }
}

package org.example.models;

public class Hebergement {
    private int id;
    private String nom;
    private String ville;
    private String type;

    public Hebergement() {
    }

    public Hebergement(int id, String nom, String ville, String type) {
        this.id = id;
        this.nom = nom;
        this.ville = ville;
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

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return nom + " - " + ville + " (" + type + ")";
    }
}

package org.example.models;

public class Transport {
    private int id;
    private String nom;
    private String typeTransport;
    private String trajet;

    public Transport() {
    }

    public Transport(int id, String nom, String typeTransport, String trajet) {
        this.id = id;
        this.nom = nom;
        this.typeTransport = typeTransport;
        this.trajet = trajet;
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

    public String getTypeTransport() {
        return typeTransport;
    }

    public void setTypeTransport(String typeTransport) {
        this.typeTransport = typeTransport;
    }

    public String getTrajet() {
        return trajet;
    }

    public void setTrajet(String trajet) {
        this.trajet = trajet;
    }

    @Override
    public String toString() {
        return nom + " - " + trajet + " (" + typeTransport + ")";
    }
}

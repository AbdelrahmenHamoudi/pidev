package org.example.Entites.hebergement;

import java.util.Objects;

public class Hebergement {

    private int id_hebergement;
    private String titre;
    private String desc_hebergement;
    private int capacite;
    private String type_hebergement;
    private boolean disponible_heberg;
    private float prixParNuit;
    private String image;


    public Hebergement()
    {
    }


    public Hebergement(int id_hebergement, String titre, String desc_hebergement,
                       int capacite, String type_hebergement,
                       boolean disponible_heberg, float prixParNuit, String image) {
        this.id_hebergement = id_hebergement;
        this.titre = titre;
        this.desc_hebergement = desc_hebergement;
        this.capacite = capacite;
        this.type_hebergement = type_hebergement;
        this.disponible_heberg = disponible_heberg;
        this.prixParNuit = prixParNuit;
        this.image = image;
    }


    public Hebergement(String titre, String desc_hebergement,
                       int capacite, String type_hebergement,
                       boolean disponible_heberg, float prixParNuit, String image) {
        this.titre = titre;
        this.desc_hebergement = desc_hebergement;
        this.capacite = capacite;
        this.type_hebergement = type_hebergement;
        this.disponible_heberg = disponible_heberg;
        this.prixParNuit = prixParNuit;
        this.image = image;
    }



    public int getId_hebergement() {
        return id_hebergement;
    }

    public void setId_hebergement(int id_hebergement) {
        this.id_hebergement = id_hebergement;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDesc_hebergement() {
        return desc_hebergement;
    }

    public void setDesc_hebergement(String desc_hebergement) {
        this.desc_hebergement = desc_hebergement;
    }

    public int getCapacite() {
        return capacite;
    }

    public void setCapacite(int capacite) {
        this.capacite = capacite;
    }

    public String getType_hebergement() {
        return type_hebergement;
    }

    public void setType_hebergement(String type_hebergement) {
        this.type_hebergement = type_hebergement;
    }

    public boolean isDisponible_heberg() {
        return disponible_heberg;
    }

    public void setDisponible_heberg(boolean disponible_heberg) {
        this.disponible_heberg = disponible_heberg;
    }

    public float getPrixParNuit() {
        return prixParNuit;
    }

    public void setPrixParNuit(float prixParNuit) {
        this.prixParNuit = prixParNuit;
    }


    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }



    @Override
    public String toString() {
        return "Hebergement{" +
                "id_hebergement=" + id_hebergement +
                ", titre='" + titre + '\'' +
                ", capacite=" + capacite +
                ", prixParNuit=" + prixParNuit +
                ", image='" + image + '\'' +
                '}';
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Hebergement)) return false;
        Hebergement that = (Hebergement) o;
        return id_hebergement == that.id_hebergement;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id_hebergement);
    }
}

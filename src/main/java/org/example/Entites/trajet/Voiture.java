package org.example.Entites.trajet;

public class Voiture {
    private int id_Voiture;
    private String marque;
    private String modele;
    private String immatriculation;
    private float prixKm;
    private boolean avecChauffeur;
    private boolean disponibilite;
    private String description;
    private String image;
    private int nb_places;


    public Voiture() {}
    public Voiture(int id_Voiture, String marque, String modele, String immatriculation, float prixKm, boolean avecChauffeur, boolean disponibilite, String description, String image, int nb_places) {
        this.id_Voiture = id_Voiture;
        this.marque = marque;
        this.modele = modele;
        this.immatriculation = immatriculation;
        this.prixKm = prixKm;
        this.avecChauffeur = avecChauffeur;
        this.disponibilite = disponibilite;
        this.description = description;
        this.image = image;
        this.nb_places = nb_places;
    }

    public int getIdVoiture() {
        return id_Voiture;
    }

    public void setIdVoiture(int idVoiture) {
        this.id_Voiture = idVoiture;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDisponibilite() {
        return disponibilite;
    }

    public void setDisponibilite(boolean disponibilite) {
        this.disponibilite = disponibilite;
    }

    public boolean isAvecChauffeur() {
        return avecChauffeur;
    }

    public void setAvecChauffeur(boolean avecChauffeur) {
        this.avecChauffeur = avecChauffeur;
    }

    public float getPrixKm() {
        return prixKm;
    }

    public void setPrixKm(float prixKm) {
        this.prixKm = prixKm;
    }

    public String getImmatriculation() {
        return immatriculation;
    }

    public void setImmatriculation(String immatriculation) {
        this.immatriculation = immatriculation;
    }

    public String getModele() {
        return modele;
    }

    public void setModele(String modele) {
        this.modele = modele;
    }

    public String getMarque() {
        return marque;
    }

    public void setMarque(String marque) {
        this.marque = marque;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getNb_places() {
        return nb_places;
    }

    public void setNb_places(int nb_places) {
        this.nb_places = nb_places;
    }

    @Override
    public String toString() {
        return "Voiture{" +
                "idVoiture=" + id_Voiture +
                ", marque='" + marque + '\'' +
                ", modele='" + modele + '\'' +
                ", immatriculation='" + immatriculation + '\'' +
                ", prixKm=" + prixKm +
                ", avecChauffeur=" + avecChauffeur +
                ", disponibilite=" + disponibilite +
                ", description='" + description + '\'' +
                ", image='" + image + '\'' +
                ", nb_places=" + nb_places +
                '}';
    }
}


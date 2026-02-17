package org.example.Entites;

import java.util.Objects;

public class Commentaire {
    private int idCommentaire;
    private User id_utilisateur;
    private Publication publication;
    private String contenuC;
    private String dateCreationC;
    private boolean statutC;
    public Commentaire(){
    }

    public Commentaire(int idCommentaire,User id_utilisateur,Publication publication,
                       String contenuC, String dateCreationC, boolean statutC) {
        this.idCommentaire = idCommentaire;
        this.id_utilisateur = id_utilisateur;
        this.publication = publication;
        this.contenuC = contenuC;
        this.dateCreationC = dateCreationC;
        this.statutC = statutC;
    }

    public Commentaire(User id_utilisateur, Publication publication, String contenuC, String dateCreationC, boolean statutC) {
        this.id_utilisateur = id_utilisateur;
        this.publication = publication;
        this.contenuC = contenuC;
        this.dateCreationC = dateCreationC;
        this.statutC = statutC;
    }

    public int getIdCommentaire() {
        return idCommentaire;
    }

    public void setIdCommentaire(int idCommentaire) {
        this.idCommentaire = idCommentaire;
    }

    public User getId_utilisateur() {
        return id_utilisateur;
    }

    public void setId_utilisateur(User id_utilisateur) {
        this.id_utilisateur = id_utilisateur;
    }

    public Publication getPublication() {
        return publication;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    public String getContenuC() {
        return contenuC;
    }

    public void setContenuC(String contenuC) {
        this.contenuC = contenuC;
    }

    public String getDateCreationC() {
        return dateCreationC;
    }

    public void setDateCreationC(String dateCreationC) {
        this.dateCreationC = dateCreationC;
    }

    public boolean isStatutC() {
        return statutC;
    }

    public void setStatutC(boolean statutC) {
        this.statutC = statutC;
    }

    @Override
    public String toString() {
        return "Commentaire{" +
                "idCommentaire=" + idCommentaire +
                ", id_utilisateur=" + id_utilisateur +
                ", publication=" + publication +
                ", contenuC='" + contenuC + '\'' +
                ", dateCreationC='" + dateCreationC + '\'' +
                ", statutC=" + statutC +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Commentaire that = (Commentaire) o;
        return idCommentaire == that.idCommentaire && statutC == that.statutC && Objects.equals(id_utilisateur, that.id_utilisateur) && Objects.equals(publication, that.publication) && Objects.equals(contenuC, that.contenuC) && Objects.equals(dateCreationC, that.dateCreationC);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCommentaire, id_utilisateur, publication, contenuC, dateCreationC, statutC);
    }
}


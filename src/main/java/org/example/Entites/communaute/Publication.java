package org.example.Entites.communaute;



import org.example.Entites.user.User;

import java.util.Objects;

public class Publication{

    private int idPublication;
    private Commentaire idCommentaire;
    private User id_utilisateur;
    private String ImgURL;
    private TypeCible typeCible;
    private String dateCreation;
    private String dateModif;
    private StatutP statutP;
    private boolean estVerifie;
    private String DescriptionP;

    public Publication() {
    }

    public Publication(int idPublication,Commentaire commentaire, User id_utilisateur, String ImgURL,TypeCible typeCible,String dateCreation,String dateModif,StatutP statutP,boolean estVerifie,String DescriptionP){
        this.idPublication = idPublication;
        this.idCommentaire= idCommentaire;
        this.id_utilisateur = id_utilisateur ;
        this.ImgURL = ImgURL ;
        this.typeCible = typeCible;
        this.dateCreation = dateCreation;
        this.dateModif = dateModif;
        this.statutP = statutP;
        this.estVerifie = estVerifie;
        this.DescriptionP = DescriptionP;
    }
   //Constructeur paramétré pour l'affichage
    public Publication(Commentaire idCommentaire, User id_utilisateur, String imgURL, TypeCible typeCible, String dateCreation, String dateModif, StatutP statutP, boolean estVerifie, String descriptionP) {
        this.idCommentaire = idCommentaire;
        this.id_utilisateur = id_utilisateur;
        this.ImgURL = imgURL;
        this.typeCible = typeCible;
        this.dateCreation = dateCreation;
        this.dateModif = dateModif;
        this.statutP = statutP;
        this.estVerifie = estVerifie;
        this.DescriptionP = descriptionP;
    }
    public Publication(int idPublication, String imgURL, TypeCible typeCible, String dateCreation, String dateModif, StatutP statutP, boolean estVerifie, String descriptionP) {
        this.idPublication = idPublication;
        this.ImgURL = imgURL;
        this.typeCible = typeCible;
        this.dateCreation = dateCreation;
        this.dateModif = dateModif;
        this.statutP = statutP;
        this.estVerifie = estVerifie;
        this.DescriptionP = descriptionP;
    }

    public int getIdPublication() {
        return idPublication;
    }

    public void setIdPublication(int idPublication) {
        this.idPublication = idPublication;
    }

    public Commentaire getIdCommentaire() {
        return idCommentaire;
    }

    public void setIdCommentaire(Commentaire idCommentaire) {
        this.idCommentaire = idCommentaire;
    }

    public User getId_utilisateur() {
        return id_utilisateur;
    }

    public void setId_utilisateur(User id_utilisateur) {
        this.id_utilisateur = id_utilisateur;
    }

    public String getImgURL() {
        return ImgURL;
    }

    public void setImgURL(String imgURL) {
        ImgURL = imgURL;
    }

    public TypeCible getTypeCible() {
        return typeCible;
    }

    public void setTypeCible(TypeCible typeCible) {
        this.typeCible = typeCible;
    }

    public String getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(String dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getDateModif() {
        return dateModif;
    }

    public void setDateModif(String dateModif) {
        this.dateModif = dateModif;
    }

    public StatutP getStatutP() {
        return statutP;
    }

    public void setStatutP(StatutP statutP) {
        this.statutP = statutP;
    }

    public boolean isEstVerifie() {
        return estVerifie;
    }

    public void setEstVerifie(boolean estVerifie) {
        this.estVerifie = estVerifie;
    }

    public String getDescriptionP() {
        return DescriptionP;
    }

    public void setDescriptionP(String descriptionP) {
        DescriptionP = descriptionP;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Publication that = (Publication) o;
        return idPublication == that.idPublication && estVerifie == that.estVerifie && Objects.equals(idCommentaire, that.idCommentaire) && Objects.equals(id_utilisateur, that.id_utilisateur) && Objects.equals(ImgURL, that.ImgURL) && typeCible == that.typeCible && Objects.equals(dateCreation, that.dateCreation) && Objects.equals(dateModif, that.dateModif) && statutP == that.statutP;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPublication, idCommentaire, id_utilisateur, ImgURL, typeCible, dateCreation, dateModif, statutP, estVerifie);
    }


}


package org.example.Entites.hebergement;

import javafx.beans.binding.BooleanExpression;
import org.example.Entites.user.User;

import java.util.Objects;

public class Reservation {

    private int id_reservation;
    private Hebergement hebergement;

    private String dateDebutR;
    private String dateFinR;
    private String nomR;
    private String prenomR;
    private int telR;
    private String statutR;

    public Reservation() {
    }


    public Reservation(int id_reservation, Hebergement hebergement, String dateDebutR, String dateFinR, String nomR, String prenomR, int telR, String statutR) {
        this.id_reservation = id_reservation;
        this.hebergement = hebergement;

        this.dateDebutR = dateDebutR;
        this.dateFinR = dateFinR;
        this.nomR = nomR;
        this.prenomR = prenomR;
        this.telR = telR;
        this.statutR = statutR;
    }


    public Reservation(Hebergement hebergement, String dateDebutR, String dateFinR, String nomR, String prenomR, int telR, String statutR) {
        this.hebergement = hebergement;

        this.dateDebutR = dateDebutR;
        this.dateFinR = dateFinR;
        this.nomR = nomR;
        this.prenomR = prenomR;
        this.telR = telR;
        this.statutR = statutR;
    }

    public int getId_reservation() {
        return id_reservation;
    }

    public Hebergement getHebergement() {
        return hebergement;
    }


    public String getDateDebutR() {
        return dateDebutR;
    }

    public String getDateFinR() {
        return dateFinR;
    }

    public String getNomR() {
        return nomR;
    }

    public String getPrenomR() {
        return prenomR;
    }

    public int getTelR() {
        return telR;
    }

    public String getStatutR() {
        return statutR;
    }

    public void setHebergement(Hebergement hebergement) {
        this.hebergement = hebergement;
    }

    public void setId_reservation(int id_reservation) {
        this.id_reservation = id_reservation;
    }


    public void setDateDebutR(String dateDebutR) {
        this.dateDebutR = dateDebutR;
    }

    public void setDateFinR(String dateFinR) {
        this.dateFinR = dateFinR;
    }

    public void setNomR(String nomR) {
        this.nomR = nomR;
    }

    public void setPrenomR(String prenomR) {
        this.prenomR = prenomR;
    }

    public void setTelR(int telR) {
        this.telR = telR;
    }

    public void setStatutR(String statutR) {
        this.statutR = statutR;
    }


    @Override
    public String toString() {
        return "Reservation{" +
                "id_reservation=" + id_reservation +
                ", hebergement=" + hebergement +

                ", dateDebutR='" + dateDebutR + '\'' +
                ", dateFinR='" + dateFinR + '\'' +
                ", nomR='" + nomR + '\'' +
                ", prenomR='" + prenomR + '\'' +
                ", telR=" + telR +
                ", statutR='" + statutR + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        return id_reservation == that.id_reservation && telR == that.telR && Objects.equals(hebergement, that.hebergement) && Objects.equals(dateDebutR, that.dateDebutR) && Objects.equals(dateFinR, that.dateFinR) && Objects.equals(nomR, that.nomR) && Objects.equals(prenomR, that.prenomR) && Objects.equals(statutR, that.statutR);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id_reservation, hebergement, dateDebutR, dateFinR, nomR, prenomR, telR, statutR);
    }
}



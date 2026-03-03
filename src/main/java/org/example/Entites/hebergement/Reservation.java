package org.example.Entites.hebergement;

import org.example.Entites.user.User;
import java.util.Objects;

public class Reservation {

    private int id_reservation;
    private Hebergement hebergement;
    private User user;
    private String dateDebutR;
    private String dateFinR;
    private String statutR;

    public Reservation() {
    }

    public Reservation(int id_reservation, Hebergement hebergement, User user,
                       String dateDebutR, String dateFinR, String statutR) {
        this.id_reservation = id_reservation;
        this.hebergement = hebergement;
        this.user = user;
        this.dateDebutR = dateDebutR;
        this.dateFinR = dateFinR;
        this.statutR = statutR;
    }

    public Reservation(Hebergement hebergement, User user,
                       String dateDebutR, String dateFinR, String statutR) {
        this.hebergement = hebergement;
        this.user = user;
        this.dateDebutR = dateDebutR;
        this.dateFinR = dateFinR;
        this.statutR = statutR;
    }

    public int getId_reservation() {
        return id_reservation;
    }

    public void setId_reservation(int id_reservation) {
        this.id_reservation = id_reservation;
    }

    public Hebergement getHebergement() {
        return hebergement;
    }

    public void setHebergement(Hebergement hebergement) {
        this.hebergement = hebergement;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getDateDebutR() {
        return dateDebutR;
    }

    public void setDateDebutR(String dateDebutR) {
        this.dateDebutR = dateDebutR;
    }

    public String getDateFinR() {
        return dateFinR;
    }

    public void setDateFinR(String dateFinR) {
        this.dateFinR = dateFinR;
    }

    public String getStatutR() {
        return statutR;
    }

    public void setStatutR(String statutR) {
        this.statutR = statutR;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "id_reservation=" + id_reservation +
                ", hebergement=" + hebergement +
                ", user=" + user +
                ", dateDebutR='" + dateDebutR + '\'' +
                ", dateFinR='" + dateFinR + '\'' +
                ", statutR='" + statutR + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        return id_reservation == that.id_reservation &&
                Objects.equals(hebergement, that.hebergement) &&
                Objects.equals(user, that.user) &&
                Objects.equals(dateDebutR, that.dateDebutR) &&
                Objects.equals(dateFinR, that.dateFinR) &&
                Objects.equals(statutR, that.statutR);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id_reservation, hebergement, user, dateDebutR, dateFinR, statutR);
    }
}
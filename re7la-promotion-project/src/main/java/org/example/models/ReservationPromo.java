package org.example.models;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

public class ReservationPromo {
    private int id;
    private int userId;
    private int promotionId;

    private Date dateDebutReservation;
    private Date dateFinReservation;
    private int nbJours;

    private float prixParJour;
    private float prixOriginal;
    private float reductionAppliquee;
    private float montantTotal;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructeur vide
    public ReservationPromo() {
    }

    // Constructeur pour création (sans ID)
    public ReservationPromo(int userId, int promotionId, Date dateDebutReservation,
                            Date dateFinReservation, int nbJours, float prixParJour,
                            float prixOriginal, float reductionAppliquee, float montantTotal) {
        this.userId = userId;
        this.promotionId = promotionId;
        this.dateDebutReservation = dateDebutReservation;
        this.dateFinReservation = dateFinReservation;
        this.nbJours = nbJours;
        this.prixParJour = prixParJour;
        this.prixOriginal = prixOriginal;
        this.reductionAppliquee = reductionAppliquee;
        this.montantTotal = montantTotal;
    }

    // Constructeur complet (pour lecture BD)
    public ReservationPromo(int id, int userId, int promotionId, Date dateDebutReservation,
                            Date dateFinReservation, int nbJours, float prixParJour,
                            float prixOriginal, float reductionAppliquee, float montantTotal,
                            Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.userId = userId;
        this.promotionId = promotionId;
        this.dateDebutReservation = dateDebutReservation;
        this.dateFinReservation = dateFinReservation;
        this.nbJours = nbJours;
        this.prixParJour = prixParJour;
        this.prixOriginal = prixOriginal;
        this.reductionAppliquee = reductionAppliquee;
        this.montantTotal = montantTotal;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(int promotionId) {
        this.promotionId = promotionId;
    }

    public Date getDateDebutReservation() {
        return dateDebutReservation;
    }

    public void setDateDebutReservation(Date dateDebutReservation) {
        this.dateDebutReservation = dateDebutReservation;
    }

    public Date getDateFinReservation() {
        return dateFinReservation;
    }

    public void setDateFinReservation(Date dateFinReservation) {
        this.dateFinReservation = dateFinReservation;
    }

    public int getNbJours() {
        return nbJours;
    }

    public void setNbJours(int nbJours) {
        this.nbJours = nbJours;
    }

    public float getPrixParJour() {
        return prixParJour;
    }

    public void setPrixParJour(float prixParJour) {
        this.prixParJour = prixParJour;
    }

    public float getPrixOriginal() {
        return prixOriginal;
    }

    public void setPrixOriginal(float prixOriginal) {
        this.prixOriginal = prixOriginal;
    }

    public float getReductionAppliquee() {
        return reductionAppliquee;
    }

    public void setReductionAppliquee(float reductionAppliquee) {
        this.reductionAppliquee = reductionAppliquee;
    }

    public float getMontantTotal() {
        return montantTotal;
    }

    public void setMontantTotal(float montantTotal) {
        this.montantTotal = montantTotal;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReservationPromo that = (ReservationPromo) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ReservationPromo{" +
                "id=" + id +
                ", userId=" + userId +
                ", promotionId=" + promotionId +
                ", dateDebutReservation=" + dateDebutReservation +
                ", dateFinReservation=" + dateFinReservation +
                ", nbJours=" + nbJours +
                ", montantTotal=" + montantTotal +
                '}';
    }
}

package org.example.Entites.promotion;

import org.example.Entites.user.User;
import java.sql.Date;
import java.sql.Timestamp;

public class ReservationPromo {
    private int id;
    private User user;              // ✅ Changé de int à User
    private Promotion promotion;
    private Date dateDebutReservation;
    private Date dateFinReservation;
    private int nbJours;
    private float prixParJour;
    private float prixOriginal;
    private float reductionAppliquee;
    private float montantTotal;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public ReservationPromo() {}

    // Constructeur avec User
    public ReservationPromo(User user, Promotion promotion, Date dateDebutReservation,
                            Date dateFinReservation, int nbJours, float prixParJour,
                            float prixOriginal, float reductionAppliquee, float montantTotal) {
        this.user = user;
        this.promotion = promotion;
        this.dateDebutReservation = dateDebutReservation;
        this.dateFinReservation = dateFinReservation;
        this.nbJours = nbJours;
        this.prixParJour = prixParJour;
        this.prixOriginal = prixOriginal;
        this.reductionAppliquee = reductionAppliquee;
        this.montantTotal = montantTotal;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public User getUser() { return user; }              // ✅ Getter User
    public void setUser(User user) { this.user = user; } // ✅ Setter User

    public Promotion getPromotion() { return promotion; }
    public void setPromotion(Promotion promotion) { this.promotion = promotion; }

    // Méthodes pour compatibilité
    public int getUserId() { return user != null ? user.getId() : 0; }
    public int getPromotionId() { return promotion != null ? promotion.getId() : 0; }

    public Date getDateDebutReservation() { return dateDebutReservation; }
    public void setDateDebutReservation(Date dateDebutReservation) { this.dateDebutReservation = dateDebutReservation; }

    public Date getDateFinReservation() { return dateFinReservation; }
    public void setDateFinReservation(Date dateFinReservation) { this.dateFinReservation = dateFinReservation; }

    public int getNbJours() { return nbJours; }
    public void setNbJours(int nbJours) { this.nbJours = nbJours; }

    public float getPrixParJour() { return prixParJour; }
    public void setPrixParJour(float prixParJour) { this.prixParJour = prixParJour; }

    public float getPrixOriginal() { return prixOriginal; }
    public void setPrixOriginal(float prixOriginal) { this.prixOriginal = prixOriginal; }

    public float getReductionAppliquee() { return reductionAppliquee; }
    public void setReductionAppliquee(float reductionAppliquee) { this.reductionAppliquee = reductionAppliquee; }

    public float getMontantTotal() { return montantTotal; }
    public void setMontantTotal(float montantTotal) { this.montantTotal = montantTotal; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
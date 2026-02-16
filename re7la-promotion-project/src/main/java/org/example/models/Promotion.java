package org.example.models;

import java.sql.Date;
import java.util.Objects;

public class Promotion {
    private int id;
    private String name;
    private String description;
    private Float discountPercentage;
    private Float discountFixed;
    private Date startDate;
    private Date endDate;
    private boolean isPack;
    private Float prixParJour;  // ⭐ NOUVEAU

    // ========== CONSTRUCTEUR VIDE ==========
    public Promotion() {
    }

    // ========== CONSTRUCTEUR SANS ID (pour création) ==========
    public Promotion(String name, String description, Float discountPercentage,
                     Float discountFixed, Date startDate, Date endDate, boolean isPack, Float prixParJour) {
        this.name = name;
        this.description = description;
        this.discountPercentage = discountPercentage;
        this.discountFixed = discountFixed;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isPack = isPack;
        this.prixParJour = prixParJour;  // ⭐ NOUVEAU
    }

    // ========== CONSTRUCTEUR COMPLET (pour lecture BD) ==========
    public Promotion(int id, String name, String description, Float discountPercentage,
                     Float discountFixed, Date startDate, Date endDate, boolean isPack, Float prixParJour) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.discountPercentage = discountPercentage;
        this.discountFixed = discountFixed;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isPack = isPack;
        this.prixParJour = prixParJour;  // ⭐ NOUVEAU
    }

    // ========== GETTERS ET SETTERS ==========
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Float getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(Float discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public Float getDiscountFixed() {
        return discountFixed;
    }

    public void setDiscountFixed(Float discountFixed) {
        this.discountFixed = discountFixed;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public boolean isPack() {
        return isPack;
    }

    public void setPack(boolean pack) {
        isPack = pack;
    }

    // ⭐ NOUVEAUX GETTERS/SETTERS POUR PRIX PAR JOUR
    public Float getPrixParJour() {
        return prixParJour;
    }

    public void setPrixParJour(Float prixParJour) {
        this.prixParJour = prixParJour;
    }

    // ========== MÉTHODES UTILITAIRES ==========
    public boolean isActive() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate start = startDate.toLocalDate();
        java.time.LocalDate end = endDate.toLocalDate();

        return !today.isBefore(start) && !today.isAfter(end);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Promotion promotion = (Promotion) o;
        return id == promotion.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Promotion{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", discountPercentage=" + discountPercentage +
                ", discountFixed=" + discountFixed +
                ", prixParJour=" + prixParJour +
                ", isPack=" + isPack +
                '}';
    }
}
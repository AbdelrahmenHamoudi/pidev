package org.example.Entites.promotion;

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
    private boolean isLocked;
    private int nbVues;
    private int nbReservations;
    private boolean generatedByAi;

    public Promotion() {}

    public Promotion(int id, String name, String description, Float discountPercentage,
                     Float discountFixed, Date startDate, Date endDate,
                     boolean isPack, boolean isLocked, int nbVues, int nbReservations) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.discountPercentage = discountPercentage;
        this.discountFixed = discountFixed;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isPack = isPack;
        this.isLocked = isLocked;
        this.nbVues = nbVues;
        this.nbReservations = nbReservations;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Float getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(Float discountPercentage) { this.discountPercentage = discountPercentage; }

    public Float getDiscountFixed() { return discountFixed; }
    public void setDiscountFixed(Float discountFixed) { this.discountFixed = discountFixed; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public boolean isPack() { return isPack; }
    public void setPack(boolean pack) { isPack = pack; }

    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }

    public int getNbVues() { return nbVues; }
    public void setNbVues(int nbVues) { this.nbVues = nbVues; }

    public int getNbReservations() { return nbReservations; }
    public void setNbReservations(int nbReservations) { this.nbReservations = nbReservations; }

    public boolean isGeneratedByAi() { return generatedByAi; }
    public void setGeneratedByAi(boolean generatedByAi) { this.generatedByAi = generatedByAi; }

    public boolean isActive() {
        java.time.LocalDate today = java.time.LocalDate.now();
        return !today.isBefore(startDate.toLocalDate())
                && !today.isAfter(endDate.toLocalDate());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Promotion)) return false;
        Promotion promotion = (Promotion) o;
        return id == promotion.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
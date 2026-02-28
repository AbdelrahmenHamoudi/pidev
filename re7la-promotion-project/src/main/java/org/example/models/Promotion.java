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
    private Float prixParJour;
    private boolean isLocked;
    private int nbVues;
    private int nbReservations;

    public Promotion() {}

    public Promotion(String name, String description, Float discountPercentage,
                     Float discountFixed, Date startDate, Date endDate,
                     boolean isPack, Float prixParJour) {
        this.name = name; this.description = description;
        this.discountPercentage = discountPercentage; this.discountFixed = discountFixed;
        this.startDate = startDate; this.endDate = endDate;
        this.isPack = isPack; this.prixParJour = prixParJour;
    }

    public Promotion(int id, String name, String description, Float discountPercentage,
                     Float discountFixed, Date startDate, Date endDate,
                     boolean isPack, Float prixParJour, boolean isLocked,
                     int nbVues, int nbReservations) {
        this.id = id; this.name = name; this.description = description;
        this.discountPercentage = discountPercentage; this.discountFixed = discountFixed;
        this.startDate = startDate; this.endDate = endDate;
        this.isPack = isPack; this.prixParJour = prixParJour;
        this.isLocked = isLocked; this.nbVues = nbVues; this.nbReservations = nbReservations;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Float getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(Float v) { this.discountPercentage = v; }
    public Float getDiscountFixed() { return discountFixed; }
    public void setDiscountFixed(Float v) { this.discountFixed = v; }
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date d) { this.startDate = d; }
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date d) { this.endDate = d; }
    public boolean isPack() { return isPack; }
    public void setPack(boolean p) { this.isPack = p; }
    public Float getPrixParJour() { return prixParJour; }
    public void setPrixParJour(Float v) { this.prixParJour = v; }
    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean v) { this.isLocked = v; }
    public int getNbVues() { return nbVues; }
    public void setNbVues(int v) { this.nbVues = v; }
    public int getNbReservations() { return nbReservations; }
    public void setNbReservations(int v) { this.nbReservations = v; }

    public boolean isActive() {
        java.time.LocalDate today = java.time.LocalDate.now();
        return !today.isBefore(startDate.toLocalDate()) && !today.isAfter(endDate.toLocalDate());
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Promotion p)) return false;
        return id == p.id;
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return "Promotion{id=" + id + ", name='" + name + "'}"; }
}
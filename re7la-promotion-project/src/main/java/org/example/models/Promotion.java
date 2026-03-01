package org.example.models;

import java.sql.Date;
import java.util.Objects;

/**
 * ✅ FIXED: Removed prixParJour from Promotion.
 *
 * Why: prixParJour was a static hardcoded price that contradicts the requirement
 * that prices must come dynamically from hébergement/activité/voiture tables.
 *
 * Price is now ALWAYS fetched from the target offer at reservation time via PriceCalculatorService.
 * The reservation itself stores prix_original + montant_total (computed at booking time).
 */
public class Promotion {

    private int    id;
    private String name;
    private String description;
    private Float  discountPercentage; // nullable — use % OR fixed
    private Float  discountFixed;      // nullable — use % OR fixed
    private Date   startDate;
    private Date   endDate;
    private boolean isPack;
    private boolean isLocked;
    private int    nbVues;
    private int    nbReservations;

    public Promotion() {}

    /** Constructor for creation (no ID yet) */
    public Promotion(String name, String description, Float discountPercentage,
                     Float discountFixed, Date startDate, Date endDate,
                     boolean isPack) {
        this.name = name;
        this.description = description;
        this.discountPercentage = discountPercentage;
        this.discountFixed = discountFixed;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isPack = isPack;
    }

    /** Full constructor for DB read */
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

    // ── Getters & Setters ──
    public int     getId()                   { return id; }
    public void    setId(int id)             { this.id = id; }

    public String  getName()                 { return name; }
    public void    setName(String name)      { this.name = name; }

    public String  getDescription()          { return description; }
    public void    setDescription(String d)  { this.description = d; }

    public Float   getDiscountPercentage()   { return discountPercentage; }
    public void    setDiscountPercentage(Float v) { this.discountPercentage = v; }

    public Float   getDiscountFixed()        { return discountFixed; }
    public void    setDiscountFixed(Float v) { this.discountFixed = v; }

    public Date    getStartDate()            { return startDate; }
    public void    setStartDate(Date d)      { this.startDate = d; }

    public Date    getEndDate()              { return endDate; }
    public void    setEndDate(Date d)        { this.endDate = d; }

    public boolean isPack()                  { return isPack; }
    public void    setPack(boolean p)        { this.isPack = p; }

    public boolean isLocked()               { return isLocked; }
    public void    setLocked(boolean v)     { this.isLocked = v; }

    public int     getNbVues()              { return nbVues; }
    public void    setNbVues(int v)         { this.nbVues = v; }

    public int     getNbReservations()      { return nbReservations; }
    public void    setNbReservations(int v) { this.nbReservations = v; }

    /** Returns true if today is within [startDate, endDate] */
    public boolean isActive() {
        java.time.LocalDate today = java.time.LocalDate.now();
        return !today.isBefore(startDate.toLocalDate())
                && !today.isAfter(endDate.toLocalDate());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Promotion p)) return false;
        return id == p.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return "Promotion{id=" + id + ", name='" + name + "'}"; }
}
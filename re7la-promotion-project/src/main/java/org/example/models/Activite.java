package org.example.models;

/**
 * ✅ FIXED: Aligned with re7la_3a9 `activite` table.
 *
 * Changes:
 *  - id maps to `idActivite`
 *  - nom → nomA (matches `nomA` column in DB)
 *  - Added prixParPersonne (maps to `prixParPersonne`) ← KEY FIELD
 *  - Added capaciteMax (maps to `capaciteMax`)
 *  - description maps to `descriptionA`
 */
public class Activite {

    private int    id;              // idActivite
    private String nomA;            // nomA
    private String descriptionA;    // descriptionA
    private String lieu;            // lieu
    private float  prixParPersonne; // prixParPersonne ← KEY FIELD for price calculation
    private int    capaciteMax;     // capaciteMax

    public Activite() {}

    /** Constructor for display in ComboBox / TableView */
    public Activite(int id, String nomA, String lieu, float prixParPersonne) {
        this.id = id;
        this.nomA = nomA;
        this.lieu = lieu;
        this.prixParPersonne = prixParPersonne;
    }

    /** Full constructor for DB read */
    public Activite(int id, String nomA, String descriptionA, String lieu,
                    float prixParPersonne, int capaciteMax) {
        this.id = id;
        this.nomA = nomA;
        this.descriptionA = descriptionA;
        this.lieu = lieu;
        this.prixParPersonne = prixParPersonne;
        this.capaciteMax = capaciteMax;
    }

    // ── Getters & Setters ──
    public int    getId()               { return id; }
    public void   setId(int id)         { this.id = id; }

    public String getNomA()             { return nomA; }
    public void   setNomA(String n)     { this.nomA = n; }

    /** Alias: getNom() for backward compat with TableView PropertyValueFactory */
    public String getNom()              { return nomA; }

    public String getDescriptionA()     { return descriptionA; }
    public void   setDescriptionA(String d) { this.descriptionA = d; }

    public String getLieu()             { return lieu; }
    public void   setLieu(String l)     { this.lieu = l; }

    /** Alias: getType() / getDetails() for display */
    public String getType()             { return lieu; }

    public float  getPrixParPersonne()  { return prixParPersonne; }
    public void   setPrixParPersonne(float p) { this.prixParPersonne = p; }

    public int    getCapaciteMax()      { return capaciteMax; }
    public void   setCapaciteMax(int c) { this.capaciteMax = c; }

    @Override
    public String toString() {
        return nomA + " — " + lieu + " (" + String.format("%.0f TND/personne", prixParPersonne) + ")";
    }
}
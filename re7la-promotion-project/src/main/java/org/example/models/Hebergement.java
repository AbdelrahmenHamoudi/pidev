package org.example.models;

/**
 * ✅ FIXED: Aligned with re7la_3a9 `hebergement` table.
 *
 * Changes:
 *  - id field maps to `id_hebergement`
 *  - nom → titre (matches `titre` column in DB)
 *  - Added prixParNuit (maps to `prixParNuit` column)
 *  - Added typeHebergement (maps to `type_hebergement`)
 *  - Added disponible (maps to `disponible_heberg`)
 */
public class Hebergement {

    private int    id;               // id_hebergement
    private String titre;            // titre
    private String descHebergement;  // desc_hebergement
    private int    capacite;
    private String typeHebergement;  // type_hebergement
    private boolean disponible;      // disponible_heberg
    private float  prixParNuit;      // prixParNuit ← KEY FIELD for price calculation

    public Hebergement() {}

    /** Constructor for display in ComboBox / TableView */
    public Hebergement(int id, String titre, String typeHebergement, float prixParNuit) {
        this.id = id;
        this.titre = titre;
        this.typeHebergement = typeHebergement;
        this.prixParNuit = prixParNuit;
        this.disponible = true;
    }

    /** Full constructor for DB read */
    public Hebergement(int id, String titre, String descHebergement, int capacite,
                       String typeHebergement, boolean disponible, float prixParNuit) {
        this.id = id;
        this.titre = titre;
        this.descHebergement = descHebergement;
        this.capacite = capacite;
        this.typeHebergement = typeHebergement;
        this.disponible = disponible;
        this.prixParNuit = prixParNuit;
    }

    // ── Getters & Setters ──
    public int     getId()               { return id; }
    public void    setId(int id)         { this.id = id; }

    public String  getTitre()            { return titre; }
    public void    setTitre(String t)    { this.titre = t; }

    /** Alias for backward compatibility with any controller calling getNom() */
    public String  getNom()              { return titre; }

    public String  getDescHebergement()  { return descHebergement; }
    public void    setDescHebergement(String d) { this.descHebergement = d; }

    public int     getCapacite()         { return capacite; }
    public void    setCapacite(int c)    { this.capacite = c; }

    public String  getTypeHebergement()  { return typeHebergement; }
    public void    setTypeHebergement(String t) { this.typeHebergement = t; }

    /** Alias: getType() for backward compat */
    public String  getType()             { return typeHebergement; }

    public boolean isDisponible()        { return disponible; }
    public void    setDisponible(boolean d) { this.disponible = d; }

    public float   getPrixParNuit()      { return prixParNuit; }
    public void    setPrixParNuit(float p) { this.prixParNuit = p; }

    @Override
    public String toString() {
        return titre + " (" + typeHebergement + ") — " + String.format("%.0f TND/nuit", prixParNuit);
    }
}
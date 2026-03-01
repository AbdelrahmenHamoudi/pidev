package org.example.models;

/**
 * ✅ NEW: Replaces Transport.java entirely.
 *
 * Now aligned with re7la_3a9 `voiture` table:
 *   id_voiture, marque, modele, immatriculation, prix_KM, avec_chauffeur, disponibilite, nb_places
 *
 * Pricing logic: prixTotal = prix_KM × distance_km
 * Distance comes from user input (point_depart → point_arrivee).
 */
public class Voiture {

    private int     id;             // id_voiture
    private String  marque;         // marque
    private String  modele;         // modele
    private String  immatriculation;
    private float   prixKm;         // prix_KM ← KEY FIELD for price calculation
    private boolean avecChauffeur;  // avec_chauffeur
    private boolean disponibilite;  // disponibilite
    private String  description;
    private int     nbPlaces;       // nb_places

    public Voiture() {}

    /** Constructor for display in ComboBox / TableView */
    public Voiture(int id, String marque, String modele, float prixKm, boolean disponibilite) {
        this.id = id;
        this.marque = marque;
        this.modele = modele;
        this.prixKm = prixKm;
        this.disponibilite = disponibilite;
    }

    /** Full constructor for DB read */
    public Voiture(int id, String marque, String modele, String immatriculation,
                   float prixKm, boolean avecChauffeur, boolean disponibilite,
                   String description, int nbPlaces) {
        this.id = id;
        this.marque = marque;
        this.modele = modele;
        this.immatriculation = immatriculation;
        this.prixKm = prixKm;
        this.avecChauffeur = avecChauffeur;
        this.disponibilite = disponibilite;
        this.description = description;
        this.nbPlaces = nbPlaces;
    }

    // ── Getters & Setters ──
    public int     getId()               { return id; }
    public void    setId(int id)         { this.id = id; }

    public String  getMarque()           { return marque; }
    public void    setMarque(String m)   { this.marque = m; }

    public String  getModele()           { return modele; }
    public void    setModele(String m)   { this.modele = m; }

    /** getNom() alias for TableView PropertyValueFactory compatibility */
    public String  getNom()              { return marque + " " + modele; }

    public String  getImmatriculation()  { return immatriculation; }
    public void    setImmatriculation(String i) { this.immatriculation = i; }

    public float   getPrixKm()           { return prixKm; }
    public void    setPrixKm(float p)    { this.prixKm = p; }

    public boolean isAvecChauffeur()     { return avecChauffeur; }
    public void    setAvecChauffeur(boolean a) { this.avecChauffeur = a; }

    public boolean isDisponibilite()     { return disponibilite; }
    public void    setDisponibilite(boolean d) { this.disponibilite = d; }

    public String  getDescription()      { return description; }
    public void    setDescription(String d) { this.description = d; }

    public int     getNbPlaces()         { return nbPlaces; }
    public void    setNbPlaces(int n)    { this.nbPlaces = n; }

    /** getDetails() for TableView display column */
    public String  getDetails()          { return String.format("%.0f TND/km · %d places", prixKm, nbPlaces); }

    @Override
    public String toString() {
        return marque + " " + modele + " — " + String.format("%.2f TND/km", prixKm)
                + (avecChauffeur ? " (avec chauffeur)" : "");
    }
}
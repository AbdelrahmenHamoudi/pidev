package org.example.services;

import org.example.models.Activite;
import org.example.models.Hebergement;
import org.example.models.Voiture;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ FIXED: Updated constructors to match new model signatures.
 *
 * Hebergement(int id, String titre, String typeHebergement, float prixParNuit)
 * Activite(int id, String nomA, String lieu, float prixParPersonne)
 * Voiture(int id, String marque, String modele, float prixKm, boolean disponibilite)
 *
 * NOTE: This class is kept as fallback / test data only.
 * In production, use OffresLiveService which reads from re7la_3a9 DB.
 */
public class OffresStaticData {

    private static OffresStaticData instance;
    private List<Hebergement> hebergements;
    private List<Activite>    activites;
    private List<Voiture>     transports;

    private OffresStaticData() {
        initHebergements();
        initActivites();
        initTransports();
    }

    public static OffresStaticData getInstance() {
        if (instance == null) {
            instance = new OffresStaticData();
        }
        return instance;
    }

    // ── Hebergement(id, titre, typeHebergement, prixParNuit) ────────
    private void initHebergements() {
        hebergements = new ArrayList<>();
        hebergements.add(new Hebergement(1, "Hôtel Marina",           "Hôtel 4 étoiles",  120f));
        hebergements.add(new Hebergement(2, "Dar Zarrouk",            "Maison d'hôte",     80f));
        hebergements.add(new Hebergement(3, "Golden Tulip Resort",    "Resort",           200f));
        hebergements.add(new Hebergement(4, "Villa Palm Beach",       "Villa",            350f));
        hebergements.add(new Hebergement(5, "Résidence El Kantaoui",  "Résidence",         90f));
    }

    // ── Activite(id, nomA, lieu, prixParPersonne) ───────────────────
    private void initActivites() {
        activites = new ArrayList<>();
        activites.add(new Activite(1, "Plongée sous-marine", "Tabarka",    60f));
        activites.add(new Activite(2, "Quad dans le Sahara", "Douz",       85f));
        activites.add(new Activite(3, "Visite Carthage",     "Tunis",      30f));
        activites.add(new Activite(4, "Parapente",           "Hammamet",  120f));
        activites.add(new Activite(5, "Balade à Cheval",     "Ain Draham", 45f));
    }

    // ── Voiture(id, marque, modele, prixKm, disponibilite) ─────────
    private void initTransports() {
        transports = new ArrayList<>();
        transports.add(new Voiture(1, "Mercedes", "Sprinter",  2.5f, true));
        transports.add(new Voiture(2, "Renault",  "Clio",      1.8f, true));
        transports.add(new Voiture(3, "Toyota",   "Corolla",   2.0f, true));
        transports.add(new Voiture(4, "Ford",     "Transit",   3.0f, true));
        transports.add(new Voiture(5, "Toyota",   "Land Cruiser", 4.0f, true));
    }

    public List<Hebergement> getAllHebergements() { return new ArrayList<>(hebergements); }
    public List<Activite>    getAllActivites()    { return new ArrayList<>(activites); }
    public List<Voiture>     getAllTransports()   { return new ArrayList<>(transports); }

    public Hebergement getHebergementById(int id) {
        return hebergements.stream().filter(h -> h.getId() == id).findFirst().orElse(null);
    }

    public Activite getActiviteById(int id) {
        return activites.stream().filter(a -> a.getId() == id).findFirst().orElse(null);
    }

    public Voiture getTransportById(int id) {
        return transports.stream().filter(t -> t.getId() == id).findFirst().orElse(null);
    }
}
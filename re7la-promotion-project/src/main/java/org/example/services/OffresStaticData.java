package org.example.services;

import org.example.models.Activite;
import org.example.models.Hebergement;
import org.example.models.Transport;
import java.util.ArrayList;
import java.util.List;

public class OffresStaticData {
    
    private static OffresStaticData instance;
    private List<Hebergement> hebergements;
    private List<Activite> activites;
    private List<Transport> transports;

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

    private void initHebergements() {
        hebergements = new ArrayList<>();
        hebergements.add(new Hebergement(1, "Hôtel Marina", "Sousse", "Hôtel 4 étoiles"));
        hebergements.add(new Hebergement(2, "Dar Zarrouk", "Sidi Bou Said", "Maison d'hôte"));
        hebergements.add(new Hebergement(3, "Golden Tulip Resort", "Hammamet", "Resort"));
        hebergements.add(new Hebergement(4, "Villa Palm Beach", "Djerba", "Villa"));
        hebergements.add(new Hebergement(5, "Résidence El Kantaoui", "Port El Kantaoui", "Résidence"));
    }

    private void initActivites() {
        activites = new ArrayList<>();
        activites.add(new Activite(1, "Plongée sous-marine", "Tabarka", "Sport"));
        activites.add(new Activite(2, "Quad dans le Sahara", "Douz", "Aventure"));
        activites.add(new Activite(3, "Visite Carthage", "Tunis", "Culture"));
        activites.add(new Activite(4, "Parapente", "Hammamet", "Sport"));
        activites.add(new Activite(5, "Balade à Cheval", "Ain Draham", "Excursion"));
    }

    private void initTransports() {
        transports = new ArrayList<>();
        transports.add(new Transport(1, "Bus Confort", "Bus", "Tunis - Tozeur"));
        transports.add(new Transport(2, "Location Voiture Compacte", "Voiture", "Aéroport Tunis"));
        transports.add(new Transport(3, "Taxi Aéroport", "Taxi", "Carthage - Centre-ville"));
        transports.add(new Transport(4, "Minibus Groupe", "Minibus", "Sur mesure"));
        transports.add(new Transport(5, "Location 4x4", "4x4", "Douz - Sahara"));
    }

    public List<Hebergement> getAllHebergements() {
        return new ArrayList<>(hebergements);
    }

    public List<Activite> getAllActivites() {
        return new ArrayList<>(activites);
    }

    public List<Transport> getAllTransports() {
        return new ArrayList<>(transports);
    }

    public Hebergement getHebergementById(int id) {
        return hebergements.stream()
                .filter(h -> h.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public Activite getActiviteById(int id) {
        return activites.stream()
                .filter(a -> a.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public Transport getTransportById(int id) {
        return transports.stream()
                .filter(t -> t.getId() == id)
                .findFirst()
                .orElse(null);
    }
}

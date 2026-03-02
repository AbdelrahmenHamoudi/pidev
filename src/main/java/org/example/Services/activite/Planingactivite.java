package org.example.Services.activite;

import org.example.Entites.activite.Planning;
import java.util.List;

public interface Planingactivite {
    // CRUD
    List<Planning> getAllPlannings();
    Planning getPlanningById(int id);
    boolean addPlanning(Planning planning);
    boolean updatePlanning(Planning planning);
    boolean deletePlanning(int id);

    // Recherche par activité
    List<Planning> getPlanningsByActivite(int id_activite);
    List<Planning> getAvailablePlannings(int id_activite);

    // Gestion des places
    boolean reserverPlace(int idPlanning);
    boolean libererPlace(int idPlanning);

    // Statistiques
    int countPlannings();
    int countPlanningsByActivite(int id_activite);
}
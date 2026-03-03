package org.example.Services.activite;

import org.example.Entites.activite.Activite;

import java.util.List;

public interface ActiviteService {
    // READ ALL
    List<Activite> getAllActivites();

    // READ BY ID
    Activite getActiviteById(int id);

    // CREATE
    boolean addActivite(Activite activite);

    // UPDATE
    boolean updateActivite(Activite activite);

    // DELETE
    boolean deleteActivite(int id);

    // SEARCH
    List<Activite> searchActivites(String keyword);

    // FILTER BY TYPE
    List<Activite> getActivitesByType(String type);

    // COUNT
    int countActivites();
}

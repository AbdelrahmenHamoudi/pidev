package org.example.utils;

/**
 * Gestionnaire de session simplifié
 * User hardcodé pour validation (pas de login complexe)
 */
public class SessionManager {

    // User "connecté" par défaut (John Doe, id=1)
    private static final int CURRENT_USER_ID = 1;
    private static final String CURRENT_USER_NAME = "John Doe";
    private static final String CURRENT_USER_EMAIL = "john@test.com";

    /**
     * Obtenir l'ID du user connecté
     */
    public static int getCurrentUserId() {
        return CURRENT_USER_ID;
    }

    /**
     * Obtenir le nom complet du user connecté
     */
    public static String getCurrentUserName() {
        return CURRENT_USER_NAME;
    }

    /**
     * Obtenir l'email du user connecté
     */
    public static String getCurrentUserEmail() {
        return CURRENT_USER_EMAIL;
    }
}

package org.example.Utils;

import org.example.Services.user.APIservices.JWTService;
import org.example.Entites.user.User;

public class UserSession {

    private static UserSession instance;
    private User currentUser;
    private String token;
    private long loginTime;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    /**
     * Définit l'utilisateur courant et génère un token JWT
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.loginTime = System.currentTimeMillis();

        // Générer le token JWT
        if (user != null) {
            this.token = JWTService.generateToken(user);
            System.out.println("✅ Token JWT généré pour: " + user.getE_mail());
        }
    }


    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Définit uniquement le token (pour restauration)
     */
    public void setToken(String token) {
        this.token = token;
        if (token != null) {
            this.currentUser = JWTService.extractUserFromToken(token);
            this.loginTime = System.currentTimeMillis();
        }
    }

    /**
     * Récupère le token JWT
     */
    public String getToken() {
        return token;
    }

    /**
     * Vérifie si le token est valide
     */
    public boolean isTokenValid() {
        return token != null && !JWTService.isTokenExpired(token);
    }

    /**
     * Rafraîchit le token
     */
    public String refreshToken() {
        if (token != null) {
            String newToken = JWTService.refreshToken(token);
            if (newToken != null) {
                this.token = newToken;
                this.currentUser = JWTService.extractUserFromToken(newToken);
            }
            return newToken;
        }
        return null;
    }

    /**
     * Efface la session
     */
    public void clearSession() {
        this.currentUser = null;
        this.token = null;
        this.loginTime = 0;
        System.out.println("✅ Session effacée");
    }

    /**
     * Vérifie si l'utilisateur est connecté
     */
    public boolean isLoggedIn() {
        return currentUser != null && isTokenValid();
    }
}
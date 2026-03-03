package org.example.Utils;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Gestionnaire de langue pour REHLA Travel.
 * Singleton permettant de basculer entre Français et Anglais.
 *
 * Usage:
 *   LanguageManager.getInstance().setLocale(Locale.ENGLISH);
 *   String text = LanguageManager.getInstance().get("welcome.title");
 */
public class LanguageManager {

    private static LanguageManager instance;

    // Locales supportées
    public static final Locale FRENCH  = Locale.FRENCH;
    public static final Locale ENGLISH = Locale.ENGLISH;

    private Locale currentLocale;
    private ResourceBundle bundle;

    // Chemin vers les fichiers properties dans /resources
    private static final String BUNDLE_BASE = "i18n/messages";

    private LanguageManager() {
        // Langue par défaut Français
        currentLocale = FRENCH;
        loadBundle();
    }

    public static LanguageManager getInstance() {
        if (instance == null) {
            instance = new LanguageManager();
        }
        return instance;
    }

    /**
     * Change la langue active et recharge les traductions.
     * @param locale Locale.FRENCH ou Locale.ENGLISH
     */
    public void setLocale(Locale locale) {
        this.currentLocale = locale;
        loadBundle();
    }

    /**
     * Bascule entre FR et EN.
     */
    public void toggleLocale() {
        if (currentLocale.equals(FRENCH)) {
            setLocale(ENGLISH);
        } else {
            setLocale(FRENCH);
        }
    }

    /**
     * Retourne la valeur traduite pour une clé donnée.
     * @param key Clé de traduction (ex: "welcome.title")
     * @return Texte traduit, ou "??key??" si introuvable
     */
    public String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return "??" + key + "??";
        }
    }

    /**
     * Retourne la locale active.
     */
    public Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * Retourne true si la langue active est le Français.
     */
    public boolean isFrench() {
        return currentLocale.equals(FRENCH);
    }

    /**
     * Retourne le label du bouton de bascule (affiche la langue CIBLE).
     * Ex: si FR actif → affiche "🇬🇧 EN"
     */
    public String getToggleButtonLabel() {
        return isFrench() ? "🇬🇧 EN" : "🇫🇷 FR";
    }

    private void loadBundle() {
        try {
            bundle = ResourceBundle.getBundle(BUNDLE_BASE, currentLocale);
        } catch (Exception e) {
            System.err.println("[LanguageManager] Fichier de langue introuvable pour: " + currentLocale);
            // Fallback sur le français
            try {
                bundle = ResourceBundle.getBundle(BUNDLE_BASE, FRENCH);
            } catch (Exception ex) {
                System.err.println("[LanguageManager] Aucun fichier de langue disponible.");
            }
        }
    }
}
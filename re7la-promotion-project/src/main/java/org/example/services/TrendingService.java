package org.example.services;

import org.example.models.Promotion;

import java.util.Comparator;
import java.util.List;

/**
 * TrendingService — Calcule dynamiquement si une promo est "Tendance".
 *
 * Conditions métier :
 *   isTrending = (nb_vues > 30) AND (nb_reservations > 10)
 *   trendingScore = (nb_vues × 0.4) + (nb_reservations × 0.6)
 *
 * Pas de colonne DB — tout est calculé à la volée.
 */
public class TrendingService {

    // ═══ Seuils métier ═══
    public static final int SEUIL_VUES         = 30;
    public static final int SEUIL_RESERVATIONS = 10;

    // ═══ Poids score ═══
    public static final double POIDS_VUES  = 0.4;
    public static final double POIDS_RESA  = 0.6;

    private static TrendingService instance;
    public static TrendingService getInstance() {
        if (instance == null) instance = new TrendingService();
        return instance;
    }

    // ════════════════════════════════════════════════════
    // EST TRENDING ?
    // ════════════════════════════════════════════════════

    public boolean isTrending(Promotion p) {
        return p.getNbVues() > SEUIL_VUES && p.getNbReservations() > SEUIL_RESERVATIONS;
    }

    // ════════════════════════════════════════════════════
    // SCORE DE POPULARITÉ
    // ════════════════════════════════════════════════════

    public double getTrendingScore(Promotion p) {
        return (p.getNbVues() * POIDS_VUES) + (p.getNbReservations() * POIDS_RESA);
    }

    /**
     * Score normalisé entre 0.0 et 1.0 pour la barre de popularité.
     * Basé sur le score max de la liste.
     */
    public double getNormalizedScore(Promotion p, List<Promotion> allPromos) {
        double max = allPromos.stream()
                .mapToDouble(this::getTrendingScore)
                .max()
                .orElse(1.0);
        if (max == 0) return 0.0;
        return Math.min(1.0, getTrendingScore(p) / max);
    }

    // ════════════════════════════════════════════════════
    // TRIER — trending d'abord, puis par score DESC
    // ════════════════════════════════════════════════════

    /**
     * Trie la liste : trending en premier, puis par score décroissant.
     * ORDER BY isTrending DESC, trendingScore DESC
     */
    public List<Promotion> sortWithTrendingFirst(List<Promotion> promos) {
        return promos.stream()
                .sorted(Comparator
                        .comparingInt((Promotion p) -> isTrending(p) ? 0 : 1)
                        .thenComparingDouble(p -> -getTrendingScore(p)))
                .toList();
    }

    // ════════════════════════════════════════════════════
    // KPI — % de promos tendance
    // ════════════════════════════════════════════════════

    public int countTrending(List<Promotion> promos) {
        return (int) promos.stream().filter(this::isTrending).count();
    }

    public double percentTrending(List<Promotion> promos) {
        if (promos.isEmpty()) return 0.0;
        return (countTrending(promos) * 100.0) / promos.size();
    }

    // ════════════════════════════════════════════════════
    // LABEL SCORE — pour affichage UI
    // ════════════════════════════════════════════════════

    public String getScoreLabel(Promotion p) {
        double score = getTrendingScore(p);
        if (score >= 50)  return "🔥🔥 Viral";
        if (score >= 25)  return "🔥 Tendance";
        if (score >= 10)  return "📈 Montant";
        if (score >= 3)   return "👀 Remarqué";
        return "💤 Dormant";
    }

    /**
     * Couleur CSS du label selon score.
     */
    public String getScoreColor(Promotion p) {
        double score = getTrendingScore(p);
        if (score >= 50)  return "#E53E3E";  // rouge vif
        if (score >= 25)  return "#F39C12";  // orange
        if (score >= 10)  return "#1ABC9C";  // turquoise
        if (score >= 3)   return "#3498DB";  // bleu
        return "#94A3B8";                     // gris
    }
}
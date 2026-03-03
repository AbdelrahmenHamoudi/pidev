package org.example.Services.promotion;

import org.example.Entites.promotion.Promotion;        // ✅
import org.example.Utils.MyBD;                         // ✅
import org.example.Services.promotion.NotificationService; // ✅ (déjà correct)

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * SmartDiscountEngine — Recalcule automatiquement les réductions.
 *
 * Appelé EN TEMPS RÉEL après chaque réservation (via ReservationService).
 *
 * Logique :
 *   1. Calcule la moyenne des réservations de toutes les promos actives
 *   2. Compare chaque promo à cette moyenne
 *   3. Ajuste la réduction :
 *      - Peu de réservations (< moy * 0.5) → boost discount
 *      - Trop de réservations (> moy * 1.5) → réduit discount
 *      - Expire bientôt (< 3 jours) → boost urgent
 *   4. UPDATE discount_percentage en base
 *   5. Min 5% / Max 50%
 */
public class SmartDiscountEngine {

    private static SmartDiscountEngine instance;
    public static SmartDiscountEngine getInstance() {
        if (instance == null) instance = new SmartDiscountEngine();
        return instance;
    }

    // ═══ Limites ═══
    private static final float MIN_DISCOUNT = 5f;
    private static final float MAX_DISCOUNT = 50f;

    // ═══ Seuils relatifs à la moyenne ═══
    private static final double SEUIL_BAS  = 0.5;   // < 50% de la moyenne → boost
    private static final double SEUIL_HAUT = 1.5;   // > 150% de la moyenne → réduire

    // ═══ Ajustements ═══
    private static final float BOOST_FAIBLE_RESA  = 5f;   // +5% si peu de réserv.
    private static final float BOOST_EXPIRE_BIENT = 8f;   // +8% si expire dans < 3j
    private static final float REDUCE_FORTE_RESA  = 5f;   // -5% si trop de réserv.

    // ════════════════════════════════════════════════════
    // POINT D'ENTRÉE — appelé après chaque réservation
    // ════════════════════════════════════════════════════

    /**
     * Recalcule et met à jour la réduction de TOUTES les promos actives.
     * Appelé en temps réel depuis ReservationService.
     */
    public void recalculateAll() {
        List<Promotion> promos = new PromotionService().getAll();
        List<Promotion> actives = promos.stream()
                .filter(Promotion::isActive)
                .filter(p -> p.getDiscountPercentage() != null)
                .toList();

        if (actives.isEmpty()) return;

        // Calcule la moyenne des réservations
        double avgReservations = actives.stream()
                .mapToInt(Promotion::getNbReservations)
                .average()
                .orElse(0.0);

        System.out.println("[SmartDiscount] 🧠 Recalcul · Moyenne réserv.: " + String.format("%.1f", avgReservations));

        for (Promotion p : actives) {
            float newDiscount = calculateNewDiscount(p, avgReservations);
            if (newDiscount != p.getDiscountPercentage()) {
                updateDiscountInDB(p.getId(), newDiscount);
                System.out.printf("[SmartDiscount] 💰 %-25s : %.0f%% → %.0f%%%n",
                        p.getName(), p.getDiscountPercentage(), newDiscount);
            }
        }
    }

    /**
     * Recalcule uniquement une promo spécifique (plus rapide).
     */
    public void recalculateForPromo(int promoId) {
        PromotionService svc = new PromotionService();
        List<Promotion> all = svc.getAll();

        Promotion target = all.stream()
                .filter(p -> p.getId() == promoId)
                .findFirst().orElse(null);
        if (target == null || target.getDiscountPercentage() == null) return;

        double avg = all.stream()
                .filter(Promotion::isActive)
                .filter(p -> p.getDiscountPercentage() != null)
                .mapToInt(Promotion::getNbReservations)
                .average().orElse(0.0);

        float newDiscount = calculateNewDiscount(target, avg);
        if (newDiscount != target.getDiscountPercentage()) {
            updateDiscountInDB(promoId, newDiscount);
            System.out.printf("[SmartDiscount] 💰 %s : %.0f%% → %.0f%%%n",
                    target.getName(), target.getDiscountPercentage(), newDiscount);

            // Notifier UNIQUEMENT l'admin (BackOffice)
            NotificationService.getInstance().info(
                    "Smart Discount 🧠",
                    String.format("Réduction ajustée : \"%s\" → %.0f%%", target.getName(), newDiscount)
            );
        }
    }

    /**
     * Recalcule pour une promo en SILENCE — sans aucune notification.
     * Appelé depuis le FrontOffice après réservation (l'user ne doit rien voir).
     */
    public void recalculateForPromoSilent(int promoId) {
        PromotionService svc = new PromotionService();
        List<Promotion> all  = svc.getAll();

        Promotion target = all.stream()
                .filter(p -> p.getId() == promoId)
                .findFirst().orElse(null);
        if (target == null || target.getDiscountPercentage() == null) return;

        double avg = all.stream()
                .filter(Promotion::isActive)
                .filter(p -> p.getDiscountPercentage() != null)
                .mapToInt(Promotion::getNbReservations)
                .average().orElse(0.0);

        float newDiscount = calculateNewDiscount(target, avg);
        if (newDiscount != target.getDiscountPercentage()) {
            updateDiscountInDB(promoId, newDiscount);
            System.out.printf("[SmartDiscount][silent] %s : %.0f%% → %.0f%%%n",
                    target.getName(), target.getDiscountPercentage(), newDiscount);
        }
    }

    // ════════════════════════════════════════════════════
    // CALCUL DE LA NOUVELLE RÉDUCTION
    // ════════════════════════════════════════════════════

    private float calculateNewDiscount(Promotion p, double avgReservations) {
        float current = p.getDiscountPercentage();
        float adjusted = current;

        int resa = p.getNbReservations();
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), p.getEndDate().toLocalDate());

        // Règle 1 — Peu de réservations par rapport à la moyenne → boost
        if (avgReservations > 0 && resa < avgReservations * SEUIL_BAS) {
            adjusted += BOOST_FAIBLE_RESA;
            System.out.printf("[SmartDiscount]   → %s : peu de réserv. (%d < %.0f) +%.0f%%%n",
                    p.getName(), resa, avgReservations * SEUIL_BAS, BOOST_FAIBLE_RESA);
        }

        // Règle 2 — Trop de réservations par rapport à la moyenne → réduire
        if (avgReservations > 0 && resa > avgReservations * SEUIL_HAUT) {
            adjusted -= REDUCE_FORTE_RESA;
            System.out.printf("[SmartDiscount]   → %s : trop de réserv. (%d > %.0f) -%.0f%%%n",
                    p.getName(), resa, avgReservations * SEUIL_HAUT, REDUCE_FORTE_RESA);
        }

        // Règle 3 — Expire bientôt (< 3 jours) → urgence boost
        if (daysLeft >= 0 && daysLeft < 3) {
            adjusted += BOOST_EXPIRE_BIENT;
            System.out.printf("[SmartDiscount]   → %s : expire dans %d jour(s) +%.0f%%%n",
                    p.getName(), daysLeft, BOOST_EXPIRE_BIENT);
        }

        // Règle 4 — Saisonnalité simple : weekend → petit boost
        // (optionnel, peut être étendu)
        // LocalDate today = LocalDate.now();
        // if (today.getDayOfWeek() == DayOfWeek.FRIDAY || today.getDayOfWeek() == DayOfWeek.SATURDAY) {
        //     adjusted += 2f;
        // }

        // Appliquer les limites min/max
        return Math.max(MIN_DISCOUNT, Math.min(MAX_DISCOUNT, adjusted));
    }

    // ════════════════════════════════════════════════════
    // UPDATE EN BASE
    // ════════════════════════════════════════════════════

    private void updateDiscountInDB(int promoId, float newDiscount) {
        String sql = "UPDATE promotion SET discount_percentage = ? WHERE id = ?";
        try (Connection conn = MyBD.getInstance().getConnection();  // ✅
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setFloat(1, newDiscount);
            ps.setInt(2, promoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SmartDiscount] ❌ Erreur UPDATE : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════
    // PREVIEW — simulation sans écriture DB (pour UI)
    // ════════════════════════════════════════════════════

    /**
     * Calcule la réduction "smart" sans modifier la DB.
     * Utile pour afficher une preview dans le BackOffice.
     */
    public float previewDiscount(Promotion p, double avgReservations) {
        if (p.getDiscountPercentage() == null) return 0f;
        return calculateNewDiscount(p, avgReservations);
    }

    /**
     * Explication textuelle de l'ajustement (pour UI BackOffice).
     */
    public String getAdjustmentReason(Promotion p, double avgReservations) {
        int resa = p.getNbReservations();
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), p.getEndDate().toLocalDate());
        StringBuilder sb = new StringBuilder();

        if (avgReservations > 0 && resa < avgReservations * SEUIL_BAS)
            sb.append("📉 Peu de réserv. → +").append(BOOST_FAIBLE_RESA).append("% · ");
        if (avgReservations > 0 && resa > avgReservations * SEUIL_HAUT)
            sb.append("📈 Forte demande → -").append(REDUCE_FORTE_RESA).append("% · ");
        if (daysLeft >= 0 && daysLeft < 3)
            sb.append("⏰ Expire dans ").append(daysLeft).append("j → +").append(BOOST_EXPIRE_BIENT).append("% · ");

        return sb.length() > 0 ? sb.toString().replaceAll(" · $", "") : "✅ Réduction stable";
    }
}
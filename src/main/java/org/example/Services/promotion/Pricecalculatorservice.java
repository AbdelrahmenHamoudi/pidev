package org.example.Services.promotion;

import org.example.Entites.promotion.PromotionTarget;
import org.example.Entites.promotion.TargetType;

import java.util.List;

/**
 * ✅ Central pricing engine for all offer types.
 *
 * Pricing rules:
 *   HEBERGEMENT : prix = prixParNuit × nbNuits
 *   ACTIVITE    : prix = prixParPersonne × nbPersonnes
 *   VOITURE     : prix = prix_KM × distanceKm
 *   PACK        : prix = sum of all targets' base prices → then discount applied once
 *
 * The discount is applied AFTER computing the base price.
 * Supports both % and fixed TND discounts.
 */
public class Pricecalculatorservice {

    private static Pricecalculatorservice instance;
    private final Offresservice offresService;

    private Pricecalculatorservice() {
        this.offresService = Offresservice.getInstance();
    }

    public static Pricecalculatorservice getInstance() {
        if (instance == null) instance = new Pricecalculatorservice();
        return instance;
    }

    // ════════════════════════════════════════════════════
    // BASE PRICE — per type
    // ════════════════════════════════════════════════════

    /**
     * HÉBERGEMENT: fetches prixParNuit from DB × nbNuits
     * @param hebergementId  target id_hebergement
     * @param nbNuits        number of nights
     */
    public float calculateHebergement(int hebergementId, int nbNuits) {
        float prix = offresService.getPrixParNuit(hebergementId);
        System.out.printf("[PriceCalc] Hébergement #%d : %.2f TND/nuit × %d nuits = %.2f TND%n",
                hebergementId, prix, nbNuits, prix * nbNuits);
        return prix * nbNuits;
    }

    /**
     * ACTIVITÉ: fetches prixParPersonne from DB × nbPersonnes
     * @param activiteId    target idActivite
     * @param nbPersonnes   number of persons
     */
    public float calculateActivite(int activiteId, int nbPersonnes) {
        float prix = offresService.getPrixParPersonne(activiteId);
        System.out.printf("[PriceCalc] Activité #%d : %.2f TND/pers × %d pers = %.2f TND%n",
                activiteId, prix, nbPersonnes, prix * nbPersonnes);
        return prix * nbPersonnes;
    }

    /**
     * VOITURE: fetches prix_KM from DB × distanceKm
     * @param voitureId   target id_voiture
     * @param distanceKm  distance in km (entered by user)
     */
    public float calculateVoiture(int voitureId, float distanceKm) {
        float prix = offresService.getPrixKm(voitureId);
        System.out.printf("[PriceCalc] Voiture #%d : %.2f TND/km × %.1f km = %.2f TND%n",
                voitureId, prix, distanceKm, prix * distanceKm);
        return prix * distanceKm;
    }

    // ════════════════════════════════════════════════════
    // PACK PRICE — sum of all targets
    // ════════════════════════════════════════════════════

    /**
     * PACK: sums base prices of all targets.
     * Each target uses its own pricing logic.
     * Pack-specific inputs (nbNuits, nbPersonnes, distanceKm) are passed as parameters.
     *
     * @param targets       list of PromotionTarget rows for this pack
     * @param nbNuits       input for HEBERGEMENT targets (0 if none)
     * @param nbPersonnes   input for ACTIVITE targets (0 if none)
     * @param distanceKm    input for VOITURE targets (0 if none)
     */
    public float calculatePackTotal(List<PromotionTarget> targets,
                                    int nbNuits, int nbPersonnes, float distanceKm) {
        float total = 0f;
        for (PromotionTarget t : targets) {
            total += switch (t.getTargetType()) {
                case HEBERGEMENT -> calculateHebergement(t.getTargetId(), nbNuits);
                case ACTIVITE    -> calculateActivite(t.getTargetId(), nbPersonnes);
                case VOITURE     -> calculateVoiture(t.getTargetId(), distanceKm);
            };
        }
        System.out.printf("[PriceCalc] Pack total (avant réduction) : %.2f TND%n", total);
        return total;
    }

    // ════════════════════════════════════════════════════
    // DISCOUNT APPLICATION
    // ════════════════════════════════════════════════════

    /**
     * Applies % or fixed discount on base price.
     * Always returns a positive value (min 0).
     */
    public float applyDiscount(float basePrice, Float discountPct, Float discountFixed) {
        if (discountPct != null && discountPct > 0) {
            float reduction = basePrice * (discountPct / 100f);
            System.out.printf("[PriceCalc] Réduction -%.0f%% : -%.2f TND → Final: %.2f TND%n",
                    discountPct, reduction, basePrice - reduction);
            return Math.max(0f, basePrice - reduction);
        }
        if (discountFixed != null && discountFixed > 0) {
            System.out.printf("[PriceCalc] Réduction fixe : -%.2f TND → Final: %.2f TND%n",
                    discountFixed, basePrice - discountFixed);
            return Math.max(0f, basePrice - discountFixed);
        }
        return basePrice;
    }

    /**
     * Returns the actual discount amount (not the final price).
     */
    public float getDiscountAmount(float basePrice, Float discountPct, Float discountFixed) {
        if (discountPct != null && discountPct > 0) return basePrice * (discountPct / 100f);
        if (discountFixed != null && discountFixed > 0) return Math.min(discountFixed, basePrice);
        return 0f;
    }

    // ════════════════════════════════════════════════════
    // PRICE BY TYPE — single dispatch method
    // Used by reservation dialog when target type is known
    // ════════════════════════════════════════════════════

    /**
     * Computes base price for a single target.
     * @param type        HEBERGEMENT / ACTIVITE / VOITURE
     * @param targetId    the offer's ID
     * @param nbNuits     used if HEBERGEMENT
     * @param nbPersonnes used if ACTIVITE
     * @param distanceKm  used if VOITURE
     */
    public float calculateByType(TargetType type, int targetId,
                                 int nbNuits, int nbPersonnes, float distanceKm) {
        return switch (type) {
            case HEBERGEMENT -> calculateHebergement(targetId, nbNuits);
            case ACTIVITE    -> calculateActivite(targetId, nbPersonnes);
            case VOITURE     -> calculateVoiture(targetId, distanceKm);
        };
    }

    // ════════════════════════════════════════════════════
    // PRICE LABEL — for display in UI
    // ════════════════════════════════════════════════════

    /** Returns a human label showing the unit price for an offer (used in backoffice) */
    public String getPriceUnitLabel(TargetType type, int targetId) {
        return switch (type) {
            case HEBERGEMENT -> {
                float p = offresService.getPrixParNuit(targetId);
                yield String.format("%.0f TND/nuit", p);
            }
            case ACTIVITE -> {
                float p = offresService.getPrixParPersonne(targetId);
                yield String.format("%.0f TND/personne", p);
            }
            case VOITURE -> {
                float p = offresService.getPrixKm(targetId);
                yield String.format("%.2f TND/km", p);
            }
        };
    }
}
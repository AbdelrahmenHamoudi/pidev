package org.example.controllers.frontoffice;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.example.models.*;
import org.example.services.*;
import org.example.utils.SessionManager;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * ✅ FULLY REWRITTEN: Dynamic pricing per offer type.
 *
 * OLD behavior: always used prixParJour (static field on Promotion)
 * NEW behavior:
 *   1. Loads PromotionTargets for this promotion
 *   2. Detects target type (HEBERGEMENT / ACTIVITE / VOITURE)
 *   3. Fetches real price from DB via PriceCalculatorService
 *   4. Shows the correct input fields per type:
 *      - HEBERGEMENT → date picker (nb nuits = date range)
 *      - ACTIVITE    → nb personnes spinner
 *      - VOITURE     → distance km field
 *   5. For PACK → shows ALL inputs needed for all types
 *
 * Session user is read from SessionManager (dynamic, set by User module on login).
 */
public class ReservationDialogController {

    // ── Promo info ──
    @FXML private Text txtNomPromo;
    @FXML private Text txtPeriodePromo;
    @FXML private Text txtReduction;

    // ── Price display ──
    @FXML private Text txtPrixBase;         // replaces txtPrixParJour — shows base price label
    @FXML private Text txtPrixOriginal;
    @FXML private Text txtReductionMontant;
    @FXML private Text txtPrixFinal;

    // ── Date inputs (HEBERGEMENT + always shown for date range) ──
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;

    // ── Activité input ──
    @FXML private Spinner<Integer> spinnerPersonnes;
    @FXML private HBox rowPersonnes;   // shown only for ACTIVITE

    // ── Voiture input ──
    @FXML private TextField txtDistanceKm;
    @FXML private HBox rowDistance;    // shown only for VOITURE

    // ── Pack info row ──
    @FXML private VBox packInfoBox;    // shown only for packs — lists all offer types

    @FXML private Text txtAvertissement;
    @FXML private Button btnConfirmer;

    private Promotion            promotion;
    private List<PromotionTarget> targets;
    private ReservationPromoService reservationService;
    private PromotionService        promotionService;
    private Pricecalculatorservice priceCalc;
    private Offresservice offresService;

    public void initialize() {
        reservationService = new ReservationPromoService();
        promotionService   = new PromotionService();
        priceCalc          = Pricecalculatorservice.getInstance();
        offresService      = Offresservice.getInstance();

        // Init spinner
        if (spinnerPersonnes != null) {
            spinnerPersonnes.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));
            spinnerPersonnes.valueProperty().addListener((obs, o, n) -> calculerPrix());
        }

        // Listeners
        if (dateDebutPicker != null)
            dateDebutPicker.valueProperty().addListener((obs, o, n) -> calculerPrix());
        if (dateFinPicker != null)
            dateFinPicker.valueProperty().addListener((obs, o, n) -> calculerPrix());
        if (txtDistanceKm != null)
            txtDistanceKm.textProperty().addListener((obs, o, n) -> calculerPrix());
    }

    public void setPromotion(Promotion promo) {
        this.promotion = promo;

        // Load targets from DB
        PromotionTargetService targetSvc = new PromotionTargetService();
        targets = targetSvc.getByPromotionId(promo.getId());

        // Display promo info
        txtNomPromo.setText(promo.getName());
        txtPeriodePromo.setText(promo.getStartDate() + " → " + promo.getEndDate());

        String reduction = "";
        if (promo.getDiscountPercentage() != null)
            reduction = "-" + String.format("%.0f", promo.getDiscountPercentage()) + "%";
        if (promo.getDiscountFixed() != null)
            reduction = "-" + String.format("%.0f", promo.getDiscountFixed()) + " TND";
        txtReduction.setText(reduction);

        // nb_vues already incremented when card was displayed in FrontOffice.
        // No double-counting here.

        // Configure UI based on offer type(s)
        configureUIForTargets();

        // Date range constraints
        configureDatePickers();
    }

    /**
     * Shows/hides input sections depending on target types.
     * For a pack, multiple sections can be shown simultaneously.
     */
    private void configureUIForTargets() {
        boolean hasHeberg  = targets.stream().anyMatch(t -> t.getTargetType() == TargetType.HEBERGEMENT);
        boolean hasActivite = targets.stream().anyMatch(t -> t.getTargetType() == TargetType.ACTIVITE);
        boolean hasVoiture  = targets.stream().anyMatch(t -> t.getTargetType() == TargetType.VOITURE);

        // Show/hide rows
        if (rowPersonnes != null) rowPersonnes.setVisible(hasActivite);
        if (rowPersonnes != null) rowPersonnes.setManaged(hasActivite);
        if (rowDistance != null)  rowDistance.setVisible(hasVoiture);
        if (rowDistance != null)  rowDistance.setManaged(hasVoiture);

        // Date picker: always show (needed for hebergement; also used to bound the reservation period)
        // For activite/voiture only, still show dates as "reservation validity window" but nb_jours=1 default

        // Set base price label
        if (txtPrixBase != null) {
            if (promotion.isPack() && targets.size() > 1) {
                txtPrixBase.setText("Pack combiné — prix calculé automatiquement");
            } else if (!targets.isEmpty()) {
                PromotionTarget first = targets.get(0);
                txtPrixBase.setText(priceCalc.getPriceUnitLabel(first.getTargetType(), first.getTargetId()));
            } else {
                txtPrixBase.setText("—");
            }
        }

        // Pack info box — list all offer names
        if (packInfoBox != null && promotion.isPack()) {
            packInfoBox.setVisible(true);
            packInfoBox.setManaged(true);
            for (PromotionTarget t : targets) {
                String typeIcon = switch (t.getTargetType()) {
                    case HEBERGEMENT -> "🏨";
                    case ACTIVITE    -> "🎯";
                    case VOITURE     -> "🚗";
                };
                String offerName = offresService.getOfferName(t.getTargetType(), t.getTargetId());
                String unitPrice = priceCalc.getPriceUnitLabel(t.getTargetType(), t.getTargetId());
                javafx.scene.text.Text lbl = new javafx.scene.text.Text(typeIcon + " " + offerName + " · " + unitPrice);
                lbl.setStyle("-fx-font-size: 11px;");
                packInfoBox.getChildren().add(lbl);
            }
        } else if (packInfoBox != null) {
            packInfoBox.setVisible(false);
            packInfoBox.setManaged(false);
        }
    }

    private void configureDatePickers() {
        if (dateDebutPicker == null) return;
        dateDebutPicker.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(promotion.getStartDate().toLocalDate())
                        || date.isAfter(promotion.getEndDate().toLocalDate()));
            }
        });
        dateFinPicker.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate debut = dateDebutPicker.getValue();
                boolean disable = empty || date.isBefore(promotion.getStartDate().toLocalDate())
                        || date.isAfter(promotion.getEndDate().toLocalDate());
                if (debut != null) disable = disable || date.isBefore(debut.plusDays(1));
                setDisable(disable);
            }
        });
    }

    /** Recalculates and updates price display on any input change. */
    private void calculerPrix() {
        if (promotion == null || targets == null) return;

        LocalDate dateDebut = dateDebutPicker != null ? dateDebutPicker.getValue() : null;
        LocalDate dateFin   = dateFinPicker   != null ? dateFinPicker.getValue()   : null;

        // Validate dates if provided
        if (dateDebut != null && dateFin != null) {
            if (!dateFin.isAfter(dateDebut)) {
                if (txtAvertissement != null) txtAvertissement.setText("⚠️ La date de fin doit être après la date de début");
                btnConfirmer.setDisable(true);
                return;
            }
            if (dateDebut.isBefore(promotion.getStartDate().toLocalDate())
                    || dateFin.isAfter(promotion.getEndDate().toLocalDate())) {
                if (txtAvertissement != null) txtAvertissement.setText("⚠️ Les dates doivent être dans la période de la promotion");
                btnConfirmer.setDisable(true);
                return;
            }
        }

        if (txtAvertissement != null) txtAvertissement.setText("✅ Données valides");
        btnConfirmer.setDisable(false);

        // Compute base price
        float basePrice = computeBasePrice(dateDebut, dateFin);

        float discountAmount = priceCalc.getDiscountAmount(basePrice,
                promotion.getDiscountPercentage(), promotion.getDiscountFixed());
        float prixFinal = basePrice - discountAmount;

        if (txtPrixOriginal   != null) txtPrixOriginal.setText(String.format("%.2f TND", basePrice));
        if (txtReductionMontant != null) txtReductionMontant.setText(String.format("-%.2f TND", discountAmount));
        if (txtPrixFinal      != null) txtPrixFinal.setText(String.format("%.2f TND", prixFinal));
    }

    /**
     * Computes base price by dispatching to PriceCalculatorService per target type.
     */
    private float computeBasePrice(LocalDate dateDebut, LocalDate dateFin) {
        int   nbNuits     = (dateDebut != null && dateFin != null) ? (int) ChronoUnit.DAYS.between(dateDebut, dateFin) : 1;
        int   nbPersonnes = (spinnerPersonnes != null) ? spinnerPersonnes.getValue() : 1;
        float distanceKm  = 0f;
        if (txtDistanceKm != null && !txtDistanceKm.getText().isBlank()) {
            try { distanceKm = Float.parseFloat(txtDistanceKm.getText().trim()); } catch (NumberFormatException ignored) {}
        }

        if (targets == null || targets.isEmpty()) return 0f;

        if (promotion.isPack()) {
            return priceCalc.calculatePackTotal(targets, nbNuits, nbPersonnes, distanceKm);
        } else {
            PromotionTarget t = targets.get(0);
            return priceCalc.calculateByType(t.getTargetType(), t.getTargetId(), nbNuits, nbPersonnes, distanceKm);
        }
    }

    @FXML
    private void handleConfirmer() {
        LocalDate dateDebut = dateDebutPicker != null ? dateDebutPicker.getValue() : LocalDate.now();
        LocalDate dateFin   = dateFinPicker   != null ? dateFinPicker.getValue()   : LocalDate.now().plusDays(1);

        if (dateDebut == null || dateFin == null) {
            showAlert(Alert.AlertType.WARNING, "Dates requises", "Veuillez sélectionner les dates.");
            return;
        }

        int   nbNuits     = (int) ChronoUnit.DAYS.between(dateDebut, dateFin);
        int   nbPersonnes = (spinnerPersonnes != null) ? spinnerPersonnes.getValue() : 1;
        float distanceKm  = 0f;
        if (txtDistanceKm != null && !txtDistanceKm.getText().isBlank()) {
            try { distanceKm = Float.parseFloat(txtDistanceKm.getText().trim()); } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Distance invalide", "Entrez une distance valide en km.");
                return;
            }
        }

        float basePrice      = computeBasePrice(dateDebut, dateFin);
        float discountAmount = priceCalc.getDiscountAmount(basePrice,
                promotion.getDiscountPercentage(), promotion.getDiscountFixed());
        float prixFinal      = basePrice - discountAmount;

        // Build reservation — prix_par_jour stores the per-unit price of the primary offer for reference
        float prixParJourRef = (targets != null && !targets.isEmpty())
                ? switch (targets.get(0).getTargetType()) {
            case HEBERGEMENT -> offresService.getPrixParNuit(targets.get(0).getTargetId());
            case ACTIVITE    -> offresService.getPrixParPersonne(targets.get(0).getTargetId());
            case VOITURE     -> offresService.getPrixKm(targets.get(0).getTargetId());
        }
                : 0f;

        ReservationPromo reservation = new ReservationPromo(
                SessionManager.getCurrentUserId(),
                promotion.getId(),
                Date.valueOf(dateDebut),
                Date.valueOf(dateFin),
                nbNuits,
                prixParJourRef,
                basePrice,
                discountAmount,
                prixFinal
        );

        ReservationPromo saved = reservationService.add(reservation);

        if (saved != null) {
            // SmartDiscount recalculates silently in background
            new Thread(() -> SmartDiscountEngine.getInstance()
                    .recalculateForPromoSilent(promotion.getId())).start();

            showAlert(Alert.AlertType.INFORMATION, "Réservation confirmée",
                    "✅ Réservation enregistrée !\n\nMontant total : " + String.format("%.2f TND", prixFinal));
            closeDialog();
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur est survenue lors de l'enregistrement.");
        }
    }

    @FXML private void handleCancel() { closeDialog(); }

    private void closeDialog() {
        Stage stage = (Stage) btnConfirmer.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content);
        alert.showAndWait();
    }
}
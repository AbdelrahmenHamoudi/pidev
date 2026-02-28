package org.example.controllers.frontoffice;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.example.models.Promotion;
import org.example.models.ReservationPromo;
import org.example.services.PromotionService;
import org.example.services.ReservationPromoService;
import org.example.services.SmartDiscountEngine;
import org.example.utils.SessionManager;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ReservationDialogController {

    @FXML private Text txtNomPromo;
    @FXML private Text txtPeriodePromo;
    @FXML private Text txtReduction;
    @FXML private Text txtPrixParJour;
    @FXML private Text txtNbJours;
    @FXML private Text txtPrixOriginal;
    @FXML private Text txtReductionMontant;
    @FXML private Text txtPrixFinal;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private Text txtAvertissement;
    @FXML private Button btnConfirmer;

    private Promotion promotion;
    private ReservationPromoService reservationService;
    private PromotionService promotionService;
    private static final float PRIX_PAR_JOUR_DEFAUT = 50.0f;

    public void initialize() {
        reservationService = new ReservationPromoService();
        promotionService   = new PromotionService();

        dateDebutPicker.valueProperty().addListener((obs, oldVal, newVal) -> calculerPrix());
        dateFinPicker.valueProperty().addListener((obs, oldVal, newVal)   -> calculerPrix());
    }

    public void setPromotion(Promotion promo) {
        this.promotion = promo;

        txtNomPromo.setText(promo.getName());

        float prixParJour = promo.getPrixParJour() != null ? promo.getPrixParJour() : PRIX_PAR_JOUR_DEFAUT;
        txtPrixParJour.setText(String.format("%.2f TND", prixParJour));

        String periodePromo = promo.getStartDate().toString() + " → " + promo.getEndDate().toString();
        txtPeriodePromo.setText(periodePromo);

        // Réduction — affichage propre sans mention "Smart"
        String reduction = "";
        if (promo.getDiscountPercentage() != null)
            reduction = "-" + String.format("%.0f", promo.getDiscountPercentage()) + "%";
        if (promo.getDiscountFixed() != null)
            reduction = "-" + String.format("%.0f", promo.getDiscountFixed()) + " TND";
        txtReduction.setText(reduction);

        // ✅ FIX nb_vues — incrémenter quand l'user ouvre le dialog
        promotionService.incrementVues(promo.getId());

        // Dates min/max
        dateDebutPicker.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(promo.getStartDate().toLocalDate())
                        || date.isAfter(promo.getEndDate().toLocalDate()));
            }
        });

        dateFinPicker.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate dateDebut = dateDebutPicker.getValue();
                boolean disable = empty || date.isBefore(promo.getStartDate().toLocalDate())
                        || date.isAfter(promo.getEndDate().toLocalDate());
                if (dateDebut != null) disable = disable || date.isBefore(dateDebut.plusDays(1));
                setDisable(disable);
            }
        });
    }

    private void calculerPrix() {
        LocalDate dateDebut = dateDebutPicker.getValue();
        LocalDate dateFin   = dateFinPicker.getValue();

        if (dateDebut == null || dateFin == null) {
            txtNbJours.setText("0 jour(s)");
            txtPrixOriginal.setText("0.00 TND");
            txtReductionMontant.setText("-0.00 TND");
            txtPrixFinal.setText("0.00 TND");
            return;
        }

        if (dateFin.isBefore(dateDebut) || dateFin.isEqual(dateDebut)) {
            txtAvertissement.setText("⚠️ La date de fin doit être après la date de début");
            btnConfirmer.setDisable(true);
            return;
        }

        if (dateDebut.isBefore(promotion.getStartDate().toLocalDate())
                || dateFin.isAfter(promotion.getEndDate().toLocalDate())) {
            txtAvertissement.setText("⚠️ Les dates doivent être dans la période de la promotion");
            btnConfirmer.setDisable(true);
            return;
        }

        txtAvertissement.setText("✅ Période valide");
        btnConfirmer.setDisable(false);

        long nbJours = ChronoUnit.DAYS.between(dateDebut, dateFin);
        txtNbJours.setText(nbJours + " jour(s)");

        float prixParJour = promotion.getPrixParJour() != null
                ? promotion.getPrixParJour() : PRIX_PAR_JOUR_DEFAUT;
        float prixOriginal = prixParJour * nbJours;
        txtPrixOriginal.setText(String.format("%.2f TND", prixOriginal));

        float reduction = 0;
        if (promotion.getDiscountPercentage() != null)
            reduction = prixOriginal * (promotion.getDiscountPercentage() / 100);
        else if (promotion.getDiscountFixed() != null)
            reduction = promotion.getDiscountFixed();
        txtReductionMontant.setText(String.format("-%.2f TND", reduction));

        float prixFinal = prixOriginal - reduction;
        txtPrixFinal.setText(String.format("%.2f TND", prixFinal));
    }

    @FXML
    private void handleConfirmer() {
        LocalDate dateDebut = dateDebutPicker.getValue();
        LocalDate dateFin   = dateFinPicker.getValue();

        if (dateDebut == null || dateFin == null) {
            showAlert(Alert.AlertType.WARNING, "Dates requises",
                    "Veuillez sélectionner les dates de début et de fin.");
            return;
        }

        long nbJours      = ChronoUnit.DAYS.between(dateDebut, dateFin);
        float prixParJour = promotion.getPrixParJour() != null
                ? promotion.getPrixParJour() : PRIX_PAR_JOUR_DEFAUT;
        float prixOriginal = prixParJour * nbJours;

        float reduction = 0;
        if (promotion.getDiscountPercentage() != null)
            reduction = prixOriginal * (promotion.getDiscountPercentage() / 100);
        else if (promotion.getDiscountFixed() != null)
            reduction = promotion.getDiscountFixed();

        float prixFinal = prixOriginal - reduction;

        ReservationPromo reservation = new ReservationPromo(
                SessionManager.getCurrentUserId(),
                promotion.getId(),
                Date.valueOf(dateDebut),
                Date.valueOf(dateFin),
                (int) nbJours,
                prixParJour,
                prixOriginal,
                reduction,
                prixFinal
        );

        // ✅ add() appelle déjà incrementReservations() en interne
        ReservationPromo saved = reservationService.add(reservation);

        if (saved != null) {
            // ✅ SmartDiscount recalcule en background — SANS notification visible par l'user
            new Thread(() -> SmartDiscountEngine.getInstance()
                    .recalculateForPromoSilent(promotion.getId())).start();

            showAlert(Alert.AlertType.INFORMATION, "Réservation confirmée",
                    "✅ Réservation enregistrée avec succès !\n\n" +
                            "Montant total : " + String.format("%.2f TND", prixFinal));
            closeDialog();
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Une erreur est survenue lors de l'enregistrement.");
        }
    }

    @FXML private void handleCancel() { closeDialog(); }

    private void closeDialog() {
        Stage stage = (Stage) btnConfirmer.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
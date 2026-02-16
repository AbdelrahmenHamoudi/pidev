package org.example.controllers.frontoffice;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.example.models.Promotion;
import org.example.models.ReservationPromo;
import org.example.services.ReservationPromoService;
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
    private static final float PRIX_PAR_JOUR_DEFAUT = 50.0f;

    public void initialize() {
        reservationService = new ReservationPromoService();


        // Listeners pour recalcul automatique
        dateDebutPicker.valueProperty().addListener((obs, oldVal, newVal) -> calculerPrix());
        dateFinPicker.valueProperty().addListener((obs, oldVal, newVal) -> calculerPrix());

    }

    /**
     * Initialiser le dialog avec une promotion
     */
    public void setPromotion(Promotion promo) {
        this.promotion = promo;

        // Afficher infos promotion
        txtNomPromo.setText(promo.getName());
        // ✅ NOUVEAU - Afficher avec "TND"
        txtPrixParJour.setText(String.format("%.2f TND", PRIX_PAR_JOUR_DEFAUT));

        String periodePromo = promo.getStartDate().toString() + " → " + promo.getEndDate().toString();
        txtPeriodePromo.setText(periodePromo);
        float prixParJour = promo.getPrixParJour() != null ? promo.getPrixParJour() : 50.0f;

        System.out.println("🔍 Prix calculé: " + prixParJour);
        System.out.println("🔍 Avant setText: " + txtPrixParJour.getText());

        txtPrixParJour.setText(String.format("%.2f TND", prixParJour));

        System.out.println("🔍 Après setText: " + txtPrixParJour.getText());

        // Afficher réduction
        String reduction = "";
        if (promo.getDiscountPercentage() != null) {
            reduction = "-" + String.format("%.0f", promo.getDiscountPercentage()) + "%";
        }
        if (promo.getDiscountFixed() != null) {
            reduction = "-" + String.format("%.0f", promo.getDiscountFixed()) + " TND";
        }
        txtReduction.setText(reduction);

        // Configurer les dates min/max
        dateDebutPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate minDate = promo.getStartDate().toLocalDate();
                LocalDate maxDate = promo.getEndDate().toLocalDate();
                setDisable(empty || date.isBefore(minDate) || date.isAfter(maxDate));
            }
        });

        dateFinPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate minDate = promo.getStartDate().toLocalDate();
                LocalDate maxDate = promo.getEndDate().toLocalDate();
                LocalDate dateDebut = dateDebutPicker.getValue();

                boolean disable = empty || date.isBefore(minDate) || date.isAfter(maxDate);
                if (dateDebut != null) {
                    disable = disable || date.isBefore(dateDebut.plusDays(1));
                }
                setDisable(disable);
            }
        });
    }

    /**
     * Calculer automatiquement les prix
     */
    private void calculerPrix() {
        LocalDate dateDebut = dateDebutPicker.getValue();
        LocalDate dateFin = dateFinPicker.getValue();

        if (dateDebut == null || dateFin == null) {
            txtNbJours.setText("0 jour(s)");
            txtPrixOriginal.setText("0.00 TND");
            txtReductionMontant.setText("-0.00 TND");
            txtPrixFinal.setText("0.00 TND");
            return;
        }

        // Valider les dates
        if (dateFin.isBefore(dateDebut) || dateFin.isEqual(dateDebut)) {
            txtAvertissement.setText("⚠️ La date de fin doit être après la date de début");
            btnConfirmer.setDisable(true);
            return;
        }

        LocalDate promoDebut = promotion.getStartDate().toLocalDate();
        LocalDate promoFin = promotion.getEndDate().toLocalDate();

        if (dateDebut.isBefore(promoDebut) || dateFin.isAfter(promoFin)) {
            txtAvertissement.setText("⚠️ Les dates doivent être dans la période de la promotion");
            btnConfirmer.setDisable(true);
            return;
        }

        txtAvertissement.setText("✅ Période valide");
        btnConfirmer.setDisable(false);

        // Calcul nombre de jours
        long nbJours = ChronoUnit.DAYS.between(dateDebut, dateFin);
        txtNbJours.setText(nbJours + " jour(s)");

        // Récupérer prix par jour
        float prixParJour = PRIX_PAR_JOUR_DEFAUT;


        // Prix original
        float prixOriginal = prixParJour * nbJours;
        txtPrixOriginal.setText(String.format("%.2f TND", prixOriginal));

        // Calculer réduction
        float reduction = 0;
        if (promotion.getDiscountPercentage() != null) {
            reduction = prixOriginal * (promotion.getDiscountPercentage() / 100);
        } else if (promotion.getDiscountFixed() != null) {
            reduction = promotion.getDiscountFixed();
        }
        txtReductionMontant.setText(String.format("-%.2f TND", reduction));

        // Prix final
        float prixFinal = prixOriginal - reduction;
        txtPrixFinal.setText(String.format("%.2f TND", prixFinal));
    }

    /**
     * Confirmer la réservation
     */
    @FXML
    private void handleConfirmer() {
        LocalDate dateDebut = dateDebutPicker.getValue();
        LocalDate dateFin = dateFinPicker.getValue();

        if (dateDebut == null || dateFin == null) {
            showAlert(Alert.AlertType.WARNING, "Dates requises",
                    "Veuillez sélectionner les dates de début et de fin.");
            return;
        }

        // Récupérer les valeurs calculées
        long nbJours = ChronoUnit.DAYS.between(dateDebut, dateFin);
        // ✅ NOUVEAU
        float prixParJour = PRIX_PAR_JOUR_DEFAUT;
        float prixOriginal = prixParJour * nbJours;

        float reduction = 0;
        if (promotion.getDiscountPercentage() != null) {
            reduction = prixOriginal * (promotion.getDiscountPercentage() / 100);
        } else if (promotion.getDiscountFixed() != null) {
            reduction = promotion.getDiscountFixed();
        }

        float prixFinal = prixOriginal - reduction;

        // Créer la réservation
        ReservationPromo reservation = new ReservationPromo(
                SessionManager.getCurrentUserId(),  // User hardcodé (id=1)
                promotion.getId(),
                Date.valueOf(dateDebut),
                Date.valueOf(dateFin),
                (int) nbJours,
                prixParJour,
                prixOriginal,
                reduction,
                prixFinal
        );

        // Enregistrer en BD
        ReservationPromo saved = reservationService.add(reservation);

        if (saved != null) {
            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "✅ Réservation enregistrée avec succès !\n\n" +
                            "Montant total : " + String.format("%.2f TND", prixFinal));
            closeDialog();
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Une erreur est survenue lors de l'enregistrement.");
        }
    }

    /**
     * Annuler
     */
    @FXML
    private void handleCancel() {
        closeDialog();
    }

    /**
     * Fermer le dialog
     */
    private void closeDialog() {
        Stage stage = (Stage) btnConfirmer.getScene().getWindow();
        stage.close();
    }

    /**
     * Afficher une alerte
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

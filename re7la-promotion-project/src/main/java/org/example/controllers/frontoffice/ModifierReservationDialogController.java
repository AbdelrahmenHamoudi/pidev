package org.example.controllers.frontoffice;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.example.models.Promotion;
import org.example.models.ReservationPromo;
import org.example.services.ReservationPromoService;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ModifierReservationDialogController {

    @FXML private Text txtNomPromo;
    @FXML private Text txtPeriodePromo;
    @FXML private Text txtReduction;
    @FXML private Text txtPrixParJour;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private Text txtNbJours;
    @FXML private Text txtPrixOriginal;
    @FXML private Text txtReductionMontant;
    @FXML private Text txtPrixFinal;
    @FXML private Button btnConfirmer;

    private ReservationPromo reservation;
    private Promotion promotion;
    private ReservationPromoService reservationService;

    public void initialize() {
        reservationService = new ReservationPromoService();

        // Listeners pour recalcul automatique
        dateDebutPicker.valueProperty().addListener((obs, oldVal, newVal) -> calculerPrix());
        dateFinPicker.valueProperty().addListener((obs, oldVal, newVal) -> calculerPrix());
    }

    /**
     * Initialiser avec une réservation existante
     */
    public void setReservation(ReservationPromo resa, Promotion promo) {
        this.reservation = resa;
        this.promotion = promo;

        // Afficher infos promotion
        txtNomPromo.setText(promo.getName());

        String periodePromo = promo.getStartDate().toString() + " → " + promo.getEndDate().toString();
        txtPeriodePromo.setText(periodePromo);

        // Afficher réduction
        String reduction = "";
        if (promo.getDiscountPercentage() != null) {
            reduction = "-" + String.format("%.0f", promo.getDiscountPercentage()) + "%";
        }
        if (promo.getDiscountFixed() != null) {
            reduction = "-" + String.format("%.0f", promo.getDiscountFixed()) + " TND";
        }
        txtReduction.setText(reduction);

        // Afficher prix par jour
        txtPrixParJour.setText(String.format("%.2f TND", resa.getPrixParJour()));

        // Pré-remplir les dates actuelles
        dateDebutPicker.setValue(resa.getDateDebutReservation().toLocalDate());
        dateFinPicker.setValue(resa.getDateFinReservation().toLocalDate());

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

        // Calculer initialement
        calculerPrix();
    }

    /**
     * Calculer automatiquement les nouveaux prix
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
            btnConfirmer.setDisable(true);
            return;
        }

        LocalDate promoDebut = promotion.getStartDate().toLocalDate();
        LocalDate promoFin = promotion.getEndDate().toLocalDate();

        if (dateDebut.isBefore(promoDebut) || dateFin.isAfter(promoFin)) {
            btnConfirmer.setDisable(true);
            return;
        }

        btnConfirmer.setDisable(false);

        // Calcul nombre de jours
        long nbJours = ChronoUnit.DAYS.between(dateDebut, dateFin);
        txtNbJours.setText(nbJours + " jour(s)");

        // Prix par jour (récupéré de la réservation existante)
        float prixParJour = reservation.getPrixParJour();

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
     * Confirmer la modification
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

        // Récupérer les nouvelles valeurs
        long nbJours = ChronoUnit.DAYS.between(dateDebut, dateFin);
        float prixParJour = reservation.getPrixParJour();
        float prixOriginal = prixParJour * nbJours;

        float reduction = 0;
        if (promotion.getDiscountPercentage() != null) {
            reduction = prixOriginal * (promotion.getDiscountPercentage() / 100);
        } else if (promotion.getDiscountFixed() != null) {
            reduction = promotion.getDiscountFixed();
        }

        float prixFinal = prixOriginal - reduction;

        // Mettre à jour la réservation
        reservation.setDateDebutReservation(Date.valueOf(dateDebut));
        reservation.setDateFinReservation(Date.valueOf(dateFin));
        reservation.setNbJours((int) nbJours);
        reservation.setPrixOriginal(prixOriginal);
        reservation.setReductionAppliquee(reduction);
        reservation.setMontantTotal(prixFinal);

        // Enregistrer en BD
        boolean updated = reservationService.update(reservation);

        if (updated) {
            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "✅ Réservation modifiée avec succès !\n\n" +
                            "Nouveau montant : " + String.format("%.2f TND", prixFinal));
            closeDialog();
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Une erreur est survenue lors de la modification.");
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
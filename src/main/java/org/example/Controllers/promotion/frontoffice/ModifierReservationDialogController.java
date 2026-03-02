package org.example.Controllers.promotion.frontoffice;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.example.Entites.promotion.Promotion;              // ✅
import org.example.Entites.promotion.ReservationPromo;       // ✅
import org.example.Entites.user.User;                        // ✅ Ajout
import org.example.Services.promotion.ReservationPromoService; // ✅
import org.example.Utils.UserSession;                         // ✅ Ajout

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
    private User currentUser;  // ✅ Utilisateur connecté

    public void initialize() {
        reservationService = new ReservationPromoService();

        // ✅ Vérifier l'authentification
        if (!checkUserAuth()) {
            return;
        }

        // Listeners pour recalcul automatique
        dateDebutPicker.valueProperty().addListener((obs, oldVal, newVal) -> calculerPrix());
        dateFinPicker.valueProperty().addListener((obs, oldVal, newVal) -> calculerPrix());
    }

    /**
     * ✅ Vérifie si l'utilisateur est authentifié via JWT
     */
    private boolean checkUserAuth() {
        if (!UserSession.getInstance().isTokenValid()) {
            showAlert(Alert.AlertType.ERROR, "Session expirée",
                    "Votre session a expiré. Veuillez vous reconnecter.");
            closeDialog();
            return false;
        }

        currentUser = UserSession.getInstance().getCurrentUser();

        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Aucun utilisateur connecté.");
            closeDialog();
            return false;
        }

        return true;
    }

    /**
     * Initialiser avec une réservation existante
     */
    public void setReservation(ReservationPromo resa, Promotion promo) {
        this.reservation = resa;
        this.promotion = promo;

        // ✅ Vérifier que l'utilisateur est le propriétaire de la réservation
        if (resa.getUser() != null && resa.getUser().getId() != currentUser.getId()) {
            boolean isAdmin = currentUser.getRole() != null &&
                    currentUser.getRole().name().equalsIgnoreCase("admin");
            if (!isAdmin) {
                showAlert(Alert.AlertType.ERROR, "Accès refusé",
                        "Vous ne pouvez modifier que vos propres réservations.");
                closeDialog();
                return;
            }
        }

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
            btnConfirmer.setDisable(true);
            return;
        }

        // Valider les dates
        if (dateFin.isBefore(dateDebut) || dateFin.isEqual(dateDebut)) {
            txtAvertissement("⚠️ La date de fin doit être après la date de début");
            btnConfirmer.setDisable(true);
            return;
        }

        LocalDate promoDebut = promotion.getStartDate().toLocalDate();
        LocalDate promoFin = promotion.getEndDate().toLocalDate();

        if (dateDebut.isBefore(promoDebut) || dateFin.isAfter(promoFin)) {
            txtAvertissement("⚠️ Les dates doivent être dans la période de la promotion");
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
     * Afficher un message d'avertissement
     */
    private void txtAvertissement(String message) {
        // Optionnel: créer un label d'avertissement dans le FXML
        System.out.println(message);
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
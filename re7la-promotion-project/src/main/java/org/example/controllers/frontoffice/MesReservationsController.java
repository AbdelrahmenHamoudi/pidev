package org.example.controllers.frontoffice;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.models.Promotion;
import org.example.models.ReservationPromo;
import org.example.services.PromotionService;
import org.example.services.ReservationPromoService;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MesReservationsController implements Initializable {

    @FXML private Text txtUserName;
    @FXML private Text txtTotalReservations;
    @FXML private VBox reservationsContainer;
    @FXML private VBox emptyState;

    private ReservationPromoService reservationService;
    private PromotionService promotionService;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        reservationService = new ReservationPromoService();
        promotionService = new PromotionService();
        txtUserName.setText(SessionManager.getCurrentUserName());
        loadReservations();
    }



    /**

    /**
     * Charger les réservations du user connecté
     */
    private void loadReservations() {
        int userId = SessionManager.getCurrentUserId();
        List<ReservationPromo> reservations = reservationService.getByUserId(userId);

        txtTotalReservations.setText(String.valueOf(reservations.size()));
        reservationsContainer.getChildren().clear();

        if (reservations.isEmpty()) {
            emptyState.setVisible(true);
            return;
        }
        emptyState.setVisible(false);

        for (ReservationPromo r : reservations) {
            reservationsContainer.getChildren().add(buildReservationCard(r));
        }
    }

    private VBox buildReservationCard(ReservationPromo r) {
        Optional<Promotion> promoOpt = promotionService.getById(r.getPromotionId());
        Promotion promo = promoOpt.orElse(null);
        boolean isPack    = promo != null && promo.isPack();
        boolean isExpired = promo != null && promo.getEndDate().toLocalDate().isBefore(LocalDate.now());

        VBox card = new VBox(0);
        String normalStyle = "-fx-background-color: white; -fx-background-radius: 14; " +
                "-fx-border-color: #E2E8F0; -fx-border-radius: 14; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 3);";
        String hoverStyle  = "-fx-background-color: white; -fx-background-radius: 14; " +
                "-fx-border-color: #F39C12; -fx-border-radius: 14; -fx-border-width: 2; " +
                "-fx-effect: dropshadow(gaussian, rgba(243,156,18,0.2), 14, 0, 0, 4); -fx-translate-y: -2;";
        card.setStyle(normalStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e  -> card.setStyle(normalStyle));

        // Colored top strip
        String stripColor = isPack ? "#F39C12" : (isExpired ? "#94A3B8" : "#1ABC9C");
        HBox strip = new HBox();
        strip.setPrefHeight(6);
        strip.setStyle("-fx-background-color: " + stripColor + "; -fx-background-radius: 14 14 0 0;");

        // Main content row
        HBox content = new HBox(16);
        content.setPadding(new Insets(14, 16, 14, 16));
        content.setAlignment(Pos.CENTER_LEFT);

        // LEFT — promo info
        VBox left = new VBox(5); HBox.setHgrow(left, Priority.ALWAYS);
        String promoName = promo != null ? promo.getName() : "Promotion #" + r.getPromotionId();
        Label nameLabel = new Label(promoName);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #2C3E50;");

        HBox badges = new HBox(6);
        if (isPack) {
            Label packBadge = new Label("🎁 Pack");
            packBadge.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #D97706; " +
                    "-fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 3 8; -fx-background-radius: 10;");
            badges.getChildren().add(packBadge);
        }
        Label statusBadge = new Label(isExpired ? "⛔ Expirée" : "✅ Active");
        statusBadge.setStyle("-fx-background-color: " + (isExpired ? "#F1F5F9" : "#F0FFF4") + "; " +
                "-fx-text-fill: " + (isExpired ? "#64748B" : "#276749") + "; " +
                "-fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 3 8; -fx-background-radius: 10;");
        badges.getChildren().add(statusBadge);

        Label datesLabel = new Label("📅 " + r.getDateDebutReservation() + " → " +
                r.getDateFinReservation() + "  ·  " + r.getNbJours() + " jour(s)");
        datesLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        Label timeLabel = new Label("🕐 Réservé " + getTimeAgo(r.getCreatedAt().toLocalDateTime()));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");

        left.getChildren().addAll(nameLabel, badges, datesLabel, timeLabel);

        // CENTER — amount
        VBox center = new VBox(2); center.setAlignment(Pos.CENTER);
        Label amountVal = new Label(String.format("%.2f", r.getMontantTotal()));
        amountVal.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: #F39C12;");
        Label amountCur = new Label("TND");
        amountCur.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");
        center.getChildren().addAll(amountVal, amountCur);

        // RIGHT — buttons
        VBox right = new VBox(8); right.setAlignment(Pos.CENTER);
        Button btnModifier = new Button("✏️ Modifier");
        btnModifier.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; " +
                "-fx-font-size: 11px; -fx-padding: 8 14; -fx-background-radius: 8; " +
                "-fx-cursor: hand; -fx-font-weight: 700;");
        btnModifier.setOnAction(e -> handleModifier(r));

        boolean canDelete = reservationService.canDelete(r);
        Button btnAnnuler = new Button(canDelete ? "🗑️ Annuler" : "🔒 Expiré");
        btnAnnuler.setStyle("-fx-background-color: " + (canDelete ? "#E74C3C" : "#CBD5E1") + "; " +
                "-fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 8 14; -fx-background-radius: 8; " +
                "-fx-cursor: " + (canDelete ? "hand" : "default") + "; -fx-font-weight: 700;");
        btnAnnuler.setDisable(!canDelete);
        if (canDelete) btnAnnuler.setOnAction(e -> handleAnnuler(r));

        right.getChildren().addAll(btnModifier, btnAnnuler);
        content.getChildren().addAll(left, center, right);
        card.getChildren().addAll(strip, content);
        return card;
    }

    /**
     * Modifier une réservation
     */
    private void handleModifier(ReservationPromo reservation) {
        try {
            // Récupérer la promotion
            Optional<Promotion> promoOpt = promotionService.getById(reservation.getPromotionId());
            if (promoOpt.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Promotion introuvable.");
                return;
            }

            // Ouvrir le dialog avec les données existantes
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/ModifierReservationDialog.fxml"));
            Parent root = loader.load();

            ModifierReservationDialogController controller = loader.getController();
            controller.setReservation(reservation, promoOpt.get());

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Modifier la réservation");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            dialogStage.showAndWait();

            // Rafraîchir après modification
            loadReservations();

        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture du dialog de modification");
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire.");
        }
    }

    /**
     * Annuler (supprimer) une réservation
     */
    private void handleAnnuler(ReservationPromo reservation) {
        // Confirmation
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmer l'annulation");
        confirmation.setHeaderText("Annuler cette réservation ?");
        confirmation.setContentText(
                "Promotion : " + getPromotionName(reservation.getPromotionId()) + "\n" +
                        "Montant : " + String.format("%.2f TND", reservation.getMontantTotal()) + "\n\n" +
                        "Cette action est irréversible."
        );

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean deleted = reservationService.delete(reservation.getId());

                if (deleted) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès",
                            "✅ Réservation annulée avec succès !");
                    loadReservations();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Une erreur est survenue lors de l'annulation.");
                }
            }
        });
    }

    /**
     * Rafraîchir la liste
     */
    @FXML
    private void handleRefresh() {
        loadReservations();
        showAlert(Alert.AlertType.INFORMATION, "Rafraîchi",
                "✅ Liste mise à jour !");
    }

    /**
     * Retour (fermer la fenêtre)
     */
    @FXML
    private void handleRetour() {
        Stage stage = (Stage) reservationsContainer.getScene().getWindow();
        stage.close();
    }

    /**
     * Obtenir le nom d'une promotion
     */
    private String getPromotionName(int promoId) {
        Optional<Promotion> promo = promotionService.getById(promoId);
        return promo.map(Promotion::getName).orElse("Inconnue");
    }

    /**
     * Calculer "il y a X heures/jours"
     */
    private String getTimeAgo(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();

        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        if (minutes < 60) {
            return "Il y a " + minutes + " min";
        }

        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours < 24) {
            return "Il y a " + hours + "h";
        }

        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days < 7) {
            return "Il y a " + days + " jour(s)";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateTime.format(formatter);
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
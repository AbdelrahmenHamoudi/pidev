package org.example.controllers.frontoffice;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.ResourceBundle;

public class MesReservationsController implements Initializable {

    @FXML private Text txtUserName;
    @FXML private Text txtTotalReservations;
    @FXML private TableView<ReservationPromo> tableReservations;
    @FXML private TableColumn<ReservationPromo, String> colId;
    @FXML private TableColumn<ReservationPromo, String> colPromotion;
    @FXML private TableColumn<ReservationPromo, String> colDateDebut;
    @FXML private TableColumn<ReservationPromo, String> colDateFin;
    @FXML private TableColumn<ReservationPromo, String> colNbJours;
    @FXML private TableColumn<ReservationPromo, String> colMontantTotal;
    @FXML private TableColumn<ReservationPromo, String> colReserveLe;
    @FXML private TableColumn<ReservationPromo, Void> colActions;
    @FXML private VBox emptyState;

    private ReservationPromoService reservationService;
    private PromotionService promotionService;
    private ObservableList<ReservationPromo> reservations;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        reservationService = new ReservationPromoService();
        promotionService = new PromotionService();

        // Afficher nom du user
        txtUserName.setText(SessionManager.getCurrentUserName());

        setupTable();
        loadReservations();
    }

    private void setupTable() {
        // Colonne ID
        colId.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getId()))
        );

        // Colonne Promotion (nom)
        colPromotion.setCellValueFactory(cellData -> {
            int promoId = cellData.getValue().getPromotionId();
            Optional<Promotion> promo = promotionService.getById(promoId);
            return new SimpleStringProperty(promo.map(Promotion::getName).orElse("Inconnue"));
        });

        // Colonne Date Début
        colDateDebut.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDateDebutReservation().toString())
        );

        // Colonne Date Fin
        colDateFin.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDateFinReservation().toString())
        );

        // Colonne Nb Jours
        colNbJours.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getNbJours() + " jour(s)")
        );

        // Colonne Montant Total
        colMontantTotal.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.2f TND", cellData.getValue().getMontantTotal()))
        );

        // Colonne Réservé le (format relatif)
        colReserveLe.setCellValueFactory(cellData -> {
            LocalDateTime createdAt = cellData.getValue().getCreatedAt().toLocalDateTime();
            String timeAgo = getTimeAgo(createdAt);
            return new SimpleStringProperty(timeAgo);
        });

        // Colonne Actions (boutons)
        colActions.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    ReservationPromo reservation = getTableView().getItems().get(getIndex());
                    setGraphic(createActionButtons(reservation));
                }
            }
        });
    }

    /**
     * Créer les boutons d'action pour chaque ligne
     */
    private HBox createActionButtons(ReservationPromo reservation) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER);

        // Bouton Modifier
        Button btnModifier = new Button("✏️ Modifier");
        btnModifier.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; " +
                "-fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 6; " +
                "-fx-cursor: hand;");
        btnModifier.setOnAction(e -> handleModifier(reservation));

        // Bouton Annuler
        Button btnAnnuler = new Button("🗑️ Annuler");

        // Vérifier si peut annuler (< 24h)
        boolean canDelete = reservationService.canDelete(reservation);

        if (canDelete) {
            btnAnnuler.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; " +
                    "-fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 6; " +
                    "-fx-cursor: hand;");
            btnAnnuler.setOnAction(e -> handleAnnuler(reservation));
        } else {
            btnAnnuler.setStyle("-fx-background-color: #95A5A6; -fx-text-fill: white; " +
                    "-fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 6;");
            btnAnnuler.setDisable(true);

            Tooltip tooltip = new Tooltip("Impossible d'annuler après 24h");
            Tooltip.install(btnAnnuler, tooltip);
        }

        box.getChildren().addAll(btnModifier, btnAnnuler);
        return box;
    }

    /**
     * Charger les réservations du user connecté
     */
    private void loadReservations() {
        int userId = SessionManager.getCurrentUserId();
        reservations = FXCollections.observableArrayList(
                reservationService.getByUserId(userId)
        );

        tableReservations.setItems(reservations);
        txtTotalReservations.setText(String.valueOf(reservations.size()));

        // Afficher empty state si vide
        if (reservations.isEmpty()) {
            tableReservations.setVisible(false);
            emptyState.setVisible(true);
        } else {
            tableReservations.setVisible(true);
            emptyState.setVisible(false);
        }
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
        Stage stage = (Stage) tableReservations.getScene().getWindow();
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
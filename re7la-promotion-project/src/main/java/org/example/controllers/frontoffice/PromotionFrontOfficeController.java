package org.example.controllers.frontoffice;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import org.example.models.*;
import org.example.services.OffresStaticData;
import org.example.services.PromotionService;
import org.example.services.PromotionTargetService;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

public class PromotionFrontOfficeController implements Initializable {

    @FXML private TextField searchField;
    @FXML private CheckBox packCheckbox;
    @FXML private CheckBox individuCheckbox;
    @FXML private CheckBox activeCheckbox;
    @FXML private Text resultsCountText;
    @FXML private ComboBox<String> sortCombo;
    @FXML private FlowPane promotionsFlowPane;
    @FXML private Button btnMesReservations;

    private PromotionService promotionService;
    private PromotionTargetService targetService;
    private OffresStaticData offresData;

    private ObservableList<Promotion> allPromotions = FXCollections.observableArrayList();
    private ObservableList<Promotion> filteredPromotions = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        promotionService = new PromotionService();
        targetService = new PromotionTargetService();
        offresData = OffresStaticData.getInstance();

        setupFilters();
        loadPromotions();
        displayPromotions();
        setupListeners();
    }

    private void setupFilters() {
        packCheckbox.setSelected(true);
        individuCheckbox.setSelected(true);
        activeCheckbox.setSelected(true);

        packCheckbox.setOnAction(e -> applyFilters());
        individuCheckbox.setOnAction(e -> applyFilters());
        activeCheckbox.setOnAction(e -> applyFilters());

        sortCombo.setItems(FXCollections.observableArrayList(
                "Plus récentes", "Réduction croissante", "Réduction décroissante"
        ));
        sortCombo.setValue("Plus récentes");
        sortCombo.setOnAction(e -> {
            sortPromotions();
            displayPromotions();
        });
    }

    private void setupListeners() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void loadPromotions() {
        allPromotions.addAll(promotionService.getAll());
        filteredPromotions.addAll(allPromotions);
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        boolean showPacks = packCheckbox.isSelected();
        boolean showIndividual = individuCheckbox.isSelected();
        boolean onlyActive = activeCheckbox.isSelected();

        filteredPromotions.clear();

        LocalDate today = LocalDate.now();

        for (Promotion promo : allPromotions) {
            boolean matchSearch = searchText.isEmpty() ||
                    promo.getName().toLowerCase().contains(searchText) ||
                    promo.getDescription().toLowerCase().contains(searchText);

            boolean matchType = (promo.isPack() && showPacks) ||
                    (!promo.isPack() && showIndividual);

            boolean matchActive = !onlyActive ||
                    (promo.getStartDate().toLocalDate().isBefore(today.plusDays(1)) &&
                            promo.getEndDate().toLocalDate().isAfter(today.minusDays(1)));

            if (matchSearch && matchType && matchActive) {
                filteredPromotions.add(promo);
            }
        }

        sortPromotions();
        displayPromotions();
        updateResultsCount();
    }

    private void sortPromotions() {
        String sortBy = sortCombo.getValue();

        switch (sortBy) {
            case "Réduction croissante":
                filteredPromotions.sort((p1, p2) -> {
                    float val1 = p1.getDiscountPercentage() != null ? p1.getDiscountPercentage() : 0;
                    float val2 = p2.getDiscountPercentage() != null ? p2.getDiscountPercentage() : 0;
                    return Float.compare(val1, val2);
                });
                break;
            case "Réduction décroissante":
                filteredPromotions.sort((p1, p2) -> {
                    float val1 = p1.getDiscountPercentage() != null ? p1.getDiscountPercentage() : 0;
                    float val2 = p2.getDiscountPercentage() != null ? p2.getDiscountPercentage() : 0;
                    return Float.compare(val2, val1);
                });
                break;
            default:
                filteredPromotions.sort((p1, p2) ->
                        p2.getStartDate().compareTo(p1.getStartDate())
                );
                break;
        }
    }

    private void displayPromotions() {
        promotionsFlowPane.getChildren().clear();

        for (Promotion promo : filteredPromotions) {
            VBox card = createPromotionCard(promo);
            promotionsFlowPane.getChildren().add(card);
        }

        if (filteredPromotions.isEmpty()) {
            VBox emptyState = new VBox(20);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPrefSize(600, 400);

            Text emoji = new Text("🎁");
            emoji.setStyle("-fx-font-size: 72px;");

            Text message = new Text("Aucune promotion trouvée");
            message.setStyle("-fx-font-size: 20px; -fx-fill: #7F8C8D; -fx-font-weight: bold;");

            Text suggestion = new Text("Essayez de modifier vos critères de recherche");
            suggestion.setStyle("-fx-font-size: 14px; -fx-fill: #95A5A6;");

            emptyState.getChildren().addAll(emoji, message, suggestion);
            promotionsFlowPane.getChildren().add(emptyState);
        }
    }

    private VBox createPromotionCard(Promotion promo) {
        VBox card = new VBox();
        card.getStyleClass().add("activity-card");
        card.setPrefWidth(340);
        card.setMaxWidth(340);
        card.setCursor(javafx.scene.Cursor.HAND);

        StackPane headerPane = new StackPane();
        headerPane.setPrefHeight(180);
        String gradient = promo.isPack() ?
                "linear-gradient(to bottom right, #F39C12, #F7DC6F)" :
                "linear-gradient(to bottom right, #1ABC9C, #3498DB)";
        headerPane.setStyle("-fx-background-color: " + gradient + "; -fx-background-radius: 15 15 0 0;");

        Text icon = new Text(promo.isPack() ? "📦" : "🎁");
        icon.setStyle("-fx-font-size: 64px;");
        headerPane.getChildren().add(icon);

        Label typeBadge = new Label(promo.isPack() ? "PACK" : "PROMO");
        typeBadge.setStyle("-fx-background-color: white; -fx-text-fill: " +
                (promo.isPack() ? "#F39C12" : "#1ABC9C") +
                "; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-size: 11px;");
        StackPane.setAlignment(typeBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(typeBadge, new Insets(15));
        headerPane.getChildren().add(typeBadge);

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 15 15;");

        Text title = new Text(promo.getName());
        title.setStyle("-fx-font-size: 18px; -fx-fill: #2C3E50; -fx-font-weight: bold;");
        title.setWrappingWidth(300);

        Text description = new Text(promo.getDescription());
        description.setStyle("-fx-font-size: 13px; -fx-fill: #64748B;");
        description.setWrappingWidth(300);

        HBox discountBox = new HBox(10);
        discountBox.setAlignment(Pos.CENTER_LEFT);

        if (promo.getDiscountPercentage() != null) {
            Text discount = new Text("-" + String.format("%.0f", promo.getDiscountPercentage()) + "%");
            discount.setStyle("-fx-font-size: 24px; -fx-fill: #F39C12; -fx-font-weight: bold;");
            discountBox.getChildren().add(discount);
        }

        if (promo.getDiscountFixed() != null) {
            Text discount = new Text("-" + String.format("%.0f", promo.getDiscountFixed()) + " TND");
            discount.setStyle("-fx-font-size: 20px; -fx-fill: #E67E22; -fx-font-weight: bold;");
            discountBox.getChildren().add(discount);
        }

        HBox datesBox = new HBox(15);
        datesBox.setAlignment(Pos.CENTER_LEFT);

        VBox startBox = new VBox(2);
        Text startLabel = new Text("Début");
        startLabel.setStyle("-fx-font-size: 11px; -fx-fill: #7F8C8D;");
        Text startDate = new Text(promo.getStartDate().toString());
        startDate.setStyle("-fx-font-size: 12px; -fx-fill: #2C3E50; -fx-font-weight: bold;");
        startBox.getChildren().addAll(startLabel, startDate);

        VBox endBox = new VBox(2);
        Text endLabel = new Text("Fin");
        endLabel.setStyle("-fx-font-size: 11px; -fx-fill: #7F8C8D;");
        Text endDate = new Text(promo.getEndDate().toString());
        endDate.setStyle("-fx-font-size: 12px; -fx-fill: #2C3E50; -fx-font-weight: bold;");
        endBox.getChildren().addAll(endLabel, endDate);

        datesBox.getChildren().addAll(startBox, new Separator(javafx.geometry.Orientation.VERTICAL), endBox);

        if (promo.isPack()) {
            List<PromotionTarget> targets = targetService.getByPromotionId(promo.getId());
            if (!targets.isEmpty()) {
                VBox targetsBox = new VBox(5);
                Label targetsLabel = new Label("Inclus dans ce pack:");
                targetsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7F8C8D; -fx-font-weight: bold;");
                targetsBox.getChildren().add(targetsLabel);

                for (PromotionTarget target : targets) {
                    String emoji = switch (target.getTargetType()) {
                        case HEBERGEMENT -> "🏨";
                        case ACTIVITE -> "🎯";
                        case TRANSPORT -> "🚗";
                    };
                    Label targetLabel = new Label(emoji + " " + target.getTargetType().name());
                    targetLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2C3E50;");
                    targetsBox.getChildren().add(targetLabel);
                }

                content.getChildren().add(targetsBox);
            }
        }

        Button reserveBtn = new Button("Réserver cette promotion ➜");
        reserveBtn.setStyle("-fx-background-color: #1ABC9C; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 12 20; -fx-background-radius: 8; -fx-cursor: hand;");
        reserveBtn.setMaxWidth(Double.MAX_VALUE);
        reserveBtn.setOnAction(e -> openReservationDialog(promo));

        LocalDate today = LocalDate.now();
        if (promo.getEndDate().toLocalDate().isBefore(today)) {
            reserveBtn.setDisable(true);
            reserveBtn.setStyle("-fx-background-color: #95A5A6; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-padding: 12 20; -fx-background-radius: 8;");
            reserveBtn.setText("Promotion expirée");
        }

        content.getChildren().addAll(title, description, new Separator(), discountBox, datesBox, reserveBtn);
        card.getChildren().addAll(headerPane, content);

        return card;
    }

    private void openReservationDialog(Promotion promo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/ReservationDialog.fxml"));
            Parent root = loader.load();

            ReservationDialogController controller = loader.getController();
            controller.setPromotion(promo);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Réserver : " + promo.getName());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            dialogStage.showAndWait();

        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture du dialog de réservation");
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Impossible d'ouvrir le formulaire de réservation.");
            alert.showAndWait();
        }
    }

    private void updateResultsCount() {
        int count = filteredPromotions.size();
        resultsCountText.setText(count + " promotion" + (count > 1 ? "s" : "") + " trouvée" + (count > 1 ? "s" : ""));
    }

    @FXML
    private void resetFilters() {
        searchField.clear();
        packCheckbox.setSelected(true);
        individuCheckbox.setSelected(true);
        activeCheckbox.setSelected(true);
        sortCombo.setValue("Plus récentes");
        applyFilters();
    }

    @FXML
    private void handleMesReservations() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/MesReservations.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Mes Réservations");
            stage.setScene(new Scene(root, 1000, 700));
            stage.show();

        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture de Mes Réservations");
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText("Impossible d'ouvrir la page Mes Réservations.");
            alert.showAndWait();
        }
    }
}
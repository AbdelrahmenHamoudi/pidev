package controllers.frontoffice;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.Activite;
import models.Planning;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ActiviteDetailsController {
    
    @FXML private Text activityIcon;
    @FXML private Label typeBadge;
    @FXML private Label statusBadge;
    @FXML private Text activityTitle;
    @FXML private Text activityLocation;
    @FXML private Text activityDescription;
    @FXML private Text activityPrice;
    @FXML private Text activityCapacity;
    @FXML private ComboBox<String> dateFilterCombo;
    @FXML private VBox planningsContainer;
    @FXML private VBox emptyPlanningsState;
    
    private Activite currentActivite;
    private List<Planning> planningsList = new ArrayList<>();
    
    public void setActivite(Activite activite) {
        this.currentActivite = activite;
        displayActiviteInfo();
        loadPlannings();
    }
    
    private void displayActiviteInfo() {
        activityIcon.setText(getIconForType(currentActivite.getType()));
        typeBadge.setText(currentActivite.getType());
        
        statusBadge.setText(currentActivite.getStatut());
        if (currentActivite.getStatut().equals("Disponible")) {
            statusBadge.getStyleClass().clear();
            statusBadge.getStyleClass().add("activity-status-available");
        } else {
            statusBadge.getStyleClass().clear();
            statusBadge.getStyleClass().add("activity-status-complet");
        }
        
        activityTitle.setText(currentActivite.getNomA());
        activityLocation.setText(currentActivite.getLieu());
        activityDescription.setText(currentActivite.getDescriptionA());
        activityPrice.setText(String.format("%.0f DT", currentActivite.getPrixParPersonne()));
        activityCapacity.setText(currentActivite.getCapaciteMax() + " personnes");
    }
    
    private void loadPlannings() {
        // TODO: Charger depuis BD
        // Exemple de données
        planningsList.add(new Planning(1, currentActivite.getIdActivite(), 
            LocalDate.now().plusDays(3), LocalTime.of(9, 0), LocalTime.of(12, 0), "Disponible", 15));
        planningsList.add(new Planning(2, currentActivite.getIdActivite(), 
            LocalDate.now().plusDays(5), LocalTime.of(14, 0), LocalTime.of(17, 0), "Disponible", 8));
        planningsList.add(new Planning(3, currentActivite.getIdActivite(), 
            LocalDate.now().plusDays(7), LocalTime.of(10, 0), LocalTime.of(13, 0), "Disponible", 12));
        planningsList.add(new Planning(4, currentActivite.getIdActivite(), 
            LocalDate.now().plusDays(10), LocalTime.of(8, 30), LocalTime.of(11, 30), "Disponible", 20));
        planningsList.add(new Planning(5, currentActivite.getIdActivite(), 
            LocalDate.now().plusDays(12), LocalTime.of(15, 0), LocalTime.of(18, 0), "Disponible", 6));
        
        setupDateFilter();
        displayPlannings(planningsList);
    }
    
    private void setupDateFilter() {
        List<String> dates = planningsList.stream()
            .map(p -> p.getDatePlanning().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        
        dates.add(0, "Toutes les dates");
        dateFilterCombo.setItems(FXCollections.observableArrayList(dates));
        dateFilterCombo.setValue("Toutes les dates");
        
        dateFilterCombo.setOnAction(e -> {
            String selected = dateFilterCombo.getValue();
            if (selected.equals("Toutes les dates")) {
                displayPlannings(planningsList);
            } else {
                List<Planning> filtered = planningsList.stream()
                    .filter(p -> p.getDatePlanning().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")).equals(selected))
                    .collect(Collectors.toList());
                displayPlannings(filtered);
            }
        });
    }
    
    private void displayPlannings(List<Planning> plannings) {
        planningsContainer.getChildren().clear();
        
        if (plannings.isEmpty()) {
            emptyPlanningsState.setVisible(true);
            emptyPlanningsState.setManaged(true);
            return;
        }
        
        emptyPlanningsState.setVisible(false);
        emptyPlanningsState.setManaged(false);
        
        for (Planning planning : plannings) {
            HBox planningCard = createPlanningCard(planning);
            planningsContainer.getChildren().add(planningCard);
        }
    }
    
    private HBox createPlanningCard(Planning planning) {
        HBox card = new HBox(20);
        card.getStyleClass().add("planning-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefHeight(100);
        
        // Date Section
        VBox dateSection = new VBox(5);
        dateSection.setAlignment(Pos.CENTER);
        dateSection.setPrefWidth(100);
        dateSection.setStyle("-fx-background-color: #F39C12; -fx-background-radius: 8; -fx-padding: 10;");
        
        String dayOfMonth = planning.getDatePlanning().format(DateTimeFormatter.ofPattern("dd"));
        String monthYear = planning.getDatePlanning().format(DateTimeFormatter.ofPattern("MMM yyyy"));
        String dayOfWeek = planning.getDatePlanning().format(DateTimeFormatter.ofPattern("EEEE"));
        
        Text dateDay = new Text(dayOfMonth);
        dateDay.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-fill: white;");
        
        Text dateMonth = new Text(monthYear);
        dateMonth.setStyle("-fx-font-size: 14px; -fx-fill: white;");
        
        dateSection.getChildren().addAll(dateDay, dateMonth);
        
        // Info Section
        VBox infoSection = new VBox(8);
        infoSection.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoSection, Priority.ALWAYS);
        
        Text dayText = new Text(dayOfWeek);
        dayText.getStyleClass().add("planning-date");
        
        HBox timeBox = new HBox(8);
        timeBox.setAlignment(Pos.CENTER_LEFT);
        Text timeIcon = new Text("🕐");
        Text timeText = new Text(String.format("%s - %s", 
            planning.getHeureDebut().format(DateTimeFormatter.ofPattern("HH:mm")),
            planning.getHeureFin().format(DateTimeFormatter.ofPattern("HH:mm"))));
        timeText.getStyleClass().add("planning-time");
        timeBox.getChildren().addAll(timeIcon, timeText);
        
        HBox placesBox = new HBox(8);
        placesBox.setAlignment(Pos.CENTER_LEFT);
        Text placesIcon = new Text("👥");
        Text placesText = new Text(planning.getNbPlacesRestantes() + " places restantes");
        placesText.getStyleClass().add("planning-places");
        placesBox.getChildren().addAll(placesIcon, placesText);
        
        infoSection.getChildren().addAll(dayText, timeBox, placesBox);
        
        // Action Section
        VBox actionSection = new VBox(10);
        actionSection.setAlignment(Pos.CENTER);
        actionSection.setPrefWidth(150);
        
        Button reserveBtn = new Button("Réserver");
        reserveBtn.setStyle("-fx-background-color: #1ABC9C; -fx-text-fill: white; -fx-font-weight: bold; " +
                           "-fx-padding: 12 25; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14px;");
        reserveBtn.setMaxWidth(Double.MAX_VALUE);
        
        if (planning.getNbPlacesRestantes() == 0 || !planning.getEtat().equals("Disponible")) {
            reserveBtn.setDisable(true);
            reserveBtn.setStyle("-fx-background-color: #95A5A6; -fx-text-fill: white; -fx-font-weight: bold; " +
                              "-fx-padding: 12 25; -fx-background-radius: 8; -fx-font-size: 14px;");
            reserveBtn.setText("Complet");
        }
        
        reserveBtn.setOnAction(e -> handleReservation(planning));
        
        // Availability indicator
        HBox availabilityBox = new HBox(5);
        availabilityBox.setAlignment(Pos.CENTER);
        
        Region indicator = new Region();
        indicator.setPrefSize(10, 10);
        indicator.setStyle("-fx-background-radius: 5;");
        
        Text availabilityText = new Text();
        availabilityText.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        
        if (planning.getNbPlacesRestantes() > 10) {
            indicator.setStyle("-fx-background-color: #27AE60; -fx-background-radius: 5;");
            availabilityText.setText("Disponible");
            availabilityText.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-fill: #27AE60;");
        } else if (planning.getNbPlacesRestantes() > 0) {
            indicator.setStyle("-fx-background-color: #F39C12; -fx-background-radius: 5;");
            availabilityText.setText("Peu de places");
            availabilityText.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-fill: #F39C12;");
        } else {
            indicator.setStyle("-fx-background-color: #E74C3C; -fx-background-radius: 5;");
            availabilityText.setText("Complet");
            availabilityText.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-fill: #E74C3C;");
        }
        
        availabilityBox.getChildren().addAll(indicator, availabilityText);
        
        actionSection.getChildren().addAll(reserveBtn, availabilityBox);
        
        card.getChildren().addAll(dateSection, infoSection, actionSection);
        
        return card;
    }
    
    private void handleReservation(Planning planning) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de réservation");
        alert.setHeaderText("Réserver cette activité?");
        
        String content = String.format(
            "Activité: %s\n" +
            "Date: %s\n" +
            "Horaire: %s - %s\n" +
            "Prix: %.0f DT par personne\n\n" +
            "Voulez-vous continuer avec la réservation?",
            currentActivite.getNomA(),
            planning.getDatePlanning().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            planning.getHeureDebut().format(DateTimeFormatter.ofPattern("HH:mm")),
            planning.getHeureFin().format(DateTimeFormatter.ofPattern("HH:mm")),
            currentActivite.getPrixParPersonne()
        );
        
        alert.setContentText(content);
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // TODO: Implémenter la logique de réservation
                showSuccess("Réservation confirmée!", 
                    "Votre réservation a été enregistrée avec succès. Vous recevrez un email de confirmation.");
            }
        });
    }
    
    private String getIconForType(String type) {
        switch (type) {
            case "Aventure": return "⛰️";
            case "Sport": return "🏊";
            case "Culture": return "🏛️";
            case "Détente": return "🧘";
            case "Excursion": return "🐎";
            default: return "🎯";
        }
    }
    
    @FXML
    private void goBack() {
        Stage stage = (Stage) activityTitle.getScene().getWindow();
        stage.close();
    }
    
    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

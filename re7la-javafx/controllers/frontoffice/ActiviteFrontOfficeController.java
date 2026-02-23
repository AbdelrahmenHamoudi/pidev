package controllers.frontoffice;

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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.Activite;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class ActiviteFrontOfficeController implements Initializable {
    
    @FXML private TextField searchField;
    @FXML private VBox typeFiltersBox;
    @FXML private Slider prixSlider;
    @FXML private Text prixText;
    @FXML private ComboBox<String> lieuCombo;
    @FXML private CheckBox disponibleCheckbox;
    @FXML private Text resultsCountText;
    @FXML private ComboBox<String> sortCombo;
    @FXML private FlowPane activitiesFlowPane;
    
    private ObservableList<Activite> allActivites = FXCollections.observableArrayList();
    private ObservableList<Activite> filteredActivites = FXCollections.observableArrayList();
    private Map<String, CheckBox> typeCheckBoxes = new HashMap<>();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFilters();
        loadActivites();
        displayActivites();
        setupListeners();
    }
    
    private void setupFilters() {
        // Type filters
        String[] types = {"Excursion", "Sport", "Culture", "Aventure", "Détente"};
        for (String type : types) {
            CheckBox cb = new CheckBox(type);
            cb.setStyle("-fx-text-fill: #2C3E50; -fx-font-size: 13px;");
            cb.setSelected(true);
            cb.setOnAction(e -> applyFilters());
            typeCheckBoxes.put(type, cb);
            typeFiltersBox.getChildren().add(cb);
        }
        
        // Prix slider
        prixSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            prixText.setText(String.format("0 - %.0f DT", newVal.doubleValue()));
            applyFilters();
        });
        
        // Lieu combo
        lieuCombo.setItems(FXCollections.observableArrayList("Tous les lieux"));
        lieuCombo.setValue("Tous les lieux");
        lieuCombo.setOnAction(e -> applyFilters());
        
        // Disponibilité
        disponibleCheckbox.setOnAction(e -> applyFilters());
        
        // Sort combo
        sortCombo.setOnAction(e -> {
            sortActivites();
            displayActivites();
        });
    }
    
    private void setupListeners() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }
    
    private void loadActivites() {
        // TODO: Charger depuis BD
        allActivites.add(new Activite(1, "Randonnée Montagneuse", 
            "Explorez les magnifiques sommets du Nord tunisien avec un guide expérimenté. Paysages à couper le souffle garantis!", 
            "Ain Draham", 45.0f, 20, "Aventure", "Disponible", "hiking.jpg"));
        
        allActivites.add(new Activite(2, "Plongée Sous-Marine", 
            "Découvrez la richesse des fonds marins méditerranéens. Équipement fourni, tous niveaux acceptés.", 
            "Tabarka", 80.0f, 12, "Sport", "Disponible", "diving.jpg"));
        
        allActivites.add(new Activite(3, "Visite Médina", 
            "Tour guidé de la médina historique de Tunis. Découvrez l'architecture traditionnelle et l'artisanat local.", 
            "Tunis", 25.0f, 30, "Culture", "Disponible", "medina.jpg"));
        
        allActivites.add(new Activite(4, "Safari Désert", 
            "Aventure exceptionnelle dans le Sahara tunisien. Nuit sous les étoiles et repas traditionnel inclus.", 
            "Douz", 120.0f, 15, "Aventure", "Complet", "desert.jpg"));
        
        allActivites.add(new Activite(5, "Yoga au Lever du Soleil", 
            "Séance de yoga relaxante face à la mer. Parfait pour commencer la journée en harmonie.", 
            "Hammamet", 35.0f, 25, "Détente", "Disponible", "yoga.jpg"));
        
        allActivites.add(new Activite(6, "Kayak en Mer", 
            "Pagayez le long de la côte et explorez des criques cachées. Idéal pour les amateurs d'aventure aquatique.", 
            "Sousse", 50.0f, 16, "Sport", "Disponible", "kayak.jpg"));
        
        allActivites.add(new Activite(7, "Atelier Poterie", 
            "Apprenez l'art traditionnel de la poterie avec un artisan local. Créez votre propre pièce unique!", 
            "Nabeul", 40.0f, 10, "Culture", "Disponible", "pottery.jpg"));
        
        allActivites.add(new Activite(8, "Balade à Cheval", 
            "Promenade équestre à travers les forêts et collines. Convient aux débutants comme aux cavaliers confirmés.", 
            "Ain Draham", 60.0f, 8, "Excursion", "Disponible", "horse.jpg"));
        
        // Ajouter les lieux au combo
        Set<String> lieux = allActivites.stream()
            .map(Activite::getLieu)
            .collect(Collectors.toSet());
        lieuCombo.getItems().addAll(lieux);
        
        filteredActivites.addAll(allActivites);
    }
    
    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        double maxPrix = prixSlider.getValue();
        String lieuFilter = lieuCombo.getValue();
        boolean onlyDisponible = disponibleCheckbox.isSelected();
        
        Set<String> selectedTypes = typeCheckBoxes.entrySet().stream()
            .filter(entry -> entry.getValue().isSelected())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        
        filteredActivites.clear();
        
        for (Activite activite : allActivites) {
            boolean matchSearch = searchText.isEmpty() || 
                activite.getNomA().toLowerCase().contains(searchText) ||
                activite.getDescriptionA().toLowerCase().contains(searchText) ||
                activite.getLieu().toLowerCase().contains(searchText);
            
            boolean matchType = selectedTypes.contains(activite.getType());
            boolean matchPrix = activite.getPrixParPersonne() <= maxPrix;
            boolean matchLieu = lieuFilter.equals("Tous les lieux") || activite.getLieu().equals(lieuFilter);
            boolean matchDisponible = !onlyDisponible || activite.getStatut().equals("Disponible");
            
            if (matchSearch && matchType && matchPrix && matchLieu && matchDisponible) {
                filteredActivites.add(activite);
            }
        }
        
        sortActivites();
        displayActivites();
        updateResultsCount();
    }
    
    private void sortActivites() {
        String sortBy = sortCombo.getValue();
        
        switch (sortBy) {
            case "Prix croissant":
                filteredActivites.sort(Comparator.comparing(Activite::getPrixParPersonne));
                break;
            case "Prix décroissant":
                filteredActivites.sort(Comparator.comparing(Activite::getPrixParPersonne).reversed());
                break;
            case "Nom A-Z":
                filteredActivites.sort(Comparator.comparing(Activite::getNomA));
                break;
            default:
                // Popularité - keep current order or implement custom logic
                break;
        }
    }
    
    private void displayActivites() {
        activitiesFlowPane.getChildren().clear();
        
        for (Activite activite : filteredActivites) {
            VBox card = createActivityCard(activite);
            activitiesFlowPane.getChildren().add(card);
        }
        
        if (filteredActivites.isEmpty()) {
            VBox emptyState = new VBox(20);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPrefSize(600, 400);
            
            Text emoji = new Text("🔍");
            emoji.setStyle("-fx-font-size: 72px;");
            
            Text message = new Text("Aucune activité trouvée");
            message.setStyle("-fx-font-size: 20px; -fx-fill: #7F8C8D; -fx-font-weight: bold;");
            
            Text suggestion = new Text("Essayez de modifier vos critères de recherche");
            suggestion.setStyle("-fx-font-size: 14px; -fx-fill: #95A5A6;");
            
            emptyState.getChildren().addAll(emoji, message, suggestion);
            activitiesFlowPane.getChildren().add(emptyState);
        }
    }
    
    private VBox createActivityCard(Activite activite) {
        VBox card = new VBox();
        card.getStyleClass().add("activity-card");
        card.setPrefWidth(320);
        card.setMaxWidth(320);
        card.setCursor(javafx.scene.Cursor.HAND);
        
        // Image placeholder
        StackPane imagePane = new StackPane();
        imagePane.setPrefHeight(200);
        imagePane.setStyle("-fx-background-color: linear-gradient(to bottom right, #F39C12, #F7DC6F); -fx-background-radius: 15 15 0 0;");
        
        // Image icon
        Text imageIcon = new Text(getIconForType(activite.getType()));
        imageIcon.setStyle("-fx-font-size: 64px;");
        imagePane.getChildren().add(imageIcon);
        
        // Type badge
        Label typeBadge = new Label(activite.getType());
        typeBadge.getStyleClass().add("activity-type-badge");
        StackPane.setAlignment(typeBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(typeBadge, new Insets(15));
        imagePane.getChildren().add(typeBadge);
        
        // Content
        VBox content = new VBox(12);
        content.getStyleClass().add("activity-content");
        
        // Title and location
        Text title = new Text(activite.getNomA());
        title.getStyleClass().add("activity-title");
        title.setWrappingWidth(280);
        
        HBox locationBox = new HBox(5);
        locationBox.setAlignment(Pos.CENTER_LEFT);
        Text locationIcon = new Text("📍");
        Text location = new Text(activite.getLieu());
        location.getStyleClass().add("activity-location");
        locationBox.getChildren().addAll(locationIcon, location);
        
        // Description
        Text description = new Text(activite.getDescriptionA());
        description.getStyleClass().add("activity-description");
        description.setWrappingWidth(280);
        
        // Price and capacity
        HBox infoBox = new HBox(15);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        
        VBox priceBox = new VBox(2);
        Text priceLabel = new Text("Prix");
        priceLabel.setStyle("-fx-font-size: 11px; -fx-fill: #7F8C8D;");
        Text price = new Text(String.format("%.0f DT", activite.getPrixParPersonne()));
        price.getStyleClass().add("activity-price");
        priceBox.getChildren().addAll(priceLabel, price);
        
        VBox capacityBox = new VBox(2);
        Text capacityLabel = new Text("Capacité");
        capacityLabel.setStyle("-fx-font-size: 11px; -fx-fill: #7F8C8D;");
        Text capacity = new Text(activite.getCapaciteMax() + " pers.");
        capacity.setStyle("-fx-font-size: 14px; -fx-fill: #2C3E50; -fx-font-weight: bold;");
        capacityBox.getChildren().addAll(capacityLabel, capacity);
        
        infoBox.getChildren().addAll(priceBox, new Separator(javafx.geometry.Orientation.VERTICAL), capacityBox);
        
        // Status and button
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);
        
        Label statusBadge = new Label(activite.getStatut());
        if (activite.getStatut().equals("Disponible")) {
            statusBadge.getStyleClass().add("activity-status-available");
        } else {
            statusBadge.getStyleClass().add("activity-status-complet");
        }
        
        Button viewBtn = new Button("Voir les plannings ➜");
        viewBtn.setStyle("-fx-background-color: #1ABC9C; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(viewBtn, Priority.ALWAYS);
        
        if (!activite.getStatut().equals("Disponible")) {
            viewBtn.setDisable(true);
            viewBtn.setStyle("-fx-background-color: #95A5A6; -fx-text-fill: white; -fx-font-weight: bold; " +
                           "-fx-padding: 10 20; -fx-background-radius: 8;");
        }
        
        viewBtn.setOnAction(e -> openActiviteDetails(activite));
        
        actionBox.getChildren().addAll(statusBadge, viewBtn);
        
        content.getChildren().addAll(title, locationBox, description, new Separator(), infoBox, actionBox);
        card.getChildren().addAll(imagePane, content);
        
        return card;
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
    
    private void openActiviteDetails(Activite activite) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../../views/frontoffice/ActiviteDetails.fxml"));
            Parent root = loader.load();
            
            ActiviteDetailsController controller = loader.getController();
            controller.setActivite(activite);
            
            Stage stage = new Stage();
            stage.setTitle("Détails - " + activite.getNomA());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir les détails de l'activité.");
        }
    }
    
    private void updateResultsCount() {
        int count = filteredActivites.size();
        resultsCountText.setText(count + " activité" + (count > 1 ? "s" : "") + " trouvée" + (count > 1 ? "s" : ""));
    }
    
    @FXML
    private void resetFilters() {
        searchField.clear();
        prixSlider.setValue(200);
        lieuCombo.setValue("Tous les lieux");
        disponibleCheckbox.setSelected(true);
        typeCheckBoxes.values().forEach(cb -> cb.setSelected(true));
        sortCombo.setValue("Popularité");
        applyFilters();
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

package controllers.backoffice;

import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Activite;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class ActiviteBackOfficeController implements Initializable {
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterTypeCombo;
    @FXML private ComboBox<String> filterStatutCombo;
    
    @FXML private Text totalActivitesText;
    @FXML private Text activitesActivesText;
    @FXML private Text capaciteTotaleText;
    
    @FXML private TableView<Activite> activitesTable;
    @FXML private TableColumn<Activite, Integer> idColumn;
    @FXML private TableColumn<Activite, String> nomColumn;
    @FXML private TableColumn<Activite, String> lieuColumn;
    @FXML private TableColumn<Activite, String> typeColumn;
    @FXML private TableColumn<Activite, Float> prixColumn;
    @FXML private TableColumn<Activite, Integer> capaciteColumn;
    @FXML private TableColumn<Activite, String> statutColumn;
    @FXML private TableColumn<Activite, Void> actionsColumn;
    
    private ObservableList<Activite> activitesList = FXCollections.observableArrayList();
    private ObservableList<Activite> filteredList = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupFilters();
        setupSearchListener();
        loadActivitesData();
        updateStatistics();
    }
    
    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("idActivite"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nomA"));
        lieuColumn.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        prixColumn.setCellValueFactory(new PropertyValueFactory<>("prixParPersonne"));
        capaciteColumn.setCellValueFactory(new PropertyValueFactory<>("capaciteMax"));
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));
        
        // Format price column
        prixColumn.setCellFactory(column -> new TableCell<Activite, Float>() {
            @Override
            protected void updateItem(Float item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f DT", item));
                }
            }
        });
        
        // Style statut column
        statutColumn.setCellFactory(column -> new TableCell<Activite, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equalsIgnoreCase("Disponible")) {
                        setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
                    } else if (item.equalsIgnoreCase("Complet")) {
                        setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #F39C12; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        // Actions column
        actionsColumn.setCellFactory(column -> new TableCell<Activite, Void>() {
            private final Button viewBtn = new Button("👁️");
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final Button planningBtn = new Button("📅");
            
            {
                viewBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
                editBtn.setStyle("-fx-background-color: #F39C12; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
                planningBtn.setStyle("-fx-background-color: #1ABC9C; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
                
                viewBtn.setOnAction(event -> {
                    Activite activite = getTableView().getItems().get(getIndex());
                    viewActivite(activite);
                });
                
                editBtn.setOnAction(event -> {
                    Activite activite = getTableView().getItems().get(getIndex());
                    editActivite(activite);
                });
                
                deleteBtn.setOnAction(event -> {
                    Activite activite = getTableView().getItems().get(getIndex());
                    deleteActivite(activite);
                });
                
                planningBtn.setOnAction(event -> {
                    Activite activite = getTableView().getItems().get(getIndex());
                    managePlanning(activite);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, viewBtn, editBtn, planningBtn, deleteBtn);
                    buttons.setAlignment(Pos.CENTER);
                    setGraphic(buttons);
                }
            }
        });
        
        activitesTable.setItems(filteredList);
    }
    
    private void setupFilters() {
        filterTypeCombo.setItems(FXCollections.observableArrayList(
            "Tous", "Excursion", "Sport", "Culture", "Aventure", "Détente"
        ));
        filterTypeCombo.setValue("Tous");
        
        filterStatutCombo.setItems(FXCollections.observableArrayList(
            "Tous", "Disponible", "Complet", "Suspendu"
        ));
        filterStatutCombo.setValue("Tous");
        
        filterTypeCombo.setOnAction(e -> applyFilters());
        filterStatutCombo.setOnAction(e -> applyFilters());
    }
    
    private void setupSearchListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }
    
    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String typeFilter = filterTypeCombo.getValue();
        String statutFilter = filterStatutCombo.getValue();
        
        filteredList.clear();
        
        for (Activite activite : activitesList) {
            boolean matchSearch = searchText.isEmpty() || 
                activite.getNomA().toLowerCase().contains(searchText) ||
                activite.getLieu().toLowerCase().contains(searchText);
            
            boolean matchType = typeFilter.equals("Tous") || activite.getType().equals(typeFilter);
            boolean matchStatut = statutFilter.equals("Tous") || activite.getStatut().equals(statutFilter);
            
            if (matchSearch && matchType && matchStatut) {
                filteredList.add(activite);
            }
        }
    }
    
    private void loadActivitesData() {
        // TODO: Remplacer par votre service de base de données
        // Exemple de données de test
        activitesList.add(new Activite(1, "Randonnée Montagneuse", "Explorez les sommets du Nord", "Ain Draham", 45.0f, 20, "Aventure", "Disponible", "hiking.jpg"));
        activitesList.add(new Activite(2, "Plongée Sous-Marine", "Découvrez les fonds marins", "Tabarka", 80.0f, 12, "Sport", "Disponible", "diving.jpg"));
        activitesList.add(new Activite(3, "Visite Médina", "Tour guidé de la médina historique", "Tunis", 25.0f, 30, "Culture", "Disponible", "medina.jpg"));
        activitesList.add(new Activite(4, "Safari Désert", "Aventure dans le Sahara", "Douz", 120.0f, 15, "Aventure", "Complet", "desert.jpg"));
        activitesList.add(new Activite(5, "Yoga au Lever du Soleil", "Séance de yoga relaxante", "Hammamet", 35.0f, 25, "Détente", "Disponible", "yoga.jpg"));
        
        filteredList.addAll(activitesList);
    }
    
    private void updateStatistics() {
        int total = activitesList.size();
        long actives = activitesList.stream()
            .filter(a -> a.getStatut().equals("Disponible"))
            .count();
        int capaciteTotale = activitesList.stream()
            .mapToInt(Activite::getCapaciteMax)
            .sum();
        
        totalActivitesText.setText(String.valueOf(total));
        activitesActivesText.setText(String.valueOf(actives));
        capaciteTotaleText.setText(String.valueOf(capaciteTotale));
    }
    
    @FXML
    private void showAddActiviteDialog() {
        Dialog<Activite> dialog = createActiviteDialog(null);
        Optional<Activite> result = dialog.showAndWait();
        
        result.ifPresent(activite -> {
            // TODO: Ajouter à la base de données
            activite.setIdActivite(activitesList.size() + 1);
            activitesList.add(activite);
            applyFilters();
            updateStatistics();
            showSuccess("Activité ajoutée avec succès!");
        });
    }
    
    private void viewActivite(Activite activite) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de l'activité");
        alert.setHeaderText(activite.getNomA());
        
        String content = String.format(
            "ID: %d\n" +
            "Lieu: %s\n" +
            "Type: %s\n" +
            "Prix: %.2f DT\n" +
            "Capacité: %d personnes\n" +
            "Statut: %s\n\n" +
            "Description:\n%s",
            activite.getIdActivite(),
            activite.getLieu(),
            activite.getType(),
            activite.getPrixParPersonne(),
            activite.getCapaciteMax(),
            activite.getStatut(),
            activite.getDescriptionA()
        );
        
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private void editActivite(Activite activite) {
        Dialog<Activite> dialog = createActiviteDialog(activite);
        Optional<Activite> result = dialog.showAndWait();
        
        result.ifPresent(updatedActivite -> {
            // TODO: Mettre à jour dans la base de données
            activitesList.set(activitesList.indexOf(activite), updatedActivite);
            applyFilters();
            updateStatistics();
            showSuccess("Activité modifiée avec succès!");
        });
    }
    
    private void deleteActivite(Activite activite) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'activité?");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer \"" + activite.getNomA() + "\"?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // TODO: Supprimer de la base de données
            activitesList.remove(activite);
            applyFilters();
            updateStatistics();
            showSuccess("Activité supprimée avec succès!");
        }
    }
    
    private void managePlanning(Activite activite) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Gestion du Planning");
        alert.setHeaderText("Planning pour: " + activite.getNomA());
        alert.setContentText("Interface de gestion du planning à implémenter.\nVous pouvez ouvrir une nouvelle fenêtre avec PlanningBackOffice.fxml");
        alert.showAndWait();
    }
    
    private Dialog<Activite> createActiviteDialog(Activite activite) {
        Dialog<Activite> dialog = new Dialog<>();
        dialog.setTitle(activite == null ? "Nouvelle Activité" : "Modifier l'Activité");
        dialog.setHeaderText(activite == null ? "Ajouter une nouvelle activité" : "Modifier les informations");
        
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        TextField nomField = new TextField(activite != null ? activite.getNomA() : "");
        TextArea descField = new TextArea(activite != null ? activite.getDescriptionA() : "");
        TextField lieuField = new TextField(activite != null ? activite.getLieu() : "");
        TextField prixField = new TextField(activite != null ? String.valueOf(activite.getPrixParPersonne()) : "");
        TextField capaciteField = new TextField(activite != null ? String.valueOf(activite.getCapaciteMax()) : "");
        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList(
            "Excursion", "Sport", "Culture", "Aventure", "Détente"
        ));
        typeCombo.setValue(activite != null ? activite.getType() : "Excursion");
        ComboBox<String> statutCombo = new ComboBox<>(FXCollections.observableArrayList(
            "Disponible", "Complet", "Suspendu"
        ));
        statutCombo.setValue(activite != null ? activite.getStatut() : "Disponible");
        TextField imageField = new TextField(activite != null ? activite.getImage() : "");
        
        descField.setPrefRowCount(3);
        nomField.setPromptText("Nom de l'activité");
        descField.setPromptText("Description détaillée");
        lieuField.setPromptText("Lieu");
        prixField.setPromptText("Prix par personne");
        capaciteField.setPromptText("Capacité maximale");
        imageField.setPromptText("Chemin de l'image");
        
        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Lieu:"), 0, 2);
        grid.add(lieuField, 1, 2);
        grid.add(new Label("Prix (DT):"), 0, 3);
        grid.add(prixField, 1, 3);
        grid.add(new Label("Capacité:"), 0, 4);
        grid.add(capaciteField, 1, 4);
        grid.add(new Label("Type:"), 0, 5);
        grid.add(typeCombo, 1, 5);
        grid.add(new Label("Statut:"), 0, 6);
        grid.add(statutCombo, 1, 6);
        grid.add(new Label("Image:"), 0, 7);
        grid.add(imageField, 1, 7);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    if (activite == null) {
                        return new Activite(
                            nomField.getText(),
                            descField.getText(),
                            lieuField.getText(),
                            Float.parseFloat(prixField.getText()),
                            Integer.parseInt(capaciteField.getText()),
                            typeCombo.getValue(),
                            statutCombo.getValue(),
                            imageField.getText()
                        );
                    } else {
                        activite.setNomA(nomField.getText());
                        activite.setDescriptionA(descField.getText());
                        activite.setLieu(lieuField.getText());
                        activite.setPrixParPersonne(Float.parseFloat(prixField.getText()));
                        activite.setCapaciteMax(Integer.parseInt(capaciteField.getText()));
                        activite.setType(typeCombo.getValue());
                        activite.setStatut(statutCombo.getValue());
                        activite.setImage(imageField.getText());
                        return activite;
                    }
                } catch (NumberFormatException e) {
                    showError("Erreur de saisie", "Veuillez vérifier les valeurs numériques.");
                    return null;
                }
            }
            return null;
        });
        
        return dialog;
    }
    
    @FXML
    private void refreshTable() {
        // TODO: Recharger depuis la base de données
        applyFilters();
        updateStatistics();
        showSuccess("Données actualisées!");
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

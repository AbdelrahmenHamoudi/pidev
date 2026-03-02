package org.example.Controllers.activite;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.example.Entites.activite.Activite;
import org.example.Entites.activite.Planning;


import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class PlanningBackOfficeController implements Initializable {

    @FXML
    private ComboBox<Activite> activiteCombo;
    @FXML private Text activiteNameText;

    @FXML private Text totalPlanningsText;
    @FXML private Text planningsDispoText;
    @FXML private Text placesTotalesText;

    @FXML private TableView<Planning> planningsTable;
    @FXML private TableColumn<Planning, Integer> idColumn;
    @FXML private TableColumn<Planning, LocalDate> dateColumn;
    @FXML private TableColumn<Planning, LocalTime> heureDebutColumn;
    @FXML private TableColumn<Planning, LocalTime> heureFinColumn;
    @FXML private TableColumn<Planning, String> etatColumn;
    @FXML private TableColumn<Planning, Integer> placesRestantesColumn;
    @FXML private TableColumn<Planning, Void> actionsColumn;

    private ObservableList<Activite> activitesList = FXCollections.observableArrayList();
    private ObservableList<Planning> planningsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupActiviteCombo();
        setupTableColumns();
        loadActivites();
    }

    private void setupActiviteCombo() {
        activiteCombo.setItems(activitesList);
        activiteCombo.setCellFactory(param -> new ListCell<Activite>() {
            @Override
            protected void updateItem(Activite item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNomA());
            }
        });
        activiteCombo.setButtonCell(new ListCell<Activite>() {
            @Override
            protected void updateItem(Activite item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNomA());
            }
        });

        activiteCombo.setOnAction(e -> {
            Activite selected = activiteCombo.getValue();
            if (selected != null) {
                activiteNameText.setText("Activité: " + selected.getNomA());
                loadPlanningsForActivite(selected.getIdActivite());
            }
        });
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("idPlanning"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("datePlanning"));
        heureDebutColumn.setCellValueFactory(new PropertyValueFactory<>("heureDebut"));
        heureFinColumn.setCellValueFactory(new PropertyValueFactory<>("heureFin"));
        etatColumn.setCellValueFactory(new PropertyValueFactory<>("etat"));
        placesRestantesColumn.setCellValueFactory(new PropertyValueFactory<>("nbPlacesRestantes"));

        // Format date column
        dateColumn.setCellFactory(column -> new TableCell<Planning, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                }
            }
        });

        // Format time columns
        heureDebutColumn.setCellFactory(column -> new TableCell<Planning, LocalTime>() {
            @Override
            protected void updateItem(LocalTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(DateTimeFormatter.ofPattern("HH:mm")));
                }
            }
        });

        heureFinColumn.setCellFactory(column -> new TableCell<Planning, LocalTime>() {
            @Override
            protected void updateItem(LocalTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(DateTimeFormatter.ofPattern("HH:mm")));
                }
            }
        });

        // Style état column
        etatColumn.setCellFactory(column -> new TableCell<Planning, String>() {
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
                    } else if (item.equalsIgnoreCase("Annulé")) {
                        setStyle("-fx-text-fill: #95A5A6; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #F39C12; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Places restantes column with color coding
        placesRestantesColumn.setCellFactory(column -> new TableCell<Planning, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(item));
                    if (item == 0) {
                        setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
                    } else if (item <= 5) {
                        setStyle("-fx-text-fill: #F39C12; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Actions column
        actionsColumn.setCellFactory(column -> new TableCell<Planning, Void>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final Button detailsBtn = new Button("👁️");

            {
                editBtn.setStyle("-fx-background-color: #F39C12; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
                detailsBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");

                editBtn.setOnAction(event -> {
                    Planning planning = getTableView().getItems().get(getIndex());
                    editPlanning(planning);
                });

                deleteBtn.setOnAction(event -> {
                    Planning planning = getTableView().getItems().get(getIndex());
                    deletePlanning(planning);
                });

                detailsBtn.setOnAction(event -> {
                    Planning planning = getTableView().getItems().get(getIndex());
                    viewPlanningDetails(planning);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, detailsBtn, editBtn, deleteBtn);
                    buttons.setAlignment(Pos.CENTER);
                    setGraphic(buttons);
                }
            }
        });

        planningsTable.setItems(planningsList);
    }

    private void loadActivites() {
        // TODO: Remplacer par service BD
        activitesList.add(new Activite(1, "Randonnée Montagneuse", "Explorez les sommets", "Ain Draham", 45.0f, 20, "Aventure", "Disponible", "hiking.jpg"));
        activitesList.add(new Activite(2, "Plongée Sous-Marine", "Découvrez les fonds marins", "Tabarka", 80.0f, 12, "Sport", "Disponible", "diving.jpg"));
        activitesList.add(new Activite(3, "Visite Médina", "Tour guidé", "Tunis", 25.0f, 30, "Culture", "Disponible", "medina.jpg"));
    }

    private void loadPlanningsForActivite(int idActivite) {
        planningsList.clear();
        // TODO: Charger depuis BD
        // Exemple de données
        planningsList.add(new Planning(1, idActivite, LocalDate.now().plusDays(3), LocalTime.of(9, 0), LocalTime.of(12, 0), "Disponible", 15));
        planningsList.add(new Planning(2, idActivite, LocalDate.now().plusDays(5), LocalTime.of(14, 0), LocalTime.of(17, 0), "Disponible", 8));
        planningsList.add(new Planning(3, idActivite, LocalDate.now().plusDays(7), LocalTime.of(10, 0), LocalTime.of(13, 0), "Complet", 0));
        planningsList.add(new Planning(4, idActivite, LocalDate.now().plusDays(10), LocalTime.of(8, 30), LocalTime.of(11, 30), "Disponible", 20));

        updateStatistics();
    }

    private void updateStatistics() {
        int total = planningsList.size();
        long disponibles = planningsList.stream()
                .filter(p -> p.getEtat().equals("Disponible"))
                .count();
        int placesTotales = planningsList.stream()
                .mapToInt(Planning::getNbPlacesRestantes)
                .sum();

        totalPlanningsText.setText(String.valueOf(total));
        planningsDispoText.setText(String.valueOf(disponibles));
        placesTotalesText.setText(String.valueOf(placesTotales));
    }

    @FXML
    private void showAddPlanningDialog() {
        Activite selectedActivite = activiteCombo.getValue();
        if (selectedActivite == null) {
            showError("Erreur", "Veuillez d'abord sélectionner une activité.");
            return;
        }

        Dialog<Planning> dialog = createPlanningDialog(null, selectedActivite);
        Optional<Planning> result = dialog.showAndWait();

        result.ifPresent(planning -> {
            // TODO: Ajouter à la BD
            planning.setIdPlanning(planningsList.size() + 1);
            planningsList.add(planning);
            updateStatistics();
            showSuccess("Planning créé avec succès!");
        });
    }

    private void editPlanning(Planning planning) {
        Activite selectedActivite = activiteCombo.getValue();
        Dialog<Planning> dialog = createPlanningDialog(planning, selectedActivite);
        Optional<Planning> result = dialog.showAndWait();

        result.ifPresent(updatedPlanning -> {
            // TODO: Mettre à jour dans BD
            planningsList.set(planningsList.indexOf(planning), updatedPlanning);
            updateStatistics();
            showSuccess("Planning modifié avec succès!");
        });
    }

    private void deletePlanning(Planning planning) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer ce planning?");
        alert.setContentText("Date: " + planning.getDatePlanning().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // TODO: Supprimer de BD
            planningsList.remove(planning);
            updateStatistics();
            showSuccess("Planning supprimé avec succès!");
        }
    }

    private void viewPlanningDetails(Planning planning) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du Planning");
        alert.setHeaderText("Planning #" + planning.getIdPlanning());

        String content = String.format(
                "Date: %s\n" +
                        "Horaire: %s - %s\n" +
                        "État: %s\n" +
                        "Places restantes: %d\n" +
                        "Activité ID: %d",
                planning.getDatePlanning().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                planning.getHeureDebut().format(DateTimeFormatter.ofPattern("HH:mm")),
                planning.getHeureFin().format(DateTimeFormatter.ofPattern("HH:mm")),
                planning.getEtat(),
                planning.getNbPlacesRestantes(),
                planning.getIdActivite()
        );

        alert.setContentText(content);
        alert.showAndWait();
    }

    private Dialog<Planning> createPlanningDialog(Planning planning, Activite activite) {
        Dialog<Planning> dialog = new Dialog<>();
        dialog.setTitle(planning == null ? "Nouveau Planning" : "Modifier le Planning");
        dialog.setHeaderText(planning == null ? "Créer un nouveau planning pour: " + activite.getNomA() : "Modifier le planning");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        DatePicker datePicker = new DatePicker(planning != null ? planning.getDatePlanning() : LocalDate.now().plusDays(1));
        Spinner<Integer> heureDebutH = new Spinner<>(0, 23, planning != null ? planning.getHeureDebut().getHour() : 9);
        Spinner<Integer> heureDebutM = new Spinner<>(0, 59, planning != null ? planning.getHeureDebut().getMinute() : 0, 15);
        Spinner<Integer> heureFinH = new Spinner<>(0, 23, planning != null ? planning.getHeureFin().getHour() : 12);
        Spinner<Integer> heureFinM = new Spinner<>(0, 59, planning != null ? planning.getHeureFin().getMinute() : 0, 15);

        TextField placesField = new TextField(planning != null ? String.valueOf(planning.getNbPlacesRestantes()) : String.valueOf(activite.getCapaciteMax()));
        ComboBox<String> etatCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Disponible", "Complet", "Annulé", "En attente"
        ));
        etatCombo.setValue(planning != null ? planning.getEtat() : "Disponible");

        heureDebutH.setEditable(true);
        heureDebutM.setEditable(true);
        heureFinH.setEditable(true);
        heureFinM.setEditable(true);

        grid.add(new Label("Date:"), 0, 0);
        grid.add(datePicker, 1, 0);

        HBox heureDebutBox = new HBox(5, heureDebutH, new Label(":"), heureDebutM);
        grid.add(new Label("Heure Début:"), 0, 1);
        grid.add(heureDebutBox, 1, 1);

        HBox heureFinBox = new HBox(5, heureFinH, new Label(":"), heureFinM);
        grid.add(new Label("Heure Fin:"), 0, 2);
        grid.add(heureFinBox, 1, 2);

        grid.add(new Label("Places:"), 0, 3);
        grid.add(placesField, 1, 3);
        grid.add(new Label("État:"), 0, 4);
        grid.add(etatCombo, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    LocalTime debut = LocalTime.of(heureDebutH.getValue(), heureDebutM.getValue());
                    LocalTime fin = LocalTime.of(heureFinH.getValue(), heureFinM.getValue());

                    if (planning == null) {
                        return new Planning(
                                activite.getIdActivite(),
                                datePicker.getValue(),
                                debut,
                                fin,
                                etatCombo.getValue(),
                                Integer.parseInt(placesField.getText())
                        );
                    } else {
                        planning.setDatePlanning(datePicker.getValue());
                        planning.setHeureDebut(debut);
                        planning.setHeureFin(fin);
                        planning.setEtat(etatCombo.getValue());
                        planning.setNbPlacesRestantes(Integer.parseInt(placesField.getText()));
                        return planning;
                    }
                } catch (Exception e) {
                    showError("Erreur de saisie", "Veuillez vérifier les valeurs saisies.");
                    return null;
                }
            }
            return null;
        });

        return dialog;
    }

    @FXML
    private void refreshPlannings() {
        Activite selected = activiteCombo.getValue();
        if (selected != null) {
            loadPlanningsForActivite(selected.getIdActivite());
            showSuccess("Plannings actualisés!");
        }
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

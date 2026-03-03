package org.example.Controllers.activite;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.Entites.activite.Activite;
import org.example.Entites.activite.Planning;
import org.example.Entites.user.User;
import org.example.Services.activite.ActiviteService;
import org.example.Services.activite.ActiviteServiceImpl;
import org.example.Services.activite.PlaningActiviteImpl;
import org.example.Utils.UserSession;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ActiviteBackOfficeController implements Initializable {

    @FXML
    private Label totalActivitesLabel;
    @FXML private Label activesLabel;
    @FXML private Label capaciteLabel;
    @FXML private TextField searchField;
    @FXML private Label userNameLabel;

    // Form Activité
    @FXML private TextField txtNom;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtLieu;
    @FXML private ComboBox<String> comboType;
    @FXML private TextField txtCapacite;
    @FXML private TextField txtPrix;
    @FXML private ComboBox<String> comboStatut;
    @FXML private TextField txtImage;

    // Ajouter ces déclarations avec les autres @FXML
    @FXML private Button btnComptesBancaires;
    @FXML private Button btnTransactions;
    @FXML private Button btnCredits;
    @FXML private Button btnCashback;
    @FXML private Button btnParametres;

    // Image upload
    @FXML private ImageView imagePreview;
    @FXML private StackPane imagePreviewPane;
    @FXML private Label lblImageName;
    @FXML private Label lblImagePlaceholder;
    @FXML private Button btnChoisirImage;
    @FXML private Button btnSupprimerImage;

    // Form Planning
    @FXML private DatePicker planningDatePicker;
    @FXML private Spinner<Integer> spinnerHeureDebut;
    @FXML private Spinner<Integer> spinnerMinuteDebut;
    @FXML private Spinner<Integer> spinnerHeureFin;
    @FXML private Spinner<Integer> spinnerMinuteFin;
    @FXML private VBox planningsListContainer;

    @FXML private Button btnAjouter;
    @FXML private Button btnSupprimer;

    // Table
    @FXML private TableView<Activite> tableActivites;
    @FXML private TableColumn<Activite, Integer> colId;
    @FXML private TableColumn<Activite, String> colNom;
    @FXML private TableColumn<Activite, String> colLieu;
    @FXML private TableColumn<Activite, String> colType;
    @FXML private TableColumn<Activite, Float> colPrix;
    @FXML private TableColumn<Activite, String> colStatut;
    @FXML private TableColumn<Activite, Void> colActions;

    @FXML private Label paginationLabel;

    private ObservableList<Activite> activitesList = FXCollections.observableArrayList();
    private ObservableList<Activite> filteredList = FXCollections.observableArrayList();
    private Activite selectedActivite = null;
    private List<Planning> tempPlanningsList = new ArrayList<>();

    private ActiviteService activiteService;
    private PlaningActiviteImpl planningService;

    // ✅ Utilisateur connecté via JWT
    private User currentUser;
    private boolean isAdmin = false;

    // ═══════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ✅ Vérifier l'authentification JWT
        if (!checkUserAuth()) {
            return;
        }

        activiteService = new ActiviteServiceImpl();
        planningService = new PlaningActiviteImpl();

        setupComboBoxes();
        setupSpinners();
        setupTableColumns();
        setupActionsColumn();
        setupListeners();

        loadActivitesFromDatabase();
        updateStatistics();

        btnSupprimer.setDisable(true);
    }

    /**
     * ✅ Vérifie si l'utilisateur est authentifié via JWT
     */
    private boolean checkUserAuth() {
        if (!UserSession.getInstance().isTokenValid()) {
            showAlert(Alert.AlertType.ERROR, "Session expirée",
                    "Votre session a expiré. Veuillez vous reconnecter.");
            redirectToLogin();
            return false;
        }

        currentUser = UserSession.getInstance().getCurrentUser();

        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Aucun utilisateur connecté.");
            redirectToLogin();
            return false;
        }

        // ✅ Vérifier si l'utilisateur est admin
        isAdmin = currentUser.getRole() != null &&
                currentUser.getRole().name().equalsIgnoreCase("admin");

        // Afficher les infos
        System.out.println("✅ Back-office activités - Utilisateur: " +
                currentUser.getPrenom() + " " + currentUser.getNom());
        System.out.println("✅ Rôle: " + currentUser.getRole() + " | Admin: " + isAdmin);
        System.out.println("✅ Token valide: " + UserSession.getInstance().isTokenValid());

        // Mettre à jour l'affichage du nom
        if (userNameLabel != null) {
            userNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        }

        return true;
    }

    /**
     * ✅ Redirige vers la page de login
     */
    private void redirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) (tableActivites != null ?
                    tableActivites.getScene().getWindow() :
                    btnAjouter.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  SETUP
    // ═══════════════════════════════════════════════════════════════

    private void setupComboBoxes() {
        comboType.setItems(FXCollections.observableArrayList(
                "Excursion", "Sport", "Culture", "Aventure", "Détente"
        ));
        comboStatut.setItems(FXCollections.observableArrayList(
                "Disponible", "Complet", "Suspendu"
        ));
        comboStatut.setValue("Disponible");
    }

    private void setupSpinners() {
        spinnerHeureDebut.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9));
        spinnerMinuteDebut.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 15));
        spinnerHeureFin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 17));
        spinnerMinuteFin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 15));
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idActivite"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nomA"));
        colLieu.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prixParPersonne"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colPrix.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Float item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f DT", item));
            }
        });

        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle(switch (item) {
                        case "Disponible" -> "-fx-text-fill: #1ABC9C; -fx-font-weight: bold;";
                        case "Complet" -> "-fx-text-fill: #E53E3E; -fx-font-weight: bold;";
                        default -> "-fx-text-fill: #F39C12; -fx-font-weight: bold;";
                    });
                }
            }
        });

        tableActivites.setItems(filteredList);
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button btnDetails = new Button("👁️");
            private final Button btnEdit = new Button("✏️");
            private final Button btnDelete = new Button("🗑️");
            private final Button btnPlannings = new Button("📅");

            {
                String base = "-fx-font-size: 13px; -fx-padding: 5 8; -fx-background-radius: 6; -fx-cursor: hand;";
                btnDetails.setStyle(base + "-fx-background-color: #3498DB; -fx-text-fill: white;");
                btnEdit.setStyle(base + "-fx-background-color: #F39C12; -fx-text-fill: white;");
                btnDelete.setStyle(base + "-fx-background-color: #E74C3C; -fx-text-fill: white;");
                btnPlannings.setStyle(base + "-fx-background-color: #1ABC9C; -fx-text-fill: white;");

                btnDetails.setTooltip(new Tooltip("Voir détails"));
                btnEdit.setTooltip(new Tooltip("Modifier"));
                btnDelete.setTooltip(new Tooltip("Supprimer"));
                btnPlannings.setTooltip(new Tooltip("Gérer plannings"));

                btnDetails.setOnAction(e ->
                        showDetailsModal(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(e ->
                        editActivite(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e ->
                        deleteActiviteConfirm(getTableView().getItems().get(getIndex())));
                btnPlannings.setOnAction(e ->
                        showDetailsModal(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(5, btnDetails, btnEdit, btnPlannings, btnDelete);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
    }

    private void setupListeners() {
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
    }

    // ═══════════════════════════════════════════════════════════════
    //  IMAGE UPLOAD
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void choisirImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image pour l'activité");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        File desktop = new File(System.getProperty("user.home") + "/Desktop");
        if (desktop.exists())
            fc.setInitialDirectory(desktop);

        File chosen = fc.showOpenDialog(btnChoisirImage.getScene().getWindow());
        if (chosen == null)
            return;

        if (chosen.length() > 5L * 1024 * 1024) {
            showAlert(Alert.AlertType.WARNING, "Image trop volumineuse",
                    "L'image dépasse 5 Mo. Veuillez en choisir une plus légère.");
            return;
        }

        try {
            Path destDir = Paths.get("src/main/resources/images/activites/");
            Files.createDirectories(destDir);

            String uniqueName = System.currentTimeMillis() + "_" + chosen.getName();
            Path destPath = destDir.resolve(uniqueName);
            Files.copy(chosen.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);

            txtImage.setText("activites/" + uniqueName);

            imagePreview.setImage(new Image(destPath.toUri().toString(), true));
            imagePreview.setVisible(true);
            imagePreview.setManaged(true);

            lblImagePlaceholder.setVisible(false);
            lblImagePlaceholder.setManaged(false);
            lblImageName.setText(chosen.getName());
            btnSupprimerImage.setVisible(true);
            btnSupprimerImage.setManaged(true);

            System.out.println("✅ Image copiée : " + destPath);

        } catch (Exception e) {
            System.err.println("❌ Erreur copie image : " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de copier l'image : " + e.getMessage());
        }
    }

    @FXML
    private void supprimerImage() {
        resetImageField();
    }

    private void resetImageField() {
        txtImage.clear();
        imagePreview.setImage(null);
        imagePreview.setVisible(false);
        imagePreview.setManaged(false);
        lblImagePlaceholder.setVisible(true);
        lblImagePlaceholder.setManaged(true);
        lblImageName.setText("Aucune image sélectionnée");
        btnSupprimerImage.setVisible(false);
        btnSupprimerImage.setManaged(false);
    }

    // ═══════════════════════════════════════════════════════════════
    //  CRUD ACTIVITÉ
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void addOrUpdateActivite() {
        // ✅ Vérifier que l'utilisateur est connecté
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Vous devez être connecté pour ajouter une activité.");
            return;
        }

        // ✅ Vérifier que l'utilisateur n'est pas banni
        if (currentUser.getStatus() != null &&
                currentUser.getStatus().name().equalsIgnoreCase("Banned")) {
            showAlert(Alert.AlertType.ERROR, "Accès refusé",
                    "Votre compte est suspendu. Contactez l'administrateur.");
            return;
        }

        try {
            if (txtNom.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Nom obligatoire.");
                return;
            }
            if (txtCapacite.getText().trim().isEmpty() || txtPrix.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Capacité et prix obligatoires.");
                return;
            }
            if (comboType.getValue() == null || comboStatut.getValue() == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Type et statut obligatoires.");
                return;
            }

            String nom = txtNom.getText().trim();
            String description = txtDescription.getText().trim();
            String lieu = txtLieu.getText().trim();
            String type = comboType.getValue();
            float prix = Float.parseFloat(txtPrix.getText().trim());
            int capacite = Integer.parseInt(txtCapacite.getText().trim());
            String statut = comboStatut.getValue();
            String image = txtImage.getText().trim().isEmpty() ? "default.jpg" : txtImage.getText().trim();

            if (selectedActivite == null) {
                // ── Ajout
                Activite activite = new Activite(nom, description, lieu, prix, capacite, type, statut, image);
                boolean success = activiteService.addActivite(activite);

                if (success) {
                    int added = 0;
                    for (Planning p : tempPlanningsList) {
                        p.setIdActivite(activite.getIdActivite());
                        if (planningService.addPlanning(p))
                            added++;
                    }
                    activitesList.add(activite);
                    showAlert(Alert.AlertType.INFORMATION, "Succès",
                            "Activité ajoutée avec succès !\n" + added + " planning(s) créé(s).");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Impossible d'ajouter l'activité dans la base de données.");
                    return;
                }

            } else {
                // ── Modification
                // Vérifier les droits (admin seulement pour modification)
                if (!isAdmin) {
                    showAlert(Alert.AlertType.ERROR, "Accès refusé",
                            "Seuls les administrateurs peuvent modifier des activités.");
                    return;
                }

                selectedActivite.setNomA(nom);
                selectedActivite.setDescriptionA(description);
                selectedActivite.setLieu(lieu);
                selectedActivite.setType(type);
                selectedActivite.setPrixParPersonne(prix);
                selectedActivite.setCapaciteMax(capacite);
                selectedActivite.setStatut(statut);
                selectedActivite.setImage(image);

                if (activiteService.updateActivite(selectedActivite)) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Activité modifiée avec succès !");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de modifier l'activité.");
                    return;
                }
            }

            applyFilters();
            updateStatistics();
            clearForm();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de saisie",
                    "Le prix et la capacité doivent être des nombres valides.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Une erreur est survenue : " + e.getMessage());
        }
    }

    @FXML
    private void deleteActivite() {
        if (selectedActivite != null)
            deleteActiviteConfirm(selectedActivite);
    }

    private void deleteActiviteConfirm(Activite activite) {
        // ✅ Vérifier les droits (admin seulement)
        if (!isAdmin) {
            showAlert(Alert.AlertType.ERROR, "Accès refusé",
                    "Seuls les administrateurs peuvent supprimer des activités.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer \"" + activite.getNomA() + "\" ?");
        confirm.setContentText("Cette action supprimera aussi tous les plannings associés.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (activiteService.deleteActivite(activite.getIdActivite())) {
                    activitesList.remove(activite);
                    applyFilters();
                    updateStatistics();
                    clearForm();
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Activité supprimée !");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer.");
                }
            }
        });
    }

    private void editActivite(Activite activite) {
        selectedActivite = activite;
        fillFormWithActivite(activite);
        loadPlanningsForActivite(activite.getIdActivite());
        btnSupprimer.setDisable(false);
        btnAjouter.setText("💾  Modifier l'activité");
    }

    @FXML
    private void handleCancel() {
        clearForm();
    }

    private void clearForm() {
        txtNom.clear();
        txtDescription.clear();
        txtLieu.clear();
        comboType.setValue(null);
        txtCapacite.clear();
        txtPrix.clear();
        comboStatut.setValue("Disponible");
        planningDatePicker.setValue(null);
        tempPlanningsList.clear();
        updatePlanningsDisplay();
        selectedActivite = null;
        tableActivites.getSelectionModel().clearSelection();
        btnSupprimer.setDisable(true);
        btnAjouter.setText("💾  Enregistrer l'activité");
        resetImageField();
    }

    private void fillFormWithActivite(Activite activite) {
        txtNom.setText(activite.getNomA());
        txtDescription.setText(activite.getDescriptionA());
        txtLieu.setText(activite.getLieu());
        comboType.setValue(activite.getType());
        txtCapacite.setText(String.valueOf(activite.getCapaciteMax()));
        txtPrix.setText(String.valueOf(activite.getPrixParPersonne()));
        comboStatut.setValue(activite.getStatut());

        String imagePath = activite.getImage();
        if (imagePath != null && !imagePath.isBlank() && !imagePath.equals("default.jpg")) {
            txtImage.setText(imagePath);
            File imgFile = new File("src/main/resources/images/" + imagePath);
            if (imgFile.exists()) {
                imagePreview.setImage(new Image(imgFile.toURI().toString(), true));
                imagePreview.setVisible(true);
                imagePreview.setManaged(true);
                lblImagePlaceholder.setVisible(false);
                lblImagePlaceholder.setManaged(false);
                lblImageName.setText(imgFile.getName());
                btnSupprimerImage.setVisible(true);
                btnSupprimerImage.setManaged(true);
            }
        } else {
            resetImageField();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  PLANNINGS
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void addPlanningToList() {
        try {
            if (planningDatePicker.getValue() == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez sélectionner une date.");
                return;
            }
            if (txtCapacite.getText() == null || txtCapacite.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Veuillez définir la capacité de l'activité d'abord.");
                return;
            }

            Integer heureD = spinnerHeureDebut.getValue();
            Integer minuteD = spinnerMinuteDebut.getValue();
            Integer heureF = spinnerHeureFin.getValue();
            Integer minuteF = spinnerMinuteFin.getValue();

            if (heureD == null || minuteD == null || heureF == null || minuteF == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Les heures ne sont pas correctement initialisées.");
                return;
            }

            LocalDate date = planningDatePicker.getValue();
            LocalTime debut = LocalTime.of(heureD, minuteD);
            LocalTime fin = LocalTime.of(heureF, minuteF);

            if (!fin.isAfter(debut)) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "L'heure de fin doit être après l'heure de début.\nDébut : " + debut + "  Fin : " + fin);
                return;
            }

            int capacite;
            try {
                capacite = Integer.parseInt(txtCapacite.getText().trim());
                if (capacite <= 0) {
                    showAlert(Alert.AlertType.ERROR, "Erreur",
                            "La capacité doit être supérieure à 0.");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "La capacité doit être un nombre entier valide.");
                return;
            }

            Planning planning = new Planning(0, 0, date, debut, fin, "Disponible", capacite);
            tempPlanningsList.add(planning);
            updatePlanningsDisplay();
            planningDatePicker.setValue(null);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur technique",
                    "Une erreur est survenue : " + e.getClass().getSimpleName() + "\n" + e.getMessage());
        }
    }

    private void loadPlanningsForActivite(int idActivite) {
        try {
            tempPlanningsList.clear();
            tempPlanningsList.addAll(planningService.getPlanningsByActivite(idActivite));
            updatePlanningsDisplay();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updatePlanningsDisplay() {
        planningsListContainer.getChildren().clear();

        if (tempPlanningsList.isEmpty()) {
            Label empty = new Label("Aucun planning");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
            planningsListContainer.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < tempPlanningsList.size(); i++) {
            Planning p = tempPlanningsList.get(i);
            final int index = i;

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: white; -fx-padding: 10; " +
                    "-fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 8;");

            Label text = new Label(String.format("🗓️  %s   %s – %s   (%d places)",
                    p.getDatePlanning().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    p.getHeureDebut().format(DateTimeFormatter.ofPattern("HH:mm")),
                    p.getHeureFin().format(DateTimeFormatter.ofPattern("HH:mm")),
                    p.getNbPlacesRestantes()));
            text.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155;");
            HBox.setHgrow(text, Priority.ALWAYS);

            Button del = new Button("✕");
            del.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; " +
                    "-fx-cursor: hand; -fx-background-radius: 6; -fx-font-weight: bold; -fx-padding: 3 8;");
            del.setOnAction(e -> {
                tempPlanningsList.remove(index);
                updatePlanningsDisplay();
            });

            row.getChildren().addAll(text, del);
            planningsListContainer.getChildren().add(row);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  MODAL DÉTAILS
    // ═══════════════════════════════════════════════════════════════

    private void showDetailsModal(Activite activite) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Détails — " + activite.getNomA());

        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #FAFAFA;");

        VBox infoBox = new VBox(12);
        infoBox.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12; " +
                "-fx-border-color: #F39C12; -fx-border-width: 2; -fx-border-radius: 12;");

        Label titre = new Label("📍 " + activite.getNomA());
        titre.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Label details = new Label(String.format(
                "Lieu: %s  |  Type: %s  |  Prix: %.2f DT\n" +
                        "Capacité: %d personnes  |  Statut: %s\n\n" +
                        "Description: %s",
                activite.getLieu(), activite.getType(), activite.getPrixParPersonne(),
                activite.getCapaciteMax(), activite.getStatut(), activite.getDescriptionA()));
        details.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        details.setWrapText(true);

        infoBox.getChildren().addAll(titre, new Separator(), details);

        Label planningsTitle = new Label("📅 Plannings de cette activité");
        planningsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        VBox planningsBox = new VBox(10);
        planningsBox.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 12;");

        try {
            List<Planning> plannings = planningService.getPlanningsByActivite(activite.getIdActivite());
            if (plannings.isEmpty()) {
                Label empty = new Label("Aucun planning pour cette activité");
                empty.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
                planningsBox.getChildren().add(empty);
            } else {
                for (Planning p : plannings) {
                    planningsBox.getChildren().add(createPlanningRow(p, modal));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ScrollPane scroll = new ScrollPane(planningsBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        scroll.setPrefHeight(300);

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 10 30; " +
                "-fx-background-radius: 8; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> modal.close());

        root.getChildren().addAll(infoBox, planningsTitle, scroll, closeBtn);
        modal.setScene(new Scene(root, 700, 600));
        modal.showAndWait();
    }

    private HBox createPlanningRow(Planning planning, Stage parentModal) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12));
        row.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8; " +
                "-fx-border-color: #E2E8F0; -fx-border-radius: 8;");

        Label icon = new Label("🗓️");
        icon.setStyle("-fx-font-size: 18px;");

        VBox info = new VBox(3);
        Label dateLabel = new Label(
                planning.getDatePlanning().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dateLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50; -fx-font-size: 14px;");

        Label timeLabel = new Label(String.format("%s – %s  |  %d places  |  %s",
                planning.getHeureDebut().format(DateTimeFormatter.ofPattern("HH:mm")),
                planning.getHeureFin().format(DateTimeFormatter.ofPattern("HH:mm")),
                planning.getNbPlacesRestantes(),
                planning.getEtat()));
        timeLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        info.getChildren().addAll(dateLabel, timeLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button btnEdit = new Button("✏️");
        btnEdit.setStyle("-fx-background-color: #F39C12; -fx-text-fill: white; " +
                "-fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;");
        btnEdit.setTooltip(new Tooltip("Modifier ce planning"));
        btnEdit.setOnAction(e -> editPlanningInModal(planning, parentModal));

        Button btnDel = new Button("🗑️");
        btnDel.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; " +
                "-fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;");
        btnDel.setTooltip(new Tooltip("Supprimer ce planning"));
        btnDel.setOnAction(e -> deletePlanningInModal(planning, parentModal));

        row.getChildren().addAll(icon, info, btnEdit, btnDel);
        return row;
    }

    private void editPlanningInModal(Planning planning, Stage parentModal) {
        showAlert(Alert.AlertType.INFORMATION, "Modification",
                "Fonctionnalité de modification du planning #" + planning.getIdPlanning());
    }

    private void deletePlanningInModal(Planning planning, Stage parentModal) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer ce planning ?");
        confirm.setContentText("Date : "
                + planning.getDatePlanning().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (planningService.deletePlanning(planning.getIdPlanning())) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Planning supprimé !");
                    parentModal.close();
                    activitesList.stream()
                            .filter(a -> a.getIdActivite() == planning.getIdActivite())
                            .findFirst()
                            .ifPresent(this::showDetailsModal);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer.");
                }
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ═══════════════════════════════════════════════════════════════

    private void loadActivitesFromDatabase() {
        try {
            activitesList.clear();
            filteredList.clear();

            // Si admin, voir toutes les activités
            // Pour l'instant on n'a pas de champ createur, donc on voit tout
            activitesList.addAll(activiteService.getAllActivites());

            filteredList.addAll(activitesList);
            paginationLabel.setText(activitesList.size() + " activité(s)");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les activités.");
        }
    }

    private void applyFilters() {
        String search = searchField.getText().toLowerCase();
        filteredList.clear();
        filteredList.addAll(activitesList.stream()
                .filter(a -> search.isEmpty() || a.getNomA().toLowerCase().contains(search)
                        || a.getLieu().toLowerCase().contains(search))
                .toList());
        paginationLabel.setText(filteredList.size() + " activité(s)");
    }

    private void updateStatistics() {
        totalActivitesLabel.setText(String.valueOf(activitesList.size()));
        activesLabel.setText(String.valueOf(
                activitesList.stream().filter(a -> "Disponible".equals(a.getStatut())).count()));
        capaciteLabel.setText(String.valueOf(
                activitesList.stream().mapToInt(Activite::getCapaciteMax).sum()));
    }

    @FXML
    private void refreshData() {
        loadActivitesFromDatabase();
        updateStatistics();
        clearForm();
        showAlert(Alert.AlertType.INFORMATION, "Actualisé", "Données rechargées !");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * ✅ Vérifie si l'utilisateur est authentifié et est admin
     */
    private boolean checkAdminAuth() {
        if (!checkUserAuth()) {
            return false;
        }

        if (!isAdmin) {
            showAlert(Alert.AlertType.ERROR, "Accès refusé",
                    "Vous devez être administrateur pour accéder à cette page.");
            return false;
        }

        return true;
    }

    // ═══════════════════════════════════════════════════════════════
//  MÉTHODES DE NAVIGATION
// ═══════════════════════════════════════════════════════════════



    // ═══════════════════════════════════════════════════════════════
//  MÉTHODES DE NAVIGATION
// ═══════════════════════════════════════════════════════════════

    /**
     * ✅ Redirection vers la gestion des utilisateurs
     */
    @FXML
    private void handleShowUsers(ActionEvent event) {
        if (!checkAdminAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/back/users.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            // Ajouter le CSS
            try {
                String css = getClass().getResource("/user/back/users.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("⚠️ CSS utilisateurs non trouvé");
            }

            stage.setScene(scene);
            stage.setTitle("Gestion des Utilisateurs - RE7LA");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            // ✅ CORRECTION: Ordre des paramètres (type, title, message)
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "❌ Impossible de charger l'interface utilisateurs: " + e.getMessage());
        }
    }

    /**
     * ✅ Redirection vers la gestion des hébergements
     */
    @FXML
    private void handleShowAccounts(ActionEvent event) {
        if (!checkAdminAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/hebergement/back/HebergementBack.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            // Ajouter le CSS
            try {
                String css = getClass().getResource("/hebergement/back/StyleHB.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("⚠️ CSS hébergement non trouvé");
            }

            stage.setScene(scene);
            stage.setTitle("Gestion des Hébergements - RE7LA");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            // ✅ CORRECTION: Ordre des paramètres (type, title, message)
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "❌ Impossible de charger l'interface hébergement: " + e.getMessage());
        }
    }

    /**
     * ✅ Redirection vers la gestion des activités
     */
    @FXML
    private void handleShowTransactions(ActionEvent event) {

    }

    /**
     * ✅ Redirection vers la gestion des trajets
     */
    @FXML
    private void handleShowCredits(ActionEvent event) {
        if (!checkAdminAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Trajet/gestionvoitureettrajet.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            // Ajouter le CSS
            try {
                String css = getClass().getResource("/Trajet/style.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("⚠️ CSS trajets non trouvé");
            }

            stage.setScene(scene);
            stage.setTitle("Gestion des Voitures et Trajets - RE7LA");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            // ✅ CORRECTION: Ordre des paramètres (type, title, message)
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "❌ Impossible de charger l'interface des trajets: " + e.getMessage());
        }
    }

    /**
     * ✅ Redirection vers la gestion de la communauté
     */
    @FXML
    private void handleShowCommunaute(ActionEvent event) {
        if (!checkAdminAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/communaute/PublicationCommentaire.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            // Ajouter le CSS
            try {
                String css = getClass().getResource("/communaute/Communaute.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("⚠️ CSS communauté non trouvé");
            }

            stage.setScene(scene);
            stage.setTitle("Gestion de la Communauté - RE7LA");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            // ✅ CORRECTION: Ordre des paramètres (type, title, message)
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "❌ Impossible de charger l'interface de la communauté: " + e.getMessage());
        }
    }

    /**
     * ✅ Redirection vers la gestion des promotions (en développement)
     */
    @FXML
    private void handleShowCashback(ActionEvent event) {
        if (!checkAdminAuth()) return;
        // ✅ CORRECTION: Ordre des paramètres (type, title, message)
        showAlert(Alert.AlertType.INFORMATION, "Information",
                "🏷️ Gestion des Promotions - En cours de développement");
    }

    /**
     * ✅ Redirection vers les paramètres (en développement)
     */
    @FXML
    private void handleShowSettings(ActionEvent event) {
        if (!checkAdminAuth()) return;
        // ✅ CORRECTION: Ordre des paramètres (type, title, message)
        showAlert(Alert.AlertType.INFORMATION, "Information",
                "⚙️ Paramètres - En cours de développement");
    }
    /**
     * ✅ Déconnexion de l'utilisateur
     */
    @FXML
    private void handleLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Déconnexion");
        alert.setHeaderText("Voulez-vous vraiment vous déconnecter ?");
        alert.setContentText("Vous serez redirigé vers la page de connexion.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // ✅ Effacer la session JWT
                UserSession.getInstance().clearSession();

                // ✅ Rediriger vers la page de login
                redirectToLogin(event);
            }
        });
    }

    /**
     * ✅ Redirige vers la page de login
     */
    private void redirectToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            // Ajouter le CSS du login si nécessaire
            try {
                String css = getClass().getResource("/user/login/login.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("⚠️ CSS login non trouvé");
            }

            stage.setScene(scene);
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "❌ Impossible de rediriger vers la page de connexion: " + e.getMessage());
        }
    }

}
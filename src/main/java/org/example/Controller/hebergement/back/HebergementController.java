package org.example.Controller.hebergement.back;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.Entites.hebergement.Hebergement;
import org.example.Entites.hebergement.Reservation;
import org.example.Services.hebergement.HebergementCRUD;
import org.example.Services.hebergement.ReservationCRUD;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class HebergementController {

    public VBox sidebar;
    // ===== FORM FIELDS =====
    @FXML private TextField txtTitre, txtCapacite, txtType, txtPrix, txtImage;
    @FXML private TextArea txtDesc;
    @FXML private CheckBox chkDisponible;
    @FXML private ImageView imagePreview;

    // ===== LABELS POUR MESSAGES D'ERREUR =====
    @FXML private Label lblTitreErreur, lblDescErreur, lblCapaciteErreur, lblTypeErreur, lblPrixErreur, lblImageErreur;

    // ===== BUTTONS =====
    @FXML private Button btnAjouter, btnSupprimer, btnParcourir;
    @FXML private Button btnGestionUtilisateurs, btnComptesBancaires, btnTransactions,
            btnCredits, btnCashback, btnParametres, btnDeconnexion;

    // ===== TABLE HEBERGEMENT =====
    @FXML private TableView<Hebergement> tableHebergement;
    @FXML private TableColumn<Hebergement, Integer> colId, colCapacite;
    @FXML private TableColumn<Hebergement, String> colTitre, colDesc, colType, colImage;
    @FXML private TableColumn<Hebergement, Boolean> colDisponible;
    @FXML private TableColumn<Hebergement, Float> colPrix;

    // ===== TABLE RÉSERVATIONS =====
    @FXML private TableView<Reservation> tableReservations;
    @FXML private TableColumn<Reservation, Integer> colResId;
    @FXML private TableColumn<Reservation, String> colResNom;
    @FXML private TableColumn<Reservation, String> colResPrenom;
    @FXML private TableColumn<Reservation, String> colResTel;
    @FXML private TableColumn<Reservation, String> colResDateDebut;
    @FXML private TableColumn<Reservation, String> colResDateFin;
    @FXML private TableColumn<Reservation, String> colResStatut;

    @FXML private Label lblReservationsCount;
    @FXML private Label lblSelectedHebergement;

    // ===== DONNÉES =====
    private final ObservableList<Hebergement> hebergementList = FXCollections.observableArrayList();
    private final ObservableList<Reservation> reservationList = FXCollections.observableArrayList();
    private Hebergement selectedHebergement = null;

    private final HebergementCRUD hc = new HebergementCRUD();
    private final ReservationCRUD rc = new ReservationCRUD();   // CRUD pour les réservations

    // Constante pour le dossier des images
    private static final String IMAGE_DIRECTORY = "src/main/resources/images/hebergements/";

    // =========================
    // INITIALIZE
    // =========================
    @FXML
    public void initialize() {

        // ----- Configuration colonnes Hébergement -----
        colId.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().getId_hebergement()).asObject());
        colTitre.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getTitre()));
        colDesc.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getDesc_hebergement()));
        colCapacite.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().getCapacite()).asObject());
        colType.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getType_hebergement()));

        // Mise en forme disponibilité
        colDisponible.setCellValueFactory(cell -> new javafx.beans.property.SimpleBooleanProperty(cell.getValue().isDisponible_heberg()).asObject());
        colDisponible.setCellFactory(column -> new TableCell<Hebergement, Boolean>() {
            @Override
            protected void updateItem(Boolean disponible, boolean empty) {
                super.updateItem(disponible, empty);
                if (empty || disponible == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(disponible ? "✅" : "❌");
                    if (disponible) {
                        setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-background-color: #E6F5F1; -fx-padding: 5; -fx-background-radius: 5;");
                    } else {
                        setStyle("-fx-text-fill: #E53E3E; -fx-font-weight: bold; -fx-background-color: #FEF2F2; -fx-padding: 5; -fx-background-radius: 5;");
                    }
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Formatage prix
        colPrix.setCellValueFactory(cell -> new javafx.beans.property.SimpleFloatProperty(cell.getValue().getPrixParNuit()).asObject());
        colPrix.setCellFactory(column -> new TableCell<Hebergement, Float>() {
            @Override
            protected void updateItem(Float prix, boolean empty) {
                super.updateItem(prix, empty);
                if (empty || prix == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f TND", prix));
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50;");
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        });

        // Formatage image
        colImage.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getImage()));
        colImage.setCellFactory(column -> new TableCell<Hebergement, String>() {
            @Override
            protected void updateItem(String image, boolean empty) {
                super.updateItem(image, empty);
                if (empty || image == null) {
                    setText(null);
                } else {
                    setText("📷 " + image);
                    setAlignment(Pos.CENTER);
                    setTooltip(new Tooltip(image));
                }
            }
        });

        // ----- Configuration colonnes Réservation -----
        colResId.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().getId_reservation()).asObject());
        colResNom.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getNomR()));
        colResPrenom.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getPrenomR()));
        colResTel.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cell.getValue().getTelR())));
        colResDateDebut.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getDateDebutR()));
        colResDateFin.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getDateFinR()));
        colResStatut.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStatutR()));

        // Mise en forme du statut
        colResStatut.setCellFactory(column -> new TableCell<Reservation, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(statut);
                    setAlignment(Pos.CENTER);
                    // Couleurs selon le statut (vous pouvez adapter)
                    if ("CONFIRMÉE".equalsIgnoreCase(statut)) {
                        setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-background-color: #E6F5F1; -fx-padding: 5; -fx-background-radius: 5;");
                    } else if ("EN ATTENTE".equalsIgnoreCase(statut)) {
                        setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-background-color: #FEF3C7; -fx-padding: 5; -fx-background-radius: 5;");
                    } else if ("ANNULÉE".equalsIgnoreCase(statut)) {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-background-color: #FEE2E2; -fx-padding: 5; -fx-background-radius: 5;");
                    } else {
                        setStyle("-fx-text-fill: #64748b; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Message quand aucune réservation
        tableReservations.setPlaceholder(new Label("Sélectionnez un hébergement pour voir ses réservations"));

        // Listeners de validation
        addValidationListeners();

        // Charger les hébergements
        loadData();

        // Sélection dans la table
        tableHebergement.setOnMouseClicked(this::handleRowSelection);
        // Alternative : utiliser un listener sur la sélection
        tableHebergement.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                handleRowSelection(null); // on peut appeler la même méthode
            }
        });

        hideAllErrorLabels();

        if (imagePreview != null) {
            imagePreview.setFitWidth(150);
            imagePreview.setFitHeight(100);
            imagePreview.setPreserveRatio(true);
        }
    }

    // =========================
    // PARCOURIR IMAGE (AVEC COPIE)
    // =========================
    @FXML
    private void browseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image depuis le Bureau");

        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
                "Images (*.jpg, *.jpeg, *.png, *.gif)",
                "*.jpg", "*.jpeg", "*.png", "*.gif");
        fileChooser.getExtensionFilters().add(imageFilter);

        String userHome = System.getProperty("user.home");
        File desktopDir = new File(userHome + "/Desktop");
        if (desktopDir.exists()) {
            fileChooser.setInitialDirectory(desktopDir);
        } else {
            fileChooser.setInitialDirectory(new File(userHome));
        }

        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            try {
                long fileSize = selectedFile.length();
                long maxSize = 5 * 1024 * 1024;
                if (fileSize > maxSize) {
                    showAlert("Fichier trop volumineux",
                            "L'image ne doit pas dépasser 5 MB.\nTaille actuelle: " + (fileSize / (1024 * 1024)) + " MB");
                    return;
                }

                File imageDir = new File(IMAGE_DIRECTORY);
                if (!imageDir.exists()) {
                    boolean created = imageDir.mkdirs();
                    if (!created) {
                        showAlert("Erreur", "Impossible de créer le dossier pour les images");
                        return;
                    }
                }

                String originalFileName = selectedFile.getName();
                String fileName = System.currentTimeMillis() + "_" +
                        originalFileName.replace(" ", "_").toLowerCase();

                File destFile = new File(IMAGE_DIRECTORY + fileName);
                Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                String imagePath = "images/hebergements/" + fileName;
                txtImage.setText(imagePath);
                updateImagePreview(imagePath);

                showAlert("Succès", "Image sélectionnée depuis le Bureau !\nFichier: " + originalFileName);

            } catch (IOException e) {
                showAlert("Erreur", "Erreur lors de la copie de l'image : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // =========================
    // PARCOURIR IMAGE SIMPLE (SANS COPIE)
    // =========================
    @FXML
    private void browseImageSimple(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image depuis le Bureau");

        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
                "Images (*.jpg, *.jpeg, *.png, *.gif)",
                "*.jpg", "*.jpeg", "*.png", "*.gif");
        fileChooser.getExtensionFilters().add(imageFilter);

        String userHome = System.getProperty("user.home");
        File desktopDir = new File(userHome + "/Desktop");
        if (desktopDir.exists()) {
            fileChooser.setInitialDirectory(desktopDir);
        }

        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            long fileSize = selectedFile.length();
            long maxSize = 5 * 1024 * 1024;
            if (fileSize > maxSize) {
                showAlert("Fichier trop volumineux",
                        "L'image ne doit pas dépasser 5 MB.\nTaille actuelle: " + (fileSize / (1024 * 1024)) + " MB");
                return;
            }

            String fileName = selectedFile.getName().toLowerCase();
            if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") &&
                    !fileName.endsWith(".png") && !fileName.endsWith(".gif")) {
                showAlert("Format non supporté",
                        "Veuillez sélectionner une image au format JPG, PNG ou GIF");
                return;
            }

            txtImage.setText(selectedFile.getAbsolutePath());
            updateImagePreview(selectedFile.getAbsolutePath());
            showAlert("Succès", "Image sélectionnée depuis le Bureau : " + selectedFile.getName());
        }
    }

    // =========================
    // METTRE À JOUR L'APERÇU DE L'IMAGE
    // =========================
    private void updateImagePreview(String imagePath) {
        if (imagePreview != null && imagePath != null && !imagePath.isEmpty()) {
            try {
                File file = new File(imagePath);
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString());
                    imagePreview.setImage(image);
                } else {
                    File relFile = new File("src/main/resources/" + imagePath);
                    if (relFile.exists()) {
                        Image image = new Image(relFile.toURI().toString());
                        imagePreview.setImage(image);
                    } else {
                        imagePreview.setImage(null);
                    }
                }
            } catch (Exception e) {
                System.out.println("Erreur lors du chargement de l'aperçu : " + e.getMessage());
                imagePreview.setImage(null);
            }
        }
    }

    // =========================
    // CACHER TOUS LES LABELS D'ERREUR
    // =========================
    private void hideAllErrorLabels() {
        if (lblTitreErreur != null) {
            lblTitreErreur.setVisible(false);
            lblTitreErreur.setText("");
        }
        if (lblDescErreur != null) {
            lblDescErreur.setVisible(false);
            lblDescErreur.setText("");
        }
        if (lblCapaciteErreur != null) {
            lblCapaciteErreur.setVisible(false);
            lblCapaciteErreur.setText("");
        }
        if (lblTypeErreur != null) {
            lblTypeErreur.setVisible(false);
            lblTypeErreur.setText("");
        }
        if (lblPrixErreur != null) {
            lblPrixErreur.setVisible(false);
            lblPrixErreur.setText("");
        }
        if (lblImageErreur != null) {
            lblImageErreur.setVisible(false);
            lblImageErreur.setText("");
        }
    }

    // =========================
    // AJOUT DES LISTENERS DE VALIDATION
    // =========================
    private void addValidationListeners() {
        txtTitre.textProperty().addListener((observable, oldValue, newValue) -> validateTitre());
        txtDesc.textProperty().addListener((observable, oldValue, newValue) -> validateDescription());
        txtCapacite.textProperty().addListener((observable, oldValue, newValue) -> validateCapacite());
        txtType.textProperty().addListener((observable, oldValue, newValue) -> validateType());
        txtPrix.textProperty().addListener((observable, oldValue, newValue) -> validatePrix());
        txtImage.textProperty().addListener((observable, oldValue, newValue) -> {
            validateImage();
            updateImagePreview(newValue);
        });
    }

    // =========================
    // MÉTHODES DE VALIDATION
    // =========================
    private boolean validateTitre() {
        String titre = txtTitre.getText();
        if (titre == null || titre.trim().isEmpty()) {
            showError(lblTitreErreur, "Le titre est obligatoire");
            return false;
        } else if (titre.length() < 3) {
            showError(lblTitreErreur, "Le titre doit contenir au moins 3 caractères");
            return false;
        } else if (titre.length() > 100) {
            showError(lblTitreErreur, "Le titre ne doit pas dépasser 100 caractères");
            return false;
        } else {
            hideError(lblTitreErreur);
            return true;
        }
    }

    private boolean validateDescription() {
        String desc = txtDesc.getText();
        if (desc == null || desc.trim().isEmpty()) {
            showError(lblDescErreur, "La description est obligatoire");
            return false;
        } else if (desc.length() < 10) {
            showError(lblDescErreur, "La description doit contenir au moins 10 caractères");
            return false;
        } else if (desc.length() > 500) {
            showError(lblDescErreur, "La description ne doit pas dépasser 500 caractères");
            return false;
        } else {
            hideError(lblDescErreur);
            return true;
        }
    }

    private boolean validateCapacite() {
        String capaciteStr = txtCapacite.getText();
        if (capaciteStr == null || capaciteStr.trim().isEmpty()) {
            showError(lblCapaciteErreur, "La capacité est obligatoire");
            return false;
        }
        try {
            int capacite = Integer.parseInt(capaciteStr);
            if (capacite <= 0) {
                showError(lblCapaciteErreur, "La capacité doit être supérieure à 0");
                return false;
            } else if (capacite > 1000) {
                showError(lblCapaciteErreur, "La capacité ne doit pas dépasser 1000");
                return false;
            } else {
                hideError(lblCapaciteErreur);
                return true;
            }
        } catch (NumberFormatException e) {
            showError(lblCapaciteErreur, "La capacité doit être un nombre entier valide");
            return false;
        }
    }

    private boolean validateType() {
        String type = txtType.getText();
        if (type == null || type.trim().isEmpty()) {
            showError(lblTypeErreur, "Le type est obligatoire");
            return false;
        } else if (type.length() < 3) {
            showError(lblTypeErreur, "Le type doit contenir au moins 3 caractères");
            return false;
        } else {
            hideError(lblTypeErreur);
            return true;
        }
    }

    private boolean validatePrix() {
        String prixStr = txtPrix.getText();
        if (prixStr == null || prixStr.trim().isEmpty()) {
            showError(lblPrixErreur, "Le prix est obligatoire");
            return false;
        }
        try {
            float prix = Float.parseFloat(prixStr);
            if (prix <= 0) {
                showError(lblPrixErreur, "Le prix doit être supérieur à 0");
                return false;
            } else if (prix > 10000) {
                showError(lblPrixErreur, "Le prix ne doit pas dépasser 10000 TND");
                return false;
            } else {
                hideError(lblPrixErreur);
                return true;
            }
        } catch (NumberFormatException e) {
            showError(lblPrixErreur, "Le prix doit être un nombre valide");
            return false;
        }
    }

    private boolean validateImage() {
        String image = txtImage.getText();
        if (image == null || image.trim().isEmpty()) {
            showError(lblImageErreur, "L'image est obligatoire");
            return false;
        } else if (!isValidImageFormat(image)) {
            showError(lblImageErreur, "Format d'image non supporté. Utilisez JPG, PNG ou GIF");
            return false;
        } else {
            hideError(lblImageErreur);
            return true;
        }
    }

    private boolean isValidImageFormat(String imagePath) {
        String lowerCase = imagePath.toLowerCase();
        return lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg") ||
                lowerCase.endsWith(".png") || lowerCase.endsWith(".gif");
    }

    private void showError(Label label, String message) {
        if (label != null) {
            label.setText(message);
            label.setVisible(true);
            label.setStyle("-fx-text-fill: #E53E3E; -fx-font-size: 11px; -fx-font-weight: normal; -fx-padding: 2 0 0 0;");
        }
    }

    private void hideError(Label label) {
        if (label != null) {
            label.setVisible(false);
            label.setText("");
        }
    }

    // =========================
    // VALIDATION GLOBALE
    // =========================
    private boolean validateAllFields() {
        boolean isValid = true;
        isValid &= validateTitre();
        isValid &= validateDescription();
        isValid &= validateCapacite();
        isValid &= validateType();
        isValid &= validatePrix();
        isValid &= validateImage();
        return isValid;
    }

    // =========================
    // CHARGER DONNÉES HEBERGEMENT
    // =========================
    private void loadData() {
        try {
            hebergementList.setAll(hc.afficherh());
            tableHebergement.setItems(hebergementList);
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement des données !");
        }
    }

    // =========================
    // CHARGER LES RÉSERVATIONS D'UN HÉBERGEMENT
    // =========================
    private void loadReservationsForHebergement(int hebergementId) {
        try {
            List<Reservation> reservations = rc.rechercherParHebergement(hebergementId);
            reservationList.setAll(reservations);
            tableReservations.setItems(reservationList);
            if (lblReservationsCount != null) {
                lblReservationsCount.setText(reservations.size() + " réservation(s)");
            }
            if (lblSelectedHebergement != null && selectedHebergement != null) {
                lblSelectedHebergement.setText("Réservations pour : " + selectedHebergement.getTitre());
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les réservations : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================
    // SELECTION TABLE HEBERGEMENT
    // =========================
    private void handleRowSelection(MouseEvent event) {
        selectedHebergement = tableHebergement.getSelectionModel().getSelectedItem();

        if (selectedHebergement != null) {
            txtTitre.setText(selectedHebergement.getTitre());
            txtDesc.setText(selectedHebergement.getDesc_hebergement());
            txtCapacite.setText(String.valueOf(selectedHebergement.getCapacite()));
            txtType.setText(selectedHebergement.getType_hebergement());
            chkDisponible.setSelected(selectedHebergement.isDisponible_heberg());
            txtPrix.setText(String.valueOf(selectedHebergement.getPrixParNuit()));
            txtImage.setText(selectedHebergement.getImage());

            updateImagePreview(selectedHebergement.getImage());
            hideAllErrorLabels();

            // Charger les réservations de cet hébergement
            loadReservationsForHebergement(selectedHebergement.getId_hebergement());
        }
    }

    // =========================
    // AJOUT / MODIFICATION HEBERGEMENT
    // =========================
    @FXML
    public void addOrUpdate(ActionEvent event) {

        if (!validateAllFields()) {
            showAlert("Erreur de validation", "Veuillez corriger les erreurs dans le formulaire");
            return;
        }

        try {
            String titre = txtTitre.getText();
            String desc = txtDesc.getText();
            int capacite = Integer.parseInt(txtCapacite.getText());
            String type = txtType.getText();
            boolean disponible = chkDisponible.isSelected();
            float prix = Float.parseFloat(txtPrix.getText());
            String image = txtImage.getText();

            if (selectedHebergement == null) {
                Hebergement h = new Hebergement(0, titre, desc, capacite, type, disponible, prix, image);
                hc.ajouterh(h);
                showAlert("Succès", "Hébergement ajouté avec succès !");
            } else {
                selectedHebergement.setTitre(titre);
                selectedHebergement.setDesc_hebergement(desc);
                selectedHebergement.setCapacite(capacite);
                selectedHebergement.setType_hebergement(type);
                selectedHebergement.setDisponible_heberg(disponible);
                selectedHebergement.setPrixParNuit(prix);
                selectedHebergement.setImage(image);

                hc.modifierh(selectedHebergement);
                showAlert("Succès", "Hébergement modifié avec succès !");
            }

            loadData();
            clearForm();

        } catch (NumberFormatException e) {
            showAlert("Erreur", "Vérifiez les champs numériques !");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur Base de Données !");
        }
    }

    // =========================
    // SUPPRESSION HEBERGEMENT AVEC CONFIRMATION
    // =========================
    @FXML
    private void deleteHebergement(ActionEvent event) {

        if (selectedHebergement != null) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirmation de suppression");
            confirmAlert.setHeaderText("Êtes-vous sûr de vouloir supprimer cet hébergement ?");

            String details = String.format(
                    "Détails de l'hébergement à supprimer :\n\n" +
                            "📋 ID: %d\n" +
                            "🏠 Titre: %s\n" +
                            "📝 Description: %s\n" +
                            "👥 Capacité: %d personnes\n" +
                            "🏷️ Type: %s\n" +
                            "✅ Disponible: %s\n" +
                            "💰 Prix par nuit: %.2f TND\n" +
                            "🖼️ Image: %s",
                    selectedHebergement.getId_hebergement(),
                    selectedHebergement.getTitre(),
                    selectedHebergement.getDesc_hebergement().length() > 50 ?
                            selectedHebergement.getDesc_hebergement().substring(0, 50) + "..." :
                            selectedHebergement.getDesc_hebergement(),
                    selectedHebergement.getCapacite(),
                    selectedHebergement.getType_hebergement(),
                    selectedHebergement.isDisponible_heberg() ? "Oui" : "Non",
                    selectedHebergement.getPrixParNuit(),
                    selectedHebergement.getImage()
            );

            Label detailsLabel = new Label(details);
            detailsLabel.setStyle("-fx-padding: 10; -fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-radius: 5;");
            confirmAlert.getDialogPane().setContent(detailsLabel);

            ButtonType btnConfirmer = new ButtonType("Confirmer la suppression", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmAlert.getButtonTypes().setAll(btnConfirmer, btnAnnuler);

            Optional<ButtonType> result = confirmAlert.showAndWait();

            if (result.isPresent() && result.get() == btnConfirmer) {
                try {
                    hc.supprimerh(selectedHebergement);

                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Succès");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("L'hébergement \"" + selectedHebergement.getTitre() + "\" a été supprimé avec succès !");
                    successAlert.showAndWait();

                    loadData();
                    clearForm();
                } catch (SQLException e) {
                    showAlert("Erreur", "Impossible de supprimer l'hébergement !");
                }
            }
        } else {
            showAlert("Attention", "Veuillez sélectionner un hébergement à supprimer !");
        }
    }

    // =========================
    // CLEAR FORM
    // =========================
    private void clearForm() {
        txtTitre.clear();
        txtDesc.clear();
        txtCapacite.clear();
        txtType.clear();
        txtPrix.clear();
        txtImage.clear();
        chkDisponible.setSelected(false);
        selectedHebergement = null;
        hideAllErrorLabels();

        if (imagePreview != null) {
            imagePreview.setImage(null);
        }

        // Réinitialiser les infos de réservation
        if (lblSelectedHebergement != null) {
            lblSelectedHebergement.setText("Aucun hébergement sélectionné");
        }
        if (lblReservationsCount != null) {
            lblReservationsCount.setText("0 réservation(s)");
        }
        reservationList.clear();
        tableReservations.setItems(reservationList);
    }

    // =========================
    // ALERT JAVA FX
    // =========================
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // =========================
    // SIDEBAR BUTTONS (autres modules)
    // =========================
    @FXML private void handleShowUsers(ActionEvent event) { System.out.println("Afficher utilisateurs"); }
    @FXML private void handleShowAccounts(ActionEvent event) { System.out.println("Afficher comptes"); }
    @FXML private void handleShowTransactions(ActionEvent event) { System.out.println("Afficher transactions"); }
    @FXML private void handleShowCredits(ActionEvent event) { System.out.println("Afficher crédits"); }
    @FXML private void handleShowCashback(ActionEvent event) { System.out.println("Afficher cashback"); }
    @FXML private void handleShowSettings(ActionEvent event) { System.out.println("Afficher paramètres"); }
    @FXML private void handleLogout(ActionEvent event) { System.out.println("Déconnexion"); }

    @FXML
    private void handleCancel(ActionEvent event) {
        clearForm();
    }

    // =========================
    // BOUTONS DE REDIRECTION VERS LA PAGE FRONT DES RÉSERVATIONS
    // =========================
    @FXML private Button btnReservation;
    @FXML private Button btnGoToReservations;

    @FXML
    private void handleShowReservations(ActionEvent event) {
        openReservationPage();
    }

    @FXML
    private void goToReservations(ActionEvent event) {
        openReservationPage();
    }

    private void openReservationPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/hebergement/front/Rservation.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Gestion des Réservations");
            stage.setScene(new Scene(root));

            stage.getScene().getStylesheets().add(
                    getClass().getResource("/hebergement/front/StyleRSV.css").toExternalForm()
            );

            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la page des réservations : " + e.getMessage());
        }
    }
}
package org.example.Controller.hebergement.front;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.example.Entites.hebergement.Hebergement;
import org.example.Entites.hebergement.Reservation;
import org.example.Services.hebergement.HebergementCRUD;
import org.example.Services.hebergement.ReservationCRUD;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.ResourceBundle;

public class ReservationController implements Initializable {

    @FXML private ImageView backgroundImage;
    // ================= COMPOSANTS FXML =================
    @FXML private TilePane tileHebergements;
    @FXML private TextField searchHebergementField;

    // Formulaire réservation
    @FXML private TextField txtNom;
    @FXML private TextField txtPrenom;
    @FXML private TextField txtTel;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private TextField txtPrixTotal;
    @FXML private ComboBox<String> comboStatut;

    // Labels d'erreur
    @FXML private Label lblNomErreur;
    @FXML private Label lblPrenomErreur;
    @FXML private Label lblTelErreur;
    @FXML private Label lblDateDebutErreur;
    @FXML private Label lblDateFinErreur;

    // Hébergement sélectionné
    @FXML private ImageView hebergementImage;
    @FXML private Label selectedHebergementTitre;
    @FXML private Label selectedHebergementPrix;
    @FXML private Label selectedHebergementDesc;

    // Boutons
    @FXML private Button btnReserver;
    @FXML private Button btnAnnuler;

    // ================= DONNÉES =================
    private ObservableList<Hebergement> allHebergements = FXCollections.observableArrayList();
    private ObservableList<Hebergement> filteredHebergements = FXCollections.observableArrayList();

    private Hebergement selectedHebergement = null;

    private final HebergementCRUD hebergementCRUD = new HebergementCRUD();
    private final ReservationCRUD reservationCRUD = new ReservationCRUD();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupStatutCombo();
        setupListeners();
        loadHebergements();
        setupValidation();
        clearForm();
    }

    private void setupStatutCombo() {
        comboStatut.setItems(FXCollections.observableArrayList(
                "EN ATTENTE", "CONFIRMÉE", "ANNULÉE", "TERMINÉE"
        ));
        comboStatut.setValue("EN ATTENTE");
    }

    private void setupListeners() {
        // Recherche d'hébergements
        if (searchHebergementField != null) {
            searchHebergementField.textProperty().addListener((obs, oldVal, newVal) ->
                    filterHebergements());
        }

        // Calcul du prix total quand les dates changent
        if (dateDebutPicker != null && dateFinPicker != null) {
            dateDebutPicker.valueProperty().addListener((obs, oldVal, newVal) ->
                    updatePrixTotal());
            dateFinPicker.valueProperty().addListener((obs, oldVal, newVal) ->
                    updatePrixTotal());
        }
    }

    private void setupValidation() {
        // Validation en temps réel
        if (txtNom != null) {
            txtNom.textProperty().addListener((obs, oldVal, newVal) -> validateNom());
        }
        if (txtPrenom != null) {
            txtPrenom.textProperty().addListener((obs, oldVal, newVal) -> validatePrenom());
        }
        if (txtTel != null) {
            txtTel.textProperty().addListener((obs, oldVal, newVal) -> validateTel());
        }
        if (dateDebutPicker != null && dateFinPicker != null) {
            dateDebutPicker.valueProperty().addListener((obs, oldVal, newVal) -> validateDates());
            dateFinPicker.valueProperty().addListener((obs, oldVal, newVal) -> validateDates());
        }
    }

    private boolean validateNom() {
        if (txtNom == null || lblNomErreur == null) return true;
        String nom = txtNom.getText();
        if (nom == null || nom.trim().isEmpty()) {
            showError(lblNomErreur, "Le nom est obligatoire");
            return false;
        } else if (nom.length() < 2) {
            showError(lblNomErreur, "Le nom doit contenir au moins 2 caractères");
            return false;
        } else {
            hideError(lblNomErreur);
            return true;
        }
    }

    private boolean validatePrenom() {
        if (txtPrenom == null || lblPrenomErreur == null) return true;
        String prenom = txtPrenom.getText();
        if (prenom == null || prenom.trim().isEmpty()) {
            showError(lblPrenomErreur, "Le prénom est obligatoire");
            return false;
        } else if (prenom.length() < 2) {
            showError(lblPrenomErreur, "Le prénom doit contenir au moins 2 caractères");
            return false;
        } else {
            hideError(lblPrenomErreur);
            return true;
        }
    }

    private boolean validateTel() {
        if (txtTel == null || lblTelErreur == null) return true;
        String telStr = txtTel.getText();
        if (telStr == null || telStr.trim().isEmpty()) {
            showError(lblTelErreur, "Le téléphone est obligatoire");
            return false;
        }
        try {
            Integer.parseInt(telStr);
            if (telStr.length() != 8) {
                showError(lblTelErreur, "Le numéro doit contenir 8 chiffres");
                return false;
            }
            hideError(lblTelErreur);
            return true;
        } catch (NumberFormatException e) {
            showError(lblTelErreur, "Le téléphone doit être un nombre valide");
            return false;
        }
    }

    private boolean validateDates() {
        if (dateDebutPicker == null || dateFinPicker == null) return true;

        LocalDate debut = dateDebutPicker.getValue();
        LocalDate fin = dateFinPicker.getValue();

        if (debut == null) {
            if (lblDateDebutErreur != null)
                showError(lblDateDebutErreur, "La date de début est obligatoire");
            return false;
        } else {
            if (lblDateDebutErreur != null)
                hideError(lblDateDebutErreur);
        }

        if (fin == null) {
            if (lblDateFinErreur != null)
                showError(lblDateFinErreur, "La date de fin est obligatoire");
            return false;
        } else {
            if (lblDateFinErreur != null)
                hideError(lblDateFinErreur);
        }

        if (debut != null && fin != null) {
            if (fin.isBefore(debut)) {
                if (lblDateFinErreur != null)
                    showError(lblDateFinErreur, "La date de fin doit être après la date de début");
                return false;
            }
            if (debut.isBefore(LocalDate.now())) {
                if (lblDateDebutErreur != null)
                    showError(lblDateDebutErreur, "La date de début ne peut pas être dans le passé");
                return false;
            }
        }

        return true;
    }

    private boolean validateAllFields() {
        boolean isValid = true;
        isValid &= validateNom();
        isValid &= validatePrenom();
        isValid &= validateTel();
        isValid &= validateDates();

        if (selectedHebergement == null) {
            showAlert("Erreur", "Veuillez sélectionner un hébergement");
            isValid = false;
        }

        return isValid;
    }

    private void showError(Label label, String message) {
        if (label != null) {
            label.setText(message);
            label.setVisible(true);
            label.setManaged(true);
        }
    }

    private void hideError(Label label) {
        if (label != null) {
            label.setVisible(false);
            label.setManaged(false);
            label.setText("");
        }
    }

    private void loadHebergements() {
        try {
            allHebergements.setAll(hebergementCRUD.afficherh());
            filteredHebergements.setAll(allHebergements);
            displayHebergements();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les hébergements : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void filterHebergements() {
        if (searchHebergementField == null) return;

        String searchText = searchHebergementField.getText().toLowerCase();

        filteredHebergements.clear();

        if (searchText.isEmpty()) {
            filteredHebergements.addAll(allHebergements);
        } else {
            for (Hebergement h : allHebergements) {
                if (h.getTitre().toLowerCase().contains(searchText) ||
                        h.getType_hebergement().toLowerCase().contains(searchText) ||
                        h.getDesc_hebergement().toLowerCase().contains(searchText)) {
                    filteredHebergements.add(h);
                }
            }
        }

        displayHebergements();
    }

    private void displayHebergements() {
        if (tileHebergements == null) return;

        tileHebergements.getChildren().clear();

        for (Hebergement h : filteredHebergements) {
            VBox card = createHebergementCard(h);
            tileHebergements.getChildren().add(card);
        }

        if (filteredHebergements.isEmpty()) {
            VBox emptyState = new VBox(10);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPrefWidth(400);
            emptyState.setPrefHeight(200);

            Label emoji = new Label("🏨");
            emoji.setStyle("-fx-font-size: 48px;");

            Label message = new Label("Aucun hébergement trouvé");
            message.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");

            emptyState.getChildren().addAll(emoji, message);
            tileHebergements.getChildren().add(emptyState);
        }
    }

    private VBox createHebergementCard(Hebergement hebergement) {
        VBox card = new VBox(10);
        card.getStyleClass().add("hebergement-card");
        card.setPrefWidth(280);
        card.setMaxWidth(280);
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> selectHebergement(hebergement));

        // Image
        StackPane imagePane = new StackPane();
        imagePane.setPrefHeight(160);
        imagePane.setStyle("-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9); -fx-background-radius: 10 10 0 0;");

        ImageView imageView = new ImageView();
        try {
            String imagePath = hebergement.getImage();
            if (imagePath != null && !imagePath.isEmpty()) {
                // Essayer différents chemins
                File file = new File("src/main/resources/" + imagePath);
                if (!file.exists()) {
                    file = new File(imagePath);
                }
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString());
                    imageView.setImage(image);
                    imageView.setFitHeight(160);
                    imageView.setFitWidth(280);
                    imageView.setPreserveRatio(true);
                }
            }
        } catch (Exception e) {
            // Image par défaut
        }

        if (imageView.getImage() == null) {
            Label icon = new Label("🏨");
            icon.setStyle("-fx-font-size: 48px; -fx-text-fill: white;");
            imagePane.getChildren().add(icon);
        } else {
            imagePane.getChildren().add(imageView);
        }

        // Badge type
        Label typeBadge = new Label(hebergement.getType_hebergement());
        typeBadge.getStyleClass().add("type-badge");
        typeBadge.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 15;");
        StackPane.setAlignment(typeBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(typeBadge, new Insets(10));
        imagePane.getChildren().add(typeBadge);

        // Contenu
        VBox content = new VBox(8);
        content.setPadding(new Insets(15));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 10 10;");

        Label titre = new Label(hebergement.getTitre());
        titre.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        titre.setWrapText(true);

        HBox infoBox = new HBox(15);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        Label capacite = new Label("👥 " + hebergement.getCapacite() + " pers.");
        capacite.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        Label prix = new Label(String.format("%.0f TND/nuit", hebergement.getPrixParNuit()));
        prix.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");

        Label separateur = new Label("•");
        separateur.setStyle("-fx-text-fill: #7f8c8d;");

        infoBox.getChildren().addAll(capacite, separateur, prix);

        Label disponibilite = new Label(hebergement.isDisponible_heberg() ? "✅ Disponible" : "❌ Non disponible");
        disponibilite.setStyle(hebergement.isDisponible_heberg() ?
                "-fx-text-fill: #27ae60; -fx-font-size: 12px;" :
                "-fx-text-fill: #e74c3c; -fx-font-size: 12px;");

        content.getChildren().addAll(titre, infoBox, disponibilite);
        card.getChildren().addAll(imagePane, content);

        // Si l'hébergement n'est pas disponible, assombrir la carte
        if (!hebergement.isDisponible_heberg()) {
            card.setStyle("-fx-opacity: 0.6;");
        }

        return card;
    }

    private void selectHebergement(Hebergement hebergement) {
        if (!hebergement.isDisponible_heberg()) {
            showAlert("Non disponible", "Cet hébergement n'est pas disponible pour le moment.");
            return;
        }

        selectedHebergement = hebergement;
        if (selectedHebergementTitre != null)
            selectedHebergementTitre.setText(hebergement.getTitre());
        if (selectedHebergementPrix != null)
            selectedHebergementPrix.setText(String.format("%.0f TND/nuit", hebergement.getPrixParNuit()));
        if (selectedHebergementDesc != null)
            selectedHebergementDesc.setText(hebergement.getDesc_hebergement());

        // Charger l'image
        if (hebergementImage != null) {
            try {
                String imagePath = hebergement.getImage();
                if (imagePath != null && !imagePath.isEmpty()) {
                    File file = new File("src/main/resources/" + imagePath);
                    if (!file.exists()) {
                        file = new File(imagePath);
                    }
                    if (file.exists()) {
                        Image image = new Image(file.toURI().toString());
                        hebergementImage.setImage(image);
                    } else {
                        hebergementImage.setImage(null);
                    }
                } else {
                    hebergementImage.setImage(null);
                }
            } catch (Exception e) {
                hebergementImage.setImage(null);
            }
        }

        // Réinitialiser le formulaire mais garder l'hébergement sélectionné
        clearFormFields();
        btnReserver.setDisable(false);
        hideAllErrors();
    }

    private void clearFormFields() {
        if (txtNom != null) txtNom.clear();
        if (txtPrenom != null) txtPrenom.clear();
        if (txtTel != null) txtTel.clear();
        if (dateDebutPicker != null) dateDebutPicker.setValue(null);
        if (dateFinPicker != null) dateFinPicker.setValue(null);
        if (txtPrixTotal != null) txtPrixTotal.clear();
        if (comboStatut != null) comboStatut.setValue("EN ATTENTE");
    }

    private void updatePrixTotal() {
        if (txtPrixTotal == null) return;

        if (selectedHebergement != null && dateDebutPicker != null && dateFinPicker != null
                && dateDebutPicker.getValue() != null && dateFinPicker.getValue() != null) {
            long jours = ChronoUnit.DAYS.between(dateDebutPicker.getValue(), dateFinPicker.getValue());
            if (jours > 0) {
                float prixTotal = selectedHebergement.getPrixParNuit() * jours;
                txtPrixTotal.setText(String.format("%.2f TND", prixTotal));
            } else {
                txtPrixTotal.setText("0.00 TND");
            }
        } else {
            txtPrixTotal.clear();
        }
    }

    @FXML
    private void reserver() {
        if (!validateAllFields()) {
            return;
        }

        try {
            Reservation reservation = new Reservation(
                    selectedHebergement,
                    dateDebutPicker.getValue().format(DATE_FORMATTER),
                    dateFinPicker.getValue().format(DATE_FORMATTER),
                    txtNom.getText().trim(),
                    txtPrenom.getText().trim(),
                    Integer.parseInt(txtTel.getText().trim()),
                    comboStatut.getValue()
            );

            reservationCRUD.ajouterh(reservation);

            showAlert("Succès", "Réservation effectuée avec succès !");
            clearForm();

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la réservation : " + e.getMessage());
            e.printStackTrace();
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Numéro de téléphone invalide");
        }
    }

    @FXML
    private void annuler() {
        clearForm();
    }

    private void clearForm() {
        clearFormFields();
        hideAllErrors();

        selectedHebergement = null;
        if (selectedHebergementTitre != null)
            selectedHebergementTitre.setText("Aucun hébergement sélectionné");
        if (selectedHebergementPrix != null)
            selectedHebergementPrix.setText("");
        if (selectedHebergementDesc != null)
            selectedHebergementDesc.setText("");
        if (hebergementImage != null)
            hebergementImage.setImage(null);

        btnReserver.setDisable(true);
    }

    private void hideAllErrors() {
        hideError(lblNomErreur);
        hideError(lblPrenomErreur);
        hideError(lblTelErreur);
        hideError(lblDateDebutErreur);
        hideError(lblDateFinErreur);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void goToHebergements(ActionEvent actionEvent) {
        // Méthode conservée si nécessaire pour navigation future
    }
}
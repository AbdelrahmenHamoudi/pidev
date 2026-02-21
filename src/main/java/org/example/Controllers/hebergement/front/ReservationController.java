package org.example.Controllers.hebergement.front;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.Entites.hebergement.Hebergement;
import org.example.Entites.hebergement.Reservation;
import org.example.Entites.user.User;
import org.example.Services.hebergement.HebergementCRUD;
import org.example.Services.hebergement.ReservationCRUD;
import org.example.Utils.UserSession;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;

public class ReservationController implements Initializable {

    @FXML private ImageView backgroundImage;

    // ================= COMPOSANTS FXML =================
    @FXML private TilePane tileHebergements;
    @FXML private TextField searchHebergementField;

    // Informations utilisateur connecté
    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label userNomLabel;
    @FXML private Label userPrenomLabel;
    @FXML private Label userEmailInfoLabel;
    @FXML private Label userTelLabel;

    // Formulaire réservation
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private TextField txtPrixTotal;
    @FXML private ComboBox<String> comboStatut;

    // Hébergement sélectionné
    @FXML private ImageView hebergementImage;
    @FXML private Label selectedHebergementTitre;
    @FXML private Label selectedHebergementPrix;

    // Boutons
    @FXML private Button btnReserver;
    @FXML private Button btnAnnuler;
    @FXML private Button btnAccueil;
    @FXML private Button btnProfil;

    // Labels d'erreur
    @FXML private Label lblDateDebutErreur;
    @FXML private Label lblDateFinErreur;

    // ================= DONNÉES =================
    private ObservableList<Hebergement> allHebergements = FXCollections.observableArrayList();
    private ObservableList<Hebergement> filteredHebergements = FXCollections.observableArrayList();

    private Hebergement selectedHebergement = null;
    private User currentUser;

    private final HebergementCRUD hebergementCRUD = new HebergementCRUD();
    private final ReservationCRUD reservationCRUD = new ReservationCRUD();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Récupérer l'utilisateur connecté depuis la session
        currentUser = UserSession.getInstance().getCurrentUser();

        if (currentUser == null) {
            showAlert("Erreur", "Aucun utilisateur connecté. Veuillez vous reconnecter.");
            return;
        }

        // Initialiser les composants
        setupStatutCombo();
        setupListeners();
        loadHebergements();
        displayUserInfo();
        clearForm();
    }

    private void setupStatutCombo() {
        comboStatut.setItems(FXCollections.observableArrayList(
                "EN ATTENTE", "CONFIRMÉE", "ANNULÉE", "TERMINÉE"
        ));
        comboStatut.setValue("EN ATTENTE");
        comboStatut.setDisable(true);
    }

    private void setupListeners() {
        if (searchHebergementField != null) {
            searchHebergementField.textProperty().addListener((obs, oldVal, newVal) ->
                    filterHebergements());
        }

        if (dateDebutPicker != null && dateFinPicker != null) {
            dateDebutPicker.valueProperty().addListener((obs, oldVal, newVal) ->
                    updatePrixTotal());
            dateFinPicker.valueProperty().addListener((obs, oldVal, newVal) ->
                    updatePrixTotal());
            dateDebutPicker.valueProperty().addListener((obs, oldVal, newVal) -> validateDates());
            dateFinPicker.valueProperty().addListener((obs, oldVal, newVal) -> validateDates());
        }
    }

    private void displayUserInfo() {
        if (currentUser != null) {
            String fullName = currentUser.getPrenom() + " " + currentUser.getNom();
            userNameLabel.setText(fullName);
            userEmailLabel.setText(currentUser.getE_mail());
            userNomLabel.setText(currentUser.getNom());
            userPrenomLabel.setText(currentUser.getPrenom());
            userEmailInfoLabel.setText(currentUser.getE_mail());
            userTelLabel.setText(currentUser.getNum_tel() != null ? currentUser.getNum_tel() : "Non renseigné");
        }
    }

    @FXML
    private void goToAccueil() {
        try {
            System.out.println("🏠 Redirection vers l'accueil...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/dashboard/homeClient.fxml"));

            if (loader.getLocation() == null) {
                throw new Exception("Fichier homeClient.fxml introuvable");
            }

            Parent root = loader.load();

            Stage stage = (Stage) btnAccueil.getScene().getWindow();
            Scene scene = new Scene(root);

            String css = getClass().getResource("/user/dashboard/homeClient.css").toExternalForm();
            scene.getStylesheets().add(css);

            stage.setScene(scene);
            stage.setTitle("RE7LA Tunisie - Accueil");
            stage.centerOnScreen();
            stage.show();

            System.out.println("✅ Redirection vers l'accueil réussie");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de retourner à l'accueil: " + e.getMessage());
        }
    }

    @FXML
    private void goToProfil() {
        try {
            System.out.println("👤 Redirection vers le profil...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/front/userProfil.fxml"));

            if (loader.getLocation() == null) {
                throw new Exception("Fichier userProfil.fxml introuvable");
            }

            Parent root = loader.load();

            Stage stage = (Stage) btnProfil.getScene().getWindow();
            Scene scene = new Scene(root);

            String css = getClass().getResource("/user/front/userProfil.css").toExternalForm();
            scene.getStylesheets().add(css);

            stage.setScene(scene);
            stage.setTitle("RE7LA Tunisie - Mon Profil");
            stage.centerOnScreen();
            stage.show();

            System.out.println("✅ Redirection vers le profil réussie");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'accéder au profil: " + e.getMessage());
        }
    }

    private boolean validateDates() {
        LocalDate debut = dateDebutPicker.getValue();
        LocalDate fin = dateFinPicker.getValue();

        if (debut == null) {
            showError(lblDateDebutErreur, "La date de début est obligatoire");
            return false;
        } else {
            hideError(lblDateDebutErreur);
        }

        if (fin == null) {
            showError(lblDateFinErreur, "La date de fin est obligatoire");
            return false;
        } else {
            hideError(lblDateFinErreur);
        }

        if (fin.isBefore(debut)) {
            showError(lblDateFinErreur, "La date de fin doit être après la date de début");
            return false;
        }

        if (debut.isBefore(LocalDate.now())) {
            showError(lblDateDebutErreur, "La date de début ne peut pas être dans le passé");
            return false;
        }

        return true;
    }

    private boolean validateAllFields() {
        boolean isValid = true;
        isValid &= validateDates();

        if (selectedHebergement == null) {
            showAlert("Erreur", "Veuillez sélectionner un hébergement");
            isValid = false;
        }

        if (currentUser == null) {
            showAlert("Erreur", "Utilisateur non connecté");
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

        StackPane imagePane = new StackPane();
        imagePane.setPrefHeight(160);
        imagePane.setStyle("-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9); -fx-background-radius: 10 10 0 0;");

        ImageView imageView = new ImageView();
        try {
            String imagePath = hebergement.getImage();
            if (imagePath != null && !imagePath.isEmpty()) {
                File file = new File(imagePath);
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString());
                    imageView.setImage(image);
                    imageView.setFitHeight(160);
                    imageView.setFitWidth(280);
                    imageView.setPreserveRatio(true);
                }
            }
        } catch (Exception e) {
        }

        if (imageView.getImage() == null) {
            Label icon = new Label("🏨");
            icon.setStyle("-fx-font-size: 48px; -fx-text-fill: white;");
            imagePane.getChildren().add(icon);
        } else {
            imagePane.getChildren().add(imageView);
        }

        Label typeBadge = new Label(hebergement.getType_hebergement());
        typeBadge.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 15;");
        StackPane.setAlignment(typeBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(typeBadge, new Insets(10));
        imagePane.getChildren().add(typeBadge);

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
        selectedHebergementTitre.setText(hebergement.getTitre());
        selectedHebergementPrix.setText(String.format("%.0f TND/nuit", hebergement.getPrixParNuit()));

        if (hebergementImage != null) {
            try {
                String imagePath = hebergement.getImage();
                if (imagePath != null && !imagePath.isEmpty()) {
                    File file = new File(imagePath);
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

        clearFormFields();
        btnReserver.setDisable(false);
        hideAllErrors();
    }

    private void clearFormFields() {
        dateDebutPicker.setValue(null);
        dateFinPicker.setValue(null);
        txtPrixTotal.clear();
        comboStatut.setValue("EN ATTENTE");
    }

    private void updatePrixTotal() {
        if (selectedHebergement != null && dateDebutPicker.getValue() != null && dateFinPicker.getValue() != null) {
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
                    currentUser,
                    dateDebutPicker.getValue().format(DATE_FORMATTER),
                    dateFinPicker.getValue().format(DATE_FORMATTER),
                    comboStatut.getValue()
            );

            reservationCRUD.ajouterh(reservation);

            showAlert("Succès", "Réservation effectuée avec succès !");
            clearForm();
            selectedHebergement = null;
            selectedHebergementTitre.setText("Aucun hébergement sélectionné...");
            selectedHebergementPrix.setText("");
            hebergementImage.setImage(null);
            btnReserver.setDisable(true);

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la réservation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void annuler() {
        clearForm();
    }

    private void clearForm() {
        clearFormFields();
        hideAllErrors();
    }

    private void hideAllErrors() {
        hideError(lblDateDebutErreur);
        hideError(lblDateFinErreur);
    }

    @FXML
    private void handleMesReservations() {
        showAlert("Information", "Fonctionnalité à venir: Liste de vos réservations");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
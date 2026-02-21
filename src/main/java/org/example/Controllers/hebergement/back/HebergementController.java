package org.example.Controllers.hebergement.back;

import javafx.scene.layout.VBox;
import org.example.Entites.hebergement.Hebergement;
import org.example.Entites.hebergement.Reservation;
import org.example.Entites.user.User;
import org.example.Services.hebergement.HebergementCRUD;
import org.example.Services.hebergement.ReservationCRUD;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class HebergementController implements Initializable {

    // ========== SIDEBAR BUTTONS ==========
    @FXML private Button btnGestionUtilisateurs;
    @FXML private Button btnComptesBancaires;
    @FXML private Button btnTransactions;
    @FXML private Button btnCredits;
    @FXML private Button btnCashback;
    @FXML private Button btnReservation;
    @FXML private Button btnParametres;
    @FXML private Button btnDeconnexion;
    @FXML private Button btnGoToReservations;

    // ========== STATS LABELS ==========
    @FXML private Label lblTotalHebergements;
    @FXML private Label lblTotalReservations;
    @FXML private Label lblDisponibles;

    // ========== FORM FIELDS ==========
    @FXML private TextField txtTitre;
    @FXML private TextArea txtDesc;
    @FXML private TextField txtCapacite;
    @FXML private TextField txtType;
    @FXML private TextField txtPrix;
    @FXML private CheckBox chkDisponible;
    @FXML private TextField txtImage;
    @FXML private ImageView imagePreview;

    // ========== ERROR LABELS ==========
    @FXML private Label lblTitreErreur;
    @FXML private Label lblDescErreur;
    @FXML private Label lblCapaciteErreur;
    @FXML private Label lblTypeErreur;
    @FXML private Label lblPrixErreur;
    @FXML private Label lblImageErreur;

    // ========== BUTTONS ==========
    @FXML private Button btnAjouter;
    @FXML private Button btnSupprimer;
    @FXML private Button btnParcourir;

    // ========== TABLES ==========
    @FXML private TableView<Hebergement> tableHebergement;
    @FXML private TableColumn<Hebergement, Integer> colId;
    @FXML private TableColumn<Hebergement, String> colTitre;
    @FXML private TableColumn<Hebergement, String> colDesc;
    @FXML private TableColumn<Hebergement, Integer> colCapacite;
    @FXML private TableColumn<Hebergement, String> colType;
    @FXML private TableColumn<Hebergement, Boolean> colDisponible;
    @FXML private TableColumn<Hebergement, Float> colPrix;
    @FXML private TableColumn<Hebergement, String> colImage;

    @FXML private TableView<Reservation> tableReservations;
    @FXML private TableColumn<Reservation, Integer> colResId;
    @FXML private TableColumn<Reservation, String> colResNom;
    @FXML private TableColumn<Reservation, String> colResPrenom;
    @FXML private TableColumn<Reservation, String> colResTel;
    @FXML private TableColumn<Reservation, String> colResDateDebut;
    @FXML private TableColumn<Reservation, String> colResDateFin;
    @FXML private TableColumn<Reservation, String> colResStatut;

    // ========== LABELS ==========
    @FXML private Label lblSelectedHebergement;
    @FXML private Label lblReservationsCount;

    // ========== VIEWS ==========
    @FXML private ScrollPane viewHebergement;
    @FXML private VBox viewEmpty;

    // ========== SERVICES ==========
    private HebergementCRUD hebergementCRUD = new HebergementCRUD();
    private ReservationCRUD reservationCRUD = new ReservationCRUD();

    // ========== DATA LISTS ==========
    private ObservableList<Hebergement> hebergementList = FXCollections.observableArrayList();
    private ObservableList<Reservation> reservationList = FXCollections.observableArrayList();

    // ========== SELECTED ITEM ==========
    private Hebergement selectedHebergement = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupReservationTableColumns();
        loadHebergements();
        setupTableSelectionListener();

        // Initialiser l'image par défaut
        try {
            imagePreview.setImage(new Image(getClass().getResourceAsStream("/hebergement/back/image/logo.png")));
        } catch (Exception e) {
            System.out.println("Logo par défaut non trouvé");
        }
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id_hebergement"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("desc_hebergement"));
        colCapacite.setCellValueFactory(new PropertyValueFactory<>("capacite"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type_hebergement"));
        colDisponible.setCellValueFactory(new PropertyValueFactory<>("disponible_heberg"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prixParNuit"));
        colImage.setCellValueFactory(new PropertyValueFactory<>("image"));
    }

    private void setupReservationTableColumns() {
        colResId.setCellValueFactory(new PropertyValueFactory<>("id_reservation"));

        // Configuration spéciale pour les colonnes qui viennent de l'objet User
        colResNom.setCellValueFactory(cellData -> {
            User user = cellData.getValue().getUser();
            return new javafx.beans.property.SimpleStringProperty(user != null ? user.getNom() : "");
        });

        colResPrenom.setCellValueFactory(cellData -> {
            User user = cellData.getValue().getUser();
            return new javafx.beans.property.SimpleStringProperty(user != null ? user.getPrenom() : "");
        });

        colResTel.setCellValueFactory(cellData -> {
            User user = cellData.getValue().getUser();
            return new javafx.beans.property.SimpleStringProperty(user != null ? user.getNum_tel() : "");
        });

        colResDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebutR"));
        colResDateFin.setCellValueFactory(new PropertyValueFactory<>("dateFinR"));
        colResStatut.setCellValueFactory(new PropertyValueFactory<>("statutR"));
    }

    private void setupTableSelectionListener() {
        tableHebergement.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedHebergement = newSelection;
                fillFormWithHebergement(newSelection);
                loadReservationsForHebergement(newSelection.getId_hebergement());
            }
        });
    }

    private void loadHebergements() {
        try {
            hebergementList.clear();
            List<Hebergement> list = hebergementCRUD.afficherh();
            hebergementList.addAll(list);
            tableHebergement.setItems(hebergementList);
            updateStats();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les hébergements: " + e.getMessage());
        }
    }

    private void loadReservationsForHebergement(int hebergementId) {
        try {
            reservationList.clear();
            List<Reservation> list = reservationCRUD.getReservationsByHebergement(hebergementId);
            reservationList.addAll(list);
            tableReservations.setItems(reservationList);

            if (selectedHebergement != null) {
                lblSelectedHebergement.setText("Hébergement: " + selectedHebergement.getTitre());
            }
            lblReservationsCount.setText(list.size() + " réservation(s)");

            updateStats();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les réservations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateStats() {
        int totalHebergements = hebergementList.size();

        int totalReservations = 0;
        try {
            List<Reservation> allReservations = reservationCRUD.afficherh();
            totalReservations = allReservations.size();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        int disponibles = (int) hebergementList.stream()
                .filter(Hebergement::isDisponible_heberg)
                .count();

        lblTotalHebergements.setText(String.valueOf(totalHebergements));
        lblTotalReservations.setText(String.valueOf(totalReservations));
        lblDisponibles.setText(String.valueOf(disponibles));
    }

    private void fillFormWithHebergement(Hebergement h) {
        txtTitre.setText(h.getTitre());
        txtDesc.setText(h.getDesc_hebergement());
        txtCapacite.setText(String.valueOf(h.getCapacite()));
        txtType.setText(h.getType_hebergement());
        txtPrix.setText(String.valueOf(h.getPrixParNuit()));
        chkDisponible.setSelected(h.isDisponible_heberg());
        txtImage.setText(h.getImage());

        if (h.getImage() != null && !h.getImage().isEmpty()) {
            try {
                Image image = new Image("file:" + h.getImage());
                imagePreview.setImage(image);
            } catch (Exception e) {
                try {
                    imagePreview.setImage(new Image(getClass().getResourceAsStream("/hebergement/back/image/logo.png")));
                } catch (Exception ex) {
                    // Ignorer
                }
            }
        }

        btnAjouter.setText("✏️ Modifier");
    }

    private void clearForm() {
        txtTitre.clear();
        txtDesc.clear();
        txtCapacite.clear();
        txtType.clear();
        txtPrix.clear();
        chkDisponible.setSelected(true);
        txtImage.clear();
        try {
            imagePreview.setImage(new Image(getClass().getResourceAsStream("/hebergement/back/image/logo.png")));
        } catch (Exception e) {
            // Ignorer
        }

        selectedHebergement = null;
        btnAjouter.setText("💾 Enregistrer");
        clearErrorLabels();
    }

    private void clearErrorLabels() {
        lblTitreErreur.setVisible(false);
        lblDescErreur.setVisible(false);
        lblCapaciteErreur.setVisible(false);
        lblTypeErreur.setVisible(false);
        lblPrixErreur.setVisible(false);
        lblImageErreur.setVisible(false);
    }

    private boolean validateForm() {
        boolean isValid = true;
        clearErrorLabels();

        if (txtTitre.getText() == null || txtTitre.getText().trim().isEmpty()) {
            lblTitreErreur.setText("Le titre est obligatoire");
            lblTitreErreur.setVisible(true);
            isValid = false;
        }

        if (txtDesc.getText() == null || txtDesc.getText().trim().isEmpty()) {
            lblDescErreur.setText("La description est obligatoire");
            lblDescErreur.setVisible(true);
            isValid = false;
        }

        try {
            int capacite = Integer.parseInt(txtCapacite.getText());
            if (capacite <= 0) {
                lblCapaciteErreur.setText("La capacité doit être positive");
                lblCapaciteErreur.setVisible(true);
                isValid = false;
            }
        } catch (NumberFormatException e) {
            lblCapaciteErreur.setText("La capacité doit être un nombre valide");
            lblCapaciteErreur.setVisible(true);
            isValid = false;
        }

        if (txtType.getText() == null || txtType.getText().trim().isEmpty()) {
            lblTypeErreur.setText("Le type est obligatoire");
            lblTypeErreur.setVisible(true);
            isValid = false;
        }

        try {
            float prix = Float.parseFloat(txtPrix.getText());
            if (prix <= 0) {
                lblPrixErreur.setText("Le prix doit être positif");
                lblPrixErreur.setVisible(true);
                isValid = false;
            }
        } catch (NumberFormatException e) {
            lblPrixErreur.setText("Le prix doit être un nombre valide");
            lblPrixErreur.setVisible(true);
            isValid = false;
        }

        if (txtImage.getText() == null || txtImage.getText().trim().isEmpty()) {
            lblImageErreur.setText("L'image est obligatoire");
            lblImageErreur.setVisible(true);
            isValid = false;
        }

        return isValid;
    }

    @FXML
    private void addOrUpdate(ActionEvent event) {
        if (!validateForm()) {
            return;
        }

        try {
            Hebergement hebergement;

            if (selectedHebergement == null) {
                hebergement = new Hebergement(
                        txtTitre.getText(),
                        txtDesc.getText(),
                        Integer.parseInt(txtCapacite.getText()),
                        txtType.getText(),
                        chkDisponible.isSelected(),
                        Float.parseFloat(txtPrix.getText()),
                        txtImage.getText()
                );

                hebergementCRUD.ajouterh(hebergement);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Hébergement ajouté avec succès !");
            } else {
                hebergement = selectedHebergement;
                hebergement.setTitre(txtTitre.getText());
                hebergement.setDesc_hebergement(txtDesc.getText());
                hebergement.setCapacite(Integer.parseInt(txtCapacite.getText()));
                hebergement.setType_hebergement(txtType.getText());
                hebergement.setDisponible_heberg(chkDisponible.isSelected());
                hebergement.setPrixParNuit(Float.parseFloat(txtPrix.getText()));
                hebergement.setImage(txtImage.getText());

                hebergementCRUD.modifierh(hebergement);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Hébergement modifié avec succès !");
            }

            loadHebergements();
            clearForm();
            reservationList.clear();
            lblSelectedHebergement.setText("Aucun hébergement sélectionné");
            lblReservationsCount.setText("0 réservation(s)");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'opération: " + e.getMessage());
        }
    }

    @FXML
    private void deleteHebergement(ActionEvent event) {
        if (selectedHebergement == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un hébergement à supprimer");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'hébergement");
        confirm.setContentText("Voulez-vous vraiment supprimer cet hébergement ?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                hebergementCRUD.supprimerh(selectedHebergement);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Hébergement supprimé avec succès !");
                loadHebergements();
                clearForm();
                reservationList.clear();
                lblSelectedHebergement.setText("Aucun hébergement sélectionné");
                lblReservationsCount.setText("0 réservation(s)");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression: " + e.getMessage());
            }
        }
    }

    @FXML
    private void browseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.gif", "*.jpeg")
        );

        File selectedFile = fileChooser.showOpenDialog(txtImage.getScene().getWindow());
        if (selectedFile != null) {
            String imagePath = selectedFile.getAbsolutePath();
            txtImage.setText(imagePath);

            try {
                Image image = new Image("file:" + imagePath);
                imagePreview.setImage(image);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger l'image");
            }
        }
    }

    @FXML
    private void handleShowUsers(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/back/users.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnGestionUtilisateurs.getScene().getWindow();
            Scene scene = new Scene(root);

            String css = getClass().getResource("/user/back/users.css").toExternalForm();
            scene.getStylesheets().add(css);

            stage.setScene(scene);
            stage.setTitle("Gestion des Utilisateurs");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger l'interface utilisateurs: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowAccounts(ActionEvent event) {
        viewHebergement.setVisible(true);
        viewEmpty.setVisible(false);
    }

    @FXML
    private void handleShowTransactions(ActionEvent event) {
        viewHebergement.setVisible(false);
        viewEmpty.setVisible(true);
    }

    @FXML
    private void handleShowCredits(ActionEvent event) {
        viewHebergement.setVisible(false);
        viewEmpty.setVisible(true);
    }

    @FXML
    private void handleShowCashback(ActionEvent event) {
        viewHebergement.setVisible(false);
        viewEmpty.setVisible(true);
    }

    @FXML
    private void handleShowReservations(ActionEvent event) {
        viewHebergement.setVisible(true);
        viewEmpty.setVisible(false);
    }

    @FXML
    private void goToReservations(ActionEvent event) {
        tableReservations.requestFocus();
    }

    @FXML
    private void handleShowSettings(ActionEvent event) {
        viewHebergement.setVisible(false);
        viewEmpty.setVisible(true);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnDeconnexion.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la page de connexion: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        clearForm();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
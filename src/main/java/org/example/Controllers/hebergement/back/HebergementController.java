package org.example.Controllers.hebergement.back;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
import org.example.Entites.hebergement.Hebergement;
import org.example.Entites.hebergement.Notification;
import org.example.Entites.hebergement.Reservation;
import org.example.Entites.user.User;
import org.example.Services.hebergement.HebergementCRUD;
import org.example.Services.hebergement.NotificationCRUD;
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
import org.example.Utils.UserSession;
import org.example.Services.user.APIservices.JWTService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public class HebergementController implements Initializable {

    // ========== TWILIO CONFIG ==========
    private static final String TWILIO_ACCOUNT_SID = "ACd472bb7b326bb567d54fcc071a8781ca";
    private static final String TWILIO_AUTH_TOKEN  = "93d99df41555b2bc66b5c282b57fa9f5";
    private static final String TWILIO_PHONE_FROM  = "+19563046258";

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
    @FXML private Button btnRapportIA;
    @FXML private Button btnAnalyseConcurrentielle;

    // ========== STATS LABELS ==========
    @FXML private Label lblTotalHebergements;
    @FXML private Label lblTotalReservations;
    @FXML private Label lblDisponibles;

    // ========== FORM FIELDS ==========
    @FXML private TextField txtTitre;
    @FXML private TextArea  txtDesc;
    @FXML private TextField txtCapacite;
    @FXML private TextField txtType;
    @FXML private TextField txtPrix;
    @FXML private CheckBox  chkDisponible;
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

    // ========== RECHERCHE ==========
    @FXML private TextField txtRecherche;

    // ========== TABLES ==========
    @FXML private TableView<Hebergement>            tableHebergement;
    @FXML private TableColumn<Hebergement, Integer> colId;
    @FXML private TableColumn<Hebergement, String>  colTitre;
    @FXML private TableColumn<Hebergement, String>  colDesc;
    @FXML private TableColumn<Hebergement, Integer> colCapacite;
    @FXML private TableColumn<Hebergement, String>  colType;
    @FXML private TableColumn<Hebergement, Boolean> colDisponible;
    @FXML private TableColumn<Hebergement, Float>   colPrix;
    @FXML private TableColumn<Hebergement, String>  colImage;

    @FXML private TableView<Reservation>            tableReservations;
    @FXML private TableColumn<Reservation, Integer> colResId;
    @FXML private TableColumn<Reservation, String>  colResNom;
    @FXML private TableColumn<Reservation, String>  colResPrenom;
    @FXML private TableColumn<Reservation, String>  colResTel;
    @FXML private TableColumn<Reservation, String>  colResDateDebut;
    @FXML private TableColumn<Reservation, String>  colResDateFin;
    @FXML private TableColumn<Reservation, String>  colResStatut;

    // ========== LABELS ==========
    @FXML private Label lblSelectedHebergement;
    @FXML private Label lblReservationsCount;

    // ========== VIEWS ==========
    @FXML private ScrollPane viewHebergement;
    @FXML private VBox       viewEmpty;

    // ========== SERVICES ==========
    private HebergementCRUD  hebergementCRUD  = new HebergementCRUD();
    private ReservationCRUD  reservationCRUD  = new ReservationCRUD();
    private NotificationCRUD notificationCRUD = new NotificationCRUD();

    // ========== DATA LISTS ==========
    private ObservableList<Hebergement> hebergementList = FXCollections.observableArrayList();
    private ObservableList<Hebergement> allHebergements = FXCollections.observableArrayList();
    private ObservableList<Reservation> reservationList = FXCollections.observableArrayList();

    // ========== SELECTED ITEM ==========
    private Hebergement selectedHebergement = null;

    // ========== STYLES VALIDATION ==========
    private static final String STYLE_ERROR = "-fx-border-color: #E53E3E; -fx-border-width: 1.5; -fx-border-radius: 6;";
    private static final String STYLE_OK    = "-fx-border-color: #1ABC9C; -fx-border-width: 1.5; -fx-border-radius: 6;";
    private static final String STYLE_RESET = "";

    // ========== UTILISATEUR CONNECTÉ ==========
    private User currentUser;
    private String userToken;

    // ============================================================
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ✅ 1. VÉRIFICATION JWT - TOKEN VALIDE ?
        if (!checkUserAuth()) {
            return;
        }

        setupTableColumns();
        setupReservationTableColumns();
        loadHebergements();
        setupTableSelectionListener();
        setupReservationConfirmationListener();
        setupValidationListeners();

        // Recherche en temps réel
        if (txtRecherche != null) {
            txtRecherche.textProperty().addListener((obs, o, n) -> handleRecherche());
        }

        try {
            Twilio.init(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN);
            System.out.println("✅ Twilio initialisé avec succès");
        } catch (Exception e) {
            System.err.println("⚠️ Erreur initialisation Twilio: " + e.getMessage());
        }

        try {
            imagePreview.setImage(new Image(
                    getClass().getResourceAsStream("/hebergement/back/image/logo.png")));
        } catch (Exception e) {
            System.out.println("Logo par défaut non trouvé");
        }

        // ✅ Afficher les infos JWT
        displayUserInfo();
    }

    /**
     * ✅ Vérifie si l'utilisateur est authentifié
     */
    private boolean checkUserAuth() {
        if (!UserSession.getInstance().isTokenValid()) {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Session expirée",
                        "Votre session a expiré. Veuillez vous reconnecter.");
                redirectToLogin();
            });
            return false;
        }

        currentUser = UserSession.getInstance().getCurrentUser();
        userToken = UserSession.getInstance().getToken();

        if (currentUser == null) {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Aucun utilisateur connecté.");
                redirectToLogin();
            });
            return false;
        }

        // ✅ Vérification supplémentaire : correspondance token/session
        Integer userIdFromToken = JWTService.extractUserId(userToken);
        if (userIdFromToken == null || userIdFromToken != currentUser.getId()) {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Session invalide",
                        "Incohérence détectée. Reconnexion nécessaire.");
                UserSession.getInstance().clearSession();
                redirectToLogin();
            });
            return false;
        }

        return true;
    }

    /**
     * ✅ Affiche les informations de l'utilisateur connecté
     */
    private void displayUserInfo() {
        if (currentUser != null) {
            System.out.println("\n🔐 === HÉBERGEMENT CONTROLLER ===");
            System.out.println("👤 Utilisateur: " + currentUser.getE_mail());
            System.out.println("👑 Rôle: " + currentUser.getRole());
            System.out.println("🆔 ID: " + currentUser.getId());

            // ✅ Vérifier si l'utilisateur est admin
            if (!"admin".equals(currentUser.getRole().name())) {
                System.out.println("⚠️ Accès limité - utilisateur non admin");
                // Optionnel : désactiver certaines fonctionnalités pour les non-admins
            }

            System.out.println("🔐 Token JWT actif");
            System.out.println("📝 Token expire le: " + JWTService.extractExpiration(userToken));
            System.out.println("🔐 ==============================\n");
        }
    }

    /**
     * ✅ Redirige vers la page de login
     */
    private void redirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) (tableHebergement != null ?
                    tableHebergement.getScene().getWindow() :
                    btnDeconnexion.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============================================================
    // ✅ VALIDATION EN TEMPS RÉEL — un listener par champ
    // ============================================================
    private void setupValidationListeners() {
        // ── Titre (ville) : lettres/espaces/tirets, min 2 caractères ──
        txtTitre.textProperty().addListener((obs, oldVal, newVal) -> {
            String v = newVal == null ? "" : newVal.trim();
            if (v.isEmpty()) {
                showFieldError(txtTitre, lblTitreErreur, "La ville est obligatoire.");
            } else if (v.length() < 2) {
                showFieldError(txtTitre, lblTitreErreur, "Minimum 2 caractères requis.");
            } else if (!v.matches("[\\p{L}\\s\\-']+")) {
                showFieldError(txtTitre, lblTitreErreur, "Lettres uniquement, pas de chiffres ni de caractères spéciaux.");
            } else {
                showFieldOk(txtTitre, lblTitreErreur);
            }
        });

        // ── Description : min 10 caractères ──
        txtDesc.textProperty().addListener((obs, oldVal, newVal) -> {
            String v = newVal == null ? "" : newVal.trim();
            if (v.isEmpty()) {
                showFieldError(txtDesc, lblDescErreur, "La description est obligatoire.");
            } else if (v.length() < 10) {
                showFieldError(txtDesc, lblDescErreur,
                        "Trop courte — min 10 caractères (actuellement : " + v.length() + ").");
            } else {
                showFieldOk(txtDesc, lblDescErreur);
            }
        });

        // ── Capacité : entier, 1 ≤ valeur ≤ 100 ──
        txtCapacite.textProperty().addListener((obs, oldVal, newVal) -> {
            String v = newVal == null ? "" : newVal.trim();
            if (v.isEmpty()) {
                showFieldError(txtCapacite, lblCapaciteErreur, "La capacité est obligatoire.");
            } else if (!v.matches("\\d+")) {
                showFieldError(txtCapacite, lblCapaciteErreur, "Nombre entier uniquement (ex : 4).");
            } else {
                int cap = Integer.parseInt(v);
                if (cap <= 0) {
                    showFieldError(txtCapacite, lblCapaciteErreur, "La capacité doit être supérieure à 0.");
                } else if (cap > 100) {
                    showFieldError(txtCapacite, lblCapaciteErreur, "Maximum 100 personnes autorisé.");
                } else {
                    showFieldOk(txtCapacite, lblCapaciteErreur);
                }
            }
        });

        // ── Type : lettres uniquement, min 3 caractères ──
        txtType.textProperty().addListener((obs, oldVal, newVal) -> {
            String v = newVal == null ? "" : newVal.trim();
            if (v.isEmpty()) {
                showFieldError(txtType, lblTypeErreur, "Le type est obligatoire.");
            } else if (v.length() < 3) {
                showFieldError(txtType, lblTypeErreur, "Minimum 3 caractères requis.");
            } else if (!v.matches("[\\p{L}\\s\\-']+")) {
                showFieldError(txtType, lblTypeErreur, "Lettres uniquement (ex : Appartement, Villa).");
            } else {
                showFieldOk(txtType, lblTypeErreur);
            }
        });

        // ── Prix : décimal positif, format 0.00, max 100 000 ──
        txtPrix.textProperty().addListener((obs, oldVal, newVal) -> {
            String v = newVal == null ? "" : newVal.trim();
            if (v.isEmpty()) {
                showFieldError(txtPrix, lblPrixErreur, "Le prix est obligatoire.");
            } else if (!v.matches("\\d+(\\.\\d{0,2})?")) {
                showFieldError(txtPrix, lblPrixErreur, "Format invalide — exemple valide : 120.50");
            } else {
                float prix;
                try { prix = Float.parseFloat(v); } catch (NumberFormatException e) { prix = 0; }
                if (prix <= 0) {
                    showFieldError(txtPrix, lblPrixErreur, "Le prix doit être supérieur à 0 TND.");
                } else if (prix > 100000) {
                    showFieldError(txtPrix, lblPrixErreur, "Valeur anormalement élevée.");
                } else {
                    showFieldOk(txtPrix, lblPrixErreur);
                }
            }
        });

        // ── Image : extension valide ──
        txtImage.textProperty().addListener((obs, oldVal, newVal) -> {
            String v = newVal == null ? "" : newVal.trim();
            if (v.isEmpty()) {
                showFieldError(txtImage, lblImageErreur, "Veuillez sélectionner une image.");
            } else if (!v.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif)$")) {
                showFieldError(txtImage, lblImageErreur, "Format accepté : JPG, JPEG, PNG, GIF.");
            } else {
                showFieldOk(txtImage, lblImageErreur);
            }
        });
    }

    // ── Helpers visuels ──────────────────────────────────────────

    /** Affiche le message d'erreur en rouge et colore la bordure en rouge. */
    private void showFieldError(Control field, Label errorLabel, String message) {
        errorLabel.setText("⚠  " + message);
        errorLabel.setVisible(true);
        field.setStyle(STYLE_ERROR);
    }

    /** Cache le message et colore la bordure en vert. */
    private void showFieldOk(Control field, Label errorLabel) {
        errorLabel.setVisible(false);
        field.setStyle(STYLE_OK);
    }

    /** Remet le champ dans son état neutre (ni rouge ni vert). */
    private void resetField(Control field, Label errorLabel) {
        errorLabel.setVisible(false);
        field.setStyle(STYLE_RESET);
    }

    // ============================================================
    // TABLE COLUMNS SETUP
    // ============================================================
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

    private void setupReservationConfirmationListener() {
        colResStatut.setCellFactory(col -> new TableCell<Reservation, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null); setGraphic(null); setStyle("");
                } else {
                    setText(statut);
                    switch (statut.toLowerCase()) {
                        case "confirmée": case "confirmee": case "confirmed":
                            setStyle("-fx-text-fill: #1ABC9C; -fx-font-weight: bold;"); break;
                        case "annulée": case "annulee": case "cancelled":
                            setStyle("-fx-text-fill: #E53E3E; -fx-font-weight: bold;"); break;
                        case "en attente": case "pending":
                            setStyle("-fx-text-fill: #F39C12; -fx-font-weight: bold;"); break;
                        default: setStyle("");
                    }
                }
            }
        });
    }

    private void setupTableSelectionListener() {
        tableHebergement.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        selectedHebergement = newSelection;
                        fillFormWithHebergement(newSelection);
                        loadReservationsForHebergement(newSelection.getId_hebergement());
                    }
                });
    }

    // ============================================================
    // DATA LOADING
    // ============================================================
    private void loadHebergements() {
        try {
            hebergementList.clear();
            allHebergements.clear();
            List<Hebergement> list = hebergementCRUD.afficherh();
            hebergementList.addAll(list);
            allHebergements.addAll(list);
            tableHebergement.setItems(hebergementList);
            updateStats();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger les hébergements: " + e.getMessage());
        }
    }

    private void loadReservationsForHebergement(int hebergementId) {
        try {
            reservationList.clear();
            List<Reservation> list = reservationCRUD.getReservationsByHebergement(hebergementId);
            reservationList.addAll(list);
            tableReservations.setItems(reservationList);
            if (selectedHebergement != null)
                lblSelectedHebergement.setText("Hébergement: " + selectedHebergement.getTitre());
            lblReservationsCount.setText(list.size() + " réservation(s)");
            updateStats();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger les réservations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateStats() {
        int totalHebergements = hebergementList.size();
        int totalReservations = 0;
        try { totalReservations = reservationCRUD.afficherh().size(); }
        catch (SQLException e) { e.printStackTrace(); }
        int disponibles = (int) hebergementList.stream()
                .filter(Hebergement::isDisponible_heberg).count();
        lblTotalHebergements.setText(String.valueOf(totalHebergements));
        lblTotalReservations.setText(String.valueOf(totalReservations));
        lblDisponibles.setText(String.valueOf(disponibles));
    }

    // ============================================================
    // RECHERCHE PAR VILLE OU CAPACITÉ
    // ============================================================
    @FXML
    private void handleRecherche() {
        String keyword = txtRecherche.getText().trim().toLowerCase();
        hebergementList.clear();
        if (keyword.isEmpty()) {
            hebergementList.addAll(allHebergements);
        } else {
            for (Hebergement h : allHebergements) {
                boolean matchVille    = h.getTitre() != null &&
                        h.getTitre().toLowerCase().contains(keyword);
                boolean matchCapacite = String.valueOf(h.getCapacite()).contains(keyword);
                if (matchVille || matchCapacite) hebergementList.add(h);
            }
        }
        selectedHebergement = null;
        reservationList.clear();
        lblSelectedHebergement.setText("Aucun hébergement sélectionné");
        lblReservationsCount.setText("0 réservation(s)");
        updateStats();
    }

    // ============================================================
    // FORM HELPERS
    // ============================================================
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
                imagePreview.setImage(new Image("file:" + h.getImage()));
            } catch (Exception e) {
                try { imagePreview.setImage(new Image(
                        getClass().getResourceAsStream("/hebergement/back/image/logo.png")));
                } catch (Exception ex) { }
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
            imagePreview.setImage(new Image(
                    getClass().getResourceAsStream("/hebergement/back/image/logo.png")));
        } catch (Exception e) { }
        selectedHebergement = null;
        btnAjouter.setText("💾 Enregistrer");
        // Réinitialise bordures + messages
        resetField(txtTitre,    lblTitreErreur);
        resetField(txtDesc,     lblDescErreur);
        resetField(txtCapacite, lblCapaciteErreur);
        resetField(txtType,     lblTypeErreur);
        resetField(txtPrix,     lblPrixErreur);
        resetField(txtImage,    lblImageErreur);
    }

    /** Validation finale au clic — déclenche les mêmes règles que le listener. */
    private boolean validateForm() {
        boolean isValid = true;

        String titre = txtTitre.getText() == null ? "" : txtTitre.getText().trim();
        if (titre.isEmpty()) {
            showFieldError(txtTitre, lblTitreErreur, "La ville est obligatoire."); isValid = false;
        } else if (titre.length() < 2) {
            showFieldError(txtTitre, lblTitreErreur, "Minimum 2 caractères requis."); isValid = false;
        } else if (!titre.matches("[\\p{L}\\s\\-']+")) {
            showFieldError(txtTitre, lblTitreErreur, "Lettres uniquement, pas de chiffres ni de caractères spéciaux."); isValid = false;
        }

        String desc = txtDesc.getText() == null ? "" : txtDesc.getText().trim();
        if (desc.isEmpty()) {
            showFieldError(txtDesc, lblDescErreur, "La description est obligatoire."); isValid = false;
        } else if (desc.length() < 10) {
            showFieldError(txtDesc, lblDescErreur,
                    "Trop courte — min 10 caractères (actuellement : " + desc.length() + ")."); isValid = false;
        }

        String capStr = txtCapacite.getText() == null ? "" : txtCapacite.getText().trim();
        if (capStr.isEmpty()) {
            showFieldError(txtCapacite, lblCapaciteErreur, "La capacité est obligatoire."); isValid = false;
        } else if (!capStr.matches("\\d+")) {
            showFieldError(txtCapacite, lblCapaciteErreur, "Nombre entier uniquement (ex : 4)."); isValid = false;
        } else {
            int cap = Integer.parseInt(capStr);
            if (cap <= 0) {
                showFieldError(txtCapacite, lblCapaciteErreur, "La capacité doit être supérieure à 0."); isValid = false;
            } else if (cap > 100) {
                showFieldError(txtCapacite, lblCapaciteErreur, "Maximum 100 personnes autorisé."); isValid = false;
            }
        }

        String type = txtType.getText() == null ? "" : txtType.getText().trim();
        if (type.isEmpty()) {
            showFieldError(txtType, lblTypeErreur, "Le type est obligatoire."); isValid = false;
        } else if (type.length() < 3) {
            showFieldError(txtType, lblTypeErreur, "Minimum 3 caractères requis."); isValid = false;
        } else if (!type.matches("[\\p{L}\\s\\-']+")) {
            showFieldError(txtType, lblTypeErreur, "Lettres uniquement (ex : Appartement, Villa)."); isValid = false;
        }

        String prixStr = txtPrix.getText() == null ? "" : txtPrix.getText().trim();
        if (prixStr.isEmpty()) {
            showFieldError(txtPrix, lblPrixErreur, "Le prix est obligatoire."); isValid = false;
        } else if (!prixStr.matches("\\d+(\\.\\d{0,2})?")) {
            showFieldError(txtPrix, lblPrixErreur, "Format invalide — exemple valide : 120.50"); isValid = false;
        } else {
            float prix;
            try { prix = Float.parseFloat(prixStr); } catch (NumberFormatException e) { prix = 0; }
            if (prix <= 0) {
                showFieldError(txtPrix, lblPrixErreur, "Le prix doit être supérieur à 0 TND."); isValid = false;
            } else if (prix > 100000) {
                showFieldError(txtPrix, lblPrixErreur, "Valeur anormalement élevée."); isValid = false;
            }
        }

        String img = txtImage.getText() == null ? "" : txtImage.getText().trim();
        if (img.isEmpty()) {
            showFieldError(txtImage, lblImageErreur, "Veuillez sélectionner une image."); isValid = false;
        } else if (!img.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif)$")) {
            showFieldError(txtImage, lblImageErreur, "Format accepté : JPG, JPEG, PNG, GIF."); isValid = false;
        }

        return isValid;
    }

    // ============================================================
    // CRUD ACTIONS
    // ============================================================
    @FXML
    private void addOrUpdate(ActionEvent event) {
        // ✅ Vérification JWT avant toute action
        if (!checkUserAuth()) return;

        if (!validateForm()) return;

        try {
            Hebergement hebergement;
            if (selectedHebergement == null) {
                hebergement = new Hebergement(
                        txtTitre.getText(), txtDesc.getText(),
                        Integer.parseInt(txtCapacite.getText()),
                        txtType.getText(), chkDisponible.isSelected(),
                        Float.parseFloat(txtPrix.getText()), txtImage.getText()
                );
                hebergementCRUD.ajouterh(hebergement);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Hébergement ajouté avec succès !");

                // ✅ Journalisation de l'action
                logAdminAction("CREATE", "Hébergement ajouté: " + txtTitre.getText());
            } else {
                float ancienPrix  = selectedHebergement.getPrixParNuit();
                float nouveauPrix = Float.parseFloat(txtPrix.getText());
                hebergement = selectedHebergement;
                hebergement.setTitre(txtTitre.getText());
                hebergement.setDesc_hebergement(txtDesc.getText());
                hebergement.setCapacite(Integer.parseInt(txtCapacite.getText()));
                hebergement.setType_hebergement(txtType.getText());
                hebergement.setDisponible_heberg(chkDisponible.isSelected());
                hebergement.setPrixParNuit(nouveauPrix);
                hebergement.setImage(txtImage.getText());
                hebergementCRUD.modifierh(hebergement);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Hébergement modifié avec succès !");

                // ✅ Journalisation de l'action
                logAdminAction("UPDATE", "Hébergement modifié: " + txtTitre.getText());

                if (nouveauPrix < ancienPrix)
                    envoyerNotificationsBaissePrix(hebergement, ancienPrix, nouveauPrix);
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

    /**
     * ✅ Journalise les actions admin (optionnel)
     */
    private void logAdminAction(String actionType, String details) {
        if (currentUser != null) {
            System.out.println("📝 [ACTION ADMIN] " + currentUser.getE_mail() +
                    " - " + actionType + " - " + details);
            // Vous pouvez appeler un service de logs ici
        }
    }

    private void envoyerNotificationsBaissePrix(Hebergement hebergement,
                                                float ancienPrix, float nouveauPrix) {
        try {
            List<Reservation> reservations = reservationCRUD
                    .getReservationsByHebergement(hebergement.getId_hebergement());
            Set<Integer> dejaNotifies = new HashSet<>();
            int nbNotifies = 0;
            for (Reservation res : reservations) {
                if (res.getUser() == null) continue;
                int userId = res.getUser().getId();
                if (dejaNotifies.contains(userId)) continue;
                dejaNotifies.add(userId);
                String message = String.format(
                        "Bonne nouvelle ! Le logement \"%s\" est maintenant " +
                                "a %.0f DT au lieu de %.0f DT par nuit !",
                        hebergement.getTitre(), nouveauPrix, ancienPrix);
                notificationCRUD.ajouter(new Notification(userId, message));
                nbNotifies++;
            }
            if (nbNotifies > 0)
                showAlert(Alert.AlertType.INFORMATION, "Notifications envoyées",
                        nbNotifies + " utilisateur(s) notifié(s) de la baisse de prix :\n" +
                                String.format("%s : %.0f DT → %.0f DT/nuit",
                                        hebergement.getTitre(), ancienPrix, nouveauPrix));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur lors de l'envoi des notifications : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void deleteHebergement(ActionEvent event) {
        // ✅ Vérification JWT avant suppression
        if (!checkUserAuth()) return;

        if (selectedHebergement == null) {
            showAlert(Alert.AlertType.WARNING, "Attention",
                    "Veuillez sélectionner un hébergement à supprimer");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'hébergement");
        confirm.setContentText("Voulez-vous vraiment supprimer cet hébergement ?");
        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                hebergementCRUD.supprimerh(selectedHebergement);

                // ✅ Journalisation de l'action
                logAdminAction("DELETE", "Hébergement supprimé: " + selectedHebergement.getTitre());

                showAlert(Alert.AlertType.INFORMATION, "Succès", "Hébergement supprimé avec succès !");
                loadHebergements();
                clearForm();
                reservationList.clear();
                lblSelectedHebergement.setText("Aucun hébergement sélectionné");
                lblReservationsCount.setText("0 réservation(s)");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Erreur lors de la suppression: " + e.getMessage());
            }
        }
    }

    // ============================================================
    // RESERVATION – CONFIRM + SMS
    // ============================================================
    @FXML
    private void confirmerReservation(ActionEvent event) {
        // ✅ Vérification JWT avant confirmation
        if (!checkUserAuth()) return;

        Reservation selectedRes = tableReservations.getSelectionModel().getSelectedItem();
        if (selectedRes == null) {
            showAlert(Alert.AlertType.WARNING, "Attention",
                    "Veuillez sélectionner une réservation à confirmer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la réservation");
        confirm.setHeaderText("Confirmation de réservation");
        confirm.setContentText(
                "Voulez-vous confirmer cette réservation et notifier l'utilisateur par SMS ?");
        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                selectedRes.setStatutR("Confirmée");
                reservationCRUD.updateStatut(selectedRes);
                if (selectedHebergement != null)
                    loadReservationsForHebergement(selectedHebergement.getId_hebergement());
                User user = selectedRes.getUser();
                if (user != null && user.getNum_tel() != null && !user.getNum_tel().isEmpty()) {
                    envoyerSMSConfirmation(user, selectedRes, selectedHebergement);
                } else {
                    showAlert(Alert.AlertType.WARNING, "SMS non envoyé",
                            "Réservation confirmée, mais le numéro de téléphone est introuvable.");
                }

                // ✅ Journalisation de l'action
                logAdminAction("CONFIRM", "Réservation confirmée pour " +
                        (user != null ? user.getPrenom() + " " + user.getNom() : "utilisateur inconnu"));

            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Erreur lors de la confirmation: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void envoyerSMSConfirmation(User user, Reservation reservation,
                                        Hebergement hebergement) {
        new Thread(() -> {
            try {
                String numeroDestinataire = formaterNumero(user.getNum_tel());
                String nomHebergement    = hebergement != null ? hebergement.getTitre() : "hébergement";
                String typeHebergement   = hebergement != null ? hebergement.getType_hebergement() : "";
                String prix              = hebergement != null
                        ? String.format("%.2f", hebergement.getPrixParNuit()) : "N/A";
                String messageBody = String.format(
                        "RE7LA Tunisie - Confirmation de Reservation\n\n" +
                                "Bonjour %s %s,\n" +
                                "Votre reservation a ete CONFIRMEE !\n\n" +
                                "Hebergement : %s (%s)\n" +
                                "Du : %s\n" +
                                "Au : %s\n" +
                                "Prix/nuit : %s TND\n\n" +
                                "Merci de votre confiance !\n" +
                                "L'equipe RE7LA Tunisie",
                        user.getPrenom(), user.getNom(),
                        nomHebergement, typeHebergement,
                        reservation.getDateDebutR(), reservation.getDateFinR(), prix);
                Message message = Message.creator(
                        new PhoneNumber(numeroDestinataire),
                        new PhoneNumber(TWILIO_PHONE_FROM),
                        messageBody).create();
                System.out.println("✅ SMS envoyé ! SID: " + message.getSid());
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.INFORMATION, "SMS Envoyé ✅",
                                "Réservation confirmée et SMS envoyé à " +
                                        user.getPrenom() + " " + user.getNom() +
                                        "\nNuméro : " + numeroDestinataire));
            } catch (Exception e) {
                System.err.println("❌ Erreur envoi SMS: " + e.getMessage());
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.WARNING, "SMS non envoyé",
                                "Réservation confirmée, mais l'envoi du SMS a échoué.\n" +
                                        "Erreur : " + e.getMessage()));
            }
        }).start();
    }

    private String formaterNumero(String numero) {
        if (numero == null || numero.trim().isEmpty()) return "";
        numero = numero.trim().replaceAll("[\\s\\-().]+", "");
        if (numero.startsWith("+"))                              return numero;
        if (numero.startsWith("00216"))                         return "+" + numero.substring(2);
        if (numero.startsWith("216") && numero.length() == 11) return "+" + numero;
        if (numero.length() == 8)                               return "+216" + numero;
        return "+216" + numero;
    }

    // ============================================================
    // IMAGE BROWSER
    // ============================================================
    @FXML
    private void browseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.gif", "*.jpeg"));
        File selectedFile = fileChooser.showOpenDialog(txtImage.getScene().getWindow());
        if (selectedFile != null) {
            String imagePath = selectedFile.getAbsolutePath();
            txtImage.setText(imagePath);
            try {
                imagePreview.setImage(new Image("file:" + imagePath));
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger l'image");
            }
        }
    }

    // ============================================================
    // RAPPORT IA MENSUEL
    // ============================================================
    @FXML
    private void handleShowRapportIA(ActionEvent event) {
        // ✅ Vérification JWT avant d'ouvrir le rapport
        if (!checkUserAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/hebergement/back/RapportIA.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("🤖 Rapport Mensuel IA – RE7LA Tunisie");
            stage.setScene(new Scene(root, 900, 600));
            stage.setResizable(true);
            stage.setMinWidth(700);
            stage.setMinHeight(500);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le rapport IA : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================
    // ANALYSE CONCURRENTIELLE
    // ============================================================
    @FXML
    private void handleShowAnalyseConcurrentielle(ActionEvent event) {
        // ✅ Vérification JWT avant d'ouvrir l'analyse
        if (!checkUserAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/hebergement/back/AnalyseConcurrentielle.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("📊 Analyse Concurrentielle – RE7LA Tunisie");
            stage.setScene(new Scene(root, 1000, 680));
            stage.setResizable(true);
            stage.setMinWidth(800);
            stage.setMinHeight(550);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir l'analyse concurrentielle : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================
    // NAVIGATION SIDEBAR
    // ============================================================
    @FXML
    private void handleShowUsers(ActionEvent event) {
        // ✅ Vérification JWT avant navigation
        if (!checkUserAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/user/back/users.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnGestionUtilisateurs.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/user/back/users.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Gestion des Utilisateurs");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger l'interface utilisateurs: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowCommunaute(ActionEvent event) {
        if (!checkUserAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/communaute/PublicationCommentaire.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            // Ajouter le CSS si nécessaire
            try {
                String css = getClass().getResource("/communaute/Communaute.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("⚠️ CSS communauté non trouvé, chargement sans styles");
            }

            stage.setScene(scene);
            stage.setTitle("Gestion de la Communauté - RE7LA");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger l'interface communauté: " + e.getMessage());
        }
    }

    @FXML private void handleShowAccounts(ActionEvent event)     {
        if (checkUserAuth()) {
            viewHebergement.setVisible(true);
            viewEmpty.setVisible(false);
        }
    }

    @FXML
    private void handleShowTransactions(ActionEvent event) {
        if (!checkUserAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/activite/views/backoffice/ActiviteBackOffice.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnTransactions.getScene().getWindow();
            Scene scene = new Scene(root);

            // Ajouter le CSS si nécessaire
            try {
                String css = getClass().getResource("/activite/css/Bstyle.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("⚠️ CSS non trouvé, chargement sans styles");
            }

            stage.setScene(scene);
            stage.setTitle("Gestion des Activités - RE7LA");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger l'interface Activités: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowCredits(ActionEvent event) {
        if (!checkUserAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Trajet/gestionvoitureettrajet.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnCredits.getScene().getWindow();
            Scene scene = new Scene(root);

            // Ajouter le CSS si nécessaire
            try {
                String css = getClass().getResource("/Trajet/style.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("⚠️ CSS non trouvé, chargement sans styles");
            }

            stage.setScene(scene);
            stage.setTitle("Gestion des Voitures et Trajets - RE7LA");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger l'interface Trajet: " + e.getMessage());
        }
    }

    @FXML private void handleShowCashback(ActionEvent event)     {
        if (checkUserAuth()) {
            viewHebergement.setVisible(false);
            viewEmpty.setVisible(true);
        }
    }

    @FXML private void handleShowReservations(ActionEvent event) {
        if (checkUserAuth()) {
            viewHebergement.setVisible(true);
            viewEmpty.setVisible(false);
        }
    }

    @FXML private void goToReservations(ActionEvent event)       {
        if (checkUserAuth()) {
            tableReservations.requestFocus();
        }
    }

    @FXML private void handleShowSettings(ActionEvent event)     {
        if (checkUserAuth()) {
            viewHebergement.setVisible(false);
            viewEmpty.setVisible(true);
        }
    }

    @FXML private void handleCancel(ActionEvent event)           {
        clearForm();
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            // ✅ Effacer la session JWT
            UserSession.getInstance().clearSession();
            System.out.println("✅ Session JWT effacée - Déconnexion réussie");

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/user/login/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnDeconnexion.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger la page de connexion: " + e.getMessage());
        }
    }

    // ============================================================
    // UTILS
    // ============================================================
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
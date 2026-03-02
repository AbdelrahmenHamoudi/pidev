package org.example.Controllers.Trajet;

import javafx.application.Platform;
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
import javafx.stage.Stage;
import org.example.Entites.trajet.StatutVoiture;
import org.example.Entites.trajet.Trajet;
import org.example.Entites.trajet.Voiture;
import org.example.Entites.user.User;
import org.example.Services.trajet.TrajetCRUD;
import org.example.Services.trajet.VoitureCRUD;
import org.example.Services.user.APIservices.JWTService;
import org.example.Utils.UserSession;

// ── Twilio SendGrid ──────────────────────────────────────────────────────────
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
// ────────────────────────────────────────────────────────────────────────────

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Comparator;

public class FrontController implements Initializable {

    @FXML private AnchorPane rootPane;

    private static final Logger LOGGER = Logger.getLogger(FrontController.class.getName());

    // ==================== UTILISATEUR CONNECTÉ ====================
    private User currentUser;
    private String userToken;

    // ── Twilio SendGrid config ───────────────────────────────────────────────
    private static final String SENDGRID_API_KEY = "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";
    private static final String FROM_EMAIL       = "dhiambarki001@gmail.com";
    // ────────────────────────────────────────────────────────────────────────

    // ==================== CONSTANTES ====================
    private static final int ITEMS_PER_PAGE_VOITURES = 9;
    private static final int ITEMS_PER_PAGE_TRAJETS  = 5;

    private Voiture voitureSelectionnee;
    private Trajet  trajetSelectionne;
    private boolean distanceApiEnCours = false;

    // ==================== FXML COMPONENTS ====================

    @FXML private Button btnOuvrirCarte;
    @FXML private Button btnVoitures;
    @FXML private Button btnTrajets;
    @FXML private Label  userNameLabel;
    @FXML private Label  userEmailLabel;
    @FXML private Label  userPointsLabel;
    @FXML private VBox   voituresSection;
    @FXML private VBox   trajetsSection;
    @FXML private VBox   filtresVoituresPanel;
    @FXML private VBox   filtresTrajetsPanel;

    // --- Navigation buttons ---
    @FXML private Button btnAccueil;
    @FXML private Button btnProfil;
    @FXML private Button btnLogout;

    // --- Voitures Section ---
    @FXML private TextField         searchVoituresField;
    @FXML private ComboBox<String>  filterPrixCombo;
    @FXML private ComboBox<String>  filterPlacesCombo;
    @FXML private ComboBox<String>  sortVoituresCombo;
    @FXML private GridPane          voituresGrid;
    @FXML private Label             pageVoituresLabel;
    @FXML private Button            btnResetVoitures;

    // --- Trajets Section ---
    @FXML private ComboBox<String>  departCombo;
    @FXML private ComboBox<String>  arriveeCombo;
    @FXML private DatePicker        dateTrajetPicker;
    @FXML private Spinner<Integer>  nbPersonnesSpinner;
    @FXML private ComboBox<String>  sortTrajetsCombo;
    @FXML private VBox              trajetsListContainer;
    @FXML private Label             pageTrajetsLabel;
    @FXML private Button            btnResetTrajets;

    // --- Formulaire CRUD Trajets ---
    @FXML private ComboBox<String>  cbDepart;
    @FXML private ComboBox<String>  cbArrivee;
    @FXML private DatePicker        dpDate;
    @FXML private Spinner<Integer>  spNbPersonnes;
    @FXML private TextField         tfDistance;
    @FXML private Button            btnAjouterTrajet;
    @FXML private Button            btnModifierTrajet;
    @FXML private Button            btnSupprimerTrajet;
    @FXML private Button            btnGestionVoiture2;

    @FXML private Button btnAssistantFlottant;
    @FXML private Label  fabTooltip;

    // ==================== SERVICES & DATA ====================

    private VoitureCRUD voitureCRUD;
    private TrajetCRUD  trajetCRUD;

    private ObservableList<Voiture> voituresList;
    private ObservableList<Voiture> voituresListFiltered;
    private ObservableList<Trajet>  trajetsList;
    private ObservableList<Trajet>  trajetsListFiltered;

    private int currentPageVoitures = 0;
    private int currentPageTrajets  = 0;

    // ==================== INITIALIZE ====================

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // ✅ 1. VÉRIFICATION JWT
        if (!checkUserAuth()) {
            return;
        }

        setBackgroundImage();

        if (btnAssistantFlottant != null) {
            javafx.animation.ScaleTransition pulse =
                    new javafx.animation.ScaleTransition(
                            javafx.util.Duration.millis(1200), btnAssistantFlottant);
            pulse.setFromX(1.0); pulse.setToX(1.06);
            pulse.setFromY(1.0); pulse.setToY(1.06);
            pulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
            pulse.setAutoReverse(true);
            pulse.play();
        }

        voitureCRUD = new VoitureCRUD();
        trajetCRUD  = new TrajetCRUD();

        setupFilters();
        setupSpinners();
        setupValidation();
        setupSearchListeners();

        loadVoitures();
        loadTrajets();
        showVoituresSection();

        if (pageVoituresLabel != null) pageVoituresLabel.setText("1");
        if (pageTrajetsLabel  != null) pageTrajetsLabel.setText("1");
    }

    // ==================== AUTH JWT ====================

    /**
     * ✅ Vérifie si l'utilisateur est authentifié via JWT
     */
    private boolean checkUserAuth() {
        if (!UserSession.getInstance().isTokenValid()) {
            Platform.runLater(() -> {
                showAlert("Session expirée", "Votre session a expiré. Veuillez vous reconnecter.");
                redirectToLogin();
            });
            return false;
        }

        currentUser = UserSession.getInstance().getCurrentUser();
        userToken   = UserSession.getInstance().getToken();

        if (currentUser == null) {
            Platform.runLater(() -> {
                showAlert("Erreur", "Aucun utilisateur connecté.");
                redirectToLogin();
            });
            return false;
        }

        // ✅ Vérification supplémentaire : correspondance token/session
        Integer userIdFromToken = JWTService.extractUserId(userToken);
        if (userIdFromToken == null || userIdFromToken != currentUser.getId()) {
            Platform.runLater(() -> {
                showAlert("Session invalide", "Incohérence détectée. Reconnexion nécessaire.");
                UserSession.getInstance().clearSession();
                redirectToLogin();
            });
            return false;
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
            Stage stage = (Stage) (btnAccueil != null ?
                    btnAccueil.getScene().getWindow() :
                    btnProfil.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== NAVIGATION ACCUEIL / PROFIL ====================

    @FXML
    private void goToAccueil() {
        if (!checkUserAuth()) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/dashboard/homeClient.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnAccueil.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Accueil");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de retourner à l'accueil: " + e.getMessage());
        }
    }

    @FXML
    private void goToProfil() {
        if (!checkUserAuth()) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/front/userProfil.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnProfil.getScene().getWindow();
            Scene scene = new Scene(root);
            String css = getClass().getResource("/user/front/userProfil.css").toExternalForm();
            scene.getStylesheets().add(css);
            stage.setScene(scene);
            stage.setTitle("RE7LA Tunisie - Mon Profil");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le profil: " + e.getMessage());
        }
    }

    // ==================== BACKGROUND ====================

    private void setBackgroundImage() {
        try {
            Image backgroundImage = new Image(
                    Objects.requireNonNull(
                            getClass().getResource("/images/fonddufront.png")).toExternalForm());
            BackgroundSize bs = new BackgroundSize(100, 100, true, true, true, false);
            rootPane.setBackground(new Background(new BackgroundImage(
                    backgroundImage,
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER, bs)));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur chargement background", e);
        }
    }

    // ==================== SETUP ====================

    private void setupFilters() {
        ObservableList<String> villes = FXCollections.observableArrayList(
                "Tunis", "Sousse", "Sfax", "Bizerte", "Gabès",
                "Ariana", "Nabeul", "Hammamet", "Kairouan", "Monastir",
                "Mahdia", "Sidi Bouzid", "Gafsa", "Tozeur", "Djerba",
                "El Kef", "Jendouba", "Siliana", "Zaghouan", "Kasserine",
                "Ben Arous", "Manouba", "Medenine", "Beja", "Tataouine"
        );

        if (filterPrixCombo != null) {
            filterPrixCombo.setItems(FXCollections.observableArrayList(
                    "Tous les prix", "< 1 DT/km", "1-1.5 DT/km", "> 1.5 DT/km"));
            filterPrixCombo.setValue("Tous les prix");
            filterPrixCombo.setOnAction(e -> appliquerFiltresVoitures());
        }
        if (filterPlacesCombo != null) {
            filterPlacesCombo.setItems(FXCollections.observableArrayList(
                    "Toutes", "2-4 places", "5-7 places", "7+ places"));
            filterPlacesCombo.setValue("Toutes");
            filterPlacesCombo.setOnAction(e -> appliquerFiltresVoitures());
        }
        if (sortVoituresCombo != null) {
            sortVoituresCombo.setItems(FXCollections.observableArrayList(
                    "Plus récentes", "Prix croissant", "Prix décroissant", "Places (croissant)"));
            sortVoituresCombo.setValue("Plus récentes");
            sortVoituresCombo.setOnAction(e -> appliquerFiltresVoitures());
        }

        if (departCombo  != null) { departCombo.setItems(villes);  departCombo.setOnAction(e -> appliquerFiltresTrajets()); }
        if (arriveeCombo != null) { arriveeCombo.setItems(villes); arriveeCombo.setOnAction(e -> appliquerFiltresTrajets()); }
        if (dateTrajetPicker != null) dateTrajetPicker.setOnAction(e -> appliquerFiltresTrajets());
        if (sortTrajetsCombo != null) {
            sortTrajetsCombo.setItems(FXCollections.observableArrayList(
                    "Date (proche)", "Date (éloignée)", "Prix croissant", "Prix décroissant"));
            sortTrajetsCombo.setValue("Date (proche)");
            sortTrajetsCombo.setOnAction(e -> appliquerFiltresTrajets());
        }

        if (cbDepart  != null) { cbDepart.setItems(villes);  cbDepart.setOnAction(e -> updateDistanceAutomatically()); }
        if (cbArrivee != null) { cbArrivee.setItems(villes); cbArrivee.setOnAction(e -> updateDistanceAutomatically()); }
    }

    private void setupSpinners() {
        if (nbPersonnesSpinner != null) {
            nbPersonnesSpinner.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1));
            nbPersonnesSpinner.valueProperty().addListener((obs, o, n) -> appliquerFiltresTrajets());
        }
        if (spNbPersonnes != null) {
            spNbPersonnes.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1));
        }
    }

    private void setupValidation() {
        if (tfDistance != null) {
            tfDistance.textProperty().addListener((obs, oldVal, newVal) -> {
                if (distanceApiEnCours) return;
                if (!newVal.matches("\\d*(\\.\\d*)?")) tfDistance.setText(oldVal);
            });
        }
        if (dpDate != null) {
            dpDate.setDayCellFactory(picker -> new DateCell() {
                @Override public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    setDisable(empty || date.isBefore(LocalDate.now()));
                }
            });
        }
    }

    private void setupSearchListeners() {
        if (searchVoituresField != null)
            searchVoituresField.textProperty()
                    .addListener((obs, o, n) -> appliquerFiltresVoitures());
    }

    // ==================== NAVIGATION SECTIONS ====================

    @FXML
    private void showVoituresSection() {
        if (voituresSection      != null) { voituresSection.setVisible(true);       voituresSection.setManaged(true); }
        if (trajetsSection       != null) { trajetsSection.setVisible(false);       trajetsSection.setManaged(false); }
        if (filtresVoituresPanel != null) { filtresVoituresPanel.setVisible(true);  filtresVoituresPanel.setManaged(true); }
        if (filtresTrajetsPanel  != null) { filtresTrajetsPanel.setVisible(false);  filtresTrajetsPanel.setManaged(false); }
        updateNavigationButtonStyles("voitures");
    }

    @FXML
    private void showTrajetsSection() {
        if (voituresSection      != null) { voituresSection.setVisible(false);      voituresSection.setManaged(false); }
        if (trajetsSection       != null) { trajetsSection.setVisible(true);        trajetsSection.setManaged(true); }
        if (filtresVoituresPanel != null) { filtresVoituresPanel.setVisible(false); filtresVoituresPanel.setManaged(false); }
        if (filtresTrajetsPanel  != null) { filtresTrajetsPanel.setVisible(true);   filtresTrajetsPanel.setManaged(true); }
        updateNavigationButtonStyles("trajets");
    }

    private void updateNavigationButtonStyles(String active) {
        String on  = "-fx-background-color: rgba(59,130,246,0.15); -fx-text-fill: #3b82f6; -fx-font-weight: bold;";
        String off = "";
        if (btnVoitures != null) btnVoitures.setStyle(active.equals("voitures") ? on : off);
        if (btnTrajets  != null) btnTrajets.setStyle(active.equals("trajets")   ? on : off);
    }

    // ==================== CHARGEMENT DONNÉES ====================

    private void loadVoitures() {
        try {
            List<Voiture> list = voitureCRUD.afficherh();
            if (list == null || list.isEmpty()) {
                if (voituresGrid != null) voituresGrid.getChildren().clear();
                return;
            }
            voituresList         = FXCollections.observableArrayList(list);
            voituresListFiltered = FXCollections.observableArrayList(list);
            currentPageVoitures  = 0;
            if (pageVoituresLabel != null) pageVoituresLabel.setText("1");
            displayVoituresGrid();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement voitures", e);
            showAlert("Erreur", "Erreur chargement voitures : " + e.getMessage());
        }
    }

    private void loadTrajets() {
        try {
            // ✅ Charger les trajets avec les informations utilisateur (JOINTURE)
            List<Trajet> list = trajetCRUD.getTrajetsWithConducteur();
            if (list == null || list.isEmpty()) {
                if (trajetsListContainer != null) trajetsListContainer.getChildren().clear();
                return;
            }
            trajetsList         = FXCollections.observableArrayList(list);
            trajetsListFiltered = FXCollections.observableArrayList(list);
            currentPageTrajets  = 0;
            if (pageTrajetsLabel != null) pageTrajetsLabel.setText("1");
            appliquerFiltresTrajets();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement trajets", e);
            showAlert("Erreur", "Erreur chargement trajets : " + e.getMessage());
        }
    }

    // ==================== FILTRES VOITURES ====================

    private void appliquerFiltresVoitures() {
        if (voituresList == null) return;
        voituresListFiltered = FXCollections.observableArrayList(voituresList);

        String keyword = (searchVoituresField != null) ? searchVoituresField.getText() : "";
        if (keyword != null && !keyword.isBlank()) {
            String lk = keyword.toLowerCase();
            voituresListFiltered.removeIf(v ->
                    !v.getMarque().toLowerCase().contains(lk)
                            && !v.getModele().toLowerCase().contains(lk));
        }

        String filterPrix = (filterPrixCombo != null) ? filterPrixCombo.getValue() : "Tous les prix";
        if (filterPrix != null && !filterPrix.equals("Tous les prix")) {
            voituresListFiltered.removeIf(v -> {
                float p = v.getPrixKm();
                switch (filterPrix) {
                    case "< 1 DT/km":   return p >= 1.0f;
                    case "1-1.5 DT/km": return p < 1.0f || p > 1.5f;
                    case "> 1.5 DT/km": return p <= 1.5f;
                    default:            return false;
                }
            });
        }

        String filterPlaces = (filterPlacesCombo != null) ? filterPlacesCombo.getValue() : "Toutes";
        if (filterPlaces != null && !filterPlaces.equals("Toutes")) {
            voituresListFiltered.removeIf(v -> {
                int pl = v.getNb_places();
                switch (filterPlaces) {
                    case "2-4 places": return pl < 2 || pl > 4;
                    case "5-7 places": return pl < 5 || pl > 7;
                    case "7+ places":  return pl < 7;
                    default:           return false;
                }
            });
        }

        String sort = (sortVoituresCombo != null) ? sortVoituresCombo.getValue() : "Plus récentes";
        if (sort != null) {
            switch (sort) {
                case "Prix croissant":
                    voituresListFiltered.sort(Comparator.comparing(Voiture::getPrixKm));
                    break;
                case "Prix décroissant":
                    voituresListFiltered.sort(Comparator.comparing(Voiture::getPrixKm).reversed());
                    break;
                case "Places (croissant)":
                    voituresListFiltered.sort(Comparator.comparing(Voiture::getNb_places));
                    break;
                default:
                    voituresListFiltered.sort(Comparator.comparing(Voiture::getIdVoiture).reversed());
                    break;
            }
        }

        currentPageVoitures = 0;
        if (pageVoituresLabel != null) pageVoituresLabel.setText("1");
        displayVoituresGrid();
    }

    // ==================== FILTRES TRAJETS ====================

    private void appliquerFiltresTrajets() {
        if (trajetsList == null) return;
        trajetsListFiltered = FXCollections.observableArrayList(trajetsList);

        String depart = (departCombo != null) ? departCombo.getValue() : null;
        if (depart != null && !depart.isBlank())
            trajetsListFiltered.removeIf(t -> !t.getPointDepart().equalsIgnoreCase(depart));

        String arrivee = (arriveeCombo != null) ? arriveeCombo.getValue() : null;
        if (arrivee != null && !arrivee.isBlank())
            trajetsListFiltered.removeIf(t -> !t.getPointArrivee().equalsIgnoreCase(arrivee));

        LocalDate date = (dateTrajetPicker != null) ? dateTrajetPicker.getValue() : null;
        if (date != null) {
            trajetsListFiltered.removeIf(t -> {
                if (t.getDateReservation() == null) return true;
                return !t.getDateReservation().toLocalDate().equals(date);
            });
        }

        Integer nbP = (nbPersonnesSpinner != null && nbPersonnesSpinner.getValue() != null)
                ? nbPersonnesSpinner.getValue() : 1;
        if (nbP > 1) trajetsListFiltered.removeIf(t -> t.getNbPersonnes() < nbP);

        String sort = (sortTrajetsCombo != null) ? sortTrajetsCombo.getValue() : "Date (proche)";
        if (sort != null) {
            switch (sort) {
                case "Date (éloignée)":
                    trajetsListFiltered.sort((a, b) -> {
                        if (a.getDateReservation() == null) return 1;
                        if (b.getDateReservation() == null) return -1;
                        return b.getDateReservation().compareTo(a.getDateReservation());
                    });
                    break;
                case "Prix croissant":
                    trajetsListFiltered.sort(Comparator.comparingDouble(t ->
                            t.getIdVoiture() != null
                                    ? t.getIdVoiture().getPrixKm() * t.getDistanceKm() / Math.max(t.getNbPersonnes(), 1)
                                    : 0));
                    break;
                case "Prix décroissant":
                    trajetsListFiltered.sort((a, b) -> {
                        double pa = a.getIdVoiture() != null ? a.getIdVoiture().getPrixKm() * a.getDistanceKm() / Math.max(a.getNbPersonnes(), 1) : 0;
                        double pb = b.getIdVoiture() != null ? b.getIdVoiture().getPrixKm() * b.getDistanceKm() / Math.max(b.getNbPersonnes(), 1) : 0;
                        return Double.compare(pb, pa);
                    });
                    break;
                default:
                    trajetsListFiltered.sort((a, b) -> {
                        if (a.getDateReservation() == null) return 1;
                        if (b.getDateReservation() == null) return -1;
                        return a.getDateReservation().compareTo(b.getDateReservation());
                    });
                    break;
            }
        }

        currentPageTrajets = 0;
        if (pageTrajetsLabel != null) pageTrajetsLabel.setText("1");
        displayTrajetsList();
    }

    @FXML private void searchVoitures() { appliquerFiltresVoitures(); }
    @FXML private void searchTrajets()  { appliquerFiltresTrajets();  }

    // ==================== RESET FILTRES ====================

    @FXML
    private void resetFiltresVoitures() {
        if (filterPrixCombo     != null) filterPrixCombo.setValue("Tous les prix");
        if (filterPlacesCombo   != null) filterPlacesCombo.setValue("Toutes");
        if (sortVoituresCombo   != null) sortVoituresCombo.setValue("Plus récentes");
        if (searchVoituresField != null) searchVoituresField.clear();
        voitureSelectionnee = null;
        if (voituresList != null) {
            voituresListFiltered = FXCollections.observableArrayList(voituresList);
            currentPageVoitures  = 0;
            if (pageVoituresLabel != null) pageVoituresLabel.setText("1");
            displayVoituresGrid();
        }
    }

    @FXML
    private void resetFiltresTrajets() {
        if (departCombo        != null) departCombo.setValue(null);
        if (arriveeCombo       != null) arriveeCombo.setValue(null);
        if (dateTrajetPicker   != null) dateTrajetPicker.setValue(null);
        if (nbPersonnesSpinner != null) nbPersonnesSpinner.getValueFactory().setValue(1);
        if (sortTrajetsCombo   != null) sortTrajetsCombo.setValue("Date (proche)");
        trajetSelectionne = null;
        if (trajetsList != null) {
            trajetsListFiltered = FXCollections.observableArrayList(trajetsList);
            currentPageTrajets  = 0;
            if (pageTrajetsLabel != null) pageTrajetsLabel.setText("1");
            displayTrajetsList();
        }
    }

    @FXML private void resetFilters() { resetFiltresVoitures(); resetFiltresTrajets(); }

    // ==================== AFFICHAGE VOITURES ====================

    private void displayVoituresGrid() {
        if (voituresGrid == null || voituresListFiltered == null) return;
        voituresGrid.getChildren().clear();
        int start = currentPageVoitures * ITEMS_PER_PAGE_VOITURES;
        int end   = Math.min(start + ITEMS_PER_PAGE_VOITURES, voituresListFiltered.size());
        int col = 0, row = 0;
        for (int i = start; i < end; i++) {
            voituresGrid.add(createCarCard(voituresListFiltered.get(i)), col, row);
            if (++col == 3) { col = 0; row++; }
        }
    }

    private VBox createCarCard(Voiture voiture) {
        VBox card = new VBox();
        card.getStyleClass().add("car-card");
        card.setPrefWidth(350); card.setMaxWidth(350);

        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("car-image-container");

        ImageView imageView = new ImageView();
        try {
            if (voiture.getImage() != null && !voiture.getImage().isEmpty())
                imageView.setImage(new Image("file:" + voiture.getImage()));
            else
                imageView.setImage(new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/images/car-default.png"))));
        } catch (Exception e) { LOGGER.log(Level.WARNING, "Erreur image voiture", e); }
        imageView.setFitWidth(350); imageView.setFitHeight(200); imageView.setPreserveRatio(true);

        Label badge = new Label("Disponible");
        badge.getStyleClass().add("car-badge");
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(15));
        imageContainer.getChildren().addAll(imageView, badge);

        VBox details = new VBox(12);
        details.getStyleClass().add("car-details");

        Label title    = new Label(voiture.getMarque() + " " + voiture.getModele());
        title.getStyleClass().add("car-title");
        Label subtitle = new Label("Berline • Automatique");
        subtitle.getStyleClass().add("car-subtitle");

        HBox features = new HBox(15);
        features.getChildren().addAll(
                new Label("👤 " + voiture.getNb_places() + " places"),
                new Label("🧳 " + (voiture.getNb_places() - 2) + " bagages"),
                new Label("⚡ Clim"));

        HBox priceSection = new HBox(); priceSection.setAlignment(Pos.CENTER);
        VBox priceBox = new VBox();
        Label price = new Label(String.format("%.2f DT/km", voiture.getPrixKm()));
        price.getStyleClass().add("car-price");
        Label priceDetail = new Label(voiture.isAvecChauffeur() ? "avec chauffeur" : "sans chauffeur");
        priceDetail.getStyleClass().add("car-price-detail");
        priceBox.getChildren().addAll(price, priceDetail);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button selectBtn = new Button("Sélectionner");
        selectBtn.getStyleClass().add("reserve-button");
        selectBtn.setOnAction(e -> selectionnerVoiture(voiture));

        priceSection.getChildren().addAll(priceBox, spacer, selectBtn);
        details.getChildren().addAll(title, subtitle, features, new Separator(), priceSection);
        card.getChildren().addAll(imageContainer, details);
        return card;
    }

    // ==================== AFFICHAGE TRAJETS ====================

    private void displayTrajetsList() {
        if (trajetsListContainer == null || trajetsListFiltered == null) return;
        trajetsListContainer.getChildren().clear();
        int start = currentPageTrajets * ITEMS_PER_PAGE_TRAJETS;
        int end   = Math.min(start + ITEMS_PER_PAGE_TRAJETS, trajetsListFiltered.size());
        for (int i = start; i < end; i++)
            trajetsListContainer.getChildren().add(createTripCard(trajetsListFiltered.get(i)));
    }

    private HBox createTripCard(Trajet trajet) {
        HBox card = new HBox(); card.getStyleClass().add("trip-card");

        VBox leftSection = new VBox(10); HBox.setHgrow(leftSection, Priority.ALWAYS);
        HBox routeInfo   = new HBox(20); routeInfo.setAlignment(Pos.CENTER_LEFT);

        // Informations du conducteur (JOINTURE USER)
        String conducteurInfo = (trajet.getIdUser() != null)
                ? "👤 " + trajet.getIdUser().getPrenom() + " " + trajet.getIdUser().getNom()
                : "👤 Conducteur inconnu";
        Label conducteur = new Label(conducteurInfo);
        conducteur.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-weight: bold;");

        Label route = new Label("📍 " + trajet.getPointDepart() + " → " + trajet.getPointArrivee());
        route.getStyleClass().add("trip-route");
        Label distance = new Label(String.format("%.0f km", trajet.getDistanceKm()));
        distance.getStyleClass().add("trip-distance");
        Label date = new Label("📅 " + new SimpleDateFormat("dd MMM yyyy").format(trajet.getDateReservation()));
        date.getStyleClass().add("trip-date");

        Region spacer1 = new Region(); HBox.setHgrow(spacer1, Priority.ALWAYS);
        Label status = new Label(trajet.getIdVoiture() != null ? "Réservé" : "Disponible");
        status.getStyleClass().add(trajet.getIdVoiture() != null ? "badge-warning" : "status-badge-available");

        routeInfo.getChildren().addAll(conducteur,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                route, new Label("•"), distance, new Label("•"), date, spacer1, status);

        HBox timeCapacity = new HBox(30); timeCapacity.setAlignment(Pos.CENTER_LEFT);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String heureDepart = timeFormat.format(trajet.getDateReservation());
        long dureeMinutes  = Math.round(trajet.getDistanceKm() / 80.0 * 60);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(trajet.getDateReservation());
        cal.add(java.util.Calendar.MINUTE, (int) dureeMinutes);
        String heureArrivee = timeFormat.format(cal.getTime());

        VBox departBox = new VBox(5);
        departBox.getChildren().addAll(labelStyled("Départ", "trip-label"), labelStyled(heureDepart, "trip-time"));
        Label arrow = new Label("━━━━━🚗━━━━━►"); arrow.getStyleClass().add("trip-arrow");
        VBox arriveeBox = new VBox(5);
        arriveeBox.getChildren().addAll(labelStyled("Arrivée", "trip-label"), labelStyled(heureArrivee, "trip-time"));

        Region spacer2 = new Region(); HBox.setHgrow(spacer2, Priority.ALWAYS);
        VBox capacityBox = new VBox(5);
        Label capacity = new Label("👤 " + trajet.getNbPersonnes() + " personnes");
        capacity.getStyleClass().add("trip-capacity");
        String carInfo = trajet.getIdVoiture() != null
                ? trajet.getIdVoiture().getMarque() + " " + trajet.getIdVoiture().getModele()
                : "🚗 En attente d'affectation";
        Label car = new Label(carInfo); car.getStyleClass().add("trip-car");
        capacityBox.getChildren().addAll(capacity, car);
        timeCapacity.getChildren().addAll(departBox, arrow, arriveeBox, spacer2, capacityBox);
        leftSection.getChildren().addAll(routeInfo, timeCapacity);

        // ── Calcul prix ──
        VBox rightSection = new VBox(10);
        rightSection.setAlignment(Pos.CENTER);
        rightSection.getStyleClass().add("trip-pricing");

        double prixTotal       = trajet.getIdVoiture() != null ? trajet.getIdVoiture().getPrixKm() * trajet.getDistanceKm() : 0;
        double prixParPersonne = trajet.getNbPersonnes() > 0 ? prixTotal / trajet.getNbPersonnes() : 0;

        Label priceLabel  = new Label(String.format("%.2f DT", prixParPersonne));
        priceLabel.getStyleClass().add("trip-price-large");
        Label priceDetail = new Label("par personne");
        priceDetail.getStyleClass().add("trip-price-detail");
        Label totalPrice = new Label(String.format("(total: %.2f DT)", prixTotal));
        totalPrice.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");

        HBox crudButtons = new HBox(10); crudButtons.setAlignment(Pos.CENTER);

        // Les boutons Edit/Delete ne sont visibles que pour le propriétaire du trajet
        boolean isOwner = currentUser != null && trajet.getIdUser() != null
                && currentUser.getId() == trajet.getIdUser().getId();

        if (isOwner) {
            Button editBtn = new Button("✏️");
            editBtn.setStyle("-fx-background-color:#f59e0b;-fx-text-fill:white;-fx-font-size:16px;-fx-padding:8 12;-fx-background-radius:8;-fx-cursor:hand;");
            editBtn.setOnAction(e -> {
                trajetSelectionne = trajet;
                if (cbDepart      != null) cbDepart.setValue(trajet.getPointDepart());
                if (cbArrivee     != null) cbArrivee.setValue(trajet.getPointArrivee());
                if (dpDate        != null) dpDate.setValue(trajet.getDateReservation().toLocalDate());
                if (spNbPersonnes != null) spNbPersonnes.getValueFactory().setValue(trajet.getNbPersonnes());
                if (tfDistance    != null) {
                    distanceApiEnCours = true;
                    tfDistance.setText(String.format("%.1f", trajet.getDistanceKm()));
                    tfDistance.setStyle(""); tfDistance.setPromptText("km");
                    distanceApiEnCours = false;
                }
                showAlert("Modification", "Trajet sélectionné. Modifiez les informations dans le formulaire.");
            });

            Button deleteBtn = new Button("🗑️");
            deleteBtn.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;-fx-font-size:16px;-fx-padding:8 12;-fx-background-radius:8;-fx-cursor:hand;");
            deleteBtn.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirmation");
                confirm.setHeaderText("Supprimer ce trajet ?");
                confirm.setContentText(trajet.getPointDepart() + " → " + trajet.getPointArrivee());
                confirm.showAndWait().ifPresent(r -> {
                    if (r == ButtonType.OK) {
                        try {
                            trajetCRUD.supprimerh(trajet);
                            loadTrajets();
                            showAlert("Succès", "Trajet supprimé !");
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "Erreur suppression", ex);
                            showAlert("Erreur", "Impossible de supprimer : " + ex.getMessage());
                        }
                    }
                });
            });
            crudButtons.getChildren().addAll(editBtn, deleteBtn);
        }

        rightSection.getChildren().addAll(priceLabel, priceDetail, totalPrice);
        if (!crudButtons.getChildren().isEmpty()) rightSection.getChildren().add(crudButtons);

        card.getChildren().addAll(leftSection, rightSection);
        return card;
    }

    private Label labelStyled(String text, String styleClass) {
        Label l = new Label(text); l.getStyleClass().add(styleClass); return l;
    }

    // ==================== SÉLECTION VOITURE ====================

    private void selectionnerVoiture(Voiture voiture) {
        voitureSelectionnee = voiture;
        showAlert("Voiture sélectionnée",
                "Voiture : " + voiture.getMarque() + " " + voiture.getModele() +
                        "\nPlaces : " + voiture.getNb_places() +
                        "\n\nAllez dans la section Trajets pour créer votre trajet.");
        showTrajetsSection();
    }

    // ══════════════════════════════════════════════════════════════════
    //  CRUD TRAJET — AJOUTER  +  ENVOI EMAIL TWILIO SENDGRID
    // ══════════════════════════════════════════════════════════════════

    @FXML
    private void ajouterTrajetAction() {
        if (!checkUserAuth()) return;

        if (voitureSelectionnee == null) {
            showAlert("Erreur", "Sélectionnez d'abord une voiture !"); return;
        }
        if (!validerFormulaireTrajet()) return;

        try {
            String    depart     = cbDepart.getValue();
            String    arrivee    = cbArrivee.getValue();
            LocalDate date       = dpDate.getValue();
            Integer   nbP        = spNbPersonnes.getValue();
            float     distanceKm = Float.parseFloat(tfDistance.getText());

            Trajet trajet = new Trajet();
            trajet.setPointDepart(depart);
            trajet.setPointArrivee(arrivee);
            trajet.setDistanceKm(distanceKm);
            trajet.setDateReservation(java.sql.Date.valueOf(date));
            trajet.setNbPersonnes(nbP);
            trajet.setIdUser(currentUser);          // ✅ utilisateur connecté via JWT
            trajet.setIdVoiture(voitureSelectionnee);
            trajet.setStatut(StatutVoiture.Reserve);

            trajetCRUD.ajouterh(trajet);
            loadTrajets();
            clearTrajetForm();

            double prixParPersonne = voitureSelectionnee.getPrixKm() * distanceKm / nbP;
            double prixTotal       = voitureSelectionnee.getPrixKm() * distanceKm;

            // ✅ Email envoyé à l'adresse e-mail de l'utilisateur connecté
            sendTwilioEmail(
                    trajet,
                    voitureSelectionnee,
                    prixParPersonne,
                    prixTotal,
                    currentUser.getE_mail(),   // ← email issu du token JWT / session
                    currentUser.getPrenom()    // ← prénom pour la salutation
            );

            showAlert("Succès",
                    "Trajet créé !\n\n" +
                            "Voiture  : " + voitureSelectionnee.getMarque() + " " + voitureSelectionnee.getModele() + "\n" +
                            "Trajet   : " + depart + " → " + arrivee + "\n" +
                            "Distance : " + String.format("%.1f km", distanceKm) + "\n" +
                            "Prix/pers: " + String.format("%.2f DT", prixParPersonne) + "\n\n" +
                            "📧 Confirmation envoyée à : " + currentUser.getE_mail());

        } catch (NumberFormatException e) {
            showAlert("Erreur", "Distance invalide !");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur ajout trajet", e);
            showAlert("Erreur", "Impossible d'ajouter le trajet : " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur", e);
            showAlert("Erreur", "Vérifiez les champs du formulaire !");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  ✅ ENVOI EMAIL TWILIO SENDGRID
    //
    //  toEmail      = currentUser.getE_mail()  → adresse de l'utilisateur
    //                                            connecté (via UserSession/JWT)
    //  prenomClient = currentUser.getPrenom()  → prénom pour la salutation
    // ══════════════════════════════════════════════════════════════════
    private void sendTwilioEmail(Trajet trajet, Voiture voiture,
                                 double prixParPersonne, double prixTotal,
                                 String toEmail, String prenomClient) {

        String dateStr = trajet.getDateReservation() != null
                ? new SimpleDateFormat("dd/MM/yyyy").format(trajet.getDateReservation())
                : "—";

        String htmlBody =
                "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;"
                        + "border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;'>"

                        // ── En-tête ──
                        + "<div style='background:linear-gradient(135deg,#6366F1,#8B5CF6);"
                        + "padding:30px;text-align:center;'>"
                        + "<h1 style='color:white;margin:0;font-size:28px;'>🚗 RE7LA</h1>"
                        + "<p style='color:rgba(255,255,255,0.85);margin:6px 0 0;font-size:15px;'>"
                        + "Confirmation de votre trajet</p></div>"

                        // ── Corps ──
                        + "<div style='padding:30px;background:#f8fafc;'>"
                        + "<p style='font-size:16px;color:#334155;'>Bonjour <strong>"
                        + prenomClient + "</strong>,</p>"
                        + "<p style='font-size:15px;color:#475569;'>"
                        + "Votre trajet a été créé avec succès. Voici le récapitulatif :</p>"

                        // ── Tableau trajet ──
                        + "<div style='background:white;border-radius:10px;padding:20px;margin:20px 0;"
                        + "box-shadow:0 2px 8px rgba(0,0,0,0.08);'>"
                        + "<h2 style='color:#1e293b;font-size:18px;margin:0 0 16px;"
                        + "border-bottom:2px solid #6366F1;padding-bottom:8px;'>📍 Détails du trajet</h2>"
                        + "<table style='width:100%;border-collapse:collapse;font-size:14px;'>"
                        + "<tr style='background:#f1f5f9;'>"
                        + "<td style='padding:10px;color:#64748b;font-weight:bold;'>Départ</td>"
                        + "<td style='padding:10px;color:#1e293b;'>" + trajet.getPointDepart() + "</td></tr>"
                        + "<tr><td style='padding:10px;color:#64748b;font-weight:bold;'>Arrivée</td>"
                        + "<td style='padding:10px;color:#1e293b;'>" + trajet.getPointArrivee() + "</td></tr>"
                        + "<tr style='background:#f1f5f9;'>"
                        + "<td style='padding:10px;color:#64748b;font-weight:bold;'>Date</td>"
                        + "<td style='padding:10px;color:#1e293b;'>" + dateStr + "</td></tr>"
                        + "<tr><td style='padding:10px;color:#64748b;font-weight:bold;'>Distance</td>"
                        + "<td style='padding:10px;color:#1e293b;'>"
                        + String.format("%.1f km", trajet.getDistanceKm()) + "</td></tr>"
                        + "<tr style='background:#f1f5f9;'>"
                        + "<td style='padding:10px;color:#64748b;font-weight:bold;'>Passagers</td>"
                        + "<td style='padding:10px;color:#1e293b;'>"
                        + trajet.getNbPersonnes() + " personne(s)</td></tr>"
                        + "</table></div>"

                        // ── Tableau voiture ──
                        + "<div style='background:white;border-radius:10px;padding:20px;margin:20px 0;"
                        + "box-shadow:0 2px 8px rgba(0,0,0,0.08);'>"
                        + "<h2 style='color:#1e293b;font-size:18px;margin:0 0 16px;"
                        + "border-bottom:2px solid #6366F1;padding-bottom:8px;'>🚙 Voiture</h2>"
                        + "<table style='width:100%;border-collapse:collapse;font-size:14px;'>"
                        + "<tr style='background:#f1f5f9;'>"
                        + "<td style='padding:10px;color:#64748b;font-weight:bold;'>Marque / Modèle</td>"
                        + "<td style='padding:10px;color:#1e293b;'>"
                        + voiture.getMarque() + " " + voiture.getModele() + "</td></tr>"
                        + "<tr><td style='padding:10px;color:#64748b;font-weight:bold;'>Places</td>"
                        + "<td style='padding:10px;color:#1e293b;'>" + voiture.getNb_places() + "</td></tr>"
                        + "<tr style='background:#f1f5f9;'>"
                        + "<td style='padding:10px;color:#64748b;font-weight:bold;'>Avec chauffeur</td>"
                        + "<td style='padding:10px;color:#1e293b;'>"
                        + (voiture.isAvecChauffeur() ? "✅ Oui" : "❌ Non") + "</td></tr>"
                        + "<tr><td style='padding:10px;color:#64748b;font-weight:bold;'>Prix / km</td>"
                        + "<td style='padding:10px;color:#1e293b;'>"
                        + String.format("%.2f DT", voiture.getPrixKm()) + "</td></tr>"
                        + "</table></div>"

                        // ── Récapitulatif prix ──
                        + "<div style='background:linear-gradient(135deg,#6366F1,#8B5CF6);"
                        + "border-radius:10px;padding:20px;text-align:center;'>"
                        + "<p style='color:rgba(255,255,255,0.8);margin:0;font-size:13px;'>PRIX TOTAL</p>"
                        + "<p style='color:white;font-size:32px;font-weight:bold;margin:4px 0;'>"
                        + String.format("%.2f DT", prixTotal) + "</p>"
                        + "<p style='color:rgba(255,255,255,0.8);margin:0;font-size:13px;'>soit "
                        + String.format("%.2f DT", prixParPersonne) + " / personne</p></div>"

                        + "<p style='font-size:14px;color:#94a3b8;text-align:center;margin-top:24px;'>"
                        + "Merci de voyager avec RE7LA 🇹🇳</p></div>"

                        // ── Pied de page ──
                        + "<div style='background:#1e293b;padding:16px;text-align:center;'>"
                        + "<p style='color:#64748b;font-size:12px;margin:0;'>"
                        + "© 2025 RE7LA — Plateforme de covoiturage tunisienne</p></div></div>";

        // Thread daemon pour ne pas bloquer le thread JavaFX
        Thread emailThread = new Thread(() -> {
            try {
                Email   from    = new Email(FROM_EMAIL);
                Email   to      = new Email(toEmail);     // ← email de l'utilisateur connecté
                Content html    = new Content("text/html", htmlBody);
                Mail    mail    = new Mail(from, "✅ Confirmation de votre trajet RE7LA", to, html);

                SendGrid sg      = new SendGrid(SENDGRID_API_KEY);
                Request  request = new Request();
                request.setMethod(Method.POST);
                request.setEndpoint("mail/send");
                request.setBody(mail.build());

                Response response = sg.api(request);

                if (response.getStatusCode() == 202) {
                    LOGGER.info("✅ Email de confirmation envoyé à " + toEmail);
                } else {
                    LOGGER.warning("⚠️ SendGrid statut : " + response.getStatusCode()
                            + " — " + response.getBody());
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Erreur envoi email Twilio SendGrid", ex);
            }
        });
        emailThread.setDaemon(true);
        emailThread.start();
    }

    // ==================== MODIFIER / SUPPRIMER TRAJET ====================

    @FXML
    private void modifierTrajet() {
        if (!checkUserAuth()) return;
        if (trajetSelectionne == null) {
            showAlert("Erreur", "Sélectionnez un trajet à modifier."); return;
        }
        if (!validerFormulaireTrajet()) return;
        try {
            trajetSelectionne.setPointDepart(cbDepart.getValue());
            trajetSelectionne.setPointArrivee(cbArrivee.getValue());
            trajetSelectionne.setDateReservation(java.sql.Date.valueOf(dpDate.getValue()));
            trajetSelectionne.setNbPersonnes(spNbPersonnes.getValue());
            trajetSelectionne.setDistanceKm(Float.parseFloat(tfDistance.getText()));
            trajetCRUD.modifierh(trajetSelectionne);
            loadTrajets(); clearTrajetForm(); trajetSelectionne = null;
            showAlert("Succès", "Trajet modifié !");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur modification", e);
            showAlert("Erreur", "Impossible de modifier : " + e.getMessage());
        }
    }

    @FXML
    private void supprimerTrajet() {
        if (!checkUserAuth()) return;
        if (trajetSelectionne == null) {
            showAlert("Erreur", "Sélectionnez un trajet à supprimer."); return;
        }
        try {
            trajetCRUD.supprimerh(trajetSelectionne);
            loadTrajets(); clearTrajetForm(); trajetSelectionne = null;
            showAlert("Succès", "Trajet supprimé !");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur suppression", e);
            showAlert("Erreur", "Impossible de supprimer : " + e.getMessage());
        }
    }

    private void clearTrajetForm() {
        if (cbDepart      != null) cbDepart.setValue(null);
        if (cbArrivee     != null) cbArrivee.setValue(null);
        if (dpDate        != null) dpDate.setValue(null);
        if (spNbPersonnes != null) spNbPersonnes.getValueFactory().setValue(1);
        if (tfDistance    != null) {
            distanceApiEnCours = true;
            tfDistance.clear(); tfDistance.setStyle(""); tfDistance.setPromptText("km");
            distanceApiEnCours = false;
        }
    }

    // ==================== DISTANCE (Nominatim + OSRM) ====================

    private void updateDistanceAutomatically() {
        if (cbDepart == null || cbArrivee == null || tfDistance == null) return;
        String dep = cbDepart.getValue();
        String arr = cbArrivee.getValue();
        if (dep == null || arr == null || dep.isEmpty() || arr.isEmpty()) return;

        distanceApiEnCours = true;
        tfDistance.clear();
        tfDistance.setPromptText("Calcul en cours…");
        tfDistance.setStyle("-fx-prompt-text-fill: #f59e0b;");
        distanceApiEnCours = false;

        javafx.concurrent.Task<Float> task = new javafx.concurrent.Task<>() {
            @Override protected Float call() { return fetchDistanceFromOSRM(dep, arr); }
        };
        task.setOnSucceeded(e -> {
            Float dist = task.getValue();
            distanceApiEnCours = true;
            if (dist != null && dist > 0) {
                tfDistance.setText(String.format("%.1f", dist));
                tfDistance.setStyle(""); tfDistance.setPromptText("km");
                LOGGER.info("Distance OSRM " + dep + "→" + arr + " : " + dist + " km");
            } else {
                tfDistance.clear();
                tfDistance.setPromptText("Saisir manuellement");
                tfDistance.setStyle("-fx-prompt-text-fill: #ef4444;");
                LOGGER.warning("OSRM : pas de distance pour " + dep + "→" + arr);
            }
            distanceApiEnCours = false;
        });
        task.setOnFailed(e -> {
            distanceApiEnCours = true;
            tfDistance.clear();
            tfDistance.setPromptText("Saisir manuellement");
            tfDistance.setStyle("-fx-prompt-text-fill: #ef4444;");
            distanceApiEnCours = false;
            LOGGER.log(Level.WARNING, "Erreur OSRM " + dep + "→" + arr, task.getException());
        });
        Thread t = new Thread(task); t.setDaemon(true); t.start();
    }

    private float fetchDistanceFromOSRM(String depart, String arrivee) {
        try {
            double[] dep = geocodeCity(depart  + ", Tunisie");
            double[] arr = geocodeCity(arrivee + ", Tunisie");
            if (dep == null || arr == null) {
                LOGGER.warning("Géocodage échoué : " + depart + " / " + arrivee);
                return -1f;
            }
            String url = String.format(
                    "http://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=false",
                    dep[1], dep[0], arr[1], arr[0]);
            java.net.HttpURLConnection conn =
                    (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("GET"); conn.setConnectTimeout(8000); conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "RE7LA-App/1.0");
            if (conn.getResponseCode() != 200) { LOGGER.warning("OSRM HTTP " + conn.getResponseCode()); return -1f; }
            String json = new String(conn.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            conn.disconnect();
            int idx = json.indexOf("\"distance\":"); if (idx == -1) return -1f;
            int start = idx + 11;
            int end   = json.indexOf(',', start); if (end == -1) end = json.indexOf('}', start);
            return Float.parseFloat(json.substring(start, end).trim()) / 1000f;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "fetchDistanceFromOSRM error", e);
            return -1f;
        }
    }

    private double[] geocodeCity(String cityName) {
        try {
            String encoded = java.net.URLEncoder.encode(cityName, "UTF-8");
            String url = "https://nominatim.openstreetmap.org/search?q=" + encoded
                    + "&format=json&limit=1&countrycodes=tn";
            java.net.HttpURLConnection conn =
                    (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("GET"); conn.setConnectTimeout(6000); conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "RE7LA-App/1.0");
            if (conn.getResponseCode() != 200) return null;
            String json = new String(conn.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            conn.disconnect();
            if (json.equals("[]") || !json.contains("\"lat\"")) return null;
            int latIdx = json.indexOf("\"lat\":\"") + 7;
            int latEnd = json.indexOf('"', latIdx);
            int lonIdx = json.indexOf("\"lon\":\"") + 7;
            int lonEnd = json.indexOf('"', lonIdx);
            return new double[]{
                    Double.parseDouble(json.substring(latIdx, latEnd)),
                    Double.parseDouble(json.substring(lonIdx, lonEnd))
            };
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Nominatim error for: " + cityName, e);
            return null;
        }
    }

    // ==================== PAGINATION VOITURES ====================

    @FXML private void nextPageVoitures() {
        int max = (int) Math.ceil((double) voituresListFiltered.size() / ITEMS_PER_PAGE_VOITURES) - 1;
        if (currentPageVoitures < max) {
            currentPageVoitures++;
            if (pageVoituresLabel != null) pageVoituresLabel.setText(String.valueOf(currentPageVoitures + 1));
            displayVoituresGrid();
        }
    }

    @FXML private void previousPageVoitures() {
        if (currentPageVoitures > 0) {
            currentPageVoitures--;
            if (pageVoituresLabel != null) pageVoituresLabel.setText(String.valueOf(currentPageVoitures + 1));
            displayVoituresGrid();
        }
    }

    // ==================== PAGINATION TRAJETS ====================

    @FXML private void nextPageTrajets() {
        int max = (int) Math.ceil((double) trajetsListFiltered.size() / ITEMS_PER_PAGE_TRAJETS) - 1;
        if (currentPageTrajets < max) {
            currentPageTrajets++;
            if (pageTrajetsLabel != null) pageTrajetsLabel.setText(String.valueOf(currentPageTrajets + 1));
            displayTrajetsList();
        }
    }

    @FXML private void previousPageTrajets() {
        if (currentPageTrajets > 0) {
            currentPageTrajets--;
            if (pageTrajetsLabel != null) pageTrajetsLabel.setText(String.valueOf(currentPageTrajets + 1));
            displayTrajetsList();
        }
    }

    // ==================== UTILITAIRES ====================

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null);
        alert.setContentText(message); alert.showAndWait();
    }

    private boolean validerFormulaireTrajet() {
        String    depart       = cbDepart      != null ? cbDepart.getValue()      : null;
        String    arrivee      = cbArrivee     != null ? cbArrivee.getValue()     : null;
        LocalDate date         = dpDate        != null ? dpDate.getValue()        : null;
        String    distanceText = tfDistance    != null ? tfDistance.getText()     : null;
        Integer   nbPersonnes  = spNbPersonnes != null ? spNbPersonnes.getValue() : null;

        if (depart == null || arrivee == null || date == null) {
            showAlert("Erreur", "Veuillez remplir tous les champs !"); return false;
        }
        if (distanceText == null || distanceText.isBlank()) {
            showAlert("Erreur",
                    "La distance est en cours de calcul — veuillez patienter quelques secondes.\n"
                            + "Si la connexion est absente, saisissez la distance manuellement."); return false;
        }
        if (depart.equalsIgnoreCase(arrivee)) {
            showAlert("Erreur", "Le départ et l'arrivée doivent être différents !"); return false;
        }
        if (date.isBefore(LocalDate.now())) {
            showAlert("Erreur", "La date doit être aujourd'hui ou future !"); return false;
        }
        try {
            float dist = Float.parseFloat(distanceText);
            if (dist <= 0) { showAlert("Erreur", "La distance doit être > 0 !"); return false; }
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Distance invalide !"); return false;
        }
        if (voitureSelectionnee != null && nbPersonnes != null
                && nbPersonnes > voitureSelectionnee.getNb_places()) {
            showAlert("Erreur", "Nombre de personnes (" + nbPersonnes
                    + ") dépasse la capacité de la voiture ("
                    + voitureSelectionnee.getNb_places() + " places) !"); return false;
        }
        return true;
    }

    // ==================== NAVIGATION BACK-OFFICE ====================

    @FXML
    private void openGestionVoiture2() {
        if (!checkUserAuth()) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Trajet/gestionvoitureettrajet.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnGestionVoiture2.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestion des Voitures"); stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur navigation", e);
            showAlert("Erreur", "Impossible d'ouvrir la gestion: " + e.getMessage());
        }
    }

    @FXML
    private void openAssistantVocal() {
        if (!checkUserAuth()) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Trajet/assistantVocal.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 900, 650));
            stage.setTitle("🤖 Assistant Vocal RE7LA");
            stage.setResizable(true); stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur ouverture assistant", e);
            showAlert("Erreur", "Impossible d'ouvrir l'assistant: " + e.getMessage());
        }
    }

    @FXML
    private void ouvrirCarte() {
        if (!checkUserAuth()) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Trajet/mapView.fxml"));
            Parent root = loader.load();
            MapController mapCtrl = loader.getController();
            String dep = cbDepart  != null ? cbDepart.getValue()  : null;
            String arr = cbArrivee != null ? cbArrivee.getValue() : null;
            mapCtrl.preRemplir(dep, arr);
            mapCtrl.setOnItineraireChoisi(() -> {
                double dist = mapCtrl.getDistanceRetour();
                if (tfDistance != null && dist > 0) {
                    distanceApiEnCours = true;
                    tfDistance.setText(String.format("%.1f", dist));
                    tfDistance.setStyle(""); tfDistance.setPromptText("km");
                    distanceApiEnCours = false;
                }
                String vDep = mapCtrl.getVilleDepart();
                if (cbDepart  != null && vDep != null && !vDep.isBlank()) cbDepart.setValue(vDep);
                String vArr = mapCtrl.getVilleArrivee();
                if (cbArrivee != null && vArr != null && !vArr.isBlank()) cbArrivee.setValue(vArr);
            });
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("🗺️ Carte RE7LA — OpenStreetMap");
            stage.setMinWidth(800); stage.setMinHeight(600); stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur ouverture carte", e);
            showAlert("Erreur", "Impossible d'ouvrir la carte : " + e.getMessage());
        }
    }

    // ==================== FAB ASSISTANT ====================

    @FXML
    private void fabMouseEntered() {
        if (fabTooltip != null) fabTooltip.setVisible(true);
        if (btnAssistantFlottant != null) btnAssistantFlottant.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #4f46e5, #7c3aed);"
                        + "-fx-text-fill: white; -fx-font-size: 22px; -fx-background-radius: 50%; -fx-cursor: hand;"
                        + "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.9), 22, 0, 0, 6);"
                        + "-fx-border-color: rgba(255,255,255,0.35); -fx-border-radius: 50%; -fx-border-width: 2;"
                        + "-fx-scale-x: 1.12; -fx-scale-y: 1.12;");
    }

    @FXML
    private void fabMouseExited() {
        if (fabTooltip != null) fabTooltip.setVisible(false);
        if (btnAssistantFlottant != null) btnAssistantFlottant.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #6366F1, #8B5CF6);"
                        + "-fx-text-fill: white; -fx-font-size: 22px; -fx-background-radius: 50%; -fx-cursor: hand;"
                        + "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.7), 16, 0, 0, 4);"
                        + "-fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 50%; -fx-border-width: 2;");
    }
}

package org.example.Controllers.hebergement.front;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.Entites.hebergement.Hebergement;
import org.example.Entites.hebergement.Notification;
import org.example.Entites.hebergement.Reservation;
import org.example.Entites.user.User;
import org.example.Services.hebergement.HebergementCRUD;
import org.example.Services.hebergement.NotificationCRUD;
import org.example.Services.hebergement.ReservationCRUD;
import org.example.Utils.LanguageManager;
import org.example.Utils.UserSession;
import org.example.Services.user.APIservices.JWTService;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ReservationController implements Initializable {

    @FXML private ImageView backgroundImage;

    // ================= HEADER =================
    @FXML private Label logoTitleLabel;
    @FXML private Label logoSubtitleLabel;
    @FXML private Label navHomeLabel;
    @FXML private Label navMyBookingsLabel;
    @FXML private Label badgeNotifLabel;
    @FXML private HBox  bandeauNotification;
    @FXML private Label bandeauNotifTexte;
    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private Button btnAccueil;
    @FXML private Button btnProfil;
    @FXML private Button btnLanguage;
    @FXML private TextField searchHebergementField;

    // ================= SIDEBAR FILTRES =================
    @FXML private Label labelFilterType;
    @FXML private VBox  typeCheckBoxContainer;
    @FXML private Label labelFilterDispo;
    @FXML private CheckBox checkDisponible;
    @FXML private Label labelFilterPrix;
    @FXML private TextField fieldPrixMax;
    @FXML private Button btnResetFilters;
    @FXML private ComboBox<String> sortComboHeb;

    // ================= SECTION HÉBERGEMENTS =================
    @FXML private TilePane tileHebergements;
    @FXML private Label sectionAvailableLabel;

    // ================= SECTION BIENVENUE =================
    @FXML private Label welcomeTitleLabel;
    @FXML private Label welcomeSubtitleLabel;

    // ================= FORMULAIRE - labels =================
    @FXML private Label formTitleLabel;
    @FXML private Label userInfoSectionLabel;
    @FXML private Label labelNom;
    @FXML private Label labelPrenom;
    @FXML private Label labelEmail;
    @FXML private Label labelTel;
    @FXML private Label labelDateDebut;
    @FXML private Label labelDateFin;
    @FXML private Label labelPrixTotal;
    @FXML private Label labelStatut;

    // ================= FORMULAIRE - infos user =================
    @FXML private Label userNomLabel;
    @FXML private Label userPrenomLabel;
    @FXML private Label userEmailInfoLabel;
    @FXML private Label userTelLabel;

    // ================= FORMULAIRE - hébergement sélectionné =================
    @FXML private ImageView hebergementImage;
    @FXML private Label selectedHebergementTitre;
    @FXML private Label selectedHebergementPrix;

    // ================= FORMULAIRE - champs =================
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private TextField txtPrixTotal;
    @FXML private ComboBox<String> comboStatut;
    @FXML private Button btnReserver;
    @FXML private Button btnAnnuler;

    // ================= BOUTONS ACTION =================
    @FXML private Button btnNearbyPlaces;
    @FXML private Button btnWeather;

    // ================= ERREURS =================
    @FXML private Label lblDateDebutErreur;
    @FXML private Label lblDateFinErreur;

    // ================= DONNÉES =================
    private ObservableList<Hebergement> allHebergements      = FXCollections.observableArrayList();
    private ObservableList<Hebergement> filteredHebergements = FXCollections.observableArrayList();
    private final Map<String, CheckBox> typeCheckBoxMap      = new LinkedHashMap<>();

    private Hebergement selectedHebergement = null;
    private User currentUser;
    private String userToken;

    private final HebergementCRUD  hebergementCRUD  = new HebergementCRUD();
    private final ReservationCRUD  reservationCRUD  = new ReservationCRUD();
    private final NotificationCRUD notificationCRUD = new NotificationCRUD();
    private final LanguageManager  lang             = LanguageManager.getInstance();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // =========================================================
    // INITIALISATION
    // =========================================================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ✅ 1. VÉRIFICATION JWT - TOKEN VALIDE ?
        if (!checkUserAuth()) {
            return;
        }

        setupStatutCombo();
        setupSortCombo();
        loadHebergements();
        setupListeners();
        displayUserInfo();
        applyLanguage();
        clearForm();
        afficherNotifications();
    }

    /**
     * ✅ Vérifie si l'utilisateur est authentifié
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
        userToken = UserSession.getInstance().getToken();

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
            Stage stage = (Stage) (searchHebergementField != null ?
                    searchHebergementField.getScene().getWindow() :
                    btnAccueil.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // GESTION DES NOTIFICATIONS
    // =========================================================

    private void afficherNotifications() {
        if (currentUser == null) return;

        // ✅ Vérification JWT avant d'accéder aux notifications
        if (!checkUserAuth()) return;

        try {
            int nonLues = notificationCRUD.compterNonLues(currentUser.getId());

            if (nonLues > 0) {
                if (badgeNotifLabel != null) {
                    badgeNotifLabel.setText(String.valueOf(nonLues));
                    badgeNotifLabel.setVisible(true);
                }
                if (navMyBookingsLabel != null) {
                    navMyBookingsLabel.setText("🔔 Mes réservations (" + nonLues + ")");
                    navMyBookingsLabel.setStyle(
                            "-fx-background-color: #E74C3C; -fx-text-fill: white;" +
                                    "-fx-font-weight: bold; -fx-padding: 10 20;" +
                                    "-fx-background-radius: 15; -fx-cursor: hand;" +
                                    "-fx-effect: dropshadow(gaussian, rgba(231,76,60,0.4), 8, 0, 0, 2);"
                    );
                }
                List<Notification> notifs = notificationCRUD.getByUser(currentUser.getId());
                for (Notification n : notifs) {
                    if (!n.isLue()) { afficherBandeau(n.getMessage()); break; }
                }
                notificationCRUD.marquerToutesLues(currentUser.getId());
            } else {
                if (badgeNotifLabel != null) badgeNotifLabel.setVisible(false);
                if (navMyBookingsLabel != null) {
                    navMyBookingsLabel.setText("📋 Mes réservations");
                    navMyBookingsLabel.setStyle(
                            "-fx-background-color: #F39C12; -fx-text-fill: white;" +
                                    "-fx-font-weight: bold; -fx-padding: 10 20;" +
                                    "-fx-background-radius: 15; -fx-cursor: hand;" +
                                    "-fx-effect: dropshadow(gaussian, rgba(243,156,18,0.4), 8, 0, 0, 2);"
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void afficherBandeau(String message) {
        if (bandeauNotification == null || bandeauNotifTexte == null) return;
        bandeauNotifTexte.setText(message);
        bandeauNotification.setVisible(true);
        bandeauNotification.setManaged(true);
    }

    @FXML
    private void fermerBandeau() {
        if (bandeauNotification != null) {
            bandeauNotification.setVisible(false);
            bandeauNotification.setManaged(false);
        }
        if (badgeNotifLabel != null) badgeNotifLabel.setVisible(false);
        if (navMyBookingsLabel != null) {
            navMyBookingsLabel.setText("📋 Mes réservations");
            navMyBookingsLabel.setStyle(
                    "-fx-background-color: #F39C12; -fx-text-fill: white;" +
                            "-fx-font-weight: bold; -fx-padding: 10 20;" +
                            "-fx-background-radius: 15; -fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgba(243,156,18,0.4), 8, 0, 0, 2);"
            );
        }
    }

    // =========================================================
    // ✅ NAVIGATION → MES RÉSERVATIONS
    // =========================================================

    @FXML
    private void handleMesReservations() {
        // ✅ Vérification JWT avant navigation
        if (!checkUserAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/hebergement/front/MesReservations.fxml"));
            if (loader.getLocation() == null)
                throw new Exception("MesReservations.fxml introuvable");
            Parent root  = loader.load();
            Stage  stage = (Stage) navMyBookingsLabel.getScene().getWindow();
            Scene  scene = new Scene(root);
            try {
                scene.getStylesheets().add(
                        getClass().getResource("/hebergement/front/StyleMesRes.css")
                                .toExternalForm());
            } catch (Exception ignored) {}
            stage.setScene(scene);
            stage.setTitle("📋 Mes Réservations");
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir Mes Réservations : " + e.getMessage());
        }
    }

    // =========================================================
    // MÉTÉO AVANCÉE
    // =========================================================

    @FXML
    private void openWeather() {
        // ✅ Vérification JWT avant d'ouvrir la météo
        if (!checkUserAuth()) return;

        if (selectedHebergement == null) {
            showAlert("Aucun hébergement sélectionné",
                    "Veuillez d'abord sélectionner un hébergement dans la liste.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/hebergement/front/WeatherDialog.fxml"));
            Parent root = loader.load();

            WeatherController controller = loader.getController();
            controller.setHebergementInfo(
                    selectedHebergement.getTitre(),
                    selectedHebergement.getType_hebergement()
            );

            Stage dialogStage = new Stage();
            dialogStage.setTitle("🌤 Météo — " + selectedHebergement.getTitre());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(btnWeather != null
                    ? btnWeather.getScene().getWindow() : null);
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.setResizable(true);
            dialogStage.setMinWidth(820);
            dialogStage.setMinHeight(650);
            dialogStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la météo : " + e.getMessage());
        }
    }

    // =========================================================
    // LIEUX PROCHES
    // =========================================================

    @FXML
    private void openNearbyPlaces() {
        // ✅ Vérification JWT avant d'ouvrir les lieux proches
        if (!checkUserAuth()) return;

        if (selectedHebergement == null) {
            showAlert("Aucun hébergement sélectionné",
                    "Veuillez d'abord sélectionner un hébergement dans la liste.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/hebergement/front/NearbyPlacesDialog.fxml"));
            Parent root = loader.load();

            NearbyPlacesController controller = loader.getController();
            controller.setCity(selectedHebergement.getTitre());

            Stage dialogStage = new Stage();
            dialogStage.setTitle("📍 Lieux proches — " + selectedHebergement.getTitre());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(btnNearbyPlaces != null
                    ? btnNearbyPlaces.getScene().getWindow() : null);
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.setResizable(true);
            dialogStage.setMinWidth(700);
            dialogStage.setMinHeight(550);
            dialogStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir les lieux proches : " + e.getMessage());
        }
    }

    // =========================================================
    // LANGUE
    // =========================================================

    @FXML
    private void toggleLanguage() {
        lang.toggleLocale();
        applyLanguage();
        applyFilters();
    }

    private void applyLanguage() {
        safeSetText(btnLanguage,       lang.getToggleButtonLabel());
        safeSetText(logoTitleLabel,    lang.get("app.name"));
        safeSetText(logoSubtitleLabel, lang.get("app.subtitle"));
        safeSetText(navHomeLabel,      lang.get("nav.home"));
        safeSetText(btnAccueil,        lang.get("btn.home"));
        safeSetText(btnProfil,         lang.get("btn.profile"));

        if (searchHebergementField != null)
            searchHebergementField.setPromptText(lang.get("search.placeholder"));

        safeSetText(labelFilterType,  lang.get("filter.type.label"));
        safeSetText(labelFilterDispo, lang.get("filter.disponibilite.label"));
        safeSetText(checkDisponible,  lang.get("filter.disponibilite.check"));
        safeSetText(labelFilterPrix,  lang.get("filter.prix.label"));
        if (fieldPrixMax != null) fieldPrixMax.setPromptText(lang.get("filter.prix.placeholder"));
        safeSetText(btnResetFilters,  lang.get("filter.reset"));

        setupSortCombo();
        updateResultsCount();

        safeSetText(welcomeTitleLabel,    lang.get("welcome.title"));
        safeSetText(welcomeSubtitleLabel, lang.get("welcome.subtitle"));
        safeSetText(formTitleLabel,       lang.get("form.title"));
        safeSetText(userInfoSectionLabel, lang.get("form.user.info"));
        safeSetText(labelNom,             lang.get("form.label.nom"));
        safeSetText(labelPrenom,          lang.get("form.label.prenom"));
        safeSetText(labelEmail,           lang.get("form.label.email"));
        safeSetText(labelTel,             lang.get("form.label.tel"));
        safeSetText(labelDateDebut,       lang.get("form.label.date.debut"));
        safeSetText(labelDateFin,         lang.get("form.label.date.fin"));
        safeSetText(labelPrixTotal,       lang.get("form.label.prix.total"));
        safeSetText(labelStatut,          lang.get("form.label.statut"));
        safeSetText(btnReserver,          lang.get("form.btn.reserve"));
        safeSetText(btnAnnuler,           lang.get("form.btn.cancel"));

        setupStatutCombo();

        if (selectedHebergement == null)
            safeSetText(selectedHebergementTitre, lang.get("form.no.selection"));
    }

    // =========================================================
    // UTILITAIRES
    // =========================================================

    private void safeSetText(Label    l, String t) { if (l != null) l.setText(t); }
    private void safeSetText(Button   b, String t) { if (b != null) b.setText(t); }
    private void safeSetText(CheckBox c, String t) { if (c != null) c.setText(t); }

    // =========================================================
    // SETUP COMBOS
    // =========================================================

    private void setupStatutCombo() {
        if (comboStatut == null) return;
        String current = comboStatut.getValue();
        comboStatut.setItems(FXCollections.observableArrayList(
                lang.get("status.pending"), lang.get("status.confirmed"),
                lang.get("status.cancelled"), lang.get("status.finished")
        ));
        if (current != null && comboStatut.getItems().contains(current))
            comboStatut.setValue(current);
        else
            comboStatut.setValue(lang.get("status.pending"));
        comboStatut.setDisable(true);
    }

    private void setupSortCombo() {
        if (sortComboHeb == null) return;
        int idx = sortComboHeb.getSelectionModel().getSelectedIndex();
        sortComboHeb.setItems(FXCollections.observableArrayList(
                lang.get("sort.nom"), lang.get("sort.prix.asc"),
                lang.get("sort.prix.desc"), lang.get("sort.capacite")
        ));
        sortComboHeb.getSelectionModel().select(Math.max(idx, 0));
    }

    // =========================================================
    // CHECKBOXES TYPE
    // =========================================================

    private void buildTypeCheckBoxes() {
        if (typeCheckBoxContainer == null) return;
        typeCheckBoxContainer.getChildren().clear();
        typeCheckBoxMap.clear();

        Set<String> types = new LinkedHashSet<>();
        for (Hebergement h : allHebergements)
            if (h.getType_hebergement() != null && !h.getType_hebergement().isBlank())
                types.add(h.getType_hebergement().trim());

        for (String type : types) {
            CheckBox cb = new CheckBox(resolveIcon(type) + " " + type);
            cb.setSelected(true);
            cb.getStyleClass().add("check-box");
            cb.setOnAction(e -> applyFilters());
            typeCheckBoxMap.put(type, cb);
            typeCheckBoxContainer.getChildren().add(cb);
        }
    }

    private String resolveIcon(String type) {
        if (type == null) return "🏘️";
        String t = type.toLowerCase();
        if (t.contains("hôtel") || t.contains("hotel"))      return "🏨";
        if (t.contains("appart") || t.contains("apart"))     return "🏢";
        if (t.contains("villa"))                              return "🏡";
        if (t.contains("riad") || t.contains("maison"))      return "🏠";
        if (t.contains("chalet") || t.contains("montagne"))  return "🏔️";
        if (t.contains("camp")  || t.contains("tente"))      return "🏕️";
        if (t.contains("bungalow"))                          return "🛖";
        if (t.contains("resort"))                            return "🌴";
        if (t.contains("auberge") || t.contains("hostel"))   return "🛏️";
        return "🏘️";
    }

    // =========================================================
    // LISTENERS
    // =========================================================

    private void setupListeners() {
        if (searchHebergementField != null)
            searchHebergementField.textProperty().addListener((obs, o, n) -> applyFilters());
        if (checkDisponible != null)
            checkDisponible.setOnAction(e -> applyFilters());
        if (fieldPrixMax != null)
            fieldPrixMax.textProperty().addListener((obs, o, n) -> applyFilters());
        if (sortComboHeb != null)
            sortComboHeb.setOnAction(e -> applyFilters());
        if (dateDebutPicker != null && dateFinPicker != null) {
            dateDebutPicker.valueProperty().addListener((obs, o, n) -> { updatePrixTotal(); validateDates(); });
            dateFinPicker.valueProperty().addListener((obs, o, n)   -> { updatePrixTotal(); validateDates(); });
        }
    }

    private void displayUserInfo() {
        if (currentUser == null) return;
        safeSetText(userNameLabel,      currentUser.getPrenom() + " " + currentUser.getNom());
        safeSetText(userEmailLabel,     currentUser.getE_mail());
        safeSetText(userNomLabel,       currentUser.getNom());
        safeSetText(userPrenomLabel,    currentUser.getPrenom());
        safeSetText(userEmailInfoLabel, currentUser.getE_mail());
        safeSetText(userTelLabel,
                currentUser.getNum_tel() != null
                        ? currentUser.getNum_tel()
                        : lang.get("form.tel.unknown"));
    }

    // =========================================================
    // FILTRAGE & TRI
    // =========================================================

    private void applyFilters() {
        String searchText = searchHebergementField != null
                ? searchHebergementField.getText().toLowerCase().trim() : "";
        boolean onlyDispo = checkDisponible != null && checkDisponible.isSelected();
        double prixMax = Double.MAX_VALUE;
        if (fieldPrixMax != null && !fieldPrixMax.getText().isBlank()) {
            try { prixMax = Double.parseDouble(fieldPrixMax.getText().trim()); }
            catch (NumberFormatException ignored) {}
        }

        Set<String> typesActifs = new HashSet<>();
        for (Map.Entry<String, CheckBox> e : typeCheckBoxMap.entrySet())
            if (e.getValue().isSelected())
                typesActifs.add(e.getKey().trim().toLowerCase());

        filteredHebergements.clear();
        for (Hebergement h : allHebergements) {
            boolean matchSearch = searchText.isEmpty()
                    || h.getTitre().toLowerCase().contains(searchText)
                    || h.getType_hebergement().toLowerCase().contains(searchText)
                    || h.getDesc_hebergement().toLowerCase().contains(searchText);
            boolean matchType  = typesActifs.isEmpty()
                    || typesActifs.contains(h.getType_hebergement().trim().toLowerCase());
            boolean matchDispo = !onlyDispo || h.isDisponible_heberg();
            boolean matchPrix  = h.getPrixParNuit() <= prixMax;

            if (matchSearch && matchType && matchDispo && matchPrix)
                filteredHebergements.add(h);
        }
        sortHebergements();
        displayHebergements();
        updateResultsCount();
    }

    private void sortHebergements() {
        if (sortComboHeb == null || sortComboHeb.getValue() == null) return;
        switch (sortComboHeb.getSelectionModel().getSelectedIndex()) {
            case 1 -> filteredHebergements.sort((a, b) -> Float.compare(a.getPrixParNuit(), b.getPrixParNuit()));
            case 2 -> filteredHebergements.sort((a, b) -> Float.compare(b.getPrixParNuit(), a.getPrixParNuit()));
            case 3 -> filteredHebergements.sort((a, b) -> Integer.compare(b.getCapacite(), a.getCapacite()));
            default -> filteredHebergements.sort((a, b) -> a.getTitre().compareToIgnoreCase(b.getTitre()));
        }
    }

    private void updateResultsCount() {
        if (sectionAvailableLabel == null) return;
        int count = filteredHebergements.size();
        sectionAvailableLabel.setText(lang.isFrench()
                ? count + " hébergement" + (count > 1 ? "s" : "") + " trouvé" + (count > 1 ? "s" : "")
                : count + " accommodation" + (count > 1 ? "s" : "") + " found");
    }

    // =========================================================
    // RÉINITIALISATION FILTRES
    // =========================================================

    @FXML
    public void resetFilters(ActionEvent actionEvent) {
        if (searchHebergementField != null) searchHebergementField.clear();
        for (CheckBox cb : typeCheckBoxMap.values()) cb.setSelected(true);
        if (checkDisponible != null) checkDisponible.setSelected(true);
        if (fieldPrixMax    != null) fieldPrixMax.clear();
        if (sortComboHeb    != null) sortComboHeb.getSelectionModel().select(0);
        applyFilters();
    }

    // =========================================================
    // CHARGEMENT & AFFICHAGE HÉBERGEMENTS
    // =========================================================

    private void loadHebergements() {
        try {
            allHebergements.setAll(hebergementCRUD.afficherh());
            filteredHebergements.setAll(allHebergements);
            buildTypeCheckBoxes();
            displayHebergements();
            updateResultsCount();
        } catch (SQLException e) {
            showAlert(lang.get("alert.error.title"),
                    MessageFormat.format(lang.get("alert.error.load"), e.getMessage()));
            e.printStackTrace();
        }
    }

    private void displayHebergements() {
        if (tileHebergements == null) return;
        tileHebergements.getChildren().clear();

        for (Hebergement h : filteredHebergements)
            tileHebergements.getChildren().add(createHebergementCard(h));

        if (filteredHebergements.isEmpty()) {
            VBox emptyState = new VBox(10);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPrefWidth(400);
            emptyState.setPrefHeight(200);
            Label emoji = new Label("🏨");
            emoji.setStyle("-fx-font-size: 48px;");
            Label msg = new Label(lang.get("card.no.result"));
            msg.setStyle("-fx-font-size: 16px; -fx-text-fill: #7F8C8D;");
            emptyState.getChildren().addAll(emoji, msg);
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

        // ── Image ──
        StackPane imagePane = new StackPane();
        imagePane.setPrefHeight(160);
        imagePane.setStyle("-fx-background-color: linear-gradient(to bottom right, #1ABC9C, #3498DB);"
                + " -fx-background-radius: 15 15 0 0;");

        ImageView imageView = new ImageView();
        try {
            String path = hebergement.getImage();
            if (path != null && !path.isEmpty()) {
                File file = new File(path);
                if (file.exists()) {
                    imageView.setImage(new Image(file.toURI().toString()));
                    imageView.setFitHeight(160);
                    imageView.setFitWidth(280);
                    imageView.setPreserveRatio(true);
                }
            }
        } catch (Exception ignored) {}

        if (imageView.getImage() == null) {
            Label icon = new Label(resolveIcon(hebergement.getType_hebergement()));
            icon.setStyle("-fx-font-size: 48px;");
            imagePane.getChildren().add(icon);
        } else {
            imagePane.getChildren().add(imageView);
        }

        Label typeBadge = new Label(resolveIcon(hebergement.getType_hebergement())
                + " " + hebergement.getType_hebergement());
        typeBadge.setStyle("-fx-background-color: white; -fx-text-fill: #1ABC9C;"
                + " -fx-font-weight: bold; -fx-padding: 5 10;"
                + " -fx-background-radius: 12; -fx-font-size: 11px;");
        StackPane.setAlignment(typeBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(typeBadge, new Insets(10));
        imagePane.getChildren().add(typeBadge);

        // ── Contenu ──
        VBox content = new VBox(8);
        content.setPadding(new Insets(15));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 15 15;");

        Label titre = new Label(hebergement.getTitre());
        titre.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        titre.setWrapText(true);

        HBox infoBox = new HBox(10);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        Label capacite = new Label("👥 " + hebergement.getCapacite() + " " + lang.get("card.persons"));
        capacite.setStyle("-fx-font-size: 12px; -fx-text-fill: #7F8C8D;");
        Label prix = new Label(String.format("%.0f TND%s",
                hebergement.getPrixParNuit(), lang.get("card.per.night")));
        prix.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #F39C12;");
        Label sep = new Label("•");
        sep.setStyle("-fx-text-fill: #CBD5E1;");
        infoBox.getChildren().addAll(capacite, sep, prix);

        boolean dispo = hebergement.isDisponible_heberg();
        Label disponibilite = new Label(dispo
                ? lang.get("card.available") : lang.get("card.not.available"));
        disponibilite.setStyle(dispo
                ? "-fx-background-color:#D1F2EB;-fx-text-fill:#1ABC9C;-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:4 10;-fx-background-radius:10;"
                : "-fx-background-color:#FADBD8;-fx-text-fill:#E74C3C;-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:4 10;-fx-background-radius:10;");

        // ── Bouton Lieux Proches ──
        Button btnNearby = new Button("📍 Lieux proches");
        styleCardBtn(btnNearby, "#EBF5FB", "#2980B9", "#AED6F1");
        btnNearby.setMaxWidth(Double.MAX_VALUE);
        btnNearby.setOnAction(e -> { selectHebergement(hebergement); openNearbyPlaces(); });
        btnNearby.setOnMouseEntered(e -> styleCardBtn(btnNearby, "#2980B9", "white", "#2980B9"));
        btnNearby.setOnMouseExited(e  -> styleCardBtn(btnNearby, "#EBF5FB", "#2980B9", "#AED6F1"));

        // ── Bouton Météo ──
        Button btnWeatherCard = new Button("🌤 Météo");
        styleCardBtn(btnWeatherCard, "#EBF8FF", "#2C7BB2", "#90CDF4");
        btnWeatherCard.setMaxWidth(Double.MAX_VALUE);
        btnWeatherCard.setOnAction(e -> { selectHebergement(hebergement); openWeather(); });
        btnWeatherCard.setOnMouseEntered(e -> styleCardBtn(btnWeatherCard, "#2C7BB2", "white", "#2C7BB2"));
        btnWeatherCard.setOnMouseExited(e  -> styleCardBtn(btnWeatherCard, "#EBF8FF", "#2C7BB2", "#90CDF4"));

        // ── Grille de boutons 2 colonnes ──
        GridPane btnGrid = new GridPane();
        btnGrid.setHgap(6);
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(50);
        btnGrid.getColumnConstraints().addAll(col1, col2);
        btnGrid.add(btnNearby,      0, 0);
        btnGrid.add(btnWeatherCard, 1, 0);

        content.getChildren().addAll(titre, infoBox, new Separator(), disponibilite, btnGrid);
        card.getChildren().addAll(imagePane, content);
        if (!dispo) card.setStyle("-fx-opacity: 0.6;");
        return card;
    }

    private void styleCardBtn(Button btn, String bg, String fg, String border) {
        btn.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-text-fill:" + fg + ";" +
                        "-fx-font-weight:bold; -fx-font-size:11px;" +
                        "-fx-padding:5 8; -fx-background-radius:10; -fx-cursor:hand;" +
                        "-fx-border-color:" + border + "; -fx-border-radius:10; -fx-border-width:1;"
        );
    }

    // =========================================================
    // SÉLECTION HÉBERGEMENT
    // =========================================================

    private void selectHebergement(Hebergement hebergement) {
        selectedHebergement = hebergement;
        safeSetText(selectedHebergementTitre, hebergement.getTitre());
        safeSetText(selectedHebergementPrix,
                String.format("%.0f TND%s", hebergement.getPrixParNuit(), lang.get("card.per.night")));

        if (hebergementImage != null) {
            try {
                String path = hebergement.getImage();
                if (path != null && !path.isEmpty()) {
                    File file = new File(path);
                    hebergementImage.setImage(file.exists() ? new Image(file.toURI().toString()) : null);
                } else {
                    hebergementImage.setImage(null);
                }
            } catch (Exception e) {
                hebergementImage.setImage(null);
            }
        }

        clearFormFields();

        if (btnReserver != null) btnReserver.setDisable(!hebergement.isDisponible_heberg());

        if (btnNearbyPlaces != null) {
            btnNearbyPlaces.setDisable(false);
            btnNearbyPlaces.setText("📍 Lieux proches de " + hebergement.getTitre());
        }

        if (btnWeather != null) {
            btnWeather.setDisable(false);
            btnWeather.setText("🌤 Météo de " + hebergement.getTitre());
        }

        hideAllErrors();
    }

    // =========================================================
    // FORMULAIRE
    // =========================================================

    private void clearFormFields() {
        if (dateDebutPicker != null) dateDebutPicker.setValue(null);
        if (dateFinPicker   != null) dateFinPicker.setValue(null);
        if (txtPrixTotal    != null) txtPrixTotal.clear();
        setupStatutCombo();
    }

    private void updatePrixTotal() {
        if (selectedHebergement != null
                && dateDebutPicker.getValue() != null
                && dateFinPicker.getValue() != null) {
            long jours = ChronoUnit.DAYS.between(
                    dateDebutPicker.getValue(), dateFinPicker.getValue());
            float total = jours > 0 ? selectedHebergement.getPrixParNuit() * jours : 0;
            txtPrixTotal.setText(String.format("%.2f TND", total));
        } else if (txtPrixTotal != null) {
            txtPrixTotal.clear();
        }
    }

    @FXML
    private void reserver() {
        // ✅ Vérification JWT avant réservation
        if (!checkUserAuth()) return;

        if (!validateAllFields()) return;
        try {
            Reservation reservation = new Reservation(
                    selectedHebergement, currentUser,
                    dateDebutPicker.getValue().format(DATE_FORMATTER),
                    dateFinPicker.getValue().format(DATE_FORMATTER),
                    comboStatut.getValue()
            );
            reservationCRUD.ajouterh(reservation);

            // ✅ Journalisation de l'action
            logUserAction("RESERVATION", "Réservation créée pour " + selectedHebergement.getTitre());

            showAlert(lang.get("alert.success.title"), lang.get("alert.success.message"));

            clearForm();
            selectedHebergement = null;
            safeSetText(selectedHebergementTitre, lang.get("form.no.selection"));
            safeSetText(selectedHebergementPrix, "");
            if (hebergementImage != null) hebergementImage.setImage(null);
            if (btnReserver     != null) btnReserver.setDisable(true);
            if (btnNearbyPlaces != null) {
                btnNearbyPlaces.setDisable(true);
                btnNearbyPlaces.setText("📍 Lieux proches");
            }
            if (btnWeather != null) {
                btnWeather.setDisable(true);
                btnWeather.setText("🌤 Météo");
            }

        } catch (SQLException e) {
            showAlert(lang.get("alert.error.title"),
                    MessageFormat.format(lang.get("alert.error.reserve"), e.getMessage()));
            e.printStackTrace();
        }
    }

    /**
     * ✅ Journalise les actions utilisateur (optionnel)
     */
    private void logUserAction(String actionType, String details) {
        if (currentUser != null) {
            System.out.println("📝 [ACTION UTILISATEUR] " + currentUser.getE_mail() +
                    " - " + actionType + " - " + details);
            // Vous pouvez appeler un service de logs ici
        }
    }

    @FXML private void annuler() { clearForm(); }

    private void clearForm() {
        clearFormFields();
        hideAllErrors();
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private boolean validateDates() {
        LocalDate debut = dateDebutPicker.getValue();
        LocalDate fin   = dateFinPicker.getValue();
        if (debut == null) { showError(lblDateDebutErreur, lang.get("error.date.debut.required")); return false; }
        else hideError(lblDateDebutErreur);
        if (fin == null)   { showError(lblDateFinErreur,   lang.get("error.date.fin.required"));   return false; }
        else hideError(lblDateFinErreur);
        if (fin.isBefore(debut))             { showError(lblDateFinErreur,   lang.get("error.date.fin.before")); return false; }
        if (debut.isBefore(LocalDate.now())) { showError(lblDateDebutErreur, lang.get("error.date.debut.past")); return false; }
        return true;
    }

    private boolean validateAllFields() {
        boolean isValid = validateDates();
        if (selectedHebergement == null) { showAlert(lang.get("alert.error.title"), lang.get("error.no.hebergement")); isValid = false; }
        if (currentUser == null)         { showAlert(lang.get("alert.error.title"), lang.get("error.no.user"));        isValid = false; }
        return isValid;
    }

    private void showError(Label label, String message) {
        if (label != null) { label.setText(message); label.setVisible(true); label.setManaged(true); }
    }

    private void hideError(Label label) {
        if (label != null) { label.setVisible(false); label.setManaged(false); label.setText(""); }
    }

    private void hideAllErrors() {
        hideError(lblDateDebutErreur);
        hideError(lblDateFinErreur);
    }

    // =========================================================
    // NAVIGATION
    // =========================================================

    @FXML
    private void goToAccueil() {
        // ✅ Vérification JWT avant navigation
        if (!checkUserAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/user/dashboard/homeClient.fxml"));
            if (loader.getLocation() == null) throw new Exception("homeClient.fxml introuvable");
            Parent root  = loader.load();
            Stage  stage = (Stage) btnAccueil.getScene().getWindow();
            Scene  scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/user/dashboard/homeClient.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(lang.get("nav.title.home"));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(lang.get("alert.error.title"),
                    MessageFormat.format(lang.get("nav.error.home"), e.getMessage()));
        }
    }

    @FXML
    private void goToProfil() {
        // ✅ Vérification JWT avant navigation
        if (!checkUserAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/user/front/userProfil.fxml"));
            if (loader.getLocation() == null) throw new Exception("userProfil.fxml introuvable");
            Parent root  = loader.load();
            Stage  stage = (Stage) btnProfil.getScene().getWindow();
            Scene  scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/user/front/userProfil.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(lang.get("nav.title.profile"));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(lang.get("alert.error.title"),
                    MessageFormat.format(lang.get("nav.error.profile"), e.getMessage()));
        }
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
            Stage stage = (Stage) btnAccueil.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de charger la page de connexion: " + e.getMessage());
        }
    }

    // =========================================================
    // ALERT
    // =========================================================

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
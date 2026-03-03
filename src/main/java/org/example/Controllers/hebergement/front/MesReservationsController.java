package org.example.Controllers.hebergement.front;

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
import org.example.Entites.hebergement.Reservation;
import org.example.Services.hebergement.ReservationCRUD;
import org.example.Utils.UserSession;
import org.example.Entites.user.User;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;

public class MesReservationsController implements Initializable {

    // ── Header ──
    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private Button btnAccueil;
    @FXML private Button btnRetour;
    @FXML private TextField searchField;

    // ── Stats ──
    @FXML private Label lblTotalRes;
    @FXML private Label lblResActives;
    @FXML private Label lblResPasses;
    @FXML private Label lblMontantTotal;

    // ── Filtres ──
    @FXML private ComboBox<String> filterStatut;
    @FXML private Button btnFiltrerTout;
    @FXML private Button btnFiltrerActif;
    @FXML private Button btnFiltrerPasse;

    // ── Liste ──
    @FXML private VBox reservationListContainer;
    @FXML private VBox lblAucuneReservation;

    private final ReservationCRUD reservationCRUD = new ReservationCRUD();
    private ObservableList<Reservation> allReservations = FXCollections.observableArrayList();
    private User currentUser;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            showAlert("Erreur", "Aucun utilisateur connecté.");
            return;
        }
        displayUserInfo();
        setupFilterCombo();
        loadReservations();
        setupSearch();
    }

    // =========================================================
    // DONNÉES
    // =========================================================

    private void loadReservations() {
        try {
            List<Reservation> list = reservationCRUD.getReservationsByUser(currentUser.getId());
            allReservations.setAll(list);
            updateStats();
            displayReservations(allReservations);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger vos réservations : " + e.getMessage());
        }
    }

    private void updateStats() {
        int total = allReservations.size();
        long actives = allReservations.stream()
                .filter(r -> isActive(r))
                .count();
        long passes = allReservations.stream()
                .filter(r -> isPast(r))
                .count();
        double montant = allReservations.stream()
                .mapToDouble(r -> calcPrix(r))
                .sum();

        safeSetText(lblTotalRes,    String.valueOf(total));
        safeSetText(lblResActives,  String.valueOf(actives));
        safeSetText(lblResPasses,   String.valueOf(passes));
        safeSetText(lblMontantTotal, String.format("%.0f TND", montant));
    }

    private boolean isActive(Reservation r) {
        try {
            LocalDate fin = LocalDate.parse(r.getDateFinR(), DATE_FMT);
            return !fin.isBefore(LocalDate.now())
                    && !"ANNULÉE".equalsIgnoreCase(r.getStatutR())
                    && !"CANCELLED".equalsIgnoreCase(r.getStatutR());
        } catch (Exception e) { return false; }
    }

    private boolean isPast(Reservation r) {
        try {
            LocalDate fin = LocalDate.parse(r.getDateFinR(), DATE_FMT);
            return fin.isBefore(LocalDate.now());
        } catch (Exception e) { return false; }
    }

    private double calcPrix(Reservation r) {
        try {
            LocalDate debut = LocalDate.parse(r.getDateDebutR(), DATE_FMT);
            LocalDate fin   = LocalDate.parse(r.getDateFinR(),   DATE_FMT);
            long jours = ChronoUnit.DAYS.between(debut, fin);
            return jours > 0 ? r.getHebergement().getPrixParNuit() * jours : 0;
        } catch (Exception e) { return 0; }
    }

    // =========================================================
    // AFFICHAGE
    // =========================================================

    private void displayReservations(List<Reservation> list) {
        if (reservationListContainer == null) return;
        reservationListContainer.getChildren().clear();

        if (list.isEmpty()) {
            if (lblAucuneReservation != null) {
                lblAucuneReservation.setVisible(true);
                lblAucuneReservation.setManaged(true);
            }
            return;
        }

        if (lblAucuneReservation != null) {
            lblAucuneReservation.setVisible(false);
            lblAucuneReservation.setManaged(false);
        }

        for (Reservation r : list) {
            reservationListContainer.getChildren().add(createReservationCard(r));
        }
    }

    private HBox createReservationCard(Reservation r) {
        HBox card = new HBox(0);
        card.getStyleClass().add("reservation-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(Double.MAX_VALUE);

        // ── Bande couleur statut (gauche) ──
        VBox colorBar = new VBox();
        colorBar.setPrefWidth(6);
        colorBar.setMinWidth(6);
        colorBar.setStyle("-fx-background-color: " + getStatutColor(r.getStatutR()) + ";"
                + " -fx-background-radius: 14 0 0 14;");

        // ── Image hébergement ──
        StackPane imgPane = new StackPane();
        imgPane.setPrefSize(90, 90);
        imgPane.setMinSize(90, 90);
        imgPane.setMaxSize(90, 90);
        imgPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #1ABC9C, #3498DB);"
                + " -fx-background-radius: 12;");
        imgPane.setMargin(imgPane, new Insets(0, 0, 0, 14));
        HBox.setMargin(imgPane, new Insets(14, 0, 14, 14));

        try {
            String path = r.getHebergement().getImage();
            if (path != null && !path.isBlank()) {
                File file = new File(path);
                if (file.exists()) {
                    ImageView iv = new ImageView(new Image(file.toURI().toString()));
                    iv.setFitWidth(90); iv.setFitHeight(90);
                    iv.setPreserveRatio(true);
                    imgPane.getChildren().add(iv);
                }
            }
        } catch (Exception ignored) {}

        if (imgPane.getChildren().isEmpty()) {
            Label icon = new Label(resolveIcon(r.getHebergement().getType_hebergement()));
            icon.setStyle("-fx-font-size: 30px;");
            imgPane.getChildren().add(icon);
        }

        // ── Infos principales ──
        VBox infoBox = new VBox(6);
        infoBox.setPadding(new Insets(14, 0, 14, 16));
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Titre + badge statut
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label titre = new Label(r.getHebergement().getTitre());
        titre.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        Label statutBadge = createStatutBadge(r.getStatutR());
        titleRow.getChildren().addAll(titre, statutBadge);

        // Type hébergement
        Label type = new Label(resolveIcon(r.getHebergement().getType_hebergement())
                + " " + r.getHebergement().getType_hebergement());
        type.setStyle("-fx-font-size: 12px; -fx-text-fill: #7F8C8D;");

        // Dates
        HBox datesRow = new HBox(8);
        datesRow.setAlignment(Pos.CENTER_LEFT);
        Label dateDebut = createInfoChip("📅", formatDate(r.getDateDebutR()), "#EBF5FB", "#2980B9");
        Label arrow     = new Label("→");
        arrow.setStyle("-fx-text-fill: #BDC3C7; -fx-font-size: 14px;");
        Label dateFin   = createInfoChip("📅", formatDate(r.getDateFinR()), "#EBF5FB", "#2980B9");

        long jours;
        try {
            jours = ChronoUnit.DAYS.between(
                    LocalDate.parse(r.getDateDebutR(), DATE_FMT),
                    LocalDate.parse(r.getDateFinR(), DATE_FMT));
        } catch (Exception e) { jours = 0; }

        Label duree = new Label("(" + jours + " nuit" + (jours > 1 ? "s" : "") + ")");
        duree.setStyle("-fx-font-size: 11px; -fx-text-fill: #95A5A6;");
        datesRow.getChildren().addAll(dateDebut, arrow, dateFin, duree);

        infoBox.getChildren().addAll(titleRow, type, datesRow);

        // ── Prix ──
        VBox prixBox = new VBox(4);
        prixBox.setAlignment(Pos.CENTER_RIGHT);
        prixBox.setPadding(new Insets(14, 20, 14, 0));
        prixBox.setMinWidth(130);

        double total = calcPrix(r);
        Label prixTotal = new Label(String.format("%.0f TND", total));
        prixTotal.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #F39C12;");
        Label prixNuit = new Label(String.format("%.0f TND/nuit", r.getHebergement().getPrixParNuit()));
        prixNuit.setStyle("-fx-font-size: 11px; -fx-text-fill: #95A5A6;");

        Label idLabel = new Label("#" + r.getId_reservation());
        idLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #CBD5E1;"
                + " -fx-background-color: #F8F9FA; -fx-padding: 2 7; -fx-background-radius: 6;");
        prixBox.getChildren().addAll(prixTotal, prixNuit, idLabel);

        card.getChildren().addAll(colorBar, imgPane, infoBox, prixBox);
        return card;
    }

    private Label createInfoChip(String icon, String text, String bg, String fg) {
        Label l = new Label(icon + " " + text);
        l.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";"
                + " -fx-font-size: 11px; -fx-font-weight: bold;"
                + " -fx-padding: 4 10; -fx-background-radius: 8;");
        return l;
    }

    private Label createStatutBadge(String statut) {
        String color  = getStatutColor(statut);
        String bgOpaq = getStatutBg(statut);
        Label l = new Label(getStatutIcon(statut) + " " + statut);
        l.setStyle("-fx-background-color: " + bgOpaq + "; -fx-text-fill: " + color + ";"
                + " -fx-font-size: 11px; -fx-font-weight: bold;"
                + " -fx-padding: 4 10; -fx-background-radius: 10;");
        return l;
    }

    private String getStatutColor(String statut) {
        if (statut == null) return "#7F8C8D";
        return switch (statut.toUpperCase()) {
            case "CONFIRMÉE", "CONFIRMED" -> "#1ABC9C";
            case "EN ATTENTE", "PENDING"  -> "#F39C12";
            case "ANNULÉE",   "CANCELLED" -> "#E74C3C";
            case "TERMINÉE",  "FINISHED"  -> "#3498DB";
            default -> "#7F8C8D";
        };
    }

    private String getStatutBg(String statut) {
        if (statut == null) return "#F0F0F0";
        return switch (statut.toUpperCase()) {
            case "CONFIRMÉE", "CONFIRMED" -> "#D1F2EB";
            case "EN ATTENTE", "PENDING"  -> "#FDEBD0";
            case "ANNULÉE",   "CANCELLED" -> "#FADBD8";
            case "TERMINÉE",  "FINISHED"  -> "#D6EAF8";
            default -> "#F0F0F0";
        };
    }

    private String getStatutIcon(String statut) {
        if (statut == null) return "•";
        return switch (statut.toUpperCase()) {
            case "CONFIRMÉE", "CONFIRMED" -> "✅";
            case "EN ATTENTE", "PENDING"  -> "⏳";
            case "ANNULÉE",   "CANCELLED" -> "❌";
            case "TERMINÉE",  "FINISHED"  -> "🏁";
            default -> "•";
        };
    }

    private String resolveIcon(String type) {
        if (type == null) return "🏘️";
        String t = type.toLowerCase();
        if (t.contains("hôtel") || t.contains("hotel"))     return "🏨";
        if (t.contains("appart") || t.contains("apart"))    return "🏢";
        if (t.contains("villa"))                             return "🏡";
        if (t.contains("riad") || t.contains("maison"))     return "🏠";
        if (t.contains("chalet") || t.contains("montagne")) return "🏔️";
        if (t.contains("camp")  || t.contains("tente"))     return "🏕️";
        if (t.contains("bungalow"))                         return "🛖";
        if (t.contains("resort"))                           return "🌴";
        if (t.contains("auberge") || t.contains("hostel"))  return "🛏️";
        return "🏘️";
    }

    private String formatDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FMT).format(DISPLAY_FMT);
        } catch (Exception e) { return dateStr; }
    }

    // =========================================================
    // FILTRES & RECHERCHE
    // =========================================================

    private void setupFilterCombo() {
        if (filterStatut == null) return;
        filterStatut.setItems(FXCollections.observableArrayList(
                "Tous", "EN ATTENTE", "CONFIRMÉE", "ANNULÉE", "TERMINÉE"
        ));
        filterStatut.setValue("Tous");
        filterStatut.setOnAction(e -> applyFilters());
    }

    private void setupSearch() {
        if (searchField != null)
            searchField.textProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void applyFilters() {
        String search = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        String statut = filterStatut != null ? filterStatut.getValue() : "Tous";

        List<Reservation> filtered = allReservations.stream()
                .filter(r -> {
                    boolean matchSearch = search.isEmpty()
                            || r.getHebergement().getTitre().toLowerCase().contains(search)
                            || r.getStatutR().toLowerCase().contains(search);
                    boolean matchStatut = "Tous".equals(statut)
                            || statut.equalsIgnoreCase(r.getStatutR());
                    return matchSearch && matchStatut;
                })
                .toList();

        displayReservations(filtered);
    }

    @FXML private void filtrerTout()   { if (filterStatut != null) filterStatut.setValue("Tous");      applyFilters(); }
    @FXML private void filtrerActif()  { if (filterStatut != null) filterStatut.setValue("CONFIRMÉE"); applyFilters(); }
    @FXML private void filtrerPasse()  { if (filterStatut != null) filterStatut.setValue("TERMINÉE");  applyFilters(); }

    // =========================================================
    // NAVIGATION
    // =========================================================

    @FXML
    private void goToAccueil() {
        navigateTo("/user/dashboard/homeClient.fxml",
                "/user/dashboard/homeClient.css", "Accueil", btnAccueil);
    }

    @FXML
    private void goToRetour() {
        navigateTo("/hebergement/front/Reservation.fxml",
                "/hebergement/front/StyleRSV.css", "Réservations", btnRetour);
    }

    private void navigateTo(String fxmlPath, String cssPath, String title, javafx.scene.Node src) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) src.getScene().getWindow();
            Scene scene = new Scene(root);
            try { scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm()); }
            catch (Exception ignored) {}
            stage.setScene(scene);
            stage.setTitle(title);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur navigation", e.getMessage());
        }
    }

    // =========================================================
    // UTILITAIRES
    // =========================================================

    private void displayUserInfo() {
        safeSetText(userNameLabel,  currentUser.getPrenom() + " " + currentUser.getNom());
        safeSetText(userEmailLabel, currentUser.getE_mail());
    }

    private void safeSetText(Label l, String t) { if (l != null) l.setText(t); }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}
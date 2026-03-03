package org.example.Controllers.activite;

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
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.example.Entites.activite.Activite;
import org.example.Entites.activite.VirtualTour;
import org.example.Entites.user.User;
import org.example.Services.activite.ActiviteService;
import org.example.Services.activite.ActiviteServiceImpl;
import org.example.Services.activite.VirtualTourServiceImpl;
import org.example.Utils.UserSession;

import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class ActiviteFrontOfficeController implements Initializable {

    @FXML private TextField searchField;
    @FXML private VBox typeFiltersBox;
    @FXML private Slider prixSlider;
    @FXML private Text prixText;
    @FXML private ComboBox<String> lieuCombo;
    @FXML private CheckBox disponibleCheckbox;
    @FXML private Text resultsCountText;
    @FXML private ComboBox<String> sortCombo;
    @FXML private FlowPane activitiesFlowPane;
    @FXML private Label userNameLabel;

    private ObservableList<Activite> allActivites = FXCollections.observableArrayList();
    private ObservableList<Activite> filteredActivites = FXCollections.observableArrayList();
    private Map<String, CheckBox> typeCheckBoxes = new HashMap<>();

    private ActiviteService activiteService;

    // ✅ Utilisateur connecté via JWT
    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ✅ Vérifier l'authentification JWT
        if (!checkUserAuth()) {
            return;
        }

        activiteService = new ActiviteServiceImpl();

        // ✅ Force 4 cards par ligne
        activitiesFlowPane.setPrefWrapLength(885);
        activitiesFlowPane.setMaxWidth(885);
        activitiesFlowPane.setHgap(15);
        activitiesFlowPane.setVgap(20);

        setupFilters();
        loadActivitesFromDatabase();
        displayActivites();
        setupListeners();
    }

    /**
     * ✅ Vérifie si l'utilisateur est authentifié via JWT
     */
    private boolean checkUserAuth() {
        if (!UserSession.getInstance().isTokenValid()) {
            showError("Session expirée", "Votre session a expiré. Veuillez vous reconnecter.");
            redirectToLogin();
            return false;
        }

        currentUser = UserSession.getInstance().getCurrentUser();

        if (currentUser == null) {
            showError("Erreur", "Aucun utilisateur connecté.");
            redirectToLogin();
            return false;
        }

        // ✅ Vérifier si l'utilisateur est banni
        if (currentUser.getStatus() != null &&
                currentUser.getStatus().name().equalsIgnoreCase("Banned")) {
            showError("Compte suspendu",
                    "Votre compte est suspendu. Veuillez contacter l'administrateur.");
            redirectToLogin();
            return false;
        }

        // Afficher les infos
        System.out.println("✅ Front-office activités - Utilisateur: " +
                currentUser.getPrenom() + " " + currentUser.getNom());
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
            Stage stage = (Stage) (activitiesFlowPane != null ?
                    activitiesFlowPane.getScene().getWindow() :
                    searchField.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadActivitesFromDatabase() {
        try {
            allActivites.clear();
            filteredActivites.clear();

            List<Activite> activites = activiteService.getAllActivites();
            allActivites.addAll(activites);
            filteredActivites.addAll(allActivites);

            Set<String> lieux = allActivites.stream()
                    .map(Activite::getLieu)
                    .collect(Collectors.toSet());
            lieuCombo.getItems().addAll(lieux);

            System.out.println("✅ " + activites.size() + " activités chargées pour le catalogue");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement des activités");
            e.printStackTrace();
            showError("Erreur de chargement",
                    "Impossible de charger les activités depuis la base de données.");
        }
    }

    private void setupFilters() {
        String[] types = { "Excursion", "Sport", "Culture", "Aventure", "Détente" };
        for (String type : types) {
            CheckBox cb = new CheckBox(type);
            cb.setStyle("-fx-text-fill: #2C3E50; -fx-font-size: 13px;");
            cb.setSelected(true);
            cb.setOnAction(e -> applyFilters());
            typeCheckBoxes.put(type, cb);
            typeFiltersBox.getChildren().add(cb);
        }

        prixSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            prixText.setText(String.format("0 - %.0f DT", newVal.doubleValue()));
            applyFilters();
        });

        lieuCombo.setItems(FXCollections.observableArrayList("Tous les lieux"));
        lieuCombo.setValue("Tous les lieux");
        lieuCombo.setOnAction(e -> applyFilters());

        disponibleCheckbox.setOnAction(e -> applyFilters());

        sortCombo.setOnAction(e -> {
            sortActivites();
            displayActivites();
        });
    }

    private void setupListeners() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        double maxPrix = prixSlider.getValue();
        String lieuFilter = lieuCombo.getValue();
        boolean onlyDisponible = disponibleCheckbox.isSelected();

        Set<String> selectedTypes = typeCheckBoxes.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        filteredActivites.clear();

        for (Activite activite : allActivites) {
            boolean matchSearch = searchText.isEmpty() ||
                    activite.getNomA().toLowerCase().contains(searchText) ||
                    activite.getDescriptionA().toLowerCase().contains(searchText) ||
                    activite.getLieu().toLowerCase().contains(searchText);

            boolean matchType = selectedTypes.contains(activite.getType());
            boolean matchPrix = activite.getPrixParPersonne() <= maxPrix;
            boolean matchLieu = lieuFilter.equals("Tous les lieux") || activite.getLieu().equals(lieuFilter);
            boolean matchDisponible = !onlyDisponible || activite.getStatut().equals("Disponible");

            if (matchSearch && matchType && matchPrix && matchLieu && matchDisponible) {
                filteredActivites.add(activite);
            }
        }

        sortActivites();
        displayActivites();
        updateResultsCount();
    }

    private void sortActivites() {
        String sortBy = sortCombo.getValue();
        switch (sortBy) {
            case "Prix croissant":
                filteredActivites.sort(Comparator.comparing(Activite::getPrixParPersonne));
                break;
            case "Prix décroissant":
                filteredActivites.sort(Comparator.comparing(Activite::getPrixParPersonne).reversed());
                break;
            case "Nom A-Z":
                filteredActivites.sort(Comparator.comparing(Activite::getNomA));
                break;
            default:
                break;
        }
    }

    private void displayActivites() {
        activitiesFlowPane.getChildren().clear();

        for (Activite activite : filteredActivites) {
            VBox card = createActivityCard(activite);
            activitiesFlowPane.getChildren().add(card);
        }

        if (filteredActivites.isEmpty()) {
            VBox emptyState = new VBox(20);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPrefSize(600, 400);

            Text emoji = new Text("🔍");
            emoji.setStyle("-fx-font-size: 72px;");

            Text message = new Text("Aucune activité trouvée");
            message.setStyle("-fx-font-size: 20px; -fx-fill: #7F8C8D; -fx-font-weight: bold;");

            Text suggestion = new Text("Essayez de modifier vos critères de recherche");
            suggestion.setStyle("-fx-font-size: 14px; -fx-fill: #95A5A6;");

            emptyState.getChildren().addAll(emoji, message, suggestion);
            activitiesFlowPane.getChildren().add(emptyState);
        }
    }

    private VBox createActivityCard(Activite activite) {
        VBox card = new VBox();
        card.getStyleClass().add("activity-card");
        card.setPrefWidth(210);
        card.setMaxWidth(210);
        card.setCursor(javafx.scene.Cursor.HAND);

        // ── Zone image ──────────────────────────────────────
        StackPane imagePane = new StackPane();
        imagePane.setPrefHeight(150);
        imagePane.setMaxWidth(Double.MAX_VALUE);
        imagePane.setStyle("-fx-background-radius: 15 15 0 0; -fx-background-color: #1A1A2E;");

        boolean imageLoaded = false;
        String imagePath = activite.getImage();

        if (imagePath != null && !imagePath.isBlank() && !imagePath.equals("default.jpg")) {
            File imgFile = new File("src/main/resources/images/" + imagePath);
            if (!imgFile.exists()) {
                imgFile = new File("src/main/resources/images/activites/"
                        + imagePath.replace("activites/", ""));
            }
            if (imgFile.exists()) {
                try {
                    javafx.scene.image.Image img = new javafx.scene.image.Image(
                            imgFile.toURI().toString(), 210, 150, false, true, true);
                    javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                    iv.setFitWidth(210);
                    iv.setFitHeight(150);
                    iv.setPreserveRatio(false);

                    javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(210, 150);
                    clip.setArcWidth(30);
                    clip.setArcHeight(30);
                    iv.setClip(clip);
                    imagePane.getChildren().add(iv);

                    javafx.scene.shape.Rectangle overlay = new javafx.scene.shape.Rectangle(210, 150);
                    overlay.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.18));
                    imagePane.getChildren().add(overlay);
                    imageLoaded = true;
                } catch (Exception ex) {
                    System.err.println("⚠️ Image introuvable : " + imgFile.getPath());
                }
            }
        }

        if (!imageLoaded) {
            imagePane.setStyle("-fx-background-radius: 15 15 0 0; -fx-background-color: "
                    + getGradientForType(activite.getType()) + ";");
            Text imageIcon = new Text(getIconForType(activite.getType()));
            imageIcon.setStyle("-fx-font-size: 48px;");
            imagePane.getChildren().add(imageIcon);
        }

        // Badge type (coin haut droit)
        Label typeBadge = new Label(activite.getType());
        typeBadge.getStyleClass().add("activity-type-badge");
        StackPane.setAlignment(typeBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(typeBadge, new Insets(10));
        imagePane.getChildren().add(typeBadge);

        // ── Contenu ──────────────────────────────────────────
        VBox content = new VBox(8);
        content.getStyleClass().add("activity-content");

        // Titre
        Text title = new Text(activite.getNomA());
        title.getStyleClass().add("activity-title");
        title.setWrappingWidth(170);

        // Lieu
        HBox locationBox = new HBox(4);
        locationBox.setAlignment(Pos.CENTER_LEFT);
        Text locationIcon = new Text("📍");
        Text location = new Text(activite.getLieu());
        location.getStyleClass().add("activity-location");
        locationBox.getChildren().addAll(locationIcon, location);

        // Description courte
        String desc = activite.getDescriptionA();
        if (desc.length() > 70)
            desc = desc.substring(0, 70) + "...";
        Text description = new Text(desc);
        description.getStyleClass().add("activity-description");
        description.setWrappingWidth(170);

        // Prix + Capacité
        HBox infoBox = new HBox(10);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        VBox priceBox = new VBox(2);
        Text priceLabel = new Text("Prix");
        priceLabel.setStyle("-fx-font-size: 10px; -fx-fill: #7F8C8D;");
        Text price = new Text(String.format("%.0f DT", activite.getPrixParPersonne()));
        price.getStyleClass().add("activity-price");
        priceBox.getChildren().addAll(priceLabel, price);

        VBox capacityBox = new VBox(2);
        Text capacityLabel = new Text("Capacité");
        capacityLabel.setStyle("-fx-font-size: 10px; -fx-fill: #7F8C8D;");
        Text capacity = new Text(activite.getCapaciteMax() + " pers.");
        capacity.setStyle("-fx-font-size: 12px; -fx-fill: #2C3E50; -fx-font-weight: bold;");
        capacityBox.getChildren().addAll(capacityLabel, capacity);

        infoBox.getChildren().addAll(priceBox, new Separator(javafx.geometry.Orientation.VERTICAL), capacityBox);

        // Statut + Bouton
        Label statusBadge = new Label(activite.getStatut());
        if (activite.getStatut().equals("Disponible")) {
            statusBadge.getStyleClass().add("activity-status-available");
        } else {
            statusBadge.getStyleClass().add("activity-status-complet");
        }

        Button viewBtn = new Button("Voir les plannings ➜");
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        if (activite.getStatut().equals("Disponible")) {
            viewBtn.setStyle("-fx-background-color: #1ABC9C; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-padding: 8 10; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 11px;");
        } else {
            viewBtn.setDisable(true);
            viewBtn.setStyle("-fx-background-color: #95A5A6; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-padding: 8 10; -fx-background-radius: 8; -fx-font-size: 11px;");
        }
        viewBtn.setOnAction(e -> openActiviteDetails(activite));

        content.getChildren().addAll(title, locationBox, description, new Separator(), infoBox, statusBadge,
                viewBtn);
        card.getChildren().addAll(imagePane, content);

        return card;
    }

    private String getGradientForType(String type) {
        switch (type != null ? type : "") {
            case "Aventure":
                return "linear-gradient(to bottom right, #FF6B35, #F7931E)";
            case "Sport":
                return "linear-gradient(to bottom right, #0F3460, #16213E)";
            case "Culture":
                return "linear-gradient(to bottom right, #F7C948, #F4A11D)";
            case "Détente":
                return "linear-gradient(to bottom right, #1ABC9C, #16A085)";
            case "Excursion":
                return "linear-gradient(to bottom right, #E74C3C, #C0392B)";
            default:
                return "linear-gradient(to bottom right, #D4A843, #E8C97E)";
        }
    }

    private String getIconForType(String type) {
        switch (type) {
            case "Aventure":
                return "⛰️";
            case "Sport":
                return "🏊";
            case "Culture":
                return "🏛️";
            case "Détente":
                return "🧘";
            case "Excursion":
                return "🐎";
            default:
                return "🎯";
        }
    }

    private void openActiviteDetails(Activite activite) {
        try {
            // ✅ CORRECTION : Utiliser le bon nom de fichier "ActivateDetails.fxml"
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/activite/views/frontoffice/ActiviteDetails.fxml"));

            // Vérifier si le fichier existe
            if (loader.getLocation() == null) {
                throw new IOException("Fichier ActivateDetails.fxml introuvable dans /activite/views/frontoffice/");
            }

            Parent root = loader.load();
            ActiviteDetailsController controller = loader.getController();
            controller.setActivite(activite);

            // Passer l'utilisateur connecté si le contrôleur l'accepte
            try {
                java.lang.reflect.Method method = controller.getClass().getMethod("initUserData", User.class);
                method.invoke(controller, currentUser);
            } catch (Exception e) {
                // Ignorer si la méthode n'existe pas
            }

            Stage stage = new Stage();
            stage.setTitle("Détails - " + activite.getNomA());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir les détails de l'activité.\nChemin: /activite/views/frontoffice/ActivateDetails.fxml");
        }
    }

    private void updateResultsCount() {
        int count = filteredActivites.size();
        resultsCountText.setText(count + " activité" + (count > 1 ? "s" : "") + " trouvée"
                + (count > 1 ? "s" : ""));
    }

    @FXML
    private void resetFilters() {
        searchField.clear();
        prixSlider.setValue(200);
        lieuCombo.setValue("Tous les lieux");
        disponibleCheckbox.setSelected(true);
        typeCheckBoxes.values().forEach(cb -> cb.setSelected(true));
        sortCombo.setValue("Popularité");
        applyFilters();
    }

    @FXML
    private void refreshActivites() {
        loadActivitesFromDatabase();
        applyFilters();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Actualisation");
        alert.setHeaderText(null);
        alert.setContentText("Catalogue actualisé depuis la base de données !");
        alert.showAndWait();
    }

    @FXML
    private void goToAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/dashboard/homeClient.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) activitiesFlowPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Accueil");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de retourner à l'accueil: " + e.getMessage());
        }
    }

    @FXML
    private void goToProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/front/userProfil.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) activitiesFlowPane.getScene().getWindow();
            Scene scene = new Scene(root);
            String css = getClass().getResource("/user/front/userProfil.css").toExternalForm();
            scene.getStylesheets().add(css);
            stage.setScene(scene);
            stage.setTitle("RE7LA Tunisie - Mon Profil");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir le profil: " + e.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void testVirtualTour() {
        Alert loading = new Alert(Alert.AlertType.INFORMATION);
        loading.setTitle("Génération...");
        loading.setHeaderText("🤖 Création de la visite virtuelle");
        loading.setContentText("Veuillez patienter 10 secondes...");
        loading.show();

        new Thread(() -> {
            try {
                VirtualTourServiceImpl service = new VirtualTourServiceImpl();
                VirtualTour tour = service.generateTour("Sidi Bou Said", "français");
                Platform.runLater(() -> {
                    loading.close();
                    Alert result = new Alert(Alert.AlertType.INFORMATION);
                    result.setTitle("✅ Visite Virtuelle");
                    result.setHeaderText("Sidi Bou Said");
                    result.setContentText(
                            "Photos : " + tour.getPhotoUrls().size() + "\n" +
                                    "Durée : " + tour.getDurationSeconds() + "s\n\n" +
                                    tour.getNarration().substring(0, 300) + "...");
                    result.showAndWait();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loading.close();
                    showError("Erreur", e.getMessage());
                });
            }
        }).start();
    }
}
package org.example.Controllers.activite;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.Entites.activite.Activite;
import org.example.Entites.activite.Planning;
import org.example.Entites.activite.Reservation;
import org.example.Entites.activite.VirtualTour;
import org.example.Entites.user.User;
import org.example.Services.activite.PlaningActiviteImpl;
import org.example.Services.activite.Planingactivite;
import org.example.Services.activite.VirtualTourServiceImpl;
import org.example.Services.activite.ReservationServiceImpl;
import org.example.Utils.UserSession;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ActiviteDetailsController {

    @FXML private Text activityIcon;
    @FXML private StackPane heroImagePane;
    @FXML private Label typeBadge;
    @FXML private Label statusBadge;
    @FXML private Text activityTitle;
    @FXML private Text activityLocation;
    @FXML private Text activityDescription;
    @FXML private Text activityPrice;
    @FXML private Text activityCapacity;
    @FXML private ComboBox<String> dateFilterCombo;
    @FXML private VBox planningsContainer;
    @FXML private VBox emptyPlanningsState;
    @FXML private VBox mainDetailsContainer;
    @FXML private WeatherMapController weatherMapWidgetController;

    private Activite currentActivite;
    private List<Planning> planningsList = new ArrayList<>();

    // Services
    private Planingactivite planningService;
    private ReservationServiceImpl reservationService;

    // Utilisateur connecté via JWT
    private User currentUser;

    public void setActivite(Activite activite) {
        this.currentActivite = activite;

        // Initialiser les services
        this.planningService = new PlaningActiviteImpl();
        this.reservationService = new ReservationServiceImpl();

        // Récupérer l'utilisateur connecté
        this.currentUser = UserSession.getInstance().getCurrentUser();

        displayActiviteInfo();
        loadPlanningsFromDatabase();
    }

    /**
     * ✅ Vérifie si l'utilisateur est authentifié
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

        return true;
    }

    /**
     * ✅ Redirige vers la page de login
     */
    private void redirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) activityTitle.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showVirtualTour() {
        if (!checkUserAuth()) return;

        if (currentActivite == null) {
            showError("Erreur", "Aucune activité sélectionnée");
            return;
        }

        String lieu = currentActivite.getLieu();

        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Génération en cours");
        loadingAlert.setHeaderText("🤖 Création de votre visite virtuelle");
        loadingAlert.setContentText(
                "Notre IA génère une visite immersive de " + lieu + "...\n\n" +
                        "⏳ Cela prendra environ 10 secondes"
        );
        loadingAlert.show();

        new Thread(() -> {
            try {
                VirtualTourServiceImpl service = new VirtualTourServiceImpl();
                VirtualTour tour = service.generateTour(lieu, "français");

                Platform.runLater(() -> {
                    loadingAlert.close();
                    openVirtualTourViewer(tour);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingAlert.close();
                    showError("Erreur", "Impossible de générer la visite : " + e.getMessage());
                });
            }
        }).start();
    }

    private void openVirtualTourViewer(VirtualTour tour) {
        try {
            // ✅ CORRECTION : Utiliser le bon chemin
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/activite/views/frontoffice/VirtualTourView.fxml"));

            if (loader.getLocation() == null) {
                throw new IOException("Fichier VirtualTourView.fxml introuvable");
            }

            Parent root = loader.load();
            VirtualTourController controller = loader.getController();
            controller.loadTour(tour);

            Stage stage = new Stage();
            stage.setTitle("RE7LA - Visite Virtuelle : " + tour.getLieu());
            stage.setScene(new Scene(root, 1200, 800));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir le visualiseur: " + e.getMessage());
        }
    }

    private void loadPlanningsFromDatabase() {
        try {
            planningsList.clear();

            List<Planning> plannings = planningService.getAvailablePlannings(currentActivite.getIdActivite());
            planningsList.addAll(plannings);

            setupDateFilter();
            displayPlannings(planningsList);

            System.out.println("✅ " + plannings.size() + " plannings chargés pour l'activité #" + currentActivite.getIdActivite());

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement des plannings");
            e.printStackTrace();
            showError("Erreur de chargement",
                    "Impossible de charger les plannings depuis la base de données.");
        }
    }

    private void displayActiviteInfo() {
        loadHeroImage(currentActivite);
        typeBadge.setText(currentActivite.getType());

        statusBadge.setText(currentActivite.getStatut());
        if (currentActivite.getStatut().equals("Disponible")) {
            statusBadge.getStyleClass().clear();
            statusBadge.getStyleClass().add("activity-status-available");
        } else {
            statusBadge.getStyleClass().clear();
            statusBadge.getStyleClass().add("activity-status-complet");
        }

        activityTitle.setText(currentActivite.getNomA());
        activityLocation.setText(currentActivite.getLieu());
        activityDescription.setText(currentActivite.getDescriptionA());
        activityPrice.setText(String.format("%.0f DT", currentActivite.getPrixParPersonne()));
        activityCapacity.setText(currentActivite.getCapaciteMax() + " personnes");

        if (weatherMapWidgetController != null) {
            System.out.println("✅ Chargement du widget météo pour : " + currentActivite.getLieu());
            weatherMapWidgetController.loadData(currentActivite);
        } else {
            System.err.println("⚠️ Le contrôleur météo n'est pas initialisé");
        }
    }

    private void setupDateFilter() {
        List<String> dates = planningsList.stream()
                .map(p -> p.getDatePlanning().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        dates.add(0, "Toutes les dates");
        dateFilterCombo.setItems(FXCollections.observableArrayList(dates));
        dateFilterCombo.setValue("Toutes les dates");

        dateFilterCombo.setOnAction(e -> {
            String selected = dateFilterCombo.getValue();
            if (selected.equals("Toutes les dates")) {
                displayPlannings(planningsList);
            } else {
                List<Planning> filtered = planningsList.stream()
                        .filter(p -> p.getDatePlanning().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")).equals(selected))
                        .collect(Collectors.toList());
                displayPlannings(filtered);
            }
        });
    }

    private void displayPlannings(List<Planning> plannings) {
        planningsContainer.getChildren().clear();
        planningsContainer.setSpacing(20);

        if (plannings.isEmpty()) {
            emptyPlanningsState.setVisible(true);
            emptyPlanningsState.setManaged(true);
            return;
        }

        emptyPlanningsState.setVisible(false);
        emptyPlanningsState.setManaged(false);

        for (Planning planning : plannings) {
            HBox planningCard = createPlanningCard(planning);
            planningsContainer.getChildren().add(planningCard);
        }
    }

    private HBox createPlanningCard(Planning planning) {
        HBox card = new HBox(25);
        card.getStyleClass().add("planning-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinHeight(140);
        card.setPrefHeight(140);

        VBox dateBox = new VBox(5);
        dateBox.getStyleClass().add("planning-date-box");
        dateBox.setAlignment(Pos.CENTER);
        dateBox.setMinWidth(120);
        dateBox.setPrefWidth(120);

        String dayOfMonth = planning.getDatePlanning().format(DateTimeFormatter.ofPattern("dd"));
        String monthYear = planning.getDatePlanning().format(DateTimeFormatter.ofPattern("MMM yyyy"));

        Text dateDay = new Text(dayOfMonth);
        dateDay.getStyleClass().add("planning-day");

        Text dateMonth = new Text(monthYear);
        dateMonth.getStyleClass().add("planning-month");

        dateBox.getChildren().addAll(dateDay, dateMonth);

        VBox infoSection = new VBox(12);
        infoSection.getStyleClass().add("planning-info");
        infoSection.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoSection, Priority.ALWAYS);

        String dayOfWeek = planning.getDatePlanning().format(DateTimeFormatter.ofPattern("EEEE"));
        Text dayText = new Text(dayOfWeek);
        dayText.getStyleClass().add("planning-date");

        HBox timeBox = new HBox(10);
        timeBox.setAlignment(Pos.CENTER_LEFT);
        Text timeIcon = new Text("🕐");
        timeIcon.setStyle("-fx-font-size: 20px;");
        Text timeText = new Text(String.format("%s - %s",
                planning.getHeureDebut().format(DateTimeFormatter.ofPattern("HH:mm")),
                planning.getHeureFin().format(DateTimeFormatter.ofPattern("HH:mm"))));
        timeText.getStyleClass().add("planning-time");
        timeBox.getChildren().addAll(timeIcon, timeText);

        HBox placesBox = new HBox(10);
        placesBox.setAlignment(Pos.CENTER_LEFT);
        Text placesIcon = new Text("👥");
        placesIcon.setStyle("-fx-font-size: 20px;");
        Text placesText = new Text(planning.getNbPlacesRestantes() + " places restantes");
        placesText.getStyleClass().add("planning-places");
        placesBox.getChildren().addAll(placesIcon, placesText);

        infoSection.getChildren().addAll(dayText, timeBox, placesBox);

        VBox actionSection = new VBox(15);
        actionSection.setAlignment(Pos.CENTER);
        actionSection.setMinWidth(180);
        actionSection.setPrefWidth(180);

        Button reserveBtn = new Button("Réserver");
        reserveBtn.getStyleClass().add("planning-reserve-btn");
        reserveBtn.setMaxWidth(Double.MAX_VALUE);

        if (planning.getNbPlacesRestantes() == 0 || !planning.getEtat().equals("Disponible")) {
            reserveBtn.setDisable(true);
            reserveBtn.setText("Complet");
            reserveBtn.setStyle("-fx-background-color: #95A5A6;");
        }

        reserveBtn.setOnAction(e -> handleReservation(planning));

        HBox availabilityBox = new HBox(8);
        availabilityBox.setAlignment(Pos.CENTER);

        Region indicator = new Region();
        indicator.setPrefSize(12, 12);
        indicator.setStyle("-fx-background-radius: 6;");

        Label availabilityLabel = new Label();
        availabilityLabel.getStyleClass().add("availability-indicator");

        if (planning.getNbPlacesRestantes() > 10) {
            indicator.setStyle("-fx-background-color: #27AE60; -fx-background-radius: 6;");
            availabilityLabel.setText("Très disponible");
            availabilityLabel.getStyleClass().add("availability-high");
        } else if (planning.getNbPlacesRestantes() > 5) {
            indicator.setStyle("-fx-background-color: #F39C12; -fx-background-radius: 6;");
            availabilityLabel.setText("Peu de places");
            availabilityLabel.getStyleClass().add("availability-medium");
        } else if (planning.getNbPlacesRestantes() > 0) {
            indicator.setStyle("-fx-background-color: #E74C3C; -fx-background-radius: 6;");
            availabilityLabel.setText("Dernières places");
            availabilityLabel.getStyleClass().add("availability-low");
        } else {
            indicator.setStyle("-fx-background-color: #95A5A6; -fx-background-radius: 6;");
            availabilityLabel.setText("Complet");
            availabilityLabel.setStyle("-fx-background-color: rgba(149,165,166,0.15); -fx-text-fill: #95A5A6;");
        }

        availabilityBox.getChildren().addAll(indicator, availabilityLabel);
        actionSection.getChildren().addAll(reserveBtn, availabilityBox);

        card.getChildren().addAll(dateBox, infoSection, actionSection);

        return card;
    }

    /**
     * ✅ Gère la réservation d'un planning
     */
    private void handleReservation(Planning planning) {
        // Vérifier si l'utilisateur est connecté
        if (!checkUserAuth()) return;

        // Vérifier si l'utilisateur a déjà réservé ce planning
        if (reservationService.hasAlreadyReserved(currentUser.getId(), planning.getIdPlanning())) {
            showError("Déjà réservé",
                    "Vous avez déjà une réservation confirmée pour ce créneau.\n" +
                            "Consultez 'Mes Réservations' pour la gérer.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de réservation");
        alert.setHeaderText("Réserver cette activité ?");
        alert.setContentText(String.format(
                "Utilisateur : %s %s\nActivité : %s\nDate : %s\nHoraire : %s - %s\nPrix : %.0f DT / personne\n\nConfirmer ?",
                currentUser.getPrenom(),
                currentUser.getNom(),
                currentActivite.getNomA(),
                planning.getDatePlanning().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                planning.getHeureDebut().format(DateTimeFormatter.ofPattern("HH:mm")),
                planning.getHeureFin().format(DateTimeFormatter.ofPattern("HH:mm")),
                currentActivite.getPrixParPersonne()
        ));

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Créer l'objet Réservation
                    Reservation reservation = new Reservation();
                    reservation.setUser(currentUser);
                    reservation.setPlanning(planning);
                    reservation.setStatut("CONFIRMEE");

                    // Appeler la méthode addReservation
                    boolean success = reservationService.addReservation(reservation);

                    if (success) {
                        showSuccess("Réservation confirmée !",
                                "Réservation enregistrée pour " +
                                        currentUser.getPrenom() + " " + currentUser.getNom() + ".");
                        // Recharger les plannings pour mettre à jour les places disponibles
                        loadPlanningsFromDatabase();
                    } else {
                        showError("Erreur de réservation",
                                "Impossible de confirmer votre réservation.\n" +
                                        "Il se peut qu'il n'y ait plus de places disponibles.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Erreur", "Une erreur est survenue lors de la réservation: " + e.getMessage());
                }
            }
        });
    }

    private void loadHeroImage(Activite activite) {
        if (heroImagePane == null) {
            if (activityIcon != null) activityIcon.setText(getIconForType(activite.getType()));
            return;
        }

        heroImagePane.getChildren().clear();
        String imagePath = activite.getImage();
        boolean loaded = false;

        if (imagePath != null && !imagePath.isBlank() && !imagePath.equals("default.jpg")) {
            File imgFile = new File("src/main/resources/images/" + imagePath);
            if (!imgFile.exists()) {
                imgFile = new File("src/main/resources/images/activites/"
                        + imagePath.replace("activites/", ""));
            }
            if (imgFile.exists()) {
                try {
                    Image img = new Image(imgFile.toURI().toString(), 350, 250, false, true, true);
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(350);
                    iv.setFitHeight(250);
                    iv.setPreserveRatio(false);

                    javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(350, 250);
                    clip.setArcWidth(24);
                    clip.setArcHeight(24);
                    iv.setClip(clip);

                    heroImagePane.getChildren().add(iv);

                    javafx.scene.shape.Rectangle overlay = new javafx.scene.shape.Rectangle(350, 250);
                    overlay.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.12));
                    heroImagePane.getChildren().add(overlay);
                    loaded = true;
                    System.out.println("✅ Image hero chargée : " + imgFile.getPath());
                } catch (Exception ex) {
                    System.err.println("⚠️ Impossible de charger l'image hero : " + ex.getMessage());
                }
            }
        }

        if (!loaded) {
            heroImagePane.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #1A1A2E, #0F3460);" +
                            " -fx-background-radius: 14;");
            Text icon = new Text(getIconForType(activite.getType()));
            icon.setStyle("-fx-font-size: 90px;" +
                    " -fx-effect: dropshadow(gaussian, rgba(232,201,126,0.5), 20, 0, 0, 0);");
            heroImagePane.getChildren().add(icon);
        }
    }

    private String getIconForType(String type) {
        switch (type) {
            case "Aventure": return "⛰️";
            case "Sport": return "🏊";
            case "Culture": return "🏛️";
            case "Détente": return "🧘";
            case "Excursion": return "🐎";
            default: return "🎯";
        }
    }

    @FXML
    private void goBack() {
        Stage stage = (Stage) activityTitle.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void openMesReservations() {
        if (!checkUserAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/frontoffice/MesReservations.fxml")
            );
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("RE7LA - Mes Réservations");
            stage.setScene(new Scene(root, 880, 680));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (Exception e) {
            System.err.println("❌ Erreur ouverture Mes Réservations : " + e.getMessage());
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir la page des réservations.");
        }
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
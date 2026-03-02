package org.example.Controllers.user;


import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.example.Entites.user.LocationInfo;
import org.example.Services.user.APIservices.GeolocationService;

public class LocationController {

    private HBox locationBox;
    private LocationInfo currentLocation;
    private ImageView flagImageView;
    private Label locationLabel;
    private Label ipLabel;

    public LocationController() {
        createLocationDisplay();
        updateLocation();
    }

    private void createLocationDisplay() {
        locationBox = new HBox(10);
        locationBox.setAlignment(Pos.CENTER_LEFT);
        locationBox.setPadding(new Insets(8, 15, 8, 15));
        locationBox.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 20; -fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 20;");

        // ImageView pour le drapeau
        flagImageView = new ImageView();
        flagImageView.setFitHeight(20);
        flagImageView.setFitWidth(24);
        flagImageView.setPreserveRatio(true);

        // Label pour la localisation
        locationLabel = new Label("Chargement...");
        locationLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        locationLabel.setTextFill(Color.WHITE);

        // Label pour l'IP
        ipLabel = new Label();
        ipLabel.setFont(Font.font("Arial", 10));
        ipLabel.setTextFill(Color.rgb(255, 255, 255, 0.7));

        VBox textBox = new VBox(2);
        textBox.setAlignment(Pos.CENTER_LEFT);
        textBox.getChildren().addAll(locationLabel, ipLabel);

        locationBox.getChildren().addAll(flagImageView, textBox);
    }

    private void updateLocation() {
        new Thread(() -> {
            try {
                currentLocation = GeolocationService.getCurrentLocation();

                javafx.application.Platform.runLater(() -> {
                    if (currentLocation != null) {
                        // Mettre à jour le drapeau
                        String flagUrl = currentLocation.getFlag();
                        if (flagUrl != null) {
                            try {
                                Image flagImage = new Image(flagUrl, true);
                                flagImageView.setImage(flagImage);
                            } catch (Exception e) {
                                System.err.println("❌ Erreur chargement drapeau: " + e.getMessage());
                            }
                        }

                        // Mettre à jour le texte
                        String location = currentLocation.getFormattedLocation();
                        locationLabel.setText(location.isEmpty() ? "Localisation inconnue" : location);

                        String ip = currentLocation.getIp();
                        ipLabel.setText("IP: " + (ip != null ? ip : "0.0.0.0"));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public HBox getView() {
        return locationBox;
    }

    public void refresh() {
        updateLocation();
    }

    public LocationInfo getCurrentLocation() {
        return currentLocation;
    }
}
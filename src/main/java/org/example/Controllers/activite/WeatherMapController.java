package org.example.Controllers.activite;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import org.example.Entites.activite.Activite;
import org.example.Services.activite.MapsServiceImpl;
import org.example.Services.activite.WeatherServiceImpl;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour le widget Météo + Carte (Leaflet.js interactif)
 */
public class WeatherMapController implements Initializable {

    @FXML private ImageView weatherIcon;
    @FXML private Label locationLabel;
    @FXML private Label conditionLabel;
    @FXML private Label temperatureLabel;
    @FXML private Label feelsLikeLabel;
    @FXML private Label humidityLabel;
    @FXML private Label windLabel;
    @FXML private WebView mapWebView;

    @FXML private HBox recommendationBox;
    @FXML private Label recommendationIcon;
    @FXML private Label recommendationLabel;

    @FXML private Label addressLabel;

    @FXML private Button btnDirections;
    @FXML private Button btnOpenMaps;
    @FXML private Button btnRefresh;

    private WeatherServiceImpl weatherService;
    private MapsServiceImpl mapsService;
    private Activite currentActivite;
    private MapsServiceImpl.Location location;

    // Mode courant de la carte
    private enum MapMode { LOCATION, ROUTE }
    private MapMode currentMapMode = MapMode.LOCATION;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        weatherService = new WeatherServiceImpl();
        mapsService = new MapsServiceImpl();

        // Activer JavaScript dans la WebView
        mapWebView.getEngine().setJavaScriptEnabled(true);
    }

    /**
     * Charge les données pour une activité
     */
    public void loadData(Activite activite) {
        this.currentActivite = activite;
        System.out.println("Chargement météo + carte pour : " + activite.getLieu());

        // Afficher la carte de chargement
        Platform.runLater(this::showLoadingMap);

        new Thread(this::loadWeather).start();
    }

    /**
     * Charge météo + géoloc
     */
    private void loadWeather() {
        try {
            location = mapsService.geocode(currentActivite.getLieu());

            if (location != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();

                // Météo
                WeatherServiceImpl.WeatherData weather =
                        weatherService.getCurrentWeatherByCoords(lat, lon);

                if (weather != null) {
                    Platform.runLater(() -> displayWeather(weather));

                    WeatherServiceImpl.WeatherRecommendation rec =
                            weatherService.getRecommendationByCoords(lat, lon, currentActivite.getType());
                    if (rec != null) {
                        Platform.runLater(() -> displayRecommendation(rec));
                    }
                }

                Platform.runLater(() -> {
                    addressLabel.setText(location.getDisplayName());
                    showLocationMap(lat, lon);
                });
            }

        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
            Platform.runLater(() -> showErrorMap("Localisation introuvable"));
        }
    }

    /**
     * Affiche les données météo dans l'UI
     */
    private void displayWeather(WeatherServiceImpl.WeatherData weather) {
        locationLabel.setText(weather.getCityName());
        conditionLabel.setText(weather.getCondition());
        temperatureLabel.setText(String.format("%.0f°C", weather.getTemperature()));
        feelsLikeLabel.setText(String.format("Ressenti %.0f°C", weather.getFeelsLike()));
        humidityLabel.setText(weather.getHumidity() + "%");
        windLabel.setText(String.format("%.0f km/h", weather.getWindSpeed() * 3.6));

        try {
            Image icon = new Image(weather.getIconUrl(), true);
            weatherIcon.setImage(icon);
        } catch (Exception e) {
            System.err.println("Icône météo non chargée");
        }
    }

    /**
     * Affiche la recommandation météo
     */
    private void displayRecommendation(WeatherServiceImpl.WeatherRecommendation rec) {
        recommendationBox.setVisible(true);
        recommendationBox.setManaged(true);
        recommendationIcon.setText(rec.isRecommended() ? "OK" : "ATTENTION");
        recommendationLabel.setText(rec.getReason());

        if (rec.isRecommended()) {
            recommendationIcon.setStyle("-fx-text-fill: #34D399; -fx-font-weight: bold; -fx-font-size: 10px;");
        } else {
            recommendationIcon.setStyle("-fx-text-fill: #F87171; -fx-font-weight: bold; -fx-font-size: 10px;");
        }
    }

    // ════════════════════════════════════════════════
    // GESTION DE LA CARTE
    // ════════════════════════════════════════════════

    /**
     * Affiche la carte de chargement
     */
    private void showLoadingMap() {
        mapWebView.getEngine().loadContent(buildLoadingHTML());
    }

    /**
     * Affiche la carte Leaflet centrée sur la localisation
     */
    private void showLocationMap(double lat, double lon) {
        currentMapMode = MapMode.LOCATION;
        mapWebView.getEngine().loadContent(buildLeafletHTML(lat, lon, false));
    }

    /**
     * Affiche l'itinéraire depuis Tunis vers la destination (dans la page)
     */
    private void showRouteMap(double destLat, double destLon) {
        currentMapMode = MapMode.ROUTE;
        mapWebView.getEngine().loadContent(buildLeafletHTML(destLat, destLon, true));
    }

    /**
     * Affiche une carte d'erreur
     */
    private void showErrorMap(String message) {
        mapWebView.getEngine().loadContent(buildErrorHTML(message));
    }

    // ════════════════════════════════════════════════
    // GÉNÉRATION DU HTML CARTE (sans Leaflet)
    // ════════════════════════════════════════════════

    /**
     * Génère le HTML avec iframe OpenStreetMap ou Google Maps Directions
     */
    private String buildLeafletHTML(double lat, double lon, boolean showRoute) {
        String cityName = currentActivite != null ? currentActivite.getLieu() : "Destination";

        // ── Mode itinéraire : Google Maps Directions embed (Tunis → destination)
        if (showRoute) {
            String iframeSrc = "https://www.google.com/maps/embed/v1/directions"
                    + "?key=AIzaSyD-9tSrke72PouQMnMX-a7eZSW0jkFMBWY"
                    + "&origin=Tunis,Tunisia"
                    + "&destination=" + lat + "," + lon
                    + "&mode=driving";

            // Fallback sans clé API : URL de directions classique dans un iframe
            String fallbackSrc = "https://maps.google.com/maps?saddr=Tunis,Tunisia"
                    + "&daddr=" + lat + "," + lon
                    + "&output=embed";

            return "<!DOCTYPE html>\n"
                    + "<html><head><meta charset=\"utf-8\">\n"
                    + "<style>*{margin:0;padding:0}html,body,iframe{width:100%;height:100%;border:0;display:block}</style>\n"
                    + "</head><body>\n"
                    + "<iframe src=\"" + fallbackSrc + "\" width=\"100%\" height=\"100%\" "
                    + "frameborder=\"0\" style=\"border:0;width:100%;height:100%;\" allowfullscreen></iframe>\n"
                    + "</body></html>";
        }

        // ── Mode localisation : OpenStreetMap iframe natif (aucune clé, toujours plein écran)
        String osmSrc = "https://www.openstreetmap.org/export/embed.html"
                + "?bbox=" + (lon - 0.05) + "%2C" + (lat - 0.05)
                + "%2C" + (lon + 0.05) + "%2C" + (lat + 0.05)
                + "&layer=mapnik"
                + "&marker=" + lat + "%2C" + lon;

        return "<!DOCTYPE html>\n"
                + "<html><head><meta charset=\"utf-8\">\n"
                + "<style>\n"
                + "* { margin:0; padding:0; }\n"
                + "html, body { width:100%; height:100%; overflow:hidden; }\n"
                + "iframe { width:100%; height:100%; border:0; display:block; }\n"
                + ".label {\n"
                + "  position:fixed; top:8px; left:50%; transform:translateX(-50%);\n"
                + "  background:rgba(15,23,42,0.92); color:#818CF8;\n"
                + "  font:700 11px sans-serif; padding:5px 16px;\n"
                + "  border-radius:20px; border:1px solid rgba(99,102,241,0.5);\n"
                + "  z-index:9999; pointer-events:none; white-space:nowrap;\n"
                + "}\n"
                + "</style>\n"
                + "</head><body>\n"
                + "<div class=\"label\">" + cityName + "</div>\n"
                + "<iframe src=\"" + osmSrc + "\" allowfullscreen></iframe>\n"
                + "</body></html>";
    }

    /**
     * HTML de chargement
     */
    private String buildLoadingHTML() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    * { margin:0; padding:0; }
                    body {
                        background: #1E293B;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        height: 220px;
                        font-family: 'Segoe UI', sans-serif;
                    }
                    .loader {
                        text-align: center;
                        color: #64748B;
                    }
                    .spinner {
                        width: 32px; height: 32px;
                        border: 3px solid rgba(99,102,241,0.2);
                        border-top-color: #6366F1;
                        border-radius: 50%;
                        animation: spin 1s linear infinite;
                        margin: 0 auto 12px;
                    }
                    @keyframes spin { to { transform: rotate(360deg); } }
                    .text { font-size: 12px; color: #64748B; }
                </style>
            </head>
            <body>
                <div class="loader">
                    <div class="spinner"></div>
                    <div class="text">Geolocalisation...</div>
                </div>
            </body>
            </html>
            """;
    }

    /**
     * HTML d'erreur
     */
    private String buildErrorHTML(String message) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    * { margin:0; padding:0; }
                    body {
                        background: #1E293B;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        height: 220px;
                        font-family: 'Segoe UI', sans-serif;
                        text-align: center;
                        color: #F87171;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div>
                    <div style="font-size:24px;margin-bottom:8px;">⚠</div>
                    <div>%s</div>
                </div>
            </body>
            </html>
            """, message);
    }

    // ════════════════════════════════════════════════
    // ACTIONS BOUTONS
    // ════════════════════════════════════════════════

    /**
     * Bouton Itinéraire → affiche la route dans la WebView
     */
    @FXML
    private void openDirections() {
        if (location == null) {
            showAlert("Localisation non disponible. Attendez le chargement.");
            return;
        }

        if (currentMapMode == MapMode.ROUTE) {
            // Basculer vers vue localisation
            showLocationMap(location.getLatitude(), location.getLongitude());
            btnDirections.setText("Itineraire");
        } else {
            // Afficher l'itinéraire dans la carte
            showRouteMap(location.getLatitude(), location.getLongitude());
            btnDirections.setText("Vue normale");
        }
    }

    /**
     * Bouton Ouvrir Maps → ouvre dans le navigateur
     */
    @FXML
    private void openInMaps() {
        if (location == null) {
            showAlert("Localisation non disponible.");
            return;
        }

        try {
            String url = String.format(
                    "https://www.google.com/maps/search/?api=1&query=%f,%f",
                    location.getLatitude(), location.getLongitude()
            );
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            showAlert("Impossible d'ouvrir Maps : " + e.getMessage());
        }
    }

    /**
     * Rafraîchit les données
     */
    @FXML
    private void refresh() {
        if (currentActivite != null) {
            btnDirections.setText("Itineraire");
            currentMapMode = MapMode.LOCATION;
            loadData(currentActivite);
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
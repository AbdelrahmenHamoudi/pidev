package org.example.Controllers.hebergement.front;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * ============================================================
 *  WeatherController — Météo Avancée RE7LA
 * ============================================================
 *  APIs utilisées :
 *   - Open-Meteo          (météo 14j + UV + vent, GRATUIT, sans clé)
 *   - Nominatim OSM       (géocodage ville → lat/lon, GRATUIT)
 *   - Open-Meteo Marine   (vagues, temp eau, GRATUIT)
 *   - Anthropic Claude    (résumé IA personnalisé)
 * ============================================================
 */
public class WeatherController implements Initializable {

    // ── Nœuds FXML ───────────────────────────────────────────
    @FXML private Label             lblVille;
    @FXML private Label             lblTypeHeberg;
    @FXML private Label             lblCoords;
    @FXML private Label             lblTempActuelle;
    @FXML private Label             lblCondition;
    @FXML private Label             lblUV;
    @FXML private Label             lblVent;
    @FXML private Label             lblHumidite;
    @FXML private Label             lblVisibilite;
    @FXML private HBox              alerteUVBox;
    @FXML private Label             alerteUVLabel;
    @FXML private HBox              alerteMerBox;
    @FXML private Label             alerteMerLabel;
    @FXML private HBox              bonTempsBox;
    @FXML private Label             bonTempsLabel;
    @FXML private VBox              marineSection;
    @FXML private Label             lblVagues;
    @FXML private Label             lblTempEau;
    @FXML private Label             lblRisqueBaignade;
    @FXML private Label             lblEtatMer;
    @FXML private HBox              forecastContainer;
    @FXML private Label             lblIASummary;
    @FXML private VBox              iaSummaryBox;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private VBox              contentArea;

    // ── Données ──────────────────────────────────────────────
    private String ville;
    private String typeHebergement;
    private double latitude;
    private double longitude;

    private final HttpClient   httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper     = new ObjectMapper();

    // 🔑 Clé Groq — GRATUITE sur https://console.groq.com
    //    1. Créer un compte → API Keys → Create API Key
    //    2. Copier la clé ici (commence par "gsk_...")
    private static final String GROQ_API_KEY = "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";

    // Types côtiers → affichage section marine
    private static final Set<String> TYPES_COTIERS = Set.of(
            "villa", "bungalow", "resort", "hotel", "hôtel",
            "auberge", "hostel", "apart", "maison"
    );

    @Override
    public void initialize(java.net.URL location, ResourceBundle resources) {
        // Initialisation réelle via setHebergementInfo()
    }

    // =========================================================
    // POINT D'ENTRÉE — appelé depuis ReservationController
    // =========================================================

    public void setHebergementInfo(String ville, String typeHebergement) {
        this.ville           = ville;
        this.typeHebergement = typeHebergement;
        if (lblVille      != null) lblVille.setText("📍 " + ville);
        if (lblTypeHeberg != null) lblTypeHeberg.setText(resolveTypeIcon(typeHebergement) + " " + typeHebergement);
        showLoading(true);
        loadWeatherAsync();
    }

    // =========================================================
    // CHARGEMENT ASYNCHRONE
    // =========================================================

    private void loadWeatherAsync() {
        CompletableFuture
                .supplyAsync(this::geocodeVille)
                .thenAccept(coords -> {
                    if (coords == null) {
                        Platform.runLater(() -> showError("Ville introuvable : " + ville));
                        return;
                    }
                    this.latitude  = coords[0];
                    this.longitude = coords[1];

                    CompletableFuture<JsonNode> wf = CompletableFuture.supplyAsync(this::fetchWeather);
                    CompletableFuture<JsonNode> mf = CompletableFuture.supplyAsync(this::fetchMarine);

                    CompletableFuture.allOf(wf, mf).thenRun(() -> {
                        try {
                            JsonNode wd = wf.get();
                            JsonNode md = mf.get();
                            Platform.runLater(() -> {
                                if (wd != null) {
                                    displayWeather(wd, md);
                                    generateAISummary(wd, md);
                                } else {
                                    showError("Impossible de récupérer les données météo.");
                                }
                                showLoading(false);
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            Platform.runLater(() -> { showError(e.getMessage()); showLoading(false); });
                        }
                    }).exceptionally(ex -> {
                        ex.printStackTrace();
                        Platform.runLater(() -> { showError(ex.getMessage()); showLoading(false); });
                        return null;
                    });
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    Platform.runLater(() -> { showError(ex.getMessage()); showLoading(false); });
                    return null;
                });
    }

    // =========================================================
    // GÉOCODAGE — Nominatim
    // =========================================================

    /**
     * Extrait le nom de ville depuis le titre de l'hébergement.
     * Ex: "Villa Hammamet" → "Hammamet", "Appart Sousse Centre" → "Sousse Centre"
     */
    private String extractCityFromTitre(String titre) {
        if (titre == null || titre.isBlank()) return "";
        String[] prefixes = {"villa", "hôtel", "hotel", "appartement", "appart",
                "maison", "resort", "bungalow", "riad", "hostel",
                "auberge", "studio", "duplex", "suite", "chambre",
                "logement", "chez"};
        String cleaned = titre.trim();
        for (String kw : prefixes) {
            cleaned = cleaned.replaceAll("(?i)^" + kw + "\\s+", "").trim();
        }
        return cleaned.isEmpty() ? titre.trim() : cleaned;
    }

    private double[] geocodeVille() {
        // Essai 1 : nom extrait du titre (ex: "Villa Hammamet" → "Hammamet")
        String cityGuess = extractCityFromTitre(this.ville);
        double[] result  = tryGeocode(cityGuess);
        // Essai 2 : titre complet si l'extraction ne marche pas
        if (result == null && !cityGuess.equalsIgnoreCase(this.ville)) {
            result = tryGeocode(this.ville);
        }
        return result;
    }

    private double[] tryGeocode(String name) {
        try {
            String encoded = URLEncoder.encode(name + ", Tunisie", StandardCharsets.UTF_8);
            String url     = "https://nominatim.openstreetmap.org/search?q="
                    + encoded + "&format=json&limit=1";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "RE7LA-JavaFX/1.0")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode arr = mapper.readTree(resp.body());
            if (arr.isArray() && arr.size() > 0) {
                // Affiche le nom de ville réel trouvé
                String displayName = arr.get(0).has("display_name")
                        ? arr.get(0).get("display_name").asText().split(",")[0].trim()
                        : name;
                Platform.runLater(() -> {
                    if (lblVille != null) lblVille.setText("📍 " + displayName);
                });
                return new double[]{
                        arr.get(0).get("lat").asDouble(),
                        arr.get(0).get("lon").asDouble()
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // =========================================================
    // MÉTÉO — Open-Meteo
    // =========================================================

    private JsonNode fetchWeather() {
        try {
            // ⚠️ Ne pas utiliser String.format pour le slash : %%2F → %2F cassé côté API
            String url = "https://api.open-meteo.com/v1/forecast?"
                    + "latitude=" + String.format(java.util.Locale.US, "%.4f", latitude)
                    + "&longitude=" + String.format(java.util.Locale.US, "%.4f", longitude)
                    // uv_index n'est PAS dans "current" → on lit uv_index_max depuis "daily"
                    + "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weathercode"
                    + "&daily=weathercode,temperature_2m_max,temperature_2m_min,"
                    + "uv_index_max,wind_speed_10m_max,precipitation_probability_max"
                    + "&timezone=Africa/Tunis"
                    + "&forecast_days=14";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(15))
                    .GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode node = mapper.readTree(resp.body());
            if (node.has("error") && node.get("error").asBoolean()) {
                System.err.println("Open-Meteo error: " + node.get("reason").asText());
                return null;
            }
            return node;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    // =========================================================
    // CONDITIONS MARINES — Open-Meteo Marine
    // =========================================================

    private JsonNode fetchMarine() {
        try {
            // Demande current + hourly comme fallback si current est null
            String url = "https://marine-api.open-meteo.com/v1/marine?"
                    + "latitude=" + String.format(java.util.Locale.US, "%.4f", latitude)
                    + "&longitude=" + String.format(java.util.Locale.US, "%.4f", longitude)
                    + "&current=wave_height,sea_surface_temperature,ocean_current_speed"
                    + "&hourly=wave_height,sea_surface_temperature"
                    + "&timezone=Africa/Tunis"
                    + "&forecast_days=1";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(15))
                    .GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode node = mapper.readTree(resp.body());
            System.out.println("Marine API response: " + resp.body().substring(0, Math.min(300, resp.body().length())));
            return node;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // =========================================================
    // AFFICHAGE MÉTÉO
    // =========================================================

    private void displayWeather(JsonNode weather, JsonNode marine) {
        JsonNode current = weather.get("current");
        JsonNode daily   = weather.get("daily");

        if (lblCoords != null)
            lblCoords.setText(String.format("%.3f°N, %.3f°E", latitude, longitude));

        double temp    = current.get("temperature_2m").asDouble();
        int    wCode   = current.get("weathercode").asInt();
        // uv_index n'est pas dans "current" → on lit uv_index_max du jour J (daily[0])
        double uvIndex = 0;
        if (daily != null && daily.has("uv_index_max") && daily.get("uv_index_max").size() > 0
                && !daily.get("uv_index_max").get(0).isNull()) {
            uvIndex = daily.get("uv_index_max").get(0).asDouble();
        }
        double wind    = current.get("wind_speed_10m").asDouble();
        int    hum     = current.get("relative_humidity_2m").asInt();

        if (lblTempActuelle != null) {
            lblTempActuelle.setText(String.format("%.0f°C", temp));
            String c = temp >= 30 ? "#E74C3C" : temp >= 20 ? "#F39C12" : "#3498DB";
            lblTempActuelle.setStyle("-fx-font-size:52px;-fx-font-weight:bold;-fx-text-fill:" + c + ";");
        }
        if (lblCondition != null) lblCondition.setText(weatherCodeToFrench(wCode));
        if (lblUV        != null) lblUV.setText(uvLabel(uvIndex));
        if (lblVent      != null) lblVent.setText(String.format("%.0f km/h", wind));
        if (lblHumidite  != null) lblHumidite.setText(hum + "%");

        // ── Alertes ────────────────────────────────────────────
        hideAllAlerts();

        if (uvIndex >= 7) {
            showHBox(alerteUVBox);
            if (alerteUVLabel != null)
                alerteUVLabel.setText("Indice UV élevé (" + (int)uvIndex + "/11) — "
                        + "Crème solaire indispensable, évitez l'exposition entre 11h et 16h.");
        }

        if (isBonTemps(wCode) && temp >= 22) {
            showHBox(bonTempsBox);
            if (bonTempsLabel != null)
                bonTempsLabel.setText("Conditions idéales ! Profitez de la plage, "
                        + "des randonnées et excursions.");
        }

        // ── Prévisions 14 jours ────────────────────────────────
        if (forecastContainer != null && daily != null) {
            forecastContainer.getChildren().clear();
            JsonNode dates   = daily.get("time");
            JsonNode maxTemp = daily.get("temperature_2m_max");
            JsonNode minTemp = daily.get("temperature_2m_min");
            JsonNode codes   = daily.get("weathercode");
            JsonNode uvMaxes = daily.get("uv_index_max");
            JsonNode precip  = daily.get("precipitation_probability_max");

            for (int i = 0; i < Math.min(14, dates.size()); i++) {
                forecastContainer.getChildren().add(createForecastCard(
                        dates.get(i).asText(),
                        codes.get(i).asInt(),
                        maxTemp.get(i).asDouble(),
                        minTemp.get(i).asDouble(),
                        uvMaxes != null && !uvMaxes.get(i).isNull() ? uvMaxes.get(i).asDouble() : 0,
                        precip  != null && !precip.get(i).isNull()  ? precip.get(i).asInt()    : 0
                ));
            }
        }

        // ── Section marine ─────────────────────────────────────
        if (marineSection != null) {
            boolean cotier = isCotier(typeHebergement);
            marineSection.setVisible(cotier && marine != null);
            marineSection.setManaged(cotier && marine != null);
            if (cotier && marine != null) displayMarine(marine);
        }
    }

    // =========================================================
    // AFFICHAGE CONDITIONS MARINES
    // =========================================================

    private void displayMarine(JsonNode marine) {
        JsonNode cur    = marine.has("current") ? marine.get("current") : null;
        JsonNode hourly = marine.has("hourly")  ? marine.get("hourly")  : null;

        // Lecture wave_height : current d'abord, puis hourly[0] si absent/nul
        double waveH = 0;
        if (cur != null && cur.has("wave_height") && !cur.get("wave_height").isNull())
            waveH = cur.get("wave_height").asDouble();
        else if (hourly != null && hourly.has("wave_height") && hourly.get("wave_height").size() > 0
                && !hourly.get("wave_height").get(0).isNull())
            waveH = hourly.get("wave_height").get(0).asDouble();

        // Lecture sea_surface_temperature : current d'abord, puis hourly[0]
        double seaTemp = 0;
        if (cur != null && cur.has("sea_surface_temperature") && !cur.get("sea_surface_temperature").isNull())
            seaTemp = cur.get("sea_surface_temperature").asDouble();
        else if (hourly != null && hourly.has("sea_surface_temperature") && hourly.get("sea_surface_temperature").size() > 0
                && !hourly.get("sea_surface_temperature").get(0).isNull())
            seaTemp = hourly.get("sea_surface_temperature").get(0).asDouble();

        System.out.println("Marine data: waveH=" + waveH + ", seaTemp=" + seaTemp);

        if (lblVagues         != null) lblVagues.setText(String.format("%.1f m", waveH));
        if (lblTempEau        != null) lblTempEau.setText(String.format("%.0f°C", seaTemp));
        if (lblEtatMer        != null) lblEtatMer.setText(etatMer(waveH));
        if (lblRisqueBaignade != null) lblRisqueBaignade.setText(risqueBaignade(waveH));

        if (waveH > 1.5) {
            showHBox(alerteMerBox);
            if (alerteMerLabel != null)
                alerteMerLabel.setText("Mer agitée (" + String.format("%.1f", waveH)
                        + " m) — Baignade et activités nautiques déconseillées.");
        }
    }

    // =========================================================
    // CARTE PRÉVISION JOURNALIÈRE
    // =========================================================

    private VBox createForecastCard(String date, int code, double max, double min,
                                    double uv, int precip) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12, 10, 12, 10));
        card.setPrefWidth(88);
        card.setMinWidth(88);
        card.setMaxWidth(88);
        card.getStyleClass().add("forecast-card");

        LocalDate d     = LocalDate.parse(date);
        boolean today   = d.equals(LocalDate.now());
        String dayName  = today ? "Auj." : d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
        String dayNum   = d.getDayOfMonth() + " " + d.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH);

        Label lblDay  = new Label(dayName);
        lblDay.setStyle(today ? "-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#F39C12;"
                : "-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#2C3E50;");
        Label lblDate = new Label(dayNum);
        lblDate.setStyle("-fx-font-size:10px;-fx-text-fill:#94A3B8;");
        Label lblIcon = new Label(weatherIcon(code));
        lblIcon.setStyle("-fx-font-size:26px;");
        Label lblMax  = new Label(String.format("%.0f°", max));
        lblMax.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#E74C3C;");
        Label lblMin  = new Label(String.format("%.0f°", min));
        lblMin.setStyle("-fx-font-size:12px;-fx-text-fill:#3498DB;");
        Label lblPrecip = new Label("💧 " + precip + "%");
        lblPrecip.setStyle("-fx-font-size:10px;-fx-text-fill:" + (precip > 60 ? "#3498DB" : "#94A3B8") + ";");

        card.getChildren().addAll(lblDay, lblDate, lblIcon, lblMax, lblMin, buildUvBar(uv), lblPrecip);

        if (today)
            card.setStyle("-fx-background-color:linear-gradient(to bottom,#FFF3E0,#FFECC7);"
                    + "-fx-background-radius:14;-fx-border-color:#F39C12;"
                    + "-fx-border-radius:14;-fx-border-width:2;");
        return card;
    }

    private HBox buildUvBar(double uv) {
        HBox box = new HBox(3);
        box.setAlignment(Pos.CENTER);
        String color = uv <= 2 ? "#27AE60" : uv <= 5 ? "#F39C12" : uv <= 7 ? "#E67E22" : "#E74C3C";
        Label uvLbl = new Label("UV");
        uvLbl.setStyle("-fx-font-size:9px;-fx-text-fill:#94A3B8;");
        Rectangle bar = new Rectangle(38, 5);
        bar.setArcWidth(5); bar.setArcHeight(5);
        bar.setFill(Color.web(color));
        Label uvVal = new Label(String.format("%.0f", uv));
        uvVal.setStyle("-fx-font-size:9px;-fx-text-fill:" + color + ";-fx-font-weight:bold;");
        box.getChildren().addAll(uvLbl, bar, uvVal);
        return box;
    }

    // =========================================================
    // RÉSUMÉ IA — Anthropic Claude
    // =========================================================

    private void generateAISummary(JsonNode weather, JsonNode marine) {
        // Clé non configurée → message explicatif
        if (GROQ_API_KEY == null || GROQ_API_KEY.equals("YOUR_GROQ_API_KEY") || GROQ_API_KEY.isBlank()) {
            Platform.runLater(() -> {
                if (iaSummaryBox != null) { iaSummaryBox.setVisible(true); iaSummaryBox.setManaged(true); }
                if (lblIASummary != null)
                    lblIASummary.setText(
                            "🔑 Pour activer le résumé IA gratuit :\n"
                                    + "1. Allez sur console.groq.com\n"
                                    + "2. Créez un compte gratuit\n"
                                    + "3. API Keys → Create Key (commence par gsk_...)\n"
                                    + "4. Collez la clé ligne 80 dans WeatherController.java"
                    );
            });
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                JsonNode cur   = weather.get("current");
                JsonNode daily = weather.get("daily");

                double temp = cur.get("temperature_2m").asDouble();
                int  wCode  = cur.get("weathercode").asInt();
                double wind = cur.get("wind_speed_10m").asDouble();

                // UV depuis daily[0]
                double uv = 0;
                if (daily != null && daily.has("uv_index_max")
                        && daily.get("uv_index_max").size() > 0
                        && !daily.get("uv_index_max").get(0).isNull()) {
                    uv = daily.get("uv_index_max").get(0).asDouble();
                }

                // Infos marines
                StringBuilder marineInfo = new StringBuilder();
                if (marine != null && marine.has("current")) {
                    JsonNode mc = marine.get("current");
                    if (mc.has("wave_height"))
                        marineInfo.append(" Vagues: ")
                                .append(String.format(java.util.Locale.US, "%.1f", mc.get("wave_height").asDouble()))
                                .append("m.");
                    if (mc.has("sea_surface_temperature"))
                        marineInfo.append(" Temp eau: ")
                                .append((int) mc.get("sea_surface_temperature").asDouble()).append("C.");
                }

                // Prompt - construction propre
                String villeClean = ville.replace(Character.toString((char)34), "")
                        .replace(Character.toString((char)39), "")
                        .replace(Character.toString((char)92), "");
                String typeClean = typeHebergement.replace(Character.toString((char)34), "")
                        .replace(Character.toString((char)39), "")
                        .replace(Character.toString((char)92), "");
                String meteoClean = weatherCodeToFrench(wCode).replaceAll("[^a-zA-Z0-9 ]", "");
                String prompt = "Tu es un assistant meteo pour touristes en Tunisie. "
                        + "Genere un resume meteo chaleureux en 2-3 phrases "
                        + "pour un voyageur a " + villeClean + " dans un " + typeClean + ". "
                        + "Donnees: temp=" + (int) temp + "C, meteo=" + meteoClean
                        + ", UV=" + (int) uv + "/11, vent=" + (int) wind + "km/h." + marineInfo
                        + " Conseils concrets, ton positif, francais, 3 phrases max.";

                // ── Build JSON body via Jackson (sûr, pas de concaténation) ──
                ObjectMapper om = new ObjectMapper();
                com.fasterxml.jackson.databind.node.ObjectNode root = om.createObjectNode();
                root.put("model", "llama3-8b-8192");   // modèle Groq gratuit rapide
                root.put("max_tokens", 250);
                root.put("temperature", 0.7);

                com.fasterxml.jackson.databind.node.ArrayNode messages = root.putArray("messages");

                com.fasterxml.jackson.databind.node.ObjectNode sysMsg = messages.addObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", "Tu es un assistant météo sympa pour touristes en Tunisie. Réponds toujours en français.");

                com.fasterxml.jackson.databind.node.ObjectNode userMsg = messages.addObject();
                userMsg.put("role", "user");
                userMsg.put("content", prompt);

                String body = om.writeValueAsString(root);

                // ── Requête HTTP vers Groq ──
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                        .header("Content-Type",  "application/json")
                        .header("Authorization", "Bearer " + GROQ_API_KEY)
                        .timeout(java.time.Duration.ofSeconds(20))
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println("Groq status: " + resp.statusCode());

                JsonNode result = mapper.readTree(resp.body());

                // Réponse format OpenAI : choices[0].message.content
                if (result.has("choices") && result.get("choices").size() > 0) {
                    return result.get("choices").get(0).get("message").get("content").asText().trim();
                }
                if (result.has("error"))
                    System.err.println("Groq error: " + result.get("error").get("message").asText());

            } catch (Exception e) { e.printStackTrace(); }
            return null;

        }).thenAccept(summary -> Platform.runLater(() -> {
            if (iaSummaryBox != null) { iaSummaryBox.setVisible(true); iaSummaryBox.setManaged(true); }
            if (lblIASummary != null)
                lblIASummary.setText(summary != null ? "💬 " + summary : "💬 Résumé IA indisponible.");
        }));
    }


    // =========================================================
    // FERMER
    // =========================================================

    @FXML
    private void fermer() {
        if (contentArea != null) {
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.close();
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private boolean isCotier(String type) {
        if (type == null) return false;
        String t = type.toLowerCase();
        return TYPES_COTIERS.stream().anyMatch(t::contains);
    }

    private boolean isBonTemps(int code) { return code == 0 || code == 1 || code == 2; }

    private String uvLabel(double uv) {
        String n = uv <= 2 ? "Faible" : uv <= 5 ? "Modéré" : uv <= 7 ? "Élevé"
                : uv <= 10 ? "Très élevé" : "Extrême";
        return String.format("%.0f/11 — %s", uv, n);
    }

    private String etatMer(double h) {
        if (h <= 0.1) return "🪞 Mer d'huile";
        if (h <= 0.5) return "😊 Belle";
        if (h <= 1.25) return "🌊 Peu agitée";
        if (h <= 2.5) return "⚠ Agitée";
        return "🔴 Très agitée";
    }

    private String risqueBaignade(double h) {
        if (h <= 0.5) return "✅ Faible — Sûre";
        if (h <= 1.2) return "🟡 Modéré — Prudence";
        return "🔴 Élevé — Déconseillée";
    }

    private String weatherCodeToFrench(int code) {
        return switch (code) {
            case 0      -> "☀ Ensoleillé";
            case 1      -> "🌤 Principalement dégagé";
            case 2      -> "⛅ Partiellement nuageux";
            case 3      -> "☁ Couvert";
            case 45, 48 -> "🌫 Brouillard";
            case 51, 53 -> "🌦 Bruine";
            case 61, 63 -> "🌧 Pluie modérée";
            case 65     -> "🌧 Pluie forte";
            case 71, 73 -> "🌨 Neige";
            case 80, 81 -> "🌦 Averses";
            case 82     -> "⛈ Averses fortes";
            case 95     -> "⛈ Orage";
            case 96, 99 -> "⛈ Orage + grêle";
            default     -> "🌡 Variable";
        };
    }

    private String weatherIcon(int code) {
        return switch (code) {
            case 0      -> "☀";
            case 1      -> "🌤";
            case 2      -> "⛅";
            case 3      -> "☁";
            case 45, 48 -> "🌫";
            case 51, 53 -> "🌦";
            case 61, 63, 65 -> "🌧";
            case 80, 81, 82 -> "🌦";
            case 95, 96, 99 -> "⛈";
            default     -> "🌡";
        };
    }

    private String resolveTypeIcon(String type) {
        if (type == null) return "🏘️";
        String t = type.toLowerCase();
        if (t.contains("hôtel") || t.contains("hotel")) return "🏨";
        if (t.contains("villa"))                         return "🏡";
        if (t.contains("appart"))                        return "🏢";
        if (t.contains("resort"))                        return "🌴";
        if (t.contains("bungalow"))                      return "🛖";
        if (t.contains("riad"))                          return "🏠";
        return "🏘️";
    }

    private void showLoading(boolean show) {
        if (loadingSpinner != null) { loadingSpinner.setVisible(show);  loadingSpinner.setManaged(show);  }
        if (contentArea    != null) { contentArea.setVisible(!show);    contentArea.setManaged(!show);    }
    }

    private void showError(String msg) {
        showLoading(false);
        if (contentArea != null) {
            contentArea.setVisible(true);
            contentArea.setManaged(true);
            Label err = new Label("❌ " + msg);
            err.setStyle("-fx-text-fill:#E74C3C;-fx-font-size:14px;-fx-font-weight:bold;");
            contentArea.getChildren().add(0, err);
        }
    }

    private void hideAllAlerts() {
        hideHBox(alerteUVBox); hideHBox(alerteMerBox); hideHBox(bonTempsBox);
        if (iaSummaryBox != null) { iaSummaryBox.setVisible(false); iaSummaryBox.setManaged(false); }
    }

    private void hideHBox(HBox b) { if (b != null) { b.setVisible(false); b.setManaged(false); } }
    private void showHBox(HBox b) { if (b != null) { b.setVisible(true);  b.setManaged(true);  } }
}
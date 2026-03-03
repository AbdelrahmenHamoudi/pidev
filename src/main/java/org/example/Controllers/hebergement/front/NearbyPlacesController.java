package org.example.Controllers.hebergement.front;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * NearbyPlacesController — Geoapify Places API
 *
 * 🔑 Comment obtenir votre clé GRATUITE (sans carte bancaire) :
 *    1. Allez sur https://myprojects.geoapify.com/register
 *    2. Créez un compte gratuit (email + mot de passe)
 *    3. Créez un nouveau projet
 *    4. Copiez la clé dans la section "API Keys"
 *    5. Collez-la dans GEOAPIFY_API_KEY ci-dessous
 *
 * ✅ 3 000 requêtes/jour gratuites  ✅ Sans carte de crédit  ✅ Tunisie très bien couverte
 */
public class NearbyPlacesController implements Initializable {

    // ================================================================
    // 🔑 VOTRE CLÉ GEOAPIFY
    // ================================================================
    private static final String GEOAPIFY_API_KEY = "ead079109e4f492d84960b9fae84639e";

    @FXML private Label             cityTitleLabel;
    @FXML private Label             citySubtitleLabel;
    @FXML private StackPane         loadingPane;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label             loadingLabel;
    @FXML private VBox              placesList;
    @FXML private Label             statsLabel;
    @FXML private HBox              categoryBar;

    private String cityName;
    private double cityLat = 0;
    private double cityLng = 0;

    private final List<PlaceItem> allPlaces      = new ArrayList<>();
    private       List<PlaceItem> filteredPlaces = new ArrayList<>();
    private       String          activeFilter   = null;

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    // ================================================================
    // POINT D'ENTRÉE
    // ================================================================
    public void setCity(String city) {
        this.cityName  = city.trim();
        this.cityLat   = 0;
        this.cityLng   = 0;
        activeFilter   = null;
        allPlaces.clear();
        filteredPlaces.clear();
        placesList.getChildren().clear();

        cityTitleLabel.setText("Lieux proches de " + city);
        citySubtitleLabel.setText("Chargement en cours…");
        showLoading("Géolocalisation de " + city + "…");

        if (GEOAPIFY_API_KEY.equals("VOTRE_CLE_GEOAPIFY_ICI") || GEOAPIFY_API_KEY.isBlank()) {
            Platform.runLater(() -> {
                hideLoading();
                showError("Clé API manquante",
                        "Remplacez GEOAPIFY_API_KEY dans le code source.\n\n"
                                + "Obtenez votre clé gratuite sur :\nhttps://myprojects.geoapify.com/register");
            });
            return;
        }

        executor.submit(this::loadFromGeoapify);
    }

    // ================================================================
    // PIPELINE : Nominatim (géocodage) → Geoapify (lieux)
    // ================================================================
    private void loadFromGeoapify() {
        try {
            // ── Étape 1 : Géocoder la ville via Nominatim ──────────────────
            String encoded = URLEncoder.encode(cityName + " Tunisie", StandardCharsets.UTF_8);
            String geoUrl  = "https://nominatim.openstreetmap.org/search"
                    + "?q=" + encoded + "&format=json&limit=1&countrycodes=tn";

            System.out.println("[DEBUG] Geocoding URL: " + geoUrl);
            String geoResp = httpGet(geoUrl, "User-Agent", "RE7LA-App/1.0", "Accept-Language", "fr");
            System.out.println("[DEBUG] Geocoding response: "
                    + geoResp.substring(0, Math.min(300, geoResp.length())));

            double lat = 0, lon = 0;
            int objStart = geoResp.indexOf('{');
            if (objStart != -1) {
                String obj = geoResp.substring(objStart);
                lat = extractQuotedDouble(obj, "lat");
                lon = extractQuotedDouble(obj, "lon");
            }

            System.out.println("[DEBUG] Parsed lat=" + lat + " lon=" + lon);

            if (lat == 0 && lon == 0) {
                Platform.runLater(() -> {
                    hideLoading();
                    showError("Ville introuvable",
                            "Nominatim ne reconnaît pas \"" + cityName + "\".\nVérifiez l'orthographe.");
                });
                return;
            }

            cityLat = lat;
            cityLng = lon;
            final double fLat = lat, fLon = lon;

            Platform.runLater(() -> {
                citySubtitleLabel.setText(
                        String.format("%.4f°N  %.4f°E  —  Rayon 3 km", fLat, fLon));
                showLoading("Chargement via Geoapify Places…");
            });

            // ── Étape 2 : Geoapify Places API ──────────────────────────────
            List<PlaceItem> results = queryGeoapify(lat, lon);
            results.sort(Comparator.comparingInt(p -> p.distance));
            final List<PlaceItem> finalList = new ArrayList<>(results);

            System.out.println("[DEBUG] Total places found: " + finalList.size());

            Platform.runLater(() -> {
                hideLoading();
                allPlaces.clear();
                allPlaces.addAll(finalList);
                filteredPlaces = new ArrayList<>(allPlaces);

                if (finalList.isEmpty()) {
                    citySubtitleLabel.setText("Aucun lieu trouvé");
                    showError("Aucun résultat",
                            "Geoapify ne retourne aucun lieu dans un rayon de 3 km.\n"
                                    + "Vérifiez votre clé API sur :\nhttps://myprojects.geoapify.com");
                } else {
                    citySubtitleLabel.setText(
                            String.format("%d lieux trouvés", finalList.size()));
                    displayPlaces(filteredPlaces);
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            final String msg = ex.getMessage() != null ? ex.getMessage() : "Erreur inconnue";
            Platform.runLater(() -> {
                hideLoading();
                showError("Erreur réseau", msg);
            });
        }
    }

    // ================================================================
    // GEOAPIFY PLACES API v2
    // ================================================================
    private List<PlaceItem> queryGeoapify(double lat, double lon) throws Exception {
        List<PlaceItem> result  = new ArrayList<>();
        Set<String>     seenIds = new HashSet<>();

        String[] categoryGroups = {
                "catering.restaurant,catering.fast_food,catering.cafe,catering.coffee_shop",
                "healthcare.hospital,healthcare.clinic,healthcare.pharmacy",
                "commercial.supermarket,commercial.grocery_or_supermarket",
                "leisure.park,beach,natural",
                "tourism.attraction,tourism.sights,entertainment.museum,heritage"
        };

        for (String cats : categoryGroups) {
            String encodedCats = URLEncoder.encode(cats, StandardCharsets.UTF_8);
            String url = "https://api.geoapify.com/v2/places"
                    + "?categories=" + encodedCats
                    + "&filter=circle:" + lon + "," + lat + ",3000"
                    + "&bias=proximity:" + lon + "," + lat
                    + "&limit=50"
                    + "&apiKey=" + GEOAPIFY_API_KEY;

            System.out.println("[DEBUG] Geoapify URL: " + url);

            String resp = httpGetWithStatus(url, "User-Agent", "RE7LA-App/1.0");
            System.out.println("[DEBUG] Response length: " + resp.length());
            System.out.println("[DEBUG] Response preview: "
                    + resp.substring(0, Math.min(500, resp.length())));

            if (resp.isBlank() || resp.equals("{}") || resp.contains("\"features\":[]")) {
                System.out.println("[DEBUG] Empty response for categories: " + cats);
                continue;
            }

            int featIdx = resp.indexOf("\"features\":[");
            if (featIdx == -1) {
                System.out.println("[DEBUG] No 'features' key found in response");
                continue;
            }

            String arr   = resp.substring(featIdx + 12);
            int    depth = 0, start = -1;

            for (int i = 0; i < arr.length(); i++) {
                char ch = arr.charAt(i);
                if (ch == '{') {
                    if (depth == 0) start = i;
                    depth++;
                } else if (ch == '}') {
                    depth--;
                    if (depth == 0 && start != -1) {
                        String feature = arr.substring(start, i + 1);

                        String name = extractStr(feature, "name");
                        if (name == null || name.isBlank()) { start = -1; continue; }

                        double iLat = lat, iLon = lon;
                        int coordIdx = feature.indexOf("\"coordinates\":[");
                        if (coordIdx != -1) {
                            String coordStr   = feature.substring(coordIdx + 15);
                            int    endBracket = coordStr.indexOf(']');
                            if (endBracket != -1) {
                                String[] parts = coordStr.substring(0, endBracket).split(",");
                                if (parts.length >= 2) {
                                    try {
                                        iLon = Double.parseDouble(parts[0].trim());
                                        iLat = Double.parseDouble(parts[1].trim());
                                    } catch (NumberFormatException ignored) {}
                                }
                            }
                        }

                        int dist = (int) Math.round(haversine(lat, lon, iLat, iLon));

                        String icon     = "📍";
                        String catLabel = "Autre";
                        int catArrIdx = feature.indexOf("\"categories\":[");
                        if (catArrIdx != -1) {
                            String catStr = feature.substring(catArrIdx + 14);
                            int    catEnd = catStr.indexOf(']');
                            if (catEnd != -1) {
                                String c = catStr.substring(0, catEnd).toLowerCase();
                                if      (c.contains("restaurant") || c.contains("fast_food"))
                                { icon = "🍽"; catLabel = "Restaurant"; }
                                else if (c.contains("cafe") || c.contains("coffee"))
                                { icon = "☕"; catLabel = "Cafe"; }
                                else if (c.contains("hospital") || c.contains("clinic"))
                                { icon = "🏥"; catLabel = "Hopital"; }
                                else if (c.contains("pharmacy"))
                                { icon = "💊"; catLabel = "Hopital"; }
                                else if (c.contains("supermarket") || c.contains("grocery"))
                                { icon = "🛒"; catLabel = "Supermarche"; }
                                else if (c.contains("beach") || c.contains("natural"))
                                { icon = "🏖"; catLabel = "Plage"; }
                                else if (c.contains("tourism") || c.contains("museum")
                                        || c.contains("attraction") || c.contains("heritage"))
                                { icon = "🏛"; catLabel = "Attraction"; }
                            }
                        }

                        String key = name.toLowerCase().trim();
                        if (!seenIds.contains(key)) {
                            seenIds.add(key);
                            result.add(new PlaceItem(name, catLabel, icon, dist, 0, iLat, iLon));
                            System.out.println("[DEBUG] Added: " + name
                                    + " (" + catLabel + ") " + dist + "m");
                        }
                        start = -1;
                    }
                }
            }
        }

        result.sort((a, b) -> {
            int cmp = a.category.compareTo(b.category);
            return cmp != 0 ? cmp : Integer.compare(a.distance, b.distance);
        });
        return result;
    }

    // ================================================================
    // HAVERSINE
    // ================================================================
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R    = 6_371_000;
        double       dLat = Math.toRadians(lat2 - lat1);
        double       dLon = Math.toRadians(lon2 - lon1);
        double       a    = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ================================================================
    // AFFICHAGE — utilise les classes CSS du nouveau thème Dark Luxury
    // ================================================================
    private void displayPlaces(List<PlaceItem> places) {
        placesList.getChildren().clear();

        if (places.isEmpty()) {
            showError("Aucun résultat", "Aucun lieu dans cette catégorie.");
            return;
        }

        String currentCat = null;
        for (PlaceItem p : places) {
            // En-tête de section catégorie
            if (!p.category.equals(currentCat)) {
                currentCat = p.category;
                Label header = new Label(p.icon + "   " + p.category.toUpperCase());
                header.getStyleClass().add("section-header");
                header.setMaxWidth(Double.MAX_VALUE);
                placesList.getChildren().add(header);
            }
            placesList.getChildren().add(createCard(p));
        }

        int count = places.size();
        if (statsLabel != null)
            statsLabel.setText(count + " lieu" + (count > 1 ? "x" : "") + " trouvé"
                    + (count > 1 ? "s" : ""));
    }

    // ================================================================
    // CARD — design Dark Luxury avec icône dans wrapper arrondi
    // ================================================================
    private HBox createCard(PlaceItem p) {
        HBox card = new HBox(14);
        card.getStyleClass().add("place-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(13, 16, 13, 16));
        card.setCursor(Cursor.HAND);

        // Icône dans un wrapper carré arrondi
        StackPane iconWrap = new StackPane();
        iconWrap.getStyleClass().add("place-icon-wrap");
        Label icon = new Label(p.icon);
        icon.setStyle("-fx-font-size:20px;");
        iconWrap.getChildren().add(icon);

        // Infos : nom + catégorie en capitales
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(p.name);
        name.getStyleClass().add("place-name");
        name.setWrapText(true);

        Label cat = new Label(p.category.toUpperCase());
        cat.getStyleClass().add("place-category");

        info.getChildren().addAll(name, cat);

        // Badge distance en or
        String distStr = p.distance < 1000
                ? p.distance + " m"
                : String.format("%.1f km", p.distance / 1000.0);
        Label dist = new Label(distStr);
        dist.getStyleClass().add("place-distance");
        dist.setMinWidth(62);
        dist.setAlignment(Pos.CENTER);

        card.setOnMouseClicked(e -> openPlaceOnMap(p));
        card.getChildren().addAll(iconWrap, info, dist);
        return card;
    }

    // ================================================================
    // ERREUR — style adapté au thème sombre
    // ================================================================
    private void showError(String title, String detail) {
        placesList.getChildren().clear();
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60));

        Label ic = new Label("⚠");
        ic.setStyle("-fx-font-size:40px; -fx-text-fill:#D4A017;");

        Label t = new Label(title);
        t.setStyle("-fx-font-size:15px; -fx-font-weight:800;"
                + "-fx-text-fill:#A8C8E8; -fx-text-alignment:center;");
        t.setWrapText(true);

        Label d = new Label(detail);
        d.setStyle("-fx-font-size:12px; -fx-text-fill:#3D6A8A;"
                + "-fx-text-alignment:center;");
        d.setWrapText(true);

        box.getChildren().addAll(ic, t, d);
        placesList.getChildren().add(box);
        if (statsLabel != null) statsLabel.setText("0 lieu trouvé");
    }

    // ================================================================
    // OUVRIR DANS LA CARTE
    // ================================================================
    private void openPlaceOnMap(PlaceItem p) {
        try {
            double lat = p.lat != 0 ? p.lat : cityLat;
            double lng = p.lng != 0 ? p.lng : cityLng;
            String url = "https://www.openstreetmap.org/?mlat=" + lat
                    + "&mlon=" + lng + "#map=17/" + lat + "/" + lng;
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ================================================================
    // FILTRES
    // ================================================================
    @FXML private void filterAll()          { applyFilter(null);          updateCatButtons(null); }
    @FXML private void filterRestaurants()  { applyFilter("Restaurant");  updateCatButtons("Restaurant"); }
    @FXML private void filterCafes()        { applyFilter("Cafe");        updateCatButtons("Cafe"); }
    @FXML private void filterBeaches()      { applyFilter("Plage");       updateCatButtons("Plage"); }
    @FXML private void filterAttractions()  { applyFilter("Attraction");  updateCatButtons("Attraction"); }
    @FXML private void filterHospitals()    { applyFilter("Hopital");     updateCatButtons("Hopital"); }
    @FXML private void filterSupermarkets() { applyFilter("Supermarche"); updateCatButtons("Supermarche"); }

    private void applyFilter(String cat) {
        activeFilter   = cat;
        filteredPlaces = new ArrayList<>();
        for (PlaceItem p : allPlaces)
            if (cat == null || p.category.equals(cat))
                filteredPlaces.add(p);
        displayPlaces(filteredPlaces);
    }

    private void updateCatButtons(String activeCat) {
        if (categoryBar == null) return;
        String[] cats = {null, "Restaurant", "Cafe", "Plage", "Attraction", "Hopital", "Supermarche"};
        int catIdx = 0;
        for (javafx.scene.Node node : categoryBar.getChildren()) {
            if (node instanceof Button btn && catIdx < cats.length) {
                boolean active = Objects.equals(cats[catIdx], activeCat);
                btn.getStyleClass().removeAll("chip", "chip-active");
                btn.getStyleClass().add(active ? "chip-active" : "chip");
                catIdx++;
            }
        }
    }

    @FXML
    public void openMapInBrowser() {
        try {
            double lat = cityLat != 0 ? cityLat : 33.8869;
            double lng = cityLng != 0 ? cityLng : 9.5375;
            String url = "https://www.openstreetmap.org/?mlat=" + lat
                    + "&mlon=" + lng + "#map=14/" + lat + "/" + lng;
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ================================================================
    // UTILITAIRES JSON
    // ================================================================
    private double extractQuotedDouble(String json, String key) {
        try {
            String s   = "\"" + key + "\":\"";
            int    idx = json.indexOf(s);
            if (idx == -1) return 0;
            int start = idx + s.length();
            return Double.parseDouble(
                    json.substring(start, json.indexOf("\"", start)).trim());
        } catch (Exception e) { return 0; }
    }

    private String extractStr(String json, String key) {
        try {
            String s   = "\"" + key + "\":\"";
            int    idx = json.indexOf(s);
            if (idx == -1) return null;
            int           start = idx + s.length();
            StringBuilder sb    = new StringBuilder();
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) { sb.append(json.charAt(++i)); continue; }
                if (c == '"') break;
                sb.append(c);
            }
            String r = sb.toString().trim();
            return r.isEmpty() ? null : r;
        } catch (Exception e) { return null; }
    }

    private String httpGet(String urlStr, String... headers) throws Exception {
        return httpGetWithStatus(urlStr, headers);
    }

    private String httpGetWithStatus(String urlStr, String... headers) throws Exception {
        URL               url = new URL(urlStr);
        HttpURLConnection c   = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        for (int i = 0; i + 1 < headers.length; i += 2)
            c.setRequestProperty(headers[i], headers[i + 1]);
        c.setConnectTimeout(15000);
        c.setReadTimeout(25000);

        int code = c.getResponseCode();
        System.out.println("[DEBUG] HTTP " + code + " ← "
                + urlStr.substring(0, Math.min(120, urlStr.length())));

        if (code != 200) {
            if (c.getErrorStream() != null) {
                BufferedReader err = new BufferedReader(
                        new InputStreamReader(c.getErrorStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = err.readLine()) != null) sb.append(line);
                System.err.println("[DEBUG] Error body: " + sb);
            }
            return "{}";
        }

        BufferedReader r  = new BufferedReader(
                new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder  sb = new StringBuilder(); String line;
        while ((line = r.readLine()) != null) sb.append(line).append('\n');
        r.close();
        return sb.toString();
    }

    // ================================================================
    // LOADING
    // ================================================================
    private void showLoading(String msg) {
        if (loadingPane      != null) { loadingPane.setVisible(true);  loadingPane.setManaged(true); }
        if (loadingLabel     != null) loadingLabel.setText(msg);
        if (loadingIndicator != null) loadingIndicator.setProgress(-1);
    }

    private void hideLoading() {
        if (loadingPane != null) { loadingPane.setVisible(false); loadingPane.setManaged(false); }
    }

    @FXML
    private void closeDialog() {
        executor.shutdownNow();
        ((Stage) cityTitleLabel.getScene().getWindow()).close();
    }

    // ================================================================
    // MODÈLE
    // ================================================================
    public static class PlaceItem {
        public final String name, category, icon;
        public final int    distance;
        public final double rating, lat, lng;

        public PlaceItem(String n, String cat, String ico, int d, double r, double la, double lo) {
            name = n; category = cat; icon = ico; distance = d; rating = r; lat = la; lng = lo;
        }
    }
}
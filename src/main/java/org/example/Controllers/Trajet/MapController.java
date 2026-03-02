package org.example.Controllers.Trajet;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.example.Entites.trajet.Trajet;
import org.example.Services.trajet.MapService;
import org.example.Services.trajet.MapService.Coordonnees;
import org.example.Services.trajet.MapService.ItineraireResult;
import org.example.Services.trajet.TrajetCRUD;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MapController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(MapController.class.getName());

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private WebView    mapWebView;
    @FXML private ComboBox<String> mapDepartCombo;
    @FXML private ComboBox<String> mapArriveeCombo;
    @FXML private VBox       resultsPanel;
    @FXML private Label      resultDistance;
    @FXML private Label      resultDuree;
    @FXML private Label      resultPrix;
    @FXML private Label      resultResume;
    @FXML private VBox       trajetsListMap;
    @FXML private VBox       loadingOverlay;
    @FXML private Label      loadingLabel;
    @FXML private Label      statusLabel;
    @FXML private Label      statusDot;

    // ── Services ──────────────────────────────────────────────────────────────
    private final MapService  mapService  = new MapService();
    private final TrajetCRUD  trajetCRUD  = new TrajetCRUD();

    // ── État ──────────────────────────────────────────────────────────────────
    private WebEngine         webEngine;
    private ItineraireResult  dernierItineraire;

    /** Callback : appelé par FrontController pour pré-remplir les combos */
    private Runnable          onItineraireChoisi;
    private double            distanceRetour;
    private String    villeDepart;   // ← NOUVEAU
    private String    villeArrivee;  // ← NOUVEAU
    private int       dureeMinutes;  // ← NOUVEAU

    // ═══════════════════════════════════════════════════════════════════════════
    //  INIT
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        webEngine = mapWebView.getEngine();

        // Charger la carte Leaflet
        webEngine.loadContent(buildLeafletHTML());

        // Remplir les combos villes
        var villes = FXCollections.observableArrayList(
                "Tunis","Sousse","Sfax","Bizerte","Gabès","Ariana","Nabeul",
                "Hammamet","Kairouan","Monastir","Mahdia","Sidi Bouzid","Gafsa",
                "Tozeur","Djerba","El Kef","Jendouba","Siliana","Zaghouan",
                "Kasserine","Ben Arous","Manouba","Medenine","Beja","Tataouine"
        );
        mapDepartCombo.setItems(villes);
        mapArriveeCombo.setItems(villes);

        loadingOverlay.setVisible(false);
        loadingOverlay.setManaged(false);

        // Charger les trajets existants dans le panneau gauche
        chargerTrajetsPanel();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ACTIONS FXML
    // ═══════════════════════════════════════════════════════════════════════════

    @FXML
    private void calculerItineraire() {
        String dep = mapDepartCombo.getValue();
        String arr = mapArriveeCombo.getValue();

        if (dep == null || arr == null) {
            showAlert("Erreur", "Veuillez sélectionner les deux villes !");
            return;
        }
        if (dep.equalsIgnoreCase(arr)) {
            showAlert("Erreur", "Départ et arrivée doivent être différents !");
            return;
        }

        // Afficher le spinner
        setLoading(true, "Calcul de l'itinéraire " + dep + " → " + arr + "…");

        // Appel REST asynchrone (ne pas bloquer le thread UI)
        CompletableFuture.supplyAsync(() -> {
            try {
                return mapService.calculerItineraire(dep, arr);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(result -> Platform.runLater(() -> {
            setLoading(false, "");
            afficherResultat(result);
            tracerItineraireOnMap(result);
            dernierItineraire = result;

        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                setLoading(false, "");
                // Fallback : utiliser les coordonnées statiques uniquement
                LOGGER.log(Level.WARNING, "OSRM indisponible, fallback statique", ex);
                afficherFallback(dep, arr);
                setStatus(false);
            });
            return null;
        });
    }

    @FXML
    private void utiliserItineraire() {
        if (dernierItineraire == null) return;
        // ① Stocker TOUTES les données avant fermeture
        distanceRetour = dernierItineraire.distanceKm;
        villeDepart    = dernierItineraire.depart.ville;
        villeArrivee   = dernierItineraire.arrivee.ville;
        dureeMinutes   = dernierItineraire.dureeMinutes;
        // ② Déclencher le callback (remplit le formulaire FrontController)
        if (onItineraireChoisi != null) onItineraireChoisi.run();
        // ③ Fermer APRÈS le callback (les getters sont encore accessibles)
        fermer();
    }

    @FXML
    private void fermer() {
        Stage stage = (Stage) mapWebView.getScene().getWindow();
        stage.close();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  AFFICHAGE RÉSULTATS
    // ═══════════════════════════════════════════════════════════════════════════

    private void afficherResultat(ItineraireResult r) {
        resultDistance.setText(String.format("%.1f km", r.distanceKm));
        resultDuree.setText(formatDuree(r.dureeMinutes));
        resultPrix.setText(String.format("%.2f DT", r.distanceKm)); // 1 DT/km
        resultResume.setText(r.resume);
        resultsPanel.setVisible(true);
        resultsPanel.setManaged(true);
        setStatus(true);
    }

    private void afficherFallback(String dep, String arr) {
        // Calculer distance statique
        double dist = calculerDistanceStatique(dep, arr);
        resultDistance.setText(String.format("%.0f km (estimé)", dist));
        resultDuree.setText(formatDuree((int)(dist * 1.2))); // ~80 km/h
        resultPrix.setText(String.format("%.2f DT", dist));
        resultResume.setText("⚠️ Mode hors ligne · distance approximative");
        resultsPanel.setVisible(true);
        resultsPanel.setManaged(true);

        // Tracer quand même sur la carte avec les coordonnées statiques
        Coordonnees c1 = mapService.geocoder(dep);
        Coordonnees c2 = mapService.geocoder(arr);
        tracerMarqueursSimples(c1, c2);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CARTE LEAFLET (JavaScript injecté)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Trace l'itinéraire complet (polyline OSRM + marqueurs) sur la carte Leaflet.
     */
    private void tracerItineraireOnMap(ItineraireResult r) {
        String js = String.format(
                "tracerItineraire(%f, %f, %f, %f, %s, '%s', '%s');",
                r.depart.lat, r.depart.lon,
                r.arrivee.lat, r.arrivee.lon,
                r.polylineGeoJson,
                r.depart.ville,
                r.arrivee.ville
        );
        webEngine.executeScript(js);
    }

    private void tracerMarqueursSimples(Coordonnees dep, Coordonnees arr) {
        String js = String.format(
                "tracerMarqueurs(%f, %f, %f, %f, '%s', '%s');",
                dep.lat, dep.lon,
                arr.lat, arr.lon,
                dep.ville, arr.ville
        );
        webEngine.executeScript(js);
    }

    /**
     * Surbrillance d'un trajet (depuis la liste de gauche).
     */
    private void zoomTrajet(String depart, String arrivee) {
        Coordonnees c1 = mapService.geocoder(depart);
        Coordonnees c2 = mapService.geocoder(arrivee);
        tracerMarqueursSimples(c1, c2);
        mapDepartCombo.setValue(depart);
        mapArriveeCombo.setValue(arrivee);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  HTML LEAFLET
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Génère la page HTML embarquée avec Leaflet.js.
     * On utilise un CDN public (unpkg).
     */
    private String buildLeafletHTML() {
        return """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>RE7LA Map</title>
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  html, body, #map { width:100%; height:100%; background:#0f172a; }
  .leaflet-container { background:#1e293b !important; }
  .custom-popup .leaflet-popup-content-wrapper {
    background: #1e293b;
    color: #e2e8f0;
    border: 1px solid #334155;
    border-radius: 12px;
    font-family: system-ui, sans-serif;
    font-size: 13px;
  }
  .custom-popup .leaflet-popup-tip { background: #1e293b; }
  .route-info-box {
    position: absolute; top: 16px; right: 16px; z-index: 999;
    background: rgba(15,23,42,0.92);
    border: 1px solid #334155;
    border-radius: 14px;
    padding: 14px 18px;
    color: white;
    font-family: system-ui, sans-serif;
    min-width: 200px;
    display: none;
    backdrop-filter: blur(8px);
  }
  .route-info-box.visible { display: block; }
  .route-info-box h3 { font-size: 13px; color: #94a3b8; margin-bottom: 8px; }
  .info-row { display: flex; justify-content: space-between; align-items: center; margin: 6px 0; }
  .info-label { font-size: 11px; color: #64748b; }
  .info-value { font-size: 16px; font-weight: bold; }
  .dist-val { color: #3b82f6; }
  .dur-val  { color: #10b981; }
</style>
</head>
<body>
<div id="map"></div>
<div class="route-info-box" id="routeInfoBox">
  <h3>📊 Itinéraire</h3>
  <div class="info-row">
    <span class="info-label">Distance</span>
    <span class="info-value dist-val" id="boxDist">—</span>
  </div>
  <div class="info-row">
    <span class="info-label">Durée</span>
    <span class="info-value dur-val" id="boxDur">—</span>
  </div>
</div>

<script>
// ── Initialisation Leaflet ────────────────────────────────────────────
var map = L.map('map', {
  center: [34.0, 9.0],
  zoom: 6,
  zoomControl: true
});

L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
  attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
  maxZoom: 18
}).addTo(map);

// ── Couches réutilisables ─────────────────────────────────────────────
var routeLayer   = null;
var markersLayer = L.layerGroup().addTo(map);

// Icônes personnalisées
var iconDepart = L.divIcon({
  html: '<div style="background:#10b981;width:16px;height:16px;border-radius:50%;border:3px solid white;box-shadow:0 2px 8px rgba(0,0,0,0.4)"></div>',
  iconSize: [16, 16], iconAnchor: [8, 8], className: ''
});
var iconArrivee = L.divIcon({
  html: '<div style="background:#ef4444;width:16px;height:16px;border-radius:50%;border:3px solid white;box-shadow:0 2px 8px rgba(0,0,0,0.4)"></div>',
  iconSize: [16, 16], iconAnchor: [8, 8], className: ''
});

// ── Fonction : tracer itinéraire complet (polyline GeoJSON OSRM) ──────
function tracerItineraire(latD, lonD, latA, lonA, geoJsonStr, villeD, villeA) {
  try {
    markersLayer.clearLayers();
    if (routeLayer) { map.removeLayer(routeLayer); routeLayer = null; }

    // Marqueur départ
    L.marker([latD, lonD], {icon: iconDepart})
      .bindPopup('<b>🟢 Départ</b><br>' + villeD, {className: 'custom-popup'})
      .addTo(markersLayer).openPopup();

    // Marqueur arrivée
    L.marker([latA, lonA], {icon: iconArrivee})
      .bindPopup('<b>🔴 Arrivée</b><br>' + villeA, {className: 'custom-popup'})
      .addTo(markersLayer);

    // Trace de l'itinéraire (polyline OSRM)
    if (geoJsonStr && geoJsonStr !== 'null') {
      var geometry = JSON.parse(geoJsonStr);
      routeLayer = L.geoJSON(geometry, {
        style: {
          color: '#3b82f6',
          weight: 5,
          opacity: 0.85,
          lineCap: 'round',
          lineJoin: 'round',
          dashArray: null
        }
      }).addTo(map);

      // Zoom sur le tracé
      map.fitBounds(routeLayer.getBounds(), { padding: [40, 40] });
    } else {
      // Fallback : ligne droite
      var line = L.polyline([[latD, lonD], [latA, lonA]], {
        color: '#3b82f6', weight: 4, opacity: 0.7, dashArray: '8 6'
      }).addTo(markersLayer);
      map.fitBounds(line.getBounds(), { padding: [60, 60] });
    }

    // Afficher le box d'info
    document.getElementById('routeInfoBox').classList.add('visible');

  } catch(e) {
    console.error('Erreur tracé itinéraire:', e);
    tracerMarqueurs(latD, lonD, latA, lonA, villeD, villeA);
  }
}

// ── Fonction : marqueurs simples sans tracé ───────────────────────────
function tracerMarqueurs(latD, lonD, latA, lonA, villeD, villeA) {
  markersLayer.clearLayers();
  if (routeLayer) { map.removeLayer(routeLayer); routeLayer = null; }

  var mD = L.marker([latD, lonD], {icon: iconDepart})
             .bindPopup('<b>🟢 ' + villeD + '</b>', {className: 'custom-popup'})
             .addTo(markersLayer);
  var mA = L.marker([latA, lonA], {icon: iconArrivee})
             .bindPopup('<b>🔴 ' + villeA + '</b>', {className: 'custom-popup'})
             .addTo(markersLayer);

  var line = L.polyline([[latD, lonD], [latA, lonA]], {
    color: '#64748b', weight: 3, opacity: 0.6, dashArray: '6 4'
  }).addTo(markersLayer);

  map.fitBounds([[latD, lonD], [latA, lonA]], { padding: [60, 60] });
  mD.openPopup();
}

// ── Mise à jour du box info ───────────────────────────────────────────
function updateInfoBox(dist, dur) {
  document.getElementById('boxDist').textContent = dist;
  document.getElementById('boxDur').textContent  = dur;
  document.getElementById('routeInfoBox').classList.add('visible');
}

// ── Zoom sur une ville ────────────────────────────────────────────────
function zoomVille(lat, lon, nom) {
  map.setView([lat, lon], 12);
  L.popup({ className: 'custom-popup' })
   .setLatLng([lat, lon])
   .setContent('<b>📍 ' + nom + '</b>')
   .openOn(map);
}
</script>
</body>
</html>
""";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PANNEAU TRAJETS (liste gauche)
    // ═══════════════════════════════════════════════════════════════════════════

    private void chargerTrajetsPanel() {
        try {
            List<Trajet> trajets = trajetCRUD.afficherh();
            trajetsListMap.getChildren().clear();

            if (trajets == null || trajets.isEmpty()) {
                Label empty = new Label("Aucun trajet enregistré");
                empty.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px; -fx-padding: 8;");
                trajetsListMap.getChildren().add(empty);
                return;
            }

            for (Trajet t : trajets) {
                trajetsListMap.getChildren().add(buildTrajetMiniCard(t));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erreur chargement trajets pour carte", e);
        }
    }

    private VBox buildTrajetMiniCard(Trajet t) {
        VBox card = new VBox(4);
        card.setStyle("""
                -fx-background-color: rgba(30,41,59,0.8);
                -fx-padding: 10 12;
                -fx-background-radius: 10;
                -fx-border-color: #334155;
                -fx-border-radius: 10;
                -fx-cursor: hand;
                """);

        String route  = t.getPointDepart() + " → " + t.getPointArrivee();
        String detail = String.format("%.0f km  ·  %s",
                t.getDistanceKm(),
                t.getDateReservation() != null
                        ? new SimpleDateFormat("dd MMM yyyy").format(t.getDateReservation())
                        : "Date ?");

        Label routeLbl = new Label("📍 " + route);
        routeLbl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label detailLbl = new Label(detail);
        detailLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px;");

        card.getChildren().addAll(routeLbl, detailLbl);

        // Hover + clic
        card.setOnMouseEntered(e ->
                card.setStyle(card.getStyle().replace("#334155", "#3b82f6")));
        card.setOnMouseExited(e ->
                card.setStyle(card.getStyle().replace("#3b82f6", "#334155")));
        card.setOnMouseClicked(e ->
                zoomTrajet(t.getPointDepart(), t.getPointArrivee()));

        return card;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ═══════════════════════════════════════════════════════════════════════════

    private void setLoading(boolean visible, String msg) {
        loadingOverlay.setVisible(visible);
        loadingOverlay.setManaged(visible);
        if (visible) loadingLabel.setText(msg);
    }

    private void setStatus(boolean online) {
        if (online) {
            statusDot.setStyle("-fx-text-fill: #10b981; -fx-font-size: 10px;");
            statusLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 12px; -fx-font-weight: bold;");
            statusLabel.setText("Connecté");
        } else {
            statusDot.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 10px;");
            statusLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 12px; -fx-font-weight: bold;");
            statusLabel.setText("Mode hors ligne");
        }
    }

    private String formatDuree(int minutes) {
        if (minutes < 60) return minutes + " min";
        return (minutes / 60) + "h" + String.format("%02d", minutes % 60);
    }

    private double calculerDistanceStatique(String dep, String arr) {
        // Même table que FrontController — fallback sans réseau
        java.util.Map<String, Double> d = new java.util.HashMap<>();
        d.put("tunis-sousse", 140.0); d.put("tunis-sfax", 270.0);
        d.put("tunis-bizerte", 66.0); d.put("tunis-gabès", 405.0);
        d.put("tunis-hammamet", 65.0); d.put("tunis-kairouan", 160.0);
        d.put("tunis-monastir", 162.0); d.put("tunis-gafsa", 370.0);
        d.put("tunis-tozeur", 430.0); d.put("tunis-djerba", 520.0);
        d.put("sousse-sfax", 130.0); d.put("sousse-monastir", 22.0);
        d.put("sfax-gabès", 135.0); d.put("sfax-djerba", 250.0);

        String k = dep.trim().toLowerCase() + "-" + arr.trim().toLowerCase();
        String r = arr.trim().toLowerCase() + "-" + dep.trim().toLowerCase();
        if (d.containsKey(k)) return d.get(k);
        if (d.containsKey(r)) return d.get(r);
        return 100.0;
    }

    private void showAlert(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }

    // ── API publique pour FrontController ─────────────────────────────────────

    public void setOnItineraireChoisi(Runnable cb) { this.onItineraireChoisi = cb; }
    public double getDistanceRetour()  { return distanceRetour; }
    public String getVilleDepart()     { return villeDepart;    }  // ← NOUVEAU
    public String getVilleArrivee()    { return villeArrivee;   }  // ← NOUVEAU
    public int    getDureeMinutes()    { return dureeMinutes;   }  // ← NOUVEAU

    /** Pré-remplit les combos depuis FrontController (trajet sélectionné). */
    public void preRemplir(String depart, String arrivee) {
        if (depart  != null) mapDepartCombo.setValue(depart);
        if (arrivee != null) mapArriveeCombo.setValue(arrivee);
    }
}

package org.example.Services.trajet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service d'intégration OpenStreetMap / OSRM
 * ─────────────────────────────────────────
 * APIs utilisées (toutes gratuites, sans clé API) :
 *  • Nominatim  → Géocodage (ville → lat/lon)
 *  • OSRM       → Calcul d'itinéraire (distance réelle + durée)
 *  • OpenRouteService → Polyline GeoJSON pour affichage sur carte
 */
public class MapService {

    private static final Logger LOGGER = Logger.getLogger(MapService.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── Endpoints ──────────────────────────────────────────────────────────────
    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/search?q=%s&countrycodes=tn&format=json&limit=1";
    private static final String OSRM_ROUTE_URL =
            "https://router.project-osrm.org/route/v1/driving/%s,%s;%s,%s?overview=full&geometries=geojson&steps=true";

    // ═══════════════════════════════════════════════════════════════════════════
    //  MODÈLES DE DONNÉES
    // ═══════════════════════════════════════════════════════════════════════════

    public static class Coordonnees {
        public final double lat;
        public final double lon;
        public final String ville;

        public Coordonnees(double lat, double lon, String ville) {
            this.lat = lat;
            this.lon = lon;
            this.ville = ville;
        }

        @Override
        public String toString() {
            return String.format("Coordonnees{ville='%s', lat=%.6f, lon=%.6f}", ville, lat, lon);
        }
    }

    public static class ItineraireResult {
        public final double distanceKm;
        public final int    dureeMinutes;
        public final String polylineGeoJson;   // Geometry GeoJSON (LineString)
        public final Coordonnees depart;
        public final Coordonnees arrivee;
        public final String      resume;        // "Tunis → Sousse : 142 km en 1h35"

        public ItineraireResult(double distanceKm, int dureeMinutes,
                                String polylineGeoJson,
                                Coordonnees depart, Coordonnees arrivee) {
            this.distanceKm     = distanceKm;
            this.dureeMinutes   = dureeMinutes;
            this.polylineGeoJson = polylineGeoJson;
            this.depart          = depart;
            this.arrivee         = arrivee;

            int heures  = dureeMinutes / 60;
            int minutes = dureeMinutes % 60;
            String dureeStr = heures > 0
                    ? heures + "h" + String.format("%02d", minutes)
                    : minutes + "min";
            this.resume = String.format("%s → %s : %.0f km en %s",
                    depart.ville, arrivee.ville, distanceKm, dureeStr);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  GÉOCODAGE  (ville tunisienne → lat/lon)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Convertit un nom de ville en coordonnées GPS via Nominatim.
     * Fallback : coordonnées pré-définies pour les principales villes tunisiennes.
     */
    public Coordonnees geocoder(String ville) {
        // 1. Essayer via Nominatim
        try {
            String encoded = URLEncoder.encode(ville + " Tunisia", StandardCharsets.UTF_8);
            String urlStr  = String.format(NOMINATIM_URL, encoded);
            String json    = httpGet(urlStr, "Nominatim/1.0");

            JsonNode root = MAPPER.readTree(json);
            if (root.isArray() && root.size() > 0) {
                JsonNode first = root.get(0);
                double lat = first.get("lat").asDouble();
                double lon = first.get("lon").asDouble();
                LOGGER.info("Geocoded '" + ville + "' → lat=" + lat + " lon=" + lon);
                return new Coordonnees(lat, lon, ville);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Nominatim indisponible, fallback coordonnées statiques", e);
        }

        // 2. Fallback : table pré-définie des principales villes tunisiennes
        return fallbackCoordonnees(ville);
    }

    private Coordonnees fallbackCoordonnees(String ville) {
        switch (ville.trim().toLowerCase()) {
            case "tunis":        return new Coordonnees(36.8190, 10.1660, ville);
            case "sousse":       return new Coordonnees(35.8245, 10.6346, ville);
            case "sfax":         return new Coordonnees(34.7400, 10.7600, ville);
            case "bizerte":      return new Coordonnees(37.2744, 9.8739,  ville);
            case "gabès":        return new Coordonnees(33.8833, 10.1000, ville);
            case "ariana":       return new Coordonnees(36.8625, 10.1956, ville);
            case "nabeul":       return new Coordonnees(36.4515, 10.7356, ville);
            case "hammamet":     return new Coordonnees(36.4000, 10.6167, ville);
            case "kairouan":     return new Coordonnees(35.6781, 10.0963, ville);
            case "monastir":     return new Coordonnees(35.7643, 10.8113, ville);
            case "mahdia":       return new Coordonnees(35.5047, 11.0622, ville);
            case "sidi bouzid":  return new Coordonnees(35.0381, 9.4858,  ville);
            case "gafsa":        return new Coordonnees(34.4250, 8.7842,  ville);
            case "tozeur":       return new Coordonnees(33.9197, 8.1335,  ville);
            case "djerba":       return new Coordonnees(33.8075, 10.8451, ville);
            case "el kef":       return new Coordonnees(36.1826, 8.7149,  ville);
            case "jendouba":     return new Coordonnees(36.5011, 8.7803,  ville);
            case "siliana":      return new Coordonnees(36.0847, 9.3717,  ville);
            case "zaghouan":     return new Coordonnees(36.4028, 10.1428, ville);
            case "kasserine":    return new Coordonnees(35.1722, 8.8306,  ville);
            case "ben arous":    return new Coordonnees(36.7531, 10.2183, ville);
            case "manouba":      return new Coordonnees(36.8092, 10.0980, ville);
            case "medenine":     return new Coordonnees(33.3549, 10.5055, ville);
            case "beja":         return new Coordonnees(36.7256, 9.1817,  ville);
            case "tataouine":    return new Coordonnees(32.9211, 10.4511, ville);
            default:             return new Coordonnees(36.8190, 10.1660, ville); // Tunis par défaut
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CALCUL D'ITINÉRAIRE  (OSRM)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calcule l'itinéraire réel entre deux villes via OSRM.
     * Retourne distance (km), durée (min) et le tracé GeoJSON.
     */
    public ItineraireResult calculerItineraire(String villeDepart, String villeArrivee) throws Exception {
        Coordonnees dep = geocoder(villeDepart);
        Coordonnees arr = geocoder(villeArrivee);

        String urlStr = String.format(OSRM_ROUTE_URL,
                dep.lon, dep.lat, arr.lon, arr.lat);

        String json     = httpGet(urlStr, "JavaFX-RE7LA/1.0");
        JsonNode root   = MAPPER.readTree(json);
        JsonNode routes = root.path("routes");

        if (!routes.isArray() || routes.size() == 0)
            throw new Exception("OSRM : aucun itinéraire trouvé entre " + villeDepart + " et " + villeArrivee);

        JsonNode route    = routes.get(0);
        double   distance = route.path("distance").asDouble() / 1000.0; // mètres → km
        int      duree    = (int) (route.path("duration").asDouble() / 60.0); // secondes → minutes

        // Geometry GeoJSON (LineString) → on la remet en string JSON
        String geometryJson = MAPPER.writeValueAsString(route.path("geometry"));

        LOGGER.info(String.format("Itinéraire %s → %s : %.1f km, %d min",
                villeDepart, villeArrivee, distance, duree));

        return new ItineraireResult(distance, duree, geometryJson, dep, arr);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  HTTP GET HELPER
    // ═══════════════════════════════════════════════════════════════════════════

    private String httpGet(String urlStr, String userAgent) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);

        int code = conn.getResponseCode();
        if (code != 200)
            throw new Exception("HTTP " + code + " pour : " + urlStr);

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}

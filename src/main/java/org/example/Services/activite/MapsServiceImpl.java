package org.example.Services.activite;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 🗺️ Service de cartes et géolocalisation
 * Utilise Nominatim (OpenStreetMap) - 100% GRATUIT
 */
public class MapsServiceImpl {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org";

    /**
     * Géocode une adresse (texte → coordonnées)
     */
    public Location geocode(String address) {
        try {
            String encodedAddress = URLEncoder.encode(address + ", Tunisia", StandardCharsets.UTF_8);
            String urlString = NOMINATIM_URL + "/search?q=" + encodedAddress +
                    "&format=json&limit=1";

            String response = makeRequest(urlString);
            JSONArray results = new JSONArray(response);

            if (results.length() > 0) {
                JSONObject result = results.getJSONObject(0);

                Location location = new Location();
                location.setName(address);
                location.setLatitude(result.getDouble("lat"));
                location.setLongitude(result.getDouble("lon"));
                location.setDisplayName(result.getString("display_name"));

                System.out.println("✅ Géocodage : " + address + " → " +
                        location.getLatitude() + ", " + location.getLongitude());

                return location;
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur géocodage : " + e.getMessage());
        }

        return null;
    }

    /**
     * Reverse géocode (coordonnées → adresse)
     */
    public String reverseGeocode(double lat, double lon) {
        try {
            String urlString = NOMINATIM_URL + "/reverse?lat=" + lat +
                    "&lon=" + lon + "&format=json";

            String response = makeRequest(urlString);
            JSONObject result = new JSONObject(response);

            return result.getString("display_name");

        } catch (Exception e) {
            System.err.println("❌ Erreur reverse géocodage : " + e.getMessage());
            return null;
        }
    }

    /**
     * Calcule la distance entre deux points (en km)
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Formule de Haversine
        final int R = 6371; // Rayon de la Terre en km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Génère l'URL d'une carte statique
     */
    public String getStaticMapUrl(double lat, double lon, int zoom) {
        // Utilise OpenStreetMap static map
        return String.format("https://www.openstreetmap.org/?mlat=%f&mlon=%f#map=%d/%f/%f",
                lat, lon, zoom, lat, lon);
    }

    /**
     * Génère l'URL Google Maps pour navigation
     */
    public String getGoogleMapsUrl(double lat, double lon) {
        return String.format("https://www.google.com/maps/dir/?api=1&destination=%f,%f",
                lat, lon);
    }

    /**
     * Génère l'URL Google Maps pour navigation depuis un point
     */
    public String getDirectionsUrl(double fromLat, double fromLon, double toLat, double toLon) {
        return String.format("https://www.google.com/maps/dir/%f,%f/%f,%f",
                fromLat, fromLon, toLat, toLon);
    }

    /**
     * Effectue une requête HTTP
     */
    private String makeRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "RE7LA-App/1.0");
        conn.setConnectTimeout(5000);

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
        );
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return response.toString();
    }

    // ========== CLASSE DE DONNÉES ==========

    public static class Location {
        private String name;
        private double latitude;
        private double longitude;
        private String displayName;

        // Getters et Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        @Override
        public String toString() {
            return String.format("%s (%.4f, %.4f)", name, latitude, longitude);
        }
    }
}
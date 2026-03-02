package org.example.Services.user.APIservices;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.Entites.user.LocationInfo;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class GeolocationService {

    // Changement d'API : ip-api.com (plus fiable)
    private static final String API_URL = "http://ip-api.com/json/";
    private static final Gson gson = new Gson();

    // Cache pour éviter les appels répétés
    private static LocationInfo cachedLocation = null;
    private static long lastCallTime = 0;
    private static final long CACHE_DURATION = 3600000; // 1 heure

    /**
     * Récupère l'IP publique de la machine
     */
    private static String getPublicIP() {
        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet("https://api.ipify.org");
            HttpResponse response = httpClient.execute(httpGet);
            String publicIP = EntityUtils.toString(response.getEntity()).trim();
            System.out.println("🌍 IP publique détectée: " + publicIP);
            return publicIP;
        } catch (Exception e) {
            System.err.println("❌ Impossible de récupérer l'IP publique: " + e.getMessage());
            return "8.8.8.8";
        }
    }

    /**
     * Récupère la localisation de l'IP actuelle
     */
    public static LocationInfo getCurrentLocation() {
        return getLocationByIP("");
    }

    /**
     * Récupère la localisation d'une IP spécifique avec cache
     */
    public static LocationInfo getLocationByIP(String ip) {

        // Vérifier si on a un cache valide
        long now = System.currentTimeMillis();
        if (cachedLocation != null && (now - lastCallTime) < CACHE_DURATION) {
            System.out.println("📦 Utilisation du cache (moins d'1 heure)");
            return cachedLocation;
        }

        try {
            // Si l'IP est vide ou locale, on récupère d'abord l'IP publique
            if (ip == null || ip.isEmpty() || ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("localhost")) {
                ip = getPublicIP();
                System.out.println("🌍 Utilisation de l'IP publique: " + ip);
            }

            // Format pour ip-api.com
            String urlString = API_URL + ip + "?fields=status,message,country,countryCode,region,city,lat,lon,timezone,isp,query";

            System.out.println("🌍 Appel API ip-api.com: " + urlString);

            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(urlString);
            httpGet.setHeader("User-Agent", "Mozilla/5.0");

            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != 200) {
                System.err.println("❌ API ip-api a retourné le code: " + statusCode);
                return getTestLocation();
            }

            String jsonResponse = EntityUtils.toString(response.getEntity());

            // Parser la réponse
            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

            // Vérifier le statut
            String status = jsonObject.get("status").getAsString();
            if (!"success".equals(status)) {
                String message = jsonObject.has("message") ? jsonObject.get("message").getAsString() : "Erreur inconnue";
                System.err.println("❌ Erreur API ip-api: " + message);
                return getTestLocation();
            }

            // Convertir vers notre format LocationInfo
            LocationInfo location = new LocationInfo();
            location.setIp(jsonObject.has("query") ? jsonObject.get("query").getAsString() : ip);
            location.setCountryName(jsonObject.has("country") ? jsonObject.get("country").getAsString() : "Inconnu");
            location.setCountryCode(jsonObject.has("countryCode") ? jsonObject.get("countryCode").getAsString() : "XX");
            location.setRegion(jsonObject.has("region") ? jsonObject.get("region").getAsString() : "");
            location.setCity(jsonObject.has("city") ? jsonObject.get("city").getAsString() : "Inconnue");
            location.setLatitude(jsonObject.has("lat") ? jsonObject.get("lat").getAsDouble() : 0);
            location.setLongitude(jsonObject.has("lon") ? jsonObject.get("lon").getAsDouble() : 0);
            location.setTimezone(jsonObject.has("timezone") ? jsonObject.get("timezone").getAsString() : "");
            location.setIsp(jsonObject.has("isp") ? jsonObject.get("isp").getAsString() : "");

            System.out.println("✅ Localisation trouvée: " + location.getFormattedLocation());

            // Mettre en cache
            cachedLocation = location;
            lastCallTime = now;

            return location;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la géolocalisation: " + e.getMessage());
            return getTestLocation();
        }
    }

    /**
     * Retourne une localisation de test réaliste
     */
    private static LocationInfo getTestLocation() {
        LocationInfo testLoc = new LocationInfo();
        testLoc.setIp("8.8.8.8");
        testLoc.setCountryName("États-Unis");
        testLoc.setCountryCode("US");
        testLoc.setCity("Mountain View");
        testLoc.setRegion("California");
        testLoc.setLatitude(37.386);
        testLoc.setLongitude(-122.0838);
        testLoc.setTimezone("America/Los_Angeles");
        testLoc.setIsp("Google LLC");
        System.out.println("🌍 Utilisation de la localisation de test: États-Unis, Mountain View");
        return testLoc;
    }
}
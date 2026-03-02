package org.example.Services.activite;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 🌤️ Service de météo avec OpenWeatherMap
 * API GRATUITE : 1000 appels/jour
 */
public class WeatherServiceImpl  {

    private static final String API_KEY = "b48f0e629cb83f8895f6f75175cd1e69";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5";

    /**
     * Obtient la météo actuelle pour un lieu
     */
    public WeatherData getCurrentWeather(String cityName) {
        try {
            String encodedCity = URLEncoder.encode(cityName + ",TN", StandardCharsets.UTF_8);
            String urlString = BASE_URL + "/weather?q=" + encodedCity +
                    "&appid=" + API_KEY + "&units=metric&lang=fr";

            String response = makeRequest(urlString);
            return parseCurrentWeather(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur météo : " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtient les prévisions sur 5 jours
     */
    public List<WeatherForecast> getForecast(String cityName) {
        try {
            String encodedCity = URLEncoder.encode(cityName + ",TN", StandardCharsets.UTF_8);
            String urlString = BASE_URL + "/forecast?q=" + encodedCity +
                    "&appid=" + API_KEY + "&units=metric&lang=fr";

            String response = makeRequest(urlString);
            return parseForecast(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur prévisions : " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Vérifie si la météo est favorable pour une activité
     */
    public WeatherRecommendation getRecommendation(String cityName, String activityType) {
        WeatherData weather = getCurrentWeather(cityName);
        if (weather == null) return null;

        WeatherRecommendation rec = new WeatherRecommendation();
        rec.setWeather(weather);

        // Logique de recommandation selon le type d'activité
        switch (activityType.toLowerCase()) {
            case "sport":
            case "aventure":
            case "excursion":
                if (weather.getTemperature() < 10) {
                    rec.setRecommended(false);
                    rec.setReason("❄️ Température trop froide (" + weather.getTemperature() + "°C)");
                } else if (weather.getTemperature() > 35) {
                    rec.setRecommended(false);
                    rec.setReason("🌡️ Température trop chaude (" + weather.getTemperature() + "°C)");
                } else if (weather.getCondition().contains("pluie") || weather.getCondition().contains("orage")) {
                    rec.setRecommended(false);
                    rec.setReason("🌧️ Conditions météo défavorables : " + weather.getCondition());
                } else {
                    rec.setRecommended(true);
                    rec.setReason("✅ Conditions idéales pour cette activité !");
                }
                break;

            case "culture":
            case "détente":
                // Ces activités sont OK en intérieur
                if (weather.getCondition().contains("orage")) {
                    rec.setRecommended(false);
                    rec.setReason("⚡ Orage prévu, déplacements difficiles");
                } else {
                    rec.setRecommended(true);
                    rec.setReason("✅ Activité praticable par tout temps");
                }
                break;

            default:
                rec.setRecommended(true);
                rec.setReason("ℹ️ Vérifiez la météo selon vos besoins");
        }

        return rec;
    }

    /**
     * Effectue une requête HTTP
     */
    private String makeRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();

        if (responseCode != 200) {
            // Read error stream to see what API returned
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8)
            );
            StringBuilder error = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) error.append(line);
            reader.close();
            throw new Exception("HTTP " + responseCode + " : " + error.toString());
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
        );
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) response.append(line);
        reader.close();

        return response.toString();
    }

    public WeatherData getCurrentWeatherByCoords(double lat, double lon) {
        try {
            String urlString = BASE_URL + "/weather?lat=" + lat + "&lon=" + lon +
                    "&appid=" + API_KEY + "&units=metric&lang=fr";
            String response = makeRequest(urlString);
            return parseCurrentWeather(response);
        } catch (Exception e) {
            System.err.println("❌ Erreur météo coords : " + e.getMessage());
            return null;
        }
    }
    /**
     * Parse la météo actuelle
     */
    private WeatherData parseCurrentWeather(String json) {
        JSONObject obj = new JSONObject(json);

        WeatherData data = new WeatherData();
        data.setCityName(obj.getString("name"));
        data.setTemperature(obj.getJSONObject("main").getDouble("temp"));
        data.setFeelsLike(obj.getJSONObject("main").getDouble("feels_like"));
        data.setHumidity(obj.getJSONObject("main").getInt("humidity"));
        data.setWindSpeed(obj.getJSONObject("wind").getDouble("speed"));

        JSONObject weather = obj.getJSONArray("weather").getJSONObject(0);
        data.setCondition(weather.getString("description"));
        data.setIcon(weather.getString("icon"));

        return data;
    }

    /**
     * Parse les prévisions
     */
    private List<WeatherForecast> parseForecast(String json) {
        List<WeatherForecast> forecasts = new ArrayList<>();
        JSONObject obj = new JSONObject(json);
        JSONArray list = obj.getJSONArray("list");

        // Prendre une prévision par jour (midi)
        for (int i = 0; i < Math.min(list.length(), 40); i += 8) {
            JSONObject item = list.getJSONObject(i);

            WeatherForecast forecast = new WeatherForecast();
            forecast.setDateTime(item.getString("dt_txt"));
            forecast.setTemperature(item.getJSONObject("main").getDouble("temp"));
            forecast.setCondition(item.getJSONArray("weather").getJSONObject(0).getString("description"));
            forecast.setIcon(item.getJSONArray("weather").getJSONObject(0).getString("icon"));

            forecasts.add(forecast);
        }

        return forecasts;
    }

    public WeatherRecommendation getRecommendationByCoords(double lat, double lon, String activityType) {
        WeatherData weather = getCurrentWeatherByCoords(lat, lon); // ← retourne WeatherData
        if (weather == null) return null;

        WeatherRecommendation rec = new WeatherRecommendation();
        rec.setWeather(weather); // ← setWeather attend un WeatherData ✅

        switch (activityType.toLowerCase()) {
            case "sport":
            case "aventure":
            case "excursion":
                if (weather.getTemperature() < 10) {
                    rec.setRecommended(false);
                    rec.setReason("❄️ Température trop froide (" + (int)weather.getTemperature() + "°C)");
                } else if (weather.getTemperature() > 35) {
                    rec.setRecommended(false);
                    rec.setReason("🌡️ Température trop chaude (" + (int)weather.getTemperature() + "°C)");
                } else if (weather.getCondition().contains("pluie") || weather.getCondition().contains("orage")) {
                    rec.setRecommended(false);
                    rec.setReason("🌧️ Conditions météo défavorables : " + weather.getCondition());
                } else {
                    rec.setRecommended(true);
                    rec.setReason("✅ Conditions idéales pour cette activité !");
                }
                break;
            case "gastronomie":
            case "bien-être":
            case "bien-etre":
            case "detente":
            case "détente":
            case "culture":
                // Activités d'intérieur ou peu sensibles à la météo
                if (weather.getCondition().contains("orage")) {
                    rec.setRecommended(false);
                    rec.setReason("⚡ Orage prévu, déplacements difficiles");
                } else {
                    rec.setRecommended(true);
                    rec.setReason("✅ Activité praticable par tout temps");
                }
                break;
            case "plage":
            case "plongée":
            case "plongee":
            case "nautique":
                if (weather.getTemperature() < 18) {
                    rec.setRecommended(false);
                    rec.setReason("🌊 Température trop fraîche pour une activité aquatique (" + (int)weather.getTemperature() + "°C)");
                } else if (weather.getCondition().contains("orage") || weather.getCondition().contains("pluie")) {
                    rec.setRecommended(false);
                    rec.setReason("🌧️ Mer agitée probable, activité déconseillée");
                } else {
                    rec.setRecommended(true);
                    rec.setReason("🌊 Parfait pour une activité en mer !");
                }
                break;
            default:
                rec.setRecommended(true);
                rec.setReason("ℹ️ Vérifiez la météo selon vos besoins");
        }

        return rec;
    }

    // ========== CLASSES DE DONNÉES ==========

    public static class WeatherData {
        private String cityName;
        private double temperature;
        private double feelsLike;
        private int humidity;
        private double windSpeed;
        private String condition;
        private String icon;

        // Getters et Setters
        public String getCityName() { return cityName; }
        public void setCityName(String cityName) { this.cityName = cityName; }

        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }

        public double getFeelsLike() { return feelsLike; }
        public void setFeelsLike(double feelsLike) { this.feelsLike = feelsLike; }

        public int getHumidity() { return humidity; }
        public void setHumidity(int humidity) { this.humidity = humidity; }

        public double getWindSpeed() { return windSpeed; }
        public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }

        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }

        public String getIconUrl() {
            return "https://openweathermap.org/img/wn/" + icon + "@2x.png";
        }

        public String getEmoji() {
            if (condition.contains("ensoleillé") || condition.contains("clair")) return "☀️";
            if (condition.contains("nuage")) return "☁️";
            if (condition.contains("pluie")) return "🌧️";
            if (condition.contains("orage")) return "⛈️";
            if (condition.contains("neige")) return "❄️";
            if (condition.contains("brouillard")) return "🌫️";
            return "🌤️";
        }
    }

    public static class WeatherForecast {
        private String dateTime;
        private double temperature;
        private String condition;
        private String icon;

        // Getters et Setters
        public String getDateTime() { return dateTime; }
        public void setDateTime(String dateTime) { this.dateTime = dateTime; }

        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }

        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }

        public String getDay() {
            // Format : "2024-03-15 12:00:00" → "Vendredi"
            try {
                LocalDateTime dt = LocalDateTime.parse(dateTime.replace(" ", "T"));
                return dt.format(DateTimeFormatter.ofPattern("EEEE"));
            } catch (Exception e) {
                return dateTime.split(" ")[0];
            }
        }
    }

    public static class WeatherRecommendation {
        private WeatherData weather;
        private boolean recommended;
        private String reason;

        // Getters et Setters
        public WeatherData getWeather() { return weather; }
        public void setWeather(WeatherData weather) { this.weather = weather; }

        public boolean isRecommended() { return recommended; }
        public void setRecommended(boolean recommended) { this.recommended = recommended; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

    }
}
package org.example.Services.user.facelogin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class FaceApiClient {

    private final String baseUrl; // ex: http://127.0.0.1:8000
    private final HttpClient http;
    private final Gson gson = new Gson();

    public FaceApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void health() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/health"))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() != 200) {
            throw new IOException("Face API health failed: " + res.statusCode() + " " + res.body());
        }
    }

    public EnrollResult enroll(String userId, String imageBase64) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("userId", userId);
        body.addProperty("imageBase64", imageBase64);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/enroll"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body), StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() != 200) {
            throw new IOException("Enroll failed: " + res.statusCode() + " " + res.body());
        }

        JsonObject json = gson.fromJson(res.body(), JsonObject.class);
        EnrollResult r = new EnrollResult();
        r.success = json.has("success") && json.get("success").getAsBoolean();
        r.userId = json.has("userId") && !json.get("userId").isJsonNull() ? json.get("userId").getAsString() : null;
        return r;
    }

    public LoginResult login(String imageBase64, double threshold) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("imageBase64", imageBase64);
        body.addProperty("threshold", threshold);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body), StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() != 200) {
            throw new IOException("Login failed: " + res.statusCode() + " " + res.body());
        }

        JsonObject json = gson.fromJson(res.body(), JsonObject.class);
        LoginResult r = new LoginResult();
        r.success = json.has("success") && json.get("success").getAsBoolean();
        r.userId = json.has("userId") && !json.get("userId").isJsonNull() ? json.get("userId").getAsString() : null;
        r.score = json.has("score") && !json.get("score").isJsonNull() ? json.get("score").getAsDouble() : null;
        return r;
    }

    public static class EnrollResult {
        public boolean success;
        public String userId;
    }

    public static class LoginResult {
        public boolean success;
        public String userId;
        public Double score;
    }
}
package org.example.Services.user.facelogin;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class FaceIdService {

    private static final String BASE_URL = "http://127.0.0.1:5000";
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    // DTO
    public record FaceResult(boolean success, String message, String username, double confidence) {}

    // ─── Health Check ────────────────────────────────────────────────────────
    public boolean isServerRunning() {
        try {
            Request request = new Request.Builder()
                    .url(BASE_URL + "/health")
                    .get()
                    .build();
            Response response = client.newCall(request).execute();
            return response.isSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    // ─── Register Face ───────────────────────────────────────────────────────
    public FaceResult registerFace(String username, byte[] imageBytes) throws IOException {
        String b64 = Base64.getEncoder().encodeToString(imageBytes);

        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("image", b64);

        Request request = new Request.Builder()
                .url(BASE_URL + "/register_face")
                .post(RequestBody.create(body.toString(), JSON_TYPE))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String raw = response.body().string();
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            boolean success = json.get("success").getAsBoolean();
            String message = json.get("message").getAsString();
            return new FaceResult(success, message, null, 0);
        }
    }

    // ─── Login with Face ─────────────────────────────────────────────────────
    public FaceResult loginWithFace(byte[] imageBytes) throws IOException {
        String b64 = Base64.getEncoder().encodeToString(imageBytes);

        JsonObject body = new JsonObject();
        body.addProperty("image", b64);

        Request request = new Request.Builder()
                .url(BASE_URL + "/login_face")
                .post(RequestBody.create(body.toString(), JSON_TYPE))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String raw = response.body().string();
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            boolean success = json.get("success").getAsBoolean();

            if (success) {
                String username = json.get("username").getAsString();
                double confidence = json.get("confidence").getAsDouble();
                return new FaceResult(true, "Login successful", username, confidence);
            } else {
                String message = json.get("message").getAsString();
                return new FaceResult(false, message, null, 0);
            }
        }
    }

    // ─── Disable Face ID ─────────────────────────────────────────────────────
    public FaceResult disableFace(String username) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);

        Request request = new Request.Builder()
                .url(BASE_URL + "/disable_face")
                .post(RequestBody.create(body.toString(), JSON_TYPE))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String raw = response.body().string();
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            boolean success = json.get("success").getAsBoolean();
            return new FaceResult(success, success ? "Face ID disabled" : "Failed", null, 0);
        }
    }
}

package org.example.Controllers.user;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.Entites.user.User;
import org.example.Services.user.UserCRUD;
import org.example.Services.user.facelogin.FaceApiClient;
import org.example.Services.user.facelogin.ImageBase64;
import org.example.Utils.UserSession;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class FaceLoginController {

    @FXML private ImageView cameraView;
    @FXML private Label statusLabel;

    private Webcam webcam;
    private volatile boolean running = false;

    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final FaceApiClient api = new FaceApiClient("http://127.0.0.1:8000");
    private final UserCRUD userCRUD = new UserCRUD();

    @FXML
    public void initialize() {
        System.out.println("Java version = " + System.getProperty("java.version"));
        statusLabel.setText("Initialisation de la webcam...");
        startCameraLoop();

        Platform.runLater(() -> {
            Stage stage = (Stage) cameraView.getScene().getWindow();
            stage.setOnCloseRequest(e -> shutdown());
        });
    }

    private void startCameraLoop() {
        Task<Void> t = new Task<>() {
            @Override
            protected Void call() throws Exception {
                webcam = Webcam.getDefault();
                if (webcam == null) {
                    throw new IllegalStateException("Aucune webcam détectée");
                }

                webcam.setViewSize(WebcamResolution.VGA.getSize());
                webcam.open();

                running = true;
                Platform.runLater(() ->
                        statusLabel.setText("Webcam ON. Cadre ton visage puis clique LOGIN.")
                );

                while (running) {
                    BufferedImage frame = webcam.getImage();
                    if (frame != null) {
                        Image fx = bufferedToFxImage(frame);
                        Platform.runLater(() -> cameraView.setImage(fx));
                    }
                    Thread.sleep(33);
                }
                return null;
            }
        };

        t.setOnFailed(e -> {
            Throwable ex = t.getException();
            if (ex != null) ex.printStackTrace();
            Platform.runLater(() -> statusLabel.setText("Erreur webcam: " + (ex != null ? ex.getMessage() : "unknown")));
        });

        executor.submit(t);
    }

    private static Image bufferedToFxImage(BufferedImage img) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return new Image(new ByteArrayInputStream(out.toByteArray()));
    }

    private BufferedImage snapshot() {
        if (webcam == null || !webcam.isOpen()) return null;
        return webcam.getImage();
    }

    @FXML
    private void onEnrollFromSession() {
        System.out.println("[FaceLoginController] onEnrollFromSession clicked");

        User current = UserSession.getInstance().getCurrentUser();
        if (current == null || current.getE_mail() == null || current.getE_mail().isBlank()) {
            statusLabel.setText("Tu dois être connecté (login normal) pour activer FaceID.");
            return;
        }

        BufferedImage img = snapshot();
        if (img == null) {
            statusLabel.setText("Impossible de capturer une image de la webcam.");
            return;
        }

        String email = current.getE_mail();
        statusLabel.setText("Activation FaceID pour " + email + "...");

        Task<Void> t = new Task<>() {
            @Override
            protected Void call() throws Exception {
                System.out.println("[FaceLoginController] calling api.health()");
                api.health();

                String b64 = ImageBase64.toJpegBase64(img, 0.85f);
                System.out.println("[FaceLoginController] enroll imageBase64 length=" + b64.length());

                FaceApiClient.EnrollResult res = api.enroll(email, b64);

                Platform.runLater(() -> {
                    if (res.success) statusLabel.setText("FaceID activé ✅ (" + res.userId + ")");
                    else statusLabel.setText("Activation FaceID échouée.");
                });
                return null;
            }
        };

        t.setOnFailed(e -> {
            Throwable ex = t.getException();
            if (ex != null) ex.printStackTrace();
            Platform.runLater(() -> statusLabel.setText("Erreur activation: " + (ex != null ? ex.getMessage() : "unknown")));
        });

        executor.submit(t);
    }

    @FXML
    private void onLogin() {
        System.out.println("[FaceLoginController] onLogin clicked");

        BufferedImage img = snapshot();
        if (img == null) {
            statusLabel.setText("Impossible de capturer une image de la webcam.");
            return;
        }

        statusLabel.setText("Login FaceID en cours...");

        Task<Void> t = new Task<>() {
            @Override
            protected Void call() throws Exception {
                System.out.println("[FaceLoginController] calling api.health()");
                api.health();

                String b64 = ImageBase64.toJpegBase64(img, 0.85f);
                System.out.println("[FaceLoginController] login imageBase64 length=" + b64.length());

                FaceApiClient.LoginResult res = api.login(b64, 0.45);
                System.out.println("[FaceLoginController] api.login result success=" + res.success + " userId=" + res.userId + " score=" + res.score);

                if (!res.success || res.userId == null) {
                    Platform.runLater(() -> statusLabel.setText("LOGIN FAIL ❌ (score=" + res.score + ")"));
                    return null;
                }

                // IMPORTANT: DB lookup hors thread UI
                User u = userCRUD.getUserByEmail(res.userId);

                Platform.runLater(() -> {
                    if (u == null) {
                        statusLabel.setText("FaceID OK mais user introuvable: " + res.userId);
                        return;
                    }

                    UserSession.getInstance().setCurrentUser(u);
                    statusLabel.setText("LOGIN OK ✅ : " + u.getPrenom() + " " + u.getNom());
                    goToHome();
                });

                return null;
            }
        };

        t.setOnFailed(e -> {
            Throwable ex = t.getException();
            if (ex != null) ex.printStackTrace();
            Platform.runLater(() -> statusLabel.setText("Erreur Login: " + (ex != null ? ex.getMessage() : "unknown")));
        });

        executor.submit(t);
    }

    private void goToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/dashboard/homeClient.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) cameraView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Re7la - Home");

            shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Navigation error: " + e.getMessage());
        }
    }

    public void shutdown() {
        running = false;
        executor.shutdownNow();
        if (webcam != null) {
            try { webcam.close(); } catch (Exception ignored) {}
        }
    }
}
package org.example.Controllers.user;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.Entites.user.Role;
import org.example.Entites.user.User;
import org.example.Services.user.CameraService;
import org.example.Services.user.UserCRUD;
import org.example.Services.user.facelogin.FaceIdService;
import org.example.Utils.UserSession;

/**
 * FaceLoginController — drives the Face ID login / enroll screen.
 *
 * Camera backend: OpenCV via CameraService (sarxos removed entirely to fix
 * the "Timeout when requesting image!" error on ASUS / modern USB webcams).
 *
 * Modes
 * ─────
 *   LOGIN  (default) – identify the user and navigate home on success.
 *   ENROLL           – register / update the face for a logged-in user.
 *                      Call initForEnroll(user) before showing the stage.
 */
public class FaceLoginController {

    // ─── Mode ─────────────────────────────────────────────────────────────────
    public enum Mode { LOGIN, ENROLL }

    // ─── FXML nodes ───────────────────────────────────────────────────────────
    @FXML private ImageView cameraView;
    @FXML private Label     statusLabel;
    @FXML private Button    captureBtn;

    // ─── Services ─────────────────────────────────────────────────────────────
    private final CameraService cameraService = new CameraService();
    private final FaceIdService faceIdService = new FaceIdService();
    private final UserCRUD userCRUD = new UserCRUD();

    // ─── State ────────────────────────────────────────────────────────────────
    private Mode mode       = Mode.LOGIN;
    private User enrollUser = null;

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setStatus("Démarrage de la caméra...", "info");

        boolean opened = cameraService.start(cameraView);
        if (opened) {
            setStatus(mode == Mode.LOGIN
                            ? "Caméra prête. Cadre ton visage et clique LOGIN."
                            : "Caméra prête. Cadre ton visage et clique ENREGISTRER.",
                    "info");
        } else {
            setStatus("❌ Impossible d'ouvrir la caméra. Vérifie les permissions.", "error");
            if (captureBtn != null) captureBtn.setDisable(true);
        }

        // Clean shutdown when window is closed
        Platform.runLater(() -> {
            if (cameraView.getScene() != null && cameraView.getScene().getWindow() != null) {
                cameraView.getScene().getWindow().setOnCloseRequest(e -> shutdown());
            }
        });
    }

    /**
     * Call this BEFORE the stage is shown to switch to ENROLL mode.
     *
     * Example from UserProfileController:
     *   FaceLoginController ctrl = loader.getController();
     *   ctrl.initForEnroll(currentUser);
     */
    public void initForEnroll(User user) {
        this.mode       = Mode.ENROLL;
        this.enrollUser = user;
        // captureBtn may be null if FXML hasn't loaded yet; initialize() applies it too
        if (captureBtn != null) captureBtn.setText("ENREGISTRER 📸");
    }

    // ─── FXML actions ─────────────────────────────────────────────────────────

    /** Unified action — wire your FXML button to #onCaptureFace */
    @FXML
    private void onCaptureFace() {
        if (mode == Mode.ENROLL) doEnroll();
        else                     doLogin();
    }

    /** Legacy alias so existing FXML using onAction="#onLogin" still works */
    @FXML
    private void onLogin() { doLogin(); }

    // ─── Login flow ───────────────────────────────────────────────────────────

    private void doLogin() {
        byte[] frame = cameraService.captureFrame();
        if (frame == null) {
            setStatus("❌ Impossible de capturer une image.", "error");
            return;
        }
        if (!faceIdService.isServerRunning()) {
            setStatus("⚠ Serveur Face ID hors ligne (http://127.0.0.1:5000).", "error");
            return;
        }

        captureBtn.setDisable(true);
        setStatus("Authentification en cours...", "info");

        new Thread(() -> {
            try {
                FaceIdService.FaceResult result = faceIdService.loginWithFace(frame);
                Platform.runLater(() -> {
                    captureBtn.setDisable(false);
                    if (result.success()) {
                        setStatus("✓ Bienvenue " + result.username()
                                        + "! (" + String.format("%.1f", result.confidence()) + "%)",
                                "success");
                     User u =   userCRUD.getUserByEmail(result.username());
                            UserSession.getInstance().setCurrentUser(u);

                        System.out.println("✅ Face login success — email: " + result.username());

                        shutdown();
                        if(u.getRole() == Role.admin){
                            try {
                                FXMLLoader loader = new FXMLLoader(
                                        getClass().getResource("/user/back/users.fxml"));
                                Parent root = loader.load();
                                Stage stage = (Stage) cameraView.getScene().getWindow();
                                stage.setScene(new Scene(root));
                                stage.setTitle("Re7la - Accueil");
                            } catch (Exception e) {
                                e.printStackTrace();
                                setStatus("Erreur de navigation: " + e.getMessage(), "error");
                            }
                        }
                        navigateToHome();
                    } else {
                        setStatus("✗ " + result.message() + ". Réessayez.", "error");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    captureBtn.setDisable(false);
                    setStatus("Erreur: " + e.getMessage(), "error");
                });
            }
        }).start();
    }

    // ─── Enroll flow ──────────────────────────────────────────────────────────

    private void doEnroll() {
        if (enrollUser == null) {
            setStatus("❌ Aucun utilisateur en session.", "error");
            return;
        }

        byte[] frame = cameraService.captureFrame();
        if (frame == null) {
            setStatus("❌ Impossible de capturer une image.", "error");
            return;
        }
        if (!faceIdService.isServerRunning()) {
            setStatus("⚠ Serveur Face ID hors ligne (http://127.0.0.1:5000).", "error");
            return;
        }

        captureBtn.setDisable(true);
        setStatus("Enregistrement du visage...", "info");

        String faceKey = enrollUser.getE_mail(); // unique face identifier

        new Thread(() -> {
            try {
                System.out.println("Registering face for " + faceKey + " ("  + " bytes)");
                FaceIdService.FaceResult result = faceIdService.registerFace(faceKey, frame);
                Platform.runLater(() -> {
                    captureBtn.setDisable(false);
                    if (result.success()) {
                        setStatus("✓ Face ID enregistré avec succès !", "success");
                        captureBtn.setText("✓ Mettre à jour");
                        new Thread(() -> {
                            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                            Platform.runLater(this::closeStage);
                        }).start();
                    } else {
                        setStatus("✗ " + result.message() + ". Réessayez.", "error");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    captureBtn.setDisable(false);
                    setStatus("Erreur: " + e.getMessage(), "error");
                });
            }
        }).start();

    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private void navigateToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/user/dashboard/homeClient.fxml"));
            Parent root = loader.load();

            // ✅ Récupérer la stage actuelle
            Stage stage = (Stage) cameraView.getScene().getWindow();

            // ✅ Créer une nouvelle scène
            Scene scene = new Scene(root);

            // ✅ Ajouter le CSS si nécessaire
            try {
                String css = getClass().getResource("/user/dashboard/homeClient.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("⚠️ CSS non trouvé");
            }

            // ✅ Changer la scène (l'ancienne interface est remplacée)
            stage.setScene(scene);
            stage.setTitle("Re7la - Accueil");
            stage.centerOnScreen(); // ✅ Centrer la fenêtre
            stage.show(); // ✅ Afficher la nouvelle scène

        } catch (Exception e) {
            e.printStackTrace();
            setStatus("Erreur de navigation: " + e.getMessage(), "error");
        }
    }


    private void closeStage() {
        try { ((Stage) cameraView.getScene().getWindow()).close(); }
        catch (Exception ignored) {}
    }

    // ─── Shutdown ─────────────────────────────────────────────────────────────

    public void shutdown() { cameraService.stop(); }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void setStatus(String msg, String type) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        switch (type) {
            case "error"   -> statusLabel.setStyle("-fx-text-fill: #E74C3C;");
            case "success" -> statusLabel.setStyle("-fx-text-fill: #27AE60;");
            default        -> statusLabel.setStyle("-fx-text-fill: #888888;");
        }
    }
}
package org.example.Services.promotion;

import animatefx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Service centralisé pour toutes les notifications de l'app RE7LA.
 * Utilise AnimateFX pour des entrées/sorties animées élégantes.
 *
 * Types :
 *  - SUCCESS (turquoise)
 *  - WARNING (orange)
 *  - INFO    (bleu nuit)
 *  - DANGER  (rouge)
 *  - PROMO   (gradient special)
 */
public class NotificationService {

    // ═══ Singleton ═══
    private static NotificationService instance;
    public static NotificationService getInstance() {
        if (instance == null) instance = new NotificationService();
        return instance;
    }

    // ═══ Palette RE7LA ═══
    private static final String ORANGE    = "#F39C12";
    private static final String JAUNE     = "#F7DC6F";
    private static final String BLEU_NUIT = "#2C3E50";
    private static final String TURQUOISE = "#1ABC9C";
    private static final String DANGER    = "#E53E3E";
    private static final String WHITE     = "#ffffff";
    private static final String BG        = "#eef2f6";

    public enum Type { SUCCESS, WARNING, INFO, DANGER, PROMO }

    // ═══════════════════════════════════════════════════
    // MÉTHODE PRINCIPALE — toujours appelée depuis JavaFX thread
    // ═══════════════════════════════════════════════════

    /**
     * Affiche une notification animée.
     * Peut être appelée depuis n'importe quel thread (Quartz inclus).
     *
     * @param type    Type de notification
     * @param title   Titre en gras
     * @param message Message détaillé
     * @param autoCloseSec  0 = pas d'autoclose, >0 = fermeture auto en N secondes
     */
    public void show(Type type, String title, String message, int autoCloseSec) {
        Platform.runLater(() -> showOnFxThread(type, title, message, autoCloseSec));
    }

    private void showOnFxThread(Type type, String title, String message, int autoCloseSec) {
        Stage notifStage = new Stage(StageStyle.TRANSPARENT);
        notifStage.setAlwaysOnTop(true);

        // ── Couleurs selon type ──
        String bgColor, borderColor, iconText;
        switch (type) {
            case SUCCESS -> { bgColor = TURQUOISE; borderColor = "#0E8E76"; iconText = "✅"; }
            case WARNING -> { bgColor = ORANGE;    borderColor = "#C87A00"; iconText = "⚠️"; }
            case DANGER  -> { bgColor = DANGER;    borderColor = "#B91C1C"; iconText = "🚨"; }
            case PROMO   -> { bgColor = BLEU_NUIT; borderColor = ORANGE;   iconText = "🎁"; }
            default      -> { bgColor = BLEU_NUIT; borderColor = "#1A2A3A"; iconText = "ℹ️"; }
        }

        // ── ROOT ──
        VBox root = new VBox(10);
        root.setPrefWidth(380);
        root.setMaxWidth(380);
        root.setPadding(new Insets(18, 20, 18, 20));
        root.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-width: 2;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 20, 0, 0, 6);"
        );

        // ── Header : icône + titre + fermer ──
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(iconText);
        icon.setStyle("-fx-font-size: 22px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: 900;" +
                        "-fx-text-fill: white; -fx-wrap-text: true;"
        );
        titleLabel.setMaxWidth(270);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.25);" +
                        "-fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-padding: 2 7; -fx-background-radius: 20; -fx-cursor: hand;" +
                        "-fx-font-size: 11px;"
        );
        closeBtn.setOnAction(e -> closeAnimated(notifStage, root));

        header.getChildren().addAll(icon, titleLabel, spacer, closeBtn);

        // ── Message ──
        Label msgLabel = new Label(message);
        msgLabel.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.9);" +
                        "-fx-wrap-text: true;"
        );
        msgLabel.setMaxWidth(340);

        // ── Barre de progression (si autoClose) ──
        if (autoCloseSec > 0) {
            javafx.scene.control.ProgressBar pb = new javafx.scene.control.ProgressBar(1.0);
            pb.setMaxWidth(Double.MAX_VALUE);
            pb.setStyle("-fx-accent: rgba(255,255,255,0.5); -fx-pref-height: 3;");

            // Animate progress bar
            javafx.animation.Timeline progressAnim = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(Duration.ZERO,
                            new javafx.animation.KeyValue(pb.progressProperty(), 1.0)),
                    new javafx.animation.KeyFrame(Duration.seconds(autoCloseSec),
                            new javafx.animation.KeyValue(pb.progressProperty(), 0.0))
            );
            progressAnim.play();

            root.getChildren().addAll(header, msgLabel, pb);
        } else {
            root.getChildren().addAll(header, msgLabel);
        }

        // ── Scène transparente ──
        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        notifStage.setScene(scene);

        // ── Position : coin bas-droite de l'écran ──
        javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();
        notifStage.setX(screen.getMaxX() - 400);
        notifStage.setY(screen.getMaxY() - 140);
        notifStage.show();

        // ── Animation entrée (AnimateFX) ──
        new SlideInRight(root).play();

        // ── AutoClose ──
        if (autoCloseSec > 0) {
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    Duration.seconds(autoCloseSec)
            );
            pause.setOnFinished(e -> closeAnimated(notifStage, root));
            pause.play();
        }
    }

    private void closeAnimated(Stage stage, VBox root) {
        SlideOutRight slideOut = new SlideOutRight(root);
        slideOut.setOnFinished(e -> stage.close());
        slideOut.play();
    }

    // ═══════════════════════════════════════════════════
    // SHORTCUTS PRATIQUES
    // ═══════════════════════════════════════════════════

    public void success(String title, String message) {
        show(Type.SUCCESS, title, message, 4);
    }

    public void warning(String title, String message) {
        show(Type.WARNING, title, message, 0);
    }

    public void info(String title, String message) {
        show(Type.INFO, title, message, 4);
    }

    public void danger(String title, String message) {
        show(Type.DANGER, title, message, 0);
    }

    public void promo(String title, String message) {
        show(Type.PROMO, title, message, 6);
    }

    // ═══════════════════════════════════════════════════
    // DIALOG MODAL (pour les alertes Quartz importantes)
    // Bloquant, centré, avec AnimateFX BounceIn
    // ═══════════════════════════════════════════════════
    public void showModal(Type type, String title, String message) {
        Platform.runLater(() -> {
            Stage modal = new Stage();
            modal.setAlwaysOnTop(true);
            modal.setTitle(title);

            String bgColor, iconText;
            switch (type) {
                case SUCCESS -> { bgColor = TURQUOISE; iconText = "✅"; }
                case WARNING -> { bgColor = ORANGE;    iconText = "⚠️"; }
                case DANGER  -> { bgColor = DANGER;    iconText = "🚨"; }
                case PROMO   -> { bgColor = BLEU_NUIT; iconText = "🎁"; }
                default      -> { bgColor = BLEU_NUIT; iconText = "ℹ️"; }
            }

            VBox root = new VBox(20);
            root.setAlignment(Pos.CENTER);
            root.setStyle("-fx-background-color: #eef2f6; -fx-padding: 35;");

            Label iconLbl = new Label(iconText);
            iconLbl.setStyle("-fx-font-size: 52px;");

            Label titleLbl = new Label(title);
            titleLbl.setStyle(
                    "-fx-font-size: 20px; -fx-font-weight: 900;" +
                            "-fx-text-fill: " + BLEU_NUIT + ";"
            );

            Label msgLbl = new Label(message);
            msgLbl.setStyle(
                    "-fx-font-size: 13px; -fx-text-fill: #64748B;" +
                            "-fx-wrap-text: true; -fx-text-alignment: center;"
            );
            msgLbl.setMaxWidth(320);
            msgLbl.setAlignment(Pos.CENTER);

            // Bande colorée en haut
            javafx.scene.layout.HBox topBar = new javafx.scene.layout.HBox();
            topBar.setPrefHeight(6);
            topBar.setStyle("-fx-background-color: " + bgColor + ";");

            Button okBtn = new Button("  OK  ");
            okBtn.setStyle(
                    "-fx-background-color: " + bgColor + ";" +
                            "-fx-text-fill: white; -fx-font-weight: 700;" +
                            "-fx-padding: 10 35; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 13px;"
            );
            okBtn.setOnAction(e -> modal.close());

            root.getChildren().addAll(iconLbl, titleLbl, msgLbl, okBtn);

            VBox wrapper = new VBox(0, topBar, root);

            Scene scene = new Scene(wrapper, 400, 340);
            modal.setScene(scene);
            modal.show();

            // BounceIn animation
            new BounceIn(root).play();
        });
    }
}

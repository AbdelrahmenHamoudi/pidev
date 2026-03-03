package org.example.Controllers.user;


import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import java.util.Random;

public class AlphabetCaptchaController {

    private VBox captchaBox;
    private Canvas canvas;
    private TextField captchaInput;
    private Label resultLabel;
    private Button refreshButton;
    private Button verifyButton;

    private String captchaText;
    private boolean isVerified = false;

    private CaptchaCallback callback;

    public interface CaptchaCallback {
        void onSuccess();
        void onError(String error);
    }

    public AlphabetCaptchaController(CaptchaCallback callback) {
        this.callback = callback;
        createCaptcha();
    }

    private void createCaptcha() {
        captchaBox = new VBox(10);
        captchaBox.setAlignment(Pos.CENTER);
        captchaBox.setPadding(new Insets(15));
        captchaBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 15; -fx-border-color: #1ABC9C; -fx-border-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        // Titre
        Label titleLabel = new Label("🔐 Recopiez les lettres ci-dessous");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#2C3E50"));

        // Canvas pour l'image CAPTCHA
        canvas = new Canvas(250, 70);
        generateNewCaptcha();

        // HBox pour les boutons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        // Champ de saisie
        HBox inputBox = new HBox(10);
        inputBox.setAlignment(Pos.CENTER);

        Label inputLabel = new Label("Saisir :");
        inputLabel.setFont(Font.font("Arial", 12));
        inputLabel.setTextFill(Color.web("#2C3E50"));

        captchaInput = new TextField();
        captchaInput.setPromptText("Entrez les lettres");
        captchaInput.setPrefWidth(150);
        captchaInput.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #BDC3C7; -fx-font-size: 14px; -fx-padding: 8;");

        inputBox.getChildren().addAll(inputLabel, captchaInput);

        // Bouton de vérification
        verifyButton = new Button("✓ Vérifier");
        verifyButton.setStyle("-fx-background-color: #F39C12; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 8 20; -fx-cursor: hand;");
        verifyButton.setOnAction(e -> verifyCaptcha());

        // Bouton de rafraîchissement
        refreshButton = new Button("🔄 Nouveau code");
        refreshButton.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 8 20; -fx-cursor: hand;");
        refreshButton.setOnAction(e -> refreshCaptcha());

        buttonBox.getChildren().addAll(verifyButton, refreshButton);

        // Label pour le résultat
        resultLabel = new Label();
        resultLabel.setFont(Font.font("Arial", 12));
        resultLabel.setAlignment(Pos.CENTER);

        // Ajouter tous les composants
        captchaBox.getChildren().addAll(
                titleLabel,
                canvas,
                inputBox,
                buttonBox,
                resultLabel
        );
    }

    private void generateNewCaptcha() {
        // Générer 6 lettres aléatoires
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // Sans I, O pour éviter la confusion
        Random random = new Random();
        captchaText = "";
        for (int i = 0; i < 6; i++) {
            captchaText += chars.charAt(random.nextInt(chars.length()));
        }

        // Dessiner le CAPTCHA sur le canvas
        drawCaptcha();
    }

    private void drawCaptcha() {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Effacer le canvas
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Fond blanc
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        Random random = new Random();

        // Dessiner des lignes de fond (pour embrouiller)
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(1);
        for (int i = 0; i < 8; i++) {
            gc.strokeLine(
                    random.nextInt(250), random.nextInt(70),
                    random.nextInt(250), random.nextInt(70)
            );
        }

        // Dessiner des points de fond
        gc.setFill(Color.LIGHTGRAY);
        for (int i = 0; i < 40; i++) {
            gc.fillOval(random.nextInt(250), random.nextInt(70), 3, 3);
        }

        // Dessiner chaque lettre avec des variations
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 32));

        for (int i = 0; i < captchaText.length(); i++) {
            // Couleur aléatoire
            Color color = Color.rgb(
                    random.nextInt(100) + 100,  // Rouge entre 100-200
                    random.nextInt(100),         // Vert entre 0-100
                    random.nextInt(100)           // Bleu entre 0-100
            );
            gc.setFill(color);

            // Position avec variation
            double x = 25 + i * 32 + random.nextInt(8);
            double y = 45 + random.nextInt(12);

            // Rotation légère
            gc.save();
            gc.translate(x, y);
            gc.rotate(random.nextInt(16) - 8); // Rotation entre -8 et +8 degrés
            gc.fillText(String.valueOf(captchaText.charAt(i)), 0, 0);
            gc.restore();
        }

        // Bordures
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(2);
        gc.strokeRect(1, 1, canvas.getWidth() - 2, canvas.getHeight() - 2);
    }

    private void verifyCaptcha() {
        String input = captchaInput.getText().trim().toUpperCase();

        if (input.isEmpty()) {
            resultLabel.setText("❓ Veuillez saisir les lettres");
            resultLabel.setTextFill(Color.web("#E74C3C"));
            if (callback != null) callback.onError("Champ vide");
            return;
        }

        if (input.equals(captchaText)) {
            resultLabel.setText("✅ Code correct !");
            resultLabel.setTextFill(Color.web("#27AE60"));
            captchaInput.setDisable(true);
            verifyButton.setDisable(true);
            refreshButton.setDisable(true);
            isVerified = true;
            if (callback != null) callback.onSuccess();
        } else {
            resultLabel.setText("❌ Code incorrect");
            resultLabel.setTextFill(Color.web("#E74C3C"));
            captchaInput.clear();
            isVerified = false;
            if (callback != null) callback.onError("Code incorrect");
        }
    }

    private void refreshCaptcha() {
        generateNewCaptcha();
        captchaInput.clear();
        captchaInput.setDisable(false);
        verifyButton.setDisable(false);
        refreshButton.setDisable(false);
        resultLabel.setText("");
        isVerified = false;
    }

    public VBox getView() {
        return captchaBox;
    }

    public void reset() {
        refreshCaptcha();
    }

    public boolean isVerified() {
        return isVerified;
    }
}

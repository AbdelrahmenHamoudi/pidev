package org.example.Controllers.user.admin.user;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.Services.user.EmailVerificationService;
import org.example.Utils.UserSession;

public class EmailVerificationController {

    @FXML private Label emailDisplayLabel;
    @FXML private TextField codeField;
    @FXML private Label errorLabel;
    @FXML private Button verifyButton;
    @FXML private Button resendButton;

    private int userId;
    private String userEmail;
    private String userName;
    private String expectedCode; // Pour stocker le code attendu

    private final EmailVerificationService verificationService = new EmailVerificationService();

    public void initData(int userId, String email, String name, String code) {
        this.userId = userId;
        this.userEmail = email;
        this.userName = name;
        this.expectedCode = code;
        emailDisplayLabel.setText("📧 " + email);

        // Pour le débogage (à supprimer en production)
        System.out.println("🔐 Code attendu: " + code);
    }

    @FXML
    private void handleVerify() {
        String code = codeField.getText().trim();

        if (code.isEmpty()) {
            errorLabel.setText("❌ Veuillez entrer le code de vérification");
            return;
        }

        verifyButton.setDisable(true);
        verifyButton.setText("VÉRIFICATION...");

        // Vérification avec le code stocké
        boolean verified = code.equals(expectedCode);

        if (verified) {
            // Marquer comme vérifié dans la base de données
            boolean dbVerified = verificationService.verifyCode(userId, code);

            if (dbVerified) {
                showAlert(Alert.AlertType.INFORMATION,
                        "✅ Email vérifié",
                        "Votre email a été vérifié avec succès !");
                redirectToLogin();
            } else {
                errorLabel.setText("❌ Erreur lors de la vérification");
                verifyButton.setDisable(false);
                verifyButton.setText("VÉRIFIER");
            }
        } else {
            errorLabel.setText("❌ Code invalide ou expiré");
            verifyButton.setDisable(false);
            verifyButton.setText("VÉRIFIER");
        }
    }

    @FXML
    private void handleResend() {
        resendButton.setDisable(true);
        resendButton.setText("ENVOI...");

        // Renvoyer un nouveau code
        String newCode = verificationService.sendVerificationEmail(userId);

        if (newCode != null) {
            this.expectedCode = newCode; // Mettre à jour le code attendu
            System.out.println("🔐 Nouveau code: " + newCode);

            showAlert(Alert.AlertType.INFORMATION,
                    "📧 Code renvoyé",
                    "Un nouveau code de vérification a été envoyé à " + userEmail);
        } else {
            showAlert(Alert.AlertType.ERROR,
                    "❌ Erreur",
                    "Impossible d'envoyer le code. Veuillez réessayer.");
        }

        resendButton.setDisable(false);
        resendButton.setText("RENVOYER LE CODE");
    }

    @FXML
    private void handleLater() {
        redirectToLogin();
    }

    private void redirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #1ABC9C; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 15; " +
                "-fx-background-radius: 15;");

        alert.showAndWait();
    }
}
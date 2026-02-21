package org.example.Controllers.user.admin.user;

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

    private final EmailVerificationService verificationService = new EmailVerificationService();

    public void initData(int userId, String email, String name) {
        this.userId = userId;
        this.userEmail = email;
        this.userName = name;
        emailDisplayLabel.setText("📧 " + email);
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

        boolean verified = verificationService.verifyCode(userId, code);

        if (verified) {
            showAlert(Alert.AlertType.INFORMATION,
                    "✅ Email vérifié",
                    "Votre email a été vérifié avec succès !");
            redirectToHome();
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

        boolean sent = verificationService.sendVerificationEmail(userId);

        if (sent) {
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
        redirectToHome();
    }

    private void redirectToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/dashboard/homeClient.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Accueil");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
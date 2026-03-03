package org.example.Controllers.user;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import org.example.Entites.user.User;
import org.example.Services.user.UserCRUD;
import org.example.Services.user.APIservices.EmailService;

import java.util.Optional;

public class ForgotPasswordController {

    @FXML
    private TextField emailField;

    @FXML
    private Label emailError;

    private final UserCRUD userCRUD = new UserCRUD();

    @FXML
    private void handleSendResetLink(ActionEvent event) {
        String email = emailField.getText().trim().toLowerCase();

        // Validation
        if (email.isEmpty()) {
            showError(emailError, "❌ Veuillez saisir votre email");
            return;
        }

        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            showError(emailError, "❌ Format d'email invalide");
            return;
        }

        // Vérifier si l'email existe
        User user = userCRUD.getUserByEmail(email);

        if (user == null) {
            showError(emailError, "❌ Aucun compte trouvé avec cet email");
            return;
        }

        // Désactiver le bouton pendant l'envoi
        Button sendBtn = (Button) event.getSource();
        sendBtn.setDisable(true);
        sendBtn.setText("ENVOI EN COURS...");

        try {
            // Générer un CODE à 6 chiffres
            String resetCode = EmailService.generateVerificationCode();

            // Envoyer l'email avec le CODE
            boolean emailSent = EmailService.sendResetPasswordCode(
                    email,
                    user.getPrenom(),
                    resetCode
            );

            if (emailSent) {
                System.out.println("📧 Code de réinitialisation envoyé à: " + email);
                System.out.println("🔑 Code: " + resetCode);

                // Ouvrir la boîte de dialogue pour saisir le code
                showCodeInputDialog(event, email, resetCode);

            } else {
                showError(emailError, "❌ Erreur lors de l'envoi de l'email");
                sendBtn.setDisable(false);
                sendBtn.setText("ENVOYER LE LIEN");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError(emailError, "❌ Erreur système: " + e.getMessage());
            sendBtn.setDisable(false);
            sendBtn.setText("ENVOYER LE LIEN");
        }
    }

    private void showCodeInputDialog(ActionEvent originalEvent, String email, String expectedCode) {
        // Créer une boîte de dialogue
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("🔐 Code de réinitialisation");
        dialog.setHeaderText("Un code a été envoyé à " + email);

        // Style
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-border-color: #1ABC9C; -fx-border-width: 2; -fx-border-radius: 15; -fx-background-radius: 15;");

        // Boutons
        ButtonType confirmButtonType = new ButtonType("VÉRIFIER", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // Champ de saisie
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        Label instructionLabel = new Label("Saisissez le code à 6 chiffres reçu par email :");
        instructionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2C3E50;");

        TextField codeField = new TextField();
        codeField.setPromptText("123456");
        codeField.setStyle("-fx-font-size: 18px; -fx-alignment: center; -fx-pref-width: 200;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 12px;");

        content.getChildren().addAll(instructionLabel, codeField, errorLabel);
        dialog.getDialogPane().setContent(content);

        // Validation du bouton
        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmButtonType);
        confirmButton.setStyle("-fx-background-color: #F39C12; -fx-text-fill: white; -fx-font-weight: bold;");

        confirmButton.addEventFilter(ActionEvent.ACTION, event -> {
            String enteredCode = codeField.getText().trim();
            if (enteredCode.isEmpty()) {
                errorLabel.setText("❌ Veuillez saisir le code");
                event.consume();
            } else if (!enteredCode.equals(expectedCode)) {
                errorLabel.setText("❌ Code incorrect");
                event.consume();
            }
        });

        // Afficher et traiter
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            // Code correct → ouvrir la boîte de dialogue pour nouveau mot de passe
            showNewPasswordDialog(originalEvent, email);
        } else {
            // Annulé → réactiver le bouton
            Button sendBtn = (Button) originalEvent.getSource();
            sendBtn.setDisable(false);
            sendBtn.setText("ENVOYER LE LIEN");
        }
    }

    private void showNewPasswordDialog(ActionEvent originalEvent, String email) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("🔒 Nouveau mot de passe");
        dialog.setHeaderText("Choisissez un nouveau mot de passe");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-border-color: #1ABC9C; -fx-border-width: 2; -fx-border-radius: 15; -fx-background-radius: 15;");

        ButtonType confirmButtonType = new ButtonType("RÉINITIALISER", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // Contenu
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        Label passLabel = new Label("Nouveau mot de passe :");
        passLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2C3E50; -fx-font-weight: bold;");

        PasswordField newPassField = new PasswordField();
        newPassField.setPromptText("••••••••");

        Label confirmLabel = new Label("Confirmer le mot de passe :");
        confirmLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2C3E50; -fx-font-weight: bold;");

        PasswordField confirmPassField = new PasswordField();
        confirmPassField.setPromptText("••••••••");

        Label passError = new Label();
        passError.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 12px;");

        Label strengthLabel = new Label("Force du mot de passe : Faible");
        strengthLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 12px;");

        // Validation en temps réel
        newPassField.textProperty().addListener((obs, old, newVal) -> {
            if (newVal.length() < 8) {
                strengthLabel.setText("Force : Trop court (min 8)");
                strengthLabel.setStyle("-fx-text-fill: #E74C3C;");
            } else if (!newVal.matches(".*[A-Za-z].*") || !newVal.matches(".*[0-9].*")) {
                strengthLabel.setText("Force : Doit contenir lettres ET chiffres");
                strengthLabel.setStyle("-fx-text-fill: #E74C3C;");
            } else if (newVal.matches(".*[A-Z].*") && newVal.matches(".*[a-z].*")) {
                strengthLabel.setText("Force : Fort");
                strengthLabel.setStyle("-fx-text-fill: #27AE60;");
            } else {
                strengthLabel.setText("Force : Moyen");
                strengthLabel.setStyle("-fx-text-fill: #F39C12;");
            }

            if (!confirmPassField.getText().isEmpty()) {
                if (!newVal.equals(confirmPassField.getText())) {
                    passError.setText("❌ Les mots de passe ne correspondent pas");
                } else {
                    passError.setText("");
                }
            }
        });

        confirmPassField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.equals(newPassField.getText())) {
                passError.setText("❌ Les mots de passe ne correspondent pas");
            } else {
                passError.setText("");
            }
        });

        content.getChildren().addAll(passLabel, newPassField, confirmLabel, confirmPassField, strengthLabel, passError);
        dialogPane.setContent(content);

        // Validation du bouton
        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmButtonType);
        confirmButton.setStyle("-fx-background-color: #F39C12; -fx-text-fill: white; -fx-font-weight: bold;");

        confirmButton.addEventFilter(ActionEvent.ACTION, event -> {
            String newPass = newPassField.getText();
            String confirmPass = confirmPassField.getText();

            if (newPass.isEmpty()) {
                passError.setText("❌ Veuillez saisir un mot de passe");
                event.consume();
            } else if (newPass.length() < 8) {
                passError.setText("❌ Minimum 8 caractères");
                event.consume();
            } else if (!newPass.matches(".*[A-Za-z].*") || !newPass.matches(".*[0-9].*")) {
                passError.setText("❌ Doit contenir lettres ET chiffres");
                event.consume();
            } else if (!newPass.equals(confirmPass)) {
                passError.setText("❌ Les mots de passe ne correspondent pas");
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == confirmButtonType) {
            // Mettre à jour le mot de passe
            try {
                User user = userCRUD.getUserByEmail(email);
                user.setMot_de_pass(newPassField.getText());
                userCRUD.updatePassword(user);

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("✅ Succès");
                success.setHeaderText("Mot de passe réinitialisé");
                success.setContentText("Votre mot de passe a été modifié avec succès.");

                DialogPane successPane = success.getDialogPane();
                successPane.setStyle("-fx-background-color: white; -fx-border-color: #27AE60; -fx-border-width: 2; -fx-border-radius: 15; -fx-background-radius: 15;");

                success.showAndWait();

                // Retourner à la page de connexion
                handleBackToLogin(originalEvent);

            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la réinitialisation: " + e.getMessage());

                // Réactiver le bouton
                Button sendBtn = (Button) originalEvent.getSource();
                sendBtn.setDisable(false);
                sendBtn.setText("ENVOYER LE LIEN");
            }
        } else {
            // Annulé → réactiver le bouton
            Button sendBtn = (Button) originalEvent.getSource();
            sendBtn.setDisable(false);
            sendBtn.setText("ENVOYER LE LIEN");
        }
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        try {
            System.out.println("🔄 Retour à la page de connexion...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));

            if (loader.getLocation() == null) {
                throw new Exception("Fichier login.fxml introuvable");
            }

            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR,
                    "❌ Erreur de navigation",
                    "Redirection impossible",
                    "Erreur: " + e.getMessage());
        }
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 12px; -fx-font-weight: bold;");
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

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #1ABC9C; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 15; " +
                "-fx-background-radius: 15;");

        alert.showAndWait();
    }

    @FXML
    private void clearField() {
        emailField.clear();
        emailError.setText("");
    }
}
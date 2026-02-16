package org.example.Controllers.user;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import org.example.Entites.user.Role;
import org.example.Entites.user.Status;
import org.example.Entites.user.User;
import org.example.Services.user.UserCRUD;
import org.example.Controllers.user.customUserException;
import org.example.Controllers.user.client.homeClientController;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label emailError;

    @FXML
    private Label passwordError;

    private final UserCRUD userCRUD = new UserCRUD();
    private boolean isCheckingEmail = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupRealTimeValidation();
        System.out.println("✅ Page de login initialisée avec contrôle de saisie en temps réel");
    }

    private void setupRealTimeValidation() {
        emailField.textProperty().addListener((obs, old, newVal) -> {
            String email = newVal.trim().toLowerCase();

            if (email.isEmpty()) {
                showError(emailError, "✉ L'email est requis");
            } else if (email.length() > 100) {
                showError(emailError, "⚠ Email trop long (max 100 caractères)");
            } else if (email.contains(" ")) {
                showError(emailError, "⚠ L'email ne doit pas contenir d'espaces");
            } else if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                showError(emailError, "⚠ Format invalide (ex: nom@domaine.tn)");
            } else {
                clearError(emailError);

                if (!isCheckingEmail) {
                    isCheckingEmail = true;
                    String emailToCheck = email;

                    new Thread(() -> {
                        try {
                            Thread.sleep(500);
                            User existing = userCRUD.getUserByEmail(emailToCheck);

                            javafx.application.Platform.runLater(() -> {
                                if (existing != null) {
                                    emailField.setStyle("-fx-border-color: #27AE60; -fx-border-width: 2;");
                                } else {
                                    emailField.setStyle("-fx-border-color: #F39C12; -fx-border-width: 2;");
                                }
                                isCheckingEmail = false;
                            });
                        } catch (Exception e) {
                            isCheckingEmail = false;
                        }
                    }).start();
                }
            }
        });

        passwordField.textProperty().addListener((obs, old, newVal) -> {
            validatePassword(newVal);
        });
    }

    private void validatePassword(String password) {
        if (password.isEmpty()) {
            showError(passwordError, "🔒 Le mot de passe est requis");
        } else if (password.length() < 4) {
            showError(passwordError, "⚠ Minimum 4 caractères");
        } else if (password.length() > 255) {
            showError(passwordError, "⚠ Mot de passe trop long");
        } else if (password.contains(" ")) {
            showError(passwordError, "⚠ Ne doit pas contenir d'espaces");
        } else if (!password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
            showError(passwordError, "⚠ Doit contenir lettres ET chiffres");
        } else {
            clearError(passwordError);
        }
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setStyle("-fx-text-fill: #F39C12; -fx-font-weight: bold; -fx-font-size: 12px;");
    }

    private void clearError(Label label) {
        label.setText("");
    }

    private boolean hasErrors() {
        return !emailError.getText().isEmpty() || !passwordError.getText().isEmpty();
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        emailError.setText("");
        passwordError.setText("");

        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        validatePassword(password);

        if (email.isEmpty()) {
            showError(emailError, "✉ L'email est requis");
        } else if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            showError(emailError, "⚠ Format d'email invalide");
        }

        if (hasErrors()) {
            return;
        }

        Button loginBtn = (Button) emailField.getScene().lookup(".tunisia-login-btn");
        if (loginBtn != null) {
            loginBtn.setDisable(true);
            loginBtn.setText("CONNEXION EN COURS...");
        }

        try {
            User existingUser = userCRUD.getUserByEmail(email);

            if (existingUser == null) {
                throw new customUserException("❌ Aucun compte trouvé avec cet email");
            }

            if (existingUser.getStatus() == Status.Banned) {
                throw new customUserException("🚫 Votre compte a été suspendu. Veuillez contacter l'administrateur.");
            }

            User user = new User();
            user.setE_mail(email);
            user.setMot_de_pass(password);

            User authenticatedUser = userCRUD.signIn(user);

            if (authenticatedUser.getRole() == null) {
                throw new customUserException("⚠ Erreur de configuration du compte");
            }

            showSuccessAnimation(loginBtn);
            redirectBasedOnRole(event, authenticatedUser);

        } catch (customUserException e) {
            passwordField.clear();
            passwordField.requestFocus();

            String errorMessage = e.getMessage();

            if (errorMessage.contains("Aucun compte")) {
                showError(emailError, errorMessage);
            } else if (errorMessage.contains("suspendu")) {
                showAlert(AlertType.WARNING,
                        "🚫 Compte suspendu",
                        "Accès refusé",
                        errorMessage + "\n\nContactez : support@re7la.tn");
            } else if (errorMessage.contains("incorrect")) {
                showError(passwordError, "❌ Email ou mot de passe incorrect");
            } else {
                showError(passwordError, "❌ " + errorMessage);
            }

        } catch (Exception e) {
            showAlert(AlertType.ERROR,
                    "🌍 Erreur système",
                    "Service temporairement indisponible",
                    "Veuillez réessayer dans quelques instants.\n\nSi le problème persiste, contactez : support@re7la.tn");
            e.printStackTrace();

        } finally {
            if (loginBtn != null) {
                loginBtn.setDisable(false);
                loginBtn.setText("SE CONNECTER");
            }
        }
    }

    private void redirectBasedOnRole(ActionEvent event, User user) {
        try {
            FXMLLoader loader;
            Parent root;
            String title;

            if (user.getRole() == Role.admin) {
                System.out.println("👑 Redirection vers Gestion des utilisateurs...");

                java.net.URL adminUrl = getClass().getResource("/user/back/users.fxml");
                if (adminUrl == null) {
                    throw new Exception("❌ Fichier users.fxml introuvable dans /user/back/");
                }

                loader = new FXMLLoader(adminUrl);
                root = loader.load();
                title = "RE7LA Tunisie - Gestion des utilisateurs";

            } else {
                System.out.println("👤 Redirection vers Home Client avec utilisateur: " + user.getPrenom() + " " + user.getNom());

                java.net.URL clientUrl = getClass().getResource("/user/dashboard/homeClient.fxml");
                if (clientUrl == null) {
                    throw new Exception("❌ Fichier homeClient.fxml introuvable");
                }

                loader = new FXMLLoader(clientUrl);
                root = loader.load();

                homeClientController clientController = loader.getController();
                clientController.initUserData(user);

                title = "RE7LA Tunisie - Accueil";
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.centerOnScreen();
            stage.show();

            System.out.println("✅ Redirection réussie vers: " + title);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR,
                    "❌ Erreur de redirection",
                    "Impossible de charger l'interface",
                    "Rôle: " + user.getRole() + "\n" +
                            "Erreur: " + e.getMessage() + "\n\n" +
                            "🔧 Vérifiez que les fichiers FXML sont présents:\n" +
                            "   - Admin: /user/back/users.fxml\n" +
                            "   - Client: /user/dashboard/homeClient.fxml");
        }
    }

    private void showSuccessAnimation(Button loginBtn) {
        if (loginBtn != null) {
            String originalColor = "-fx-background-color: linear-gradient(to right, #F39C12, #e67e22, #F39C12);";
            loginBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
            loginBtn.setText("✓ CONNEXION RÉUSSIE");

            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(() -> {
                        loginBtn.setStyle(originalColor);
                        loginBtn.setText("SE CONNECTER");
                    });
                } catch (InterruptedException ignored) {}
            }).start();
        }
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

        dialogPane.lookup(".header-panel").setStyle("-fx-background-color: linear-gradient(to right, #1ABC9C20, #F39C1220);");

        alert.showAndWait();
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        showAlert(AlertType.INFORMATION,
                "🔑 Mot de passe oublié",
                "Réinitialisation de mot de passe",
                "Un email vous sera envoyé pour réinitialiser votre mot de passe.\n\nCette fonctionnalité sera bientôt disponible !");
    }

    @FXML
    private void handleSignUp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/signup/signup.fxml"));

            if (loader.getLocation() == null) {
                throw new Exception("Fichier signup.fxml introuvable");
            }

            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Créer un compte");
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR,
                    "❌ Erreur de navigation",
                    "Impossible d'accéder à la page d'inscription",
                    "Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void handleSocialLogin(ActionEvent event) {
        Button source = (Button) event.getSource();
        String provider = source.getText();

        showAlert(AlertType.INFORMATION,
                "🌍 Connexion sociale",
                "Connexion avec " + provider,
                "Cette fonctionnalité sera bientôt disponible !\n\nMerci de votre patience.");
    }

    @FXML
    private void clearFields() {
        emailField.clear();
        passwordField.clear();
        emailError.setText("");
        passwordError.setText("");
    }
}
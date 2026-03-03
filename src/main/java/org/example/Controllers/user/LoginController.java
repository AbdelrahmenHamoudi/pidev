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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import org.example.Entites.user.Role;
import org.example.Entites.user.Status;
import org.example.Entites.user.User;
import org.example.Services.user.APIservices.TwoFAService;
import org.example.Services.user.UserCRUD;
import org.example.Services.user.ConnexionLogService;
import org.example.Services.user.AdminNotificationService;

import org.example.Controllers.user.customUserException;
import org.example.Controllers.user.client.homeClientController;
import org.example.Utils.UserSession;
import org.example.Services.user.APIservices.JWTService;
import org.example.Services.user.APIservices.EmailService;
import org.example.Controllers.user.AlphabetCaptchaController;
import org.example.Controllers.user.LocationController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URL;
import java.util.Optional;
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

    @FXML
    private VBox captchaContainer;

    @FXML
    private HBox locationContainer;

    private final UserCRUD userCRUD = new UserCRUD();
    private final ConnexionLogService logService = new ConnexionLogService();
    private final TwoFAService twoFAService = new TwoFAService();
    private boolean isCheckingEmail = false;

    private AlphabetCaptchaController captchaController;
    private LocationController locationController;

    // Variables pour stocker l'utilisateur en cours de connexion (pour la 2FA)
    private User pendingUser;
    private String pendingPassword;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupRealTimeValidation();

        // ✅ Vérifier s'il y a une session existante
        checkExistingSession();

        // ✅ Initialiser le CAPTCHA
        initCaptcha();

        // ✅ Initialiser l'affichage de la localisation
        initLocationDisplay();

        System.out.println("✅ Page de login initialisée avec contrôle de saisie en temps réel");
    }

    private void initCaptcha() {
        captchaController = new AlphabetCaptchaController(new AlphabetCaptchaController.CaptchaCallback() {
            @Override
            public void onSuccess() {
                System.out.println("✅ CAPTCHA alphabétique validé pour login");
            }

            @Override
            public void onError(String error) {
                System.err.println("❌ Erreur CAPTCHA login: " + error);
            }
        });

        captchaContainer.getChildren().add(captchaController.getView());
    }

    private void initLocationDisplay() {
        locationController = new LocationController();
        locationContainer.getChildren().add(locationController.getView());
    }

    private void checkExistingSession() {
        UserSession session = UserSession.getInstance();
        if (session.isLoggedIn()) {
            User currentUser = session.getCurrentUser();
            System.out.println("👤 Session existante trouvée pour: " + currentUser.getE_mail());
        }
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

    private String getClientIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "";
        }
    }

    private String getDeviceInfo() {
        String os = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String javaVersion = System.getProperty("java.version");
        return os + " " + osVersion + " | Java: " + javaVersion;
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        emailError.setText("");
        passwordError.setText("");

        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String ipAddress = getClientIp();
        String deviceInfo = getDeviceInfo();

        validatePassword(password);

        if (email.isEmpty()) {
            showError(emailError, "✉ L'email est requis");
        } else if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            showError(emailError, "⚠ Format d'email invalide");
        }

        if (hasErrors()) {
            logService.logConnexion(0, ipAddress, deviceInfo, false, "Erreur de validation");
            return;
        }

        // ✅ Vérification CAPTCHA
        if (!captchaController.isVerified()) {
            showAlert(AlertType.WARNING,
                    "🔐 Validation requise",
                    "Vérification de sécurité",
                    "Veuillez recopier les lettres affichées");
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
                logService.logConnexion(0, ipAddress, deviceInfo, false, "Email inexistant: " + email);
                throw new customUserException("❌ Aucun compte trouvé avec cet email");
            }

            if (existingUser.getStatus() == Status.Banned) {
                logService.logConnexion(existingUser.getId(), ipAddress, deviceInfo, false, "Compte suspendu");
                throw new customUserException("🚫 Votre compte a été suspendu. Veuillez contacter l'administrateur.");
            }

            User user = new User();
            user.setE_mail(email);
            user.setMot_de_pass(password);

            // ✅ Vérifier le mot de passe
            User authenticatedUser = userCRUD.signIn(user);

            // ✅ Vérifier si la 2FA est activée
            boolean is2FAEnabled = twoFAService.is2FAEnabled(authenticatedUser.getId());

            if (is2FAEnabled) {
                // Stocker l'utilisateur en attente et demander le code 2FA
                pendingUser = authenticatedUser;
                pendingPassword = password;
                show2FADialog(event, authenticatedUser);

                if (loginBtn != null) {
                    loginBtn.setDisable(false);
                    loginBtn.setText("SE CONNECTER");
                }
                return;
            }

            // ✅ Pas de 2FA, connexion directe
            completeLogin(event, authenticatedUser, ipAddress, deviceInfo, loginBtn);

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

                User existingUser = userCRUD.getUserByEmail(email);
                if (existingUser != null) {
                    logService.logConnexion(existingUser.getId(), ipAddress, deviceInfo, false, "Mot de passe incorrect");
                }
            } else {
                showError(passwordError, "❌ " + errorMessage);
            }

            if (loginBtn != null) {
                loginBtn.setDisable(false);
                loginBtn.setText("SE CONNECTER");
            }

        } catch (Exception e) {
            logService.logConnexion(0, ipAddress, deviceInfo, false, "Erreur système: " + e.getMessage());

            showAlert(AlertType.ERROR,
                    "🌍 Erreur système",
                    "Service temporairement indisponible",
                    "Veuillez réessayer dans quelques instants.\n\nSi le problème persiste, contactez : support@re7la.tn");
            e.printStackTrace();

            if (loginBtn != null) {
                loginBtn.setDisable(false);
                loginBtn.setText("SE CONNECTER");
            }
        }
    }

    /**
     * Affiche la boîte de dialogue pour le code 2FA
     */
    private void show2FADialog(ActionEvent event, User user) {
        // Créer le dialogue
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("🔐 Double authentification");
        dialog.setHeaderText("Entrez le code à 6 chiffres généré par votre application");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-border-color: #3498DB; -fx-border-width: 2; -fx-border-radius: 15; -fx-background-radius: 15;");
        dialogPane.setPrefWidth(350);

        // Contenu
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(javafx.geometry.Pos.CENTER);

        Label instructionLabel = new Label("Code à 6 chiffres :");
        instructionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2C3E50;");

        TextField codeField = new TextField();
        codeField.setPromptText("123456");
        codeField.setStyle("-fx-font-size: 24px; -fx-alignment: center; -fx-pref-width: 200; -fx-font-family: monospace;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 12px;");

        content.getChildren().addAll(instructionLabel, codeField, errorLabel);
        dialogPane.setContent(content);

        // Boutons
        ButtonType verifyButtonType = new ButtonType("VÉRIFIER", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(verifyButtonType, ButtonType.CANCEL);

        Button verifyButton = (Button) dialogPane.lookupButton(verifyButtonType);
        verifyButton.setStyle("-fx-background-color: #F39C12; -fx-text-fill: white; -fx-font-weight: bold;");

        // Validation
        verifyButton.addEventFilter(ActionEvent.ACTION, e -> {
            String code = codeField.getText().trim();
            if (code.isEmpty()) {
                errorLabel.setText("❌ Veuillez entrer le code");
                e.consume();
                return;
            }

            try {
                int verificationCode = Integer.parseInt(code);
                boolean isValid = twoFAService.verifyCode(user.getId(), verificationCode);

                if (!isValid) {
                    errorLabel.setText("❌ Code invalide");
                    e.consume();
                }
            } catch (NumberFormatException ex) {
                errorLabel.setText("❌ Code invalide (doit être 6 chiffres)");
                e.consume();
            }
        });

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            // Code valide, finaliser la connexion
            String ipAddress = getClientIp();
            String deviceInfo = getDeviceInfo();
            Button loginBtn = (Button) emailField.getScene().lookup(".tunisia-login-btn");

            completeLogin(event, user, ipAddress, deviceInfo, loginBtn);
        } else {
            // Annulé, remettre le bouton
            Button loginBtn = (Button) emailField.getScene().lookup(".tunisia-login-btn");
            if (loginBtn != null) {
                loginBtn.setDisable(false);
                loginBtn.setText("SE CONNECTER");
            }
        }
    }

    /**
     * Finalise la connexion après validation (avec ou sans 2FA)
     */
    private void completeLogin(ActionEvent event, User authenticatedUser, String ipAddress, String deviceInfo, Button loginBtn) {
        try {
            UserSession.getInstance().setCurrentUser(authenticatedUser);
            String token = UserSession.getInstance().getToken();

            System.out.println("\n🔐 === CONNEXION AVEC JWT ===");
            System.out.println("👤 Utilisateur: " + authenticatedUser.getE_mail());
            System.out.println("👑 Rôle: " + authenticatedUser.getRole());
            System.out.println("⏱️ Expiration: dans 24 heures");

            if (JWTService.isTokenExpired(token)) {
                System.out.println("⚠️ Attention: Token déjà expiré!");
            } else {
                System.out.println("✅ Token valide et actif");
                System.out.println("📋 Contenu du token:");
                System.out.println("   - ID: " + JWTService.extractUserId(token));
                System.out.println("   - Email: " + JWTService.extractEmail(token));
                System.out.println("   - Rôle: " + JWTService.extractRole(token));
            }
            System.out.println("🔐 =======================\n");

            if (authenticatedUser.getRole() == null) {
                throw new customUserException("⚠ Erreur de configuration du compte");
            }

            logService.logConnexion(authenticatedUser.getId(), ipAddress, deviceInfo, true, null);

            if (authenticatedUser.getRole() == Role.admin) {
                try {
                    AdminNotificationService notifService = new AdminNotificationService();
                    notifService.notifySystemAlert(
                            "🔐 Connexion administrateur",
                            authenticatedUser.getPrenom() + " " + authenticatedUser.getNom() + " s'est connecté depuis " + ipAddress,
                            "LOW"
                    );
                } catch (Exception e) {
                    System.err.println("⚠️ Impossible de créer la notification admin: " + e.getMessage());
                }
            }

            showSuccessAnimation(loginBtn);
            redirectBasedOnRole(event, authenticatedUser);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR,
                    "❌ Erreur de connexion",
                    "Impossible de finaliser la connexion",
                    "Erreur: " + e.getMessage());

            if (loginBtn != null) {
                loginBtn.setDisable(false);
                loginBtn.setText("SE CONNECTER");
            }
        }
    }

    @FXML
    private void openFaceLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/FaceLogin.fxml"));
            Parent root = loader.load();

// ✅ Get the current stage (the one currently showing)
            Stage stage = (Stage) emailField.getScene().getWindow();

// ✅ Replace the scene (old window is replaced)
            stage.setScene(new Scene(root));
            stage.setTitle("Face ID");
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR,
                    "❌ Erreur de navigation",
                    "Impossible d'accéder à la reconnaissance faciale",
                    "Erreur: " + e.getMessage());
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

            String token = UserSession.getInstance().getToken();
            System.out.println("🔐 Token JWT actif pour la redirection");

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

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        try {
            System.out.println("🔄 Redirection vers la page de réinitialisation...");

            // Charger la nouvelle interface
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/forgotPassword.fxml"));

            if (loader.getLocation() == null) {
                throw new Exception("Fichier forgotPassword.fxml introuvable");
            }

            Parent root = loader.load();

            // Récupérer la stage actuelle
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Changer la scène
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Mot de passe oublié");
            stage.centerOnScreen();
            stage.show();

            System.out.println("✅ Redirection vers forgotPassword.fxml réussie");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR,
                    "❌ Erreur de navigation",
                    "Redirection impossible",
                    "Impossible d'ouvrir la page de réinitialisation:\n" + e.getMessage());
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
package org.example.Controllers.user;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.Entites.user.Role;
import org.example.Entites.user.Status;
import org.example.Entites.user.User;
import org.example.Services.user.UserCRUD;
import org.example.Services.user.AdminNotificationService;
import org.example.Services.user.EmailVerificationService;
import org.example.Utils.UserSession;
import org.example.Services.user.APIservices.EmailService;
import org.example.Services.user.APIservices.SmsService;
import org.example.Controllers.user.AlphabetCaptchaController;

import java.io.File;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class SignUpController {

    @FXML private TextField prenomField;
    @FXML private TextField nomField;
    @FXML private DatePicker dateNaissPicker;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private TextField imageUrlField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private CheckBox termsCheckBox;

    @FXML private VBox captchaContainer;

    @FXML private Label prenomError;
    @FXML private Label nomError;
    @FXML private Label dateError;
    @FXML private Label emailError;
    @FXML private Label telephoneError;
    @FXML private Label imageUrlError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;

    @FXML private VBox imagePreviewBox;
    @FXML private ImageView previewImageView;
    @FXML private Button chooseImageButton;

    private final UserCRUD userCRUD = new UserCRUD();
    private final AdminNotificationService notifService = new AdminNotificationService();
    private File selectedImageFile;
    private boolean isCheckingEmail = false;

    private AlphabetCaptchaController captchaController;

    @FXML
    public void initialize() {
        setupRealTimeValidation();
        initCaptcha();

        imageUrlField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty()) {
                try {
                    Image image = new Image(newValue, 70, 70, true, true);
                    previewImageView.setImage(image);
                    imagePreviewBox.setVisible(true);
                    imagePreviewBox.setManaged(true);
                    selectedImageFile = null;
                    clearError(imageUrlError);
                } catch (Exception e) {
                    if (selectedImageFile == null) {
                        imagePreviewBox.setVisible(false);
                        imagePreviewBox.setManaged(false);
                    }
                }
            } else {
                if (selectedImageFile == null) {
                    imagePreviewBox.setVisible(false);
                    imagePreviewBox.setManaged(false);
                }
            }
        });
    }

    private void initCaptcha() {
        captchaController = new AlphabetCaptchaController(new AlphabetCaptchaController.CaptchaCallback() {
            @Override
            public void onSuccess() {
                System.out.println("✅ CAPTCHA alphabétique validé");
            }

            @Override
            public void onError(String error) {
                System.err.println("❌ Erreur CAPTCHA: " + error);
            }
        });

        captchaContainer.getChildren().add(captchaController.getView());
    }

    private void setupRealTimeValidation() {
        prenomField.textProperty().addListener((obs, old, newVal) -> {
            String val = newVal.trim();
            if (val.isEmpty()) {
                showError(prenomError, "👤 Le prénom est requis");
            } else if (val.length() < 2) {
                showError(prenomError, "👤 Minimum 2 caractères");
            } else if (val.length() > 50) {
                showError(prenomError, "👤 Maximum 50 caractères");
            } else if (!val.matches("^[a-zA-ZÀ-ÿ\\s-]+$")) {
                showError(prenomError, "👤 Caractères non autorisés");
            } else {
                clearError(prenomError);
            }
        });

        nomField.textProperty().addListener((obs, old, newVal) -> {
            String val = newVal.trim();
            if (val.isEmpty()) {
                showError(nomError, "👤 Le nom est requis");
            } else if (val.length() < 2) {
                showError(nomError, "👤 Minimum 2 caractères");
            } else if (val.length() > 50) {
                showError(nomError, "👤 Maximum 50 caractères");
            } else if (!val.matches("^[a-zA-ZÀ-ÿ\\s-]+$")) {
                showError(nomError, "👤 Caractères non autorisés");
            } else {
                clearError(nomError);
            }
        });

        emailField.textProperty().addListener((obs, old, newVal) -> {
            String email = newVal.trim().toLowerCase();
            if (email.isEmpty()) {
                showError(emailError, "📧 L'email est requis");
            } else if (email.length() > 100) {
                showError(emailError, "📧 Email trop long");
            } else if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                showError(emailError, "📧 Format invalide");
            } else {
                if (!isCheckingEmail) {
                    isCheckingEmail = true;
                    new Thread(() -> {
                        try {
                            Thread.sleep(500);
                            User existing = userCRUD.getUserByEmail(email);
                            javafx.application.Platform.runLater(() -> {
                                if (existing != null) {
                                    showError(emailError, "📧 Cet email est déjà utilisé");
                                } else {
                                    clearError(emailError);
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

        telephoneField.textProperty().addListener((obs, old, newVal) -> {
            String tel = newVal.trim();
            if (tel.isEmpty()) {
                showError(telephoneError, "📞 Le téléphone est requis");
            } else if (tel.length() > 20) {
                showError(telephoneError, "📞 Numéro trop long");
            } else if (!tel.matches("^(\\+216|0)?[2-9][0-9]{7}$")) {
                showError(telephoneError, "📞 Format tunisien invalide");
            } else {
                clearError(telephoneError);
            }
        });

        dateNaissPicker.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal == null) {
                showError(dateError, "📅 La date de naissance est requise");
            } else {
                int age = Period.between(newVal, LocalDate.now()).getYears();
                if (age < 18) {
                    showError(dateError, "📅 Vous devez avoir au moins 18 ans");
                } else if (age > 120) {
                    showError(dateError, "📅 Date invalide");
                } else {
                    clearError(dateError);
                }
            }
        });

        passwordField.textProperty().addListener((obs, old, newVal) -> {
            validatePassword();
        });

        confirmPasswordField.textProperty().addListener((obs, old, newVal) -> {
            validateConfirmPassword();
        });
    }

    private void validatePassword() {
        String password = passwordField.getText();
        if (password.isEmpty()) {
            showError(passwordError, "🔒 Le mot de passe est requis");
        } else if (password.length() < 8) {
            showError(passwordError, "🔒 Minimum 8 caractères");
        } else if (password.length() > 255) {
            showError(passwordError, "🔒 Mot de passe trop long");
        } else if (!password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
            showError(passwordError, "🔒 Doit contenir lettres ET chiffres");
        } else if (!password.matches(".*[A-Z].*")) {
            showError(passwordError, "🔒 Doit contenir une majuscule");
        } else if (!password.matches(".*[a-z].*")) {
            showError(passwordError, "🔒 Doit contenir une minuscule");
        } else {
            clearError(passwordError);
        }
        if (!confirmPasswordField.getText().isEmpty()) {
            validateConfirmPassword();
        }
    }

    private void validateConfirmPassword() {
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        if (!confirm.equals(password)) {
            showError(confirmPasswordError, "✓ Les mots de passe ne correspondent pas");
        } else {
            clearError(confirmPasswordError);
        }
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 11px; -fx-font-weight: bold;");
    }

    private void clearError(Label label) {
        label.setText("");
    }

    @FXML
    private void handleChooseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");

        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
                "Fichiers image", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp");
        fileChooser.getExtensionFilters().add(imageFilter);

        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        Stage stage = (Stage) chooseImageButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                selectedImageFile = file;
                imageUrlField.setText(file.toURI().toString());

                Image image = new Image(file.toURI().toString(), 70, 70, true, true);
                previewImageView.setImage(image);
                imagePreviewBox.setVisible(true);
                imagePreviewBox.setManaged(true);

                clearError(imageUrlError);
                System.out.println("✅ Image sélectionnée: " + file.getAbsolutePath());

            } catch (Exception e) {
                showError(imageUrlError, "❌ Erreur de chargement de l'image");
                e.printStackTrace();
            }
        }
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (!prenomError.getText().isEmpty()) isValid = false;
        if (!nomError.getText().isEmpty()) isValid = false;
        if (!emailError.getText().isEmpty()) isValid = false;
        if (!telephoneError.getText().isEmpty()) isValid = false;
        if (!dateError.getText().isEmpty()) isValid = false;
        if (!passwordError.getText().isEmpty()) isValid = false;
        if (!confirmPasswordError.getText().isEmpty()) isValid = false;

        if (!termsCheckBox.isSelected()) {
            showAlert(Alert.AlertType.WARNING,
                    "📋 Conditions d'utilisation",
                    "Acceptation requise",
                    "Veuillez accepter les conditions d'utilisation et la politique de confidentialité.");
            isValid = false;
        }

        return isValid;
    }

    @FXML
    private void handleSignUp(ActionEvent event) {
        if (!validateForm()) return;

        // ✅ Vérification CAPTCHA
        if (!captchaController.isVerified()) {
            showAlert(Alert.AlertType.WARNING,
                    "🔐 Validation requise",
                    "Vérification de sécurité",
                    "Veuillez recopier les lettres affichées");
            return;
        }

        Button signUpBtn = (Button) event.getSource();
        signUpBtn.setDisable(true);
        signUpBtn.setText("INSCRIPTION EN COURS...");

        try {
            String prenom = prenomField.getText().trim();
            String nom = nomField.getText().trim();
            LocalDate dateNaiss = dateNaissPicker.getValue();
            String email = emailField.getText().trim().toLowerCase();
            String telephone = telephoneField.getText().trim();
            String password = passwordField.getText();

            String imageUrl = null;
            if (selectedImageFile != null) {
                imageUrl = selectedImageFile.toURI().toString();
            } else if (!imageUrlField.getText().trim().isEmpty()) {
                imageUrl = imageUrlField.getText().trim();
            }

            // ✅ CRÉER L'UTILISATEUR (avec email_non_vérifié)
            User newUser = new User();
            newUser.setNom(nom);
            newUser.setPrenom(prenom);
            newUser.setDate_naiss(dateNaiss.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            newUser.setE_mail(email);
            newUser.setNum_tel(telephone);
            newUser.setMot_de_pass(password);
            newUser.setImage(imageUrl);
            newUser.setRole(Role.user);
            newUser.setStatus(Status.Unbanned);
            // Note: email_verified = false par défaut

            userCRUD.createUser(newUser);
            User createdUser = userCRUD.getUserByEmail(email);

            // ✅ ENVOI EMAIL DE VÉRIFICATION
            EmailVerificationService verificationService = new EmailVerificationService();
            String verificationCode = verificationService.sendVerificationEmail(createdUser.getId());
            System.out.println("📧 Email de vérification envoyé à: " + email);
            System.out.println("🔐 Code de vérification: " + verificationCode);

            // ✅ OUVRIR LA BOÎTE DE DIALOGUE DE VÉRIFICATION
            boolean verified = showVerificationDialog(email, verificationCode);

            if (verified) {
                // ✅ MARQUER L'EMAIL COMME VÉRIFIÉ
                userCRUD.markEmailAsVerified(createdUser.getId());

                // ✅ ENVOI EMAIL DE BIENVENUE (après vérification)
                EmailService.sendWelcomeEmail(email, prenom);
                System.out.println("📧 Email de bienvenue envoyé à: " + email);

                // ✅ ENVOI SMS DE BIENVENUE (après vérification)
                if (telephone != null && !telephone.isEmpty()) {
                    boolean smsSent = SmsService.sendWelcomeSms(createdUser);
                    if (smsSent) {
                        System.out.println("📱 SMS de bienvenue envoyé à: " + telephone);
                    }
                }

                // ✅ NOTIFICATION ADMIN
                User admin = UserSession.getInstance().getCurrentUser();
                String adminName = (admin != null) ? admin.getPrenom() + " " + admin.getNom() : "Système";
                notifService.notifyNewUser(adminName, prenom + " " + nom);

                showSuccessMessage();
                redirectToLogin(event);
            } else {
                // ❌ SI CODE INCORRECT, SUPPRIMER LE COMPTE
                userCRUD.deleteUser(createdUser);
                showAlert(Alert.AlertType.ERROR,
                        "❌ Vérification échouée",
                        "Code incorrect",
                        "Le code de vérification est incorrect. Votre inscription a été annulée.");
                signUpBtn.setDisable(false);
                signUpBtn.setText("S'INSCRIRE");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR,
                    "❌ Erreur d'inscription",
                    "Création de compte échouée",
                    "Erreur: " + e.getMessage());
            signUpBtn.setDisable(false);
            signUpBtn.setText("S'INSCRIRE");
        }
    }

    private boolean showVerificationDialog(String email, String expectedCode) {
        // Créer la boîte de dialogue
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("🔐 Vérification email");
        dialog.setHeaderText("Code de vérification envoyé à " + email);

        // Style
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-border-color: #1ABC9C; -fx-border-width: 2; -fx-border-radius: 15; -fx-background-radius: 15;");

        // Boutons
        ButtonType verifyButtonType = new ButtonType("VÉRIFIER", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(verifyButtonType, ButtonType.CANCEL);

        // Contenu
        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(20));

        Label instructionLabel = new Label("Entrez le code à 6 chiffres reçu par email :");
        instructionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2C3E50;");

        TextField codeField = new TextField();
        codeField.setPromptText("123456");
        codeField.setStyle("-fx-font-size: 18px; -fx-alignment: center; -fx-pref-width: 200;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 12px;");

        content.getChildren().addAll(instructionLabel, codeField, errorLabel);
        dialogPane.setContent(content);

        // Validation
        Button verifyButton = (Button) dialog.getDialogPane().lookupButton(verifyButtonType);
        verifyButton.setStyle("-fx-background-color: #F39C12; -fx-text-fill: white; -fx-font-weight: bold;");

        // Activer/désactiver le bouton
        verifyButton.addEventFilter(ActionEvent.ACTION, event -> {
            String enteredCode = codeField.getText().trim();
            if (enteredCode.isEmpty()) {
                errorLabel.setText("❌ Veuillez saisir le code");
                event.consume();
            } else if (!enteredCode.equals(expectedCode)) {
                errorLabel.setText("❌ Code incorrect");
                event.consume();
            }
        });

        Optional<String> result = dialog.showAndWait();
        return result.isPresent();
    }

    @FXML
    private void handleLoginRedirect(ActionEvent event) {
        redirectToLogin(event);
    }

    private void redirectToLogin(ActionEvent event) {
        try {
            System.out.println("🔄 Redirection vers login...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));

            if (loader.getLocation() == null) {
                throw new Exception("Fichier login.fxml introuvable");
            }

            Parent root = loader.load();
            Stage stage;

            if (event != null) {
                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            } else {
                stage = (Stage) prenomField.getScene().getWindow();
            }

            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR,
                    "❌ Erreur de navigation",
                    "Redirection impossible",
                    "Impossible de charger la page de connexion.\n\n" +
                            "Chemin: /user/login/login.fxml\n" +
                            "Erreur: " + e.getMessage());
        }
    }

    private void showSuccessMessage() {
        String fullName = prenomField.getText().trim() + " " + nomField.getText().trim();
        String telephone = telephoneField.getText().trim();

        String smsMessage = "";
        if (telephone != null && !telephone.isEmpty()) {
            smsMessage = "📱 Un SMS de bienvenue vous a été envoyé.\n";
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("✅ Inscription réussie");
        alert.setHeaderText("Bienvenue sur RE7LA Tunisie !");
        alert.setContentText(
                "👤 " + fullName + "\n" +
                        "📧 " + emailField.getText().trim() + "\n" +
                        "📞 " + (telephone.isEmpty() ? "Non renseigné" : telephone) + "\n\n" +
                        "✨ Votre compte a été créé avec succès.\n" +
                        "📧 Un email de bienvenue vous a été envoyé.\n" +
                        smsMessage +
                        "🌍 Vous allez être redirigé vers la page de connexion."
        );

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #1ABC9C; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 15; " +
                "-fx-background-radius: 15;");
        dialogPane.lookup(".header-panel").setStyle("-fx-background-color: linear-gradient(to right, #1ABC9C20, #F39C1220);");

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
}
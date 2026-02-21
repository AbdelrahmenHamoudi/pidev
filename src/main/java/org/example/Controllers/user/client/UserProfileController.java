package org.example.Controllers.user.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.application.Platform;
import org.example.Entites.user.User;
import org.example.Services.user.UserCRUD;
import org.example.Utils.UserSession;
import org.example.Services.user.APIservices.JWTService;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.ResourceBundle;

public class UserProfileController implements Initializable {

    // ============ HEADER ============
    @FXML private ImageView profilePhotoImage;
    @FXML private Label photoFileNameLabel;
    @FXML private Label userNameDisplay;
    @FXML private Label userEmailDisplay;
    @FXML private Label userPointsDisplay;

    // ============ FORMULAIRE ============
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private DatePicker birthDatePicker;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    // ============ LABELS D'ERREUR ============
    @FXML private Label firstNameError;
    @FXML private Label lastNameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label birthDateError;
    @FXML private Label newPasswordError;
    @FXML private Label confirmPasswordError;

    // ============ UTILISATEUR CONNECTÉ ============
    private User currentUser;
    private File selectedImageFile;

    // ============ SERVICE CRUD ============
    private final UserCRUD userCRUD = new UserCRUD();

    // ============ FLAG POUR ÉVITER LES VÉRIFICATIONS MULTIPLES ============
    private boolean isCheckingEmail = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ✅ 1. VÉRIFICATION JWT - TOKEN VALIDE ?
        if (!UserSession.getInstance().isTokenValid()) {
            System.out.println("⚠️ Token invalide ou expiré - Redirection vers login");
            Platform.runLater(this::redirectToLogin);
            return;
        }

        // ✅ 2. RÉCUPÉRER L'UTILISATEUR DEPUIS LA SESSION
        currentUser = UserSession.getInstance().getCurrentUser();

        // ✅ 3. VÉRIFICATION SUPPLÉMENTAIRE - CORRESPONDANCE TOKEN/SESSION
        String token = UserSession.getInstance().getToken();
        Integer userIdFromToken = JWTService.extractUserId(token);

        if (userIdFromToken == null) {
            System.out.println("❌ Token invalide - ID non trouvé");
            Platform.runLater(this::redirectToLogin);
            return;
        }

        if (currentUser == null || currentUser.getId() != userIdFromToken) {
            System.out.println("⚠️ Incohérence entre token et session - Reconnexion nécessaire");
            UserSession.getInstance().clearSession();
            Platform.runLater(this::redirectToLogin);
            return;
        }

        // ✅ 4. AFFICHAGE DES INFOS JWT
        System.out.println("\n🔐 === PROFIL UTILISATEUR AVEC JWT ===");
        System.out.println("👤 Utilisateur: " + currentUser.getE_mail());
        System.out.println("📝 Token valide jusqu'au: " + JWTService.extractExpiration(token));
        System.out.println("🆔 ID depuis token: " + userIdFromToken);
        System.out.println("🔐 ===============================\n");

        // ✅ 5. AFFICHER LES DONNÉES UTILISATEUR
        displayUserInfo();

        setupRealTimeValidation();
        setupPasswordStrengthListener();
        System.out.println("✅ Page de profil initialisée avec JWT");
    }

    /**
     * ✅ REDIRECTION VERS LA PAGE DE LOGIN
     */
    private void redirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) firstNameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayUserInfo() {
        if (currentUser != null) {
            userNameDisplay.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            userEmailDisplay.setText(currentUser.getE_mail());
            userPointsDisplay.setText("🏆 " + calculateUserPoints(currentUser) + " points");

            firstNameField.setText(currentUser.getPrenom());
            lastNameField.setText(currentUser.getNom());
            emailField.setText(currentUser.getE_mail());
            phoneField.setText(currentUser.getNum_tel() != null ? currentUser.getNum_tel() : "");

            if (currentUser.getImage() != null && !currentUser.getImage().isEmpty()) {
                try {
                    Image img = new Image(currentUser.getImage(), 100, 100, true, true);
                    profilePhotoImage.setImage(img);
                    photoFileNameLabel.setText("Photo de profil");
                } catch (Exception e) {
                    System.err.println("❌ Erreur chargement image: " + e.getMessage());
                }
            }

            if (currentUser.getDate_naiss() != null && !currentUser.getDate_naiss().isEmpty()) {
                try {
                    birthDatePicker.setValue(LocalDate.parse(currentUser.getDate_naiss()));
                } catch (DateTimeParseException e) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        birthDatePicker.setValue(LocalDate.parse(currentUser.getDate_naiss(), formatter));
                    } catch (Exception ex) {
                        birthDatePicker.setValue(null);
                    }
                }
            }

            firstNameField.setText(firstNameField.getText());
            lastNameField.setText(lastNameField.getText());
            emailField.setText(emailField.getText());
            phoneField.setText(phoneField.getText());
        }
    }

    /**
     * ✅ VALIDATION EN TEMPS RÉEL - TOUS LES CHAMPS
     */
    private void setupRealTimeValidation() {
        // === PRÉNOM ===
        firstNameField.textProperty().addListener((obs, old, newVal) -> {
            String val = newVal.trim();
            if (val.isEmpty()) {
                showError(firstNameError, "❌ Le prénom est requis");
            } else if (val.length() < 2) {
                showError(firstNameError, "❌ Minimum 2 caractères");
            } else if (val.length() > 50) {
                showError(firstNameError, "❌ Maximum 50 caractères");
            } else if (!val.matches("^[a-zA-ZÀ-ÿ\\s-]+$")) {
                showError(firstNameError, "❌ Caractères non autorisés (lettres uniquement)");
            } else {
                clearError(firstNameError);
            }
        });

        // === NOM ===
        lastNameField.textProperty().addListener((obs, old, newVal) -> {
            String val = newVal.trim();
            if (val.isEmpty()) {
                showError(lastNameError, "❌ Le nom est requis");
            } else if (val.length() < 2) {
                showError(lastNameError, "❌ Minimum 2 caractères");
            } else if (val.length() > 50) {
                showError(lastNameError, "❌ Maximum 50 caractères");
            } else if (!val.matches("^[a-zA-ZÀ-ÿ\\s-]+$")) {
                showError(lastNameError, "❌ Caractères non autorisés (lettres uniquement)");
            } else {
                clearError(lastNameError);
            }
        });

        // === EMAIL ===
        emailField.textProperty().addListener((obs, old, newVal) -> {
            String email = newVal.trim().toLowerCase();

            if (email.isEmpty()) {
                showError(emailError, "❌ L'email est requis");
            } else if (email.length() > 100) {
                showError(emailError, "❌ Email trop long (max 100 caractères)");
            } else if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                showError(emailError, "❌ Format d'email invalide");
            } else {
                clearError(emailError);

                if (!isCheckingEmail && currentUser != null && !email.equals(currentUser.getE_mail())) {
                    isCheckingEmail = true;
                    String emailToCheck = email;

                    new Thread(() -> {
                        try {
                            Thread.sleep(500);
                            User existing = userCRUD.getUserByEmail(emailToCheck);

                            javafx.application.Platform.runLater(() -> {
                                if (existing != null) {
                                    showError(emailError, "❌ Cet email est déjà utilisé");
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

        // === TÉLÉPHONE ===
        phoneField.textProperty().addListener((obs, old, newVal) -> {
            String tel = newVal.trim();
            if (!tel.isEmpty()) {
                if (tel.length() > 20) {
                    showError(phoneError, "❌ Numéro trop long");
                } else if (!tel.matches("^(\\+216|0)?[2-9][0-9]{7}$")) {
                    showError(phoneError, "❌ Format tunisien invalide (ex: +21699123456)");
                } else {
                    clearError(phoneError);
                }
            } else {
                clearError(phoneError);
            }
        });

        // === DATE DE NAISSANCE ===
        birthDatePicker.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                int age = Period.between(newVal, LocalDate.now()).getYears();
                if (age < 18) {
                    showError(birthDateError, "❌ Vous devez avoir au moins 18 ans");
                } else if (age > 120) {
                    showError(birthDateError, "❌ Date invalide");
                } else {
                    clearError(birthDateError);
                }
            } else {
                clearError(birthDateError);
            }
        });

        // === NOUVEAU MOT DE PASSE ===
        newPasswordField.textProperty().addListener((obs, old, newVal) -> {
            validateNewPassword();
        });

        // === CONFIRMATION MOT DE PASSE ===
        confirmPasswordField.textProperty().addListener((obs, old, newVal) -> {
            validateConfirmPassword();
        });
    }

    /**
     * ✅ VALIDATION SPÉCIFIQUE DU MOT DE PASSE
     */
    private void validateNewPassword() {
        String password = newPasswordField.getText();

        if (password.isEmpty()) {
            clearError(newPasswordError);
        } else if (password.length() < 8) {
            showError(newPasswordError, "❌ Minimum 8 caractères");
        } else if (password.length() > 255) {
            showError(newPasswordError, "❌ Mot de passe trop long");
        } else if (!password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
            showError(newPasswordError, "❌ Doit contenir lettres ET chiffres");
        } else if (!password.matches(".*[A-Z].*")) {
            showError(newPasswordError, "❌ Doit contenir au moins une majuscule");
        } else if (!password.matches(".*[a-z].*")) {
            showError(newPasswordError, "❌ Doit contenir au moins une minuscule");
        } else {
            clearError(newPasswordError);
        }

        if (!confirmPasswordField.getText().isEmpty()) {
            validateConfirmPassword();
        }
    }

    /**
     * ✅ VALIDATION DE LA CONFIRMATION DU MOT DE PASSE
     */
    private void validateConfirmPassword() {
        String password = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (!confirm.isEmpty() && !confirm.equals(password)) {
            showError(confirmPasswordError, "❌ Les mots de passe ne correspondent pas");
        } else {
            clearError(confirmPasswordError);
        }
    }

    /**
     * ✅ INDICATEUR DE FORCE DU MOT DE PASSE
     */
    private void setupPasswordStrengthListener() {
        newPasswordField.textProperty().addListener((obs, old, newVal) -> {
        });
    }

    /**
     * ✅ AFFICHER UN MESSAGE D'ERREUR
     */
    private void showError(Label label, String message) {
        label.setText(message);
        label.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 11px; -fx-font-weight: bold;");
    }

    /**
     * ✅ EFFACER UN MESSAGE D'ERREUR
     */
    private void clearError(Label label) {
        label.setText("");
    }

    /**
     * ✅ VÉRIFIER S'IL Y A DES ERREURS
     */
    private boolean hasErrors() {
        return !firstNameError.getText().isEmpty() ||
                !lastNameError.getText().isEmpty() ||
                !emailError.getText().isEmpty() ||
                !phoneError.getText().isEmpty() ||
                !birthDateError.getText().isEmpty() ||
                !newPasswordError.getText().isEmpty() ||
                !confirmPasswordError.getText().isEmpty();
    }

    /**
     * ✅ INITIALISER LES DONNÉES DE L'UTILISATEUR
     */
    public void initUserData(User user) {
        this.currentUser = user;
        if (user != null) {
            displayUserInfo();
        }
    }

    /**
     * ✅ CALCULER LES POINTS DE L'UTILISATEUR
     */
    private int calculateUserPoints(User user) {
        return 1000 + (user.getId() * 10);
    }

    // ============ GESTIONNAIRES D'ÉVÉNEMENTS ============

    @FXML
    private void handleLogoClick(MouseEvent event) {
        handleHome(new ActionEvent(event.getSource(), null));
    }

    @FXML
    private void handleHome(ActionEvent event) {
        // ✅ Vérification JWT avant de naviguer
        if (!UserSession.getInstance().isTokenValid()) {
            showAlert("Session expirée", "Veuillez vous reconnecter", Alert.AlertType.WARNING);
            redirectToLogin();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/dashboard/homeClient.fxml"));
            Parent root = loader.load();

            homeClientController clientController = loader.getController();
            clientController.initUserData(currentUser);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Accueil");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de retourner à l'accueil", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleEditPhoto(ActionEvent event) {
        handleChangePhoto(event);
    }

    @FXML
    private void handleChangePhoto(ActionEvent event) {
        // ✅ Vérification JWT
        if (!UserSession.getInstance().isTokenValid()) {
            showAlert("Session expirée", "Veuillez vous reconnecter", Alert.AlertType.WARNING);
            redirectToLogin();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp")
        );

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                selectedImageFile = file;
                String imageUrl = file.toURI().toString();
                Image img = new Image(imageUrl, 100, 100, true, true);
                profilePhotoImage.setImage(img);
                photoFileNameLabel.setText("Fichier: " + file.getName());

                if (currentUser != null) {
                    currentUser.setImage(imageUrl);
                    userCRUD.updateImageUser(currentUser);
                    showAlert("Succès", "Photo mise à jour avec succès", Alert.AlertType.INFORMATION);
                }
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de charger l'image", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleSaveProfile(ActionEvent event) {
        // ✅ VÉRIFICATION JWT AVANT TOUTE ACTION
        if (!UserSession.getInstance().isTokenValid()) {
            showAlert("Session expirée", "Veuillez vous reconnecter", Alert.AlertType.WARNING);
            redirectToLogin();
            return;
        }

        // ✅ Vérification supplémentaire : comparer l'ID du token avec currentUser
        String token = UserSession.getInstance().getToken();
        Integer userIdFromToken = JWTService.extractUserId(token);

        if (userIdFromToken == null || userIdFromToken != currentUser.getId()) {
            showAlert("Erreur", "Session invalide - Veuillez vous reconnecter", Alert.AlertType.ERROR);
            UserSession.getInstance().clearSession();
            redirectToLogin();
            return;
        }

        if (hasErrors()) {
            showAlert("Validation", "Veuillez corriger les erreurs avant d'enregistrer", Alert.AlertType.WARNING);
            return;
        }

        if (currentUser == null) {
            showAlert("Erreur", "Aucun utilisateur connecté", Alert.AlertType.ERROR);
            return;
        }

        try {
            String newEmail = emailField.getText().trim().toLowerCase();
            if (!newEmail.equals(currentUser.getE_mail())) {
                User existing = userCRUD.getUserByEmail(newEmail);
                if (existing != null) {
                    showError(emailError, "❌ Cet email est déjà utilisé");
                    showAlert("Erreur", "Cet email est déjà utilisé par un autre compte", Alert.AlertType.ERROR);
                    return;
                }
            }

            currentUser.setPrenom(firstNameField.getText().trim());
            currentUser.setNom(lastNameField.getText().trim());
            currentUser.setE_mail(newEmail);
            currentUser.setNum_tel(phoneField.getText().trim().isEmpty() ? null : phoneField.getText().trim());

            if (birthDatePicker.getValue() != null) {
                currentUser.setDate_naiss(birthDatePicker.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            } else {
                currentUser.setDate_naiss(null);
            }

            if (!newPasswordField.getText().isEmpty()) {
                if (newPasswordField.getText().equals(confirmPasswordField.getText())) {
                    currentUser.setMot_de_pass(newPasswordField.getText());
                    userCRUD.updatePassword(currentUser);

                    // ✅ Régénérer le token après changement de mot de passe
                    UserSession.getInstance().setCurrentUser(currentUser);
                    System.out.println("🔄 Token régénéré après changement de mot de passe");
                }
            }

            userCRUD.updateUser(currentUser);

            userNameDisplay.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            userEmailDisplay.setText(currentUser.getE_mail());

            showAlert("Succès", "Profil mis à jour avec succès", Alert.AlertType.INFORMATION);

            newPasswordField.clear();
            confirmPasswordField.clear();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la mise à jour: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        // ✅ Vérification JWT
        if (!UserSession.getInstance().isTokenValid()) {
            redirectToLogin();
            return;
        }

        if (currentUser != null) {
            firstNameField.setText(currentUser.getPrenom());
            lastNameField.setText(currentUser.getNom());
            emailField.setText(currentUser.getE_mail());
            phoneField.setText(currentUser.getNum_tel() != null ? currentUser.getNum_tel() : "");

            if (currentUser.getDate_naiss() != null && !currentUser.getDate_naiss().isEmpty()) {
                try {
                    birthDatePicker.setValue(LocalDate.parse(currentUser.getDate_naiss()));
                } catch (Exception e) {
                    birthDatePicker.setValue(null);
                }
            } else {
                birthDatePicker.setValue(null);
            }

            newPasswordField.clear();
            confirmPasswordField.clear();
        }

        clearAllErrors();
    }

    @FXML
    private void handleDeleteAccount(ActionEvent event) {
        // ✅ VÉRIFICATION JWT AVANT SUPPRESSION
        if (!UserSession.getInstance().isTokenValid()) {
            showAlert("Session expirée", "Veuillez vous reconnecter", Alert.AlertType.WARNING);
            redirectToLogin();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("🗑️ Confirmation de suppression");
        confirm.setHeaderText("Êtes-vous sûr de vouloir supprimer votre compte ?");
        confirm.setContentText("Cette action est irréversible. Toutes vos données seront définitivement supprimées.");

        DialogPane dialogPane = confirm.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-border-color: #E74C3C; -fx-border-width: 2; -fx-border-radius: 15; -fx-background-radius: 15;");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userCRUD.deleteUser(currentUser);
                UserSession.getInstance().clearSession();

                showAlert("Compte supprimé", "Votre compte a été supprimé avec succès.", Alert.AlertType.INFORMATION);

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("RE7LA Tunisie - Connexion");
                stage.centerOnScreen();

            } catch (Exception e) {
                showAlert("Erreur", "Impossible de supprimer le compte: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            // ✅ Afficher les infos avant déconnexion
            System.out.println("👋 Déconnexion de: " + currentUser.getE_mail());

            // ✅ Effacer la session (token inclus)
            UserSession.getInstance().clearSession();
            System.out.println("✅ Token JWT effacé - Session fermée");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
        } catch (Exception e) {
            showAlert("Erreur", "Déconnexion impossible: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ============ UTILITAIRES ============

    private void clearAllErrors() {
        clearError(firstNameError);
        clearError(lastNameError);
        clearError(emailError);
        clearError(phoneError);
        clearError(birthDateError);
        clearError(newPasswordError);
        clearError(confirmPasswordError);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
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
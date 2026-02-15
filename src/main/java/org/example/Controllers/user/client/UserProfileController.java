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
import org.example.Entites.user.User;
import org.example.Services.user.UserCRUD;

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
        setupRealTimeValidation();
        setupPasswordStrengthListener();
        System.out.println("✅ Page de profil initialisée avec contrôle de saisie complet");
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

            // Validation format
            if (email.isEmpty()) {
                showError(emailError, "❌ L'email est requis");
            } else if (email.length() > 100) {
                showError(emailError, "❌ Email trop long (max 100 caractères)");
            } else if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                showError(emailError, "❌ Format d'email invalide");
            } else {
                clearError(emailError);

                // ✅ Vérifier si l'email existe déjà (sauf pour l'utilisateur actuel)
                if (!isCheckingEmail && currentUser != null && !email.equals(currentUser.getE_mail())) {
                    isCheckingEmail = true;
                    String emailToCheck = email;

                    new Thread(() -> {
                        try {
                            Thread.sleep(500); // Délai pour éviter trop de requêtes
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
                clearError(phoneError); // Téléphone optionnel
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
                clearError(birthDateError); // Date optionnelle
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
            clearError(newPasswordError); // Optionnel en modification
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

        // Revalider la confirmation si elle existe
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
            // Cette méthode peut être implémentée pour afficher la force du mot de passe
            // avec des barres de progression ou des couleurs
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
            // Afficher les informations
            userNameDisplay.setText(user.getPrenom() + " " + user.getNom());
            userEmailDisplay.setText(user.getE_mail());
            userPointsDisplay.setText("🏆 " + calculateUserPoints(user) + " points");

            // Remplir le formulaire
            firstNameField.setText(user.getPrenom());
            lastNameField.setText(user.getNom());
            emailField.setText(user.getE_mail());
            phoneField.setText(user.getNum_tel() != null ? user.getNum_tel() : "");

            // Charger l'image
            if (user.getImage() != null && !user.getImage().isEmpty()) {
                try {
                    Image img = new Image(user.getImage(), 100, 100, true, true);
                    profilePhotoImage.setImage(img);
                    photoFileNameLabel.setText("Photo de profil");
                } catch (Exception e) {
                    System.err.println("❌ Erreur chargement image: " + e.getMessage());
                }
            }

            // Charger la date de naissance
            if (user.getDate_naiss() != null && !user.getDate_naiss().isEmpty()) {
                try {
                    birthDatePicker.setValue(LocalDate.parse(user.getDate_naiss()));
                } catch (DateTimeParseException e) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        birthDatePicker.setValue(LocalDate.parse(user.getDate_naiss(), formatter));
                    } catch (Exception ex) {
                        birthDatePicker.setValue(null);
                    }
                }
            }

            // Forcer les validations
            firstNameField.setText(firstNameField.getText());
            lastNameField.setText(lastNameField.getText());
            emailField.setText(emailField.getText());
            phoneField.setText(phoneField.getText());
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

                // Mettre à jour dans la base de données
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
        // ✅ Vérifier s'il y a des erreurs de validation
        if (hasErrors()) {
            showAlert("Validation", "Veuillez corriger les erreurs avant d'enregistrer", Alert.AlertType.WARNING);
            return;
        }

        if (currentUser == null) {
            showAlert("Erreur", "Aucun utilisateur connecté", Alert.AlertType.ERROR);
            return;
        }

        try {
            // Vérifier si l'email a changé et s'il est unique
            String newEmail = emailField.getText().trim().toLowerCase();
            if (!newEmail.equals(currentUser.getE_mail())) {
                User existing = userCRUD.getUserByEmail(newEmail);
                if (existing != null) {
                    showError(emailError, "❌ Cet email est déjà utilisé");
                    showAlert("Erreur", "Cet email est déjà utilisé par un autre compte", Alert.AlertType.ERROR);
                    return;
                }
            }

            // Mettre à jour les informations
            currentUser.setPrenom(firstNameField.getText().trim());
            currentUser.setNom(lastNameField.getText().trim());
            currentUser.setE_mail(newEmail);
            currentUser.setNum_tel(phoneField.getText().trim().isEmpty() ? null : phoneField.getText().trim());

            if (birthDatePicker.getValue() != null) {
                currentUser.setDate_naiss(birthDatePicker.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            } else {
                currentUser.setDate_naiss(null);
            }

            // Mettre à jour le mot de passe si fourni
            if (!newPasswordField.getText().isEmpty()) {
                if (newPasswordField.getText().equals(confirmPasswordField.getText())) {
                    currentUser.setMot_de_pass(newPasswordField.getText());
                    userCRUD.updatePassword(currentUser);
                }
            }

            // Sauvegarder dans la base de données
            userCRUD.updateUser(currentUser);

            // Mettre à jour l'affichage
            userNameDisplay.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            userEmailDisplay.setText(currentUser.getE_mail());

            showAlert("Succès", "Profil mis à jour avec succès", Alert.AlertType.INFORMATION);

            // Effacer les champs de mot de passe
            newPasswordField.clear();
            confirmPasswordField.clear();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la mise à jour: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        // Recharger les données originales
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
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("🗑️ Confirmation de suppression");
        confirm.setHeaderText("Êtes-vous sûr de vouloir supprimer votre compte ?");
        confirm.setContentText("Cette action est irréversible. Toutes vos données seront définitivement supprimées.");

        DialogPane dialogPane = confirm.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-border-color: #E74C3C; -fx-border-width: 2; -fx-border-radius: 15; -fx-background-radius: 15;");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Supprimer le compte
                userCRUD.deleteUser(currentUser);

                showAlert("Compte supprimé", "Votre compte a été supprimé avec succès.", Alert.AlertType.INFORMATION);

                // Rediriger vers la page de connexion
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
package org.example.Controllers.user.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.example.Entites.user.User;

import java.net.URL;
import java.util.ResourceBundle;

public class homeClientController implements Initializable {

    @FXML
    private ImageView userAvatarImage;

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userPointsLabel;


    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        System.out.println("✅ Page d'accueil client initialisée");
    }


    public void initUserData(User user) {
        this.currentUser = user;

        if (user != null) {

            String fullName = user.getPrenom() + " " + user.getNom();
            userNameLabel.setText(fullName);


            if (user.getImage() != null && !user.getImage().isEmpty()) {
                try {
                    Image avatarImage = new Image(user.getImage(), 42, 42, true, true);
                    userAvatarImage.setImage(avatarImage);
                } catch (Exception e) {
                    System.err.println("❌ Erreur chargement avatar: " + e.getMessage());

                }
            }


            int points = calculateUserPoints(user);
            userPointsLabel.setText("🏆 " + points + " points");

            System.out.println("✅ Utilisateur chargé: " + fullName);
        } else {

            userNameLabel.setText("Invité");
            userPointsLabel.setText("🏆 0 points");
        }
    }


    private int calculateUserPoints(User user) {

        return 1000 + (user.getId() * 10);
    }


    @FXML
    private void handleLogoClick(MouseEvent event) {
        System.out.println("🏠 Logo cliqué - Retour à l'accueil");

    }

    @FXML
    private void handleUserMenuClick(MouseEvent event) {
        if (currentUser != null) {
            System.out.println("👤 Redirection vers le profil pour: " + currentUser.getPrenom());
            redirectToUserProfile(event);
        } else {
            System.out.println("👤 Menu utilisateur cliqué (non connecté)");

            redirectToLogin(event);
        }
    }


    private void redirectToUserProfile(MouseEvent event) {
        try {
            System.out.println("🔄 Redirection vers le profil utilisateur...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/front/userProfil.fxml"));

            if (loader.getLocation() == null) {
                throw new Exception("Fichier userProfil.fxml introuvable");
            }

            Parent root = loader.load();

            UserProfileController profileController = loader.getController();
            profileController.initUserData(currentUser);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Mon Profil");
            stage.centerOnScreen();
            stage.show();

            System.out.println("✅ Redirection vers le profil réussie");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR,
                    "❌ Erreur de navigation",
                    "Impossible d'accéder à la page de profil",
                    "Erreur: " + e.getMessage());
        }
    }


    @FXML
    private void handleSearch(ActionEvent event) {
        System.out.println("🔍 Recherche effectuée");
        showAlert(Alert.AlertType.INFORMATION,
                "Recherche",
                "Fonctionnalité de recherche",
                "La recherche sera bientôt disponible.");
    }


    @FXML
    private void handleViewAllDestinations(ActionEvent event) {
        System.out.println("🌍 Voir toutes les destinations");
        showAlert(Alert.AlertType.INFORMATION,
                "Destinations",
                "Toutes les destinations",
                "Cette fonctionnalité sera bientôt disponible.");
    }


    @FXML
    private void handleViewAllHebergements(ActionEvent event) {
        System.out.println("🏨 Voir tous les hébergements");
        showAlert(Alert.AlertType.INFORMATION,
                "Hébergements",
                "Tous les hébergements",
                "Cette fonctionnalité sera bientôt disponible.");
    }


    @FXML
    private void handleViewAllActivites(ActionEvent event) {
        System.out.println("🎯 Voir toutes les activités");
        showAlert(Alert.AlertType.INFORMATION,
                "Activités",
                "Toutes les activités",
                "Cette fonctionnalité sera bientôt disponible.");
    }


    @FXML
    private void handleViewAllPromotions(ActionEvent event) {
        System.out.println("🏷️ Voir toutes les promotions");
        showAlert(Alert.AlertType.INFORMATION,
                "Promotions",
                "Toutes les promotions",
                "Cette fonctionnalité sera bientôt disponible.");
    }


    @FXML
    private void handleViewAllReviews(ActionEvent event) {
        System.out.println("💬 Voir tous les avis");
        showAlert(Alert.AlertType.INFORMATION,
                "Avis",
                "Tous les avis",
                "Cette fonctionnalité sera bientôt disponible.");
    }
    @FXML
    private void handleReserverHebergement(ActionEvent event) {
        if (currentUser != null) {
            System.out.println("📅 Réservation d'hébergement par: " + currentUser.getPrenom());
            showAlert(Alert.AlertType.INFORMATION,
                    "Réservation",
                    "Réservation d'hébergement",
                    "Cette fonctionnalité sera bientôt disponible.");
        } else {
            redirectToLogin(event);
        }
    }


    @FXML
    private void handleReserverActivite(ActionEvent event) {
        if (currentUser != null) {
            System.out.println("📅 Réservation d'activité par: " + currentUser.getPrenom());
            showAlert(Alert.AlertType.INFORMATION,
                    "Réservation",
                    "Réservation d'activité",
                    "Cette fonctionnalité sera bientôt disponible.");
        } else {
            redirectToLogin(event);
        }
    }


    private void redirectToLogin(MouseEvent event) {
        try {
            System.out.println("🔄 Redirection vers login...");

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


    private void redirectToLogin(ActionEvent event) {
        try {
            System.out.println("🔄 Redirection vers login...");

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
    private void handleLogout(ActionEvent event) {
        try {
            System.out.println("🚪 Déconnexion...");
            redirectToLogin(event);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR,
                    "❌ Erreur",
                    "Déconnexion impossible",
                    e.getMessage());
        }
    }
}
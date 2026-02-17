package org.example.GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;

public class HomePage extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // ✅ Chargement avec vérification
            System.out.println("🔍 Chargement de login.fxml...");

            java.net.URL fxmlUrl = getClass().getResource("/user/login/login.fxml");

            if (fxmlUrl == null) {
                System.err.println("❌ Fichier introuvable: /user/login/login.fxml");

                // Afficher les ressources disponibles pour debug
                System.err.println("📁 Ressources disponibles dans /user/:");
                java.net.URL userUrl = getClass().getResource("/user");
                if (userUrl != null) {
                    try {
                        java.io.File userFolder = new java.io.File(userUrl.toURI());
                        for (java.io.File file : userFolder.listFiles()) {
                            System.err.println("   - " + file.getName());
                        }
                    } catch (Exception ex) {
                        System.err.println("   Impossible de lister");
                    }
                }

                throw new Exception("login.fxml introuvable");
            }

            Parent root = FXMLLoader.load(fxmlUrl);
            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
            stage.show();

            System.out.println("✅ Application démarrée avec succès!");

        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("❌ Erreur de démarrage",
                    "Impossible de charger l'interface",
                    "Fichier: /user/login/login.fxml\n\n" +
                            "🔧 SOLUTION:\n" +
                            "1️⃣ Vérifiez que le dossier 'login' existe dans src/main/resources/user/\n" +
                            "2️⃣ Vérifiez que le fichier 'login.fxml' existe\n" +
                            "3️⃣ Exécutez 'mvn clean compile'\n" +
                            "4️⃣ Vérifiez target/classes/user/login/login.fxml\n\n" +
                            "📋 Erreur: " + e.getMessage());
        }
    }

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #F39C12; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 15; " +
                "-fx-background-radius: 15;");

        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
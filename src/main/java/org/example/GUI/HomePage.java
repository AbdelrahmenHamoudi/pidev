package org.example.GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HomePage extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // Charger le FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/hebergement/back/HebergementBack.fxml"));
            Parent root = loader.load();

            // Créer la scène
            Scene scene = new Scene(root);

            // Ajouter le CSS
            scene.getStylesheets().add(getClass().getResource("/hebergement/back/StyleHB.css").toExternalForm());

            // Configurer la fenêtre
            stage.setTitle("Gestion Hébergements - Travel Admin");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement du FXML ou du CSS !");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

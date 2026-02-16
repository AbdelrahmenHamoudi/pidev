package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Charger le BackOffice par défaut (vous pouvez changer pour FrontOffice)
        Parent root = FXMLLoader.load(getClass().getResource("/views/backoffice/PromotionBackOffice.fxml"));
        //Parent root = FXMLLoader.load(getClass().getResource("/views/frontoffice/PromotionFrontOffice.fxml"));

        Scene scene = new Scene(root, 1100, 700);
        
        primaryStage.setTitle("RE7LA - Gestion des Promotions");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

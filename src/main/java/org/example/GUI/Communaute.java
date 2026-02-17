package org.example.GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Communaute extends Application {

    @Override
    public void start(Stage primaryStage) {

        System.out.println("STEP 1: Application started");

        try {
            System.out.println("STEP 2: Before FXML load");

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Communaute.fxml")
            );

            Parent root = loader.load();

            System.out.println("STEP 3: After FXML load");

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/Communaute.css").toExternalForm());

            System.out.println("STEP 4: Scene created");

            primaryStage.setScene(scene);

            System.out.println("STEP 5: Before show()");

            primaryStage.show();

            System.out.println("STEP 6: Stage shown");

        } catch (Exception e) {
            System.out.println("ERROR OCCURRED:");
            e.printStackTrace();
        }
    }

}

package org.example;

import animatefx.animation.FadeIn;
import animatefx.animation.ZoomIn;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.services.NotificationService;
import org.example.services.SchedulerService;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        // ══════════════════════════════════════════════
        // 1. QUARTZ — démarrage + planification auto
        //    Planifie les jobs de toutes les promos en DB
        // ══════════════════════════════════════════════
        SchedulerService scheduler = SchedulerService.getInstance();
        scheduler.start();

        // ── Test notifications (décommenter pour voir immédiatement) ──
         //scheduler.scheduleTestIn5s();

        // ══════════════════════════════════════════════
        // 2. INTERFACE JAVAFX
        // ══════════════════════════════════════════════
       /* Parent root = FXMLLoader.load(
                getClass().getResource("/views/backoffice/PromotionBackOffice.fxml")
        );*/
         //Décommenter pour FrontOffice :
         Parent root = FXMLLoader.load(getClass().getResource("/views/frontoffice/PromotionFrontOffice.fxml"));

        Scene scene = new Scene(root, 1200, 760);
        primaryStage.setTitle("RE7LA — Gestion des Promotions");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        primaryStage.setScene(scene);
        primaryStage.show();

        // ══════════════════════════════════════════════
        // 3. ANIMATEFX — animation d'entrée de l'app
        // ══════════════════════════════════════════════
        root.setOpacity(0);
        ZoomIn zoomIn = new ZoomIn(root);
        zoomIn.setOnFinished(e -> {
            root.setOpacity(1);
            // Toast de bienvenue avec info Quartz
            NotificationService.getInstance().info(
                    "RE7LA — Démarré !",
                    "Scheduler actif · Promotions suivies automatiquement."
            );
        });
        root.setOpacity(1);
        zoomIn.play();

        // ══════════════════════════════════════════════
        // 4. ARRÊT PROPRE de Quartz à la fermeture
        // ══════════════════════════════════════════════
        primaryStage.setOnCloseRequest(e -> {
            System.out.println("[App] Fermeture — arrêt du scheduler...");
            scheduler.stop();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
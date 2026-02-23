import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Classe principale pour lancer l'application RE7LA
 * 
 * Configuration VM Options requise:
 * --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Choisissez quelle interface lancer:
        // Option 1: Back Office (Administration)
        // launchBackOffice(primaryStage);
        
        // Option 2: Front Office (Client)
        launchFrontOffice(primaryStage);
        
        // Option 3: Menu de sélection
        // launchSelectionMenu(primaryStage);
    }

    /**
     * Lance l'interface Back Office (Administration des activités)
     */
    private void launchBackOffice(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/ActiviteBackOffice.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 1200, 800);
        
        stage.setTitle("RE7LA - Back Office - Gestion des Activités");
        stage.setScene(scene);
        stage.setMaximized(true);
        
        // Ajouter l'icône (optionnel)
        // stage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/images/logo.png")));
        
        stage.show();
    }

    /**
     * Lance l'interface Front Office (Catalogue client)
     */
    private void launchFrontOffice(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/ActiviteFrontOffice.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 1400, 900);
        
        stage.setTitle("RE7LA - Découvrez des expériences inoubliables");
        stage.setScene(scene);
        stage.setMaximized(true);
        
        stage.show();
    }

    /**
     * Lance un menu de sélection pour choisir entre Back Office et Front Office
     */
    private void launchSelectionMenu(Stage stage) throws Exception {
        // TODO: Créer une interface de sélection
        // Pour l'instant, lance directement le Front Office
        launchFrontOffice(stage);
    }

    /**
     * Lance l'interface de gestion des plannings (Back Office)
     */
    private void launchPlanningBackOffice(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/PlanningBackOffice.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 1200, 800);
        
        stage.setTitle("RE7LA - Gestion des Plannings");
        stage.setScene(scene);
        stage.setMaximized(true);
        
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


/**
 * CLASSES ALTERNATIVES POUR NAVIGATION
 */

// Si vous voulez créer un système de navigation entre les interfaces:

class NavigationHelper {
    
    /**
     * Ouvre une nouvelle fenêtre pour le Back Office Activités
     */
    public static void openActiviteBackOffice() {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationHelper.class.getResource("/views/backoffice/ActiviteBackOffice.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Gestion des Activités");
            stage.setScene(new Scene(root, 1200, 800));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Ouvre une nouvelle fenêtre pour le Back Office Plannings
     */
    public static void openPlanningBackOffice() {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationHelper.class.getResource("/views/backoffice/PlanningBackOffice.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Gestion des Plannings");
            stage.setScene(new Scene(root, 1200, 800));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Ouvre le catalogue Front Office
     */
    public static void openFrontOffice() {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationHelper.class.getResource("/views/frontoffice/ActiviteFrontOffice.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("RE7LA - Catalogue d'Activités");
            stage.setScene(new Scene(root, 1400, 900));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


/**
 * EXEMPLE D'UTILISATION AVEC UN MENU PRINCIPAL
 */
class MenuPrincipalController {
    
    // Dans un bouton FXML:
    // <Button text="Back Office" onAction="#openBackOffice"/>
    
    public void openBackOffice() {
        NavigationHelper.openActiviteBackOffice();
    }
    
    public void openPlannings() {
        NavigationHelper.openPlanningBackOffice();
    }
    
    public void openCatalogue() {
        NavigationHelper.openFrontOffice();
    }
}

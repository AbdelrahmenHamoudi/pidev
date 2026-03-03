package org.example.Controllers.activite;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.Entites.activite.ReservationDetail;  // ✅ Changé : import de la classe externe
import org.example.Entites.user.User;
import org.example.Services.activite.ReservationServiceImpl;
import org.example.Utils.UserSession;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Vue USER — "Mes Réservations"
 * Affiche uniquement les réservations de l'utilisateur connecté (via JWT)
 */
public class MesReservationsController implements Initializable {

    @FXML private Label lblTitre;
    @FXML private Label lblConf;
    @FXML private Label lblAnn;
    @FXML private ComboBox<String> filtreStatut;
    @FXML private VBox container;
    @FXML private Label lblVide;

    private ReservationServiceImpl reservationService;
    private List<ReservationDetail> liste;  // ✅ Utilisation de la classe externe

    // ✅ Utilisateur connecté via JWT
    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // ✅ Vérifier l'authentification JWT
        if (!checkUserAuth()) {
            return;
        }

        reservationService = new ReservationServiceImpl();

        // Afficher le nom de l'utilisateur connecté
        lblTitre.setText("Mes réservations — " + currentUser.getPrenom() + " " + currentUser.getNom());

        filtreStatut.getItems().addAll("Toutes", "CONFIRMEE", "ANNULEE");
        filtreStatut.setValue("Toutes");
        filtreStatut.setOnAction(e -> afficher());

        charger();
    }

    /**
     * ✅ Vérifie si l'utilisateur est authentifié via JWT
     */
    private boolean checkUserAuth() {
        if (!UserSession.getInstance().isTokenValid()) {
            showAlert("Session expirée", "Votre session a expiré. Veuillez vous reconnecter.");
            redirectToLogin();
            return false;
        }

        currentUser = UserSession.getInstance().getCurrentUser();

        if (currentUser == null) {
            showAlert("Erreur", "Aucun utilisateur connecté.");
            redirectToLogin();
            return false;
        }

        // ✅ Vérifier si l'utilisateur est banni
        if (currentUser.getStatus() != null &&
                "Banned".equalsIgnoreCase(currentUser.getStatus().name())) {
            showAlert("Compte suspendu",
                    "Votre compte est suspendu. Veuillez contacter l'administrateur.");
            redirectToLogin();
            return false;
        }

        System.out.println("✅ Mes réservations - Utilisateur: " +
                currentUser.getPrenom() + " " + currentUser.getNom());
        return true;
    }

    /**
     * ✅ Redirige vers la page de login
     */
    private void redirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) (container != null ?
                    container.getScene().getWindow() :
                    lblTitre.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de rediriger vers la page de connexion.");
        }
    }

    private void charger() {
        try {
            // ✅ Utiliser la nouvelle méthode qui retourne des ReservationDetail
            liste = reservationService.getMesReservations();

            long conf = liste.stream().filter(r -> "CONFIRMEE".equals(r.statut)).count();
            long ann  = liste.stream().filter(r -> "ANNULEE".equals(r.statut)).count();
            lblConf.setText(String.valueOf(conf));
            lblAnn.setText(String.valueOf(ann));
            afficher();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger vos réservations.");
        }
    }

    private void afficher() {
        String f = filtreStatut.getValue();
        List<ReservationDetail> filtre = "Toutes".equals(f) ? liste
                : liste.stream().filter(r -> f.equals(r.statut)).collect(Collectors.toList());

        container.getChildren().clear();
        lblVide.setVisible(filtre.isEmpty());
        lblVide.setManaged(filtre.isEmpty());

        if (filtre.isEmpty() && !liste.isEmpty()) {
            Label info = new Label("Aucune réservation avec le statut \"" + f + "\"");
            info.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic; -fx-padding: 20;");
            container.getChildren().add(info);
        } else {
            for (ReservationDetail r : filtre) {
                container.getChildren().add(buildCard(r));
            }
        }
    }

    private VBox buildCard(ReservationDetail r) {
        boolean conf = "CONFIRMEE".equals(r.statut);
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                "-fx-border-width: 2; -fx-border-radius: 12;" +
                (conf ? "-fx-border-color: #34D399;" : "-fx-border-color: #F87171;"));

        // Titre + badge
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label nom = new Label(r.nomActivite);
        nom.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        HBox.setHgrow(nom, Priority.ALWAYS);
        Label badge = new Label(conf ? "✔ Confirmée" : "✖ Annulée");
        badge.setStyle(conf
                ? "-fx-background-color:#D1FAE5;-fx-text-fill:#065F46;-fx-font-weight:bold;-fx-padding:4 12;-fx-background-radius:20;-fx-font-size:12px;"
                : "-fx-background-color:#FEE2E2;-fx-text-fill:#991B1B;-fx-font-weight:bold;-fx-padding:4 12;-fx-background-radius:20;-fx-font-size:12px;");
        header.getChildren().addAll(nom, badge);

        // Infos
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        Label infos = new Label(String.format(
                "📍 %s   |   📅 %s   |   🕐 %s – %s   |   💶 %.0f DT/pers.",
                r.lieu,
                r.date_planning.format(df),
                r.heure_debut.format(tf),
                r.heure_fin.format(tf),
                r.prix));
        infos.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        infos.setWrapText(true);

        // Footer avec nom d'utilisateur et référence
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label userLabel = new Label("👤 " + r.getUserName());
        userLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        Label ref = new Label("Réf. #" + r.id_reservation);
        ref.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        footer.getChildren().addAll(userLabel, ref, spacer);

        if (conf) {
            Button btnAnn = new Button("Annuler");
            btnAnn.setStyle("-fx-background-color:#FEE2E2;-fx-text-fill:#DC2626;-fx-font-weight:bold;" +
                    "-fx-padding:6 16;-fx-background-radius:8;-fx-cursor:hand;");
            btnAnn.setOnAction(e -> annuler(r));
            footer.getChildren().add(btnAnn);
        }

        card.getChildren().addAll(header, new Separator(), infos, footer);
        return card;
    }

    private void annuler(ReservationDetail r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Annulation");
        confirm.setHeaderText("Annuler « " + r.nomActivite + " » ?");
        confirm.setContentText("La place sera libérée. Cette action est irréversible.");
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try {
                    // ✅ Utiliser l'ID de l'utilisateur connecté
                    boolean ok = reservationService.annulerReservation(r.id_reservation, currentUser.getId());
                    Alert res = new Alert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                    res.setHeaderText(null);
                    res.setContentText(ok ? "✅ Réservation annulée avec succès." : "❌ Impossible d'annuler la réservation.");
                    res.showAndWait();
                    if (ok) charger();
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Erreur", "Une erreur est survenue lors de l'annulation.");
                }
            }
        });
    }

    @FXML
    private void refresh() {
        charger();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
package org.example.Controllers.hebergement.back;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.example.Entites.hebergement.Hebergement;
import org.example.Entites.hebergement.Reservation;
import org.example.Services.hebergement.AnalyseConcurrentielleService;
import org.example.Services.hebergement.AnalyseConcurrentielleService.AnalyseConcurrentielle;
import org.example.Services.hebergement.AnalyseConcurrentielleService.DonneesMarche;
import org.example.Services.hebergement.HebergementCRUD;
import org.example.Services.hebergement.ReservationCRUD;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class AnalyseConcurrentielleController implements Initializable {

    // ── Sélection hébergement ──
    @FXML private ComboBox<Hebergement> comboHebergement;
    @FXML private Button btnAnalyser;
    @FXML private ProgressIndicator progressAnalyse;
    @FXML private Label lblStatut;

    // ── Données admin (panneau gauche) ──
    @FXML private Label lblVille;
    @FXML private Label lblType;
    @FXML private Label lblPrixAdmin;
    @FXML private Label lblOccupationAdmin;
    @FXML private Label lblSaison;

    // ── Données marché (panneau centre) ──
    @FXML private Label lblPrixMarche;
    @FXML private Label lblOccupationMarche;
    @FXML private Label lblRevenuMarche;
    @FXML private Label lblAttractiv;

    // ── Badges comparatifs ──
    @FXML private Label lblBadgePrix;
    @FXML private Label lblBadgeOccupation;
    @FXML private Label lblEcartPrix;
    @FXML private Label lblEcartOccupation;

    // ── Analyse IA ──
    @FXML private TextArea txtAnalyseIA;
    @FXML private VBox panneauResultat;

    private final HebergementCRUD hebergementCRUD     = new HebergementCRUD();
    private final ReservationCRUD reservationCRUD     = new ReservationCRUD();
    private final AnalyseConcurrentielleService service = new AnalyseConcurrentielleService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        panneauResultat.setVisible(false);
        progressAnalyse.setVisible(false);
        chargerHebergements();
    }

    private void chargerHebergements() {
        try {
            List<Hebergement> liste = hebergementCRUD.afficherh();
            comboHebergement.getItems().addAll(liste);

            // Afficher le titre dans la ComboBox
            comboHebergement.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Hebergement h, boolean empty) {
                    super.updateItem(h, empty);
                    setText(empty || h == null ? null :
                            h.getTitre() + " – " + h.getType_hebergement()
                                    + " (" + String.format("%.0f", h.getPrixParNuit()) + " DT)");
                }
            });
            comboHebergement.setButtonCell(comboHebergement.getCellFactory().call(null));

        } catch (SQLException e) {
            lblStatut.setText("❌ Erreur chargement : " + e.getMessage());
        }
    }

    @FXML
    private void lancerAnalyse() {
        Hebergement h = comboHebergement.getValue();
        if (h == null) {
            lblStatut.setText("⚠️ Sélectionnez un hébergement d'abord.");
            return;
        }

        btnAnalyser.setDisable(true);
        progressAnalyse.setVisible(true);
        panneauResultat.setVisible(false);
        lblStatut.setText("🔄 Analyse concurrentielle en cours...");

        new Thread(() -> {
            try {
                // Calculer taux d'occupation réel depuis BDD
                double tauxOccupation = calculerTauxOccupation(h);

                AnalyseConcurrentielle analyse = service.analyser(
                        h.getTitre(),
                        h.getType_hebergement(),
                        h.getPrixParNuit(),
                        tauxOccupation,
                        h.getCapacite()
                );

                Platform.runLater(() -> afficherResultats(analyse));

            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatut.setText("❌ Erreur : " + e.getMessage());
                    btnAnalyser.setDisable(false);
                    progressAnalyse.setVisible(false);
                });
            }
        }).start();
    }

    private void afficherResultats(AnalyseConcurrentielle a) {
        // ── Données admin ──
        lblVille.setText(a.ville);
        lblType.setText(a.typeHebergement);
        lblPrixAdmin.setText(String.format("%.0f DT / nuit", a.prixAdmin));
        lblOccupationAdmin.setText(String.format("%.1f%%", a.tauxOccupationAdmin));
        lblSaison.setText(a.saisonActuelle);

        // ── Données marché ──
        DonneesMarche m = a.marche;
        lblPrixMarche.setText(String.format("%.0f DT / nuit", m.prixMoyen));
        lblOccupationMarche.setText(String.format("%.1f%%", m.tauxOccupationMoyen));
        lblRevenuMarche.setText(String.format("%.0f DT / mois", m.revenuMensuelMoyen));
        lblAttractiv.setText(m.attractivite);

        // ── Badge prix ──
        String badgePrix = a.statutPrix();
        lblBadgePrix.setText(badgePrix);
        lblBadgePrix.setStyle(getBadgeStyle(badgePrix));

        String signePrix = a.ecartPrix < 0 ? "▼" : "▲";
        lblEcartPrix.setText(String.format("%s %.1f%% %s le marché",
                signePrix, Math.abs(a.ecartPrix),
                a.ecartPrix < 0 ? "sous" : "au-dessus de"));
        lblEcartPrix.setStyle(a.ecartPrix <= 5 && a.ecartPrix >= -10
                ? "-fx-text-fill: #1ABC9C;" : "-fx-text-fill: #E53E3E;");

        // ── Badge occupation ──
        String badgeOcc = a.statutOccupation();
        lblBadgeOccupation.setText(badgeOcc);
        lblBadgeOccupation.setStyle(getBadgeStyle(badgeOcc));

        String signeOcc = a.ecartOccupation >= 0 ? "▲" : "▼";
        lblEcartOccupation.setText(String.format("%s %+.1f pts vs marché",
                signeOcc, a.ecartOccupation));
        lblEcartOccupation.setStyle(a.ecartOccupation >= -5
                ? "-fx-text-fill: #1ABC9C;" : "-fx-text-fill: #E53E3E;");

        // ── Analyse IA ──
        txtAnalyseIA.setText(a.analyseIA);

        // ── Afficher le panneau ──
        panneauResultat.setVisible(true);
        progressAnalyse.setVisible(false);
        btnAnalyser.setDisable(false);
        lblStatut.setText("✅ Analyse terminée – " + a.ville + " · " + a.typeHebergement);
    }

    private String getBadgeStyle(String statut) {
        return switch (statut) {
            case "COMPÉTITIF", "BON" ->
                    "-fx-background-color: #1ABC9C; -fx-text-fill: white; " +
                            "-fx-background-radius: 20; -fx-padding: 4 12; -fx-font-weight: bold;";
            case "MOYEN", "TROP BAS" ->
                    "-fx-background-color: #F39C12; -fx-text-fill: white; " +
                            "-fx-background-radius: 20; -fx-padding: 4 12; -fx-font-weight: bold;";
            default ->
                    "-fx-background-color: #E53E3E; -fx-text-fill: white; " +
                            "-fx-background-radius: 20; -fx-padding: 4 12; -fx-font-weight: bold;";
        };
    }

    /**
     * Calcule le taux d'occupation réel depuis les réservations en BDD
     */
    private double calculerTauxOccupation(Hebergement h) {
        try {
            List<Reservation> reservations = reservationCRUD
                    .getReservationsByHebergement(h.getId_hebergement());
            // Taux simplifié : nb réservations / 30 jours * 100
            int nbRes = reservations.size();
            return Math.min(100, (nbRes / 30.0) * 100);
        } catch (SQLException e) {
            return 65.0; // valeur par défaut
        }
    }
}

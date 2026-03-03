package org.example.Controllers.activite;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.example.Entites.activite.ReservationDetail;
import org.example.Services.activite.ReservationServiceImpl;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Vue ADMIN — toutes les réservations de tous les utilisateurs.
 * Correspond à ReservationsAdmin.fxml
 */
public class ReservationsAdminController implements Initializable {

    @FXML private Label lblTotal;
    @FXML private Label lblConfirmees;
    @FXML private Label lblAnnulees;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filtreStatut;

    @FXML private TableView<ReservationDetail> table;
    @FXML private TableColumn<ReservationDetail, Integer> colId;
    @FXML private TableColumn<ReservationDetail, String>  colUser;
    @FXML private TableColumn<ReservationDetail, String>  colActivite;
    @FXML private TableColumn<ReservationDetail, String>  colLieu;
    @FXML private TableColumn<ReservationDetail, String>  colDate;
    @FXML private TableColumn<ReservationDetail, String>  colHoraire;
    @FXML private TableColumn<ReservationDetail, String>  colStatut;
    @FXML private TableColumn<ReservationDetail, Void>    colActions;

    @FXML private Label lblPagination;

    private ReservationServiceImpl reservationService;
    private ObservableList<ReservationDetail> allRows = FXCollections.observableArrayList();
    private ObservableList<ReservationDetail> filteredRows = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        reservationService = new ReservationServiceImpl();

        filtreStatut.getItems().addAll("Tous", "CONFIRMEE", "ANNULEE");
        filtreStatut.setValue("Tous");
        filtreStatut.setOnAction(e -> appliquerFiltres());
        searchField.textProperty().addListener((obs, o, n) -> appliquerFiltres());

        setupColonnes();
        setupColonneActions();
        table.setItems(filteredRows);

        charger();
    }

    private void setupColonnes() {
        colId.setCellValueFactory(c ->
                new javafx.beans.property.SimpleIntegerProperty(c.getValue().id_reservation).asObject());

        colActivite.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().nomActivite));

        colLieu.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().lieu));

        colUser.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getUserName()));

        colDate.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().date_planning.format(DATE_FMT)));

        colHoraire.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().heure_debut.format(TIME_FMT) + " – " +
                                c.getValue().heure_fin.format(TIME_FMT)));

        colStatut.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().statut));

        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(s);
                if ("CONFIRMEE".equals(s)) {
                    setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void setupColonneActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnAnnuler = new Button("Annuler");
            private final Button btnSupprimer = new Button("Supprimer");

            {
                btnAnnuler.setStyle("-fx-background-color:#FEF3C7;-fx-text-fill:#B45309;-fx-font-weight:bold;" +
                        "-fx-padding:5 10;-fx-background-radius:6;-fx-cursor:hand;");
                btnSupprimer.setStyle("-fx-background-color:#FEE2E2;-fx-text-fill:#DC2626;-fx-font-weight:bold;" +
                        "-fx-padding:5 10;-fx-background-radius:6;-fx-cursor:hand;");

                btnAnnuler.setOnAction(e -> {
                    ReservationDetail r = getTableView().getItems().get(getIndex());
                    handleAnnuler(r);
                });

                btnSupprimer.setOnAction(e -> {
                    ReservationDetail r = getTableView().getItems().get(getIndex());
                    handleSupprimer(r);
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                ReservationDetail r = getTableView().getItems().get(getIndex());
                HBox box = new HBox(6);
                box.setAlignment(Pos.CENTER);
                if ("CONFIRMEE".equals(r.statut)) {
                    box.getChildren().add(btnAnnuler);
                }
                box.getChildren().add(btnSupprimer);
                setGraphic(box);
            }
        });
    }

    private void charger() {
        allRows.clear();
        // ✅ Utiliser getAllReservationsDetails() au lieu de getAllReservations()
        allRows.addAll(reservationService.getAllReservationsDetails());
        appliquerFiltres();
        updateStats();
    }

    private void appliquerFiltres() {
        String search = searchField.getText().toLowerCase().trim();
        String statut = filtreStatut.getValue();

        List<ReservationDetail> result = allRows.stream()
                .filter(r -> "Tous".equals(statut) || statut.equals(r.statut))
                .filter(r -> search.isEmpty()
                        || r.nomActivite.toLowerCase().contains(search)
                        || r.getUserName().toLowerCase().contains(search)
                        || r.lieu.toLowerCase().contains(search))
                .collect(Collectors.toList());

        filteredRows.clear();
        filteredRows.addAll(result);
        lblPagination.setText(filteredRows.size() + " réservation(s)");
    }

    private void updateStats() {
        lblTotal.setText(String.valueOf(allRows.size()));
        lblConfirmees.setText(String.valueOf(
                allRows.stream().filter(r -> "CONFIRMEE".equals(r.statut)).count()));
        lblAnnulees.setText(String.valueOf(
                allRows.stream().filter(r -> "ANNULEE".equals(r.statut)).count()));
    }

    private void handleAnnuler(ReservationDetail r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Annuler la réservation");
        confirm.setHeaderText("Annuler la réservation de " + r.getUserName() + " ?");
        confirm.setContentText("La place sera restituée au planning « " + r.nomActivite + " ».");

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                // ✅ Utiliser annulerReservation avec l'ID de l'utilisateur
                boolean ok = reservationService.annulerReservation(r.id_reservation, r.id_user);
                showAlert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                        ok ? "Annulée" : "Erreur",
                        ok ? "Réservation annulée avec succès." : "Impossible d'annuler.");
                if (ok) charger();
            }
        });
    }

    private void handleSupprimer(ReservationDetail r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Supprimer définitivement cette réservation ?");
        confirm.setContentText("Réf. #" + r.id_reservation + " — " + r.nomActivite);

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                boolean ok = reservationService.deleteReservation(r.id_reservation);
                showAlert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                        ok ? "Supprimée" : "Erreur",
                        ok ? "Réservation supprimée." : "Impossible de supprimer.");
                if (ok) charger();
            }
        });
    }

    @FXML
    private void refresh() {
        charger();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
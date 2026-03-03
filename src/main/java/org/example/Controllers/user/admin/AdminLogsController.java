package org.example.Controllers.user.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.chart.PieChart;
import javafx.stage.Stage;
import javafx.application.Platform;
import org.example.Entites.user.AdminActionLog;
import org.example.Entites.user.ConnexionLog;
import org.example.Entites.user.User;
import org.example.Services.user.AdminActionLogService;
import org.example.Services.user.ConnexionLogService;
import org.example.Services.user.UserCRUD;
import org.example.Utils.UserSession;
import org.example.Services.user.APIservices.JWTService;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class AdminLogsController implements Initializable {

    @FXML private Label todayConnexionsLabel;
    @FXML private Label successRateLabel;
    @FXML private Label actionsCountLabel;
    @FXML private Label totalLogsLabel;

    @FXML private ComboBox<String> userFilterCombo;
    @FXML private DatePicker dateFilterPicker;
    @FXML private ComboBox<String> actionTypeCombo;
    @FXML private ComboBox<String> adminFilterCombo;

    @FXML private TableView<ConnexionLog> connexionTable;
    @FXML private TableColumn<ConnexionLog, Integer> colLogId;
    @FXML private TableColumn<ConnexionLog, String> colLogUser;
    @FXML private TableColumn<ConnexionLog, String> colLogTime;
    @FXML private TableColumn<ConnexionLog, String> colLogIp;
    @FXML private TableColumn<ConnexionLog, String> colLogDevice;
    @FXML private TableColumn<ConnexionLog, String> colLogStatus;
    @FXML private TableColumn<ConnexionLog, String> colLogDuration;

    // Nouvelles colonnes pour la localisation
    @FXML private TableColumn<ConnexionLog, String> colLogCountry;
    @FXML private TableColumn<ConnexionLog, String> colLogCity;

    @FXML private TableView<AdminActionLog> actionTable;
    @FXML private TableColumn<AdminActionLog, Integer> colActionId;
    @FXML private TableColumn<AdminActionLog, String> colActionAdmin;
    @FXML private TableColumn<AdminActionLog, String> colActionType;
    @FXML private TableColumn<AdminActionLog, String> colActionTarget;
    @FXML private TableColumn<AdminActionLog, String> colActionDesc;
    @FXML private TableColumn<AdminActionLog, String> colActionTime;
    @FXML private TableColumn<AdminActionLog, String> colActionIp;

    // Nouveaux composants pour les statistiques de localisation
    @FXML private PieChart countryPieChart;
    @FXML private ListView<String> cityListView;
    @FXML private Label topCountryLabel;
    @FXML private Label uniqueCountriesLabel;

    private final ConnexionLogService logService = new ConnexionLogService();
    private final AdminActionLogService actionService = new AdminActionLogService();
    private final UserCRUD userCRUD = new UserCRUD();

    private ObservableList<ConnexionLog> connexionLogs = FXCollections.observableArrayList();
    private ObservableList<AdminActionLog> actionLogs = FXCollections.observableArrayList();

    private boolean checkAdminAuth() {
        if (!UserSession.getInstance().isTokenValid()) {
            Platform.runLater(() -> {
                showAlert("Session expirée", "Votre session a expiré. Veuillez vous reconnecter.", Alert.AlertType.WARNING);
                redirectToLogin();
            });
            return false;
        }

        String token = UserSession.getInstance().getToken();
        String role = JWTService.extractRole(token);

        if (!"admin".equals(role)) {
            Platform.runLater(() -> {
                showAlert("Accès refusé", "Vous n'avez pas les droits d'administrateur.", Alert.AlertType.ERROR);
                redirectToLogin();
            });
            return false;
        }

        Integer adminId = JWTService.extractUserId(token);
        System.out.println("👑 Admin logs (ID: " + adminId + ") - Page chargée");

        return true;
    }

    private void redirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) todayConnexionsLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!checkAdminAuth()) {
            return;
        }

        setupConnexionTable();
        setupActionTable();
        loadStats();
        loadConnexionLogs();
        loadActionLogs();
        loadLocationStats();
        setupFilters();

        String token = UserSession.getInstance().getToken();
        System.out.println("🔐 Token JWT actif pour logs admin");
        System.out.println("👤 Admin: " + JWTService.extractEmail(token));
    }

    private void setupConnexionTable() {
        colLogId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colLogUser.setCellValueFactory(cellData -> {
            try {
                User user = userCRUD.getUserById(cellData.getValue().getUserId());
                return new SimpleStringProperty(user != null ? user.getPrenom() + " " + user.getNom() : "Utilisateur " + cellData.getValue().getUserId());
            } catch (Exception e) {
                return new SimpleStringProperty("ID: " + cellData.getValue().getUserId());
            }
        });

        colLogTime.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getLoginTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
        );

        colLogIp.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        colLogDevice.setCellValueFactory(new PropertyValueFactory<>("deviceInfo"));

        colLogStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isSuccess() ? "✅ Succès" : "❌ Échec")
        );
        colLogStatus.setCellFactory(col -> new TableCell<ConnexionLog, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    if (item.contains("Succès")) {
                        setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
                    }
                }
            }
        });

        colLogDuration.setCellValueFactory(cellData -> {
            ConnexionLog log = cellData.getValue();
            if (log.getLogoutTime() != null) {
                Duration duration = Duration.between(log.getLoginTime(), log.getLogoutTime());
                long minutes = duration.toMinutes();
                long seconds = duration.getSeconds() % 60;
                return new SimpleStringProperty(String.format("%d min %d sec", minutes, seconds));
            } else {
                return new SimpleStringProperty("En cours");
            }
        });

        // Nouvelles colonnes de localisation
        colLogCountry.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCountry() != null ?
                        cellData.getValue().getCountry() : "-")
        );

        colLogCity.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCity() != null ?
                        cellData.getValue().getCity() : "-")
        );
    }

    private void setupActionTable() {
        colActionId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colActionAdmin.setCellValueFactory(new PropertyValueFactory<>("adminName"));
        colActionType.setCellValueFactory(new PropertyValueFactory<>("actionType"));
        colActionTarget.setCellValueFactory(new PropertyValueFactory<>("targetType"));
        colActionDesc.setCellValueFactory(new PropertyValueFactory<>("details"));
        colActionTime.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
        );
        colActionIp.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
    }

    private void loadStats() {
        List<Object[]> dailyStats = logService.getDailyStats();
        int total = 0;
        int success = 0;
        for (Object[] stat : dailyStats) {
            total += (int) stat[1];
            success += (int) stat[2];
        }

        todayConnexionsLabel.setText(String.valueOf(logService.getTodayConnexions()));
        double rate = total > 0 ? (success * 100.0 / total) : 0;
        successRateLabel.setText(String.format("%.1f%%", rate));

        actionsCountLabel.setText(String.valueOf(actionService.getAllLogs(1000).size()));
    }

    private void loadConnexionLogs() {
        connexionLogs.setAll(logService.getAllLogs(500));
        connexionTable.setItems(connexionLogs);
        totalLogsLabel.setText(connexionLogs.size() + " connexions");
    }

    private void loadActionLogs() {
        actionLogs.setAll(actionService.getAllLogs(500));
        actionTable.setItems(actionLogs);
    }

    private void loadLocationStats() {
        // Statistiques par pays
        List<Object[]> countryStats = logService.getCountryStats();
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        int totalCountries = 0;
        String topCountry = "";
        int topCountryCount = 0;

        for (Object[] stat : countryStats) {
            String country = (String) stat[0];
            int count = (int) stat[1];
            pieChartData.add(new PieChart.Data(country + " (" + count + ")", count));

            totalCountries++;
            if (count > topCountryCount) {
                topCountryCount = count;
                topCountry = country;
            }
        }

        countryPieChart.setData(pieChartData);
        countryPieChart.setTitle("Répartition des connexions par pays");
        countryPieChart.setLabelsVisible(true);
        countryPieChart.setLegendVisible(true);

        topCountryLabel.setText(topCountry + " (" + topCountryCount + " connexions)");
        uniqueCountriesLabel.setText(String.valueOf(totalCountries));

        // Statistiques par ville
        List<Object[]> cityStats = logService.getCityStats();
        ObservableList<String> cityItems = FXCollections.observableArrayList();

        for (Object[] stat : cityStats) {
            String city = (String) stat[0];
            String country = (String) stat[1];
            int count = (int) stat[2];
            cityItems.add("📍 " + city + ", " + country + " : " + count + " connexion(s)");
        }

        cityListView.setItems(cityItems);

        // Si pas de données
        if (cityItems.isEmpty()) {
            cityListView.setItems(FXCollections.observableArrayList("Aucune donnée de localisation disponible"));
        }
        if (pieChartData.isEmpty()) {
            countryPieChart.setData(FXCollections.observableArrayList(new PieChart.Data("Aucune donnée", 1)));
        }
    }

    private void setupFilters() {
        // À implémenter si nécessaire
    }

    @FXML
    private void resetConnexionFilters() {
        if (!checkAdminAuth()) return;
        userFilterCombo.setValue(null);
        dateFilterPicker.setValue(null);
        connexionTable.setItems(connexionLogs);
    }

    @FXML
    private void resetActionFilters() {
        if (!checkAdminAuth()) return;
        actionTypeCombo.setValue(null);
        adminFilterCombo.setValue(null);
        actionTable.setItems(actionLogs);
    }

    @FXML
    private void handleBack(ActionEvent event) {
        if (!checkAdminAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/back/users.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Admin - Dashboard");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void refreshStats() {
        if (!checkAdminAuth()) return;

        loadStats();
        loadConnexionLogs();
        loadActionLogs();
        loadLocationStats();

        showAlert("Succès", "Statistiques actualisées", Alert.AlertType.INFORMATION);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
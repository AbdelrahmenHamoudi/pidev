package org.example.Controllers.user.admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;
import org.example.Entites.user.AdminNotification;
import org.example.Services.user.AdminNotificationService;
import org.example.Utils.UserSession;
import org.example.Services.user.APIservices.JWTService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class AdminNotificationsController implements Initializable {

    @FXML private Label unreadCountLabel;
    @FXML private ListView<AdminNotification> notificationsList;

    private final AdminNotificationService notifService = new AdminNotificationService();
    private ObservableList<AdminNotification> notifications = FXCollections.observableArrayList();

    private Runnable onNotificationsChanged;

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
        System.out.println("👑 Admin notifications (ID: " + adminId + ") - Page chargée");

        return true;
    }

    private void redirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) unreadCountLabel.getScene().getWindow();
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

        setupNotificationsList();
        loadNotifications();
        updateUnreadCount();

        String token = UserSession.getInstance().getToken();
        System.out.println("🔐 Token JWT actif pour notifications");
    }

    public void setOnNotificationsChanged(Runnable callback) {
        this.onNotificationsChanged = callback;
    }

    private void setupNotificationsList() {
        notificationsList.setCellFactory(lv -> new ListCell<AdminNotification>() {
            @Override
            protected void updateItem(AdminNotification item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox container = new HBox(15);
                    container.setStyle("-fx-padding: 15; -fx-alignment: center-left;");

                    Label iconLabel = new Label(getIconForType(item.getType()));
                    iconLabel.setStyle("-fx-font-size: 24px; -fx-min-width: 40;");

                    VBox contentBox = new VBox(5);

                    HBox headerBox = new HBox(10);
                    Label titleLabel = new Label(item.getTitle());
                    titleLabel.setStyle(item.isRead() ?
                            "-fx-font-size: 14px; -fx-font-weight: normal;" :
                            "-fx-font-size: 14px; -fx-font-weight: bold;");

                    Label priorityLabel = new Label(item.getPriority());
                    priorityLabel.setStyle(getPriorityStyle(item.getPriority()));
                    priorityLabel.setStyle(priorityLabel.getStyle() + "-fx-font-size: 11px; -fx-background-radius: 10; -fx-padding: 2 8;");

                    headerBox.getChildren().addAll(titleLabel, priorityLabel);

                    Label messageLabel = new Label(item.getMessage());
                    messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2C3E50;");
                    messageLabel.setWrapText(true);
                    messageLabel.setMaxWidth(600);

                    Label dateLabel = new Label(
                            item.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    );
                    dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #7F8C8D;");

                    contentBox.getChildren().addAll(headerBox, messageLabel, dateLabel);

                    VBox actionsBox = new VBox(10);
                    actionsBox.setStyle("-fx-alignment: center;");

                    if (!item.isRead()) {
                        Button markReadBtn = new Button("✓ Marquer comme lu");
                        markReadBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 15; -fx-padding: 5 10;");
                        markReadBtn.setOnAction(e -> {
                            if (checkAdminAuth()) markAsRead(item);
                        });
                        actionsBox.getChildren().add(markReadBtn);
                    }

                    if (item.getActionLink() != null) {
                        Button actionBtn = new Button(item.getActionText() != null ? item.getActionText() : "Voir");
                        actionBtn.setStyle("-fx-background-color: #F39C12; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 15; -fx-padding: 5 10;");
                        actionBtn.setOnAction(e -> {
                            if (checkAdminAuth()) navigateToAction(item);
                        });
                        actionsBox.getChildren().add(actionBtn);
                    }

                    Button deleteBtn = new Button("🗑️");
                    deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E74C3C; -fx-font-size: 14px; -fx-cursor: hand;");
                    deleteBtn.setOnAction(e -> {
                        if (checkAdminAuth()) deleteNotification(item);
                    });
                    actionsBox.getChildren().add(deleteBtn);

                    container.getChildren().addAll(iconLabel, contentBox, actionsBox);

                    if (!item.isRead()) {
                        container.setStyle("-fx-background-color: #EBF5FF; -fx-background-radius: 10; -fx-padding: 15;");
                    } else {
                        container.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15; -fx-border-color: #ECF0F1; -fx-border-width: 1; -fx-border-radius: 10;");
                    }

                    setGraphic(container);
                }
            }
        });
    }

    private void loadNotifications() {
        notifications.setAll(notifService.getAllNotifications(100));
        notificationsList.setItems(notifications);
        updateUnreadCount();

        if (onNotificationsChanged != null) {
            onNotificationsChanged.run();
        }
    }

    private void updateUnreadCount() {
        int unread = (int) notifications.stream().filter(n -> !n.isRead()).count();
        unreadCountLabel.setText(unread + " notification(s) non lue(s)");
    }

    private String getIconForType(String type) {
        switch(type) {
            case "SUCCESS": return "✅";
            case "WARNING": return "⚠️";
            case "ERROR": return "❌";
            default: return "ℹ️";
        }
    }

    private String getPriorityStyle(String priority) {
        switch(priority) {
            case "URGENT":
                return "-fx-background-color: #E74C3C; -fx-text-fill: white;";
            case "HIGH":
                return "-fx-background-color: #F39C12; -fx-text-fill: white;";
            case "MEDIUM":
                return "-fx-background-color: #3498DB; -fx-text-fill: white;";
            default:
                return "-fx-background-color: #95A5A6; -fx-text-fill: white;";
        }
    }

    @FXML
    private void markAllAsRead() {
        if (!checkAdminAuth()) return;
        notifService.markAllAsRead();
        loadNotifications();
        showAlert("Succès", "Toutes les notifications ont été marquées comme lues.", Alert.AlertType.INFORMATION);
    }

    private void markAsRead(AdminNotification notif) {
        notifService.markAsRead(notif.getId());
        loadNotifications();
    }

    private void deleteNotification(AdminNotification notif) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la notification");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette notification ?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                notifService.deleteNotification(notif.getId());
                loadNotifications();
            }
        });
    }

    private void navigateToAction(AdminNotification notif) {
        try {
            if (notif.getActionLink() != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(notif.getActionLink()));
                Parent root = loader.load();
                Stage stage = (Stage) notificationsList.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.centerOnScreen();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de naviguer vers la page demandée.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
       /* if (!checkAdminAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/back/users.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Admin - Dashboard");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
package org.example.Controllers.user.admin.user;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.Entites.user.Role;
import org.example.Entites.user.Status;
import org.example.Entites.user.User;
import org.example.Services.user.UserCRUD;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminUsersController implements Initializable {

    @FXML private VBox sidebar;
    @FXML private Button btnGestionUtilisateurs, btnComptesBancaires, btnTransactions,
            btnCredits, btnCashback, btnParametres, btnDeconnexion;

    @FXML private StackPane mainContentStack;
    @FXML private VBox viewEmpty;

    @FXML private Label totalUsersLabel;
    @FXML private Label adminCountLabel;
    @FXML private Label userCountLabel;
    @FXML private Label bannedCountLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterRoleCombo;
    @FXML private ComboBox<String> filterStatusCombo;
    @FXML private Button btnResetFilters;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colAvatar;
    @FXML private TableColumn<User, String> colNomComplet;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colTelephone;
    @FXML private TableColumn<User, String> colDateNaiss;
    @FXML private TableColumn<User, Role> colRole;
    @FXML private TableColumn<User, Status> colStatus;
    @FXML private TableColumn<User, Void> colActions;

    @FXML private TextField txtId;
    @FXML private TextField txtPrenom;
    @FXML private TextField txtNom;
    @FXML private DatePicker dpDateNaiss;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelephone;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtImage;
    @FXML private ComboBox<Role> comboRole;
    @FXML private ComboBox<Status> comboStatus;
    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnAnnuler;
    @FXML private Button btnParcourirImage;

    @FXML private Label errorPrenom;
    @FXML private Label errorNom;
    @FXML private Label errorDate;
    @FXML private Label errorEmail;
    @FXML private Label errorTelephone;
    @FXML private Label errorPassword;
    @FXML private Label errorRole;
    @FXML private Label errorStatus;
    @FXML private Label errorImage;

    @FXML private VBox imagePreviewBox;
    @FXML private ImageView previewImageView;
    @FXML private Label imageFileNameLabel;

    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private FilteredList<User> filteredData;
    private User selectedUser = null;
    private final UserCRUD userCRUD = new UserCRUD();
    private File selectedImageFile;
    private boolean isEditing = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupFilters();
        loadUserData();
        setupRealTimeValidation();
        setupFormListeners();

        comboRole.setItems(FXCollections.observableArrayList(Role.values()));
        comboStatus.setItems(FXCollections.observableArrayList(Status.values()));

        btnModifier.setDisable(true);
        btnSupprimer.setDisable(true);

        setDefaultImage();
    }

    private void setupRealTimeValidation() {
        txtPrenom.textProperty().addListener((obs, old, newVal) -> {
            if (newVal.trim().isEmpty()) {
                showError(errorPrenom, "❌ Le prénom est requis");
            } else if (newVal.length() < 2) {
                showError(errorPrenom, "❌ Minimum 2 caractères");
            } else if (newVal.length() > 50) {
                showError(errorPrenom, "❌ Maximum 50 caractères");
            } else if (!newVal.matches("^[a-zA-ZÀ-ÿ\\s-]+$")) {
                showError(errorPrenom, "❌ Caractères non autorisés");
            } else {
                clearError(errorPrenom);
            }
        });

        txtNom.textProperty().addListener((obs, old, newVal) -> {
            if (newVal.trim().isEmpty()) {
                showError(errorNom, "❌ Le nom est requis");
            } else if (newVal.length() < 2) {
                showError(errorNom, "❌ Minimum 2 caractères");
            } else if (newVal.length() > 50) {
                showError(errorNom, "❌ Maximum 50 caractères");
            } else if (!newVal.matches("^[a-zA-ZÀ-ÿ\\s-]+$")) {
                showError(errorNom, "❌ Caractères non autorisés");
            } else {
                clearError(errorNom);
            }
        });

        txtEmail.textProperty().addListener((obs, old, newVal) -> {
            String email = newVal.trim().toLowerCase();
            if (email.isEmpty()) {
                showError(errorEmail, "❌ L'email est requis");
            } else if (email.length() > 100) {
                showError(errorEmail, "❌ Email trop long");
            } else if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                showError(errorEmail, "❌ Format invalide (ex: nom@domaine.tn)");
            } else {
                clearError(errorEmail);
            }
        });

        txtTelephone.textProperty().addListener((obs, old, newVal) -> {
            String tel = newVal.trim();
            if (!tel.isEmpty()) {
                if (tel.length() > 20) {
                    showError(errorTelephone, "❌ Numéro trop long");
                } else if (!tel.matches("^(\\+216|0)?[2-9][0-9]{7}$")) {
                    showError(errorTelephone, "❌ Format tunisien invalide (ex: +21699123456)");
                } else {
                    clearError(errorTelephone);
                }
            } else {
                clearError(errorTelephone);
            }
        });

        txtPassword.textProperty().addListener((obs, old, newVal) -> {
            if (!isEditing && selectedUser == null) {
                if (newVal.isEmpty()) {
                    showError(errorPassword, "❌ Le mot de passe est requis");
                } else if (newVal.length() < 8) {
                    showError(errorPassword, "❌ Minimum 8 caractères");
                } else if (newVal.length() > 255) {
                    showError(errorPassword, "❌ Mot de passe trop long");
                } else if (!newVal.matches(".*[A-Za-z].*") || !newVal.matches(".*[0-9].*")) {
                    showError(errorPassword, "❌ Doit contenir lettres ET chiffres");
                } else if (!newVal.matches(".*[A-Z].*")) {
                    showError(errorPassword, "❌ Doit contenir une majuscule");
                } else if (!newVal.matches(".*[a-z].*")) {
                    showError(errorPassword, "❌ Doit contenir une minuscule");
                } else {
                    clearError(errorPassword);
                }
            } else {
                if (!newVal.isEmpty()) {
                    if (newVal.length() < 8) {
                        showError(errorPassword, "❌ Minimum 8 caractères");
                    } else if (newVal.length() > 255) {
                        showError(errorPassword, "❌ Mot de passe trop long");
                    } else if (!newVal.matches(".*[A-Za-z].*") || !newVal.matches(".*[0-9].*")) {
                        showError(errorPassword, "❌ Doit contenir lettres ET chiffres");
                    } else if (!newVal.matches(".*[A-Z].*")) {
                        showError(errorPassword, "❌ Doit contenir une majuscule");
                    } else if (!newVal.matches(".*[a-z].*")) {
                        showError(errorPassword, "❌ Doit contenir une minuscule");
                    } else {
                        clearError(errorPassword);
                    }
                } else {
                    clearError(errorPassword);
                }
            }
        });

        comboRole.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal == null) {
                showError(errorRole, "❌ Le rôle est requis");
            } else {
                clearError(errorRole);
            }
        });

        comboStatus.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal == null) {
                showError(errorStatus, "❌ Le statut est requis");
            } else {
                clearError(errorStatus);
            }
        });

        dpDateNaiss.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                int age = Period.between(newVal, LocalDate.now()).getYears();
                if (age < 18) {
                    showError(errorDate, "❌ Vous devez avoir au moins 18 ans");
                } else if (age > 120) {
                    showError(errorDate, "❌ Date invalide");
                } else {
                    clearError(errorDate);
                }
            } else {
                clearError(errorDate);
            }
        });
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 11px; -fx-font-weight: bold;");
    }

    private void clearError(Label label) {
        label.setText("");
    }

    private void setDefaultImage() {
        try {
            Image defaultImage = new Image("https://i.pravatar.cc/150?u=default", 130, 130, true, true);
            previewImageView.setImage(defaultImage);
            imagePreviewBox.setVisible(true);
            imageFileNameLabel.setText("Avatar par défaut");
        } catch (Exception e) {
            previewImageView.setImage(null);
            imagePreviewBox.setVisible(true);
            imageFileNameLabel.setText("Aucune image");
        }
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colAvatar.setCellFactory(col -> new TableCell<User, String>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitHeight(40);
                imageView.setFitWidth(40);
                imageView.setPreserveRatio(true);
                imageView.setStyle("-fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    String imageUrl = user.getImage();
                    try {
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            imageView.setImage(new Image(imageUrl, true));
                        } else {
                            imageView.setImage(new Image("https://i.pravatar.cc/150?u=" + user.getId(), true));
                        }
                    } catch (Exception e) {
                        imageView.setImage(new Image("https://i.pravatar.cc/150?u=" + user.getId(), true));
                    }
                    setGraphic(imageView);
                }
            }
        });

        colNomComplet.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getPrenom() + " " + cellData.getValue().getNom()
                )
        );

        colEmail.setCellValueFactory(new PropertyValueFactory<>("e_mail"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("num_tel"));

        colDateNaiss.setCellValueFactory(cellData -> {
            String date = cellData.getValue().getDate_naiss();
            if (date != null && !date.isEmpty()) {
                try {
                    LocalDate ld = LocalDate.parse(date);
                    date = ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } catch (Exception ignored) {}
            }
            return new javafx.beans.property.SimpleStringProperty(date != null ? date : "");
        });

        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setCellFactory(col -> new TableCell<User, Role>() {
            @Override
            protected void updateItem(Role role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(5);
                    Label icon = new Label(role == Role.admin ? "👑" : "👤");
                    icon.setStyle("-fx-font-size: 14px;");
                    Label text = new Label(role == Role.admin ? "Admin" : "User");
                    text.setStyle(role == Role.admin ?
                            "-fx-text-fill: #F39C12; -fx-font-weight: bold;" :
                            "-fx-text-fill: #3498DB; -fx-font-weight: bold;");
                    box.getChildren().addAll(icon, text);

                    Tooltip tooltip = new Tooltip(role == Role.admin ? "👑 Administrateur" : "👤 Utilisateur standard");
                    Tooltip.install(box, tooltip);

                    setGraphic(box);
                }
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<User, Status>() {
            @Override
            protected void updateItem(Status status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(5);
                    Label icon = new Label(status == Status.Unbanned ? "✅" : "🚫");
                    icon.setStyle("-fx-font-size: 14px;");
                    Label text = new Label(status == Status.Unbanned ? "Actif" : "Suspendu");
                    text.setStyle(status == Status.Unbanned ?
                            "-fx-text-fill: #27AE60; -fx-font-weight: bold;" :
                            "-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
                    box.getChildren().addAll(icon, text);

                    Tooltip tooltip = new Tooltip(status == Status.Unbanned ? "✅ Compte actif" : "🚫 Compte suspendu");
                    Tooltip.install(box, tooltip);

                    setGraphic(box);
                }
            }
        });

        colActions.setCellFactory(col -> new TableCell<User, Void>() {
            private final Button viewBtn = new Button("👁️");
            private final Button editBtn = new Button("✏️");
            private final Button toggleBtn = new Button();
            private final Button deleteBtn = new Button("🗑️");
            private final HBox pane = new HBox(5, viewBtn, editBtn, toggleBtn, deleteBtn);

            {
                String baseStyle = "-fx-background-color: transparent; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 5; -fx-min-width: 32; -fx-min-height: 32; -fx-background-radius: 16;";

                viewBtn.setStyle(baseStyle + "-fx-text-fill: #3498DB;");
                viewBtn.setTooltip(new Tooltip("👁️ Voir les détails"));

                editBtn.setStyle(baseStyle + "-fx-text-fill: #F1C40F;");
                editBtn.setTooltip(new Tooltip("✏️ Modifier l'utilisateur"));

                deleteBtn.setStyle(baseStyle + "-fx-text-fill: #E74C3C;");
                deleteBtn.setTooltip(new Tooltip("🗑️ Supprimer l'utilisateur"));

                pane.setAlignment(javafx.geometry.Pos.CENTER);
                pane.setSpacing(5);
                pane.setStyle("-fx-padding: 0;");

                viewBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    viewUserDetails(user);
                });

                editBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    editUser(user);
                });

                deleteBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    deleteUser(user);
                });

                toggleBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    toggleUserStatus(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());

                    String baseStyle = "-fx-background-color: transparent; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 5; -fx-min-width: 32; -fx-min-height: 32; -fx-background-radius: 16;";

                    if (user.getStatus() == Status.Unbanned) {
                        toggleBtn.setText("🔒");
                        toggleBtn.setStyle(baseStyle + "-fx-text-fill: #F39C12;");
                        toggleBtn.setTooltip(new Tooltip("🔒 Suspendre l'utilisateur"));
                    } else {
                        toggleBtn.setText("🔓");
                        toggleBtn.setStyle(baseStyle + "-fx-text-fill: #27AE60;");
                        toggleBtn.setTooltip(new Tooltip("🔓 Activer l'utilisateur"));
                    }

                    setGraphic(pane);
                }
            }
        });
    }

    private void showSuccessMessage(String action, User user) {
        String emoji = "";
        String title = "";

        switch(action) {
            case "add":
                emoji = "✅";
                title = "Utilisateur ajouté avec succès";
                break;
            case "update":
                emoji = "✏️";
                title = "Utilisateur modifié avec succès";
                break;
            case "delete":
                emoji = "🗑️";
                title = "Utilisateur supprimé avec succès";
                break;
            case "suspend":
                emoji = "🔒";
                title = "Utilisateur suspendu";
                break;
            case "activate":
                emoji = "🔓";
                title = "Utilisateur activé";
                break;
        }

        String fullName = user.getPrenom() + " " + user.getNom();
        String role = user.getRole() == Role.admin ? "Administrateur" : "Utilisateur";
        String status = user.getStatus() == Status.Unbanned ? "Actif ✅" : "Suspendu 🚫";

        String details = emoji + " " + title + "\n\n" +
                "🆔 ID: " + user.getId() + "\n" +
                "👤 Nom complet: " + fullName + "\n" +
                "📧 Email: " + user.getE_mail() + "\n" +
                "📞 Téléphone: " + (user.getNum_tel() != null ? user.getNum_tel() : "Non renseigné") + "\n" +
                "👑 Rôle: " + role + "\n" +
                "⚡ Statut: " + status;

        showAlert(emoji + " Succès", details, Alert.AlertType.INFORMATION);
    }

    private void viewUserDetails(User user) {
        String fullName = user.getPrenom() + " " + user.getNom();
        String role = user.getRole() == Role.admin ? "Administrateur" : "Utilisateur";
        String status = user.getStatus() == Status.Unbanned ? "Actif ✅" : "Suspendu 🚫";

        String details = "👤 Détails de l'utilisateur\n\n" +
                "🆔 ID: " + user.getId() + "\n" +
                "👤 Prénom: " + user.getPrenom() + "\n" +
                "👤 Nom: " + user.getNom() + "\n" +
                "📧 Email: " + user.getE_mail() + "\n" +
                "📞 Téléphone: " + (user.getNum_tel() != null ? user.getNum_tel() : "Non renseigné") + "\n" +
                "📅 Date naissance: " + (user.getDate_naiss() != null ? user.getDate_naiss() : "Non renseignée") + "\n" +
                "👑 Rôle: " + role + "\n" +
                "⚡ Statut: " + status + "\n" +
                "🖼️ Image: " + (user.getImage() != null && !user.getImage().isEmpty() ? "Disponible" : "Par défaut");

        showAlert("👁️ Détails utilisateur", details, Alert.AlertType.INFORMATION);
    }

    private void setupFilters() {
        filterRoleCombo.setItems(FXCollections.observableArrayList(
                "Tous les rôles", "Administrateurs", "Utilisateurs"
        ));
        filterRoleCombo.getSelectionModel().selectFirst();

        filterStatusCombo.setItems(FXCollections.observableArrayList(
                "Tous les statuts", "Actifs", "Suspendus"
        ));
        filterStatusCombo.getSelectionModel().selectFirst();
    }

    private void loadUserData() {
        try {
            userList.setAll(userCRUD.ShowUsers());
            filteredData = new FilteredList<>(userList, p -> true);
            usersTable.setItems(filteredData);
            applyFilters();
            updateStats();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les utilisateurs: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateStats() {
        int total = userList.size();
        int admins = (int) userList.stream().filter(u -> u.getRole() == Role.admin).count();
        int users = (int) userList.stream().filter(u -> u.getRole() == Role.user).count();
        int banned = (int) userList.stream().filter(u -> u.getStatus() == Status.Banned).count();

        totalUsersLabel.setText(String.valueOf(total));
        adminCountLabel.setText(String.valueOf(admins));
        userCountLabel.setText(String.valueOf(users));
        bannedCountLabel.setText(String.valueOf(banned));
    }

    private void setupFormListeners() {
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
        filterRoleCombo.setOnAction(e -> applyFilters());
        filterStatusCombo.setOnAction(e -> applyFilters());

        txtImage.textProperty().addListener((obs, old, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                try {
                    Image img = new Image(newVal, 130, 130, true, true);
                    previewImageView.setImage(img);
                    imagePreviewBox.setVisible(true);
                    imageFileNameLabel.setText("URL: " + (newVal.length() > 30 ? newVal.substring(0, 30) + "..." : newVal));
                } catch (Exception e) {
                }
            } else {
                if (selectedImageFile == null && (selectedUser == null || selectedUser.getImage() == null)) {
                    setDefaultImage();
                }
            }
        });
    }

    private void applyFilters() {
        if (filteredData == null) return;

        filteredData.setPredicate(user -> {
            String search = searchField.getText().toLowerCase();
            if (!search.isEmpty()) {
                boolean match = user.getPrenom().toLowerCase().contains(search) ||
                        user.getNom().toLowerCase().contains(search) ||
                        user.getE_mail().toLowerCase().contains(search) ||
                        (user.getNum_tel() != null && user.getNum_tel().toLowerCase().contains(search));
                if (!match) return false;
            }

            String roleFilter = filterRoleCombo.getValue();
            if (roleFilter != null) {
                if (roleFilter.equals("Administrateurs") && user.getRole() != Role.admin) return false;
                if (roleFilter.equals("Utilisateurs") && user.getRole() != Role.user) return false;
            }

            String statusFilter = filterStatusCombo.getValue();
            if (statusFilter != null) {
                if (statusFilter.equals("Actifs") && user.getStatus() != Status.Unbanned) return false;
                if (statusFilter.equals("Suspendus") && user.getStatus() != Status.Banned) return false;
            }

            return true;
        });
    }

    @FXML
    private void handleRowSelection(MouseEvent event) {
        if (event.getClickCount() >= 1) {
            selectedUser = usersTable.getSelectionModel().getSelectedItem();
            if (selectedUser != null) {
                fillForm(selectedUser);
                btnModifier.setDisable(false);
                btnSupprimer.setDisable(false);
                btnAjouter.setDisable(true);
                isEditing = true;
            }
        }
    }

    private void fillForm(User user) {
        txtId.setText(String.valueOf(user.getId()));
        txtPrenom.setText(user.getPrenom());
        txtNom.setText(user.getNom());

        if (user.getDate_naiss() != null && !user.getDate_naiss().isEmpty()) {
            try {
                dpDateNaiss.setValue(LocalDate.parse(user.getDate_naiss()));
            } catch (Exception e) {
                dpDateNaiss.setValue(null);
            }
        } else {
            dpDateNaiss.setValue(null);
        }

        txtEmail.setText(user.getE_mail());
        txtTelephone.setText(user.getNum_tel() != null ? user.getNum_tel() : "");
        txtPassword.clear();
        txtImage.setText(user.getImage() != null ? user.getImage() : "");
        comboRole.setValue(user.getRole());
        comboStatus.setValue(user.getStatus());

        if (user.getImage() != null && !user.getImage().isEmpty()) {
            try {
                Image img = new Image(user.getImage(), 130, 130, true, true);
                previewImageView.setImage(img);
                imagePreviewBox.setVisible(true);
                imageFileNameLabel.setText("URL: " + (user.getImage().length() > 30 ? user.getImage().substring(0, 30) + "..." : user.getImage()));
            } catch (Exception e) {
                setDefaultImage();
            }
        } else {
            setDefaultImage();
        }
        selectedImageFile = null;
    }

    @FXML
    private void clearForm() {
        txtId.clear();
        txtPrenom.clear();
        txtNom.clear();
        dpDateNaiss.setValue(null);
        txtEmail.clear();
        txtTelephone.clear();
        txtPassword.clear();
        txtImage.clear();
        comboRole.setValue(null);
        comboStatus.setValue(null);

        clearError(errorPrenom);
        clearError(errorNom);
        clearError(errorDate);
        clearError(errorEmail);
        clearError(errorTelephone);
        clearError(errorPassword);
        clearError(errorRole);
        clearError(errorStatus);
        clearError(errorImage);

        setDefaultImage();

        selectedUser = null;
        selectedImageFile = null;
        isEditing = false;

        btnAjouter.setDisable(false);
        btnModifier.setDisable(true);
        btnSupprimer.setDisable(true);
    }

    @FXML
    private void handleBrowseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp")
        );
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        Stage stage = (Stage) btnParcourirImage.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedImageFile = file;
            String imageUrl = file.toURI().toString();
            txtImage.setText(imageUrl);

            try {
                Image img = new Image(imageUrl, 130, 130, true, true);
                previewImageView.setImage(img);
                imagePreviewBox.setVisible(true);
                imageFileNameLabel.setText("Fichier: " + file.getName());
                clearError(errorImage);
            } catch (Exception e) {
                showError(errorImage, "❌ Impossible de charger l'image");
            }
        }
    }

    private boolean validateFormComplete(boolean isNew) {
        boolean isValid = true;

        if (txtPrenom.getText().trim().isEmpty()) {
            showError(errorPrenom, "❌ Le prénom est requis");
            isValid = false;
        } else if (txtPrenom.getText().length() < 2) {
            showError(errorPrenom, "❌ Minimum 2 caractères");
            isValid = false;
        } else if (txtPrenom.getText().length() > 50) {
            showError(errorPrenom, "❌ Maximum 50 caractères");
            isValid = false;
        } else if (!txtPrenom.getText().matches("^[a-zA-ZÀ-ÿ\\s-]+$")) {
            showError(errorPrenom, "❌ Caractères non autorisés");
            isValid = false;
        }

        if (txtNom.getText().trim().isEmpty()) {
            showError(errorNom, "❌ Le nom est requis");
            isValid = false;
        } else if (txtNom.getText().length() < 2) {
            showError(errorNom, "❌ Minimum 2 caractères");
            isValid = false;
        } else if (txtNom.getText().length() > 50) {
            showError(errorNom, "❌ Maximum 50 caractères");
            isValid = false;
        } else if (!txtNom.getText().matches("^[a-zA-ZÀ-ÿ\\s-]+$")) {
            showError(errorNom, "❌ Caractères non autorisés");
            isValid = false;
        }

        String email = txtEmail.getText().trim().toLowerCase();
        if (email.isEmpty()) {
            showError(errorEmail, "❌ L'email est requis");
            isValid = false;
        } else if (email.length() > 100) {
            showError(errorEmail, "❌ Email trop long");
            isValid = false;
        } else if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            showError(errorEmail, "❌ Format invalide");
            isValid = false;
        }

        String tel = txtTelephone.getText().trim();
        if (!tel.isEmpty()) {
            if (tel.length() > 20) {
                showError(errorTelephone, "❌ Numéro trop long");
                isValid = false;
            } else if (!tel.matches("^(\\+216|0)?[2-9][0-9]{7}$")) {
                showError(errorTelephone, "❌ Format tunisien invalide");
                isValid = false;
            }
        }

        LocalDate dateNaiss = dpDateNaiss.getValue();
        if (dateNaiss != null) {
            int age = Period.between(dateNaiss, LocalDate.now()).getYears();
            if (age < 18) {
                showError(errorDate, "❌ Vous devez avoir au moins 18 ans");
                isValid = false;
            } else if (age > 120) {
                showError(errorDate, "❌ Date invalide");
                isValid = false;
            }
        }

        String password = txtPassword.getText();
        if (isNew) {
            if (password.isEmpty()) {
                showError(errorPassword, "❌ Le mot de passe est requis");
                isValid = false;
            } else if (password.length() < 8) {
                showError(errorPassword, "❌ Minimum 8 caractères");
                isValid = false;
            } else if (password.length() > 255) {
                showError(errorPassword, "❌ Mot de passe trop long");
                isValid = false;
            } else if (!password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
                showError(errorPassword, "❌ Doit contenir lettres ET chiffres");
                isValid = false;
            } else if (!password.matches(".*[A-Z].*")) {
                showError(errorPassword, "❌ Doit contenir une majuscule");
                isValid = false;
            } else if (!password.matches(".*[a-z].*")) {
                showError(errorPassword, "❌ Doit contenir une minuscule");
                isValid = false;
            }
        } else {
            if (!password.isEmpty()) {
                if (password.length() < 8) {
                    showError(errorPassword, "❌ Minimum 8 caractères");
                    isValid = false;
                } else if (password.length() > 255) {
                    showError(errorPassword, "❌ Mot de passe trop long");
                    isValid = false;
                } else if (!password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
                    showError(errorPassword, "❌ Doit contenir lettres ET chiffres");
                    isValid = false;
                } else if (!password.matches(".*[A-Z].*")) {
                    showError(errorPassword, "❌ Doit contenir une majuscule");
                    isValid = false;
                } else if (!password.matches(".*[a-z].*")) {
                    showError(errorPassword, "❌ Doit contenir une minuscule");
                    isValid = false;
                }
            }
        }

        if (comboRole.getValue() == null) {
            showError(errorRole, "❌ Le rôle est requis");
            isValid = false;
        }

        if (comboStatus.getValue() == null) {
            showError(errorStatus, "❌ Le statut est requis");
            isValid = false;
        }

        return isValid;
    }

    @FXML
    private void ajouterUtilisateur(ActionEvent event) {
        if (!validateFormComplete(true)) return;

        try {
            String email = txtEmail.getText().trim().toLowerCase();
            User existing = userCRUD.getUserByEmail(email);
            if (existing != null) {
                showError(errorEmail, "❌ Cet email est déjà utilisé !");
                showAlert("Erreur", "❌ Cet email est déjà utilisé par un autre compte !", Alert.AlertType.ERROR);
                return;
            }

            User newUser = new User();
            newUser.setPrenom(txtPrenom.getText().trim());
            newUser.setNom(txtNom.getText().trim());
            newUser.setDate_naiss(dpDateNaiss.getValue() != null ?
                    dpDateNaiss.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null);
            newUser.setE_mail(email);
            newUser.setNum_tel(txtTelephone.getText().trim().isEmpty() ? null : txtTelephone.getText().trim());
            newUser.setMot_de_pass(txtPassword.getText());
            newUser.setImage(txtImage.getText().trim().isEmpty() ? null : txtImage.getText().trim());
            newUser.setRole(comboRole.getValue() != null ? comboRole.getValue() : Role.user);
            newUser.setStatus(comboStatus.getValue() != null ? comboStatus.getValue() : Status.Unbanned);

            userCRUD.createUser(newUser);

            User createdUser = userCRUD.getUserByEmail(email);

            showSuccessMessage("add", createdUser);
            loadUserData();
            clearForm();

        } catch (SQLException e) {
            showAlert("Erreur", "❌ Erreur lors de l'ajout: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void modifierUtilisateur(ActionEvent event) {
        if (selectedUser == null) {
            showAlert("Attention", "⚠️ Sélectionnez un utilisateur à modifier !", Alert.AlertType.WARNING);
            return;
        }

        if (!validateFormComplete(false)) return;

        try {
            String email = txtEmail.getText().trim().toLowerCase();
            if (!email.equals(selectedUser.getE_mail())) {
                User existing = userCRUD.getUserByEmail(email);
                if (existing != null) {
                    showError(errorEmail, "❌ Cet email est déjà utilisé !");
                    showAlert("Erreur", "❌ Cet email est déjà utilisé par un autre compte !", Alert.AlertType.ERROR);
                    return;
                }
            }

            String oldImage = selectedUser.getImage();

            selectedUser.setPrenom(txtPrenom.getText().trim());
            selectedUser.setNom(txtNom.getText().trim());
            selectedUser.setDate_naiss(dpDateNaiss.getValue() != null ?
                    dpDateNaiss.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null);
            selectedUser.setE_mail(email);
            selectedUser.setNum_tel(txtTelephone.getText().trim().isEmpty() ? null : txtTelephone.getText().trim());

            String newImageUrl = txtImage.getText().trim().isEmpty() ? null : txtImage.getText().trim();

            boolean imageChanged = (oldImage == null && newImageUrl != null) ||
                    (oldImage != null && !oldImage.equals(newImageUrl));

            if (imageChanged) {
                selectedUser.setImage(newImageUrl);
                userCRUD.updateImageUser(selectedUser);
            }

            selectedUser.setRole(comboRole.getValue());
            selectedUser.setStatus(comboStatus.getValue());

            if (!txtPassword.getText().isEmpty()) {
                selectedUser.setMot_de_pass(txtPassword.getText());
                userCRUD.updatePassword(selectedUser);
            }

            userCRUD.updateUser(selectedUser);

            showSuccessMessage("update", selectedUser);
            loadUserData();
            clearForm();

        } catch (SQLException e) {
            showAlert("Erreur", "❌ Erreur lors de la modification: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void supprimerUtilisateur(ActionEvent event) {
        if (selectedUser == null) {
            showAlert("Attention", "⚠️ Sélectionnez un utilisateur à supprimer !", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("🗑️ Supprimer l'utilisateur ?");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer " +
                selectedUser.getPrenom() + " " + selectedUser.getNom() + " ?\n\nCette action est irréversible.");

        DialogPane dialogPane = confirm.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-border-color: #E74C3C; -fx-border-width: 2; -fx-border-radius: 15; -fx-background-radius: 15;");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                User deletedUser = selectedUser;
                userCRUD.deleteUser(selectedUser);
                showSuccessMessage("delete", deletedUser);
                loadUserData();
                clearForm();
            } catch (SQLException e) {
                showAlert("Erreur", "❌ Impossible de supprimer: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void toggleUserStatus(User user) {
        try {
            String action;
            Status oldStatus = user.getStatus();

            if (user.getStatus() == Status.Unbanned) {
                user.setStatus(Status.Banned);
                action = "suspend";
            } else {
                user.setStatus(Status.Unbanned);
                action = "activate";
            }
            userCRUD.updateUser(user);

            showSuccessMessage(action, user);
            loadUserData();

        } catch (SQLException e) {
            showAlert("Erreur", "❌ Impossible de changer le statut: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void editUser(User user) {
        usersTable.getSelectionModel().select(user);
        fillForm(user);
        btnModifier.setDisable(false);
        btnSupprimer.setDisable(false);
        btnAjouter.setDisable(true);
        isEditing = true;
    }

    private void deleteUser(User user) {
        selectedUser = user;
        supprimerUtilisateur(null);
    }

    @FXML
    private void resetFilters() {
        searchField.clear();
        filterRoleCombo.getSelectionModel().selectFirst();
        filterStatusCombo.getSelectionModel().selectFirst();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-border-color: #1ABC9C; -fx-border-width: 2; -fx-border-radius: 15; -fx-background-radius: 15;");

        alert.showAndWait();
    }

    @FXML private void handleShowUsers(ActionEvent event) {}

    @FXML private void handleShowAccounts(ActionEvent event) {
        showAlert("Information", "📦 Gestion des Hébergements - En cours de développement", Alert.AlertType.INFORMATION);
    }

    @FXML private void handleShowTransactions(ActionEvent event) {
        showAlert("Information", "🎯 Gestion des Activités - En cours de développement", Alert.AlertType.INFORMATION);
    }

    @FXML private void handleShowCredits(ActionEvent event) {
        showAlert("Information", "🚗 Gestion des Trajets - En cours de développement", Alert.AlertType.INFORMATION);
    }

    @FXML private void handleShowCashback(ActionEvent event) {
        showAlert("Information", "🏷️ Gestion des Promotions - En cours de développement", Alert.AlertType.INFORMATION);
    }

    @FXML private void handleShowSettings(ActionEvent event) {
        showAlert("Information", "⚙️ Paramètres - En cours de développement", Alert.AlertType.INFORMATION);
    }

    @FXML private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnDeconnexion.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
        } catch (Exception e) {
            showAlert("Erreur", "Déconnexion impossible: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML private void handleCancel(ActionEvent event) {
        clearForm();
    }
}
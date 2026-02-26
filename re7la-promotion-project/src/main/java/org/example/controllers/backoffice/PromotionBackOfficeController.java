package org.example.controllers.backoffice;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.models.*;
import org.example.services.OffresStaticData;
import org.example.services.PromotionService;
import org.example.services.PromotionTargetService;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class PromotionBackOfficeController implements Initializable {

    // ========== SIDEBAR ==========
    @FXML private Button btnGestionUtilisateurs;
    @FXML private Button btnComptesBancaires;
    @FXML private Button btnTransactions;
    @FXML private Button btnCredits;
    @FXML private Button btnCashback;
    @FXML private Button btnStatistiques;   // ⭐ bouton stats
    @FXML private Button btnParametres;
    @FXML private Button btnDeconnexion;

    // ========== STATS LABELS ==========
    @FXML private Label statTotalPromo;
    @FXML private Label statPacksPromo;
    @FXML private Label statActivesPromo;

    // ========== MAIN CONTENT ==========
    @FXML private StackPane mainContentStack;
    @FXML private ScrollPane viewPromotion;
    @FXML private VBox viewEmpty;

    // ========== FORM FIELDS ==========
    @FXML private TextField txtNom;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtPourcentage;
    @FXML private TextField txtFixe;
    @FXML private TextField txtPrixParJour;
    @FXML private DatePicker dateDebut;
    @FXML private DatePicker dateFin;
    @FXML private CheckBox chkPack;
    @FXML private Button btnAjouter;
    @FXML private Button btnSupprimer;

    // ========== SELECTION OFFRES ==========
    @FXML private ComboBox<String> comboTypeOffre;
    @FXML private TableView<Object> tableOffres;
    @FXML private TableColumn<Object, Integer> colOffreId;
    @FXML private TableColumn<Object, String> colOffreNom;
    @FXML private TableColumn<Object, String> colOffreDetails;
    @FXML private Button btnAjouterOffre;

    // ========== TABLE PROMOTIONS ==========
    @FXML private TableView<Promotion> tablePromotion;
    @FXML private TableColumn<Promotion, Integer> colId;
    @FXML private TableColumn<Promotion, String> colNom;
    @FXML private TableColumn<Promotion, String> colDescription;
    @FXML private TableColumn<Promotion, Float> colPourcentage;
    @FXML private TableColumn<Promotion, Float> colFixe;
    @FXML private TableColumn<Promotion, Float> colPrixParJour;
    @FXML private TableColumn<Promotion, Date> colDateDebut;
    @FXML private TableColumn<Promotion, Date> colDateFin;
    @FXML private TableColumn<Promotion, String> colType;

    // ========== SERVICES ==========
    private PromotionService promotionService;
    private PromotionTargetService targetService;
    private OffresStaticData offresData;

    // ========== DATA ==========
    private ObservableList<Promotion> promotions;
    private Promotion selectedPromotion;
    private List<PromotionTarget> selectedTargets = new ArrayList<>();

    // ═══════════════════════════════════════════════════════════
    // INITIALISATION
    // ═══════════════════════════════════════════════════════════
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        promotionService = new PromotionService();
        targetService = new PromotionTargetService();
        offresData = OffresStaticData.getInstance();

        setupPromotionTable();
        setupOffresTable();
        setupComboBox();
        loadPromotions();
        showView(viewPromotion);
    }

    // ═══════════════════════════════════════════════════════════
    // SETUP TABLES
    // ═══════════════════════════════════════════════════════════
    private void setupPromotionTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colPourcentage.setCellValueFactory(new PropertyValueFactory<>("discountPercentage"));
        colFixe.setCellValueFactory(new PropertyValueFactory<>("discountFixed"));
        colPrixParJour.setCellValueFactory(new PropertyValueFactory<>("prixParJour"));
        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        colType.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().isPack() ? "Pack" : "Individuel"
                )
        );

        tablePromotion.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> {
                    if (newSel != null) fillFormWithPromotion(newSel);
                }
        );
    }

    private void setupOffresTable() {
        colOffreId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOffreNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colOffreDetails.setCellValueFactory(cellData -> {
            Object item = cellData.getValue();
            if (item instanceof Hebergement h)
                return new javafx.beans.property.SimpleStringProperty(h.getVille() + " - " + h.getType());
            if (item instanceof Activite a)
                return new javafx.beans.property.SimpleStringProperty(a.getLieu() + " - " + a.getType());
            if (item instanceof Transport t)
                return new javafx.beans.property.SimpleStringProperty(t.getTrajet() + " - " + t.getTypeTransport());
            return new javafx.beans.property.SimpleStringProperty("");
        });
    }

    private void setupComboBox() {
        comboTypeOffre.setItems(FXCollections.observableArrayList("Hébergement", "Activité", "Transport"));
        comboTypeOffre.setOnAction(e -> loadOffresForSelectedType());
    }

    // ═══════════════════════════════════════════════════════════
    // LOAD DATA
    // ═══════════════════════════════════════════════════════════
    private void loadPromotions() {
        promotions = FXCollections.observableArrayList(promotionService.getAll());
        tablePromotion.setItems(promotions);
        updateStats();
    }

    private void updateStats() {
        try {
            if (promotions == null || promotions.isEmpty()) {
                if (statTotalPromo != null) statTotalPromo.setText("0");
                if (statPacksPromo != null) statPacksPromo.setText("0");
                if (statActivesPromo != null) statActivesPromo.setText("0");
                return;
            }

            int total = promotions.size();
            long packs = promotions.stream().filter(Promotion::isPack).count();
            LocalDate today = LocalDate.now();
            long actives = promotions.stream().filter(p -> {
                LocalDate debut = p.getStartDate().toLocalDate();
                LocalDate fin = p.getEndDate().toLocalDate();
                return !today.isBefore(debut) && !today.isAfter(fin);
            }).count();

            if (statTotalPromo != null) statTotalPromo.setText(String.valueOf(total));
            if (statPacksPromo != null) statPacksPromo.setText(String.valueOf(packs));
            if (statActivesPromo != null) statActivesPromo.setText(String.valueOf(actives));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadOffresForSelectedType() {
        String type = comboTypeOffre.getValue();
        if (type == null) return;

        ObservableList<Object> offres = FXCollections.observableArrayList();
        switch (type) {
            case "Hébergement" -> offres.addAll(offresData.getAllHebergements());
            case "Activité"    -> offres.addAll(offresData.getAllActivites());
            case "Transport"   -> offres.addAll(offresData.getAllTransports());
        }
        tableOffres.setItems(offres);
    }

    // ═══════════════════════════════════════════════════════════
    // FORM
    // ═══════════════════════════════════════════════════════════
    private void fillFormWithPromotion(Promotion promo) {
        selectedPromotion = promo;
        txtNom.setText(promo.getName());
        txtDescription.setText(promo.getDescription());

        txtPourcentage.setText(promo.getDiscountPercentage() != null ? promo.getDiscountPercentage().toString() : "");
        txtFixe.setText(promo.getDiscountFixed() != null ? promo.getDiscountFixed().toString() : "");
        txtPrixParJour.setText(promo.getPrixParJour() != null ? promo.getPrixParJour().toString() : "50.0");

        dateDebut.setValue(promo.getStartDate().toLocalDate());
        dateFin.setValue(promo.getEndDate().toLocalDate());
        chkPack.setSelected(promo.isPack());
        btnAjouter.setText("💾   Modifier la promotion");
    }

    @FXML
    private void addOrUpdate() {
        try {
            String nom = txtNom.getText().trim();
            String description = txtDescription.getText().trim();

            if (nom.isEmpty() || description.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Champs requis", "Le nom et la description sont obligatoires.");
                return;
            }
            if (!nom.matches(".*[a-zA-ZÀ-ÿ].*")) {
                showAlert(Alert.AlertType.WARNING, "Nom invalide", "Le nom doit contenir au moins une lettre.");
                return;
            }
            if (!description.matches(".*[a-zA-ZÀ-ÿ].*")) {
                showAlert(Alert.AlertType.WARNING, "Description invalide", "La description doit contenir au moins une lettre.");
                return;
            }

            Float pourcentage = null;
            Float fixe = null;

            if (!txtPourcentage.getText().trim().isEmpty()) {
                pourcentage = Float.parseFloat(txtPourcentage.getText().trim());
                if (pourcentage < 0 || pourcentage > 100) {
                    showAlert(Alert.AlertType.WARNING, "Pourcentage invalide", "Le pourcentage doit être entre 0 et 100.");
                    return;
                }
            }
            if (!txtFixe.getText().trim().isEmpty()) {
                fixe = Float.parseFloat(txtFixe.getText().trim());
                if (fixe <= 0) {
                    showAlert(Alert.AlertType.WARNING, "Réduction fixe invalide", "La réduction fixe doit être supérieure à 0.");
                    return;
                }
            }
            if (pourcentage == null && fixe == null) {
                showAlert(Alert.AlertType.WARNING, "Réduction requise", "Spécifiez un pourcentage ou une réduction fixe.");
                return;
            }
            if (txtPrixParJour.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Prix par jour requis", "Le prix par jour est obligatoire.");
                return;
            }

            Float prixParJour = Float.parseFloat(txtPrixParJour.getText().trim());
            if (prixParJour <= 0) {
                showAlert(Alert.AlertType.WARNING, "Prix invalide", "Le prix par jour doit être supérieur à 0.");
                return;
            }

            LocalDate debut = dateDebut.getValue();
            LocalDate fin = dateFin.getValue();
            if (debut == null || fin == null) {
                showAlert(Alert.AlertType.WARNING, "Dates requises", "Les dates de début et de fin sont obligatoires.");
                return;
            }
            if (!fin.isAfter(debut)) {
                showAlert(Alert.AlertType.WARNING, "Dates invalides", "La date de fin doit être après la date de début.");
                return;
            }

            boolean isPack = chkPack.isSelected();

            if (selectedPromotion == null) {
                Promotion newPromo = new Promotion(nom, description, pourcentage, fixe,
                        Date.valueOf(debut), Date.valueOf(fin), isPack, prixParJour);
                Promotion saved = promotionService.add(newPromo);
                if (saved != null && saved.getId() > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Promotion ajoutée ! ID : " + saved.getId());
                } else {
                    showAlert(Alert.AlertType.ERROR, "Échec", "❌ Impossible d'enregistrer la promotion.");
                    return;
                }
            } else {
                selectedPromotion.setName(nom);
                selectedPromotion.setDescription(description);
                selectedPromotion.setDiscountPercentage(pourcentage);
                selectedPromotion.setDiscountFixed(fixe);
                selectedPromotion.setStartDate(Date.valueOf(debut));
                selectedPromotion.setEndDate(Date.valueOf(fin));
                selectedPromotion.setPack(isPack);
                selectedPromotion.setPrixParJour(prixParJour);
                boolean updated = promotionService.update(selectedPromotion);
                if (updated) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Promotion modifiée !");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Échec", "❌ Impossible de modifier la promotion.");
                    return;
                }
            }

            loadPromotions();
            tablePromotion.refresh();
            handleCancel();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de format", "Valeurs numériques invalides : " + e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur inattendue", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void ajouterOffreAPromotion() {
        if (selectedPromotion == null) {
            showAlert(Alert.AlertType.WARNING, "Promotion non sélectionnée", "Sélectionnez d'abord une promotion.");
            return;
        }
        Object selectedOffre = tableOffres.getSelectionModel().getSelectedItem();
        if (selectedOffre == null) {
            showAlert(Alert.AlertType.WARNING, "Offre non sélectionnée", "Sélectionnez une offre dans la liste.");
            return;
        }

        TargetType targetType = null;
        int targetId = 0;

        if (selectedOffre instanceof Hebergement h) { targetType = TargetType.HEBERGEMENT; targetId = h.getId(); }
        else if (selectedOffre instanceof Activite a) { targetType = TargetType.ACTIVITE;    targetId = a.getId(); }
        else if (selectedOffre instanceof Transport t) { targetType = TargetType.TRANSPORT;   targetId = t.getId(); }

        if (targetType != null) {
            targetService.add(new PromotionTarget(selectedPromotion.getId(), targetType, targetId));
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Offre ajoutée à la promotion !");
        }
    }

    @FXML
    private void deletePromotion() {
        if (selectedPromotion == null) {
            showAlert(Alert.AlertType.WARNING, "Aucune sélection", "Sélectionnez une promotion à supprimer.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer la promotion ?");
        confirmation.setContentText("Cette action est irréversible.");
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                targetService.deleteByPromotionId(selectedPromotion.getId());
                promotionService.delete(selectedPromotion.getId());
                loadPromotions();
                handleCancel();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Promotion supprimée !");
            }
        });
    }

    @FXML
    private void handleCancel() {
        txtNom.clear();
        txtDescription.clear();
        txtPourcentage.clear();
        txtFixe.clear();
        txtPrixParJour.clear();
        dateDebut.setValue(null);
        dateFin.setValue(null);
        chkPack.setSelected(false);
        selectedPromotion = null;
        btnAjouter.setText("💾   Enregistrer la promotion");
        tablePromotion.getSelectionModel().clearSelection();
    }

    // ═══════════════════════════════════════════════════════════
    // ⭐ OUVERTURE PAGE STATISTIQUES
    // ═══════════════════════════════════════════════════════════
    @FXML
    private void handleShowStatistiques() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/backoffice/StatistiquesReservations.fxml")
            );
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Statistiques des Reservations");
            stage.setScene(new Scene(root, 1400, 900));
            stage.setResizable(true);
            stage.show();

        } catch (IOException e) {
            System.err.println("Erreur ouverture statistiques : " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir les statistiques.\n" + e.getMessage());
        }
    }

    // ========== SIDEBAR NAVIGATION ==========
    @FXML private void handleShowUsers()        { showView(viewEmpty); }
    @FXML private void handleShowAccounts()     { showView(viewEmpty); }
    @FXML private void handleShowTransactions() { showView(viewEmpty); }
    @FXML private void handleShowCredits()      { showView(viewEmpty); }
    @FXML private void handleShowCashback()     { showView(viewPromotion); }
    @FXML private void handleShowSettings()     { showView(viewEmpty); }
    @FXML private void handleLogout()           { showAlert(Alert.AlertType.INFORMATION, "Déconnexion", "À implémenter."); }

    // ========== HELPERS ==========
    private void showView(javafx.scene.Node view) {
        viewPromotion.setVisible(false);
        viewEmpty.setVisible(false);
        view.setVisible(true);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
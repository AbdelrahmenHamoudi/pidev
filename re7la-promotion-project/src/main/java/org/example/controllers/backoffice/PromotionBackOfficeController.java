package org.example.controllers.backoffice;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.models.*;
import org.example.services.OffresStaticData;
import org.example.services.PromotionService;
import org.example.services.PromotionTargetService;

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
    @FXML private Button btnParametres;
    @FXML private Button btnDeconnexion;

    // ====== labels =========
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
    @FXML private TableColumn<Promotion, Float> colPrixParJour;  // ⭐
    @FXML private TableColumn<Promotion, Date> colDateDebut;
    @FXML private TableColumn<Promotion, Date> colDateFin;
    @FXML private TableColumn<Promotion, String> colType;

    // Services
    private PromotionService promotionService;
    private PromotionTargetService targetService;
    private OffresStaticData offresData;

    // Data
    private ObservableList<Promotion> promotions;
    private Promotion selectedPromotion;
    private List<PromotionTarget> selectedTargets = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize services

        promotionService = new PromotionService();
        targetService = new PromotionTargetService();
        offresData = OffresStaticData.getInstance();

        // Setup tables
        setupPromotionTable();
        setupOffresTable();
        setupComboBox();

        // Load data
        loadPromotions();

        // Show promotion view by default
        showView(viewPromotion);
    }

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

        // Selection listener
        tablePromotion.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    fillFormWithPromotion(newSelection);
                }
            }
        );
    }

    private void setupOffresTable() {
        colOffreId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOffreNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        
        // Details selon le type
        colOffreDetails.setCellValueFactory(cellData -> {
            Object item = cellData.getValue();
            if (item instanceof Hebergement) {
                Hebergement h = (Hebergement) item;
                return new javafx.beans.property.SimpleStringProperty(h.getVille() + " - " + h.getType());
            } else if (item instanceof Activite) {
                Activite a = (Activite) item;
                return new javafx.beans.property.SimpleStringProperty(a.getLieu() + " - " + a.getType());
            } else if (item instanceof Transport) {
                Transport t = (Transport) item;
                return new javafx.beans.property.SimpleStringProperty(t.getTrajet() + " - " + t.getTypeTransport());
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
    }

    private void setupComboBox() {
        comboTypeOffre.setItems(FXCollections.observableArrayList(
            "Hébergement", "Activité", "Transport"
        ));
        
        comboTypeOffre.setOnAction(e -> loadOffresForSelectedType());
    }

    private void loadPromotions() {
        promotions = FXCollections.observableArrayList(promotionService.getAll());
        tablePromotion.setItems(promotions);
    }
//⭐  update labels
    private void updateStats() {
        try {
            // Total promotions
            int total = promotions.size();

            // Packs promo
            long packs = promotions.stream()
                    .filter(Promotion::isPack)
                    .count();

            // Actives (entre date début et date fin)
            java.time.LocalDate today = java.time.LocalDate.now();
            long actives = promotions.stream()
                    .filter(p -> {
                        java.time.LocalDate debut = p.getStartDate().toLocalDate();
                        java.time.LocalDate fin = p.getEndDate().toLocalDate();
                        return !today.isBefore(debut) && !today.isAfter(fin);
                    })
                    .count();

            // Mettre à jour les labels
            if (statTotalPromo != null) statTotalPromo.setText(String.valueOf(total));
            if (statPacksPromo != null) statPacksPromo.setText(String.valueOf(packs));
            if (statActivesPromo != null) statActivesPromo.setText(String.valueOf(actives));

        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour des stats");
            e.printStackTrace();
        }
    }

    private void loadOffresForSelectedType() {
        String type = comboTypeOffre.getValue();
        if (type == null) return;

        ObservableList<Object> offres = FXCollections.observableArrayList();
        
        switch (type) {
            case "Hébergement":
                offres.addAll(offresData.getAllHebergements());
                break;
            case "Activité":
                offres.addAll(offresData.getAllActivites());
                break;
            case "Transport":
                offres.addAll(offresData.getAllTransports());
                break;
        }
        
        tableOffres.setItems(offres);
    }

    // ✅ NOUVEAU
    private void fillFormWithPromotion(Promotion promo) {
        selectedPromotion = promo;

        txtNom.setText(promo.getName());
        txtDescription.setText(promo.getDescription());

        if (promo.getDiscountPercentage() != null) {
            txtPourcentage.setText(promo.getDiscountPercentage().toString());
        } else {
            txtPourcentage.clear();
        }

        if (promo.getDiscountFixed() != null) {
            txtFixe.setText(promo.getDiscountFixed().toString());
        } else {
            txtFixe.clear();
        }

        // ⭐ AJOUTÉ
        if (promo.getPrixParJour() != null) {
            txtPrixParJour.setText(promo.getPrixParJour().toString());
        } else {
            txtPrixParJour.setText("50.0");
        }

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
                showAlert(Alert.AlertType.WARNING, "Champs requis", 
                    "Le nom et la description sont obligatoires.");
                return;
            }
            
            Float pourcentage = null;
            Float fixe = null;
            
            if (!txtPourcentage.getText().trim().isEmpty()) {
                pourcentage = Float.parseFloat(txtPourcentage.getText().trim());
            }
            
            if (!txtFixe.getText().trim().isEmpty()) {
                fixe = Float.parseFloat(txtFixe.getText().trim());
            }
            
            if (pourcentage == null && fixe == null) {
                showAlert(Alert.AlertType.WARNING, "Réduction requise", 
                    "Vous devez spécifier soit un pourcentage soit une réduction fixe.");
                return;
            }
            // ⭐ AJOUT VALIDATION PRIX PAR JOUR
            if (txtPrixParJour.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Prix par jour requis",
                        "Vous devez spécifier le prix par jour.");
                return;
            }

            Float prixParJour = Float.parseFloat(txtPrixParJour.getText().trim());

            if (prixParJour <= 0) {
                showAlert(Alert.AlertType.WARNING, "Prix invalide",
                        "Le prix par jour doit être supérieur à 0.");
                return;
            }
            
            LocalDate debut = dateDebut.getValue();
            LocalDate fin = dateFin.getValue();
            
            if (debut == null || fin == null) {
                showAlert(Alert.AlertType.WARNING, "Dates requises", 
                    "Les dates de début et de fin sont obligatoires.");
                return;
            }

            boolean isPack = chkPack.isSelected();
            if (selectedPromotion == null) {
                // Ajout
                Promotion newPromo = new Promotion(nom, description, pourcentage, fixe,
                        Date.valueOf(debut), Date.valueOf(fin), isPack, prixParJour);  // ⭐
                promotionService.add(newPromo);

                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Promotion ajoutée avec succès !");
                // ✅ NOUVEAU
            } else {
                // Modification
                selectedPromotion.setName(nom);
                selectedPromotion.setDescription(description);
                selectedPromotion.setDiscountPercentage(pourcentage);
                selectedPromotion.setDiscountFixed(fixe);
                selectedPromotion.setStartDate(Date.valueOf(debut));
                selectedPromotion.setEndDate(Date.valueOf(fin));
                selectedPromotion.setPack(isPack);
                selectedPromotion.setPrixParJour(prixParJour);  // ⭐ AJOUTÉ

                promotionService.update(selectedPromotion);

                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Promotion modifiée avec succès !");
            }

            loadPromotions();
            tablePromotion.refresh();   // ⭐
            handleCancel();


        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de format", 
                "Veuillez entrer des valeurs numériques valides pour les réductions.");
        }
    }

    @FXML
    private void ajouterOffreAPromotion() {
        if (selectedPromotion == null) {
            showAlert(Alert.AlertType.WARNING, "Promotion non sélectionnée", 
                "Veuillez d'abord sélectionner ou créer une promotion.");
            return;
        }
        
        Object selectedOffre = tableOffres.getSelectionModel().getSelectedItem();
        if (selectedOffre == null) {
            showAlert(Alert.AlertType.WARNING, "Offre non sélectionnée", 
                "Veuillez sélectionner une offre dans la liste.");
            return;
        }
        
        String type = comboTypeOffre.getValue();
        TargetType targetType = null;
        int targetId = 0;
        
        if (selectedOffre instanceof Hebergement) {
            targetType = TargetType.HEBERGEMENT;
            targetId = ((Hebergement) selectedOffre).getId();
        } else if (selectedOffre instanceof Activite) {
            targetType = TargetType.ACTIVITE;
            targetId = ((Activite) selectedOffre).getId();
        } else if (selectedOffre instanceof Transport) {
            targetType = TargetType.TRANSPORT;
            targetId = ((Transport) selectedOffre).getId();
        }
        
        if (targetType != null) {
            PromotionTarget target = new PromotionTarget(
                selectedPromotion.getId(), targetType, targetId
            );
            targetService.add(target);
            
            showAlert(Alert.AlertType.INFORMATION, "Succès", 
                "Offre ajoutée à la promotion !");
        }
    }

    @FXML
    private void deletePromotion() {
        if (selectedPromotion == null) {
            showAlert(Alert.AlertType.WARNING, "Aucune sélection", 
                "Veuillez sélectionner une promotion à supprimer.");
            return;
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer la promotion ?");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer cette promotion ?");
        
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

    // ✅ NOUVEAU
    @FXML
    private void handleCancel() {
        txtNom.clear();
        txtDescription.clear();
        txtPourcentage.clear();
        txtFixe.clear();
        txtPrixParJour.clear();  // ⭐ AJOUTÉ
        dateDebut.setValue(null);
        dateFin.setValue(null);
        chkPack.setSelected(false);
        selectedPromotion = null;
        btnAjouter.setText("💾   Enregistrer la promotion");
        tablePromotion.getSelectionModel().clearSelection();
    }

    // ========== SIDEBAR NAVIGATION ==========
    @FXML
    private void handleShowUsers() {
        showView(viewEmpty);
    }

    @FXML
    private void handleShowAccounts() {
        showView(viewEmpty);
    }

    @FXML
    private void handleShowTransactions() {
        showView(viewEmpty);
    }

    @FXML
    private void handleShowCredits() {
        showView(viewEmpty);
    }

    @FXML
    private void handleShowCashback() {
        showView(viewPromotion);
    }

    @FXML
    private void handleShowSettings() {
        showView(viewEmpty);
    }

    @FXML
    private void handleLogout() {
        showAlert(Alert.AlertType.INFORMATION, "Déconnexion", "Fonctionnalité à implémenter");
    }

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

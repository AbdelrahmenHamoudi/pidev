package org.example.controllers.frontoffice;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.models.*;
import org.example.services.OffresStaticData;
import org.example.services.PromoCodeService;
import org.example.services.PromotionService;
import org.example.services.PromotionTargetService;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

public class PromotionFrontOfficeController implements Initializable {

    @FXML private TextField searchField;
    @FXML private CheckBox packCheckbox;
    @FXML private CheckBox individuCheckbox;
    @FXML private CheckBox activeCheckbox;
    @FXML private Text resultsCountText;
    @FXML private ComboBox<String> sortCombo;
    @FXML private FlowPane promotionsFlowPane;
    @FXML private Button btnMesReservations;

    private PromotionService promotionService;
    private PromotionTargetService targetService;
    private OffresStaticData offresData;
    private PromoCodeService promoCodeService; // ⭐

    private ObservableList<Promotion> allPromotions   = FXCollections.observableArrayList();
    private ObservableList<Promotion> filteredPromotions = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        promotionService  = new PromotionService();
        targetService     = new PromotionTargetService();
        offresData        = OffresStaticData.getInstance();
        promoCodeService  = new PromoCodeService(); // ⭐

        setupFilters();
        loadPromotions();
        displayPromotions();
        setupListeners();
    }

    private void setupFilters() {
        packCheckbox.setSelected(true);
        individuCheckbox.setSelected(true);
        activeCheckbox.setSelected(true);
        packCheckbox.setOnAction(e -> applyFilters());
        individuCheckbox.setOnAction(e -> applyFilters());
        activeCheckbox.setOnAction(e -> applyFilters());
        sortCombo.setItems(FXCollections.observableArrayList("Plus récentes", "Réduction croissante", "Réduction décroissante"));
        sortCombo.setValue("Plus récentes");
        sortCombo.setOnAction(e -> { sortPromotions(); displayPromotions(); });
    }

    private void setupListeners() {
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void loadPromotions() {
        allPromotions.addAll(promotionService.getAll());
        filteredPromotions.addAll(allPromotions);
    }

    private void applyFilters() {
        String search = searchField.getText().toLowerCase();
        boolean showPacks = packCheckbox.isSelected();
        boolean showIndiv = individuCheckbox.isSelected();
        boolean onlyActive = activeCheckbox.isSelected();
        LocalDate today = LocalDate.now();
        filteredPromotions.clear();
        for (Promotion p : allPromotions) {
            boolean matchSearch = search.isEmpty()
                    || p.getName().toLowerCase().contains(search)
                    || p.getDescription().toLowerCase().contains(search);
            boolean matchType = (p.isPack() && showPacks) || (!p.isPack() && showIndiv);
            boolean matchActive = !onlyActive
                    || (!p.getStartDate().toLocalDate().isAfter(today) && !p.getEndDate().toLocalDate().isBefore(today));
            if (matchSearch && matchType && matchActive) filteredPromotions.add(p);
        }
        sortPromotions();
        displayPromotions();
        updateResultsCount();
    }

    private void sortPromotions() {
        switch (sortCombo.getValue()) {
            case "Réduction croissante"  -> filteredPromotions.sort((a, b) -> Float.compare(
                    a.getDiscountPercentage() != null ? a.getDiscountPercentage() : 0,
                    b.getDiscountPercentage() != null ? b.getDiscountPercentage() : 0));
            case "Réduction décroissante" -> filteredPromotions.sort((a, b) -> Float.compare(
                    b.getDiscountPercentage() != null ? b.getDiscountPercentage() : 0,
                    a.getDiscountPercentage() != null ? a.getDiscountPercentage() : 0));
            default -> filteredPromotions.sort((a, b) -> b.getStartDate().compareTo(a.getStartDate()));
        }
    }

    private void displayPromotions() {
        promotionsFlowPane.getChildren().clear();
        for (Promotion p : filteredPromotions) promotionsFlowPane.getChildren().add(createCard(p));
        if (filteredPromotions.isEmpty()) {
            VBox empty = new VBox(15);
            empty.setAlignment(Pos.CENTER);
            empty.setPrefSize(600, 400);
            Text icon = new Text("🎁"); icon.setStyle("-fx-font-size: 64px;");
            Text msg  = new Text("Aucune promotion trouvée");
            msg.setStyle("-fx-font-size: 18px; -fx-fill: #7F8C8D; -fx-font-weight: bold;");
            empty.getChildren().addAll(icon, msg);
            promotionsFlowPane.getChildren().add(empty);
        }
    }

    // ═══════════════════════════════════════════════════
    // ⭐ CRÉATION DES CARTES — avec badge cadenas si verrouillée
    // ═══════════════════════════════════════════════════
    private VBox createCard(Promotion promo) {
        VBox card = new VBox();
        card.getStyleClass().add("activity-card");
        card.setPrefWidth(340);
        card.setMaxWidth(340);
        card.setCursor(javafx.scene.Cursor.HAND);

        // Header gradient
        StackPane header = new StackPane();
        header.setPrefHeight(170);
        String gradient = promo.isPack()
                ? "linear-gradient(to bottom right, #F39C12, #F7DC6F)"
                : "linear-gradient(to bottom right, #1ABC9C, #2C3E50)";
        header.setStyle("-fx-background-color: " + gradient + "; -fx-background-radius: 15 15 0 0;");

        Text icon = new Text(promo.isPack() ? "📦" : "🎁");
        icon.setStyle("-fx-font-size: 56px;");
        header.getChildren().add(icon);

        // Badge type
        Label typeBadge = new Label(promo.isPack() ? "PACK" : "PROMO");
        typeBadge.setStyle("-fx-background-color: white; -fx-text-fill: "
                + (promo.isPack() ? "#F39C12" : "#1ABC9C")
                + "; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 10; -fx-font-size: 11px;");
        StackPane.setAlignment(typeBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(typeBadge, new Insets(12));
        header.getChildren().add(typeBadge);

        // ⭐ Badge cadenas si verrouillée
        if (promo.isLocked()) {
            Label lockBadge = new Label("🔒 Code requis");
            lockBadge.setStyle("-fx-background-color: #2C3E50; -fx-text-fill: white; "
                    + "-fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 10; -fx-font-size: 11px;");
            StackPane.setAlignment(lockBadge, Pos.TOP_LEFT);
            StackPane.setMargin(lockBadge, new Insets(12));
            header.getChildren().add(lockBadge);
        }

        // Contenu
        VBox content = new VBox(10);
        content.setPadding(new Insets(18));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 15 15;");

        Text title = new Text(promo.getName());
        title.setStyle("-fx-font-size: 17px; -fx-fill: #2C3E50; -fx-font-weight: bold;");
        title.setWrappingWidth(300);

        Text desc = new Text(promo.getDescription());
        desc.setStyle("-fx-font-size: 12px; -fx-fill: #64748B;");
        desc.setWrappingWidth(300);

        // Réduction
        HBox discountBox = new HBox(8);
        discountBox.setAlignment(Pos.CENTER_LEFT);
        if (promo.getDiscountPercentage() != null) {
            Text d = new Text("-" + String.format("%.0f", promo.getDiscountPercentage()) + "%");
            d.setStyle("-fx-font-size: 22px; -fx-fill: #F39C12; -fx-font-weight: bold;");
            discountBox.getChildren().add(d);
        }
        if (promo.getDiscountFixed() != null) {
            Text d = new Text("-" + String.format("%.0f", promo.getDiscountFixed()) + " TND");
            d.setStyle("-fx-font-size: 18px; -fx-fill: #E67E22; -fx-font-weight: bold;");
            discountBox.getChildren().add(d);
        }

        // Dates
        HBox dates = new HBox(12);
        dates.setAlignment(Pos.CENTER_LEFT);
        VBox sb = new VBox(2);
        Text sl = new Text("Début"); sl.setStyle("-fx-font-size: 10px; -fx-fill: #7F8C8D;");
        Text sv = new Text(promo.getStartDate().toString()); sv.setStyle("-fx-font-size: 11px; -fx-fill: #2C3E50; -fx-font-weight: bold;");
        sb.getChildren().addAll(sl, sv);
        VBox eb = new VBox(2);
        Text el = new Text("Fin"); el.setStyle("-fx-font-size: 10px; -fx-fill: #7F8C8D;");
        Text ev = new Text(promo.getEndDate().toString()); ev.setStyle("-fx-font-size: 11px; -fx-fill: #2C3E50; -fx-font-weight: bold;");
        eb.getChildren().addAll(el, ev);
        dates.getChildren().addAll(sb, new Separator(javafx.geometry.Orientation.VERTICAL), eb);

        content.getChildren().addAll(title, desc, new Separator(), discountBox, dates);

        // Offres du pack
        if (promo.isPack()) {
            List<PromotionTarget> targets = targetService.getByPromotionId(promo.getId());
            if (!targets.isEmpty()) {
                VBox tb = new VBox(4);
                Label tl = new Label("Inclus :");
                tl.setStyle("-fx-font-size: 11px; -fx-text-fill: #7F8C8D; -fx-font-weight: bold;");
                tb.getChildren().add(tl);
                for (PromotionTarget t : targets) {
                    String em = switch (t.getTargetType()) {
                        case HEBERGEMENT -> "🏨"; case ACTIVITE -> "🎯"; case TRANSPORT -> "🚗";
                    };
                    Label tLabel = new Label(em + " " + t.getTargetType().name());
                    tLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #2C3E50;");
                    tb.getChildren().add(tLabel);
                }
                content.getChildren().add(tb);
            }
        }

        // ⭐ Bouton différent selon verrouillage
        Button btn;
        LocalDate today = LocalDate.now();
        boolean expired = promo.getEndDate().toLocalDate().isBefore(today);

        if (expired) {
            btn = new Button("Promotion expirée");
            btn.setDisable(true);
            btn.setStyle("-fx-background-color: #94A3B8; -fx-text-fill: white; -fx-font-weight: bold; "
                    + "-fx-padding: 11 18; -fx-background-radius: 8;");
        } else if (promo.isLocked()) {
            // ⭐ Promo verrouillée → bouton "Débloquer avec un code"
            btn = new Button("🔒  Débloquer avec un code promo");
            btn.setStyle("-fx-background-color: #2C3E50; -fx-text-fill: white; -fx-font-weight: bold; "
                    + "-fx-padding: 11 18; -fx-background-radius: 8; -fx-cursor: hand;");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> showPromoCodeDialog(promo));
        } else {
            btn = new Button("Réserver cette promotion  ➜");
            btn.setStyle("-fx-background-color: #1ABC9C; -fx-text-fill: white; -fx-font-weight: bold; "
                    + "-fx-padding: 11 18; -fx-background-radius: 8; -fx-cursor: hand;");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> openReservationDialog(promo));
        }
        btn.setMaxWidth(Double.MAX_VALUE);
        content.getChildren().add(btn);

        card.getChildren().addAll(header, content);
        return card;
    }

    // ═══════════════════════════════════════════════════
    // ⭐ DIALOG SAISIE DU CODE PROMO
    // ═══════════════════════════════════════════════════
    private void showPromoCodeDialog(Promotion promo) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Débloquer : " + promo.getName());
        dialog.setResizable(false);

        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: #eef2f6; -fx-padding: 30;");
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(400);

        // Icône + titre
        Label lockIcon = new Label("🔒");
        lockIcon.setStyle("-fx-font-size: 48px;");

        Label title = new Label("Promotion Verrouillée");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #2C3E50;");

        Label subtitle = new Label(promo.getName());
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748B;");

        Label info = new Label("Saisissez votre code promo pour accéder\nà cette promotion exclusive.");
        info.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B; -fx-text-alignment: center;");
        info.setAlignment(Pos.CENTER);

        // Champ saisie
        VBox inputBox = new VBox(8);
        inputBox.setAlignment(Pos.CENTER);
        Label inputLabel = new Label("CODE PROMO");
        inputLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #64748B;");
        TextField codeField = new TextField();
        codeField.setPromptText("Ex: RE7LA-A3F9-B2K1");
        codeField.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 12 15; "
                + "-fx-background-radius: 12; -fx-border-color: #E2E8F0; -fx-border-radius: 12; "
                + "-fx-text-fill: #2C3E50; -fx-font-family: monospace; -fx-alignment: CENTER;");
        codeField.setMaxWidth(280);
        // Auto-uppercase
        codeField.textProperty().addListener((obs, o, n) -> {
            if (!n.equals(n.toUpperCase())) codeField.setText(n.toUpperCase());
        });
        inputBox.getChildren().addAll(inputLabel, codeField);

        // Message d'erreur (caché par défaut)
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #E53E3E; -fx-font-weight: 600;");
        errorLabel.setVisible(false);

        // Boutons
        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER);

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #2C3E50; -fx-font-weight: 600; "
                + "-fx-padding: 10 24; -fx-background-radius: 20; -fx-cursor: hand;");
        btnAnnuler.setOnAction(e -> dialog.close());

        Button btnValider = new Button("✓  Valider le code");
        btnValider.setStyle("-fx-background-color: #F39C12; -fx-text-fill: white; -fx-font-weight: 700; "
                + "-fx-padding: 10 24; -fx-background-radius: 20; -fx-cursor: hand;");

        // ⭐ Validation via PromoCodeService
        btnValider.setOnAction(e -> {
            String saisie = codeField.getText().trim();
            if (saisie.isEmpty()) {
                errorLabel.setText("⚠ Veuillez saisir un code.");
                errorLabel.setVisible(true);
                return;
            }
            boolean valid = promoCodeService.validateCode(saisie, promo.getId());
            if (valid) {
                // Code valide → ouvrir la réservation
                dialog.close();
                openReservationDialog(promo);
                // Le code sera marqué comme utilisé dans openReservationDialog après confirmation
            } else {
                errorLabel.setText("❌ Code invalide ou déjà utilisé.");
                errorLabel.setVisible(true);
                codeField.setStyle(codeField.getStyle()
                        + "-fx-border-color: #E53E3E; -fx-border-width: 2;");
            }
        });

        // Valider aussi avec Enter
        codeField.setOnAction(e -> btnValider.fire());

        buttons.getChildren().addAll(btnAnnuler, btnValider);

        root.getChildren().addAll(lockIcon, title, subtitle, info, inputBox, errorLabel, buttons);

        dialog.setScene(new Scene(root));
        dialog.show();
    }

    private void openReservationDialog(Promotion promo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/ReservationDialog.fxml"));
            Parent root = loader.load();
            ReservationDialogController ctrl = loader.getController();
            ctrl.setPromotion(promo);
            Stage stage = new Stage();
            stage.setTitle("Réserver : " + promo.getName());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setContentText("Impossible d'ouvrir le formulaire de réservation.");
            a.showAndWait();
        }
    }

    private void updateResultsCount() {
        int c = filteredPromotions.size();
        resultsCountText.setText(c + " promotion" + (c > 1 ? "s" : "") + " trouvée" + (c > 1 ? "s" : ""));
    }

    @FXML private void resetFilters() {
        searchField.clear();
        packCheckbox.setSelected(true);
        individuCheckbox.setSelected(true);
        activeCheckbox.setSelected(true);
        sortCombo.setValue("Plus récentes");
        applyFilters();
    }

    @FXML private void handleMesReservations() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/frontoffice/MesReservations.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Mes Réservations");
            stage.setScene(new Scene(root, 1000, 700));
            stage.show();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir Mes Réservations.").showAndWait();
        }
    }
}
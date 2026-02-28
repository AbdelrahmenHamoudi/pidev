package org.example.controllers.backoffice;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.models.*;
import org.example.services.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class PromotionBackOfficeController implements Initializable {

    // ── Sidebar buttons ──
    @FXML private Button btnGestionUtilisateurs, btnComptesBancaires,
            btnTransactions, btnCredits, btnCashback,
            btnStatistiques, btnParametres, btnDeconnexion;

    // ── Stats labels ──
    @FXML private Label statTotalPromo, statPacksPromo, statActivesPromo;

    // ── Views ──
    @FXML private StackPane mainContentStack;
    @FXML private ScrollPane viewPromotion;
    @FXML private VBox viewEmpty;

    // ── Form ──
    @FXML private TextField txtNom, txtPourcentage, txtFixe, txtPrixParJour;
    @FXML private TextArea  txtDescription;
    @FXML private DatePicker dateDebut, dateFin;
    @FXML private CheckBox chkPack, chkLocked;
    @FXML private Button btnAjouter, btnSupprimer;

    // ── Offres ──
    @FXML private ComboBox<String> comboTypeOffre;
    @FXML private TableView<Object> tableOffres;
    @FXML private TableColumn<Object, Integer> colOffreId;
    @FXML private TableColumn<Object, String>  colOffreNom, colOffreDetails;

    // ── Liste promos (FlowPane de cards) ──
    @FXML private FlowPane promoCardsPane;
    @FXML private TextField searchPromoField;

    // ── Services ──
    private PromotionService      promotionService;
    private PromotionTargetService targetService;
    private OffresStaticData      offresData;
    private PromoCodeService      promoCodeService;
    private PromotionPdfService   pdfService;

    private ObservableList<Promotion> allPromos = FXCollections.observableArrayList();
    private Promotion selectedPromotion = null;

    // ── Couleurs (inline JavaFX) ──
    private static final String ORANGE    = "#F39C12";
    private static final String JAUNE     = "#F7DC6F";
    private static final String BLEU_NUIT = "#2C3E50";
    private static final String TURQUOISE = "#1ABC9C";
    private static final String BG_LIGHT  = "#eef2f6";
    private static final String WHITE     = "#ffffff";
    private static final String GRAY      = "#64748B";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        promotionService = new PromotionService();
        targetService    = new PromotionTargetService();
        offresData       = OffresStaticData.getInstance();
        promoCodeService = new PromoCodeService();
        pdfService       = new PromotionPdfService();

        setupOffresTable();
        setupComboBox();
        loadAndDisplayPromos();
        showView(viewPromotion);

        // Recherche live
        if (searchPromoField != null)
            searchPromoField.textProperty().addListener((obs, o, n) -> filterCards(n));
    }

    // ════════════════════════════════════════════════════
    // CHARGEMENT + AFFICHAGE DES CARDS
    // ════════════════════════════════════════════════════

    private void loadAndDisplayPromos() {
        allPromos.clear();
        allPromos.addAll(promotionService.getAll());
        buildCards(allPromos);
        updateStats();
    }

    private void filterCards(String keyword) {
        if (keyword == null || keyword.isBlank()) { buildCards(allPromos); return; }
        String kw = keyword.toLowerCase();
        ObservableList<Promotion> filtered = allPromos.filtered(p ->
                p.getName().toLowerCase().contains(kw) ||
                        p.getDescription().toLowerCase().contains(kw));
        buildCards(filtered);
    }

    private void buildCards(Iterable<Promotion> promos) {
        if (promoCardsPane == null) return;
        promoCardsPane.getChildren().clear();
        for (Promotion p : promos) {
            VBox card = createPromoCard(p);
            promoCardsPane.getChildren().add(card);
        }
        if (!promoCardsPane.getChildren().iterator().hasNext()) {
            Label empty = new Label("Aucune promotion trouvée");
            empty.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");
            promoCardsPane.getChildren().add(empty);
        }
    }

    /**
     * Crée une card visuelle pour une promotion — le cœur du design.
     */
    private VBox createPromoCard(Promotion promo) {
        VBox card = new VBox(0);
        card.setPrefWidth(310);
        card.setMaxWidth(310);
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);" +
                        "-fx-border-color: #E2E8F0;" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1;"
        );

        // ── Hover effect ──
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(243,156,18,0.25), 18, 0, 0, 5);" +
                        "-fx-border-color: " + ORANGE + ";" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 2;" +
                        "-fx-translate-y: -3;"
        ));
        card.setOnMouseExited(e -> {
            boolean sel = selectedPromotion != null && selectedPromotion.getId() == promo.getId();
            card.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 16;" +
                            "-fx-effect: dropshadow(gaussian," + (sel ? "rgba(243,156,18,0.3)" : "rgba(0,0,0,0.08)") + ", 12, 0, 0, 3);" +
                            "-fx-border-color: " + (sel ? ORANGE : "#E2E8F0") + ";" +
                            "-fx-border-radius: 16;" +
                            "-fx-border-width: " + (sel ? "2" : "1") + ";"
            );
        });
        card.setOnMouseClicked(e -> selectCard(promo, card));

        // ── HEADER DÉGRADÉ avec photo auto ──
        StackPane header = new StackPane();
        header.setPrefHeight(160);
        header.setStyle(buildGradient(promo) + " -fx-background-radius: 16 16 0 0;");

        // Grande icône centrale
        Label bigIcon = new Label(getPromoIcon(promo));
        bigIcon.setStyle("-fx-font-size: 56px;");

        // Badge type (top-right)
        Label typeBadge = new Label(promo.isPack() ? "📦 PACK" : "🎁 PROMO");
        typeBadge.setStyle(
                "-fx-background-color: rgba(255,255,255,0.92);" +
                        "-fx-text-fill: " + (promo.isPack() ? ORANGE : TURQUOISE) + ";" +
                        "-fx-font-weight: bold; -fx-font-size: 10px;" +
                        "-fx-padding: 4 10; -fx-background-radius: 20;"
        );
        StackPane.setAlignment(typeBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(typeBadge, new Insets(10));

        header.getChildren().addAll(bigIcon, typeBadge);

        // Badge cadenas (top-left, si verrouillée)
        if (promo.isLocked()) {
            Label lockBadge = new Label("🔒 Verrouillée");
            lockBadge.setStyle(
                    "-fx-background-color: " + BLEU_NUIT + ";" +
                            "-fx-text-fill: white; -fx-font-weight: bold;" +
                            "-fx-font-size: 10px; -fx-padding: 4 10; -fx-background-radius: 20;"
            );
            StackPane.setAlignment(lockBadge, Pos.TOP_LEFT);
            StackPane.setMargin(lockBadge, new Insets(10));
            header.getChildren().add(lockBadge);
        }

        // Badge actif/expiré (bottom-left)
        boolean active = promo.isActive();
        Label statusBadge = new Label(active ? "✅ Active" : "⛔ Expirée");
        statusBadge.setStyle(
                "-fx-background-color: " + (active ? TURQUOISE : "#94A3B8") + ";" +
                        "-fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-font-size: 9px; -fx-padding: 3 9; -fx-background-radius: 10;"
        );
        StackPane.setAlignment(statusBadge, Pos.BOTTOM_LEFT);
        StackPane.setMargin(statusBadge, new Insets(10));
        header.getChildren().add(statusBadge);

        // ── CONTENT ──
        VBox content = new VBox(8);
        content.setPadding(new Insets(14, 16, 10, 16));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 16 16;");

        // Nom
        Label nameLabel = new Label(promo.getName());
        nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: " + BLEU_NUIT + ";");
        nameLabel.setWrapText(true);

        // Description (tronquée)
        String desc = promo.getDescription();
        if (desc != null && desc.length() > 60) desc = desc.substring(0, 60) + "…";
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + GRAY + ";");
        descLabel.setWrapText(true);

        // Réduction + prix
        HBox priceRow = new HBox(8);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        if (promo.getDiscountPercentage() != null) {
            Label discPct = new Label("-" + String.format("%.0f", promo.getDiscountPercentage()) + "%");
            discPct.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: " + ORANGE + ";");
            priceRow.getChildren().add(discPct);
        }
        if (promo.getDiscountFixed() != null) {
            Label discFix = new Label("-" + String.format("%.0f", promo.getDiscountFixed()) + " TND");
            discFix.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #E67E22;");
            priceRow.getChildren().add(discFix);
        }
        Label ppj = new Label(String.format("%.0f TND/j", promo.getPrixParJour() != null ? promo.getPrixParJour() : 50f));
        ppj.setStyle("-fx-font-size: 11px; -fx-text-fill: " + GRAY + "; -fx-padding: 0 0 0 6;");
        priceRow.getChildren().add(ppj);

        // Dates
        HBox datesRow = new HBox(6);
        datesRow.setAlignment(Pos.CENTER_LEFT);
        Label calIcon = new Label("📅");
        calIcon.setStyle("-fx-font-size: 11px;");
        Label dates = new Label(promo.getStartDate() + "  →  " + promo.getEndDate());
        dates.setStyle("-fx-font-size: 10px; -fx-text-fill: " + GRAY + ";");
        datesRow.getChildren().addAll(calIcon, dates);

        // Stats vues + réservations
        HBox statsRow = new HBox(12);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.setStyle(
                "-fx-background-color: " + BG_LIGHT + ";" +
                        "-fx-padding: 6 10; -fx-background-radius: 8;"
        );
        Label vuesLbl = new Label("👁  " + promo.getNbVues() + " vues");
        vuesLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: " + BLEU_NUIT + ";");
        Label resaLbl = new Label("📋  " + promo.getNbReservations() + " réserv.");
        resaLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: " + TURQUOISE + ";");
        statsRow.getChildren().addAll(vuesLbl, resaLbl);

        // Séparateur
        Separator sep = new Separator();
        sep.setStyle("-fx-border-color: #F1F5F9;");

        // ── 3 BOUTONS CARRÉS ──
        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER);

        Button btnQR      = buildSquareBtn("🔒", "QR Code",  BLEU_NUIT, "#3D5166");
        Button btnDetails = buildSquareBtn("📋", "Détails",  ORANGE,    "#E67E22");
        Button btnDelete  = buildSquareBtn("🗑️", "Suppr.",  "#E53E3E",  "#C53030");

        // Actions boutons
        btnQR.setOnAction(e -> { e.consume(); handleQRCode(promo); });
        btnDetails.setOnAction(e -> { e.consume(); handleDetails(promo); });
        btnDelete.setOnAction(e -> { e.consume(); handleDeleteFromCard(promo); });

        // Editer en cliquant sur la card
        card.setOnMouseClicked(e -> selectCard(promo, card));

        btnRow.getChildren().addAll(btnQR, btnDetails, btnDelete);
        content.getChildren().addAll(nameLabel, descLabel, priceRow, datesRow, statsRow, sep, btnRow);
        card.getChildren().addAll(header, content);

        // Fade in
        FadeTransition ft = new FadeTransition(Duration.millis(350), card);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        return card;
    }

    // ── Sélection d'une card ──
    private void selectCard(Promotion promo, VBox card) {
        selectedPromotion = promo;
        fillForm(promo);
        // Met en évidence visuellement
        if (promoCardsPane != null) {
            promoCardsPane.getChildren().forEach(n -> {
                if (n instanceof VBox v) v.setStyle(v.getStyle()
                        .replace("-fx-border-color: " + ORANGE, "-fx-border-color: #E2E8F0")
                        .replace("-fx-border-width: 2", "-fx-border-width: 1"));
            });
        }
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(243,156,18,0.35), 16, 0, 0, 4);" +
                        "-fx-border-color: " + ORANGE + ";" +
                        "-fx-border-radius: 16; -fx-border-width: 2;"
        );
    }

    // ── Bouton carré stylisé ──
    private Button buildSquareBtn(String icon, String label, String bgColor, String hoverColor) {
        Button btn = new Button(icon + "\n" + label);
        btn.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: 700;" +
                        "-fx-padding: 10 14; -fx-background-radius: 10; -fx-cursor: hand;" +
                        "-fx-alignment: CENTER; -fx-pref-width: 80; -fx-pref-height: 55;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + hoverColor + ";" +
                        "-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: 700;" +
                        "-fx-padding: 10 14; -fx-background-radius: 10; -fx-cursor: hand;" +
                        "-fx-alignment: CENTER; -fx-pref-width: 80; -fx-pref-height: 55;" +
                        "-fx-translate-y: -2;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: 700;" +
                        "-fx-padding: 10 14; -fx-background-radius: 10; -fx-cursor: hand;" +
                        "-fx-alignment: CENTER; -fx-pref-width: 80; -fx-pref-height: 55;"
        ));
        return btn;
    }

    // ── Gradient selon type ──
    private String buildGradient(Promotion promo) {
        if (promo.isPack())
            return "-fx-background-color: linear-gradient(to bottom right, #F39C12, #F7DC6F);";
        if (promo.isLocked())
            return "-fx-background-color: linear-gradient(to bottom right, #2C3E50, #4A6278);";
        return "-fx-background-color: linear-gradient(to bottom right, #1ABC9C, #2C3E50);";
    }

    // ── Icône selon type ──
    private String getPromoIcon(Promotion promo) {
        if (promo.isPack())   return "📦";
        if (promo.isLocked()) return "🔒";
        return "🎁";
    }

    // ════════════════════════════════════════════════════
    // ACTION QR CODE — Génère + affiche
    // ════════════════════════════════════════════════════
    private void handleQRCode(Promotion promo) {
        if (!promo.isLocked()) {
            // Proposer de verrouiller
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Verrouiller la promotion ?");
            confirm.setHeaderText("\"" + promo.getName() + "\" n'est pas verrouillée.");
            confirm.setContentText("Voulez-vous la verrouiller et générer un code QR ?");
            Optional<ButtonType> r = confirm.showAndWait();
            if (r.isEmpty() || r.get() != ButtonType.OK) return;

            promo.setLocked(true);
            promotionService.update(promo);
            loadAndDisplayPromos();
        }

        // Créer le code en BD
        PromoCode code = promoCodeService.createPromoCode(promo.getId());
        if (code == null) { showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de générer le code."); return; }

        // Générer image QR
        Image qrImg = promoCodeService.generateQrCodeImage(code.getQrContent(), 280);

        // Afficher dialog
        showQrDialog(promo, code, qrImg);
    }

    private void showQrDialog(Promotion promo, PromoCode code, Image qrImg) {
        Stage d = new Stage();
        d.initModality(Modality.APPLICATION_MODAL);
        d.setTitle("QR Code — " + promo.getName());

        VBox root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: " + BG_LIGHT + "; -fx-padding: 30;");

        Label title = new Label("🔒  Code Promo Généré");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: " + BLEU_NUIT + ";");

        Label sub = new Label(promo.getName());
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: " + GRAY + ";");

        ImageView qrView = new ImageView(qrImg);
        qrView.setFitWidth(260); qrView.setFitHeight(260);
        qrView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4);");

        // Code texte
        VBox codeBox = new VBox(5);
        codeBox.setAlignment(Pos.CENTER);
        codeBox.setStyle("-fx-background-color: white; -fx-padding: 14 24;" +
                "-fx-background-radius: 12; -fx-border-color: " + JAUNE + ";" +
                "-fx-border-radius: 12; -fx-border-width: 2;");
        Label codeLbl = new Label("Code à partager avec le client :");
        codeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + GRAY + ";");
        Label codeVal = new Label(code.getCode());
        codeVal.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: " + ORANGE +
                "; -fx-font-family: monospace;");
        codeBox.getChildren().addAll(codeLbl, codeVal);

        Button btnClose = new Button("✓  Fermer");
        btnClose.setStyle("-fx-background-color: " + ORANGE + "; -fx-text-fill: white;" +
                "-fx-font-weight: 700; -fx-padding: 10 30; -fx-background-radius: 20; -fx-cursor: hand;");
        btnClose.setOnAction(e -> d.close());

        root.getChildren().addAll(title, sub, qrView, codeBox, btnClose);
        d.setScene(new Scene(root, 400, 580));
        d.show();
    }

    // ════════════════════════════════════════════════════
    // ACTION DÉTAILS
    // ════════════════════════════════════════════════════
    private void handleDetails(Promotion promo) {
        promotionService.incrementVues(promo.getId());
        promo.setNbVues(promo.getNbVues() + 1);

        Stage d = new Stage();
        d.initModality(Modality.APPLICATION_MODAL);
        d.setTitle("Détails — " + promo.getName());

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: " + BLEU_NUIT + "; -fx-padding: 20 25;");
        Label hIcon = new Label(getPromoIcon(promo));
        hIcon.setStyle("-fx-font-size: 32px;");
        VBox hText = new VBox(3);
        Label hName = new Label(promo.getName());
        hName.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: white;");
        Label hSub = new Label("ID #" + promo.getId() + "  ·  " + (promo.isPack() ? "Pack" : "Individuel"));
        hSub.setStyle("-fx-font-size: 12px; -fx-text-fill: " + JAUNE + ";");
        hText.getChildren().addAll(hName, hSub);
        header.getChildren().addAll(hIcon, hText);

        // Content
        VBox content = new VBox(16);
        content.setStyle("-fx-background-color: " + BG_LIGHT + "; -fx-padding: 22;");

        // Grid info
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setStyle("-fx-background-color: white; -fx-padding: 18; -fx-background-radius: 12;");
        addDetailRow(grid, 0, "📝 Nom", promo.getName());
        addDetailRow(grid, 1, "📋 Description", promo.getDescription());
        addDetailRow(grid, 2, "💰 Réduction",
                (promo.getDiscountPercentage()!=null ? promo.getDiscountPercentage()+"%" : "—") + "  /  " +
                        (promo.getDiscountFixed()!=null ? promo.getDiscountFixed()+" TND fixe" : "—"));
        addDetailRow(grid, 3, "💵 Prix / Jour", (promo.getPrixParJour()!=null ? promo.getPrixParJour() : 50f) + " TND");
        addDetailRow(grid, 4, "📅 Période", promo.getStartDate() + "  →  " + promo.getEndDate());
        addDetailRow(grid, 5, "🏷️ Type", promo.isPack() ? "Pack Combiné" : "Individuelle");
        addDetailRow(grid, 6, "🔒 Verrouillée", promo.isLocked() ? "Oui" : "Non");
        addDetailRow(grid, 7, "📊 Statut", promo.isActive() ? "✅ Active" : "⛔ Expirée");

        // Stats cards
        HBox statsBox = new HBox(12);
        statsBox.getChildren().addAll(
                buildStatCard("👁", String.valueOf(promo.getNbVues()), "Vues", JAUNE),
                buildStatCard("📋", String.valueOf(promo.getNbReservations()), "Réservations", TURQUOISE)
        );

        content.getChildren().addAll(grid, statsBox);

        // Bouton fermer
        Button btnClose = new Button("✓  Fermer");
        btnClose.setStyle("-fx-background-color: " + ORANGE + "; -fx-text-fill: white;" +
                "-fx-font-weight: 700; -fx-padding: 10 30; -fx-background-radius: 20; -fx-cursor: hand;");
        btnClose.setOnAction(e -> { d.close(); loadAndDisplayPromos(); });

        VBox root = new VBox(0);
        root.getChildren().addAll(header, content, btnClose);
        VBox.setMargin(btnClose, new Insets(0, 22, 18, 22));

        d.setScene(new Scene(root, 520, 620));
        d.show();
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + GRAY + ";");
        Label v = new Label(value != null ? value : "—");
        v.setStyle("-fx-font-size: 12px; -fx-text-fill: " + BLEU_NUIT + "; -fx-wrap-text: true;");
        v.setMaxWidth(280);
        grid.add(l, 0, row); grid.add(v, 1, row);
    }

    private HBox buildStatCard(String icon, String value, String label, String color) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-padding: 16 30;" +
                "-fx-background-radius: 12; -fx-border-color: " + color +
                "; -fx-border-radius: 12; -fx-border-width: 2;");
        Label ic = new Label(icon); ic.setStyle("-fx-font-size: 20px;");
        Label va = new Label(value); va.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: " + BLEU_NUIT + ";");
        Label la = new Label(label); la.setStyle("-fx-font-size: 11px; -fx-text-fill: " + GRAY + ";");
        card.getChildren().addAll(ic, va, la);
        HBox wrapper = new HBox(card); wrapper.setHgrow(card, Priority.ALWAYS); card.setMaxWidth(Double.MAX_VALUE);
        return wrapper;
    }

    // ════════════════════════════════════════════════════
    // ACTION DELETE depuis card
    // ════════════════════════════════════════════════════
    private void handleDeleteFromCard(Promotion promo) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Supprimer \"" + promo.getName() + "\" ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                targetService.deleteByPromotionId(promo.getId());
                promotionService.delete(promo.getId());
                if (selectedPromotion != null && selectedPromotion.getId() == promo.getId()) {
                    selectedPromotion = null; handleCancel();
                }
                loadAndDisplayPromos();
                showAlert(Alert.AlertType.INFORMATION, "Supprimé", "✅ Promotion supprimée !");
            }
        });
    }

    // ════════════════════════════════════════════════════
    // PDF — avec choix de langue
    // ════════════════════════════════════════════════════
    @FXML
    private void handleExportPDF() {
        if (selectedPromotion == null) {
            showAlert(Alert.AlertType.WARNING, "Aucune sélection",
                    "Cliquez d'abord sur une promotion dans la liste, puis sur PDF.");
            return;
        }

        // Dialog choix de langue
        ChoiceDialog<String> langDialog = new ChoiceDialog<>("Français", "Français", "English");
        langDialog.setTitle("Langue du PDF");
        langDialog.setHeaderText("Choisissez la langue du document PDF");
        langDialog.setContentText("Langue :");
        Optional<String> langResult = langDialog.showAndWait();
        if (langResult.isEmpty()) return;
        String lang = langResult.get().equals("English") ? "en" : "fr";

        // Récupérer le code promo si verrouillée
        String promoCode = null;
        if (selectedPromotion.isLocked()) {
            PromoCode pc = promoCodeService.getByCode(selectedPromotion.getId());
            if (pc != null) promoCode = pc.getCode();
        }

        // Sélecteur de fichier
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le PDF");
        fc.setInitialFileName("promo_" + selectedPromotion.getId() + "_" + selectedPromotion.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showSaveDialog(null);
        if (file == null) return;

        final String finalPromoCode = promoCode;
        final String finalLang = lang;

        // Génération dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                pdfService.generatePdf(selectedPromotion, finalPromoCode, finalLang, file.getAbsolutePath());
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.INFORMATION, "PDF Généré",
                                "✅ PDF enregistré :\n" + file.getAbsolutePath()));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Erreur PDF",
                                "Impossible de générer le PDF :\n" + e.getMessage()));
            }
        }).start();
    }

    // ════════════════════════════════════════════════════
    // FORM : CRUD
    // ════════════════════════════════════════════════════
    private void fillForm(Promotion promo) {
        txtNom.setText(promo.getName());
        txtDescription.setText(promo.getDescription());
        txtPourcentage.setText(promo.getDiscountPercentage()!=null ? promo.getDiscountPercentage().toString() : "");
        txtFixe.setText(promo.getDiscountFixed()!=null ? promo.getDiscountFixed().toString() : "");
        txtPrixParJour.setText(promo.getPrixParJour()!=null ? promo.getPrixParJour().toString() : "50");
        dateDebut.setValue(promo.getStartDate().toLocalDate());
        dateFin.setValue(promo.getEndDate().toLocalDate());
        chkPack.setSelected(promo.isPack());
        if (chkLocked != null) chkLocked.setSelected(promo.isLocked());
        btnAjouter.setText("💾  Modifier");
    }

    @FXML
    private void addOrUpdate() {
        try {
            String nom = txtNom.getText().trim();
            String desc = txtDescription.getText().trim();
            if (nom.isEmpty() || desc.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Champs requis", "Nom et description obligatoires."); return; }

            Float pct = null, fix = null;
            if (!txtPourcentage.getText().trim().isEmpty()) pct = Float.parseFloat(txtPourcentage.getText().trim());
            if (!txtFixe.getText().trim().isEmpty()) fix = Float.parseFloat(txtFixe.getText().trim());
            if (pct == null && fix == null) { showAlert(Alert.AlertType.WARNING, "Réduction", "Spécifiez au moins une réduction."); return; }

            if (txtPrixParJour.getText().trim().isEmpty()) { showAlert(Alert.AlertType.WARNING, "Prix requis", "Le prix par jour est obligatoire."); return; }
            Float ppj = Float.parseFloat(txtPrixParJour.getText().trim());

            LocalDate debut = dateDebut.getValue(), fin = dateFin.getValue();
            if (debut == null || fin == null) { showAlert(Alert.AlertType.WARNING, "Dates", "Les deux dates sont obligatoires."); return; }
            if (!fin.isAfter(debut)) { showAlert(Alert.AlertType.WARNING, "Dates", "La date de fin doit être après le début."); return; }

            boolean isPack  = chkPack.isSelected();
            boolean isLocked = chkLocked != null && chkLocked.isSelected();

            if (selectedPromotion == null) {
                Promotion newP = new Promotion(nom, desc, pct, fix, Date.valueOf(debut), Date.valueOf(fin), isPack, ppj);
                newP.setLocked(isLocked);
                Promotion saved = promotionService.add(newP);
                if (saved != null) {
                    if (isLocked) { selectedPromotion = saved; handleQRCode(saved); }
                    else showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Promotion créée !");
                } else { showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de créer."); return; }
            } else {
                selectedPromotion.setName(nom); selectedPromotion.setDescription(desc);
                selectedPromotion.setDiscountPercentage(pct); selectedPromotion.setDiscountFixed(fix);
                selectedPromotion.setStartDate(Date.valueOf(debut)); selectedPromotion.setEndDate(Date.valueOf(fin));
                selectedPromotion.setPack(isPack); selectedPromotion.setPrixParJour(ppj);
                selectedPromotion.setLocked(isLocked);
                boolean ok = promotionService.update(selectedPromotion);
                if (ok) showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Promotion modifiée !");
                else { showAlert(Alert.AlertType.ERROR, "Erreur", "Modification impossible."); return; }
            }

            loadAndDisplayPromos();
            handleCancel();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Format", "Valeurs numériques incorrectes.");
        }
    }

    @FXML private void deletePromotion() {
        if (selectedPromotion == null) { showAlert(Alert.AlertType.WARNING, "Aucune sélection", "Sélectionnez une promotion d'abord."); return; }
        handleDeleteFromCard(selectedPromotion);
    }

    @FXML private void handleCancel() {
        txtNom.clear(); txtDescription.clear(); txtPourcentage.clear(); txtFixe.clear(); txtPrixParJour.clear();
        dateDebut.setValue(null); dateFin.setValue(null);
        chkPack.setSelected(false); if (chkLocked != null) chkLocked.setSelected(false);
        selectedPromotion = null;
        btnAjouter.setText("💾  Enregistrer la promotion");
    }

    @FXML private void ajouterOffreAPromotion() {
        if (selectedPromotion == null) { showAlert(Alert.AlertType.WARNING, "Aucune promo", "Sélectionnez une promotion."); return; }
        Object sel = tableOffres.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert(Alert.AlertType.WARNING, "Aucune offre", "Sélectionnez une offre."); return; }
        TargetType tt = null; int tid = 0;
        if (sel instanceof Hebergement h) { tt = TargetType.HEBERGEMENT; tid = h.getId(); }
        else if (sel instanceof Activite a) { tt = TargetType.ACTIVITE; tid = a.getId(); }
        else if (sel instanceof Transport t) { tt = TargetType.TRANSPORT; tid = t.getId(); }
        if (tt != null) {
            targetService.add(new PromotionTarget(selectedPromotion.getId(), tt, tid));
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Offre ajoutée !");
        }
    }

    // ════════════════════════════════════════════════════
    // STATS + SIDEBAR
    // ════════════════════════════════════════════════════
    private void updateStats() {
        int total = allPromos.size();
        long packs = allPromos.stream().filter(Promotion::isPack).count();
        LocalDate today = LocalDate.now();
        long actives = allPromos.stream().filter(p ->
                !today.isBefore(p.getStartDate().toLocalDate()) && !today.isAfter(p.getEndDate().toLocalDate())
        ).count();
        if (statTotalPromo  != null) statTotalPromo.setText(String.valueOf(total));
        if (statPacksPromo  != null) statPacksPromo.setText(String.valueOf(packs));
        if (statActivesPromo!= null) statActivesPromo.setText(String.valueOf(actives));
    }

    @FXML private void handleShowStatistiques() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/backoffice/StatistiquesReservations.fxml"));
            Stage s = new Stage(); s.setTitle("Statistiques");
            s.setScene(new Scene(root, 1400, 900)); s.show();
        } catch (IOException e) { showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
    }

    // ── Offres table setup ──
    private void setupOffresTable() {
        if (colOffreId == null) return;
        colOffreId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOffreNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colOffreDetails.setCellValueFactory(c -> {
            Object item = c.getValue();
            if (item instanceof Hebergement h) return new javafx.beans.property.SimpleStringProperty(h.getVille());
            if (item instanceof Activite a)    return new javafx.beans.property.SimpleStringProperty(a.getLieu());
            if (item instanceof Transport t)   return new javafx.beans.property.SimpleStringProperty(t.getTrajet());
            return new javafx.beans.property.SimpleStringProperty("");
        });
    }

    private void setupComboBox() {
        if (comboTypeOffre == null) return;
        comboTypeOffre.setItems(FXCollections.observableArrayList("Hébergement", "Activité", "Transport"));
        comboTypeOffre.setOnAction(e -> {
            String type = comboTypeOffre.getValue();
            if (type == null) return;
            ObservableList<Object> offres = FXCollections.observableArrayList();
            switch (type) {
                case "Hébergement" -> offres.addAll(offresData.getAllHebergements());
                case "Activité"    -> offres.addAll(offresData.getAllActivites());
                case "Transport"   -> offres.addAll(offresData.getAllTransports());
            }
            tableOffres.setItems(offres);
        });
    }

    private void showView(javafx.scene.Node v) {
        viewPromotion.setVisible(false); viewEmpty.setVisible(false); v.setVisible(true);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(content); a.showAndWait();
    }

    @FXML private void handleShowUsers()        { showView(viewEmpty); }
    @FXML private void handleShowAccounts()     { showView(viewEmpty); }
    @FXML private void handleShowTransactions() { showView(viewEmpty); }
    @FXML private void handleShowCredits()      { showView(viewEmpty); }
    @FXML private void handleShowCashback()     { showView(viewPromotion); }
    @FXML private void handleShowSettings()     { showView(viewEmpty); }
    @FXML private void handleLogout()           { showAlert(Alert.AlertType.INFORMATION, "Déconnexion", "À implémenter."); }
}
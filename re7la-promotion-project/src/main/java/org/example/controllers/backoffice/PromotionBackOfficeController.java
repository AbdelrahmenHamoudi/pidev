package org.example.controllers.backoffice;

import animatefx.animation.*;
import javafx.animation.*;
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
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.models.*;
import org.example.services.*;
import org.example.utils.AnimationHelper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
/**
 * ✅ FIXED:
 *  - OffresStaticData → OffresService (real DB)
 *  - Transport → Voiture (aligned with voiture table)
 *  - TargetType.TRANSPORT → TargetType.VOITURE
 *  - Removed prixParJour from form and Promotion creation
 *  - ComboBox now shows "Hébergement / Activité / Voiture"
 *  - Table columns aligned with new models (getNom(), getDetails())
 */
public class PromotionBackOfficeController implements Initializable {

    @FXML private Button btnGestionUtilisateurs, btnComptesBancaires,
            btnTransactions, btnCredits, btnCashback,
            btnStatistiques, btnParametres, btnDeconnexion;

    // ── KPI Stats ──
    @FXML private Label statTotalPromo, statPacksPromo, statActivesPromo;
    @FXML private HBox  statsHBox;

    // ── Views ──
    @FXML private StackPane mainContentStack;
    @FXML private ScrollPane viewPromotion;
    @FXML private VBox viewEmpty;
    @FXML private ScrollPane viewAiPacks;   // ✅ AI Pack Generator view

    // ── AI Pack Generator fields ──
    @FXML private GridPane aiPacksGrid;
    @FXML private Label    aiStatusLabel;
    @FXML private Label    statAiTotal;
    @FXML private Label    statAiActive;
    @FXML private Button   btnAnalyse;

    // ── Formulaire ──
    @FXML private TextField  txtNom, txtPourcentage, txtFixe;
    @FXML private TextArea   txtDescription;
    @FXML private DatePicker dateDebut, dateFin;
    @FXML private CheckBox   chkPack, chkLocked;
    @FXML private Button     btnAjouter, btnSupprimer;

    // ── Offres ──
    @FXML private ComboBox<String>       comboTypeOffre;
    @FXML private TableView<Object>      tableOffres;
    @FXML private TableColumn<Object, Integer> colOffreId;
    @FXML private TableColumn<Object, String>  colOffreNom, colOffreDetails;
    @FXML private VBox                   stagedOffresBox;  // shows staged offers before save

    // ── Staged offers (for NEW promotion pack creation before save) ──
    private final List<PromotionTarget> stagedOffers = new ArrayList<>();

    // ── Cards ──
    @FXML private GridPane promoCardsGrid;
    @FXML private TextField searchPromoField;

    private static final int COLS = 3;

    // ── Services ──
    private PromotionService       promotionService;
    private PromotionTargetService targetService;
    private Offresservice offresService;
    private PromoCodeService       promoCodeService;
    private PromotionPdfService    pdfService;
    private NotificationService    notifService;
    private SchedulerService       schedulerService;
    private TrendingService        trendingService;
    private SmartDiscountEngine    discountEngine;
    private PackSuggestionService  suggestionService;  // ✅ AI service

    private ObservableList<Promotion> allPromos = FXCollections.observableArrayList();
    private Promotion selectedPromotion = null;

    // ── Palette ──
    private static final String ORANGE    = "#F39C12";
    private static final String JAUNE     = "#F7DC6F";
    private static final String BLEU_NUIT = "#2C3E50";
    private static final String TURQUOISE = "#1ABC9C";
    private static final String BG_LIGHT  = "#eef2f6";
    private static final String WHITE     = "#ffffff";
    private static final String GRAY      = "#64748B";
    private static final String ROUGE     = "#E53E3E";

    // ════════════════════════════════════════════════════
    // INIT
    // ════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        promotionService = new PromotionService();
        targetService    = new PromotionTargetService();
        offresService    = Offresservice.getInstance();  // ✅ FIXED
        promoCodeService = new PromoCodeService();
        pdfService       = new PromotionPdfService();
        notifService     = NotificationService.getInstance();
        schedulerService = SchedulerService.getInstance();
        trendingService  = TrendingService.getInstance();
        discountEngine   = SmartDiscountEngine.getInstance();
        suggestionService = PackSuggestionService.getInstance();

        setupGrid();
        setupOffresTable();
        setupComboBox();
        showView(viewPromotion);

        Platform.runLater(() -> {
            if (btnAjouter != null) AnimationHelper.slideInLeft(btnAjouter);
        });

        loadAndDisplayPromos();

        if (searchPromoField != null)
            searchPromoField.textProperty().addListener((obs, o, n) -> filterCards(n));
    }

    // ════════════════════════════════════════════════════
    // GRID
    // ════════════════════════════════════════════════════

    private void setupGrid() {
        if (promoCardsGrid == null) return;
        promoCardsGrid.getColumnConstraints().clear();
        promoCardsGrid.setHgap(16);
        promoCardsGrid.setVgap(16);
        for (int i = 0; i < COLS; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / COLS);
            cc.setHgrow(Priority.ALWAYS);
            promoCardsGrid.getColumnConstraints().add(cc);
        }
    }

    // ════════════════════════════════════════════════════
    // LOAD + DISPLAY
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
        buildCards(allPromos.filtered(p ->
                p.getName().toLowerCase().contains(kw) ||
                        p.getDescription().toLowerCase().contains(kw)));
    }

    private void buildCards(Iterable<Promotion> promos) {
        if (promoCardsGrid == null) return;
        promoCardsGrid.getChildren().clear();
        promoCardsGrid.getRowConstraints().clear();

        List<VBox> cards = new ArrayList<>();
        int i = 0;
        for (Promotion p : promos) {
            VBox card = createPromoCard(p);
            card.setMaxWidth(Double.MAX_VALUE);
            promoCardsGrid.add(card, i % COLS, i / COLS);
            cards.add(card);
            i++;
        }

        if (cards.isEmpty()) {
            Label empty = new Label("Aucune promotion trouvée");
            empty.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");
            GridPane.setColumnSpan(empty, COLS);
            promoCardsGrid.add(empty, 0, 0);
            return;
        }
        AnimationHelper.staggeredFadeIn(cards, 70);
    }

    // ════════════════════════════════════════════════════
    // CARD
    // ════════════════════════════════════════════════════

    private VBox createPromoCard(Promotion promo) {
        boolean trending = trendingService.isTrending(promo);

        VBox card = new VBox(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setCursor(javafx.scene.Cursor.HAND);
        setCardStyle(card, false, trending);

        card.setOnMouseEntered(e -> { setCardHoverStyle(card); new Pulse(card).play(); });
        card.setOnMouseExited(e  -> {
            boolean sel = selectedPromotion != null && selectedPromotion.getId() == promo.getId();
            setCardStyle(card, sel, trending);
        });
        card.setOnMouseClicked(e -> selectCard(promo, card, trending));

        // Header
        StackPane header = new StackPane();
        header.setPrefHeight(120);
        header.setStyle(buildGradient(promo, trending) + " -fx-background-radius: 14 14 0 0;");
        Label bigIcon = new Label(trending ? "🔥" : getPromoIcon(promo));
        bigIcon.setStyle("-fx-font-size: 40px;");
        header.getChildren().add(bigIcon);

        Label typeBadge = new Label(promo.isPack() ? "📦 PACK" : "🎁 PROMO");
        typeBadge.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-text-fill: " +
                (promo.isPack() ? ORANGE : TURQUOISE) +
                "; -fx-font-weight: bold; -fx-font-size: 8px; -fx-padding: 3 7; -fx-background-radius: 20;");
        StackPane.setAlignment(typeBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(typeBadge, new Insets(8));
        header.getChildren().add(typeBadge);

        if (trending) {
            Label tb = new Label("🔥 TENDANCE");
            tb.setStyle("-fx-background-color: " + ROUGE + "; -fx-text-fill: white;" +
                    "-fx-font-weight: 900; -fx-font-size: 8px; -fx-padding: 3 8; -fx-background-radius: 20;");
            StackPane.setAlignment(tb, Pos.TOP_LEFT);
            StackPane.setMargin(tb, new Insets(8));
            header.getChildren().add(tb);
        } else if (promo.isLocked()) {
            Label lb = new Label("🔒");
            lb.setStyle("-fx-background-color: " + BLEU_NUIT + "; -fx-text-fill: white;" +
                    "-fx-font-size: 8px; -fx-padding: 3 8; -fx-background-radius: 20;");
            StackPane.setAlignment(lb, Pos.TOP_LEFT);
            StackPane.setMargin(lb, new Insets(8));
            header.getChildren().add(lb);
        }

        boolean active = promo.isActive();
        Label statusBadge = new Label(active ? "✅" : "⛔");
        statusBadge.setStyle("-fx-background-color: " + (active ? TURQUOISE : "#94A3B8") +
                "; -fx-text-fill: white; -fx-font-size: 8px; -fx-padding: 3 7; -fx-background-radius: 10;");
        StackPane.setAlignment(statusBadge, Pos.BOTTOM_LEFT);
        StackPane.setMargin(statusBadge, new Insets(8));
        header.getChildren().add(statusBadge);

        // Content
        VBox content = new VBox(5);
        content.setPadding(new Insets(10, 12, 8, 12));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 14 14;");

        Label nameLabel = new Label(promo.getName());
        nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: " + BLEU_NUIT + ";");
        nameLabel.setWrapText(true);

        HBox priceRow = new HBox(5); priceRow.setAlignment(Pos.CENTER_LEFT);
        if (promo.getDiscountPercentage() != null) {
            Label d = new Label("-" + String.format("%.0f", promo.getDiscountPercentage()) + "%");
            d.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: " + ORANGE + ";");
            priceRow.getChildren().add(d);
        }
        if (promo.getDiscountFixed() != null) {
            Label d = new Label("-" + String.format("%.0f", promo.getDiscountFixed()) + " TND");
            d.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #E67E22;");
            priceRow.getChildren().add(d);
        }
        Label smartBadge = new Label("🧠 Smart");
        smartBadge.setStyle("-fx-background-color: " + BG_LIGHT + "; -fx-text-fill: " + BLEU_NUIT +
                "; -fx-font-size: 7px; -fx-font-weight: 700; -fx-padding: 2 5; -fx-background-radius: 8;");
        priceRow.getChildren().add(smartBadge);

        double score = trendingService.getTrendingScore(promo);
        String scoreColor = trendingService.getScoreColor(promo);
        Label scoreLbl = new Label(trendingService.getScoreLabel(promo) + " · " + String.format("%.1f", score));
        scoreLbl.setStyle("-fx-font-size: 8px; -fx-text-fill: " + scoreColor + "; -fx-font-weight: 700;");

        HBox statsRow = new HBox(8); statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.setStyle("-fx-background-color: " + BG_LIGHT + "; -fx-padding: 4 8; -fx-background-radius: 6;");
        Label vuesLbl = new Label("👁 0"); vuesLbl.setStyle("-fx-font-size: 9px; -fx-font-weight: 700; -fx-text-fill: " + BLEU_NUIT + ";");
        Label resaLbl = new Label("📋 0"); resaLbl.setStyle("-fx-font-size: 9px; -fx-font-weight: 700; -fx-text-fill: " + TURQUOISE + ";");
        statsRow.getChildren().addAll(vuesLbl, resaLbl);

        PauseTransition pt = new PauseTransition(Duration.millis(400));
        pt.setOnFinished(e -> {
            vuesLbl.setText("👁 " + promo.getNbVues() + " vues");
            resaLbl.setText("📋 " + promo.getNbReservations() + " réserv.");
        });
        pt.play();

        Separator sep = new Separator(); sep.setStyle("-fx-border-color: #F1F5F9;");

        HBox btnRow = new HBox(6); btnRow.setAlignment(Pos.CENTER);
        Button btnQR      = buildSquareBtn("🔒", "QR",     BLEU_NUIT, "#3D5166");
        Button btnDetails = buildSquareBtn("📋", "Détails", ORANGE,    "#E67E22");
        Button btnDelete  = buildSquareBtn("🗑️","Suppr.",  ROUGE,     "#C53030");
        btnQR.setOnAction(e      -> { e.consume(); handleQRCode(promo); });
        btnDetails.setOnAction(e -> { e.consume(); handleDetails(promo); });
        btnDelete.setOnAction(e  -> { e.consume(); handleDeleteFromCard(promo); });
        btnRow.getChildren().addAll(btnQR, btnDetails, btnDelete);

        content.getChildren().addAll(nameLabel, priceRow, scoreLbl, statsRow, sep, btnRow);
        card.getChildren().addAll(header, content);
        return card;
    }

    private void setCardStyle(VBox card, boolean selected, boolean trending) {
        String borderColor = selected ? ORANGE : (trending ? "#FEB2B2" : "#E2E8F0");
        String shadow      = selected ? "rgba(243,156,18,0.3)" : (trending ? "rgba(229,62,62,0.15)" : "rgba(0,0,0,0.07)");
        int    bw          = (selected || trending) ? 2 : 1;
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14;" +
                "-fx-effect: dropshadow(gaussian," + shadow + ", 12, 0, 0, 3);" +
                "-fx-border-color: " + borderColor + "; -fx-border-radius: 14; -fx-border-width: " + bw + ";");
    }

    private void setCardHoverStyle(VBox card) {
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14;" +
                "-fx-effect: dropshadow(gaussian, rgba(243,156,18,0.25), 18, 0, 0, 5);" +
                "-fx-border-color: " + ORANGE + "; -fx-border-radius: 14; -fx-border-width: 2; -fx-translate-y: -2;");
    }

    private void selectCard(Promotion promo, VBox card, boolean trending) {
        selectedPromotion = promo;
        fillForm(promo);
        promoCardsGrid.getChildren().forEach(n -> {
            if (n instanceof VBox v) setCardStyle(v, false, trendingService.isTrending(promo));
        });
        setCardStyle(card, true, trending);
        new Flash(card).play();
    }

    private Button buildSquareBtn(String icon, String lbl, String bg, String hover) {
        Button btn = new Button(icon + "\n" + lbl);
        String base = "-fx-background-color:" + bg + "; -fx-text-fill:white; -fx-font-size:8px;" +
                " -fx-font-weight:700; -fx-padding:7 10; -fx-background-radius:8; -fx-cursor:hand;" +
                " -fx-alignment:CENTER; -fx-pref-width:65; -fx-pref-height:44;";
        String hov  = "-fx-background-color:" + hover + "; -fx-text-fill:white; -fx-font-size:8px;" +
                " -fx-font-weight:700; -fx-padding:7 10; -fx-background-radius:8; -fx-cursor:hand;" +
                " -fx-alignment:CENTER; -fx-pref-width:65; -fx-pref-height:44; -fx-translate-y:-1;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> { btn.setStyle(hov); new Pulse(btn).play(); });
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private String buildGradient(Promotion p, boolean trending) {
        if (trending)    return "-fx-background-color: linear-gradient(to bottom right, #E53E3E, #F39C12);";
        if (p.isPack())  return "-fx-background-color: linear-gradient(to bottom right, #F39C12, #F7DC6F);";
        if (p.isLocked())return "-fx-background-color: linear-gradient(to bottom right, #2C3E50, #4A6278);";
        return "-fx-background-color: linear-gradient(to bottom right, #1ABC9C, #2C3E50);";
    }

    private String getPromoIcon(Promotion p) {
        if (p.isPack())  return "📦";
        if (p.isLocked()) return "🔒";
        return "🎁";
    }

    // ════════════════════════════════════════════════════
    // QR CODE
    // ════════════════════════════════════════════════════

    private void handleQRCode(Promotion promo) {
        if (!promo.isLocked()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Verrouiller ?");
            confirm.setHeaderText("\"" + promo.getName() + "\" n'est pas verrouillée.");
            confirm.setContentText("Verrouiller et générer un code QR ?");
            Optional<ButtonType> r = confirm.showAndWait();
            if (r.isEmpty() || r.get() != ButtonType.OK) return;
            promo.setLocked(true);
            promotionService.update(promo);
            loadAndDisplayPromos();
        }
        PromoCode code = promoCodeService.createPromoCode(promo.getId());
        if (code == null) { notifService.danger("Erreur", "Impossible de générer le code."); return; }
        Image qrImg = promoCodeService.generateQrCodeImage(code.getQrContent(), 260);
        showQrDialog(promo, code, qrImg);
        notifService.success("QR Code généré !", "Code : " + code.getCode());
    }

    private void showQrDialog(Promotion promo, PromoCode code, Image qrImg) {
        Stage d = new Stage(); d.initModality(Modality.APPLICATION_MODAL);
        d.setTitle("QR — " + promo.getName());
        VBox root = new VBox(14); root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: " + BG_LIGHT + "; -fx-padding: 28;");
        Label title = new Label("🔒  Code Généré");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: " + BLEU_NUIT + ";");
        ImageView qrView = new ImageView(qrImg);
        qrView.setFitWidth(240); qrView.setFitHeight(240);
        VBox codeBox = new VBox(4); codeBox.setAlignment(Pos.CENTER);
        codeBox.setStyle("-fx-background-color: white; -fx-padding: 12 22; -fx-background-radius: 10;" +
                "-fx-border-color: " + JAUNE + "; -fx-border-radius: 10; -fx-border-width: 2;");
        Label codeVal = new Label(code.getCode());
        codeVal.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: " + ORANGE + "; -fx-font-family: monospace;");
        codeBox.getChildren().add(codeVal);
        Button closeBtn = new Button("✓  Fermer");
        closeBtn.setStyle("-fx-background-color: " + ORANGE + "; -fx-text-fill: white; -fx-font-weight: 700;" +
                "-fx-padding: 10 28; -fx-background-radius: 20; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> d.close());
        root.getChildren().addAll(title, qrView, codeBox, closeBtn);
        d.setScene(new Scene(root, 370, 500)); d.show();
        new BounceIn(qrView).play();
        new ZoomIn(codeBox).play();
    }

    // ════════════════════════════════════════════════════
    // DÉTAILS
    // ════════════════════════════════════════════════════

    private void handleDetails(Promotion promo) {
        // ✅ FIXED: Do NOT increment nb_vues here.
        // nb_vues must only be incremented from FrontOffice (real user interactions).
        // Admin clicking Details in BackOffice must NOT affect trending score.

        boolean trending = trendingService.isTrending(promo);
        double  score    = trendingService.getTrendingScore(promo);
        double  norm     = Math.min(1.0, score / 50.0);

        Stage d = new Stage(); d.initModality(Modality.APPLICATION_MODAL);
        d.setTitle("Détails — " + promo.getName());

        HBox header = new HBox(14); header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: " + BLEU_NUIT + "; -fx-padding: 18 22;");
        Label hIcon = new Label(trending ? "🔥" : getPromoIcon(promo)); hIcon.setStyle("-fx-font-size: 30px;");
        VBox hText = new VBox(3);
        Label hName = new Label(promo.getName());
        hName.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: white;");
        Label hSub = new Label("ID #" + promo.getId() + "  ·  " + (promo.isPack() ? "Pack" : "Individuel") +
                (trending ? "  ·  🔥 TENDANCE" : ""));
        hSub.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (trending ? "#FEB2B2" : JAUNE) + ";");
        hText.getChildren().addAll(hName, hSub);
        header.getChildren().addAll(hIcon, hText);

        VBox content = new VBox(14);
        content.setStyle("-fx-background-color: " + BG_LIGHT + "; -fx-padding: 18;");

        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(8);
        grid.setStyle("-fx-background-color: white; -fx-padding: 14; -fx-background-radius: 10;");
        addDetailRow(grid, 0, "📝 Nom",          promo.getName());
        addDetailRow(grid, 1, "📋 Description",  promo.getDescription());
        addDetailRow(grid, 2, "💰 Réduction",
                (promo.getDiscountPercentage() != null ? promo.getDiscountPercentage() + "%" : "—") + "  /  " +
                        (promo.getDiscountFixed() != null ? promo.getDiscountFixed() + " TND" : "—"));
        addDetailRow(grid, 3, "📅 Période",      promo.getStartDate() + "  →  " + promo.getEndDate());
        addDetailRow(grid, 4, "🏷️ Type",         promo.isPack() ? "Pack Combiné" : "Individuelle");
        addDetailRow(grid, 5, "🔒 Verrouillée",  promo.isLocked() ? "Oui" : "Non");
        addDetailRow(grid, 6, "📊 Statut",       promo.isActive() ? "✅ Active" : "⛔ Expirée");

        // Show linked offer targets with dynamic prices
        List<PromotionTarget> targets = targetService.getByPromotionId(promo.getId());
        if (!targets.isEmpty()) {
            VBox targetsBox = new VBox(8);
            targetsBox.setStyle("-fx-background-color: white; -fx-padding: 14; -fx-background-radius: 10;");
            Label targetsTitle = new Label("🎯 Offres liées");
            targetsTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: " + BLEU_NUIT + ";");
            targetsBox.getChildren().add(targetsTitle);
            Pricecalculatorservice priceCalc = Pricecalculatorservice.getInstance();
            for (PromotionTarget t : targets) {
                String offerName = offresService.getOfferName(t.getTargetType(), t.getTargetId());
                String unitPrice = priceCalc.getPriceUnitLabel(t.getTargetType(), t.getTargetId());
                String typeIcon = switch (t.getTargetType()) {
                    case HEBERGEMENT -> "🏨";
                    case ACTIVITE    -> "🎯";
                    case VOITURE     -> "🚗";
                };
                HBox row = new HBox(8); row.setAlignment(Pos.CENTER_LEFT);
                Label lbl = new Label(typeIcon + " " + offerName + " — " + unitPrice);
                lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + BLEU_NUIT + ";");
                row.getChildren().add(lbl);
                targetsBox.getChildren().add(row);
            }
            grid.add(targetsBox, 0, 7); GridPane.setColumnSpan(targetsBox, 2);
        }

        // Trending section
        VBox trendBox = buildTrendingSection(promo, trending, score, norm);
        VBox smartBox = buildSmartDiscountSection(promo);

        HBox statsBox = new HBox(10);
        statsBox.getChildren().addAll(
                buildStatCardAnimated("👁",  0, promo.getNbVues(),         "Vues",         JAUNE),
                buildStatCardAnimated("📋",  0, promo.getNbReservations(), "Réservations", TURQUOISE)
        );

        content.getChildren().addAll(grid, trendBox, smartBox, statsBox);

        Button btnClose = new Button("✓  Fermer");
        btnClose.setStyle("-fx-background-color: " + ORANGE + "; -fx-text-fill: white;" +
                "-fx-font-weight: 700; -fx-padding: 10 28; -fx-background-radius: 20; -fx-cursor: hand;");
        btnClose.setOnAction(e -> { d.close(); loadAndDisplayPromos(); });

        VBox root = new VBox(0);
        root.getChildren().addAll(header, content, btnClose);
        VBox.setMargin(btnClose, new Insets(0, 18, 14, 18));

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        d.setScene(new Scene(scroll, 520, 680)); d.show();
        new SlideInDown(header).play();
        new FadeInUp(content).play();
        if (trending) new Tada(trendBox).play();
    }

    private VBox buildTrendingSection(Promotion promo, boolean trending, double score, double norm) {
        VBox trendBox = new VBox(10);
        trendBox.setStyle("-fx-background-color: white; -fx-padding: 14; -fx-background-radius: 10;" +
                "-fx-border-color: " + (trending ? ROUGE : "#E2E8F0") + "; -fx-border-width: 2; -fx-border-radius: 10;");
        HBox trendHeader = new HBox(8); trendHeader.setAlignment(Pos.CENTER_LEFT);
        Label trendIcon  = new Label(trending ? "🔥" : "📊"); trendIcon.setStyle("-fx-font-size: 16px;");
        Label trendTitle = new Label("Popularité & Tendance");
        trendTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + BLEU_NUIT + ";");
        trendHeader.getChildren().addAll(trendIcon, trendTitle);
        Label trendStatus = new Label(trending ? "✔ Tendance" : "✖ Non tendance");
        trendStatus.setStyle("-fx-background-color: " + (trending ? "#F0FFF4" : "#FFF5F5") + ";" +
                "-fx-text-fill: " + (trending ? "#276749" : ROUGE) + ";" +
                "-fx-font-weight: 800; -fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 8;" +
                "-fx-border-color: " + (trending ? "#9AE6B4" : "#FEB2B2") + "; -fx-border-width: 1; -fx-border-radius: 8;");
        VBox scoreSection = new VBox(6);
        HBox scoreLabelRow = new HBox();
        Label scoreLabel = new Label("Score : " + trendingService.getScoreLabel(promo));
        scoreLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + trendingService.getScoreColor(promo) + ";");
        Label scoreVal = new Label(String.format("%.1f", score));
        scoreVal.setStyle("-fx-font-size: 11px; -fx-text-fill: " + GRAY + ";");
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        scoreLabelRow.getChildren().addAll(scoreLabel, sp2, scoreVal);
        StackPane barBg = new StackPane(); barBg.setPrefHeight(8);
        barBg.setStyle("-fx-background-color: #E2E8F0; -fx-background-radius: 4;");
        HBox barFill = new HBox(); barFill.setPrefHeight(8);
        barFill.setStyle("-fx-background-color: " + trendingService.getScoreColor(promo) + "; -fx-background-radius: 4;");
        barBg.widthProperty().addListener((obs, o, w) -> barFill.setPrefWidth(w.doubleValue() * norm));
        StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
        barBg.getChildren().add(barFill);
        HBox condRow = new HBox(12);
        Label condVues = new Label((promo.getNbVues() > 30 ? "✅" : "❌") + " " + promo.getNbVues() + " vues (seuil: 30)");
        condVues.setStyle("-fx-font-size: 10px; -fx-text-fill: " + GRAY + ";");
        Label condResa = new Label((promo.getNbReservations() > 10 ? "✅" : "❌") + " " + promo.getNbReservations() + " réserv. (seuil: 10)");
        condResa.setStyle("-fx-font-size: 10px; -fx-text-fill: " + GRAY + ";");
        condRow.getChildren().addAll(condVues, condResa);
        scoreSection.getChildren().addAll(scoreLabelRow, barBg, condRow);
        trendBox.getChildren().addAll(trendHeader, trendStatus, scoreSection);
        return trendBox;
    }

    private VBox buildSmartDiscountSection(Promotion promo) {
        List<Promotion> all = new ArrayList<>(allPromos);
        double avg = all.stream().filter(Promotion::isActive)
                .filter(p -> p.getDiscountPercentage() != null)
                .mapToInt(Promotion::getNbReservations).average().orElse(0.0);
        float preview = discountEngine.previewDiscount(promo, avg);
        String reason = discountEngine.getAdjustmentReason(promo, avg);

        VBox box = new VBox(10);
        box.setStyle("-fx-background-color: white; -fx-padding: 14; -fx-background-radius: 10;" +
                "-fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 10;");
        Label title = new Label("🧠  Smart Discount Engine");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + BLEU_NUIT + ";");
        HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT);
        Label current = new Label("Actuel : " + String.format("%.0f", promo.getDiscountPercentage() != null ? promo.getDiscountPercentage() : 0f) + "%");
        current.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + GRAY + ";");
        Label arrow = new Label("→"); arrow.setStyle("-fx-font-size: 14px; -fx-text-fill: " + GRAY + ";");
        Label next = new Label("Smart : " + String.format("%.0f", preview) + "%");
        next.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: " +
                (preview > (promo.getDiscountPercentage() != null ? promo.getDiscountPercentage() : 0f) ? ROUGE : TURQUOISE) + ";");
        row.getChildren().addAll(current, arrow, next);
        Label reasonLbl = new Label(reason);
        reasonLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: " + GRAY + "; -fx-wrap-text: true;");
        reasonLbl.setMaxWidth(400);
        Label avgLbl = new Label("Moyenne réserv. actives : " + String.format("%.1f", avg));
        avgLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: #94A3B8;");
        box.getChildren().addAll(title, row, reasonLbl, avgLbl);
        return box;
    }

    private HBox buildStatCardAnimated(String icon, int from, int to, String label, String color) {
        VBox card = new VBox(4); card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-padding: 14 24; -fx-background-radius: 10;" +
                "-fx-border-color: " + color + "; -fx-border-radius: 10; -fx-border-width: 2;");
        Label ic = new Label(icon); ic.setStyle("-fx-font-size: 18px;");
        Label va = new Label("0"); va.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: " + BLEU_NUIT + ";");
        Label la = new Label(label); la.setStyle("-fx-font-size: 10px; -fx-text-fill: " + GRAY + ";");
        card.getChildren().addAll(ic, va, la);
        Platform.runLater(() -> { AnimationHelper.countUp(va, from, to, 900); new ZoomIn(card).play(); });
        HBox w = new HBox(card); HBox.setHgrow(card, Priority.ALWAYS); card.setMaxWidth(Double.MAX_VALUE);
        return w;
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label l = new Label(label); l.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: " + GRAY + ";");
        Label v = new Label(value != null ? value : "—"); v.setStyle("-fx-font-size: 11px; -fx-text-fill: " + BLEU_NUIT + "; -fx-wrap-text: true;"); v.setMaxWidth(260);
        grid.add(l, 0, row); grid.add(v, 1, row);
    }

    // ════════════════════════════════════════════════════
    // DELETE
    // ════════════════════════════════════════════════════

    private void handleDeleteFromCard(Promotion promo) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer"); confirm.setHeaderText("Supprimer \"" + promo.getName() + "\" ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                schedulerService.unschedulePromo(promo.getId());
                targetService.deleteByPromotionId(promo.getId());
                promotionService.delete(promo.getId());
                if (selectedPromotion != null && selectedPromotion.getId() == promo.getId()) {
                    selectedPromotion = null; handleCancel();
                }
                loadAndDisplayPromos();
                notifService.info("Supprimé", "✅ \"" + promo.getName() + "\" supprimée.");
            }
        });
    }

    // ════════════════════════════════════════════════════
    // PDF
    // ════════════════════════════════════════════════════

    @FXML private void handleExportPDF() {
        if (selectedPromotion == null) {
            notifService.warning("Aucune sélection", "Cliquez d'abord sur une card."); return;
        }
        ChoiceDialog<String> dlg = new ChoiceDialog<>("Français", "Français", "English");
        dlg.setTitle("Langue"); dlg.setHeaderText("Langue du PDF"); dlg.setContentText("Langue :");
        Optional<String> res = dlg.showAndWait(); if (res.isEmpty()) return;
        String lang = res.get().equals("English") ? "en" : "fr";
        String code = null;
        if (selectedPromotion.isLocked()) {
            PromoCode pc = promoCodeService.getByCode(selectedPromotion.getId());
            if (pc != null) code = pc.getCode();
        }
        FileChooser fc = new FileChooser(); fc.setTitle("Enregistrer le PDF");
        fc.setInitialFileName("promo_" + selectedPromotion.getId() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showSaveDialog(null); if (file == null) return;
        final String fCode = code, fLang = lang; final Promotion fp = selectedPromotion;
        new Thread(() -> {
            try {
                pdfService.generatePdf(fp, fCode, fLang, file.getAbsolutePath());
                Platform.runLater(() -> {
                    notifService.success("PDF Généré !", "📄 " + file.getName());
                    try { java.awt.Desktop.getDesktop().open(file); }
                    catch (Exception ex) {
                        try { new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath()).start(); }
                        catch (Exception ex2) { System.err.println("[PDF] Cannot open: " + ex2.getMessage()); }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> notifService.danger("Erreur PDF", e.getMessage()));
            }
        }).start();
    }

    // ════════════════════════════════════════════════════
    // FORM CRUD
    // ════════════════════════════════════════════════════

    private void fillForm(Promotion promo) {
        txtNom.setText(promo.getName());
        txtDescription.setText(promo.getDescription());
        txtPourcentage.setText(promo.getDiscountPercentage() != null ? promo.getDiscountPercentage().toString() : "");
        txtFixe.setText(promo.getDiscountFixed() != null ? promo.getDiscountFixed().toString() : "");
        dateDebut.setValue(promo.getStartDate().toLocalDate());
        dateFin.setValue(promo.getEndDate().toLocalDate());
        chkPack.setSelected(promo.isPack());
        if (chkLocked != null) chkLocked.setSelected(promo.isLocked());
        btnAjouter.setText("💾  Modifier");
        AnimationHelper.slideInLeft(btnAjouter);

        // Populate staged box with existing linked offers
        stagedOffers.clear();
        if (stagedOffresBox != null) stagedOffresBox.getChildren().clear();
        if (promo.isPack()) {
            List<PromotionTarget> targets = targetService.getByPromotionId(promo.getId());
            for (PromotionTarget t : targets) {
                stagedOffers.add(t);
                String name   = offresService.getOfferName(t.getTargetType(), t.getTargetId());
                String detail = getOfferDetail(t.getTargetType(), t.getTargetId());
                addStagedRow(name != null ? name : "Offre #" + t.getTargetId(),
                        detail, t.getTargetType(), t.getTargetId(), true);
            }
        }
        // Reset combo/table
        if (comboTypeOffre != null) comboTypeOffre.setValue(null);
        if (tableOffres    != null) tableOffres.getItems().clear();
    }

    private String getOfferDetail(TargetType type, int id) {
        return switch (type) {
            case HEBERGEMENT -> {
                Hebergement h = offresService.getHebergementById(id);
                yield h != null ? h.getTypeHebergement() + " · " + String.format("%.0f TND/nuit", h.getPrixParNuit()) : "";
            }
            case ACTIVITE -> {
                Activite a = offresService.getActiviteById(id);
                yield a != null ? a.getLieu() + " · " + String.format("%.0f TND/pers.", a.getPrixParPersonne()) : "";
            }
            case VOITURE -> {
                Voiture v = offresService.getVoitureById(id);
                yield v != null ? String.format("%.2f TND/km · %d places", v.getPrixKm(), v.getNbPlaces()) : "";
            }
        };
    }

    @FXML private void addOrUpdate() {
        try {
            String nom  = txtNom.getText().trim();
            String desc = txtDescription.getText().trim();
            if (nom.isEmpty() || desc.isEmpty()) {
                AnimationHelper.shake(txtNom);
                notifService.warning("Champs requis", "Nom et description obligatoires."); return;
            }
            Float pct = null, fix = null;
            if (!txtPourcentage.getText().trim().isEmpty()) pct = Float.parseFloat(txtPourcentage.getText().trim());
            if (!txtFixe.getText().trim().isEmpty())        fix = Float.parseFloat(txtFixe.getText().trim());
            if (pct == null && fix == null) { notifService.warning("Réduction", "Spécifiez une réduction."); return; }
            LocalDate debut = dateDebut.getValue(), fin = dateFin.getValue();
            if (debut == null || fin == null) { notifService.warning("Dates", "Les deux dates sont requises."); return; }
            if (!fin.isAfter(debut)) { AnimationHelper.shake(dateFin); notifService.warning("Dates invalides", "Fin doit être après début."); return; }
            boolean isPack = chkPack.isSelected(), locked = chkLocked != null && chkLocked.isSelected();

            if (selectedPromotion == null) {
                // ✅ FIXED: no more prixParJour in constructor
                Promotion newP = new Promotion(nom, desc, pct, fix, Date.valueOf(debut), Date.valueOf(fin), isPack);
                newP.setLocked(locked);
                Promotion saved = promotionService.add(newP);
                if (saved != null) {
                    // ✅ Save all staged offers now that we have the promo ID
                    for (PromotionTarget staged : stagedOffers)
                        targetService.add(new PromotionTarget(saved.getId(), staged.getTargetType(), staged.getTargetId()));
                    boolean jobs = schedulerService.schedulePromo(saved);
                    String packInfo = stagedOffers.isEmpty() ? "" : " · " + stagedOffers.size() + " offre(s) liée(s)";
                    notifService.success("Créée !", "✅ \"" + nom + "\"" + packInfo + (jobs ? " · Jobs planifiés" : ""));
                    if (locked) { selectedPromotion = saved; handleQRCode(saved); }
                } else { notifService.danger("Erreur", "Impossible de créer."); return; }
            } else {
                selectedPromotion.setName(nom); selectedPromotion.setDescription(desc);
                selectedPromotion.setDiscountPercentage(pct); selectedPromotion.setDiscountFixed(fix);
                selectedPromotion.setStartDate(Date.valueOf(debut)); selectedPromotion.setEndDate(Date.valueOf(fin));
                selectedPromotion.setPack(isPack); selectedPromotion.setLocked(locked);
                if (promotionService.update(selectedPromotion)) {
                    schedulerService.unschedulePromo(selectedPromotion.getId());
                    schedulerService.schedulePromo(selectedPromotion);
                    notifService.success("Modifiée !", "✅ \"" + nom + "\" · Jobs mis à jour.");
                } else { notifService.danger("Erreur", "Modification impossible."); return; }
            }
            loadAndDisplayPromos(); handleCancel();
        } catch (NumberFormatException e) { notifService.danger("Format", "Valeurs numériques incorrectes."); }
    }

    @FXML private void deletePromotion() {
        if (selectedPromotion == null) { notifService.warning("", "Sélectionnez une promotion."); return; }
        handleDeleteFromCard(selectedPromotion);
    }

    @FXML private void handleCancel() {
        txtNom.clear(); txtDescription.clear(); txtPourcentage.clear(); txtFixe.clear();
        dateDebut.setValue(null); dateFin.setValue(null);
        chkPack.setSelected(false); if (chkLocked != null) chkLocked.setSelected(false);
        selectedPromotion = null; btnAjouter.setText("💾  Enregistrer");
        // Clear staged offers
        stagedOffers.clear();
        if (stagedOffresBox != null) stagedOffresBox.getChildren().clear();
        if (comboTypeOffre != null) comboTypeOffre.setValue(null);
        if (tableOffres    != null) tableOffres.getItems().clear();
    }

    // ════════════════════════════════════════════════════
    // OFFRES — ✅ FIXED: uses OffresService (real DB)
    // ════════════════════════════════════════════════════

    @FXML private void ajouterOffreAPromotion() {
        Object sel = tableOffres.getSelectionModel().getSelectedItem();
        if (sel == null) {
            notifService.warning("", "Sélectionnez d'abord une offre dans la liste.");
            AnimationHelper.shake(tableOffres);
            return;
        }

        TargetType tt = null; int tid = 0; String offerName = ""; String detail = "";
        if (sel instanceof Hebergement h) {
            tt = TargetType.HEBERGEMENT; tid = h.getId();
            offerName = h.getNom();
            detail    = h.getTypeHebergement() + " · " + String.format("%.0f TND/nuit", h.getPrixParNuit());
        } else if (sel instanceof Activite a) {
            tt = TargetType.ACTIVITE; tid = a.getId();
            offerName = a.getNom();
            detail    = a.getLieu() + " · " + String.format("%.0f TND/pers.", a.getPrixParPersonne());
        } else if (sel instanceof Voiture v) {
            tt = TargetType.VOITURE; tid = v.getId();
            offerName = v.getNom();
            detail    = String.format("%.2f TND/km · %d places", v.getPrixKm(), v.getNbPlaces());
        }
        if (tt == null) return;

        // Check duplicate in staged list
        final TargetType finalTt = tt; final int finalTid = tid;
        boolean duplicate = stagedOffers.stream().anyMatch(
                t -> t.getTargetType() == finalTt && t.getTargetId() == finalTid);
        if (duplicate) {
            notifService.warning("Déjà ajoutée", "Cette offre est déjà dans le pack.");
            return;
        }

        // If editing an existing promotion, save directly
        if (selectedPromotion != null) {
            targetService.add(new PromotionTarget(selectedPromotion.getId(), tt, tid));
            notifService.success("Offre ajoutée !", "Liée à la promotion.");
            // Also add to staged for visual display
            stagedOffers.add(new PromotionTarget(0, tt, tid));
            addStagedRow(offerName, detail, tt, tid, true);
        } else {
            // New promotion: stage for later
            stagedOffers.add(new PromotionTarget(0, tt, tid));
            addStagedRow(offerName, detail, tt, tid, false);
            // Show staged header if first item
            if (stagedOffers.size() == 1) {
                notifService.success("Offre mise en attente ✓",
                        "Elle sera liée après l'enregistrement du pack.");
            }
        }

        // Reset combo and table so user can pick another type immediately
        comboTypeOffre.setValue(null);
        tableOffres.getItems().clear();
        tableOffres.getSelectionModel().clearSelection();
    }

    private void addStagedRow(String name, String detail, TargetType type, int tid, boolean saved) {
        if (stagedOffresBox == null) return;

        // Add "Offres en attente" header label if this is the first item and not editing
        if (stagedOffresBox.getChildren().isEmpty() && !saved) {
            Label header = new Label("📋 Offres en attente d'enregistrement :");
            header.setStyle("-fx-font-size: 9px; -fx-font-weight: 700; -fx-text-fill: #6366F1; -fx-padding: 4 0 2 0;");
            stagedOffresBox.getChildren().add(header);
        }

        String icon = switch (type) {
            case HEBERGEMENT -> "🏨";
            case ACTIVITE    -> "🎯";
            case VOITURE     -> "🚗";
        };
        String statusColor = saved ? "#1ABC9C" : "#6366F1";
        String statusBg    = saved ? "#F0FFF4" : "#EEF2FF";
        String statusBorder = saved ? "#A7F3D0" : "#C7D2FE";

        HBox row = new HBox(8); row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: " + statusBg + "; -fx-padding: 7 10;" +
                "-fx-background-radius: 8; -fx-border-color: " + statusBorder + ";" +
                "-fx-border-radius: 8; -fx-border-width: 1;");

        Label iconLbl = new Label(icon); iconLbl.setStyle("-fx-font-size: 13px;");

        VBox textBox = new VBox(1); HBox.setHgrow(textBox, Priority.ALWAYS);
        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #2C3E50;");
        Label detailLbl = new Label(detail);
        detailLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: #64748B;");
        textBox.getChildren().addAll(nameLbl, detailLbl);

        Label statusDot = new Label(saved ? "✅" : "⏳");
        statusDot.setStyle("-fx-font-size: 11px;");

        // Remove button
        Button btnRemove = new Button("✕");
        btnRemove.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8;" +
                "-fx-font-size: 10px; -fx-padding: 2 4; -fx-cursor: hand;");
        btnRemove.setOnMouseEntered(e -> btnRemove.setStyle(
                "-fx-background-color: #FEE2E2; -fx-text-fill: #E53E3E;" +
                        "-fx-font-size: 10px; -fx-padding: 2 4; -fx-cursor: hand; -fx-background-radius: 4;"));
        btnRemove.setOnMouseExited(e -> btnRemove.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #94A3B8;" +
                        "-fx-font-size: 10px; -fx-padding: 2 4; -fx-cursor: hand;"));

        final TargetType fType = type; final int fTid = tid;
        btnRemove.setOnAction(e -> {
            stagedOffers.removeIf(t -> t.getTargetType() == fType && t.getTargetId() == fTid);
            stagedOffresBox.getChildren().remove(row);
            // If saved to DB and editing, remove from DB too
            if (saved && selectedPromotion != null) {
                targetService.getByPromotionId(selectedPromotion.getId()).stream()
                        .filter(t -> t.getTargetType() == fType && t.getTargetId() == fTid)
                        .findFirst().ifPresent(t -> targetService.delete(t.getId()));
            }
            // Remove header if no more offers
            if (stagedOffresBox.getChildren().size() == 1) stagedOffresBox.getChildren().clear();
        });

        row.getChildren().addAll(iconLbl, textBox, statusDot, btnRemove);
        stagedOffresBox.getChildren().add(row);
        new animatefx.animation.FadeInUp(row).play();
    }

    // ════════════════════════════════════════════════════
    // STATS KPI
    // ════════════════════════════════════════════════════

    private void updateStats() {
        int total   = allPromos.size();
        long packs  = allPromos.stream().filter(Promotion::isPack).count();
        LocalDate today = LocalDate.now();
        long actives = allPromos.stream().filter(p ->
                !today.isBefore(p.getStartDate().toLocalDate()) && !today.isAfter(p.getEndDate().toLocalDate())
        ).count();

        if (statTotalPromo   != null) AnimationHelper.countUp(statTotalPromo,   0, total,         900);
        if (statPacksPromo   != null) AnimationHelper.countUp(statPacksPromo,   0, (int) packs,   1050);
        if (statActivesPromo != null) AnimationHelper.countUp(statActivesPromo, 0, (int) actives, 1200);

        if (statsHBox != null)
            Platform.runLater(() -> AnimationHelper.staggeredBounceIn(statsHBox.getChildren().stream().toList(), 150));

        double trendPct = trendingService.percentTrending(new ArrayList<>(allPromos));
        System.out.printf("[Trending] %.1f%% des promos sont en tendance (%d/%d)%n",
                trendPct, trendingService.countTrending(new ArrayList<>(allPromos)), total);
    }

    @FXML private void handleShowStatistiques() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/backoffice/StatistiquesReservations.fxml"));
            Stage s = new Stage(); s.setTitle("Statistiques");
            s.setScene(new Scene(root, 1400, 900)); s.show();
            new FadeIn(root).play();
        } catch (IOException e) { notifService.danger("Erreur", e.getMessage()); }
    }

    // ── Setup ──

    private void setupOffresTable() {
        if (colOffreId == null) return;
        colOffreId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOffreNom.setCellValueFactory(new PropertyValueFactory<>("nom")); // getNom() on all 3 models
        colOffreDetails.setCellValueFactory(c -> {
            Object item = c.getValue();
            if (item instanceof Hebergement h) return new javafx.beans.property.SimpleStringProperty(h.getTypeHebergement() + " — " + String.format("%.0f TND/nuit", h.getPrixParNuit()));
            if (item instanceof Activite a)    return new javafx.beans.property.SimpleStringProperty(a.getLieu() + " — " + String.format("%.0f TND/pers.", a.getPrixParPersonne()));
            if (item instanceof Voiture v)     return new javafx.beans.property.SimpleStringProperty(String.format("%.2f TND/km · %d places", v.getPrixKm(), v.getNbPlaces()));
            return new javafx.beans.property.SimpleStringProperty("");
        });
    }

    /**
     * ✅ FIXED: ComboBox now shows "Hébergement / Activité / Voiture"
     * and loads from real DB via OffresService
     */
    private void setupComboBox() {
        if (comboTypeOffre == null) return;
        comboTypeOffre.setItems(FXCollections.observableArrayList("Hébergement", "Activité", "Voiture"));
        comboTypeOffre.setOnAction(e -> {
            String type = comboTypeOffre.getValue(); if (type == null) return;
            ObservableList<Object> offres = FXCollections.observableArrayList();
            switch (type) {
                case "Hébergement" -> offres.addAll(offresService.getAllHebergements());
                case "Activité"    -> offres.addAll(offresService.getAllActivites());
                case "Voiture"     -> offres.addAll(offresService.getAllVoitures());
            }
            tableOffres.setItems(offres);
        });
    }

    private void showView(javafx.scene.Node v) {
        viewPromotion.setVisible(false);
        viewEmpty.setVisible(false);
        if (viewAiPacks != null) viewAiPacks.setVisible(false);
        v.setVisible(true);
    }

    @FXML private void handleShowUsers()        { showView(viewEmpty); }
    @FXML private void handleShowAccounts()     { showView(viewEmpty); }
    @FXML private void handleShowTransactions() { showView(viewEmpty); }
    @FXML private void handleShowCredits()      { showView(viewEmpty); }
    @FXML private void handleShowCashback()     { showView(viewPromotion); }
    @FXML private void handleShowSettings()     { showView(viewEmpty); }
    @FXML private void handleShowAiPacks()      {
        if (viewAiPacks != null) {
            showView(viewAiPacks);
            updateAiStats();
        }
    }
    @FXML private void handleLogout() { notifService.info("Déconnexion", "À implémenter."); }

    // ════════════════════════════════════════════════════
    // AI PACK GENERATOR — all logic inline (single controller)
    // ════════════════════════════════════════════════════

    @FXML
    public void handleAnalyse() {
        if (btnAnalyse != null) {
            btnAnalyse.setDisable(true);
            btnAnalyse.setText("⏳ Analyse en cours…");
        }
        if (aiStatusLabel != null) {
            aiStatusLabel.setText("🤖 Analyse des co-réservations en cours…");
            aiStatusLabel.setStyle("-fx-text-fill: #7C3AED; -fx-font-weight: 700;");
        }
        new Thread(() -> {
            if (suggestionService == null) suggestionService = PackSuggestionService.getInstance();
            List<PackSuggestionDTO> suggestions = suggestionService.generateSuggestions();
            Platform.runLater(() -> {
                displayAiSuggestions(suggestions);
                if (btnAnalyse != null) {
                    btnAnalyse.setDisable(false);
                    btnAnalyse.setText("🔄 Relancer l'analyse");
                }
            });
        }).start();
    }

    private void displayAiSuggestions(List<PackSuggestionDTO> list) {
        if (aiPacksGrid == null) return;
        aiPacksGrid.getChildren().clear();
        aiPacksGrid.getRowConstraints().clear();

        if (list.isEmpty()) {
            Label icon = new Label("🤖"); icon.setStyle("-fx-font-size: 46px;");
            Label msg  = new Label("Aucune co-réservation détectée.");
            msg.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #64748B;");
            Label hint = new Label("Le système a besoin d'historique de réservations pour générer des suggestions.");
            hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
            VBox empty = new VBox(10, icon, msg, hint);
            empty.setAlignment(Pos.CENTER); empty.setPrefHeight(220);
            GridPane.setColumnSpan(empty, 3); aiPacksGrid.add(empty, 0, 0);
            if (aiStatusLabel != null) {
                aiStatusLabel.setText("Aucune suggestion disponible pour le moment.");
                aiStatusLabel.setStyle("-fx-text-fill: #64748B; -fx-font-weight: 600;");
            }
            return;
        }

        if (aiStatusLabel != null) {
            aiStatusLabel.setText("✅ " + list.size() + " pack(s) suggéré(s) — cliquez « Créer » pour valider.");
            aiStatusLabel.setStyle("-fx-text-fill: #1ABC9C; -fx-font-weight: 700;");
        }

        // Setup 3-column grid for AI cards
        if (aiPacksGrid.getColumnConstraints().isEmpty()) {
            for (int i = 0; i < 3; i++) {
                ColumnConstraints cc = new ColumnConstraints();
                cc.setPercentWidth(33.33); cc.setHgrow(Priority.ALWAYS);
                aiPacksGrid.getColumnConstraints().add(cc);
            }
        }

        List<VBox> cards = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            VBox card = buildAiCard(list.get(i));
            card.setMaxWidth(Double.MAX_VALUE);
            aiPacksGrid.add(card, i % 3, i / 3);
            cards.add(card);
        }
        AnimationHelper.staggeredFadeIn(cards, 80);
    }

    private VBox buildAiCard(PackSuggestionDTO dto) {
        VBox card = new VBox(0);
        card.setMaxWidth(Double.MAX_VALUE);
        String borderNormal = "-fx-background-color: white; -fx-background-radius: 14;" +
                "-fx-border-color: #E9D8FD; -fx-border-radius: 14; -fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(124,58,237,0.10), 12, 0, 0, 4);";
        String borderHover  = "-fx-background-color: white; -fx-background-radius: 14;" +
                "-fx-border-color: #7C3AED; -fx-border-radius: 14; -fx-border-width: 2;" +
                "-fx-effect: dropshadow(gaussian, rgba(124,58,237,0.22), 18, 0, 0, 6); -fx-translate-y: -2;";
        card.setStyle(borderNormal);
        card.setOnMouseEntered(e -> card.setStyle(borderHover));
        card.setOnMouseExited(e  -> card.setStyle(borderNormal));

        // Header gradient
        StackPane header = new StackPane();
        header.setPrefHeight(100);
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, #7C3AED, #4F46E5);" +
                "-fx-background-radius: 14 14 0 0;");
        Label bigIcon = new Label("🤖"); bigIcon.setStyle("-fx-font-size: 34px;");
        header.getChildren().add(bigIcon);

        // AI badge top-left
        Label aiBadge = new Label("✨ IA");
        aiBadge.setStyle("-fx-background-color: rgba(255,255,255,0.18); -fx-text-fill: white;" +
                "-fx-font-weight: 900; -fx-font-size: 8px; -fx-padding: 3 8; -fx-background-radius: 20;");
        StackPane.setAlignment(aiBadge, Pos.TOP_LEFT);
        StackPane.setMargin(aiBadge, new Insets(8));
        header.getChildren().add(aiBadge);

        // Confidence badge top-right
        String confColor = dto.getConfidenceScore() >= 70 ? "#10B981"
                : dto.getConfidenceScore() >= 40 ? ORANGE : ROUGE;
        Label confBadge = new Label(String.format("%.0f%%", dto.getConfidenceScore()));
        confBadge.setStyle("-fx-background-color: rgba(255,255,255,0.92); -fx-text-fill: " + confColor + ";" +
                "-fx-font-weight: 900; -fx-font-size: 10px; -fx-padding: 3 9; -fx-background-radius: 20;");
        StackPane.setAlignment(confBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(confBadge, new Insets(8));
        header.getChildren().add(confBadge);

        // Content
        VBox content = new VBox(9);
        content.setPadding(new Insets(13, 15, 13, 15));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 14 14;");

        // Pack name
        Label nameLabel = new Label(dto.getSuggestedName());
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + BLEU_NUIT + ";");
        nameLabel.setWrapText(true);

        // City chip
        String city = (dto.getDetectedCity() != null && !dto.getDetectedCity().isBlank())
                ? dto.getDetectedCity() : "Tunisie";
        Label cityChip = new Label("📍 " + city);
        cityChip.setStyle("-fx-background-color: #F5F3FF; -fx-text-fill: #7C3AED;" +
                "-fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 3 10; -fx-background-radius: 20;");

        // Offers box
        VBox offersBox = new VBox(5);
        offersBox.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 9 11; -fx-background-radius: 8;");
        Label ofTitle = new Label("📦 Offres incluses");
        ofTitle.setStyle("-fx-font-size: 9px; -fx-font-weight: 700; -fx-text-fill: " + GRAY + ";");
        Label o1 = new Label(typeIcon(dto.getOffer1Type()) + "  " + dto.getOffer1Name());
        o1.setStyle("-fx-font-size: 11px; -fx-text-fill: " + BLEU_NUIT + ";"); o1.setWrapText(true);
        Label o2 = new Label(typeIcon(dto.getOffer2Type()) + "  " + dto.getOffer2Name());
        o2.setStyle("-fx-font-size: 11px; -fx-text-fill: " + BLEU_NUIT + ";"); o2.setWrapText(true);
        offersBox.getChildren().addAll(ofTitle, o1, o2);

        // Stats row
        HBox statsRow = new HBox(8); statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.setStyle("-fx-background-color: #F9FAFB; -fx-padding: 7 10; -fx-background-radius: 8;");
        Label freqLbl = new Label("🔁 " + dto.getFrequency() + " co-rés.");
        freqLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: " + BLEU_NUIT + ";");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label priceLbl = new Label("💰 " + String.format("%.0f TND", dto.getEstimatedTotalPrice()));
        priceLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: " + TURQUOISE + ";");
        statsRow.getChildren().addAll(freqLbl, spacer, priceLbl);

        Separator sep = new Separator();

        // AI Reasoning label (only if present)
        String reasoning = dto.getAiReasoning();
        VBox reasoningBox = new VBox();
        if (reasoning != null && !reasoning.isBlank() && !reasoning.startsWith("⚠️")) {
            Label rLabel = new Label("💡 " + reasoning);
            rLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #7C3AED; -fx-font-style: italic;");
            rLabel.setWrapText(true);
            reasoningBox.setStyle("-fx-background-color: #F5F3FF; -fx-padding: 6 10; -fx-background-radius: 7;");
            reasoningBox.getChildren().add(rLabel);
        } else if (reasoning != null && reasoning.startsWith("⚠️")) {
            Label rLabel = new Label(reasoning);
            rLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #92400E;");
            rLabel.setWrapText(true);
            reasoningBox.setStyle("-fx-background-color: #FEF3C7; -fx-padding: 5 10; -fx-background-radius: 7;");
            reasoningBox.getChildren().add(rLabel);
        }

        // Bottom row: discount + create button
        HBox bottomRow = new HBox(8); bottomRow.setAlignment(Pos.CENTER_LEFT);
        Label discLabel = new Label("-" + String.format("%.0f%%", dto.getSuggestedDiscount()));
        discLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: " + ORANGE + ";");
        Region spacer2 = new Region(); HBox.setHgrow(spacer2, Priority.ALWAYS);

        Button btnCreate = new Button("✨ Créer ce Pack");
        String btnStyle   = "-fx-background-color: #7C3AED; -fx-text-fill: white; -fx-font-weight: 700;" +
                "-fx-font-size: 11px; -fx-padding: 8 16; -fx-background-radius: 9; -fx-cursor: hand;";
        String btnHover   = "-fx-background-color: #6D28D9; -fx-text-fill: white; -fx-font-weight: 700;" +
                "-fx-font-size: 11px; -fx-padding: 8 16; -fx-background-radius: 9; -fx-cursor: hand; -fx-translate-y: -1;";
        btnCreate.setStyle(btnStyle);
        btnCreate.setOnMouseEntered(e -> { btnCreate.setStyle(btnHover); new Pulse(btnCreate).play(); });
        btnCreate.setOnMouseExited(e  -> btnCreate.setStyle(btnStyle));
        btnCreate.setOnAction(e -> showAiConfirmDialog(dto, card, btnCreate));

        bottomRow.getChildren().addAll(discLabel, spacer2, btnCreate);
        content.getChildren().addAll(nameLabel, cityChip, offersBox, statsRow, sep, reasoningBox, bottomRow);
        card.getChildren().addAll(header, content);
        return card;
    }

    private String typeIcon(TargetType t) {
        if (t == null) return "🎯";
        return switch (t) {
            case HEBERGEMENT -> "🏨";
            case ACTIVITE    -> "🎯";
            case VOITURE     -> "🚗";
        };
    }

    private void showAiConfirmDialog(PackSuggestionDTO dto, VBox sourceCard, Button sourceBtn) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Confirmation — Création Pack IA");
        dialog.setResizable(false);

        // Header
        HBox hdr = new HBox(12); hdr.setAlignment(Pos.CENTER_LEFT);
        hdr.setStyle("-fx-background-color: linear-gradient(to right, #7C3AED, #4F46E5); -fx-padding: 18 24;");
        Label hIcon = new Label("🤖"); hIcon.setStyle("-fx-font-size: 26px;");
        VBox hText = new VBox(2);
        Label hTitle = new Label("Création de Pack IA");
        hTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: white;");
        Label hSub = new Label("Vérification avant création automatique");
        hSub.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.75);");
        hText.getChildren().addAll(hTitle, hSub);
        hdr.getChildren().addAll(hIcon, hText);

        // Body
        VBox body = new VBox(12);
        body.setStyle("-fx-padding: 20 24; -fx-background-color: white;");

        String city = (dto.getDetectedCity() != null && !dto.getDetectedCity().isBlank())
                ? dto.getDetectedCity() : "Tunisie";

        VBox info = new VBox(8);
        info.setStyle("-fx-background-color: #F5F3FF; -fx-padding: 15; -fx-background-radius: 11;" +
                "-fx-border-color: #DDD6FE; -fx-border-radius: 11; -fx-border-width: 1;");
        Label iTitle = new Label("📋 Récapitulatif du pack à créer");
        iTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #7C3AED;");
        Label iName = new Label("📦 " + dto.getSuggestedName());
        iName.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + BLEU_NUIT + ";");
        iName.setWrapText(true);
        Label iCity = new Label("📍 Ville : " + city);
        iCity.setStyle("-fx-font-size: 11px; -fx-text-fill: " + GRAY + ";");
        Label iO1 = new Label(typeIcon(dto.getOffer1Type()) + " " + dto.getOffer1Name());
        iO1.setStyle("-fx-font-size: 11px; -fx-text-fill: " + BLEU_NUIT + ";"); iO1.setWrapText(true);
        Label iO2 = new Label(typeIcon(dto.getOffer2Type()) + " " + dto.getOffer2Name());
        iO2.setStyle("-fx-font-size: 11px; -fx-text-fill: " + BLEU_NUIT + ";"); iO2.setWrapText(true);
        Separator is = new Separator();
        Label iPrice = new Label("💰 Prix estimé : " + String.format("%.2f TND", dto.getEstimatedTotalPrice())
                + "   🏷️ Réduction : -" + String.format("%.0f%%", dto.getSuggestedDiscount()));
        iPrice.setStyle("-fx-font-size: 11px; -fx-text-fill: " + TURQUOISE + "; -fx-font-weight: 700;");
        Label iFreq = new Label("🔁 Co-réservations détectées : " + dto.getFrequency()
                + "   🎯 Confiance : " + String.format("%.0f%%", dto.getConfidenceScore()));
        iFreq.setStyle("-fx-font-size: 10px; -fx-text-fill: " + GRAY + ";");

        // AI reasoning
        String reasoning = dto.getAiReasoning();
        Label iReason = new Label();
        if (reasoning != null && !reasoning.isBlank()) {
            iReason.setText("💡 " + reasoning);
            iReason.setStyle("-fx-font-size: 10px; -fx-text-fill: #7C3AED; -fx-font-style: italic;");
            iReason.setWrapText(true);
        }
        info.getChildren().addAll(iTitle, iName, iCity, iO1, iO2, is, iPrice, iFreq, iReason);

        Label warning = new Label(
                "⚠️ Cette action créera automatiquement une promotion de type pack basée " +
                        "sur les patterns de réservation des utilisateurs. L'action est réversible depuis le panneau des promotions.");
        warning.setStyle("-fx-font-size: 10px; -fx-text-fill: #92400E; -fx-background-color: #FEF3C7;" +
                "-fx-padding: 10 12; -fx-background-radius: 8;");
        warning.setWrapText(true); warning.setMaxWidth(400);

        body.getChildren().addAll(info, warning);

        // Buttons
        HBox btnRow = new HBox(10); btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setStyle("-fx-padding: 14 24 18 24; -fx-background-color: #F9FAFB;" +
                "-fx-border-color: #E2E8F0; -fx-border-width: 1 0 0 0;");
        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: white; -fx-text-fill: " + GRAY + "; -fx-font-weight: 700;" +
                "-fx-padding: 9 22; -fx-background-radius: 9; -fx-cursor: hand;" +
                "-fx-border-color: #E2E8F0; -fx-border-radius: 9; -fx-border-width: 1;");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnOk = new Button("✨ Confirmer la création");
        btnOk.setStyle("-fx-background-color: #7C3AED; -fx-text-fill: white; -fx-font-weight: 700;" +
                "-fx-padding: 9 22; -fx-background-radius: 9; -fx-cursor: hand;");
        btnOk.setOnAction(e -> { dialog.close(); createAiPack(dto, sourceCard, sourceBtn); });
        btnRow.getChildren().addAll(btnCancel, btnOk);

        VBox root = new VBox(0, hdr, body, btnRow);
        root.setStyle("-fx-background-color: white;");
        dialog.setScene(new Scene(root, 450, 490));
        dialog.show();
        new SlideInDown(hdr).play();
    }

    private void createAiPack(PackSuggestionDTO dto, VBox sourceCard, Button sourceBtn) {
        // Safety: check for duplicate by name or targets
        List<Promotion> existing = promotionService.getAll();
        for (Promotion ex : existing) {
            if (!ex.isPack()) continue;
            if (ex.getName().equalsIgnoreCase(dto.getSuggestedName())) {
                showDuplicateAlert(ex.getName()); return;
            }
            List<PromotionTarget> exTargets = targetService.getByPromotionId(ex.getId());
            boolean has1 = exTargets.stream().anyMatch(t ->
                    t.getTargetType() == dto.getOffer1Type() && t.getTargetId() == dto.getOffer1Id());
            boolean has2 = exTargets.stream().anyMatch(t ->
                    t.getTargetType() == dto.getOffer2Type() && t.getTargetId() == dto.getOffer2Id());
            if (has1 && has2) { showDuplicateAlert(ex.getName()); return; }
        }

        // Create via PromotionService.createFromAiSuggestion (never direct DB)
        Promotion saved = promotionService.createFromAiSuggestion(dto);
        if (saved == null || saved.getId() <= 0) {
            notifService.danger("Erreur IA", "Impossible de créer le pack. Vérifiez la connexion DB.");
            return;
        }

        // Insert targets
        targetService.add(new PromotionTarget(saved.getId(), dto.getOffer1Type(), dto.getOffer1Id()));
        targetService.add(new PromotionTarget(saved.getId(), dto.getOffer2Type(), dto.getOffer2Id()));

        notifService.success("Pack IA créé ✨",
                "\"" + saved.getName() + "\" · -" + String.format("%.0f", dto.getSuggestedDiscount()) + "%");

        System.out.printf("[AI] Pack créé id=%d name='%s' targets=[%s#%d, %s#%d]%n",
                saved.getId(), saved.getName(),
                dto.getOffer1Type(), dto.getOffer1Id(),
                dto.getOffer2Type(), dto.getOffer2Id());

        // Visual feedback on card
        if (sourceBtn != null) {
            sourceBtn.setDisable(true);
            sourceBtn.setText("✅ Créé");
            sourceBtn.setStyle("-fx-background-color: #1ABC9C; -fx-text-fill: white; -fx-font-weight: 700;" +
                    "-fx-font-size: 11px; -fx-padding: 8 16; -fx-background-radius: 9;");
        }
        if (sourceCard != null) {
            sourceCard.setStyle("-fx-background-color: #F0FFF4; -fx-background-radius: 14;" +
                    "-fx-border-color: #1ABC9C; -fx-border-radius: 14; -fx-border-width: 2;" +
                    "-fx-effect: dropshadow(gaussian, rgba(26,188,156,0.25), 14, 0, 0, 4);");
            new Flash(sourceCard).play();
        }
        updateAiStats();
        loadAndDisplayPromos(); // refresh main promo view as well
    }

    private void showDuplicateAlert(String name) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Pack déjà existant");
        alert.setHeaderText("Ce pack existe déjà.");
        alert.setContentText("Un pack similaire nommé « " + name + " » existe déjà.\nAucune création effectuée.");
        alert.showAndWait();
    }

    private void updateAiStats() {
        if (statAiTotal == null) return;
        List<Promotion> all = promotionService.getAll();
        long total  = all.stream().filter(Promotion::isPack).count();
        long active = all.stream().filter(p -> p.isPack() && p.isActive()).count();
        AnimationHelper.countUp(statAiTotal,  0, (int) total,  800);
        AnimationHelper.countUp(statAiActive, 0, (int) active, 1000);
    }
}
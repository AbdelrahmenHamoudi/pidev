package org.example.controllers.frontoffice;

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
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.models.*;
import org.example.services.*;
import org.example.utils.AnimationHelper;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import org.example.models.ReservationPromo;

/**
 * ✅ FIXED:
 *  - OffresStaticData → OffresService (real DB)
 *  - userNameText now uses SessionManager.getCurrentUserName() dynamically
 *  - All other logic, animations, and features preserved
 */
public class PromotionFrontOfficeController implements Initializable {

    @FXML private TextField        searchField;
    @FXML private CheckBox         packCheckbox, individuCheckbox, activeCheckbox;
    @FXML private Text             resultsCountText;
    @FXML private ComboBox<String> sortCombo;
    @FXML private GridPane         promotionsGrid;

    // ── Header navigation buttons ──
    @FXML private Button           btnMesReservations;
    @FXML private Button           btnAccueil;
    @FXML private Button           btnProfil;

    // ── Header user info labels (filled dynamically from SessionManager) ──
    @FXML private Label            labelUserName;
    @FXML private Label            labelUserEmail;

    // ── Language selector ──
    @FXML private ComboBox<String> languageSelector;

    // ── Legacy Text field — kept for backward compat if still in FXML ──
    @FXML private Text             userNameText;

    // ── Sidebar panels ──
    @FXML private Text  sidebarUserName;
    @FXML private Label sidebarReservCount;
    @FXML private Label loyaltyBadge;
    @FXML private VBox  sidebarTrendingBox;
    @FXML private VBox  sidebarUpcomingBox;

    private PromotionService       promotionService;
    private PromotionTargetService targetService;
    private Offresservice offresService;  // ✅ FIXED: was OffresStaticData
    private PromoCodeService       promoCodeService;
    private TrendingService        trendingService;
    private NotificationService    notifService;

    private ObservableList<Promotion> allPromotions      = FXCollections.observableArrayList();
    private ObservableList<Promotion> filteredPromotions = FXCollections.observableArrayList();

    private static final String ORANGE    = "#F39C12";
    private static final String BLEU_NUIT = "#2C3E50";
    private static final String TURQUOISE = "#1ABC9C";
    private static final String ROUGE     = "#E53E3E";
    private static final String BG        = "#eef2f6";
    private static final String GRAY      = "#64748B";
    private static final int    COLS      = 4;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        promotionService = new PromotionService();
        targetService    = new PromotionTargetService();
        offresService    = Offresservice.getInstance();  // ✅ FIXED
        promoCodeService = new PromoCodeService();
        trendingService  = TrendingService.getInstance();
        notifService     = NotificationService.getInstance();

        // ── Inject current session user into header (fixes stale-user bug) ──
        if (labelUserName != null) {
            labelUserName.setText(SessionManager.getCurrentUserName());
        }
        if (labelUserEmail != null) {
            labelUserEmail.setText(SessionManager.getCurrentUserEmail());
        }
        // Legacy Text node - kept for backward compat
        if (userNameText != null) {
            userNameText.setText(SessionManager.getCurrentUserName());
        }

        // ── Language selector setup ──
        //if (languageSelector != null) {
            //languageSelector.getItems().addAll("FR", "EN");
            //languageSelector.setValue("FR");
            // Wire language switch here when i18n is ready:
            //languageSelector.setOnAction(e -> switchLanguage(languageSelector.getValue()));
       // }

        // ── Header navigation button handlers ──
        if (btnAccueil != null) {
            btnAccueil.setOnAction(e -> handleAccueil());
        }
        if (btnProfil != null) {
            btnProfil.setOnAction(e -> handleProfil());
        }

        setupGrid();
        setupFilters();
        loadPromotions();
        setupListeners();
        Platform.runLater(this::loadSidebar);

        Platform.runLater(() -> { if (promotionsGrid != null) AnimationHelper.fadeIn(promotionsGrid); });
    }

    private void setupGrid() {
        if (promotionsGrid == null) return;
        promotionsGrid.getColumnConstraints().clear();
        promotionsGrid.setHgap(16);
        promotionsGrid.setVgap(16);
        for (int i = 0; i < COLS; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / COLS);
            cc.setHgrow(Priority.ALWAYS);
            promotionsGrid.getColumnConstraints().add(cc);
        }
    }

    private void setupFilters() {
        packCheckbox.setSelected(true); individuCheckbox.setSelected(true); activeCheckbox.setSelected(true);
        packCheckbox.setOnAction(e    -> applyFilters());
        individuCheckbox.setOnAction(e -> applyFilters());
        activeCheckbox.setOnAction(e  -> applyFilters());
        sortCombo.setItems(FXCollections.observableArrayList(
                "🔥 Tendance d'abord", "Plus récentes", "Réduction croissante", "Réduction décroissante"));
        sortCombo.setValue("🔥 Tendance d'abord");
        sortCombo.setOnAction(e -> { sortPromotions(); displayPromotions(); });
    }

    private void setupListeners() {
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
    }

    // ════════════════════════════════════════════════════
    // SIDEBAR — loads dynamically for the logged-in user
    // ════════════════════════════════════════════════════

    private void loadSidebar() {
        int userId = SessionManager.getCurrentUserId();
        String userName = SessionManager.getCurrentUserName();

        // ── Mon Profil ──
        if (sidebarUserName != null)
            sidebarUserName.setText(userName);

        ReservationPromoService resaService = new ReservationPromoService();
        List<ReservationPromo> myResa = resaService.getByUserId(userId);

        if (sidebarReservCount != null)
            sidebarReservCount.setText(myResa.size() + " réservation(s)");

        if (loyaltyBadge != null) {
            String badge = myResa.size() >= 10 ? "🥇 Expert"
                    : myResa.size() >= 5  ? "🥈 Régulier"
                    : "🥉 Débutant";
            loyaltyBadge.setText(badge);
        }

        // ── En Tendance — show top 3 by score (not just strict isTrending threshold) ──
        if (sidebarTrendingBox != null) {
            // Remove anything after the Separator (index 0 = label, 1 = separator)
            while (sidebarTrendingBox.getChildren().size() > 2)
                sidebarTrendingBox.getChildren().remove(2);

            // Sort ALL promos by score descending, take top 3
            List<Promotion> sorted = trendingService.sortWithTrendingFirst(new ArrayList<>(allPromotions));
            List<Promotion> topTrending = sorted.stream()
                    .filter(p -> trendingService.getTrendingScore(p) > 0)
                    .limit(3)
                    .toList();

            if (topTrending.isEmpty()) {
                Label none = new Label("Aucune promotion en tendance");
                none.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
                sidebarTrendingBox.getChildren().add(none);
            } else {
                for (Promotion p : topTrending) {
                    boolean isTrend = trendingService.isTrending(p);
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-background-color: " + (isTrend ? "#FFF5F5" : "#F9FAFB") +
                            "; -fx-padding: 7 10; -fx-background-radius: 8; -fx-cursor: hand;");

                    Label flame = new Label(isTrend ? "🔥" : "📈");
                    flame.setStyle("-fx-font-size: 13px;");

                    VBox info = new VBox(1);
                    Label name = new Label(p.getName());
                    name.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #2C3E50;");
                    name.setWrapText(true);
                    name.setMaxWidth(160);

                    String scoreLabel = trendingService.getScoreLabel(p);
                    String scoreColor = trendingService.getScoreColor(p);
                    Label scoreLbl = new Label(scoreLabel + " · score " +
                            String.format("%.0f", trendingService.getTrendingScore(p)));
                    scoreLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: " + scoreColor + "; -fx-font-weight: 600;");

                    info.getChildren().addAll(name, scoreLbl);
                    row.getChildren().addAll(flame, info);
                    sidebarTrendingBox.getChildren().add(row);
                }
            }
        }

        // ── Prochaines Réservations — next upcoming (today or future) ──
        if (sidebarUpcomingBox != null) {
            while (sidebarUpcomingBox.getChildren().size() > 2)
                sidebarUpcomingBox.getChildren().remove(2);

            LocalDate today = LocalDate.now();
            List<ReservationPromo> upcoming = myResa.stream()
                    .filter(r -> !r.getDateDebutReservation().toLocalDate().isBefore(today))
                    .sorted(Comparator.comparing(r -> r.getDateDebutReservation().toLocalDate()))
                    .limit(3)
                    .toList();

            if (upcoming.isEmpty()) {
                // Also show the 3 most recent past reservations if no upcoming
                List<ReservationPromo> recent = myResa.stream()
                        .sorted(Comparator.comparing((ReservationPromo r) ->
                                r.getDateDebutReservation().toLocalDate()).reversed())
                        .limit(3)
                        .toList();

                if (recent.isEmpty()) {
                    Label none = new Label("Aucune réservation");
                    none.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
                    sidebarUpcomingBox.getChildren().add(none);
                } else {
                    for (ReservationPromo r : recent) {
                        promotionService.getById(r.getPromotionId()).ifPresent(p -> {
                            sidebarUpcomingBox.getChildren().add(buildResaRow(p, r, false));
                        });
                    }
                }
            } else {
                for (ReservationPromo r : upcoming) {
                    promotionService.getById(r.getPromotionId()).ifPresent(p -> {
                        sidebarUpcomingBox.getChildren().add(buildResaRow(p, r, true));
                    });
                }
            }
        }
    }

    private VBox buildResaRow(Promotion p, ReservationPromo r, boolean upcoming) {
        VBox row = new VBox(3);
        row.setStyle("-fx-background-color: " + (upcoming ? "#EEF2F6" : "#F9FAFB") +
                "; -fx-padding: 8 10; -fx-background-radius: 8;");

        Label name = new Label((p.isPack() ? "🎁 " : "🎟 ") + p.getName());
        name.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #2C3E50;");
        name.setWrapText(true);
        name.setMaxWidth(200);

        Label date = new Label("📅 " + r.getDateDebutReservation() + " → " + r.getDateFinReservation());
        date.setStyle("-fx-font-size: 9px; -fx-text-fill: #64748B;");

        String prixStr = String.format("%.2f TND", r.getMontantTotal());
        Label prix = new Label("💰 " + prixStr);
        prix.setStyle("-fx-font-size: 9px; -fx-text-fill: #1ABC9C; -fx-font-weight: 600;");

        row.getChildren().addAll(name, date, prix);
        return row;
    }

    private void loadPromotions() {
        allPromotions.addAll(promotionService.getAll());
        filteredPromotions.addAll(allPromotions);
        sortPromotions(); displayPromotions(); updateResultsCount();
    }

    private void applyFilters() {
        String kw = searchField.getText().toLowerCase();
        boolean showPacks  = packCheckbox.isSelected();
        boolean showIndiv  = individuCheckbox.isSelected();
        boolean onlyActive = activeCheckbox.isSelected();
        LocalDate today    = LocalDate.now();
        filteredPromotions.clear();
        for (Promotion p : allPromotions) {
            boolean mS = kw.isEmpty() || p.getName().toLowerCase().contains(kw) || p.getDescription().toLowerCase().contains(kw);
            boolean mT = (p.isPack() && showPacks) || (!p.isPack() && showIndiv);
            boolean mA = !onlyActive || (!p.getStartDate().toLocalDate().isAfter(today) && !p.getEndDate().toLocalDate().isBefore(today));
            if (mS && mT && mA) filteredPromotions.add(p);
        }
        sortPromotions(); displayPromotions(); updateResultsCount();
    }

    private void sortPromotions() {
        String sort = sortCombo.getValue(); if (sort == null) sort = "🔥 Tendance d'abord";
        switch (sort) {
            case "Réduction croissante"   -> filteredPromotions.sort((a, b) -> Float.compare(a.getDiscountPercentage() != null ? a.getDiscountPercentage() : 0f, b.getDiscountPercentage() != null ? b.getDiscountPercentage() : 0f));
            case "Réduction décroissante" -> filteredPromotions.sort((a, b) -> Float.compare(b.getDiscountPercentage() != null ? b.getDiscountPercentage() : 0f, a.getDiscountPercentage() != null ? a.getDiscountPercentage() : 0f));
            case "Plus récentes"          -> filteredPromotions.sort((a, b) -> b.getStartDate().compareTo(a.getStartDate()));
            default -> filteredPromotions.setAll(trendingService.sortWithTrendingFirst(new ArrayList<>(filteredPromotions)));
        }
    }

    // ════════════════════════════════════════════════════
    // DISPLAY
    // ════════════════════════════════════════════════════

    private void displayPromotions() {
        promotionsGrid.getChildren().clear(); promotionsGrid.getRowConstraints().clear();
        if (filteredPromotions.isEmpty()) {
            VBox empty = new VBox(15); empty.setAlignment(Pos.CENTER); empty.setPrefSize(600, 300);
            Label icon = new Label("🎁"); icon.setStyle("-fx-font-size: 56px;");
            Label msg  = new Label("Aucune promotion trouvée"); msg.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + GRAY + ";");
            empty.getChildren().addAll(icon, msg);
            GridPane.setColumnSpan(empty, COLS); promotionsGrid.add(empty, 0, 0); return;
        }
        List<VBox> cards = new ArrayList<>();
        for (int i = 0; i < filteredPromotions.size(); i++) {
            VBox card = createCard(filteredPromotions.get(i));
            card.setMaxWidth(Double.MAX_VALUE);
            promotionsGrid.add(card, i % COLS, i / COLS);
            cards.add(card);
        }
        AnimationHelper.staggeredFadeIn(cards, 70);
        PauseTransition pt = new PauseTransition(Duration.millis(cards.size() * 70L + 400));
        pt.setOnFinished(e -> pulseTrendingBadges());
        pt.play();
    }

    // ════════════════════════════════════════════════════
    // CARD
    // ════════════════════════════════════════════════════

    private VBox createCard(Promotion promo) {
        boolean trending = trendingService.isTrending(promo);

        // ✅ FIXED: Increment nb_vues when USER sees the card in FrontOffice.
        // This is the correct place — only real user interactions count.
        promotionService.incrementVues(promo.getId());
        promo.setNbVues(promo.getNbVues() + 1);

        VBox card = new VBox(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setCursor(javafx.scene.Cursor.HAND);
        applyCardStyle(card, trending);

        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 14;" +
                    "-fx-border-color: " + (trending ? ROUGE : ORANGE) + "; -fx-border-radius: 14; -fx-border-width: 2;" +
                    "-fx-effect: dropshadow(gaussian, rgba(243,156,18,0.3), 18, 0, 0, 6); -fx-translate-y: -3;");
            new Pulse(card).play();
        });
        card.setOnMouseExited(e -> applyCardStyle(card, trending));

        // Header
        StackPane header = new StackPane();
        header.setPrefHeight(130);
        header.setStyle((trending
                ? "-fx-background-color: linear-gradient(to bottom right, #E53E3E, #F39C12);"
                : promo.isPack()
                ? "-fx-background-color: linear-gradient(to bottom right, #F39C12, #F7DC6F);"
                : "-fx-background-color: linear-gradient(to bottom right, #1ABC9C, #2C3E50);")
                + " -fx-background-radius: 14 14 0 0;");

        Label bigIcon = new Label(trending ? "🔥" : (promo.isPack() ? "📦" : "🎁"));
        bigIcon.setStyle("-fx-font-size: 42px;");
        header.getChildren().add(bigIcon);

        if (trending) {
            Label tb = new Label("🔥 TRENDING");
            tb.setId("trendingBadge");
            tb.setStyle("-fx-background-color: rgba(0,0,0,0.35); -fx-text-fill: white;" +
                    "-fx-font-weight: 900; -fx-font-size: 9px; -fx-padding: 4 10; -fx-background-radius: 20;");
            StackPane.setAlignment(tb, Pos.TOP_LEFT);
            StackPane.setMargin(tb, new Insets(8));
            header.getChildren().add(tb);
        } else if (promo.isLocked()) {
            Label lb = new Label("🔒 Exclusif");
            lb.setStyle("-fx-background-color: " + BLEU_NUIT + "; -fx-text-fill: white;" +
                    "-fx-font-weight: bold; -fx-font-size: 9px; -fx-padding: 3 8; -fx-background-radius: 20;");
            StackPane.setAlignment(lb, Pos.TOP_LEFT);
            StackPane.setMargin(lb, new Insets(8));
            header.getChildren().add(lb);
        }

        Label typeBadge = new Label(promo.isPack() ? "📦 PACK" : "🎁 PROMO");
        typeBadge.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-text-fill: " +
                (promo.isPack() ? ORANGE : TURQUOISE) +
                "; -fx-font-weight: bold; -fx-font-size: 9px; -fx-padding: 3 8; -fx-background-radius: 20;");
        StackPane.setAlignment(typeBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(typeBadge, new Insets(8));
        header.getChildren().add(typeBadge);

        // Content
        VBox content = new VBox(7);
        content.setPadding(new Insets(12, 14, 12, 14));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 14 14;");

        Label nameLabel = new Label(promo.getName());
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + BLEU_NUIT + ";");
        nameLabel.setWrapText(true);

        String desc = promo.getDescription();
        if (desc != null && desc.length() > 55) desc = desc.substring(0, 55) + "…";
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + GRAY + ";");
        descLabel.setWrapText(true);

        HBox discountRow = new HBox(6); discountRow.setAlignment(Pos.CENTER_LEFT);
        if (promo.getDiscountPercentage() != null) {
            Label d = new Label("-" + String.format("%.0f", promo.getDiscountPercentage()) + "%");
            d.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: " + ORANGE + ";");
            discountRow.getChildren().add(d);
        }
        if (promo.getDiscountFixed() != null) {
            Label d = new Label("-" + String.format("%.0f", promo.getDiscountFixed()) + " TND");
            d.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #E67E22;");
            discountRow.getChildren().add(d);
        }

        // Show pack offer types as small chips
        if (promo.isPack()) {
            List<PromotionTarget> targets = targetService.getByPromotionId(promo.getId());
            if (!targets.isEmpty()) {
                HBox chipsRow = new HBox(5); chipsRow.setAlignment(Pos.CENTER_LEFT);
                Set<TargetType> seen = new HashSet<>();
                for (PromotionTarget t : targets) {
                    if (seen.add(t.getTargetType())) {
                        String chip = switch (t.getTargetType()) {
                            case HEBERGEMENT -> "🏨";
                            case ACTIVITE    -> "🎯";
                            case VOITURE     -> "🚗";
                        };
                        Label chipLbl = new Label(chip);
                        chipLbl.setStyle("-fx-background-color: " + BG + "; -fx-padding: 2 6;" +
                                "-fx-background-radius: 8; -fx-font-size: 11px;");
                        chipsRow.getChildren().add(chipLbl);
                    }
                }
                content.getChildren().add(chipsRow);
            }
        }

        Label datesLabel = new Label("📅 " + promo.getStartDate() + " → " + promo.getEndDate());
        datesLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: " + GRAY + ";");

        Separator sep = new Separator(); sep.setStyle("-fx-border-color: #F1F5F9;");
        Button btn = buildActionButton(promo, trending);

        content.getChildren().addAll(nameLabel, descLabel, discountRow, datesLabel, sep, btn);
        card.getChildren().addAll(header, content);
        return card;
    }

    private void applyCardStyle(VBox card, boolean trending) {
        card.setStyle(trending
                ? "-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #F39C12;" +
                "-fx-border-radius: 14; -fx-border-width: 2; -fx-effect: dropshadow(gaussian, rgba(243,156,18,0.3), 14, 0, 0, 4);"
                : "-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #E2E8F0;" +
                "-fx-border-radius: 14; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 3);"
        );
    }

    private Button buildActionButton(Promotion promo, boolean trending) {
        boolean expired = promo.getEndDate().toLocalDate().isBefore(LocalDate.now());
        Button btn;
        if (expired) {
            btn = new Button("Promotion expirée"); btn.setDisable(true);
            btn.setStyle("-fx-background-color: #94A3B8; -fx-text-fill: white; -fx-font-weight: bold;" +
                    "-fx-padding: 9 14; -fx-background-radius: 8; -fx-font-size: 11px;");
        } else if (promo.isLocked()) {
            btn = new Button("🔒 Débloquer");
            btn.setStyle("-fx-background-color: " + BLEU_NUIT + "; -fx-text-fill: white; -fx-font-weight: bold;" +
                    "-fx-padding: 9 14; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 11px;");
            btn.setOnAction(e -> showPromoCodeDialog(promo));
        } else {
            btn = new Button(trending ? "🔥 Réserver" : "Réserver ➜");
            btn.setStyle("-fx-background-color: " + (trending ? ROUGE : TURQUOISE) + "; -fx-text-fill: white;" +
                    "-fx-font-weight: bold; -fx-padding: 9 14; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 11px;");
            btn.setOnAction(e -> openReservationDialog(promo));
        }
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnMouseEntered(e -> new Pulse(btn).play());
        return btn;
    }

    private void pulseTrendingBadges() {
        promotionsGrid.lookupAll("#trendingBadge").forEach(node -> {
            Timeline loop = new Timeline(
                    new KeyFrame(Duration.millis(0),    e -> new Pulse(node).play()),
                    new KeyFrame(Duration.millis(2500))
            );
            loop.setCycleCount(Animation.INDEFINITE); loop.play();
        });
    }

    // ════════════════════════════════════════════════════
    // DIALOGS
    // ════════════════════════════════════════════════════

    private void showPromoCodeDialog(Promotion promo) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Débloquer : " + promo.getName());
        dialog.setResizable(false);

        VBox root = new VBox(18); root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: " + BG + "; -fx-padding: 30;");
        root.setPrefWidth(400);

        Label lockIcon = new Label("🔒"); lockIcon.setStyle("-fx-font-size: 48px;");
        Label title    = new Label("Promotion Verrouillée");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: " + BLEU_NUIT + ";");
        Label subtitle = new Label(promo.getName()); subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: " + GRAY + ";");
        Label info = new Label("Saisissez votre code promo pour accéder\nà cette promotion exclusive.");
        info.setStyle("-fx-font-size: 12px; -fx-text-fill: " + GRAY + "; -fx-text-alignment: center;");
        info.setAlignment(Pos.CENTER);

        TextField codeField = new TextField();
        codeField.setPromptText("Ex: RE7LA-A3F9-B2K1");
        codeField.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 12 15;" +
                "-fx-background-radius: 12; -fx-border-color: #E2E8F0; -fx-border-radius: 12;" +
                "-fx-text-fill: " + BLEU_NUIT + "; -fx-font-family: monospace; -fx-alignment: CENTER;");
        codeField.setMaxWidth(280);
        codeField.textProperty().addListener((obs, o, n) -> { if (!n.equals(n.toUpperCase())) codeField.setText(n.toUpperCase()); });

        Label errorLabel = new Label(""); errorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + ROUGE + "; -fx-font-weight: 600;");
        errorLabel.setVisible(false);

        HBox buttons = new HBox(12); buttons.setAlignment(Pos.CENTER);
        Button btnA = new Button("Annuler");
        btnA.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: " + BLEU_NUIT + "; -fx-font-weight: 600; -fx-padding: 10 24; -fx-background-radius: 20; -fx-cursor: hand;");
        btnA.setOnAction(e -> dialog.close());
        Button btnV = new Button("✓  Valider");
        btnV.setStyle("-fx-background-color: " + ORANGE + "; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 10 24; -fx-background-radius: 20; -fx-cursor: hand;");
        btnV.setOnAction(e -> {
            String s = codeField.getText().trim();
            if (s.isEmpty()) { errorLabel.setText("⚠ Veuillez saisir un code."); errorLabel.setVisible(true); AnimationHelper.shake(codeField); return; }
            if (promoCodeService.validateCode(s, promo.getId())) { dialog.close(); openReservationDialog(promo); }
            else { errorLabel.setText("❌ Code invalide ou déjà utilisé."); errorLabel.setVisible(true); AnimationHelper.shake(codeField); }
        });
        codeField.setOnAction(e -> btnV.fire());
        buttons.getChildren().addAll(btnA, btnV);

        root.getChildren().addAll(lockIcon, title, subtitle, info, codeField, errorLabel, buttons);
        dialog.setScene(new Scene(root)); dialog.show();
        new BounceIn(lockIcon).play(); new SlideInDown(title).play();
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
            // Refresh promotions AND sidebar after closing
            allPromotions.clear(); allPromotions.addAll(promotionService.getAll()); applyFilters();
            Platform.runLater(this::loadSidebar);
        } catch (IOException e) {
            notifService.danger("Erreur", "Impossible d'ouvrir le formulaire de réservation.");
        }
    }

    private void updateResultsCount() {
        int c = filteredPromotions.size();
        long t = filteredPromotions.stream().filter(trendingService::isTrending).count();
        String txt = c + " promotion" + (c > 1 ? "s" : "") + " trouvée" + (c > 1 ? "s" : "");
        if (t > 0) txt += " · 🔥 " + t + " en tendance";
        resultsCountText.setText(txt);
    }

    @FXML private void resetFilters() {
        searchField.clear(); packCheckbox.setSelected(true); individuCheckbox.setSelected(true);
        activeCheckbox.setSelected(true); sortCombo.setValue("🔥 Tendance d'abord"); applyFilters();
    }

    @FXML private void handleRefresh() {
        int countBefore = allPromotions.size();
        allPromotions.clear();
        allPromotions.addAll(promotionService.getAll());
        int countAfter = allPromotions.size();
        applyFilters();
        Platform.runLater(this::loadSidebar);
        int newCount = countAfter - countBefore;
        if (newCount > 0) {
            showNewPromoNotification(newCount);
        }
    }

    private void showNewPromoNotification(int count) {
        // Create a toast-style notification bar
        HBox toast = new HBox(10);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setStyle("-fx-background-color: #2C3E50; -fx-padding: 12 20; -fx-background-radius: 10;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 14, 0, 0, 4);");
        toast.setMaxWidth(380);

        Label icon = new Label("🔔");
        icon.setStyle("-fx-font-size: 16px;");
        Label msg = new Label(count + " nouvelle" + (count > 1 ? "s" : "") + " promotion" + (count > 1 ? "s" : "") + " disponible" + (count > 1 ? "s" : "") + " !");
        msg.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: white;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8;" +
                "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 2 6;");

        toast.getChildren().addAll(icon, msg, sp, btnClose);

        // Add to top-right of the promotions grid area by overlaying
        StackPane overlay = new StackPane(toast);
        overlay.setAlignment(Pos.TOP_RIGHT);
        overlay.setStyle("-fx-padding: 0 20 0 0;");
        overlay.setPickOnBounds(false);
        overlay.setMouseTransparent(false);

        // Insert above the grid
        if (promotionsGrid.getParent() instanceof ScrollPane sp2 &&
                sp2.getParent() instanceof VBox vb) {
            vb.getChildren().add(0, overlay);
            btnClose.setOnAction(e -> vb.getChildren().remove(overlay));

            // Auto-remove after 5 seconds
            PauseTransition pause = new PauseTransition(Duration.seconds(5));
            pause.setOnFinished(e -> {
                FadeTransition fade = new FadeTransition(Duration.millis(400), overlay);
                fade.setFromValue(1); fade.setToValue(0);
                fade.setOnFinished(ev -> vb.getChildren().remove(overlay));
                fade.play();
            });
            pause.play();
            new animatefx.animation.SlideInDown(toast).play();
        }
    }

    @FXML private void handleMesReservations() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/frontoffice/MesReservations.fxml"));
            Stage s = new Stage(); s.setTitle("Mes Réservations"); s.setScene(new Scene(root, 1000, 700)); s.show();
            new FadeIn(root).play();
        } catch (IOException e) { notifService.danger("Erreur", "Impossible d'ouvrir Mes Réservations."); }
    }

    // ── Header button handlers ──

    @FXML
    private void handleAccueil() {
        // Integration point: navigate to Accueil / home view
        // Example: MainApp.showAccueil();
    }

    @FXML
    private void handleProfil() {
        // Integration point: open profile page
        // Example: MainApp.showProfil(SessionManager.getCurrentUserId());
    }

    @FXML
    private void applyFiltersAction() {
        applyFilters();
    }
}
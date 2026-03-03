package org.example.Controllers.communaute;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.Entites.communaute.Commentaire;
import org.example.Entites.communaute.Publication;
import org.example.Entites.communaute.StatutP;
import org.example.Entites.communaute.TypeCible;
import org.example.Entites.user.Role;
import org.example.Entites.user.User;
import org.example.Services.communaute.CommentaireCRUD;
import org.example.Services.communaute.PublicationCRUD;
import org.example.Services.communaute.ReactionAnalyticsService;
import org.example.Services.communaute.ReactionAnalyticsService.Reaction;
import org.example.Utils.UserSession;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CommunauteController implements Initializable {

    // ================= HEADER =================
    @FXML private TextField searchField;
    @FXML private Button btnMesReservations;
    @FXML private ImageView logoImageView;
    @FXML private HBox userHeader;

    // ================= PUBLICATIONS AREA =================
    @FXML private VBox publicationsFlowPane;
    @FXML private Label resultsCountText;
    @FXML private ComboBox<String> sortCombo;
    @FXML private TextField pubSearchField;
    @FXML private Button btnClearSearch;

    // ── Active filter state ─────────────────────────────────
    private String activeSearch = "";

    // ================= ANALYTICS SIDEBAR =================
    @FXML private VBox trendingPanel;
    @FXML private Label trendingTitle;
    @FXML private Label trendingDesc;
    @FXML private Label mostReactedLabel;
    @FXML private Label mostCommentedLabel;

    // ───────────────────────────────────────────────────────
    //  State
    // ───────────────────────────────────────────────────────
    private List<Publication> allPublications = new ArrayList<>();
    private final ReactionAnalyticsService analytics = ReactionAnalyticsService.getInstance();
    private final Map<Integer, Integer> commentCounts = new HashMap<>();

    private ChatbotController chatbot;
    private GlobalChatController globalChat;

    @FXML private Button btnOpenChat;
    @FXML private StackPane rootStackPane;

    private final CommentaireCRUD commentDAO = new CommentaireCRUD();

    // ================= UTILISATEUR CONNECTÉ (JWT) =================
    private User currentUser;
    private boolean isAdmin = false;

    // ───────────────────────────────────────────────────────
    //  BAD WORDS
    // ───────────────────────────────────────────────────────
    private static final List<String> BAD_WORDS = List.of("kalb", "couchon", "bhim", "merde", "putain", "con", "salope");

    // ───────────────────────────────────────────────────────
    //  INITIALIZE
    // ───────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // ✅ Vérifier l'authentification JWT
        if (!checkUserAuth()) {
            return;
        }

        System.out.println("✅ Front-office - Utilisateur: " + currentUser.getPrenom() + " " + currentUser.getNom());
        System.out.println("✅ Rôle (enum): " + currentUser.getRole());

        // ✅ Vérifier si l'utilisateur est admin (correction pour enum Role)
        isAdmin = false;
        if (currentUser.getRole() != null) {
            // Comparaison directe avec l'enum
            isAdmin = currentUser.getRole() == Role.admin;
            System.out.println("🔍 Rôle exact: " + currentUser.getRole());
        } else {
            System.out.println("⚠️ Rôle est NULL");
        }
        System.out.println("✅ Accès admin: " + isAdmin);

        // ── Circular logo ──────────────────────────────────
        if (logoImageView != null) {
            try {
                InputStream is = getClass().getResourceAsStream("/img/Re7la.jpeg");
                if (is == null) is = getClass().getResourceAsStream("/resources/img/Re7la.jpeg");
                if (is != null) {
                    Image img = new Image(is);
                    logoImageView.setImage(img);
                    Circle clip = new Circle(41, 41, 41);
                    logoImageView.setClip(clip);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Erreur chargement logo: " + e.getMessage());
            }
        }

        // Mettre à jour l'affichage du nom utilisateur
        updateUserHeader();

        loadPublications();
        setupSortListener();
        setupSearchListener();

        Platform.runLater(() -> {
            try {
                globalChat = new GlobalChatController(currentUser.getPrenom() + " " + currentUser.getNom(), btnOpenChat);
                globalChat.inject(rootStackPane);
            } catch (Exception e) {
                System.err.println("⚠️ Erreur initialisation chat: " + e.getMessage());
            }
        });
    }

    /**
     * ✅ Vérifie si l'utilisateur est authentifié via JWT
     */
    private boolean checkUserAuth() {
        if (!UserSession.getInstance().isTokenValid()) {
            showAlert("Session expirée", "Votre session a expiré. Veuillez vous reconnecter.");
            redirectToLogin();
            return false;
        }

        currentUser = UserSession.getInstance().getCurrentUser();

        if (currentUser == null) {
            showAlert("Erreur", "Aucun utilisateur connecté.");
            redirectToLogin();
            return false;
        }

        return true;
    }

    /**
     * ✅ Met à jour l'affichage du nom de l'utilisateur dans le header
     */
    private void updateUserHeader() {
        if (userHeader != null) {
            // Chercher le Text à l'intérieur du HBox
            for (javafx.scene.Node node : userHeader.getChildren()) {
                if (node instanceof Text) {
                    Text userName = (Text) node;
                    userName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
                    break;
                }
            }
        }
    }

    /**
     * ✅ Redirige vers la page de login
     */
    private void redirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) (publicationsFlowPane != null ?
                    publicationsFlowPane.getScene().getWindow() :
                    btnOpenChat.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Erreur redirection login: " + e.getMessage());
        }
    }

    // ───────────────────────────────────────────────────────
    //  LOAD DATA
    // ───────────────────────────────────────────────────────
    private void loadPublications() {
        publicationsFlowPane.getChildren().clear();
        PublicationCRUD service = new PublicationCRUD();
        try {
            allPublications = service.afficherh();
            // Filtrer les publications selon le statut (si nécessaire)
            allPublications.removeIf(p -> p.getStatutP() == StatutP.MASQUE);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les publications: " + e.getMessage());
        }
        refreshCommentCounts();
        activeSearch = "";
        if (pubSearchField != null) pubSearchField.clear();
        if (btnClearSearch != null) {
            btnClearSearch.setVisible(false);
            btnClearSearch.setManaged(false);
        }
        renderPublications(allPublications);
    }

    private void refreshCommentCounts() {
        commentCounts.clear();
        for (Publication p : allPublications) {
            try {
                commentCounts.put(p.getIdPublication(),
                        commentDAO.getCommentsByPublicationId(p.getIdPublication()).size());
            } catch (SQLException ignored) {
                commentCounts.put(p.getIdPublication(), 0);
            }
        }
    }

    // ───────────────────────────────────────────────────────
    //  CREATE PUBLICATION CARD
    // ───────────────────────────────────────────────────────
    private VBox createPublicationCard(Publication p) {

        boolean isTrending = analytics.getTrendingPublicationId(commentCounts) == p.getIdPublication();

        // ── Outer card shell ────────────────────────────────
        VBox card = new VBox(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:20;" +
                        "-fx-effect:dropshadow(gaussian,rgba(44,62,80,0.11),16,0,0,5);" +
                        "-fx-border-color:" + (isTrending ? "#F39C12" : "#EEF2F6") + ";" +
                        "-fx-border-radius:20;" +
                        "-fx-border-width:" + (isTrending ? "2" : "1.5") + ";"
        );

        // ══════════════════════════════════════════════════════
        //  1. IMAGE BANNER
        // ══════════════════════════════════════════════════════
        StackPane imagePane = new StackPane();
        imagePane.setPrefHeight(220);
        imagePane.setMaxWidth(Double.MAX_VALUE);

        // Photo
        ImageView img = new ImageView();
        img.setFitHeight(220);
        img.setFitWidth(800);
        img.setPreserveRatio(false);
        img.setSmooth(true);

        try {
            InputStream imgStream = getClass().getResourceAsStream("/img/" + p.getImgURL());
            if (imgStream != null) {
                img.setImage(new Image(imgStream));
            } else {
                InputStream ph = getClass().getResourceAsStream("/img/default.jpg");
                if (ph != null) img.setImage(new Image(ph));
            }
        } catch (Exception e) {
            // Image par défaut silencieuse
        }

        Rectangle imgClip = new Rectangle(800, 220);
        imgClip.setArcWidth(20);
        imgClip.setArcHeight(20);
        img.setClip(imgClip);

        // Gradient veil
        Rectangle veil = new Rectangle(800, 220);
        veil.setArcWidth(20);
        veil.setArcHeight(20);
        veil.setFill(new LinearGradient(
                0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new Stop(0.30, Color.TRANSPARENT),
                new Stop(1.00, Color.rgb(15, 23, 42, 0.80))
        ));

        // Type badge
        String typeColor, typeIcon;
        switch (p.getTypeCible()) {
            case HEBERGEMENT:
                typeColor = "#1ABC9C";
                typeIcon = "🏨  Hébergement";
                break;
            case ACTIVITE:
                typeColor = "#3B82F6";
                typeIcon = "🎯  Activité";
                break;
            case TRANSPORT:
                typeColor = "#F39C12";
                typeIcon = "🚌  Transport";
                break;
            default:
                typeColor = "#64748B";
                typeIcon = p.getTypeCible().name();
        }
        Label typeBadge = new Label(typeIcon);
        typeBadge.setStyle(
                "-fx-background-color:" + typeColor + ";" +
                        "-fx-text-fill:white;-fx-font-weight:800;-fx-font-size:11px;" +
                        "-fx-padding:5 13;-fx-background-radius:20;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.28),6,0,0,2);"
        );
        HBox topRightBox = new HBox(typeBadge);
        topRightBox.setPadding(new Insets(13, 14, 0, 14));
        topRightBox.setAlignment(Pos.TOP_RIGHT);
        StackPane.setAlignment(topRightBox, Pos.TOP_RIGHT);

        // ── Author chip ──────────────────────────────────────
        // Avatar
        StackPane avatarStack = new StackPane();
        Circle ring = new Circle(18);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.web("#F39C12"));
        ring.setStrokeWidth(2.5);
        ring.setEffect(new Glow(0.6));

        ImageView uAvatar = new ImageView();
        uAvatar.setFitWidth(32);
        uAvatar.setFitHeight(32);
        Circle avatarClip = new Circle(16, 16, 16);
        uAvatar.setClip(avatarClip);

        try {
            InputStream uStream = getClass().getResourceAsStream("/img/UserIconPlaceholder.jpg");
            if (uStream != null) uAvatar.setImage(new Image(uStream));
        } catch (Exception e) {
            // Ignorer
        }

        avatarStack.getChildren().addAll(ring, uAvatar);

        // Nom d'utilisateur
        Text uNameText = new Text(p.getId_utilisateur() != null ?
                p.getId_utilisateur().getPrenom() + " " + p.getId_utilisateur().getNom() : "Utilisateur inconnu");
        uNameText.setStyle("-fx-font-weight:800;-fx-font-size:13px;");
        uNameText.setFill(new LinearGradient(
                0, 0, 1, 0, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#2C3E50")),
                new Stop(1.0, Color.web("#F39C12"))
        ));

        Label dateLbl = new Label("📅 " + (p.getDateCreation() != null ? p.getDateCreation() : "—"));
        dateLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#64748B;");

        VBox uTextCol = new VBox(1, uNameText, dateLbl);
        uTextCol.setAlignment(Pos.CENTER_LEFT);

        HBox authorChip = new HBox(8, avatarStack, uTextCol);
        authorChip.setAlignment(Pos.CENTER_LEFT);
        authorChip.setPadding(new Insets(6, 12, 6, 12));
        authorChip.setStyle(
                "-fx-background-color:rgba(30,58,138,0.18);" +
                        "-fx-background-radius:30;" +
                        "-fx-border-color:rgba(99,162,255,0.25);" +
                        "-fx-border-radius:30;" +
                        "-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(30,58,138,0.18),8,0,0,2);"
        );

        imagePane.getChildren().addAll(img, veil, topRightBox);

        // ══════════════════════════════════════════════════════
        //  2. BODY
        // ══════════════════════════════════════════════════════
        VBox body = new VBox(11);
        body.setPadding(new Insets(16, 18, 8, 18));

        // Description
        HBox descRow = new HBox(10);
        descRow.setAlignment(Pos.TOP_LEFT);
        Rectangle accentBar = new Rectangle(3, 42);
        accentBar.setFill(new LinearGradient(
                0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#F39C12")),
                new Stop(1, Color.web("#1ABC9C"))
        ));
        accentBar.setArcWidth(3);
        accentBar.setArcHeight(3);

        Label desc = new Label(truncate(p.getDescriptionP(), 140));
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size:13px;-fx-text-fill:#334155;-fx-line-spacing:2;");
        desc.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(desc, Priority.ALWAYS);
        descRow.getChildren().addAll(accentBar, desc);

        // Stats
        int totalReactions = analytics.getTotalReactions(p.getIdPublication());
        int totalComments = commentCounts.getOrDefault(p.getIdPublication(), 0);
        Optional<Reaction> dom = analytics.getDominantReaction(p.getIdPublication());

        HBox statsRow = new HBox(7);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.setOnMouseClicked(ev -> ev.consume());
        statsRow.getChildren().add(pill((dom.isPresent() ? dom.get().emoji : "❤️") + "  " + totalReactions, "#FFF7ED", "#F39C12", "#EA7C00"));
        statsRow.getChildren().add(pill("💬  " + totalComments, "#EFF6FF", "#3B82F6", "#2563EB"));
        if (dom.isPresent())
            statsRow.getChildren().add(pill("🏆 " + dom.get().label, "#F0FDF4", "#1ABC9C", "#0E9F82"));
        statsRow.setUserData("statsRow");

        body.getChildren().addAll(authorChip, descRow, statsRow);

        // ══════════════════════════════════════════════════════
        //  3. REACTION BAR
        // ══════════════════════════════════════════════════════
        HBox reactionBar = buildReactionBar(p.getIdPublication());
        reactionBar.setPadding(new Insets(4, 18, 4, 18));

        // ══════════════════════════════════════════════════════
        //  4. TRENDING BANNER
        // ══════════════════════════════════════════════════════
        if (isTrending) {
            HBox trendBanner = new HBox(8);
            trendBanner.setAlignment(Pos.CENTER);
            trendBanner.setPadding(new Insets(7, 0, 7, 0));
            trendBanner.setStyle(
                    "-fx-background-color:linear-gradient(to right,#FF5722,#F39C12,#FF5722);"
            );
            Text trendText = new Text("🔥  EN TENDANCE  •  Publication la plus populaire  🔥");
            trendText.setStyle("-fx-font-size:11px;-fx-font-weight:800;");
            trendText.setFill(Color.WHITE);
            trendBanner.getChildren().add(trendText);
            body.getChildren().add(0, trendBanner);
        }

        // ══════════════════════════════════════════════════════
        //  5. FOOTER
        // ══════════════════════════════════════════════════════
        HBox footer = buildCardFooter(p);

        card.getChildren().addAll(imagePane, body, reactionBar, footer);

        card.setOnMouseClicked(e -> openPublicationPopup(p));

        // Hover effect
        String baseStyle = card.getStyle();
        card.setOnMouseEntered(e -> {
            card.setStyle(baseStyle +
                    "-fx-effect:dropshadow(gaussian,rgba(243,156,18,0.28),22,0,0,7);" +
                    "-fx-border-color:#F39C12;-fx-translate-y:-3;");
        });
        card.setOnMouseExited(e -> {
            card.setStyle(baseStyle +
                    "-fx-effect:dropshadow(gaussian,rgba(44,62,80,0.11),16,0,0,5);" +
                    "-fx-translate-y:0;");
        });

        return card;
    }

    private HBox buildCardFooter(Publication p) {
        HBox footer = new HBox(8);
        footer.setPadding(new Insets(10, 14, 13, 14));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle(
                "-fx-background-color:#F8FAFC;" +
                        "-fx-background-radius:0 0 20 20;" +
                        "-fx-border-color:#EEF2F6;" +
                        "-fx-border-width:1 0 0 0;"
        );

        // Vérifier si l'utilisateur peut modifier/supprimer
        boolean canModify = isAdmin || (p.getId_utilisateur() != null &&
                p.getId_utilisateur().getId() == currentUser.getId());

        if (canModify) {
            Button editBtn = footerBtn("✏", "#EFF6FF", "#3B82F6");
            Button delBtn = footerBtn("🗑", "#FEF2F2", "#EF4444");
            editBtn.setOnAction(e -> {
                e.consume();
                showPublicationFormModal(p);
            });
            delBtn.setOnAction(e -> {
                e.consume();
                showDeletePublicationConfirm(p);
            });
            footer.getChildren().addAll(editBtn, delBtn);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button sumBtn = footerBtn("📝 Résumer", "#EEF2FF", "#6366F1");
        Button askBtn = footerBtn("🤖 IA", "#E0F7FA", "#0097A7");
        sumBtn.setOnAction(e -> {
            e.consume();
            openChatbotForSummary(p);
        });
        askBtn.setOnAction(e -> {
            e.consume();
            openChatbot();
        });

        footer.getChildren().addAll(spacer, sumBtn, askBtn);
        footer.setOnMouseClicked(ev -> ev.consume());
        return footer;
    }

    /** Small coloured stat pill */
    private Label pill(String text, String bg, String fg, String border) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-text-fill:" + fg + ";" +
                        "-fx-font-weight:700;-fx-font-size:11px;" +
                        "-fx-padding:4 10;-fx-background-radius:20;" +
                        "-fx-border-color:" + border + "22;" +
                        "-fx-border-radius:20;"
        );
        return l;
    }

    /** Small footer action button */
    private Button footerBtn(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-text-fill:" + fg + ";" +
                        "-fx-font-weight:700;-fx-font-size:12px;" +
                        "-fx-background-radius:20;-fx-padding:5 13;-fx-cursor:hand;"
        );
        b.setOnMouseEntered(e -> b.setStyle(b.getStyle() +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.12),6,0,0,2);-fx-scale-x:1.05;-fx-scale-y:1.05;"));
        b.setOnMouseExited(e -> b.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-text-fill:" + fg + ";" +
                        "-fx-font-weight:700;-fx-font-size:12px;" +
                        "-fx-background-radius:20;-fx-padding:5 13;-fx-cursor:hand;"
        ));
        return b;
    }

    // ═══════════════════════════════════════════════════════════
    //  MODAL ENGINE
    // ═══════════════════════════════════════════════════════════
    private StackPane showModal(VBox dialogContent) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:rgba(15,23,42,0.55);");
        overlay.setPickOnBounds(true);
        overlay.setOnMouseClicked(ev -> {
            if (ev.getTarget() == overlay) closeModal(overlay);
        });

        ScrollPane scroll = new ScrollPane(dialogContent);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;");
        scroll.setMaxHeight(720);
        scroll.setMinWidth(500);
        scroll.setMaxWidth(545);

        StackPane card = new StackPane(scroll);
        card.setMaxWidth(545);
        card.setStyle("-fx-background-color:white;-fx-background-radius:20;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.30),30,0,0,8);");
        card.setOnMouseClicked(ev -> ev.consume());

        overlay.getChildren().add(card);
        rootStackPane.getChildren().add(overlay);

        // Animate in
        overlay.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), overlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), card);
        scaleIn.setFromX(0.88);
        scaleIn.setFromY(0.88);
        scaleIn.setToX(1);
        scaleIn.setToY(1);
        scaleIn.play();
        return overlay;
    }

    private void closeModal(StackPane overlay) {
        FadeTransition ft = new FadeTransition(Duration.millis(150), overlay);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> rootStackPane.getChildren().remove(overlay));
        ft.play();
    }

    // ═══════════════════════════════════════════════════════════
    //  NEW / EDIT PUBLICATION FORM MODAL
    // ═══════════════════════════════════════════════════════════
    @FXML
    private void handleNewPublication() {
        showPublicationFormModal(null);
    }

    private void showPublicationFormModal(Publication existing) {
        boolean isEdit = existing != null;

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:white;-fx-background-radius:20;");

        // Header
        VBox header = new VBox(5);
        header.setPadding(new Insets(28, 30, 22, 30));
        header.setStyle("-fx-background-color:linear-gradient(to bottom right,#F39C12,#E67E22);" +
                "-fx-background-radius:20 20 0 0;");
        Label titleLbl = new Label(isEdit ? "✏  Modifier la publication" : "✨  Nouvelle publication");
        titleLbl.setStyle("-fx-font-size:20px;-fx-font-weight:800;-fx-text-fill:white;");
        Label subLbl = new Label(isEdit ? "Mettez à jour les informations" :
                "Partagez quelque chose avec la communauté RE7LA");
        subLbl.setStyle("-fx-font-size:13px;-fx-text-fill:rgba(255,255,255,0.85);");
        header.getChildren().addAll(titleLbl, subLbl);

        // Body
        VBox body = new VBox(18);
        body.setPadding(new Insets(28, 30, 28, 30));

        // Type section
        VBox typeSection = sectionBox("🏷  Type de publication", "#FFF7ED", "#FED7AA");
        ComboBox<TypeCible> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(TypeCible.values());
        typeCombo.setPromptText("Choisissez un type…");
        typeCombo.setMaxWidth(Double.MAX_VALUE);
        typeCombo.setStyle("-fx-background-color:white;-fx-background-radius:10;" +
                "-fx-border-color:#FED7AA;-fx-border-radius:10;-fx-font-size:13px;-fx-padding:4 8;");
        if (isEdit) typeCombo.setValue(existing.getTypeCible());
        typeSection.getChildren().add(typeCombo);

        // Description section
        VBox descSection = sectionBox("📝  Description", "#F0FDF4", "#BBF7D0");
        TextArea descArea = new TextArea(isEdit ? existing.getDescriptionP() : "");
        descArea.setPromptText("Décrivez votre publication…");
        descArea.setPrefRowCount(4);
        descArea.setWrapText(true);
        descArea.setStyle("-fx-background-color:white;-fx-background-radius:10;" +
                "-fx-border-color:#BBF7D0;-fx-border-radius:10;-fx-font-size:13px;-fx-padding:8;");
        descSection.getChildren().add(descArea);

        // Image section
        VBox imgSection = sectionBox("🖼  Image", "#EFF6FF", "#BFDBFE");
        final String[] selectedPath = {isEdit ? existing.getImgURL() : null};
        ImageView thumb = new ImageView();
        thumb.setFitWidth(60);
        thumb.setFitHeight(60);
        thumb.setPreserveRatio(true);
        Rectangle clip = new Rectangle(60, 60);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        thumb.setClip(clip);
        if (isEdit && existing.getImgURL() != null) {
            try {
                InputStream is = getClass().getResourceAsStream("/img/" + existing.getImgURL());
                if (is != null) thumb.setImage(new Image(is));
            } catch (Exception e) {
                // Ignorer
            }
        }
        Label fileNameLbl = new Label(selectedPath[0] != null ? selectedPath[0] : "Aucun fichier sélectionné");
        fileNameLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;-fx-font-style:italic;");
        Button chooseBtn = styledBtn("📎  Choisir",
                "-fx-background-color:linear-gradient(to right,#6366F1,#8B5CF6);-fx-text-fill:white;" +
                        "-fx-font-weight:bold;-fx-font-size:12px;-fx-background-radius:20;-fx-padding:7 16;" +
                        "-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.4),8,0,0,2);");
        chooseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir une image");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File f = fc.showOpenDialog(rootStackPane.getScene().getWindow());
            if (f != null) {
                selectedPath[0] = f.getName();
                fileNameLbl.setText(f.getName());
                thumb.setImage(new Image(f.toURI().toString()));
            }
        });
        HBox imgRow = new HBox(14, thumb, new VBox(4, fileNameLbl, chooseBtn));
        imgRow.setAlignment(Pos.CENTER_LEFT);
        imgSection.getChildren().add(imgRow);

        // Buttons
        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        Button cancelBtn = styledBtn("Annuler",
                "-fx-background-color:#F1F5F9;-fx-text-fill:#64748B;-fx-font-weight:bold;" +
                        "-fx-background-radius:12;-fx-padding:10 22;-fx-cursor:hand;");
        Button saveBtn = styledBtn(isEdit ? "💾  Enregistrer" : "🚀  Publier",
                "-fx-background-color:linear-gradient(to right,#F39C12,#E67E22);" +
                        "-fx-text-fill:white;-fx-font-weight:800;-fx-font-size:14px;" +
                        "-fx-background-radius:12;-fx-padding:10 28;-fx-cursor:hand;" +
                        "-fx-effect:dropshadow(gaussian,rgba(243,156,18,0.5),10,0,0,3);");
        btnRow.getChildren().addAll(cancelBtn, saveBtn);

        body.getChildren().addAll(typeSection, descSection, imgSection, btnRow);
        root.getChildren().addAll(header, body);

        StackPane overlay = showModal(root);

        cancelBtn.setOnAction(e -> closeModal(overlay));
        saveBtn.setOnAction(e -> {
            if (selectedPath[0] == null || descArea.getText().isBlank() || typeCombo.getValue() == null) {
                showInlineError(body, "⚠  Veuillez remplir tous les champs.");
                return;
            }
            if (containsBadWord(descArea.getText())) {
                showBadWordAlert();
                return;
            }
            try {
                PublicationCRUD svc = new PublicationCRUD();
                if (isEdit) {
                    existing.setImgURL(selectedPath[0]);
                    existing.setTypeCible(typeCombo.getValue());
                    existing.setDescriptionP(descArea.getText());
                    existing.setDateModif(LocalDateTime.now().toString());
                    svc.modifierh(existing);
                } else {
                    Publication n = new Publication();
                    n.setId_utilisateur(currentUser);
                    n.setImgURL(selectedPath[0]);
                    n.setTypeCible(typeCombo.getValue());
                    n.setDateCreation(LocalDateTime.now().toString());
                    n.setDateModif(LocalDateTime.now().toString());
                    n.setStatutP(StatutP.VALIDE);
                    n.setEstVerifie(false);
                    n.setDescriptionP(descArea.getText());
                    svc.ajouterh(n);
                }
                closeModal(overlay);
                loadPublications();
            } catch (SQLException ex) {
                showInlineError(body, "❌ Erreur BD : " + ex.getMessage());
            }
        });
    }

    private void showDeletePublicationConfirm(Publication p) {
        VBox root = confirmDialog("🗑  Supprimer la publication",
                "Êtes-vous sûr de vouloir supprimer cette publication ?\nCette action est irréversible.",
                "#EF4444", "#FEF2F2", "#FECACA", "Supprimer");
        StackPane overlay = showModal(root);
        VBox body = (VBox) root.getChildren().get(1);
        HBox btnRow = (HBox) body.getChildren().get(1);
        ((Button) btnRow.getChildren().get(0)).setOnAction(e -> closeModal(overlay));
        ((Button) btnRow.getChildren().get(1)).setOnAction(e -> {
            try {
                // Supprimer les commentaires d'abord
                List<Commentaire> comments = commentDAO.getCommentsByPublicationId(p.getIdPublication());
                for (Commentaire c : comments) commentDAO.supprimerh(c);
                new PublicationCRUD().supprimerh(p);
                closeModal(overlay);
                loadPublications();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Impossible de supprimer: " + ex.getMessage());
            }
        });
    }

    // ═══════════════════════════════════════════════════════════
    //  PUBLICATION DETAIL POPUP
    // ═══════════════════════════════════════════════════════════
    private void openPublicationPopup(Publication p) {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:white;-fx-background-radius:20;");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 16, 24));
        header.setStyle("-fx-background-color:#1E293B;-fx-background-radius:20 20 0 0;");

        ImageView uImg = new ImageView();
        uImg.setFitWidth(40);
        uImg.setFitHeight(40);
        Circle imgClip = new Circle(20, 20, 20);
        uImg.setClip(imgClip);
        try {
            InputStream us = getClass().getResourceAsStream("/img/UserIconPlaceholder.jpg");
            if (us != null) uImg.setImage(new Image(us));
        } catch (Exception e) {
            // Ignorer
        }

        VBox uText = new VBox(2);
        String userName = p.getId_utilisateur() != null ?
                p.getId_utilisateur().getPrenom() + " " + p.getId_utilisateur().getNom() : "Utilisateur inconnu";
        Label nameLbl = new Label(userName);
        nameLbl.setStyle("-fx-font-weight:bold;-fx-font-size:14px;-fx-text-fill:white;");
        Label dateLbl = new Label(p.getDateCreation() != null ? p.getDateCreation() : "");
        dateLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#94A3B8;");
        uText.getChildren().addAll(nameLbl, dateLbl);
        header.getChildren().addAll(uImg, uText);

        VBox body = new VBox(14);
        body.setPadding(new Insets(18, 24, 24, 24));

        // Image
        ImageView pubImg = new ImageView();
        pubImg.setFitHeight(250);
        pubImg.setFitWidth(492);
        pubImg.setPreserveRatio(false);
        pubImg.setSmooth(true);
        try {
            InputStream pi = getClass().getResourceAsStream("/img/" + p.getImgURL());
            if (pi != null) pubImg.setImage(new Image(pi));
        } catch (Exception e) {
            // Ignorer
        }
        Rectangle r2 = new Rectangle(492, 250);
        r2.setArcWidth(14);
        r2.setArcHeight(14);
        pubImg.setClip(r2);

        Label desc = new Label(p.getDescriptionP());
        desc.setWrapText(true);
        desc.setMaxWidth(492);
        desc.setStyle("-fx-font-size:14px;-fx-text-fill:#334155;");

        HBox reactionBar = buildReactionBar(p.getIdPublication());
        VBox analyticsCard = buildAnalyticsCard(p.getIdPublication());

        // AI bar
        HBox aiBar = new HBox(10);
        aiBar.setAlignment(Pos.CENTER_LEFT);
        Button sumBtn = styledBtn("📝 Résumer avec IA",
                "-fx-background-color:#E8EAF6;-fx-text-fill:#283593;-fx-background-radius:15;" +
                        "-fx-font-weight:bold;-fx-font-size:12px;-fx-padding:6 14;-fx-cursor:hand;");
        Button sentBtn = styledBtn("😊 Analyser commentaires",
                "-fx-background-color:#E0F7FA;-fx-text-fill:#006064;-fx-background-radius:15;" +
                        "-fx-font-weight:bold;-fx-font-size:12px;-fx-padding:6 14;-fx-cursor:hand;");
        aiBar.getChildren().addAll(sumBtn, sentBtn);

        // Comments
        Label commTitle = new Label("💬 Commentaires");
        commTitle.setStyle("-fx-font-weight:800;-fx-font-size:15px;-fx-text-fill:#1E293B;");
        VBox commentsList = new VBox(10);
        loadCommentsIntoBox(p, commentsList);

        // Add comment row
        HBox addRow = new HBox(10);
        addRow.setAlignment(Pos.CENTER_LEFT);
        addRow.setPadding(new Insets(8, 0, 0, 0));
        TextField tf = new TextField();
        tf.setPromptText("Écrire un commentaire…");
        tf.setStyle("-fx-background-color:#F8FAFC;-fx-background-radius:20;-fx-border-color:#E2E8F0;" +
                "-fx-border-radius:20;-fx-padding:9 16;-fx-font-size:13px;");
        HBox.setHgrow(tf, Priority.ALWAYS);
        Button sendBtn = styledBtn("➤",
                "-fx-background-color:#F39C12;-fx-text-fill:white;-fx-font-size:15px;-fx-font-weight:bold;" +
                        "-fx-background-radius:50;-fx-min-width:38;-fx-min-height:38;-fx-cursor:hand;" +
                        "-fx-effect:dropshadow(gaussian,rgba(243,156,18,0.5),8,0,0,2);");
        sendBtn.setOnAction(e -> {
            String txt = tf.getText().trim();
            if (txt.isEmpty()) return;
            if (containsBadWord(txt)) {
                showBadWordAlert();
                return;
            }
            try {
                Commentaire nc = new Commentaire();
                nc.setId_utilisateur(currentUser);
                nc.setPublication(p);
                nc.setContenuC(txt);
                nc.setDateCreationC(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                nc.setStatutC(true);
                commentDAO.ajouterh(nc);
                tf.clear();
                commentsList.getChildren().clear();
                loadCommentsIntoBox(p, commentsList);
                refreshCommentCounts();
                refreshAnalyticsSidebar();
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Impossible d'ajouter le commentaire: " + ex.getMessage());
            }
        });
        addRow.getChildren().addAll(tf, sendBtn);

        body.getChildren().addAll(pubImg, desc, reactionBar, analyticsCard,
                aiBar, new Separator(), commTitle, commentsList, addRow);
        root.getChildren().addAll(header, body);

        StackPane overlay = showModal(root);

        sumBtn.setOnAction(e -> {
            closeModal(overlay);
            openChatbotForSummary(p);
        });
        sentBtn.setOnAction(e -> {
            closeModal(overlay);
            openChatbotForSentiment(p);
        });
    }

    private void loadCommentsIntoBox(Publication p, VBox box) {
        try {
            List<Commentaire> list = commentDAO.getCommentsByPublicationId(p.getIdPublication());
            if (list.isEmpty()) {
                Label empty = new Label("Aucun commentaire pour l'instant. Soyez le premier ! 👋");
                empty.setStyle("-fx-text-fill:#94A3B8;-fx-font-style:italic;-fx-font-size:13px;");
                box.getChildren().add(empty);
                return;
            }
            for (Commentaire c : list) {
                box.getChildren().add(buildCommentRow(c, p, box));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            box.getChildren().add(new Label("Erreur de chargement des commentaires."));
        }
    }

    private HBox buildCommentRow(Commentaire c, Publication p, VBox parentBox) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setStyle("-fx-background-color:#F8FAFC;-fx-background-radius:14;" +
                "-fx-border-color:#E2E8F0;-fx-border-radius:14;");

        ImageView av = new ImageView();
        av.setFitWidth(34);
        av.setFitHeight(34);
        Circle avClip = new Circle(17, 17, 17);
        av.setClip(avClip);
        try {
            InputStream is = getClass().getResourceAsStream("/img/UserIconPlaceholder.jpg");
            if (is != null) av.setImage(new Image(is));
        } catch (Exception e) {
            // Ignorer
        }

        VBox textCol = new VBox(3);
        HBox.setHgrow(textCol, Priority.ALWAYS);
        String authorName = c.getId_utilisateur() != null ?
                c.getId_utilisateur().getPrenom() + " " + c.getId_utilisateur().getNom() : "Utilisateur inconnu";
        Label authorLbl = new Label(authorName);
        authorLbl.setStyle("-fx-font-weight:bold;-fx-font-size:12px;-fx-text-fill:#334155;");
        Label contentLbl = new Label(c.getContenuC());
        contentLbl.setWrapText(true);
        contentLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#475569;");
        textCol.getChildren().addAll(authorLbl, contentLbl);

        // Vérifier si l'utilisateur peut modifier/supprimer ce commentaire
        boolean canModify = isAdmin || (c.getId_utilisateur() != null &&
                c.getId_utilisateur().getId() == currentUser.getId());

        VBox acts = new VBox(4);
        acts.setAlignment(Pos.TOP_RIGHT);

        if (canModify) {
            Button editC = styledBtn("✏",
                    "-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;-fx-background-radius:8;" +
                            "-fx-font-size:12px;-fx-padding:3 8;-fx-cursor:hand;");
            Button delC = styledBtn("🗑",
                    "-fx-background-color:#FEF2F2;-fx-text-fill:#EF4444;-fx-background-radius:8;" +
                            "-fx-font-size:12px;-fx-padding:3 8;-fx-cursor:hand;");
            editC.setOnAction(e -> showEditCommentModal(c, p, parentBox));
            delC.setOnAction(e -> showDeleteCommentConfirm(c, p, parentBox));
            acts.getChildren().addAll(editC, delC);
        }

        row.getChildren().addAll(av, textCol, acts);
        return row;
    }

    private void showEditCommentModal(Commentaire c, Publication p, VBox commentsList) {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:white;-fx-background-radius:20;");

        VBox header = new VBox(4);
        header.setPadding(new Insets(24, 28, 18, 28));
        header.setStyle("-fx-background-color:linear-gradient(to right,#3B82F6,#6366F1);" +
                "-fx-background-radius:20 20 0 0;");
        Label t = new Label("✏  Modifier le commentaire");
        t.setStyle("-fx-font-size:17px;-fx-font-weight:800;-fx-text-fill:white;");
        header.getChildren().add(t);

        VBox body = new VBox(16);
        body.setPadding(new Insets(24, 28, 24, 28));

        VBox sec = sectionBox("💬  Contenu", "#EFF6FF", "#BFDBFE");
        TextArea ta = new TextArea(c.getContenuC());
        ta.setWrapText(true);
        ta.setPrefRowCount(4);
        ta.setStyle("-fx-background-color:#F8FAFC;-fx-background-radius:12;-fx-border-color:#BFDBFE;" +
                "-fx-border-radius:12;-fx-font-size:13px;-fx-padding:10;");
        sec.getChildren().add(ta);

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        Button cancelBtn = styledBtn("Annuler",
                "-fx-background-color:#F1F5F9;-fx-text-fill:#64748B;-fx-font-weight:bold;" +
                        "-fx-background-radius:12;-fx-padding:9 20;-fx-cursor:hand;");
        Button saveBtn = styledBtn("💾  Enregistrer",
                "-fx-background-color:linear-gradient(to right,#3B82F6,#6366F1);-fx-text-fill:white;" +
                        "-fx-font-weight:800;-fx-background-radius:12;-fx-padding:9 22;-fx-cursor:hand;" +
                        "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.4),8,0,0,2);");
        btnRow.getChildren().addAll(cancelBtn, saveBtn);

        body.getChildren().addAll(sec, btnRow);
        root.getChildren().addAll(header, body);

        StackPane overlay = showModal(root);
        cancelBtn.setOnAction(e -> closeModal(overlay));
        saveBtn.setOnAction(e -> {
            String txt = ta.getText().trim();
            if (txt.isEmpty()) return;
            if (containsBadWord(txt)) {
                showBadWordAlert();
                return;
            }
            try {
                c.setContenuC(txt);
                new CommentaireCRUD().modifierh(c);
                closeModal(overlay);
                commentsList.getChildren().clear();
                loadCommentsIntoBox(p, commentsList);
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Impossible de modifier: " + ex.getMessage());
            }
        });
    }

    private void showDeleteCommentConfirm(Commentaire c, Publication p, VBox commentsList) {
        VBox root = confirmDialog("🗑  Supprimer le commentaire",
                "Voulez-vous vraiment supprimer ce commentaire ?\nCette action est irréversible.",
                "#EF4444", "#FEF2F2", "#FECACA", "Supprimer");
        StackPane overlay = showModal(root);
        VBox body = (VBox) root.getChildren().get(1);
        HBox btnRow = (HBox) body.getChildren().get(2);
        ((Button) btnRow.getChildren().get(0)).setOnAction(e -> closeModal(overlay));
        ((Button) btnRow.getChildren().get(1)).setOnAction(e -> {
            try {
                new CommentaireCRUD().supprimerh(c);
                closeModal(overlay);
                commentsList.getChildren().clear();
                loadCommentsIntoBox(p, commentsList);
                refreshCommentCounts();
                refreshAnalyticsSidebar();
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Impossible de supprimer: " + ex.getMessage());
            }
        });
    }

    // ═══════════════════════════════════════════════════════════
    //  REACTION BAR
    // ═══════════════════════════════════════════════════════════
    private HBox buildReactionBar(int publicationId) {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 0, 0, 0));
        bar.setOnMouseClicked(e -> e.consume());
        for (Reaction r : Reaction.values()) {
            bar.getChildren().add(createReactionButton(r, publicationId, bar));
        }
        return bar;
    }

    private String reactionIdleStyle(Reaction r) {
        switch (r) {
            case LIKE:
                return "-fx-background-color:#E3F2FD;-fx-text-fill:#1565C0;";
            case LOVE:
                return "-fx-background-color:#FCE4EC;-fx-text-fill:#C62828;";
            case WOW:
                return "-fx-background-color:#FFF8E1;-fx-text-fill:#F57F17;";
            case SAD:
                return "-fx-background-color:#EDE7F6;-fx-text-fill:#4527A0;";
            default:
                return "-fx-background-color:#F0F2F5;-fx-text-fill:#333;";
        }
    }

    private String reactionActiveStyle(Reaction r) {
        switch (r) {
            case LIKE:
                return "-fx-background-color:#1565C0;-fx-text-fill:white;-fx-border-color:#0D47A1;-fx-border-radius:20;";
            case LOVE:
                return "-fx-background-color:#C62828;-fx-text-fill:white;-fx-border-color:#B71C1C;-fx-border-radius:20;";
            case WOW:
                return "-fx-background-color:#F57F17;-fx-text-fill:white;-fx-border-color:#E65100;-fx-border-radius:20;";
            case SAD:
                return "-fx-background-color:#4527A0;-fx-text-fill:white;-fx-border-color:#311B92;-fx-border-radius:20;";
            default:
                return "-fx-background-color:#555;-fx-text-fill:white;";
        }
    }

    private Button createReactionButton(Reaction r, int publicationId, HBox bar) {
        String base = "-fx-background-radius:20;-fx-font-size:14px;-fx-padding:5 12;-fx-cursor:hand;-fx-font-weight:bold;";
        Button btn = new Button(r.emoji + "  " + analytics.getReactionCount(publicationId, r));
        btn.setStyle(base + reactionIdleStyle(r));
        btn.setOnMouseEntered(e -> {
            if (!Boolean.TRUE.equals(btn.getUserData()))
                btn.setStyle(base + reactionIdleStyle(r) + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),6,0,0,2);-fx-scale-x:1.08;-fx-scale-y:1.08;");
        });
        btn.setOnMouseExited(e -> {
            if (!Boolean.TRUE.equals(btn.getUserData()))
                btn.setStyle(base + reactionIdleStyle(r));
        });
        btn.setOnAction(e -> {
            e.consume();
            analytics.addReaction(publicationId, r);
            btn.setText(r.emoji + "  " + analytics.getReactionCount(publicationId, r));
            btn.setUserData(true);
            btn.setStyle(base + reactionActiveStyle(r) + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.25),8,0,0,3);-fx-scale-x:1.12;-fx-scale-y:1.12;");
            FadeTransition bounce = new FadeTransition(Duration.millis(180), btn);
            bounce.setFromValue(0.5);
            bounce.setToValue(1.0);
            bounce.play();
            updateStatsInCard(publicationId, bar);
            refreshAnalyticsSidebar();
        });
        return btn;
    }

    private void updateStatsInCard(int publicationId, HBox reactionBar) {
        if (reactionBar.getParent() instanceof VBox outerCard) {
            for (javafx.scene.Node child : outerCard.getChildren()) {
                if (child instanceof VBox bodyBox) {
                    bodyBox.getChildren().removeIf(n -> "statsRow".equals(n.getUserData()));
                    int total = analytics.getTotalReactions(publicationId);
                    int comments = commentCounts.getOrDefault(publicationId, 0);
                    Optional<Reaction> dom = analytics.getDominantReaction(publicationId);
                    HBox statsRow = new HBox(8);
                    statsRow.setAlignment(Pos.CENTER_LEFT);
                    statsRow.setOnMouseClicked(ev -> ev.consume());
                    statsRow.getChildren().add(pill(
                            (dom.isPresent() ? dom.get().emoji : "❤️") + "  " + total,
                            "#FFF7ED", "#F39C12", "#EA7C00"));
                    statsRow.getChildren().add(pill("💬  " + comments, "#EFF6FF", "#3B82F6", "#2563EB"));
                    if (dom.isPresent())
                        statsRow.getChildren().add(pill("🏆 " + dom.get().label, "#F0FDF4", "#1ABC9C", "#0E9F82"));
                    statsRow.setUserData("statsRow");
                    bodyBox.getChildren().add(statsRow);
                    break;
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  ANALYTICS SIDEBAR
    // ═══════════════════════════════════════════════════════════
    private void refreshAnalyticsSidebar() {
        if (trendingPanel == null) return;
        int trendId = analytics.getTrendingPublicationId(commentCounts);
        int reactId = analytics.getMostReactedPublicationId();
        if (trendId != -1) {
            Publication t = findById(trendId);
            if (t != null) {
                trendingTitle.setText("🔥 Publication tendance");
                trendingDesc.setText(truncate(t.getDescriptionP(), 60));
                trendingPanel.setVisible(true);
                trendingPanel.setManaged(true);
            }
        } else {
            trendingPanel.setVisible(false);
            trendingPanel.setManaged(false);
        }
        if (mostReactedLabel != null && reactId != -1) {
            Publication p = findById(reactId);
            if (p != null) {
                mostReactedLabel.setText("⭐ Plus réagi: " + truncate(p.getDescriptionP(), 40) +
                        " (" + analytics.getTotalReactions(reactId) + ")");
            }
        }
        if (mostCommentedLabel != null) {
            commentCounts.entrySet().stream().max(Map.Entry.comparingByValue()).ifPresent(e -> {
                Publication p = findById(e.getKey());
                if (p != null) {
                    mostCommentedLabel.setText("💬 Plus commenté: " + truncate(p.getDescriptionP(), 40) +
                            " (" + e.getValue() + ")");
                }
            });
        }
    }

    private VBox buildAnalyticsCard(int publicationId) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color:#FFF8E1;-fx-background-radius:10;-fx-padding:12;" +
                "-fx-border-color:#FFD54F;-fx-border-radius:10;");
        Label t = new Label("📊 Analyse des réactions");
        t.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#E65100;");
        card.getChildren().add(t);
        int total = analytics.getTotalReactions(publicationId);
        for (Reaction r : Reaction.values()) {
            int cnt = analytics.getReactionCount(publicationId, r);
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            Label el = new Label(r.emoji + "  " + r.label);
            el.setMinWidth(100);
            ProgressBar bar = new ProgressBar(total == 0 ? 0 : (double) cnt / total);
            bar.setPrefWidth(150);
            bar.setStyle("-fx-accent:#FF9800;");
            row.getChildren().addAll(el, bar, new Label(cnt + ""));
            card.getChildren().add(row);
        }
        analytics.getDominantReaction(publicationId).ifPresent(r -> {
            Label d = new Label("🏆 Réaction dominante : " + r.emoji + " " + r.label);
            d.setStyle("-fx-font-weight:bold;-fx-text-fill:#BF360C;-fx-font-size:12px;");
            card.getChildren().add(d);
        });
        return card;
    }

    // ═══════════════════════════════════════════════════════════
    //  RENDER METHODS
    // ═══════════════════════════════════════════════════════════
    private void renderPublications(List<Publication> source) {
        // 1. Filter by search term
        List<Publication> filtered = applySearch(source);

        // 2. Apply sort
        List<Publication> sorted = new ArrayList<>(filtered);
        String option = sortCombo != null && sortCombo.getValue() != null ? sortCombo.getValue() : "Plus récentes";
        switch (option) {
            case "A-Z":
                sorted.sort(Comparator.comparing(pub ->
                        (pub.getId_utilisateur() != null ? pub.getId_utilisateur().getNom() + " " + pub.getId_utilisateur().getPrenom() : "").toLowerCase()));
                break;
            case "Z-A":
                sorted.sort(Comparator.comparing((Publication pub) ->
                        (pub.getId_utilisateur() != null ? pub.getId_utilisateur().getNom() + " " + pub.getId_utilisateur().getPrenom() : "").toLowerCase()).reversed());
                break;
            case "Plus récentes":
                sorted.sort(Comparator.comparing(Publication::getDateCreation, Comparator.nullsLast(Comparator.reverseOrder())));
                break;
            case "Plus anciennes":
                sorted.sort(Comparator.comparing(Publication::getDateCreation, Comparator.nullsLast(Comparator.naturalOrder())));
                break;
            case "Plus réagies":
                sorted.sort(Comparator.comparingInt((Publication pub) ->
                        analytics.getTotalReactions(pub.getIdPublication())).reversed());
                break;
        }

        // 3. Render
        publicationsFlowPane.getChildren().clear();

        if (sorted.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60, 0, 60, 0));
            Label icon = new Label("🔍");
            icon.setStyle("-fx-font-size:48px;");
            Label msg = new Label("Aucune publication trouvée");
            msg.setStyle("-fx-font-size:16px;-fx-font-weight:700;-fx-text-fill:#94A3B8;");
            Label sub = new Label("Essayez un autre terme ou réinitialisez les filtres");
            sub.setStyle("-fx-font-size:13px;-fx-text-fill:#CBD5E1;");
            empty.getChildren().addAll(icon, msg, sub);
            publicationsFlowPane.getChildren().add(empty);
        } else {
            for (Publication pub : sorted) {
                VBox card = createPublicationCard(pub);
                card.setMaxWidth(Double.MAX_VALUE);
                publicationsFlowPane.getChildren().add(card);
            }
        }

        int total = sorted.size();
        String countText = total + " publication" + (total > 1 ? "s" : "") + " trouvée" + (total > 1 ? "s" : "");
        if (!activeSearch.isBlank())
            countText += "  •  \"" + activeSearch + "\"";
        resultsCountText.setText(countText);
        refreshAnalyticsSidebar();
    }

    private List<Publication> applySearch(List<Publication> source) {
        if (activeSearch.isBlank()) return source;
        String q = activeSearch.toLowerCase().trim();
        return source.stream().filter(p -> {
            if (p.getTypeCible() != null && p.getTypeCible().name().toLowerCase().contains(q)) return true;
            if (p.getId_utilisateur() != null) {
                String fullName = (p.getId_utilisateur().getNom() + " " + p.getId_utilisateur().getPrenom()).toLowerCase();
                if (fullName.contains(q)) return true;
            }
            if (p.getDescriptionP() != null && p.getDescriptionP().toLowerCase().contains(q)) return true;
            return false;
        }).collect(java.util.stream.Collectors.toList());
    }

    private void setupSortListener() {
        if (sortCombo != null) {
            sortCombo.setOnAction(e -> renderPublications(allPublications));
        }
    }

    private void setupSearchListener() {
        if (pubSearchField != null) {
            pubSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
                activeSearch = newVal == null ? "" : newVal.trim();
                boolean hasText = !activeSearch.isBlank();
                if (btnClearSearch != null) {
                    btnClearSearch.setVisible(hasText);
                    btnClearSearch.setManaged(hasText);
                }
            });
        }
    }

    @FXML
    private void handleSearch() {
        activeSearch = pubSearchField != null ? pubSearchField.getText().trim() : "";
        boolean hasText = !activeSearch.isBlank();
        if (btnClearSearch != null) {
            btnClearSearch.setVisible(hasText);
            btnClearSearch.setManaged(hasText);
        }
        renderPublications(allPublications);
    }

    @FXML
    private void handleClearSearch() {
        if (pubSearchField != null) pubSearchField.clear();
        activeSearch = "";
        if (btnClearSearch != null) {
            btnClearSearch.setVisible(false);
            btnClearSearch.setManaged(false);
        }
        renderPublications(allPublications);
    }

    // ═══════════════════════════════════════════════════════════
    //  ACTIONS / FXML
    // ═══════════════════════════════════════════════════════════
    @FXML
    private void handleMesReservations() {
        System.out.println("Open reservations page");
        // Implémenter la redirection vers les réservations
    }

    @FXML
    private void openGlobalChat() {
        if (globalChat != null) globalChat.toggle();
    }

    @FXML
    private void openMiniGame(javafx.event.ActionEvent event) {
        String url = "about:blank";
        if (event.getSource() instanceof Button btn && btn.getUserData() instanceof String u) {
            url = u;
        }
        final String gameUrl = url;

        Stage gameStage = new Stage();
        gameStage.initModality(javafx.stage.Modality.NONE);
        gameStage.initStyle(javafx.stage.StageStyle.DECORATED);
        gameStage.setTitle("🕹  RE7LA Mini-jeux");
        gameStage.setMinWidth(900);
        gameStage.setMinHeight(640);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 16, 10, 16));
        header.setStyle("-fx-background-color:linear-gradient(to right,#2C3E50,#34495E);");

        Label icon = new Label("🕹");
        icon.setStyle("-fx-font-size:22px;");
        Label title = new Label("Mini-jeux RE7LA");
        title.setStyle("-fx-font-size:15px;-fx-font-weight:800;-fx-text-fill:white;");

        TextField urlBar = new TextField(gameUrl);
        urlBar.setEditable(false);
        urlBar.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-text-fill:#E2E8F0;");
        HBox.setHgrow(urlBar, Priority.ALWAYS);

        HBox pills = new HBox(6);
        pills.setAlignment(Pos.CENTER_RIGHT);
        String[][] games = {
                {"🧱", "Brick", "https://s3t.itch.io/brick-break.html"},
                {"🟦", "2048", "https://play2048.co/"},
                {"🟩", "Tetris", "https://tetr.io/"},
                {"🐍", "Snake", "https://www.google.com/fbx?fbx=snake_arcade"},
                {"🔢", "Sudoku", "https://sudoku.com/"}
        };

        javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
        webView.setStyle("-fx-background-color:#1E293B;");
        VBox.setVgrow(webView, Priority.ALWAYS);

        for (String[] g : games) {
            Button pill = new Button(g[0] + " " + g[1]);
            boolean active = g[2].equals(gameUrl);
            pill.setStyle("-fx-background-color:" + (active ? "#F39C12" : "rgba(255,255,255,0.12)") + ";" +
                    "-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:700;" +
                    "-fx-background-radius:20;-fx-padding:4 11;-fx-cursor:hand;");
            final String targetUrl = g[2];
            pill.setOnAction(ev -> {
                webView.getEngine().load(targetUrl);
                urlBar.setText(targetUrl);
                pills.getChildren().forEach(n -> {
                    if (n instanceof Button b)
                        b.setStyle(b.getStyle().replace("#F39C12", "rgba(255,255,255,0.12)"));
                });
                pill.setStyle(pill.getStyle().replace("rgba(255,255,255,0.12)", "#F39C12"));
            });
            pills.getChildren().add(pill);
        }

        header.getChildren().addAll(icon, title, urlBar, pills);

        StackPane webStack = new StackPane(webView);
        VBox.setVgrow(webStack, Priority.ALWAYS);
        Label loadingLbl = new Label("Chargement du jeu…");
        loadingLbl.setStyle("-fx-font-size:15px;-fx-font-weight:700;-fx-text-fill:#94A3B8;");
        webStack.getChildren().add(loadingLbl);
        StackPane.setAlignment(loadingLbl, Pos.CENTER);
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, o, n) -> {
            if (n == javafx.concurrent.Worker.State.SUCCEEDED ||
                    n == javafx.concurrent.Worker.State.FAILED) {
                loadingLbl.setVisible(false);
            }
        });

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(8, 16, 8, 16));
        footer.setStyle("-fx-background-color:#1E293B;");
        Label footerLbl = new Label("🎮  Profitez du jeu — bonne chance !");
        footerLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;");
        Region fSpacer = new Region();
        HBox.setHgrow(fSpacer, Priority.ALWAYS);
        Button closeBtn = new Button("✕  Fermer");
        closeBtn.setStyle("-fx-background-color:#EF4444;-fx-text-fill:white;-fx-font-weight:700;");
        closeBtn.setOnAction(e -> gameStage.close());
        footer.getChildren().addAll(footerLbl, fSpacer, closeBtn);

        VBox root = new VBox(header, webStack, footer);
        VBox.setVgrow(webStack, Priority.ALWAYS);
        root.setStyle("-fx-background-color:#1E293B;");

        Scene scene = new Scene(root, 940, 680);
        gameStage.setScene(scene);
        gameStage.show();

        webView.getEngine().load(gameUrl);
    }

    @FXML
    private void openChatbot() {
        try {
            chatbot = new ChatbotController(allPublications, commentCounts);
            chatbot.openChatWindow((Stage) publicationsFlowPane.getScene().getWindow());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le chatbot: " + e.getMessage());
        }
    }

    private void openChatbotForSummary(Publication p) {
        try {
            chatbot = new ChatbotController(allPublications, commentCounts);
            chatbot.openWithPublicationContext((Stage) publicationsFlowPane.getScene().getWindow(), p);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le chatbot: " + e.getMessage());
        }
    }

    private void openChatbotForSentiment(Publication p) {
        try {
            chatbot = new ChatbotController(allPublications, commentCounts);
            Stage owner = (Stage) publicationsFlowPane.getScene().getWindow();
            chatbot.openChatWindow(owner);
            new Thread(() -> {
                try {
                    Thread.sleep(600);
                } catch (InterruptedException ignored) {
                }
                Platform.runLater(() -> chatbot.openWithPublicationContext(owner, p));
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le chatbot: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  REUSABLE UI HELPERS
    // ═══════════════════════════════════════════════════════════
    private Button styledBtn(String text, String style) {
        Button b = new Button(text);
        b.setStyle(style);
        return b;
    }

    private VBox sectionBox(String label, String bg, String border) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12, 14, 12, 14));
        box.setStyle("-fx-background-color:" + bg + ";-fx-border-color:" + border +
                ";-fx-border-radius:10;-fx-background-radius:10;");
        Label l = new Label(label);
        l.setStyle("-fx-font-weight:700;-fx-font-size:12px;-fx-text-fill:#374151;");
        box.getChildren().add(l);
        return box;
    }

    private VBox confirmDialog(String title, String message,
                               String accent, String bgLight, String borderLight, String confirmLabel) {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:white;-fx-background-radius:20;");

        HBox header = new HBox();
        header.setPadding(new Insets(22, 28, 18, 28));
        header.setStyle("-fx-background-color:" + bgLight + ";-fx-background-radius:20 20 0 0;" +
                "-fx-border-color:" + borderLight + ";-fx-border-width:0 0 1 0;");
        Label t = new Label(title);
        t.setStyle("-fx-font-size:17px;-fx-font-weight:800;-fx-text-fill:" + accent + ";");
        header.getChildren().add(t);

        VBox body = new VBox(20);
        body.setPadding(new Insets(22, 28, 24, 28));
        Label msg = new Label(message);
        msg.setWrapText(true);
        msg.setStyle("-fx-font-size:13px;-fx-text-fill:#475569;");

        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        Button cancelBtn = styledBtn("Annuler",
                "-fx-background-color:#F1F5F9;-fx-text-fill:#64748B;-fx-font-weight:bold;" +
                        "-fx-background-radius:12;-fx-padding:9 20;-fx-cursor:hand;");
        Button confirmBtn = styledBtn(confirmLabel,
                "-fx-background-color:" + accent + ";-fx-text-fill:white;-fx-font-weight:800;" +
                        "-fx-background-radius:12;-fx-padding:9 22;-fx-cursor:hand;");
        btnRow.getChildren().addAll(cancelBtn, confirmBtn);
        body.getChildren().addAll(msg, btnRow);
        root.getChildren().addAll(header, body);
        return root;
    }

    private void showInlineError(VBox container, String message) {
        container.getChildren().removeIf(n -> "errorLabel".equals(n.getUserData()));
        Label err = new Label(message);
        err.setUserData("errorLabel");
        err.setStyle("-fx-text-fill:#EF4444;-fx-font-size:12px;-fx-font-weight:bold;");
        container.getChildren().add(0, err);
        new Timer().schedule(new TimerTask() {
            public void run() {
                Platform.runLater(() -> container.getChildren().remove(err));
            }
        }, 4000);
    }

    private Publication findById(int id) {
        return allPublications.stream().filter(p -> p.getIdPublication() == id).findFirst().orElse(null);
    }

    private boolean containsBadWord(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        for (String bad : BAD_WORDS) {
            if (lower.contains(bad.toLowerCase())) return true;
        }
        return false;
    }

    private void showBadWordAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Contenu inapproprié");
        alert.setHeaderText("Mot interdit détecté !");
        alert.setContentText("Votre texte contient un mot interdit.\nVeuillez modifier votre contenu avant de continuer.");
        try {
            InputStream is = getClass().getResourceAsStream("/img/stop_sign.png");
            if (is == null) is = getClass().getResourceAsStream("/stop_sign.png");
            if (is != null) {
                ImageView icon = new ImageView(new Image(is));
                icon.setFitWidth(80);
                icon.setFitHeight(80);
                icon.setPreserveRatio(true);
                alert.setGraphic(icon);
            }
        } catch (Exception ignored) {
        }
        alert.showAndWait();
    }

    @FXML
    private void goToAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/dashboard/homeClient.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) publicationsFlowPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Accueil");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de retourner à l'accueil: " + e.getMessage());
        }
    }

    @FXML
    private void goToProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/front/userProfil.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) publicationsFlowPane.getScene().getWindow();
            Scene scene = new Scene(root);
            String css = getClass().getResource("/user/front/userProfil.css").toExternalForm();
            scene.getStylesheets().add(css);
            stage.setScene(scene);
            stage.setTitle("RE7LA Tunisie - Mon Profil");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le profil: " + e.getMessage());
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
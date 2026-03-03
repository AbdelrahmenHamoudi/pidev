package org.example.Controllers.communaute;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.Entites.communaute.Publication;
import org.example.Services.communaute.CommentaireCRUD;
import org.example.Services.communaute.CommunauteAIService;
import org.example.Services.communaute.ReactionAnalyticsService;

import java.util.*;

/**
 * 🤖 ChatbotController
 * ─────────────────────────────────────────────────────────────────
 * Drives the AI assistant popup.  Built entirely in code — no FXML
 * needed — so it is self-contained and easy to embed anywhere.
 *
 * Features:
 *   • Free-text Q&A about the community
 *   • One-click quick actions (trending, most commented, sentiment…)
 *   • Smart comment suggestion mode
 *   • Per-publication summarise button (called from the main controller)
 * ─────────────────────────────────────────────────────────────────
 */
public class ChatbotController {

    // ── Dependencies injected by the parent controller ─────────────
    private final List<Publication>      publications;
    private final Map<Integer, Integer>  commentCounts;
    private final ReactionAnalyticsService analytics = ReactionAnalyticsService.getInstance();
    private final CommunauteAIService    aiService   = new CommunauteAIService();
    private final CommentaireCRUD        commentDAO  = new CommentaireCRUD();

    // ── UI nodes (built in buildUI()) ──────────────────────────────
    private VBox    chatMessages;
    private ScrollPane chatScroll;
    private TextField  inputField;
    private Button     sendButton;
    private Label      typingIndicator;

    // ── Optional: currently focused publication for context ────────
    private Publication focusedPublication = null;

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════
    public ChatbotController(List<Publication> publications,
                             Map<Integer, Integer> commentCounts) {
        this.publications  = publications;
        this.commentCounts = commentCounts;
    }

    // ═══════════════════════════════════════════════════════════════
    //  PUBLIC — open the chat popup
    // ═══════════════════════════════════════════════════════════════
    public void openChatWindow(Stage owner) {
        Stage chatStage = new Stage();
        chatStage.initOwner(owner);
        chatStage.setTitle("🤖 Assistant Communauté — RE7LA");
        chatStage.setResizable(true);

        VBox root = buildUI(chatStage);

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 520, 680);
        chatStage.setScene(scene);
        chatStage.show();

        // Welcome message after a tiny delay so UI renders first
        new Thread(() -> {
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> addBotMessage(
                    "👋 Bonjour ! Je suis l'assistant RE7LA.\n\n" +
                    "Je peux vous aider avec :\n" +
                    "• 📊 Analyser les publications\n" +
                    "• 💬 Analyser les commentaires\n" +
                    "• 🔥 Trouver les tendances\n" +
                    "• ✍️ Améliorer vos commentaires\n\n" +
                    "Posez-moi votre question ou utilisez les boutons rapides ci-dessous !"
            ));
        }).start();
    }

    /**
     * Called from the main controller when user clicks
     * "📝 Résumer" on a specific publication card.
     */
    public void openWithPublicationContext(Stage owner, Publication p) {
        this.focusedPublication = p;
        openChatWindow(owner);
        // Trigger summary immediately
        new Thread(() -> {
            try { Thread.sleep(600); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                addUserMessage("📝 Résumer la publication #" + p.getIdPublication());
                startTyping();
            });
            String summary = aiService.summarisePublication(p);
            Platform.runLater(() -> {
                stopTyping();
                addBotMessage("📋 **Résumé de la publication :**\n\n" + summary);
            });
        }).start();
    }

    // ═══════════════════════════════════════════════════════════════
    //  UI BUILDER
    // ═══════════════════════════════════════════════════════════════
    private VBox buildUI(Stage stage) {

        // ── Root container ─────────────────────────────────────────
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #F0F2F5;");

        // ── Header ─────────────────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));
        header.setStyle("-fx-background-color: linear-gradient(to right, #1A237E, #283593);" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);");

        Label avatarLabel = new Label("🤖");
        avatarLabel.setStyle("-fx-font-size: 28px;");

        VBox headerText = new VBox(2);
        Label titleLbl  = new Label("Assistant Communauté");
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px;");
        Label subLbl    = new Label("Propulsé par OpenAI GPT");
        subLbl.setStyle("-fx-text-fill: #90CAF9; -fx-font-size: 11px;");
        headerText.getChildren().addAll(titleLbl, subLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Online dot
        Label onlineDot = new Label("● En ligne");
        onlineDot.setStyle("-fx-text-fill: #69F0AE; -fx-font-size: 11px;");

        header.getChildren().addAll(avatarLabel, headerText, spacer, onlineDot);

        // ── Chat area ──────────────────────────────────────────────
        chatMessages = new VBox(10);
        chatMessages.setPadding(new Insets(15, 15, 10, 15));
        chatMessages.setFillWidth(true);

        chatScroll = new ScrollPane(chatMessages);
        chatScroll.setFitToWidth(true);
        chatScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        chatScroll.setStyle("-fx-background-color: #F0F2F5; -fx-border-color: transparent;");
        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        // ── Typing indicator ──────────────────────────────────────
        typingIndicator = new Label("🤖  L'assistant rédige une réponse...");
        typingIndicator.setStyle("-fx-font-size: 12px; -fx-text-fill: #888; -fx-font-style: italic;");
        typingIndicator.setPadding(new Insets(0, 0, 0, 15));
        typingIndicator.setVisible(false);
        typingIndicator.setManaged(false);

        // ── Quick action chips ─────────────────────────────────────
        VBox quickActionsBox = buildQuickActions();

        // ── Input row ─────────────────────────────────────────────
        HBox inputRow = buildInputRow();

        root.getChildren().addAll(header, chatScroll, typingIndicator, quickActionsBox, inputRow);
        return root;
    }

    // ── Quick-action chips ─────────────────────────────────────────
    private VBox buildQuickActions() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10, 15, 6, 15));
        box.setStyle("-fx-background-color: #FFFFFF;" +
                "-fx-border-color: #E0E0E0; -fx-border-width: 1 0 0 0;");

        Label lbl = new Label("Actions rapides :");
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #999; -fx-font-weight: bold;");

        // Row 1
        HBox row1 = new HBox(8);
        row1.setAlignment(Pos.CENTER_LEFT);
        row1.getChildren().addAll(
            chip("🔥 Tendances",        () -> quickAsk("Quelle est la publication la plus tendance en ce moment ?")),
            chip("💬 Plus commentée",   () -> quickAsk("Quelle publication a le plus de commentaires ?")),
            chip("⭐ Plus réagie",      () -> quickAsk("Quelle publication a obtenu le plus de réactions ?"))
        );

        // Row 2
        HBox row2 = new HBox(8);
        row2.setAlignment(Pos.CENTER_LEFT);
        row2.getChildren().addAll(
            chip("🏖 Plage & Mer",      () -> quickAsk("Trouve les publications qui parlent de plage ou de mer.")),
            chip("👤 Auteur le + actif",() -> quickAsk("Quel utilisateur publie le plus dans la communauté ?")),
            chip("📅 Récentes",         () -> quickAsk("Quelles sont les publications les plus récentes ?"))
        );

        // Row 3
        HBox row3 = new HBox(8);
        row3.setAlignment(Pos.CENTER_LEFT);
        row3.getChildren().addAll(
            chip("😊 Sentiment",        () -> quickAsk("Quel est le sentiment général de la communauté sur les publications ?")),
            chip("✍️ Améliorer commentaire", this::openCommentSuggestionMode)
        );

        box.getChildren().addAll(lbl, row1, row2, row3);
        return box;
    }

    private Button chip(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: #E8EAF6;" +
            "-fx-text-fill: #283593;" +
            "-fx-font-size: 11.5px;" +
            "-fx-background-radius: 15;" +
            "-fx-padding: 5 10;" +
            "-fx-cursor: hand;" +
            "-fx-font-weight: bold;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle().replace("#E8EAF6","#C5CAE9")));
        btn.setOnMouseExited(e  -> btn.setStyle(btn.getStyle().replace("#C5CAE9","#E8EAF6")));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    // ── Input row ─────────────────────────────────────────────────
    private HBox buildInputRow() {
        inputField = new TextField();
        inputField.setPromptText("Posez votre question...");
        inputField.setStyle(
            "-fx-background-radius: 22;" +
            "-fx-border-radius: 22;" +
            "-fx-border-color: #BDBDBD;" +
            "-fx-padding: 10 15;" +
            "-fx-font-size: 13px;"
        );
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputField.setOnAction(e -> handleSend());

        sendButton = new Button("➤");
        sendButton.setStyle(
            "-fx-background-color: #283593;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px;" +
            "-fx-background-radius: 22;" +
            "-fx-min-width: 44px;" +
            "-fx-min-height: 44px;" +
            "-fx-cursor: hand;"
        );
        sendButton.setOnAction(e -> handleSend());

        HBox row = new HBox(10, inputField, sendButton);
        row.setPadding(new Insets(10, 15, 15, 15));
        row.setAlignment(Pos.CENTER);
        row.setStyle("-fx-background-color: white;");
        return row;
    }

    // ═══════════════════════════════════════════════════════════════
    //  MESSAGE HANDLING
    // ═══════════════════════════════════════════════════════════════
    private void handleSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.clear();
        addUserMessage(text);
        dispatchQuestion(text);
    }

    private void quickAsk(String question) {
        addUserMessage(question);
        dispatchQuestion(question);
    }

    /**
     * Routes the question to the right AI method and streams the answer back.
     */
    private void dispatchQuestion(String question) {
        startTyping();
        sendButton.setDisable(true);

        // Build reaction totals map for analytics context
        Map<Integer, Integer> reactionTotals = new HashMap<>();
        for (Publication p : publications) {
            reactionTotals.put(p.getIdPublication(),
                analytics.getTotalReactions(p.getIdPublication()));
        }

        new Thread(() -> {
            String answer;
            try {
                answer = aiService.answerCommunityQuestion(question, publications, commentCounts, reactionTotals);
            } catch (Exception e) {
                answer = "❌ Une erreur s'est produite : " + e.getMessage();
            }
            final String finalAnswer = answer;
            Platform.runLater(() -> {
                stopTyping();
                sendButton.setDisable(false);
                addBotMessage(finalAnswer);
            });
        }).start();
    }

    // ── Comment suggestion mode ────────────────────────────────────
    private void openCommentSuggestionMode() {
        addBotMessage("✍️ **Mode suggestion de commentaire activé !**\n\n" +
                "Écrivez votre ébauche de commentaire et je vais la rendre plus professionnelle.\n" +
                "*(Vous pouvez aussi mentionner sur quelle publication)*");

        // Swap to suggestion mode — intercept next send
        inputField.setPromptText("Entrez votre ébauche de commentaire...");

        // One-shot override: next message goes to suggestion API
        sendButton.setOnAction(e -> {
            String draft = inputField.getText().trim();
            if (draft.isEmpty()) return;
            inputField.clear();
            addUserMessage(draft);

            // Pick first publication as context, or focused one
            Publication ctx = focusedPublication != null ? focusedPublication
                    : (publications.isEmpty() ? null : publications.get(0));

            startTyping();
            sendButton.setDisable(true);

            new Thread(() -> {
                String suggestion = ctx != null
                        ? aiService.suggestBetterComment(draft, ctx)
                        : "💡 Suggestion : \"" + draft + " — merci pour ce partage !\"";
                Platform.runLater(() -> {
                    stopTyping();
                    sendButton.setDisable(false);
                    addBotMessage("💡 **Voici une version améliorée :**\n\n\"" + suggestion + "\"");
                    // Restore normal send
                    sendButton.setOnAction(ev -> handleSend());
                    inputField.setPromptText("Posez votre question...");
                });
            }).start();
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  BUBBLE BUILDERS
    // ═══════════════════════════════════════════════════════════════
    private void addUserMessage(String text) {
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(360);
        bubble.setStyle(
            "-fx-background-color: #283593;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 18 18 4 18;" +
            "-fx-padding: 10 15;" +
            "-fx-font-size: 13px;"
        );

        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setMaxWidth(Double.MAX_VALUE);

        appendWithAnimation(row);
    }

    private void addBotMessage(String text) {
        VBox bubbleBox = new VBox(4);
        bubbleBox.setMaxWidth(380);

        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size: 18px;");

        // Render **bold** markers simply by splitting (basic markdown-lite)
        Label bubble = new Label(text.replace("**", ""));
        bubble.setWrapText(true);
        bubble.setMaxWidth(360);
        bubble.setStyle(
            "-fx-background-color: white;" +
            "-fx-text-fill: #1A1A2E;" +
            "-fx-background-radius: 18 18 18 4;" +
            "-fx-padding: 10 15;" +
            "-fx-font-size: 13px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);"
        );

        HBox inner = new HBox(8, avatar, bubble);
        inner.setAlignment(Pos.TOP_LEFT);

        HBox row = new HBox(inner);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        appendWithAnimation(row);
    }

    private void appendWithAnimation(HBox row) {
        FadeTransition ft = new FadeTransition(Duration.millis(300), row);
        ft.setFromValue(0);
        ft.setToValue(1);
        chatMessages.getChildren().add(row);
        ft.play();
        // Auto-scroll to bottom
        Platform.runLater(() -> chatScroll.setVvalue(1.0));
    }

    // ═══════════════════════════════════════════════════════════════
    //  TYPING INDICATOR
    // ═══════════════════════════════════════════════════════════════
    private void startTyping() {
        typingIndicator.setVisible(true);
        typingIndicator.setManaged(true);
    }

    private void stopTyping() {
        typingIndicator.setVisible(false);
        typingIndicator.setManaged(false);
    }
}

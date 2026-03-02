package org.example.Controllers.communaute;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.example.Services.communaute.AblyRealtimeChat;
import org.example.Services.communaute.AblyRealtimeChat.ChatMessage;
import org.example.Services.communaute.ChatHistoryService;
import org.example.Services.communaute.ChatHistoryService.HistoryMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 💬 GlobalChatController
 * ─────────────────────────────────────────────────────────────────
 * Messenger-style chat panel embedded directly inside the main
 * page's StackPane as an overlay — NOT a separate Stage.
 *
 * This is the correct approach for JavaFX: a separate undecorated
 * Stage always ends up blocking the main window's input. An overlay
 * VBox inside the same scene shares focus naturally.
 *
 * Call inject(rootStackPane) once, then toggle() to show/hide.
 * ─────────────────────────────────────────────────────────────────
 */
public class GlobalChatController {

    // ── Config ────────────────────────────────────────────────────
    private final String username;

    // ── Ably service ──────────────────────────────────────────────
    private final AblyRealtimeChat   ably    = AblyRealtimeChat.getInstance();

    // ── Chat history (local file persistence) ─────────────────────
    private final ChatHistoryService history = ChatHistoryService.getInstance();

    // ── UI nodes ──────────────────────────────────────────────────
    private VBox       chatPanel;     // the whole overlay panel
    private VBox       messagesBox;
    private ScrollPane scroll;
    private TextField  inputField;
    private Label      statusDot;
    private Label      statusText;
    private boolean    visible = false;

    // ── FAB badge ─────────────────────────────────────────────────
    private final Button fab;
    private int unreadCount = 0;

    // ── Root StackPane we inject ourselves into ───────────────────
    private StackPane rootPane;

    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm");

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════
    public GlobalChatController(String username, Button fab) {
        this.username = username;
        this.fab      = fab;
    }

    // ═══════════════════════════════════════════════════════════════
    //  INJECT — call once from CommunauteController.initialize()
    //  Pass the root StackPane (the one that already holds the
    //  scrollable content and the FAB buttons).
    // ═══════════════════════════════════════════════════════════════
    public void inject(StackPane root) {
        this.rootPane = root;
        buildPanel();

        // Position: bottom-right corner, above the FAB buttons
        StackPane.setAlignment(chatPanel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatPanel, new Insets(0, 28, 130, 0));

        root.getChildren().add(chatPanel);
        chatPanel.setVisible(false);
        chatPanel.setManaged(false);
    }

    // ═══════════════════════════════════════════════════════════════
    //  TOGGLE — show / hide with slide animation
    // ═══════════════════════════════════════════════════════════════
    public void toggle() {
        if (visible) {
            hide();
        } else {
            show();
        }
    }

    private void show() {
        visible = true;
        unreadCount = 0;
        updateFabBadge();

        chatPanel.setVisible(true);
        chatPanel.setManaged(true);
        chatPanel.setTranslateY(40);
        chatPanel.setOpacity(0);

        FadeTransition ft = new FadeTransition(Duration.millis(200), chatPanel);
        ft.setToValue(1); ft.play();

        TranslateTransition tt = new TranslateTransition(Duration.millis(200), chatPanel);
        tt.setToY(0); tt.play();

        connectIfNeeded();
        Platform.runLater(() -> inputField.requestFocus());
    }

    private void hide() {
        visible = false;
        FadeTransition ft = new FadeTransition(Duration.millis(150), chatPanel);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            chatPanel.setVisible(false);
            chatPanel.setManaged(false);
        });
        ft.play();
    }

    // ═══════════════════════════════════════════════════════════════
    //  BUILD PANEL
    // ═══════════════════════════════════════════════════════════════
    private void buildPanel() {
        chatPanel = new VBox(0);
        chatPanel.setPrefSize(340, 460);
        chatPanel.setMaxSize(340, 460);
        chatPanel.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 20, 0, 0, 6);"
        );

        // ── Header ────────────────────────────────────────────────
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(13, 16, 13, 16));
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, #0084FF, #0066CC);" +
                        "-fx-background-radius: 16 16 0 0;"
        );

        StackPane avatar = new StackPane();
        Circle avatarCircle = new Circle(17);
        avatarCircle.setStyle("-fx-fill: rgba(255,255,255,0.22);");
        Label avatarLbl = new Label("RE");
        avatarLbl.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:10px;");
        avatar.getChildren().addAll(avatarCircle, avatarLbl);

        VBox titleBox = new VBox(2);
        Label titleLbl = new Label("Chat Communauté");
        titleLbl.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:14px;");

        HBox statusRow = new HBox(4);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusDot  = new Label("●");
        statusDot.setStyle("-fx-text-fill:#FFD700;-fx-font-size:9px;");
        statusText = new Label("Connexion…");
        statusText.setStyle("-fx-text-fill:rgba(255,255,255,0.8);-fx-font-size:11px;");
        statusRow.getChildren().addAll(statusDot, statusText);
        titleBox.getChildren().addAll(titleLbl, statusRow);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:white;" +
                        "-fx-background-radius:50;-fx-font-size:12px;" +
                        "-fx-min-width:28;-fx-min-height:28;-fx-cursor:hand;"
        );
        closeBtn.setOnAction(e -> hide());
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(closeBtn.getStyle().replace("0.15","0.3")));
        closeBtn.setOnMouseExited(e  -> closeBtn.setStyle(closeBtn.getStyle().replace("0.3","0.15")));

        header.getChildren().addAll(avatar, titleBox, spacer, closeBtn);

        // ── Messages area ──────────────────────────────────────────
        messagesBox = new VBox(8);
        messagesBox.setPadding(new Insets(12));
        messagesBox.setFillWidth(true);

        scroll = new ScrollPane(messagesBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color:#F0F2F5;-fx-border-color:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Input bar ──────────────────────────────────────────────
        HBox inputBar = new HBox(8);
        inputBar.setAlignment(Pos.CENTER);
        inputBar.setPadding(new Insets(9, 12, 11, 12));
        inputBar.setStyle(
                "-fx-background-color:#FFFFFF;" +
                        "-fx-border-color:#E4E6EB;-fx-border-width:1 0 0 0;" +
                        "-fx-background-radius:0 0 16 16;"
        );

        inputField = new TextField();
        inputField.setPromptText("Écrire un message…");
        inputField.setStyle(
                "-fx-background-color:#F0F2F5;-fx-background-radius:20;" +
                        "-fx-border-color:transparent;-fx-padding:8 14;-fx-font-size:13px;"
        );
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputField.setOnAction(e -> handleSend());

        Button sendBtn = new Button("➤");
        sendBtn.setStyle(
                "-fx-background-color:#0084FF;-fx-text-fill:white;" +
                        "-fx-background-radius:50;-fx-font-size:14px;" +
                        "-fx-min-width:36;-fx-min-height:36;-fx-cursor:hand;"
        );
        sendBtn.setOnAction(e -> handleSend());
        sendBtn.setOnMouseEntered(e -> sendBtn.setStyle(sendBtn.getStyle().replace("#0084FF","#0066CC")));
        sendBtn.setOnMouseExited(e  -> sendBtn.setStyle(sendBtn.getStyle().replace("#0066CC","#0084FF")));

        inputBar.getChildren().addAll(inputField, sendBtn);
        chatPanel.getChildren().addAll(header, scroll, inputBar);

        // Load persisted history
        loadHistory();
    }

    // ═══════════════════════════════════════════════════════════════
    //  LOAD HISTORY
    // ═══════════════════════════════════════════════════════════════
    private void loadHistory() {
        List<HistoryMessage> msgs = history.load();
        if (msgs.isEmpty()) return;
        String lastDate = null;
        for (HistoryMessage m : msgs) {
            String date = m.formattedDate();
            if (!date.equals(lastDate)) { addDateSeparator(date); lastDate = date; }
            if (m.own) addOwnMessageWithTime(m.text, m.formattedTime());
            else       addOtherMessageWithTime(m.username, m.text, m.formattedTime());
        }
        addSystemMessage("— Fin de l'historique —");
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONNECT
    // ═══════════════════════════════════════════════════════════════
    private void connectIfNeeded() {
        if (ably.isConnected()) { setStatus(true, "En ligne"); return; }
        ably.connect(
                username,
                msg -> Platform.runLater(() -> receiveMessage(msg)),
                ()  -> Platform.runLater(() -> {
                    setStatus(true, "En ligne");
                    addSystemMessage("✅ Connecté au chat communauté !");
                }),
                err -> Platform.runLater(() -> {
                    setStatus(false, "Hors ligne");
                    addSystemMessage("❌ " + err);
                })
        );
    }

    // ═══════════════════════════════════════════════════════════════
    //  SEND
    // ═══════════════════════════════════════════════════════════════
    private void handleSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        if (!ably.isConnected()) { addSystemMessage("⚠️ Pas encore connecté…"); return; }
        inputField.clear();
        ably.send(username, text);
        history.save(username, text, true);
        addOwnMessage(text);
    }

    // ═══════════════════════════════════════════════════════════════
    //  RECEIVE
    // ═══════════════════════════════════════════════════════════════
    private void receiveMessage(ChatMessage msg) {
        if (msg.username.equals(username)) return;
        history.save(msg.username, msg.text, false);
        addOtherMessage(msg.username, msg.text);
        if (!visible) { unreadCount++; updateFabBadge(); }
    }

    // ═══════════════════════════════════════════════════════════════
    //  BUBBLE BUILDERS
    // ═══════════════════════════════════════════════════════════════
    private void addOwnMessage(String text) {
        addOwnMessageWithTime(text, TIME_FMT.format(new Date()));
    }

    private void addOwnMessageWithTime(String text, String time) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        VBox bubble = new VBox(3);
        bubble.setMaxWidth(220);
        bubble.setStyle("-fx-background-color:#0084FF;-fx-background-radius:18 18 4 18;-fx-padding:9 13;");
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setStyle("-fx-text-fill:white;-fx-font-size:13px;");
        Label timeLbl = new Label(time);
        timeLbl.setStyle("-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:10px;");
        bubble.getChildren().addAll(msg, timeLbl);
        row.getChildren().add(bubble);
        appendBubble(row);
    }

    private void addOtherMessage(String sender, String text) {
        addOtherMessageWithTime(sender, text, TIME_FMT.format(new Date()));
    }

    private void addOtherMessageWithTime(String sender, String text, String time) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        StackPane av = new StackPane();
        Circle c = new Circle(15);
        c.setStyle("-fx-fill:" + nameToColor(sender) + ";");
        Label initials = new Label(sender.substring(0, Math.min(2, sender.length())).toUpperCase());
        initials.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:9px;");
        av.getChildren().addAll(c, initials);
        VBox bubble = new VBox(3);
        bubble.setMaxWidth(210);
        bubble.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:18 18 18 4;" +
                        "-fx-padding:9 13;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),4,0,0,1);"
        );
        Label senderLbl = new Label(sender);
        senderLbl.setStyle("-fx-font-weight:bold;-fx-font-size:11px;-fx-text-fill:" + nameToColor(sender) + ";");
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setStyle("-fx-text-fill:#1C1E21;-fx-font-size:13px;");
        Label timeLbl = new Label(time);
        timeLbl.setStyle("-fx-text-fill:#999;-fx-font-size:10px;");
        bubble.getChildren().addAll(senderLbl, msg, timeLbl);
        row.getChildren().addAll(av, bubble);
        appendBubble(row);
    }

    private void addSystemMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER);
        Label lbl = new Label(text);
        lbl.setStyle(
                "-fx-background-color:rgba(0,0,0,0.06);-fx-background-radius:10;" +
                        "-fx-text-fill:#65676B;-fx-font-size:11px;-fx-padding:4 10;"
        );
        row.getChildren().add(lbl);
        appendBubble(row);
    }

    private void addDateSeparator(String date) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER);
        Label lbl = new Label("─── " + date + " ───");
        lbl.setStyle("-fx-text-fill:#B0B3B8;-fx-font-size:11px;-fx-padding:6 0;");
        row.getChildren().add(lbl);
        appendBubble(row);
    }

    private void appendBubble(HBox row) {
        FadeTransition ft = new FadeTransition(Duration.millis(200), row);
        ft.setFromValue(0); ft.setToValue(1);
        messagesBox.getChildren().add(row);
        ft.play();
        Platform.runLater(() -> scroll.setVvalue(1.0));
    }

    // ═══════════════════════════════════════════════════════════════
    //  STATUS / BADGE
    // ═══════════════════════════════════════════════════════════════
    private void setStatus(boolean online, String label) {
        statusDot.setStyle("-fx-text-fill:" + (online ? "#69F0AE" : "#FF5252") + ";-fx-font-size:9px;");
        statusText.setText(label);
    }

    private void updateFabBadge() {
        if (unreadCount > 0) {
            fab.setText("💬  Chat  (" + unreadCount + ")");
        } else {
            fab.setText("💬  Chat");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════
    private String nameToColor(String name) {
        String[] p = {"#E91E63","#9C27B0","#3F51B5","#2196F3","#009688","#FF5722","#795548","#607D8B"};
        return p[Math.abs(name.hashCode()) % p.length];
    }

    public void closeAndDisconnect() {
        ably.disconnect();
    }
}

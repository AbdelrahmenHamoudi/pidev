package org.example.Controllers.Trajet;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.example.Entites.trajet.Trajet;
import org.example.Services.trajet.TrajetCRUD;
import org.example.Services.trajet.VoitureCRUD;

import javax.sound.sampled.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.*;

public class AssistantVocalController implements Initializable {

    @FXML private VBox       chatContainer;
    @FXML private ScrollPane chatScroll;
    @FXML private TextField  txtMessage;
    @FXML private Button     btnMicro;
    @FXML private Button     btnEnvoyer;
    @FXML private Label      lblStatut;
    @FXML private Label      lblLangue;
    @FXML private Circle     circleAnimation;
    @FXML private HBox       enregistrementIndicateur;  // HBox contenant le cercle + label
    @FXML private ComboBox<String> langueCombo;
    @FXML private VBox       rapportPanel;
    @FXML private Label      lblRapportInfo;
    @FXML private Label      lblProblemeStatus;
    @FXML private FlowPane   problemesRapides;

    private static final String GEMINI_API_KEY  = "AIzaSyDkDv5k8dKcEYXNd7Ue_OAqvbTTIeEvRsU";
    private static final String GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    // FIX 1 : volatile pour visibilite inter-threads
    private volatile boolean enregistrement     = false;
    // FIX 2 : verrou contre double-lancement
    private volatile boolean transcriptionEnCours = false;

    private TargetDataLine microLine = null;
    private String langueActuelle    = "fr";
    private Trajet trajetEnCours     = null;
    private final List<JsonObject> historique = new ArrayList<>();

    private TrajetCRUD  trajetCRUD;
    private VoitureCRUD voitureCRUD;

    private static final Logger LOGGER = Logger.getLogger(AssistantVocalController.class.getName());
    private final Gson gson = new Gson();

    private static final Map<String, String[]> PROBLEMES = new LinkedHashMap<>();
    static {
        PROBLEMES.put("Panne moteur",  new String[]{"panne", "moteur"});
        PROBLEMES.put("Crevaison",     new String[]{"pneu", "crevaison"});
        PROBLEMES.put("Carburant",     new String[]{"carburant", "essence"});
        PROBLEMES.put("Accident",      new String[]{"accident"});
        PROBLEMES.put("Meteo danger",  new String[]{"meteo", "pluie"});
        PROBLEMES.put("Passager",      new String[]{"passager"});
    }

    private static final String SYSTEM_PROMPT = """
        Tu es l'assistant vocal de RE7LA, une app de covoiturage tunisienne.
        Tu parles en FRANCAIS, ARABE ou ANGLAIS selon la langue du message.
        Reponds toujours en 3 phrases max (reponse vocale).
        Villes: Tunis, Sousse, Sfax, Bizerte, Gabes, Ariana, Nabeul, Hammamet,
        Kairouan, Monastir, Mahdia, Gafsa, Tozeur, Djerba, El Kef, Medenine, Tataouine.
        Prefixe [SIGNALEMENT] pour signalement, [NAVIGATION] pour navigation, [RAPPORT] pour rapport.
        """;

    // ════════════════════════════════════════════════════
    //  INITIALISATION
    // ════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        trajetCRUD  = new TrajetCRUD();
        voitureCRUD = new VoitureCRUD();

        setupLangueCombo();
        setupProblemesBoutons();
        setupAnimationMicro();
        chargerTrajetEnCours();

        // FIX 3 : cacher l'indicateur au demarrage
        cacherIndicateur();

        afficherMessageAssistant(getMessageAccueil());
    }

    private void setupLangueCombo() {
        if (langueCombo == null) return;
        langueCombo.setItems(javafx.collections.FXCollections.observableArrayList(
                "FR Francais", "SA Arabe", "GB English"));
        langueCombo.setValue("FR Francais");
        langueCombo.setOnAction(e -> {
            String v = langueCombo.getValue();
            if (v == null) return;
            if (v.contains("Arabe"))   langueActuelle = "ar";
            else if (v.contains("English")) langueActuelle = "en";
            else langueActuelle = "fr";
            if (lblLangue != null) lblLangue.setText(v);
        });
    }

    private void setupProblemesBoutons() {
        if (problemesRapides == null) return;
        problemesRapides.getChildren().clear();
        for (String p : PROBLEMES.keySet()) {
            Button btn = new Button(p);
            btn.setStyle("-fx-background-color:#FEF3C7;-fx-text-fill:#92400E;" +
                    "-fx-background-radius:20;-fx-padding:6 12;-fx-cursor:hand;-fx-font-size:12px;");
            btn.setOnAction(e -> signalerProblemeRapide(p));
            problemesRapides.getChildren().add(btn);
        }
    }

    private void setupAnimationMicro() {
        if (circleAnimation == null) return;
        ScaleTransition pulse = new ScaleTransition(Duration.millis(600), circleAnimation);
        pulse.setFromX(1.0); pulse.setToX(1.4);
        pulse.setFromY(1.0); pulse.setToY(1.4);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        circleAnimation.setUserData(pulse);
    }

    // ─── Helpers indicateur ──────────────────────────────

    /** Affiche "Enregistrement en cours..." avec animation */
    private void afficherIndicateur() {
        if (enregistrementIndicateur != null) {
            enregistrementIndicateur.setVisible(true);
            enregistrementIndicateur.setManaged(true);
        }
        if (circleAnimation != null && circleAnimation.getUserData() instanceof ScaleTransition p) p.play();
    }

    /** Cache complètement l'indicateur (sans occuper d'espace) */
    private void cacherIndicateur() {
        if (enregistrementIndicateur != null) {
            enregistrementIndicateur.setVisible(false);
            enregistrementIndicateur.setManaged(false);  // ne prend plus de place dans le layout
        }
        if (circleAnimation != null && circleAnimation.getUserData() instanceof ScaleTransition p) p.stop();
    }

    private void chargerTrajetEnCours() {
        try {
            List<Trajet> trajets = trajetCRUD.afficherh();
            if (trajets != null && !trajets.isEmpty()) {
                trajetEnCours = trajets.get(trajets.size() - 1);
                updateRapportPanel();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erreur chargement trajet", e);
        }
    }

    private void updateRapportPanel() {
        if (rapportPanel == null || trajetEnCours == null) return;
        if (lblRapportInfo != null)
            lblRapportInfo.setText(String.format("%s -> %s  |  %.0f km  |  %s",
                    trajetEnCours.getPointDepart(), trajetEnCours.getPointArrivee(),
                    trajetEnCours.getDistanceKm(),
                    new SimpleDateFormat("dd/MM/yyyy").format(trajetEnCours.getDateReservation())));
    }

    // ════════════════════════════════════════════════════
    //  MICRO
    // ════════════════════════════════════════════════════

    private static final int MAX_SEC = 5;
    private javafx.animation.Timeline timerEnregistrement;

    @FXML
    private void toggleMicro() {
        if (!enregistrement) demarrerEnregistrement();
        else arreterEnregistrement();
    }

    private void demarrerEnregistrement() {
        // Bloquer si transcription déjà en cours
        if (transcriptionEnCours) {
            setStatut("Transcription en cours, patientez...", "#F59E0B");
            return;
        }

        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            setStatut("Micro non disponible", "#EF4444");
            return;
        }

        try {
            microLine = (TargetDataLine) AudioSystem.getLine(info);
            microLine.open(format);
            microLine.start();
            enregistrement = true;

            // FIX 4 : UI mise a jour DIRECTEMENT (JavaFX thread) - pas de runLater
            btnMicro.setText("Stop");
            btnMicro.setStyle(getBtnMicroStyle(true));
            lblStatut.setText("Parlez... (max " + MAX_SEC + "s)");
            lblStatut.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");
            afficherIndicateur();

            final ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
            final AudioFormat captureFormat = format;

            Thread captureThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                try {
                    // FIX 5 : volatile → le thread voit enregistrement=false immédiatement
                    while (enregistrement && microLine != null && microLine.isOpen()) {
                        int n = microLine.read(buffer, 0, buffer.length);
                        if (n > 0) audioBuffer.write(buffer, 0, n);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Capture interrompue", e);
                }

                if (audioBuffer.size() > 0) {
                    LOGGER.info("Audio capture: " + audioBuffer.size() + " bytes");
                    transcriptionEnCours = true;
                    Platform.runLater(() -> setStatut("Transcription en cours...", "#6366F1"));
                    transcrireEtEnvoyer(audioBuffer.toByteArray(), captureFormat);
                } else {
                    Platform.runLater(() -> {
                        setStatut("Aucun audio, reessayez", "#F59E0B");
                        cacherIndicateur();
                    });
                }
            });
            captureThread.setDaemon(true);
            captureThread.start();

            timerEnregistrement = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(
                            javafx.util.Duration.seconds(MAX_SEC),
                            e -> { if (enregistrement) arreterEnregistrement(); }
                    )
            );
            timerEnregistrement.play();

        } catch (LineUnavailableException e) {
            enregistrement = false;
            microLine = null;
            cacherIndicateur();
            setStatut("Erreur micro: " + e.getMessage(), "#EF4444");
        }
    }

    private void arreterEnregistrement() {
        if (timerEnregistrement != null) { timerEnregistrement.stop(); timerEnregistrement = null; }

        // FIX 6 : volatile false → thread capture sort de la boucle while
        enregistrement = false;

        if (microLine != null) {
            microLine.stop();
            microLine.flush();
            microLine.close();
            microLine = null;
        }

        // FIX 7 : on remet juste le bouton - l'indicateur reste jusqu'a fin de transcription
        Platform.runLater(() -> {
            btnMicro.setText("Parler");
            btnMicro.setStyle(getBtnMicroStyle(false));
            if (txtMessage != null) txtMessage.requestFocus();
        });
    }

    // ════════════════════════════════════════════════════
    //  TEXTE → GEMINI
    // ════════════════════════════════════════════════════

    @FXML
    private void envoyerMessage() {
        String msg = txtMessage.getText().trim();
        if (msg.isEmpty()) return;
        txtMessage.clear();
        afficherMessageConducteur(msg);
        setStatut("Gemini reflechit...", "#6366F1");
        Thread t = new Thread(() -> {
            String rep = appelGemini(msg);
            Platform.runLater(() -> { traiterReponseGemini(rep, msg); setStatut("Pret", "#10B981"); });
        });
        t.setDaemon(true);
        t.start();
    }

    // ════════════════════════════════════════════════════
    //  AUDIO → GEMINI
    // ════════════════════════════════════════════════════

    private void transcrireEtEnvoyer(byte[] pcmBytes, AudioFormat fmt) {
        Thread t = new Thread(() -> {
            try {
                byte[] wavBytes   = pcmToWav(pcmBytes, fmt);
                String base64Audio = java.util.Base64.getEncoder().encodeToString(wavBytes);

                JsonObject body    = new JsonObject();
                JsonArray contents = new JsonArray();

                if (historique.isEmpty()) {
                    JsonObject su = new JsonObject(); su.addProperty("role", "user");
                    JsonArray sp = new JsonArray(); JsonObject spt = new JsonObject();
                    spt.addProperty("text", SYSTEM_PROMPT); sp.add(spt); su.add("parts", sp);
                    JsonObject sm = new JsonObject(); sm.addProperty("role", "model");
                    JsonArray mp = new JsonArray(); JsonObject mpt = new JsonObject();
                    mpt.addProperty("text", "Compris."); mp.add(mpt); sm.add("parts", mp);
                    historique.add(su); historique.add(sm);
                }

                int start = Math.max(0, historique.size() - 8);
                for (int i = start; i < historique.size(); i++) contents.add(historique.get(i));

                JsonObject userMsg = new JsonObject(); userMsg.addProperty("role", "user");
                JsonArray parts = new JsonArray();
                JsonObject audioPart = new JsonObject(); JsonObject inlineData = new JsonObject();
                inlineData.addProperty("mimeType", "audio/wav");
                inlineData.addProperty("data", base64Audio);
                audioPart.add("inlineData", inlineData); parts.add(audioPart);
                JsonObject textPart = new JsonObject();
                textPart.addProperty("text", "Transcris en " + getLangLabel() +
                        " puis reponds (2-3 phrases). Format: [TRANSCRIPTION: texte] puis reponse.");
                parts.add(textPart);
                userMsg.add("parts", parts);
                contents.add(userMsg);
                body.add("contents", contents);

                JsonObject gc = new JsonObject();
                gc.addProperty("temperature", 0.7);
                gc.addProperty("maxOutputTokens", 400);
                body.add("generationConfig", gc);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(GEMINI_ENDPOINT + GEMINI_API_KEY))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                        .build();

                HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                    String texte = json.getAsJsonArray("candidates").get(0).getAsJsonObject()
                            .getAsJsonObject("content").getAsJsonArray("parts")
                            .get(0).getAsJsonObject().get("text").getAsString();

                    String transcription = "", reponse = texte;
                    if (texte.contains("[TRANSCRIPTION:")) {
                        int s = texte.indexOf("[TRANSCRIPTION:") + 15;
                        int e2 = texte.indexOf("]", s);
                        if (e2 > s) {
                            transcription = texte.substring(s, e2).trim();
                            reponse = texte.substring(e2 + 1).trim();
                        }
                    }

                    final String trans = transcription, rep = reponse;
                    historique.add(userMsg);
                    JsonObject am = new JsonObject(); am.addProperty("role", "model");
                    JsonArray ap = new JsonArray(); JsonObject apt = new JsonObject();
                    apt.addProperty("text", texte); ap.add(apt); am.add("parts", ap);
                    historique.add(am);

                    Platform.runLater(() -> {
                        if (!trans.isEmpty()) {
                            if (txtMessage != null) { txtMessage.setText(trans); txtMessage.positionCaret(trans.length()); }
                            afficherMessageConducteur(trans);
                        }
                        traiterReponseGemini(rep, trans.isEmpty() ? "[audio]" : trans);
                        // FIX 8 : cacher l'indicateur ICI seulement, quand tout est termine
                        cacherIndicateur();
                        setStatut("Pret", "#10B981");
                        transcriptionEnCours = false;
                    });

                } else {
                    LOGGER.warning("Gemini erreur: " + response.statusCode() + " " + response.body());
                    Platform.runLater(() -> {
                        setStatut("Transcription echouee - tapez votre message", "#F59E0B");
                        cacherIndicateur();
                        transcriptionEnCours = false;
                    });
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur transcription", e);
                Platform.runLater(() -> {
                    setStatut("Erreur transcription", "#EF4444");
                    cacherIndicateur();
                    transcriptionEnCours = false;
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private byte[] pcmToWav(byte[] pcmData, AudioFormat fmt) throws IOException {
        int ch = fmt.getChannels(), sr = (int)fmt.getSampleRate(), bd = fmt.getSampleSizeInBits();
        int br = sr*ch*bd/8, ba = ch*bd/8, ds = pcmData.length;
        ByteArrayOutputStream wav = new ByteArrayOutputStream();
        wav.write("RIFF".getBytes()); wav.write(intToLe(36+ds));
        wav.write("WAVE".getBytes()); wav.write("fmt ".getBytes());
        wav.write(intToLe(16)); wav.write(shortToLe(1)); wav.write(shortToLe(ch));
        wav.write(intToLe(sr)); wav.write(intToLe(br)); wav.write(shortToLe(ba));
        wav.write(shortToLe(bd)); wav.write("data".getBytes());
        wav.write(intToLe(ds)); wav.write(pcmData);
        return wav.toByteArray();
    }
    private byte[] intToLe(int v) { return new byte[]{(byte)v,(byte)(v>>8),(byte)(v>>16),(byte)(v>>24)}; }
    private byte[] shortToLe(int v) { return new byte[]{(byte)v,(byte)(v>>8)}; }
    private String getLangLabel() { return switch(langueActuelle){case"ar"->"arabe";case"en"->"anglais";default->"francais";}; }

    // ════════════════════════════════════════════════════
    //  APPEL GEMINI TEXTE
    // ════════════════════════════════════════════════════

    private String appelGemini(String msg) {
        try {
            String ctx = "";
            if (trajetEnCours != null) {
                double p = trajetEnCours.getIdVoiture() != null
                        ? trajetEnCours.getIdVoiture().getPrixKm() * trajetEnCours.getDistanceKm() : 0;
                ctx = String.format("\n[TRAJET] %s->%s | %.0f km | %d pers | %.2f DT",
                        trajetEnCours.getPointDepart(), trajetEnCours.getPointArrivee(),
                        trajetEnCours.getDistanceKm(), trajetEnCours.getNbPersonnes(), p);
            }
            JsonObject body = new JsonObject();
            JsonArray contents = new JsonArray();
            if (historique.isEmpty()) {
                JsonObject su = new JsonObject(); su.addProperty("role","user");
                JsonArray sp = new JsonArray(); JsonObject spt = new JsonObject();
                spt.addProperty("text", SYSTEM_PROMPT+ctx); sp.add(spt); su.add("parts",sp);
                JsonObject sm = new JsonObject(); sm.addProperty("role","model");
                JsonArray mp = new JsonArray(); JsonObject mpt = new JsonObject();
                mpt.addProperty("text","Compris."); mp.add(mpt); sm.add("parts",mp);
                historique.add(su); historique.add(sm);
            }
            int start = Math.max(0, historique.size()-8);
            for (int i=start; i<historique.size(); i++) contents.add(historique.get(i));
            JsonObject um = new JsonObject(); um.addProperty("role","user");
            JsonArray up = new JsonArray(); JsonObject upt = new JsonObject();
            upt.addProperty("text",msg); up.add(upt); um.add("parts",up);
            contents.add(um); body.add("contents",contents);
            JsonObject gc = new JsonObject(); gc.addProperty("temperature",0.7);
            gc.addProperty("maxOutputTokens",300); body.add("generationConfig",gc);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_ENDPOINT+GEMINI_API_KEY))
                    .header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body))).build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode()==200) {
                JsonObject json = gson.fromJson(res.body(), JsonObject.class);
                String texte = json.getAsJsonArray("candidates").get(0).getAsJsonObject()
                        .getAsJsonObject("content").getAsJsonArray("parts")
                        .get(0).getAsJsonObject().get("text").getAsString();
                historique.add(um);
                JsonObject am = new JsonObject(); am.addProperty("role","model");
                JsonArray ap = new JsonArray(); JsonObject apt = new JsonObject();
                apt.addProperty("text",texte); ap.add(apt); am.add("parts",ap);
                historique.add(am);
                return texte;
            } else { LOGGER.warning("Gemini HTTP: "+res.statusCode()); return getMessageErreur(); }
        } catch (Exception e) { LOGGER.log(Level.SEVERE,"Erreur Gemini",e); return getMessageErreur(); }
    }

    // ════════════════════════════════════════════════════
    //  INTENTIONS / SIGNALEMENT / RAPPORT
    // ════════════════════════════════════════════════════

    private void traiterReponseGemini(String rep, String orig) {
        if (rep==null||rep.isBlank()) return;
        if (rep.startsWith("[SIGNALEMENT]")) {
            afficherMessageAssistant(rep.replace("[SIGNALEMENT]","").trim());
            enregistrerSignalement(orig); mettreAJourStatutProbleme("Signalement enregistre");
        } else if (rep.startsWith("[NAVIGATION]")) {
            afficherMessageAssistant(rep.replace("[NAVIGATION]","").trim());
            afficherCarteNavigation(orig);
        } else if (rep.startsWith("[RAPPORT]")) {
            afficherMessageAssistant(rep.replace("[RAPPORT]","").trim());
            genererRapportComplet();
        } else { afficherMessageAssistant(rep); }
    }

    private void signalerProblemeRapide(String type) {
        afficherMessageConducteur("Signaler: " + type);
        setStatut("Signalement...", "#F59E0B");
        Thread t = new Thread(() -> {
            String rep = appelGemini("Signaler: " + type + ". Confirme et donne conseils securite.");
            Platform.runLater(() -> {
                enregistrerSignalement(type); afficherMessageAssistant(rep);
                mettreAJourStatutProbleme(type+" signale"); setStatut("Pret","#10B981");
            });
        });
        t.setDaemon(true); t.start();
    }

    @FXML
    private void genererRapport() {
        if (trajetEnCours==null) { afficherMessageAssistant("Aucun trajet actif."); return; }
        setStatut("Generation rapport...", "#6366F1");
        Thread t = new Thread(() -> {
            String rep = appelGemini(buildRapportPrompt());
            Platform.runLater(() -> { genererRapportComplet(); afficherMessageAssistant(rep); setStatut("Rapport genere","#10B981"); });
        });
        t.setDaemon(true); t.start();
    }

    private String buildRapportPrompt() {
        if (trajetEnCours==null) return "Rapport vide.";
        double p = trajetEnCours.getIdVoiture()!=null
                ? trajetEnCours.getIdVoiture().getPrixKm()*trajetEnCours.getDistanceKm() : 0;
        return String.format("Rapport trajet %s->%s | %.0f km | %d pers | %.2f DT | gain %.2f DT | %s. En %s, 3 phrases.",
                trajetEnCours.getPointDepart(), trajetEnCours.getPointArrivee(),
                trajetEnCours.getDistanceKm(), trajetEnCours.getNbPersonnes(), p, p*0.85,
                new SimpleDateFormat("dd/MM/yyyy HH:mm").format(trajetEnCours.getDateReservation()),
                langueActuelle.equals("ar")?"arabe":langueActuelle.equals("en")?"anglais":"francais");
    }

    private void genererRapportComplet() {
        if (trajetEnCours==null) return;
        double p = trajetEnCours.getIdVoiture()!=null
                ? trajetEnCours.getIdVoiture().getPrixKm()*trajetEnCours.getDistanceKm() : 0;
        afficherBlocRapport(String.format("""
            ══════════════════════════
            RAPPORT DE TRAJET
            ══════════════════════════
            Date  : %s
            Trajet: %s -> %s
            Dist. : %.0f km | Pers.: %d
            Prix  : %.2f DT
            Gain  : %.2f DT
            ══════════════════════════""",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                trajetEnCours.getPointDepart(), trajetEnCours.getPointArrivee(),
                trajetEnCours.getDistanceKm(), trajetEnCours.getNbPersonnes(), p, p*0.85));
    }

    private void afficherCarteNavigation(String orig) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color:#EFF6FF;-fx-background-radius:12;-fx-padding:12;" +
                "-fx-border-color:#93C5FD;-fx-border-radius:12;-fx-border-width:1;");
        Label t = new Label("Navigation activee"); t.setStyle("-fx-font-weight:bold;-fx-text-fill:#1D4ED8;");
        Label i = new Label("Itineraire calcule"); i.setStyle("-fx-text-fill:#374151;-fx-font-size:12px;"); i.setWrapText(true);
        card.getChildren().addAll(t, i);
        Platform.runLater(() -> { chatContainer.getChildren().add(card); scrollToBottom(); });
    }

    private void enregistrerSignalement(String type) {
        LOGGER.info("SIGNALEMENT [" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "] : " + type);
    }

    private void mettreAJourStatutProbleme(String s) {
        if (lblProblemeStatus!=null) { lblProblemeStatus.setText(s); lblProblemeStatus.setStyle("-fx-text-fill:#DC2626;-fx-font-weight:bold;"); }
    }

    // ════════════════════════════════════════════════════
    //  AFFICHAGE MESSAGES
    // ════════════════════════════════════════════════════

    private void afficherMessageConducteur(String texte) {
        HBox c = new HBox(); c.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label b = new Label(texte); b.setWrapText(true); b.setMaxWidth(320);
        b.setStyle("-fx-background-color:#3B82F6;-fx-text-fill:white;-fx-background-radius:18 18 4 18;-fx-padding:10 14;-fx-font-size:13px;");
        Label t = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        t.setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:10px;");
        VBox m = new VBox(3,b,t); m.setAlignment(javafx.geometry.Pos.CENTER_RIGHT); c.getChildren().add(m);
        Platform.runLater(() -> { chatContainer.getChildren().add(c); scrollToBottom(); });
    }

    // ════════════════════════════════════════════════════
    //  SYNTHÈSE VOCALE (TTS)
    // ════════════════════════════════════════════════════

    /**
     * Lit le texte à voix haute via Google Translate TTS (gratuit, sans clé).
     * Appelé automatiquement après chaque réponse de l'assistant.
     */
    private void parler(String texte) {
        if (texte == null || texte.isBlank()) return;

        // Nettoyer le texte : enlever préfixes internes et tronquer à 200 chars max
        String textePropre = texte
                .replace("[SIGNALEMENT]", "")
                .replace("[NAVIGATION]", "")
                .replace("[RAPPORT]", "")
                .trim();

        // Google TTS supporte max ~200 caractères par requête
        if (textePropre.length() > 200) textePropre = textePropre.substring(0, 200);

        final String texteFinal = textePropre;
        final String langue = switch (langueActuelle) {
            case "ar" -> "ar";
            case "en" -> "en";
            default   -> "fr";
        };

        Thread ttsThread = new Thread(() -> {
            try {
                // URL Google Translate TTS (gratuit, pas de clé requise)
                String encoded = URLEncoder.encode(texteFinal, StandardCharsets.UTF_8);
                String ttsUrl = "https://translate.google.com/translate_tts?ie=UTF-8&tl=" + langue
                        + "&client=tw-ob&q=" + encoded;

                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .build();

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ttsUrl))
                        // User-Agent obligatoire sinon Google bloque
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .header("Referer", "https://translate.google.com/")
                        .GET()
                        .build();

                HttpResponse<byte[]> response = client.send(req, HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() == 200) {
                    byte[] mp3Data = response.body();
                    jouerAudio(mp3Data);
                } else {
                    LOGGER.warning("TTS erreur HTTP: " + response.statusCode());
                }

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Erreur TTS", e);
            }
        });
        ttsThread.setDaemon(true);
        ttsThread.start();
    }

    /**
     * Joue les bytes MP3 reçus via javax.sound (AudioSystem).
     */
    private void jouerAudio(byte[] mp3Bytes) {
        try (InputStream is = new ByteArrayInputStream(mp3Bytes);
             AudioInputStream audioIn = AudioSystem.getAudioInputStream(is)) {

            AudioFormat baseFormat = audioIn.getFormat();
            // Convertir MP3 → PCM pour la lecture
            AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );

            try (AudioInputStream decodedIn = AudioSystem.getAudioInputStream(decodedFormat, audioIn)) {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
                try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                    line.open(decodedFormat);
                    line.start();

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = decodedIn.read(buffer, 0, buffer.length)) != -1) {
                        line.write(buffer, 0, bytesRead);
                    }
                    line.drain();
                }
            }
        } catch (Exception e) {
            // MP3 natif non supporté par Java sans lib externe → fallback
            LOGGER.warning("TTS lecture MP3 echouee (MP3 SPI manquant?): " + e.getMessage());
            jouerAudioFallback(mp3Bytes);
        }
    }

    /**
     * Fallback : écrire l'MP3 dans un fichier temp et l'ouvrir avec le lecteur système.
     * Fonctionne sur Windows/Mac/Linux sans dépendance supplémentaire.
     */
    private void jouerAudioFallback(byte[] mp3Bytes) {
        try {
            java.io.File tmpFile = java.io.File.createTempFile("re7la_tts_", ".mp3");
            tmpFile.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                fos.write(mp3Bytes);
            }
            // Ouvrir avec le lecteur par défaut du système
            java.awt.Desktop.getDesktop().open(tmpFile);
        } catch (Exception e) {
            LOGGER.warning("TTS fallback echoue: " + e.getMessage());
        }
    }

    private void afficherMessageAssistant(String texte) {
        HBox c = new HBox(10); c.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label av = new Label("AI"); av.setStyle("-fx-background-color:#6366F1;-fx-background-radius:20;-fx-padding:6 8;-fx-font-size:12px;-fx-text-fill:white;-fx-font-weight:bold;");
        Label b = new Label(texte); b.setWrapText(true); b.setMaxWidth(320);
        b.setStyle("-fx-background-color:#F3F4F6;-fx-text-fill:#111827;-fx-background-radius:18 18 18 4;-fx-padding:10 14;-fx-font-size:13px;");
        Label t = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        t.setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:10px;");
        VBox m = new VBox(3,b,t); c.getChildren().addAll(av,m);
        FadeTransition f = new FadeTransition(Duration.millis(300),c); f.setFromValue(0); f.setToValue(1);
        Platform.runLater(() -> { chatContainer.getChildren().add(c); f.play(); scrollToBottom(); });
        // TTS : lire la réponse à voix haute
        parler(texte);
    }

    private void afficherBlocRapport(String texte) {
        Label r = new Label(texte); r.setWrapText(true);
        r.setStyle("-fx-background-color:#F0FDF4;-fx-text-fill:#14532D;-fx-background-radius:12;-fx-padding:14;-fx-font-family:Monospace;-fx-font-size:11px;-fx-border-color:#86EFAC;-fx-border-radius:12;-fx-border-width:1;");
        Platform.runLater(() -> { chatContainer.getChildren().add(r); scrollToBottom(); });
    }

    private void scrollToBottom() { chatScroll.setVvalue(1.0); }

    @FXML
    private void effacerConversation() {
        chatContainer.getChildren().clear(); historique.clear(); afficherMessageAssistant(getMessageAccueil());
    }

    private void setStatut(String msg, String couleur) {
        Platform.runLater(() -> {
            if (lblStatut!=null) { lblStatut.setText(msg); lblStatut.setStyle("-fx-text-fill:"+couleur+";-fx-font-size:12px;"); }
        });
    }

    private String getMessageAccueil() {
        return switch(langueActuelle) {
            case "ar" -> "مرحباً! أنا مساعدك في RE7LA\nالملاحة، الإبلاغ، التقارير.\nكيف يمكنني مساعدتك؟";
            case "en" -> "Hello! I'm your RE7LA assistant\nNavigation, reporting, trip reports.\nHow can I help?";
            default   -> "Bonjour ! Assistant RE7LA\nNavigation, signalement, rapports de trajet.\nComment puis-je vous aider ?";
        };
    }

    private String getMessageErreur() {
        return switch(langueActuelle) {
            case "ar" -> "خطأ في الاتصال. حاول مجدداً.";
            case "en" -> "Connection error. Please try again.";
            default   -> "Erreur de connexion. Reessayez.";
        };
    }

    private String getBtnMicroStyle(boolean actif) {
        String color = actif ? "#EF4444" : "#6366F1";
        return "-fx-background-color:"+color+";-fx-text-fill:white;-fx-background-radius:50;" +
                "-fx-min-width:60;-fx-min-height:60;-fx-font-size:14px;-fx-cursor:hand;";
    }
}

package org.example.Controllers.activite;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.Entites.activite.VirtualTour;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ResourceBundle;

/**
 * 🎬 Contrôleur pour le visualiseur de visite virtuelle
 */
public class VirtualTourController implements Initializable {

    // ========== UI ELEMENTS ==========
    @FXML private Label lblTitle;
    @FXML private Label lblSubtitle;
    @FXML private Label lblPhotoCounter;
    @FXML private TextArea lblNarration;   // ✅ TextArea : fond contrôlable, texte toujours lisible
    @FXML private Label lblCurrentTime;
    @FXML private Label lblTotalTime;
    @FXML private Label lblVolume;
    @FXML private Label lblLoadingStatus;
    @FXML private Label lblAudioStatus;    // ✅ Message d'erreur audio visible

    @FXML private ImageView photoView;
    @FXML private StackPane photoContainer;
    @FXML private StackPane loadingOverlay;

    @FXML private Button btnBack;
    @FXML private Button btnDownload;
    @FXML private Button btnShare;
    @FXML private Button btnSettings;
    @FXML private Button btnPlayPause;
    @FXML private Button btnPrevious;
    @FXML private Button btnNext;
    @FXML private Button btnPrevPhoto;
    @FXML private Button btnNextPhoto;

    @FXML private ProgressBar audioProgressBar;
    @FXML private Slider volumeSlider;
    @FXML private CheckBox chkAutoPlay;

    // ========== DATA ==========
    private VirtualTour tour;
    private MediaPlayer mediaPlayer;
    private int currentPhotoIndex = 0;
    private boolean isPlaying = false;
    private Timeline photoTimeline;

    // Cache local des images téléchargées : URL → chemin local
    private final Map<String, String> imageCache = new LinkedHashMap<>();
    private final ExecutorService downloadExecutor = Executors.newFixedThreadPool(3);

    // Dossier de cache local
    private static final String CACHE_DIR = "src/main/resources/images/cache/";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupVolumeControl();
        setupAutoPlayToggle();
        // Créer le dossier cache si nécessaire
        try { Files.createDirectories(Paths.get(CACHE_DIR)); } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CHARGEMENT DE LA VISITE
    // ═══════════════════════════════════════════════════════════════════════════

    public void loadTour(VirtualTour tour) {
        this.tour = tour;

        lblTitle.setText("🎨 Découvrez " + tour.getLieu());
        lblSubtitle.setText("Visite virtuelle avec " + tour.getPhotoUrls().size() + " photos");
        lblNarration.setText(tour.getNarration());

        // ✅ Vider le cache mémoire : chaque visite a ses propres URLs
        imageCache.clear();

        // ✅ Fermeture propre si l'utilisateur clique sur le X de la fenêtre
        Platform.runLater(() -> {
            Stage stage = (Stage) btnBack.getScene().getWindow();
            stage.setOnCloseRequest(e -> {
                if (mediaPlayer != null) mediaPlayer.stop();
                downloadExecutor.shutdownNow();
            });
        });

        // Télécharger toutes les images en arrière-plan AVANT d'afficher
        showLoading("⬇️  Téléchargement des photos...");
        downloadAllImages(tour.getPhotoUrls(), () -> {
            Platform.runLater(() -> {
                currentPhotoIndex = 0;
                displayPhoto(0);
                loadAudio();
                loadingOverlay.setVisible(false);
            });
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TÉLÉCHARGEMENT LOCAL DES IMAGES (FIX PRINCIPAL)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Télécharge toutes les images localement en parallèle,
     * puis appelle onComplete quand tout est prêt.
     */
    private void downloadAllImages(List<String> urls, Runnable onComplete) {
        if (urls == null || urls.isEmpty()) {
            onComplete.run();
            return;
        }

        // Compteur thread-safe
        int[] remaining = {urls.size()};

        for (int i = 0; i < urls.size(); i++) {
            final String url = urls.get(i);
            final int index = i;

            downloadExecutor.submit(() -> {
                try {
                    String localPath = downloadImage(url, index);
                    if (localPath != null) {
                        imageCache.put(url, localPath);
                        System.out.println("✅ Image " + (index+1) + " mise en cache : " + localPath);
                    } else {
                        System.err.println("❌ Échec image " + (index+1) + " : " + url);
                    }
                } catch (Exception e) {
                    System.err.println("❌ Erreur image " + (index+1) + " : " + e.getMessage());
                } finally {
                    synchronized (remaining) {
                        remaining[0]--;
                        Platform.runLater(() ->
                                lblLoadingStatus.setText("⬇️  Photos : " +
                                        (urls.size() - remaining[0]) + "/" + urls.size())
                        );
                        if (remaining[0] == 0) {
                            onComplete.run();
                        }
                    }
                }
            });
        }
    }

    /**
     * Télécharge une image depuis une URL et la sauvegarde localement.
     * Le nom de fichier est basé sur un hash de l'URL pour être unique par image
     * et éviter les collisions entre différents lieux.
     * Retourne le chemin local, ou null en cas d'échec.
     */
    private String downloadImage(String imageUrl, int index) {
        String ext = imageUrl.contains(".png") ? ".png" : ".jpg";

        // ✅ Hash de l'URL → nom de fichier unique par image, pas par index
        // Empêche photo_0.jpg de Mahdia d'être réutilisée pour Sidi Bou Said
        String urlHash = String.format("%08x", imageUrl.hashCode() & 0xFFFFFFFFL);
        String localPath = CACHE_DIR + "img_" + urlHash + ext;

        // Si déjà en cache sur disque avec cette URL précise, réutiliser
        File cachedFile = new File(localPath);
        if (cachedFile.exists() && cachedFile.length() > 5000) {
            System.out.println("📁 Cache hit image " + (index+1) + " (" + urlHash + ")");
            return localPath;
        }

        try {
            URL url = new URL(imageUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            // User-Agent requis par certaines APIs (Wikimedia)
            conn.setRequestProperty("User-Agent", "VirtualTourApp/1.0");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("  HTTP " + responseCode + " pour " + imageUrl);
                return null;
            }

            // Copier le flux vers le fichier local
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, Paths.get(localPath), StandardCopyOption.REPLACE_EXISTING);
            }

            // Vérifier que le fichier est valide (> 5 KB)
            if (cachedFile.exists() && cachedFile.length() > 5000) {
                return localPath;
            } else {
                System.err.println("  Fichier trop petit, image invalide");
                cachedFile.delete();
                return null;
            }

        } catch (Exception e) {
            System.err.println("  Download échoué : " + e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  AFFICHAGE DES PHOTOS (depuis le cache local)
    // ═══════════════════════════════════════════════════════════════════════════

    private void displayPhoto(int index) {
        if (tour == null || tour.getPhotoUrls().isEmpty()) return;

        currentPhotoIndex = index;
        String photoUrl = tour.getPhotoUrls().get(index);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), photoView);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(e -> {
            Image image = null;

            // Essai 1 : depuis le cache local (priorité)
            String localPath = imageCache.get(photoUrl);
            if (localPath != null) {
                File localFile = new File(localPath);
                if (localFile.exists()) {
                    try {
                        image = new Image(localFile.toURI().toString());
                        System.out.println("📁 Image " + (index+1) + " depuis cache local");
                    } catch (Exception ex) {
                        System.err.println("❌ Erreur lecture cache : " + ex.getMessage());
                    }
                }
            }

            // Essai 2 : depuis l'URL distante (fallback)
            if (image == null || image.isError()) {
                try {
                    System.out.println("🌐 Image " + (index+1) + " depuis URL distante");
                    image = new Image(photoUrl, true);
                } catch (Exception ex) {
                    System.err.println("❌ Erreur URL distante : " + ex.getMessage());
                }
            }

            // Essai 3 : image placeholder si tout échoue
            if (image == null || image.isError()) {
                System.err.println("⚠️  Image " + (index+1) + " indisponible — placeholder");
                image = createPlaceholderImage();
            }

            final Image finalImage = image;
            photoView.setImage(finalImage);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), photoView);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });

        fadeOut.play();

        lblPhotoCounter.setText((index + 1) + "/" + tour.getPhotoUrls().size());
        btnPrevPhoto.setDisable(index == 0);
        btnNextPhoto.setDisable(index == tour.getPhotoUrls().size() - 1);
    }

    /**
     * Crée une image placeholder grise quand aucune source n'est disponible
     */
    private Image createPlaceholderImage() {
        // Utilise une ressource locale dans le projet si disponible
        try {
            InputStream placeholder = getClass().getResourceAsStream("/images/placeholder.jpg");
            if (placeholder != null) return new Image(placeholder);
        } catch (Exception ignored) {}
        // Sinon image vide 1x1 pixel transparente
        return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  AUDIO (inchangé)
    // ═══════════════════════════════════════════════════════════════════════════

    private void loadAudio() {
        // ✅ Vérification explicite du path avant tout
        if (tour.getAudioPath() == null || tour.getAudioPath().isBlank()) {
            System.err.println("❌ Pas de fichier audio (génération gTTS échouée)");
            showAudioError("Narration audio indisponible — vérifiez que Python et gTTS sont installés.");
            return;
        }

        try {
            File audioFile = new File(tour.getAudioPath());
            if (!audioFile.exists()) {
                System.err.println("❌ Fichier audio non trouvé : " + tour.getAudioPath());
                showAudioError("Fichier audio introuvable : " + tour.getAudioPath());
                return;
            }

            Media media = new Media(audioFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(volumeSlider.getValue() / 100.0);
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> updateProgress());
            mediaPlayer.setOnEndOfMedia(this::handleAudioEnded);
            mediaPlayer.setOnReady(() -> {
                lblTotalTime.setText(formatTime(mediaPlayer.getTotalDuration()));
                // Afficher la durée réelle dès que le media est prêt
                System.out.println("✅ Audio prêt : " + formatTime(mediaPlayer.getTotalDuration()));
            });
            mediaPlayer.setOnError(() -> {
                System.err.println("❌ Erreur MediaPlayer : " + mediaPlayer.getError());
                showAudioError("Erreur de lecture audio.");
            });

            System.out.println("✅ Audio chargé : " + tour.getAudioPath());
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement audio : " + e.getMessage());
            e.printStackTrace();
            showAudioError("Impossible de charger l'audio : " + e.getMessage());
        }
    }

    /**
     * Affiche un message d'erreur rouge sous la barre de progression audio.
     */
    private void showAudioError(String message) {
        Platform.runLater(() -> {
            if (lblAudioStatus != null) {
                lblAudioStatus.setText("⚠ " + message);
                lblAudioStatus.setVisible(true);
                lblAudioStatus.setManaged(true);
            }
            // Désactiver les contrôles audio inutilisables
            btnPlayPause.setDisable(true);
            btnPrevious.setDisable(true);
            btnNext.setDisable(true);
        });
    }

    private void updateProgress() {
        if (mediaPlayer == null) return;
        Duration current = mediaPlayer.getCurrentTime();
        Duration total = mediaPlayer.getTotalDuration();
        if (total != null && total.toMillis() > 0) {
            double progress = current.toMillis() / total.toMillis();
            audioProgressBar.setProgress(progress);
            lblCurrentTime.setText(formatTime(current));
            if (chkAutoPlay.isSelected()) autoChangePhoto(progress);
        }
    }

    private void autoChangePhoto(double progress) {
        int totalPhotos = tour.getPhotoUrls().size();
        int targetIndex = (int) (progress * totalPhotos);
        if (targetIndex != currentPhotoIndex && targetIndex < totalPhotos) {
            displayPhoto(targetIndex);
        }
    }

    private String formatTime(Duration duration) {
        int seconds = (int) duration.toSeconds();
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private void handleAudioEnded() {
        isPlaying = false;
        btnPlayPause.setText("▶");
        mediaPlayer.seek(Duration.ZERO);
        audioProgressBar.setProgress(0);
        displayPhoto(0);
    }

    private void setupVolumeControl() {
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            lblVolume.setText(String.format("%.0f%%", newVal.doubleValue()));
            if (mediaPlayer != null) mediaPlayer.setVolume(newVal.doubleValue() / 100.0);
        });
    }

    private void setupAutoPlayToggle() {
        chkAutoPlay.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && photoTimeline != null) photoTimeline.stop();
        });
    }

    // ========== EVENT HANDLERS ==========

    @FXML private void handlePlayPause() {
        if (mediaPlayer == null) return;
        if (isPlaying) { mediaPlayer.pause(); btnPlayPause.setText("▶"); }
        else           { mediaPlayer.play();  btnPlayPause.setText("⏸"); }
        isPlaying = !isPlaying;
    }

    @FXML private void handlePrevious() {
        if (mediaPlayer == null) return;
        Duration newTime = mediaPlayer.getCurrentTime().subtract(Duration.seconds(10));
        mediaPlayer.seek(newTime.lessThan(Duration.ZERO) ? Duration.ZERO : newTime);
    }

    @FXML private void handleNext() {
        if (mediaPlayer == null) return;
        Duration newTime = mediaPlayer.getCurrentTime().add(Duration.seconds(10));
        Duration total = mediaPlayer.getTotalDuration();
        mediaPlayer.seek(newTime.greaterThan(total) ? total : newTime);
    }

    @FXML private void handlePreviousPhoto() {
        if (currentPhotoIndex > 0) displayPhoto(currentPhotoIndex - 1);
    }

    @FXML private void handleNextPhoto() {
        if (currentPhotoIndex < tour.getPhotoUrls().size() - 1) displayPhoto(currentPhotoIndex + 1);
    }

    @FXML private void handleBack() {
        if (mediaPlayer != null) mediaPlayer.stop();
        downloadExecutor.shutdownNow(); // Arrêter les téléchargements en cours
        ((Stage) btnBack.getScene().getWindow()).close();
    }

    @FXML private void handleDownload() {
        if (tour == null || tour.getAudioPath() == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Télécharger la narration audio");
        fc.setInitialFileName(tour.getLieu().replaceAll("\\s+", "_") + ".mp3");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers MP3", "*.mp3"));
        File dest = fc.showSaveDialog((Stage) btnDownload.getScene().getWindow());
        if (dest != null) {
            try {
                Files.copy(new File(tour.getAudioPath()).toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                new Alert(Alert.AlertType.INFORMATION, "Sauvegardé : " + dest.getName()).showAndWait();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait();
            }
        }
    }

    @FXML private void handleShare() {
        new Alert(Alert.AlertType.INFORMATION,
                "📍 " + tour.getLieu() + "\n📸 " + tour.getPhotoUrls().size() + " photos\n" +
                        "⏱️  " + tour.getDurationSeconds() + "s\n\n" +
                        "Lien : re7la.tn/tours/" + tour.getLieu().toLowerCase().replaceAll("\\s+", "-")
        ).showAndWait();
    }

    @FXML private void handleSettings() {
        new Alert(Alert.AlertType.INFORMATION,
                "✅ Changement automatique de photo\n✅ Narration audio synchronisée\n\nVersion : 1.0"
        ).showAndWait();
    }

    public void showLoading(String message) {
        Platform.runLater(() -> { lblLoadingStatus.setText(message); loadingOverlay.setVisible(true); });
    }

    public void hideLoading() {
        Platform.runLater(() -> loadingOverlay.setVisible(false));
    }
}
package org.example.Controllers.Trajet;

import com.itextpdf.text.Font;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Alert;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.example.Entites.trajet.StatutVoiture;
import org.example.Entites.trajet.Trajet;
import org.example.Entites.trajet.Voiture;
import org.example.Entites.user.User;
import org.example.Services.trajet.TrajetCRUD;
import org.example.Services.trajet.VoitureCRUD;
import org.example.Services.user.APIservices.JWTService;
import org.example.Utils.UserSession;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

public class PageAddVoiture {

    public PageAddVoiture() {}

    private static final Logger LOGGER = Logger.getLogger(PageAddVoiture.class.getName());

    // ================= FXML ORIGINAUX =================

    @FXML private Label prixTotalLabel;
    @FXML private Label disponiblesLabel;
    @FXML private ScrollPane viewVoitures;
    @FXML private ScrollPane viewTrajets;
    @FXML private CheckBox avecChauffeurCheck;
    @FXML private TableColumn<Voiture, Boolean> colAvecChauffeur;

    @FXML private TableView<Voiture> tableVoitures;
    @FXML private TableColumn<Voiture, Integer> colIdVoiture;
    @FXML private TableColumn<Voiture, String>  colMarque;
    @FXML private TableColumn<Voiture, String>  colModele;
    @FXML private TableColumn<Voiture, String>  colImmatriculation;
    @FXML private TableColumn<Voiture, Float>   colPrixKm;
    @FXML private TableColumn<Voiture, Integer> colNbPlaces;
    @FXML private TableColumn<Voiture, Boolean> colDisponibilite;
    @FXML private TableColumn<Voiture, String>  colImage;
    @FXML private TableColumn<Voiture, String>  colDescription;

    @FXML private TextField marqueField;
    @FXML private TextField  modeleField;
    @FXML private TextField  immatriculationField;
    @FXML private TextField  prixKmField;
    @FXML private TextField  nbPlacesField;
    @FXML private TextField  imageField;
    @FXML private TextArea   descriptionField;
    @FXML private CheckBox   disponibiliteCheck;

    @FXML private TableView<Trajet>              tableTrajets;
    @FXML private TableColumn<Trajet, Integer>   colIdTrajet;
    @FXML private TableColumn<Trajet, String>    colDepart;
    @FXML private TableColumn<Trajet, String>    colArrivee;
    @FXML private TableColumn<Trajet, Float>     colDistance;
    @FXML private TableColumn<Trajet, Date>      colDate;
    @FXML private TableColumn<Trajet, Integer>   colNbPersonnes;
    @FXML private TableColumn<Trajet, StatutVoiture> colStatut;

    @FXML private Label nbVoituresLabel;
    @FXML private Label nbTrajetsLabel;
    @FXML private Label trajetsEnCoursLabel;
    @FXML private Label trajetsTerminesLabel;

    @FXML private HBox  paginationContainer;
    @FXML private Label paginationLabel;

    @FXML private Button btnGestionVoiture;
    @FXML private Button btnGestionHebergement;
    @FXML private Button btnGestionUtilisateurs;
    @FXML private Button btnDeconnexion;

    // ================= NOUVEAUX FXML =================

    @FXML private TextField searchVoituresField;
    @FXML private TextField searchTrajetsField;

    @FXML private Button btnStatistiques;
    @FXML private Button btnAfficherPDF;
    @FXML private Button btnPdfVoitures;
    @FXML private Button btnPdfTrajets;

    @FXML private HBox  paginationTrajetContainer;
    @FXML private Label paginationTrajetLabel;

    // ================= INFOS UTILISATEUR =================
    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label userRoleLabel;

    // ================= NOTIFICATIONS =================
    @FXML private Label notificationBadge;
    @FXML private Button btnNotifications;

    // ================= SERVICES =================

    private final VoitureCRUD voitureService = new VoitureCRUD();
    private final TrajetCRUD  trajetService  = new TrajetCRUD();

    // ================= LISTES =================

    private final ObservableList<Voiture> allVoitures      = FXCollections.observableArrayList();
    private final ObservableList<Voiture> filteredVoitures = FXCollections.observableArrayList();
    private final ObservableList<Trajet>  trajetList        = FXCollections.observableArrayList();
    private final ObservableList<Trajet>  filteredTrajets   = FXCollections.observableArrayList();

    // ================= PAGINATION =================

    private int currentPage   = 1;
    private final int itemsPerPage = 10;
    private int totalItems    = 0;
    private int totalPages    = 0;

    private int currentPageTrajet = 1;
    private int totalTrajets      = 0;
    private int totalPagesTrajet  = 0;

    // ================= UTILISATEUR CONNECTÉ =================
    private User currentUser;
    private String userToken;

    // ================================================================
    //  INITIALIZE
    // ================================================================

    @FXML
    public void initialize() {
        // ✅ 1. VÉRIFICATION JWT
        if (!checkUserAuth()) {
            return;
        }

        // ✅ 2. AFFICHER LES INFOS UTILISATEUR
        displayUserInfo();

        // Action PDF
        btnAfficherPDF.setOnAction(e -> ouvrirPDF());

        // --- colonnes voitures ---
        colAvecChauffeur.setCellValueFactory(new PropertyValueFactory<>("avecChauffeur"));
        colIdVoiture.setCellValueFactory(new PropertyValueFactory<>("idVoiture"));
        colMarque.setCellValueFactory(new PropertyValueFactory<>("marque"));
        colModele.setCellValueFactory(new PropertyValueFactory<>("modele"));
        colImmatriculation.setCellValueFactory(new PropertyValueFactory<>("immatriculation"));
        colPrixKm.setCellValueFactory(new PropertyValueFactory<>("prixKm"));
        colNbPlaces.setCellValueFactory(new PropertyValueFactory<>("nb_places"));
        colDisponibilite.setCellValueFactory(new PropertyValueFactory<>("disponibilite"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        colImage.setCellValueFactory(new PropertyValueFactory<>("image"));
        colImage.setCellFactory(column -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);
                if (empty || imagePath == null || imagePath.isEmpty()) {
                    setGraphic(null); return;
                }
                File file = new File(imagePath);
                Image image = file.exists()
                        ? new Image(file.toURI().toString(), 100, 60, true, true)
                        : new Image("file:images/default.png", 100, 60, true, true);
                imageView.setImage(image);
                setGraphic(imageView);
            }
        });

        // --- colonnes trajets ---
        colIdTrajet.setCellValueFactory(new PropertyValueFactory<>("idTrajet"));
        colDepart.setCellValueFactory(new PropertyValueFactory<>("pointDepart"));
        colArrivee.setCellValueFactory(new PropertyValueFactory<>("pointArrivee"));
        colDistance.setCellValueFactory(new PropertyValueFactory<>("distanceKm"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateReservation"));
        colNbPersonnes.setCellValueFactory(new PropertyValueFactory<>("nbPersonnes"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        loadVoitures();
        loadTrajets();
        initSelectionVoiture();
    }

    /**
     * ✅ Vérifie si l'utilisateur est authentifié
     */
    private boolean checkUserAuth() {
        if (!UserSession.getInstance().isTokenValid()) {
            Platform.runLater(() -> {
                showAlert("Session expirée", "Votre session a expiré. Veuillez vous reconnecter.");
                redirectToLogin();
            });
            return false;
        }

        currentUser = UserSession.getInstance().getCurrentUser();
        userToken = UserSession.getInstance().getToken();

        if (currentUser == null) {
            Platform.runLater(() -> {
                showAlert("Erreur", "Aucun utilisateur connecté.");
                redirectToLogin();
            });
            return false;
        }

        Integer userIdFromToken = JWTService.extractUserId(userToken);
        if (userIdFromToken == null || userIdFromToken != currentUser.getId()) {
            Platform.runLater(() -> {
                showAlert("Session invalide", "Incohérence détectée. Reconnexion nécessaire.");
                UserSession.getInstance().clearSession();
                redirectToLogin();
            });
            return false;
        }

        return true;
    }

    /**
     * ✅ Affiche les informations de l'utilisateur
     */
    private void displayUserInfo() {
        if (currentUser == null) return;

        if (userNameLabel  != null) userNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        if (userEmailLabel != null) userEmailLabel.setText(currentUser.getE_mail());
        if (userRoleLabel  != null) userRoleLabel.setText("Rôle: " + currentUser.getRole().name());

        System.out.println("\n🔐 === PAGE ADD VOITURE ===");
        System.out.println("👤 Utilisateur: " + currentUser.getE_mail());
        System.out.println("👑 Rôle: " + currentUser.getRole());
        System.out.println("🔐 ========================\n");
    }

    /**
     * ✅ Redirige vers la page de login
     */
    private void redirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) (btnGestionVoiture != null ?
                    btnGestionVoiture.getScene().getWindow() :
                    btnDeconnexion.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================================================================
    //  NAVIGATION
    // ================================================================

    @FXML
    private void handleShowUsers() {
        if (!checkUserAuth()) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/back/users.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnGestionUtilisateurs.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/user/back/users.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Gestion des Utilisateurs - RE7LA");
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "❌ Impossible de charger l'interface utilisateurs: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowHebergements() {
        if (!checkUserAuth()) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/hebergement/back/HebergementBack.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnGestionHebergement.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/hebergement/back/StyleHB.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Gestion des Hébergements - RE7LA");
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "❌ Impossible de charger l'interface hébergements: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowTrajets() {
        if (!checkUserAuth()) return;
        loadTrajets();
        showInfo("Liste des trajets actualisée");
    }

    @FXML
    private void handleShowPromotions() {
        if (!checkUserAuth()) return;
        showInfo("🎁 Gestion des Promotions - En cours de développement");
    }

    @FXML
    private void handleShowSettings() {
        if (!checkUserAuth()) return;
        showInfo("⚙️ Paramètres - En cours de développement");
    }

    @FXML
    private void handleViewNotifications() {
        if (!checkUserAuth()) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/back/adminNotifications.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("RE7LA Admin - Notifications");
            stage.setScene(new Scene(root));
            Stage parentStage = (Stage) btnNotifications.getScene().getWindow();
            stage.setX(parentStage.getX() + 100);
            stage.setY(parentStage.getY() + 100);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les notifications");
        }
    }

    // ================================================================
    //  DÉCONNEXION
    // ================================================================

    @FXML
    private void handleLogout() {
        try {
            if (currentUser != null) System.out.println("👋 Déconnexion de: " + currentUser.getE_mail());
            UserSession.getInstance().clearSession();
            System.out.println("✅ Session JWT effacée - Déconnexion réussie");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnDeconnexion.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de charger la page de connexion: " + e.getMessage());
        }
    }

    @FXML public void showVoitures() { viewVoitures.setVisible(true);  viewTrajets.setVisible(false); }
    @FXML public void showTrajets()  { viewVoitures.setVisible(false); viewTrajets.setVisible(true);  }

    @FXML
    private void openGestionVoiture() {
        loadVoitures();
        loadTrajets();
    }

    // ================================================================
    //  LOAD DATA
    // ================================================================

    private void loadVoitures() {
        try {
            allVoitures.clear();
            allVoitures.addAll(voitureService.afficherh());
            filteredVoitures.setAll(allVoitures);
            totalItems = filteredVoitures.size();
            totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
            showPage(1);
            updateStats();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur ", e);
        }
    }

    private void loadTrajets() {
        try {
            trajetList.clear();
            trajetList.addAll(trajetService.afficherh());
            filteredTrajets.setAll(trajetList);
            totalTrajets     = filteredTrajets.size();
            totalPagesTrajet = (int) Math.ceil((double) totalTrajets / itemsPerPage);
            showPageTrajet(1);
            updateStatsTrajets();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur ", e);
        }
    }

    // ================================================================
    //  RECHERCHE
    // ================================================================

    @FXML
    public void rechercherVoitures() {
        String kw = searchVoituresField.getText().trim().toLowerCase();
        if (kw.isEmpty()) {
            filteredVoitures.setAll(allVoitures);
        } else {
            List<Voiture> result = allVoitures.stream().filter(v ->
                    safe(v.getMarque()).contains(kw) ||
                            safe(v.getModele()).contains(kw) ||
                            safe(v.getImmatriculation()).contains(kw) ||
                            safe(v.getDescription()).contains(kw)
            ).collect(Collectors.toList());
            filteredVoitures.setAll(result);
        }
        totalItems = filteredVoitures.size();
        totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        showPage(1);
    }

    @FXML
    public void rechercherTrajets() {
        String kw = searchTrajetsField.getText().trim().toLowerCase();
        if (kw.isEmpty()) {
            filteredTrajets.setAll(trajetList);
        } else {
            List<Trajet> result = trajetList.stream().filter(t ->
                    safe(t.getPointDepart()).contains(kw) ||
                            safe(t.getPointArrivee()).contains(kw) ||
                            (t.getStatut() != null && t.getStatut().toString().toLowerCase().contains(kw))
            ).collect(Collectors.toList());
            filteredTrajets.setAll(result);
        }
        totalTrajets     = filteredTrajets.size();
        totalPagesTrajet = (int) Math.ceil((double) totalTrajets / itemsPerPage);
        showPageTrajet(1);
    }

    // ================================================================
    //  EXPORT PDF
    // ================================================================

    @FXML
    public void exporterPdfVoitures() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le PDF voitures");
        fc.setInitialFileName("voitures.pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        File file = fc.showSaveDialog(btnPdfVoitures.getScene().getWindow());
        if (file == null) return;

        try {
            Document doc = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            Font fTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.DARK_GRAY);
            Paragraph titre = new Paragraph("Liste des Voitures — RE7LA", fTitre);
            titre.setAlignment(Element.ALIGN_CENTER); titre.setSpacingAfter(18); doc.add(titre);

            Font fSub = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, BaseColor.GRAY);
            Paragraph sub = new Paragraph("Généré le : " + new java.util.Date(), fSub);
            sub.setAlignment(Element.ALIGN_RIGHT); sub.setSpacingAfter(12); doc.add(sub);

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 2f, 2f, 2.5f, 1.5f, 1.2f, 1.5f});

            Font fHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
            for (String h : new String[]{"ID", "Marque", "Modèle", "Immatriculation", "Prix/km", "Places", "Disponible"}) {
                PdfPCell cell = new PdfPCell(new Phrase(h, fHead));
                cell.setBackgroundColor(new BaseColor(37, 99, 235));
                cell.setPadding(8); cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorderColor(BaseColor.WHITE); table.addCell(cell);
            }

            Font fData = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.DARK_GRAY);
            boolean alt = false;
            for (Voiture v : filteredVoitures) {
                BaseColor bg = alt ? new BaseColor(239, 246, 255) : BaseColor.WHITE;
                for (String val : new String[]{
                        String.valueOf(v.getIdVoiture()), nvl(v.getMarque()), nvl(v.getModele()),
                        nvl(v.getImmatriculation()), String.format("%.2f DT", v.getPrixKm()),
                        String.valueOf(v.getNb_places()), v.isDisponibilite() ? "✔ Oui" : "✘ Non"}) {
                    PdfPCell cell = new PdfPCell(new Phrase(val, fData));
                    cell.setBackgroundColor(bg); cell.setPadding(6);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER); table.addCell(cell);
                }
                alt = !alt;
            }
            doc.add(table);

            Font fTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.DARK_GRAY);
            Paragraph total = new Paragraph("Total : " + filteredVoitures.size() + " voiture(s)", fTotal);
            total.setSpacingBefore(10); doc.add(total);
            doc.close();
            showInfo("PDF généré avec succès !\n" + file.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur PDF voitures", e);
            showAlert("Erreur lors de la génération du PDF : " + e.getMessage());
        }
    }

    @FXML
    public void exporterPdfTrajets() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le PDF trajets");
        fc.setInitialFileName("trajets.pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        File file = fc.showSaveDialog(btnPdfTrajets.getScene().getWindow());
        if (file == null) return;

        try {
            Document doc = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            Font fTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.DARK_GRAY);
            Paragraph titre = new Paragraph("Liste des Trajets — RE7LA", fTitre);
            titre.setAlignment(Element.ALIGN_CENTER); titre.setSpacingAfter(18); doc.add(titre);

            Font fSub = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, BaseColor.GRAY);
            Paragraph sub = new Paragraph("Généré le : " + new java.util.Date(), fSub);
            sub.setAlignment(Element.ALIGN_RIGHT); sub.setSpacingAfter(12); doc.add(sub);

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 2.5f, 2.5f, 2f, 2f, 1.5f, 2f});

            Font fHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
            for (String h : new String[]{"ID", "Départ", "Arrivée", "Distance (km)", "Date", "Personnes", "Statut"}) {
                PdfPCell cell = new PdfPCell(new Phrase(h, fHead));
                cell.setBackgroundColor(new BaseColor(37, 99, 235));
                cell.setPadding(8); cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorderColor(BaseColor.WHITE); table.addCell(cell);
            }

            Font fData = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.DARK_GRAY);
            boolean alt = false;
            for (Trajet t : filteredTrajets) {
                BaseColor bg = alt ? new BaseColor(239, 246, 255) : BaseColor.WHITE;
                for (String val : new String[]{
                        String.valueOf(t.getIdTrajet()), nvl(t.getPointDepart()), nvl(t.getPointArrivee()),
                        String.format("%.1f", t.getDistanceKm()),
                        t.getDateReservation() != null ? t.getDateReservation().toString() : "-",
                        String.valueOf(t.getNbPersonnes()),
                        t.getStatut() != null ? t.getStatut().toString() : "-"}) {
                    PdfPCell cell = new PdfPCell(new Phrase(val, fData));
                    cell.setBackgroundColor(bg); cell.setPadding(6);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER); table.addCell(cell);
                }
                alt = !alt;
            }
            doc.add(table);

            Font fTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.DARK_GRAY);
            Paragraph total = new Paragraph("Total : " + filteredTrajets.size() + " trajet(s)", fTotal);
            total.setSpacingBefore(10); doc.add(total);
            doc.close();
            showInfo("PDF généré avec succès !\n" + file.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur PDF trajets", e);
            showAlert("Erreur lors de la génération du PDF : " + e.getMessage());
        }
    }

    // ================================================================
    //  CRUD VOITURE
    // ================================================================

    @FXML
    public void ajouterVoiture() {
        if (validerFormulaire()) return;
        try {
            Voiture v = new Voiture();
            v.setMarque(marqueField.getText());
            v.setModele(modeleField.getText());
            v.setImmatriculation(immatriculationField.getText());
            v.setPrixKm(Float.parseFloat(prixKmField.getText()));
            v.setNb_places(Integer.parseInt(nbPlacesField.getText()));
            v.setImage(imageField.getText());
            v.setDescription(descriptionField.getText());
            v.setDisponibilite(disponibiliteCheck.isSelected());
            voitureService.ajouterh(v);
            loadVoitures();
            clearVoitureForm();
            updateStats();
        } catch (Exception e) { showAlert("Erreur ajout voiture"); }
    }

    @FXML
    public void modifierVoiture() {
        if (validerFormulaire()) return;
        Voiture v = tableVoitures.getSelectionModel().getSelectedItem();
        if (v == null) return;
        try {
            v.setMarque(marqueField.getText());
            v.setModele(modeleField.getText());
            v.setImmatriculation(immatriculationField.getText());
            v.setPrixKm(Float.parseFloat(prixKmField.getText()));
            v.setNb_places(Integer.parseInt(nbPlacesField.getText()));
            v.setImage(imageField.getText());
            v.setDescription(descriptionField.getText());
            v.setDisponibilite(disponibiliteCheck.isSelected());
            voitureService.modifierh(v);
            loadVoitures();
            updateStats();
        } catch (Exception e) { showAlert("Erreur modification voiture"); }
    }

    @FXML
    public void supprimerVoiture() {
        Voiture v = tableVoitures.getSelectionModel().getSelectedItem();
        if (v == null) return;
        try {
            voitureService.supprimerh(v);
            loadVoitures();
            updateStats();
        } catch (Exception e) { showAlert("Erreur suppression voiture"); }
    }

    private void clearVoitureForm() {
        marqueField.clear(); modeleField.clear(); immatriculationField.clear();
        prixKmField.clear(); nbPlacesField.clear(); imageField.clear();
        descriptionField.clear(); disponibiliteCheck.setSelected(false);
    }

    // ================================================================
    //  PAGINATION
    // ================================================================

    private void showPage(int page) {
        if (filteredVoitures.isEmpty()) {
            tableVoitures.setItems(FXCollections.observableArrayList());
            if (paginationLabel != null) paginationLabel.setText("0-0 sur 0 Voitures");
            updatePaginationButtons(); return;
        }
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        currentPage = page;
        int from = Math.min((currentPage - 1) * itemsPerPage, totalItems - 1);
        int to   = Math.min(from + itemsPerPage, totalItems);
        tableVoitures.setItems(FXCollections.observableArrayList(filteredVoitures.subList(from, to)));
        if (paginationLabel != null)
            paginationLabel.setText((from + 1) + "-" + to + " sur " + totalItems + " Voitures");
        updatePaginationButtons();
    }

    private void updatePaginationButtons() {
        if (paginationContainer == null) return;
        paginationContainer.getChildren().clear();
        Button prev = new Button("←"); prev.setOnAction(e -> showPage(currentPage - 1));
        paginationContainer.getChildren().add(prev);
        for (int i = 1; i <= totalPages; i++) {
            Button btn = new Button(String.valueOf(i));
            if (i == currentPage) btn.getStyleClass().add("pagination-button-active");
            int p = i; btn.setOnAction(e -> showPage(p));
            paginationContainer.getChildren().add(btn);
        }
        Button next = new Button("→"); next.setOnAction(e -> showPage(currentPage + 1));
        paginationContainer.getChildren().add(next);
    }

    private void showPageTrajet(int page) {
        if (filteredTrajets.isEmpty()) {
            tableTrajets.setItems(FXCollections.observableArrayList());
            if (paginationTrajetLabel != null) paginationTrajetLabel.setText("0 Trajets");
            updatePaginationTrajetButtons(); return;
        }
        if (page < 1) page = 1;
        if (page > totalPagesTrajet) page = totalPagesTrajet;
        currentPageTrajet = page;
        int from = Math.min((currentPageTrajet - 1) * itemsPerPage, totalTrajets - 1);
        int to   = Math.min(from + itemsPerPage, totalTrajets);
        tableTrajets.setItems(FXCollections.observableArrayList(filteredTrajets.subList(from, to)));
        if (paginationTrajetLabel != null)
            paginationTrajetLabel.setText((from + 1) + "-" + to + " sur " + totalTrajets + " Trajets");
        updatePaginationTrajetButtons();
    }

    private void updatePaginationTrajetButtons() {
        if (paginationTrajetContainer == null) return;
        paginationTrajetContainer.getChildren().clear();
        Button prev = new Button("←"); prev.setOnAction(e -> showPageTrajet(currentPageTrajet - 1));
        paginationTrajetContainer.getChildren().add(prev);
        for (int i = 1; i <= totalPagesTrajet; i++) {
            Button btn = new Button(String.valueOf(i));
            if (i == currentPageTrajet) btn.getStyleClass().add("pagination-button-active");
            int p = i; btn.setOnAction(e -> showPageTrajet(p));
            paginationTrajetContainer.getChildren().add(btn);
        }
        Button next = new Button("→"); next.setOnAction(e -> showPageTrajet(currentPageTrajet + 1));
        paginationTrajetContainer.getChildren().add(next);
    }

    // ================================================================
    //  STATS
    // ================================================================

    @FXML
    private void updateStats() {
        int nb = allVoitures.size(); double prix = 0; int dispo = 0;
        for (Voiture v : allVoitures) { prix += v.getPrixKm(); if (v.isDisponibilite()) dispo++; }
        if (nbVoituresLabel  != null) nbVoituresLabel.setText(String.valueOf(nb));
        if (prixTotalLabel   != null) prixTotalLabel.setText(String.format("%.2f DT", prix));
        if (disponiblesLabel != null) disponiblesLabel.setText(String.valueOf(dispo));
    }

    private void updateStatsTrajets() {
        int total = trajetList.size(), enCours = 0, termine = 0;
        for (Trajet t : trajetList) {
            if (t.getStatut() == StatutVoiture.En_cours)   enCours++;
            if (t.getStatut() == StatutVoiture.Disponible) termine++;
        }
        if (nbTrajetsLabel       != null) nbTrajetsLabel.setText(String.valueOf(total));
        if (trajetsEnCoursLabel  != null) trajetsEnCoursLabel.setText(String.valueOf(enCours));
        if (trajetsTerminesLabel != null) trajetsTerminesLabel.setText(String.valueOf(termine));
    }

    // ================================================================
    //  SÉLECTION VOITURE
    // ================================================================

    private void initSelectionVoiture() {
        tableVoitures.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null) return;
            marqueField.setText(sel.getMarque());
            modeleField.setText(sel.getModele());
            immatriculationField.setText(sel.getImmatriculation());
            prixKmField.setText(String.valueOf(sel.getPrixKm()));
            nbPlacesField.setText(String.valueOf(sel.getNb_places()));
            imageField.setText(sel.getImage());
            descriptionField.setText(sel.getDescription());
            avecChauffeurCheck.setSelected(sel.isAvecChauffeur());
            disponibiliteCheck.setSelected(sel.isDisponibilite());
        });
    }

    // ================================================================
    //  PARCOURIR IMAGE
    // ================================================================

    @FXML
    private void parcourirImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = fc.showOpenDialog((Stage) imageField.getScene().getWindow());
        if (file != null) imageField.setText(file.getAbsolutePath());
    }

    // ================================================================
    //  VALIDATION
    // ================================================================

    private boolean validerFormulaire() {
        String marque          = marqueField.getText().trim();
        String modele          = modeleField.getText().trim();
        String immatriculation = immatriculationField.getText().trim();
        String prixKmText      = prixKmField.getText().trim();
        String nbPlacesText    = nbPlacesField.getText().trim();
        String imagePath       = imageField.getText().trim();

        if (marque.isEmpty() || modele.isEmpty() || immatriculation.isEmpty() ||
                prixKmText.isEmpty() || nbPlacesText.isEmpty() || imagePath.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs obligatoires !"); return true;
        }
        try {
            double prixKm = Double.parseDouble(prixKmText);
            if (prixKm < 0) { showAlert("Erreur", "Le prix par km doit être positif !"); return true; }
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Le champ Prix KM doit être un nombre !"); return true;
        }
        try {
            int nbPlaces = Integer.parseInt(nbPlacesText);
            if (nbPlaces <= 0) { showAlert("Erreur", "Le nombre de places doit être supérieur à 0 !"); return true; }
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Le champ Nombre de places doit être un nombre entier !"); return true;
        }
        File imgFile = new File(imagePath);
        if (!imgFile.exists() || !imgFile.isFile()) {
            showAlert("Erreur", "Veuillez choisir une image valide !"); return true;
        }
        immatriculation = immatriculation.toUpperCase();
        boolean isPlaque = immatriculation.matches("^[A-Z0-9\\- ]{3,15}$");
        boolean isVIN    = immatriculation.matches("^[A-HJ-NPR-Z0-9]{17}$");
        if (!isPlaque && !isVIN) {
            showAlert("Erreur", "Format invalide ! Entrez une plaque ou un VIN valide (17 caractères)."); return true;
        }
        return false;
    }

    // ================================================================
    //  UTILITAIRES
    // ================================================================

    private String safe(String s) { return s == null ? "" : s.toLowerCase(); }
    private String nvl(String s)  { return s == null ? "" : s; }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null); a.setContentText(msg); a.show();
    }

    private void showAlert(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titre); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    // ================================================================
    //  DÉCODER VIN
    // ================================================================

    @FXML
    private void decoderVIN() {
        String vin = immatriculationField.getText().trim();
        if (vin.isEmpty()) { showAlert("Erreur", "Veuillez entrer un numéro VIN !"); return; }

        String url = "https://vpic.nhtsa.dot.gov/api/vehicles/decodevinvaluesextended/" + vin + "?format=json";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());
            JSONArray results = json.getJSONArray("Results");
            if (!results.isEmpty()) {
                JSONObject data = results.getJSONObject(0);
                marqueField.setText(data.optString("Make"));
                modeleField.setText(data.optString("Model"));
            } else {
                showAlert("Erreur", "Aucune information trouvée pour ce VIN !");
            }
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de décoder le VIN : " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Erreur lors de l'opération", e);
        }
    }

    // ================================================================
    //  PDF / STATISTIQUES
    // ================================================================

    @FXML
    private void ouvrirPDF() {
        try {
            File pdfFile = new File("C:\\Users\\Abderrahmen\\Downloads\\VIN_Report.pdf");
            if (pdfFile.exists()) {
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(pdfFile);
                else System.out.println("Desktop n'est pas supporté sur cette machine.");
            } else {
                showAlert("Erreur", "Fichier PDF introuvable !");
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ouverture du PDF", ex);
            showAlert("Erreur", "Impossible d'ouvrir le PDF: " + ex.getMessage());
        }
    }

    @FXML
    private void handleShowCommunaute(ActionEvent event) {
        if (!checkUserAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/communaute/PublicationCommentaire.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            // Ajouter le CSS si nécessaire
            try {
                String css = getClass().getResource("/communaute/Communaute.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("⚠️ CSS communauté non trouvé, chargement sans styles");
            }

            stage.setScene(scene);
            stage.setTitle("Gestion de la Communauté - RE7LA");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger l'interface communauté: " + e.getMessage());
        }
    }

    @FXML
    private void ouvrirStatistiques() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Trajet/statistiques.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1100, 750));
            stage.setTitle("📊 Statistiques RE7LA");
            stage.setMinWidth(900); stage.setMinHeight(600); stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur ouverture statistiques", e);
            showAlert("Erreur", "Impossible d'ouvrir les statistiques : " + e.getMessage());
        }
    }

    @FXML
    public void handleShowActivites(ActionEvent event) {
        if (!checkUserAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/activite/views/backoffice/ActiviteBackOffice.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            // Ajouter le CSS si nécessaire
            try {
                String css = getClass().getResource("/activite/css/Bstyle.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("⚠️ CSS activités non trouvé, chargement sans styles");
            }

            stage.setScene(scene);
            stage.setTitle("Gestion des Activités - RE7LA");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger l'interface activités: " + e.getMessage());
        }
    }
}
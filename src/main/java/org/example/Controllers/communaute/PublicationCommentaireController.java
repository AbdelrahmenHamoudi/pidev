package org.example.Controllers.communaute;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.Entites.communaute.Commentaire;
import org.example.Entites.communaute.Publication;
import org.example.Entites.communaute.StatutP;
import org.example.Entites.communaute.TypeCible;
import org.example.Entites.user.Role;
import org.example.Entites.user.User;
import org.example.Services.communaute.CommentaireCRUD;
import org.example.Services.communaute.PublicationCRUD;
import org.example.Utils.UserSession;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PublicationCommentaireController {

    // ===============================
    // LAYOUT
    // ===============================
    @FXML private StackPane mainContentStack;
    @FXML private ScrollPane viewHebergement;
    @FXML private ScrollPane viewCommunaute;
    @FXML private VBox viewEmpty;

    // ===============================
    // STATS + CHART
    // ===============================
    @FXML private Label lblTotalReactions;
    @FXML private Label lblTotalPublications;
    @FXML private StackPane chartContainer;
    @FXML private Button btnExportPDF;

    // ===============================
    // TABLEVIEW — PUBLICATION
    // ===============================
    @FXML private TableView<Publication> tableCommunaute;
    @FXML private TableColumn<Publication, String> colImg;
    @FXML private TableColumn<Publication, Integer> colId;
    @FXML private TableColumn<Publication, TypeCible> colTypeCible;
    @FXML private TableColumn<Publication, String> colDateC;
    @FXML private TableColumn<Publication, String> colDateM;
    @FXML private TableColumn<Publication, StatutP> colStatut;
    @FXML private TableColumn<Publication, Boolean> colVerifie;
    @FXML private TableColumn<Publication, String> colDesc;
    @FXML private TableColumn<Publication, String> colUser;
    @FXML private TableColumn<Publication, Integer> colCommentaire;

    // ===============================
    // SEARCH + SORT — PUBLICATION
    // ===============================
    @FXML private TextField tfSearchPublication;
    @FXML private ComboBox<String> cbSortPublication;

    // ===============================
    // FORM FIELDS — PUBLICATION
    // ===============================
    @FXML private TextField tfImgURL;
    @FXML private Label lblSelectedImage;
    @FXML private ImageView imgPreview;
    @FXML private Label lblImagePlaceholder;
    @FXML private ComboBox<TypeCible> cbTypeCible;
    @FXML private ComboBox<StatutP> cbStatut;
    @FXML private CheckBox cbVerifie;
    @FXML private TextArea tfDescription;

    // ===============================
    // TABLEVIEW — COMMENTAIRE
    // ===============================
    @FXML private TableView<Commentaire> tableCommentaires;
    @FXML private TableColumn<Commentaire, Integer> colIdCommentaire;
    @FXML private TableColumn<Commentaire, String> colContenu;
    @FXML private TableColumn<Commentaire, String> colDateCreationC;
    @FXML private TableColumn<Commentaire, Boolean> colStatutC;
    @FXML private TableColumn<Commentaire, String> colUserC;

    // ===============================
    // SEARCH + SORT — COMMENTAIRE
    // ===============================
    @FXML private TextField tfSearchCommentaire;
    @FXML private ComboBox<String> cbSortCommentaire;

    // ===============================
    // FORM FIELDS — COMMENTAIRE
    // ===============================
    @FXML private TextField tfContenuCommentaire;

    // ===============================
    // BOUTONS DE FILTRAGE
    // ===============================
    @FXML private Button btnMesPublications;
    @FXML private Button btnToutesPublications;

    // ===============================
    // SERVICES
    // ===============================
    private final PublicationCRUD publicationService = new PublicationCRUD();
    private final CommentaireCRUD commentaireService = new CommentaireCRUD();

    // ===============================
    // BACKING LISTS
    // ===============================
    private ObservableList<Publication> allPublications = FXCollections.observableArrayList();
    private FilteredList<Publication> filteredPublications;
    private ObservableList<Commentaire> allCommentaires = FXCollections.observableArrayList();
    private FilteredList<Commentaire> filteredCommentaires;

    // ===============================
    // UTILISATEUR CONNECTÉ (JWT)
    // ===============================
    private User currentUser;
    private boolean isAdmin = false;

    // ===============================
    // BAD WORDS FILTER
    // ===============================
    private static final List<String> BAD_WORDS = List.of("kalb", "couchon", "bhim", "merde", "putain");
    private static final String SORT_AZ = "A → Z";
    private static final String SORT_ZA = "Z → A";
    private static final String SORT_DATE_C = "Date Création ↑";
    private static final String SORT_DATE_M = "Date Modification ↑";
    private static final String SORT_SHUFFLE = "🔀 Aléatoire";

    // ===============================
    // INITIALIZE
    // ===============================
    @FXML
    void initialize() {
        // ✅ Vérifier l'authentification JWT
        if (!checkUserAuth()) {
            return;
        }

        // Afficher les infos de l'utilisateur connecté
        System.out.println("✅ Utilisateur connecté: " + currentUser.getPrenom() + " " + currentUser.getNom());
        System.out.println("✅ Rôle (enum): " + currentUser.getRole());
        System.out.println("✅ Token valide: " + UserSession.getInstance().isTokenValid());

        // ✅ Vérifier si l'utilisateur est admin (gestion d'enum)
        isAdmin = false;
        if (currentUser.getRole() != null) {
            // Comparaison directe avec l'enum
            isAdmin = currentUser.getRole() == Role.admin;
            System.out.println("🔍 Rôle exact: " + currentUser.getRole());
        } else {
            System.out.println("⚠️ Rôle est NULL");
        }
        System.out.println("✅ Accès admin: " + isAdmin);

        // Initialiser les ComboBox
        cbTypeCible.setItems(FXCollections.observableArrayList(TypeCible.values()));
        cbStatut.setItems(FXCollections.observableArrayList(StatutP.values()));

        setupTableColumns();
        setupSortOptions();
        setupListeners();

        loadPublications();
        updateStatsAndChart();
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
     * ✅ Redirige vers la page de login
     */
    private void redirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) (tableCommunaute != null ?
                    tableCommunaute.getScene().getWindow() :
                    btnExportPDF.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setTitle("RE7LA Tunisie - Connexion");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupTableColumns() {
        // ── Publication columns ──────────────────────────────────
        colId.setCellValueFactory(new PropertyValueFactory<>("idPublication"));
        colTypeCible.setCellValueFactory(new PropertyValueFactory<>("typeCible"));
        colDateC.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        colDateM.setCellValueFactory(new PropertyValueFactory<>("dateModif"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statutP"));
        colVerifie.setCellValueFactory(new PropertyValueFactory<>("estVerifie"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("DescriptionP"));

        // ── Colonne Utilisateur ─────────────────────────────────
        colUser.setCellValueFactory(cellData -> {
            User user = cellData.getValue().getId_utilisateur();
            if (user != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        user.getPrenom() + " " + user.getNom()
                );
            }
            return new javafx.beans.property.SimpleStringProperty("Inconnu");
        });

        // ── Colonne nombre de commentaires ───────────────────────
        if (colCommentaire != null) {
            colCommentaire.setCellValueFactory(cellData -> {
                try {
                    int idPub = cellData.getValue().getIdPublication();
                    int count = commentaireService.getCommentsByPublicationId(idPub).size();
                    return new javafx.beans.property.SimpleIntegerProperty(count).asObject();
                } catch (Exception e) {
                    return new javafx.beans.property.SimpleIntegerProperty(0).asObject();
                }
            });
        }

        // ── Image column — render thumbnail instead of text ──────
        colImg.setCellValueFactory(new PropertyValueFactory<>("ImgURL"));
        colImg.setCellFactory(col -> new TableCell<>() {
            private final ImageView iv = new ImageView();

            {
                iv.setFitWidth(60);
                iv.setFitHeight(45);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                iv.setStyle("-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.15),4,0,0,1);");
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String imgUrl, boolean empty) {
                super.updateItem(imgUrl, empty);
                if (empty || imgUrl == null || imgUrl.isBlank()) {
                    setGraphic(null);
                } else {
                    Image img = loadImage(imgUrl);
                    iv.setImage(img);
                    setGraphic(img != null ? iv : new Label("🖼"));
                }
            }
        });

        // Make rows taller so thumbnails are visible
        tableCommunaute.setRowFactory(tv -> {
            TableRow<Publication> row = new TableRow<>();
            row.setPrefHeight(60);
            return row;
        });

        // ── Commentaire columns ──────────────────────────────────
        colIdCommentaire.setCellValueFactory(new PropertyValueFactory<>("idCommentaire"));
        colContenu.setCellValueFactory(new PropertyValueFactory<>("contenuC"));
        colDateCreationC.setCellValueFactory(new PropertyValueFactory<>("dateCreationC"));
        colStatutC.setCellValueFactory(new PropertyValueFactory<>("statutC"));

        // Colonne utilisateur pour commentaire
        colUserC.setCellValueFactory(cellData -> {
            User user = cellData.getValue().getId_utilisateur();
            if (user != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        user.getPrenom() + " " + user.getNom()
                );
            }
            return new javafx.beans.property.SimpleStringProperty("Inconnu");
        });
    }

    private void setupSortOptions() {
        ObservableList<String> sortOptions = FXCollections.observableArrayList(
                SORT_AZ, SORT_ZA, SORT_DATE_C, SORT_DATE_M, SORT_SHUFFLE);
        cbSortPublication.setItems(sortOptions);
        cbSortCommentaire.setItems(sortOptions);
        cbSortPublication.setOnAction(e -> applySortPublications());
        cbSortCommentaire.setOnAction(e -> applySortCommentaires());
    }

    private void setupListeners() {
        // ── Row selection → fill form ─────────────────────────────
        tableCommunaute.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, sel) -> {
                    if (sel != null) {
                        tfImgURL.setText(sel.getImgURL());
                        lblSelectedImage.setText(sel.getImgURL() != null ? sel.getImgURL() : "Aucune image");
                        updateFormImagePreview(sel.getImgURL());
                        cbTypeCible.setValue(sel.getTypeCible());
                        cbStatut.setValue(sel.getStatutP());
                        cbVerifie.setSelected(sel.isEstVerifie());
                        tfDescription.setText(sel.getDescriptionP());
                        loadCommentaires(sel);
                    }
                });

        tableCommentaires.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, sel) -> {
                    if (sel != null) {
                        tfContenuCommentaire.setText(sel.getContenuC());
                    }
                });

        // ── Boutons de filtrage ───────────────────────────────────
        if (btnMesPublications != null) {
            btnMesPublications.setOnAction(e -> handleShowMyPublications());
        }
        if (btnToutesPublications != null) {
            btnToutesPublications.setOnAction(e -> handleShowAllPublications());
        }
    }

    // ===============================
    // FILE PICKER — called from FXML
    // ===============================
    @FXML
    private void handleChooseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.svg", "*.gif", "*.webp"));

        Stage stage = (Stage) tfDescription.getScene().getWindow();
        File file = fc.showOpenDialog(stage);

        if (file != null) {
            // Copy to resources/img so the app can load it by name
            String fileName = file.getName();
            try {
                Path dest = Paths.get("src/main/resources/img/" + fileName);
                Files.createDirectories(dest.getParent());
                Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                // If copy fails, still store the full path as fallback
                System.err.println("⚠️ Could not copy image to resources: " + ex.getMessage());
            }

            tfImgURL.setText(fileName);
            lblSelectedImage.setText(fileName);

            // Show preview in form
            try {
                Image img = new Image(file.toURI().toString(), 80, 60, true, true);
                imgPreview.setImage(img);
                imgPreview.setVisible(true);
                lblImagePlaceholder.setVisible(false);
            } catch (Exception ex) {
                imgPreview.setVisible(false);
                lblImagePlaceholder.setVisible(true);
            }
        }
    }

    // ── Update the form preview when a row is selected ────────────
    private void updateFormImagePreview(String imgUrl) {
        if (imgUrl == null || imgUrl.isBlank()) {
            imgPreview.setVisible(false);
            lblImagePlaceholder.setVisible(true);
            return;
        }
        Image img = loadImage(imgUrl);
        if (img != null) {
            imgPreview.setImage(img);
            imgPreview.setVisible(true);
            lblImagePlaceholder.setVisible(false);
        } else {
            imgPreview.setVisible(false);
            lblImagePlaceholder.setVisible(true);
        }
    }

    // ── Central image loader (tries classpath then file system) ───
    private Image loadImage(String imgUrl) {
        if (imgUrl == null || imgUrl.isBlank()) return null;
        try {
            // 1. Try classpath /img/
            InputStream is = getClass().getResourceAsStream("/img/" + imgUrl);
            if (is != null) return new Image(is, 60, 45, true, true);
            // 2. Try as absolute/relative path
            File f = new File(imgUrl);
            if (f.exists()) return new Image(f.toURI().toString(), 60, 45, true, true);
            // 3. Try src/main/resources/img/
            File f2 = new File("src/main/resources/img/" + imgUrl);
            if (f2.exists()) return new Image(f2.toURI().toString(), 60, 45, true, true);
        } catch (Exception ignored) {
        }
        return null;
    }

    // ===============================
    // LOAD DATA
    // ===============================
    private void loadPublications() {
        try {
            allPublications = FXCollections.observableArrayList(publicationService.afficherh());
            setupPublicationSearch();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les publications: " + e.getMessage());
        }
    }

    private void loadCommentaires(Publication pub) {
        try {
            allCommentaires = FXCollections.observableArrayList(
                    commentaireService.afficherParPublication(pub.getIdPublication()));
            setupCommentaireSearch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===============================
    // SEARCH — PUBLICATION
    // ===============================
    private void setupPublicationSearch() {
        filteredPublications = new FilteredList<>(allPublications, p -> true);
        tfSearchPublication.textProperty().addListener((obs, o, newVal) ->
                filteredPublications.setPredicate(pub -> {
                    if (newVal == null || newVal.trim().isEmpty()) return true;
                    return matchesPublication(pub, newVal.toLowerCase().trim());
                }));
        tableCommunaute.setItems(new SortedList<>(filteredPublications));
    }

    private boolean matchesPublication(Publication p, String t) {
        if (p.getImgURL() != null && p.getImgURL().toLowerCase().contains(t)) return true;
        if (p.getDescriptionP() != null && p.getDescriptionP().toLowerCase().contains(t)) return true;
        if (p.getTypeCible() != null && p.getTypeCible().name().toLowerCase().contains(t)) return true;
        if (p.getStatutP() != null && p.getStatutP().name().toLowerCase().contains(t)) return true;
        if (p.getDateCreation() != null && p.getDateCreation().contains(t)) return true;
        if (p.getDateModif() != null && p.getDateModif().contains(t)) return true;
        if (String.valueOf(p.getIdPublication()).contains(t)) return true;
        if (p.getId_utilisateur() != null) {
            String u = (p.getId_utilisateur().getNom() + " " + p.getId_utilisateur().getPrenom()).toLowerCase();
            if (u.contains(t)) return true;
        }
        return String.valueOf(p.isEstVerifie()).contains(t);
    }

    // ===============================
    // SEARCH — COMMENTAIRE
    // ===============================
    private void setupCommentaireSearch() {
        filteredCommentaires = new FilteredList<>(allCommentaires, c -> true);
        tfSearchCommentaire.textProperty().addListener((obs, o, newVal) ->
                filteredCommentaires.setPredicate(c -> {
                    if (newVal == null || newVal.trim().isEmpty()) return true;
                    return matchesCommentaire(c, newVal.toLowerCase().trim());
                }));
        tableCommentaires.setItems(new SortedList<>(filteredCommentaires));
    }

    private boolean matchesCommentaire(Commentaire c, String t) {
        if (c.getContenuC() != null && c.getContenuC().toLowerCase().contains(t)) return true;
        if (c.getDateCreationC() != null && c.getDateCreationC().contains(t)) return true;
        if (String.valueOf(c.getIdCommentaire()).contains(t)) return true;
        if (String.valueOf(c.isStatutC()).contains(t)) return true;
        if (c.getId_utilisateur() != null) {
            String u = (c.getId_utilisateur().getNom() + " " + c.getId_utilisateur().getPrenom()).toLowerCase();
            if (u.contains(t)) return true;
        }
        return false;
    }

    // ===============================
    // SORT — PUBLICATION
    // ===============================
    @FXML
    private void applySortPublications() {
        if (allPublications == null) return;
        String sel = cbSortPublication.getValue();
        if (sel == null) return;
        List<Publication> list = new ArrayList<>(allPublications);
        switch (sel) {
            case SORT_AZ:
                list.sort(Comparator.comparing(p -> p.getDescriptionP() != null ? p.getDescriptionP().toLowerCase() : ""));
                break;
            case SORT_ZA:
                list.sort(Comparator.comparing((Publication p) -> p.getDescriptionP() != null ? p.getDescriptionP().toLowerCase() : "").reversed());
                break;
            case SORT_DATE_C:
                list.sort(Comparator.comparing(p -> p.getDateCreation() != null ? p.getDateCreation() : ""));
                break;
            case SORT_DATE_M:
                list.sort(Comparator.comparing(p -> p.getDateModif() != null ? p.getDateModif() : ""));
                break;
            case SORT_SHUFFLE:
                Collections.shuffle(list);
                break;
        }
        String cur = tfSearchPublication.getText();
        allPublications = FXCollections.observableArrayList(list);
        setupPublicationSearch();
        tfSearchPublication.setText(cur);
    }

    // ===============================
    // SORT — COMMENTAIRE
    // ===============================
    @FXML
    private void applySortCommentaires() {
        if (allCommentaires == null) return;
        String sel = cbSortCommentaire.getValue();
        if (sel == null) return;
        List<Commentaire> list = new ArrayList<>(allCommentaires);
        switch (sel) {
            case SORT_AZ:
                list.sort(Comparator.comparing(c -> c.getContenuC() != null ? c.getContenuC().toLowerCase() : ""));
                break;
            case SORT_ZA:
                list.sort(Comparator.comparing((Commentaire c) -> c.getContenuC() != null ? c.getContenuC().toLowerCase() : "").reversed());
                break;
            case SORT_DATE_C:
            case SORT_DATE_M:
                list.sort(Comparator.comparing(c -> c.getDateCreationC() != null ? c.getDateCreationC() : ""));
                break;
            case SORT_SHUFFLE:
                Collections.shuffle(list);
                break;
        }
        String cur = tfSearchCommentaire.getText();
        allCommentaires = FXCollections.observableArrayList(list);
        setupCommentaireSearch();
        tfSearchCommentaire.setText(cur);
    }

    // ===============================
    // FILTRES PAR UTILISATEUR
    // ===============================
    @FXML
    private void handleShowMyPublications() {
        try {
            filteredPublications.setPredicate(pub -> {
                return pub.getId_utilisateur() != null &&
                        pub.getId_utilisateur().getId() == currentUser.getId();
            });

            showInfo("Filtre appliqué", "Affichage de vos publications uniquement");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowAllPublications() {
        filteredPublications.setPredicate(pub -> true);
        tfSearchPublication.clear();
        showInfo("Filtre retiré", "Affichage de toutes les publications");
    }

    // ===============================
    // FRONT OFFICE REDIRECT
    // ===============================
    @FXML
    private void goToFrontOffice(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/communaute/Communaute.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("FrontOffice - Communauté");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le front office: " + e.getMessage());
        }
    }

    // ===============================
    // SHOW COMMUNITY VIEW
    // ===============================
    @FXML
    private void handleShowCommunaute() {
        viewEmpty.setVisible(false);
        viewCommunaute.setVisible(true);
        loadPublications();
        updateStatsAndChart();
    }

    // ===============================
    // ADD PUBLICATION
    // ===============================
    @FXML
    private void handleAjouter() {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String now = LocalDateTime.now().format(fmt);

            String imgURL = tfImgURL.getText().trim();
            if (imgURL.isEmpty() || !(imgURL.endsWith(".jpeg") || imgURL.endsWith(".jpg")
                    || imgURL.endsWith(".png") || imgURL.endsWith(".svg") || imgURL.endsWith(".gif") || imgURL.endsWith(".webp"))) {
                showAlert("Erreur", "Veuillez choisir une image valide (.jpeg, .png, .svg…).");
                return;
            }
            if (cbTypeCible.getValue() == null) {
                showAlert("Erreur", "Veuillez sélectionner un TypeCible.");
                return;
            }
            if (cbStatut.getValue() == null) {
                showAlert("Erreur", "Veuillez sélectionner un Statut.");
                return;
            }

            String desc = tfDescription.getText().trim();
            if (desc.length() > 1000) {
                showAlert("Erreur", "La description ne peut pas dépasser 1000 caractères.");
                return;
            }
            if (containsBadWord(desc)) {
                showBadWordAlert();
                return;
            }

            // ✅ Utiliser l'utilisateur connecté
            Publication p = new Publication();
            p.setId_utilisateur(currentUser);
            p.setImgURL(imgURL);
            p.setTypeCible(cbTypeCible.getValue());
            p.setStatutP(cbStatut.getValue());
            p.setEstVerifie(cbVerifie.isSelected());
            p.setDescriptionP(desc);
            p.setDateCreation(now);
            p.setDateModif(now);

            publicationService.ajouterh(p);

            showInfo("Succès", "Publication créée par " + currentUser.getPrenom() + " " + currentUser.getNom());

            loadPublications();
            clearForm();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de créer la publication: " + e.getMessage());
        }
    }

    // ===============================
    // UPDATE PUBLICATION
    // ===============================
    @FXML
    private void handleModifier() {
        try {
            Publication sel = tableCommunaute.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("Erreur", "Sélectionnez une publication à modifier");
                return;
            }

            // ✅ Vérifier les droits (propriétaire ou admin)
            if (!canModifyPublication(sel)) {
                showAlert("Accès refusé", "Vous ne pouvez modifier que vos propres publications");
                return;
            }

            if (containsBadWord(tfDescription.getText())) {
                showBadWordAlert();
                return;
            }

            sel.setImgURL(tfImgURL.getText());
            sel.setTypeCible(cbTypeCible.getValue());
            sel.setStatutP(cbStatut.getValue());
            sel.setEstVerifie(cbVerifie.isSelected());
            sel.setDescriptionP(tfDescription.getText());
            sel.setDateModif(LocalDateTime.now().toString());

            publicationService.modifierh(sel);

            showInfo("Succès", "Publication modifiée avec succès");

            loadPublications();
            clearForm();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de modifier: " + e.getMessage());
        }
    }

    // ===============================
    // DELETE PUBLICATION
    // ===============================
    @FXML
    private void handleDelete() {
        try {
            Publication sel = tableCommunaute.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("Aucune sélection", "Veuillez sélectionner une publication à supprimer !");
                return;
            }

            // ✅ Vérifier les droits (propriétaire ou admin)
            if (!canModifyPublication(sel)) {
                showAlert("Accès refusé", "Vous ne pouvez supprimer que vos propres publications");
                return;
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Voulez-vous vraiment supprimer cette publication ?",
                    ButtonType.YES, ButtonType.NO);
            alert.showAndWait().ifPresent(t -> {
                if (t == ButtonType.YES) {
                    try {
                        publicationService.supprimerh(sel);
                        showInfo("Succès", "Publication supprimée");
                        loadPublications();
                        clearForm();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showAlert("Erreur", "Impossible de supprimer: " + ex.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Vérifie si l'utilisateur peut modifier/supprimer une publication
     */
    private boolean canModifyPublication(Publication pub) {
        return isAdmin || (pub.getId_utilisateur() != null &&
                pub.getId_utilisateur().getId() == currentUser.getId());
    }

    /**
     * Vérifie si l'utilisateur peut modifier/supprimer un commentaire
     */
    private boolean canModifyCommentaire(Commentaire com) {
        return isAdmin || (com.getId_utilisateur() != null &&
                com.getId_utilisateur().getId() == currentUser.getId());
    }

    // ===============================
    // ADD COMMENTAIRE
    // ===============================
    @FXML
    private void handleAjouterCommentaire() {
        try {
            Publication pub = tableCommunaute.getSelectionModel().getSelectedItem();
            if (pub == null) {
                showAlert("Erreur", "Veuillez sélectionner une publication pour commenter !");
                return;
            }
            String contenu = tfContenuCommentaire.getText().trim();
            if (contenu.isEmpty()) {
                showAlert("Erreur", "Le commentaire ne peut pas être vide !");
                return;
            }
            if (contenu.length() > 5000) {
                showAlert("Erreur", "Le commentaire est trop long !");
                return;
            }
            if (containsBadWord(contenu)) {
                showBadWordAlert();
                return;
            }

            // ✅ Utiliser l'utilisateur connecté
            Commentaire c = new Commentaire();
            c.setId_utilisateur(currentUser);
            c.setPublication(pub);
            c.setContenuC(contenu);
            c.setDateCreationC(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            c.setStatutC(true);

            commentaireService.ajouterh(c);

            showInfo("Succès", "Commentaire ajouté par " + currentUser.getPrenom());

            loadCommentaires(pub);
            tfContenuCommentaire.clear();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ajouter le commentaire: " + e.getMessage());
        }
    }

    // ===============================
    // UPDATE COMMENTAIRE
    // ===============================
    @FXML
    private void handleModifierCommentaire() {
        try {
            Commentaire sel = tableCommentaires.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("Erreur", "Veuillez sélectionner un commentaire à modifier !");
                return;
            }

            // ✅ Vérifier les droits
            if (!canModifyCommentaire(sel)) {
                showAlert("Accès refusé", "Vous ne pouvez modifier que vos propres commentaires");
                return;
            }

            String contenu = tfContenuCommentaire.getText().trim();
            if (contenu.isEmpty()) {
                showAlert("Erreur", "Le commentaire ne peut pas être vide !");
                return;
            }
            if (contenu.length() > 5000) {
                showAlert("Erreur", "Le commentaire est trop long !");
                return;
            }
            if (containsBadWord(contenu)) {
                showBadWordAlert();
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Voulez-vous vraiment modifier ce commentaire ?",
                    ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.YES) {
                sel.setContenuC(contenu);
                commentaireService.modifierh(sel);
                showInfo("Succès", "Commentaire modifié");
                loadCommentaires(sel.getPublication());
                tfContenuCommentaire.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur inattendue !");
        }
    }

    // ===============================
    // DELETE COMMENTAIRE
    // ===============================
    @FXML
    private void handleSupprimerCommentaire() {
        try {
            Commentaire sel = tableCommentaires.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("Erreur", "Veuillez sélectionner un commentaire à supprimer !");
                return;
            }

            // ✅ Vérifier les droits
            if (!canModifyCommentaire(sel)) {
                showAlert("Accès refusé", "Vous ne pouvez supprimer que vos propres commentaires");
                return;
            }

            new Alert(Alert.AlertType.CONFIRMATION,
                    "Voulez-vous vraiment supprimer ce commentaire ?",
                    ButtonType.YES, ButtonType.NO)
                    .showAndWait().ifPresent(t -> {
                        if (t == ButtonType.YES) {
                            try {
                                commentaireService.supprimerh(sel);
                                showInfo("Succès", "Commentaire supprimé");
                                loadCommentaires(sel.getPublication());
                                tfContenuCommentaire.clear();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                showAlert("Erreur", "Impossible de supprimer: " + ex.getMessage());
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===============================
    // STATS + CHART
    // ===============================
    private void updateStatsAndChart() {
        int totalPubs = allPublications.size();
        int totalReactions = 0;
        int totalComments = 0;

        for (Publication p : allPublications) {
            try {
                totalComments += commentaireService.getCommentsByPublicationId(p.getIdPublication()).size();
            } catch (Exception ignored) {
            }
        }

        if (lblTotalPublications != null) lblTotalPublications.setText(String.valueOf(totalPubs));
        if (lblTotalReactions != null) lblTotalReactions.setText(String.valueOf(totalReactions));
        if (chartContainer != null) buildChart(totalPubs, totalComments, totalReactions);
    }

    private void buildChart(int pubs, int comments, int reactions) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setStyle("-fx-tick-label-fill:#64748B; -fx-font-size:9px;");
        yAxis.setStyle("-fx-tick-label-fill:#64748B; -fx-font-size:9px;");
        yAxis.setAnimated(false);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.setBarGap(4);
        chart.setCategoryGap(16);
        chart.setPrefHeight(90);
        chart.setStyle(
                "-fx-background-color:transparent;" +
                        "-fx-plot-background-color:transparent;" +
                        "-fx-padding:0;" +
                        "-fx-horizontal-grid-lines-visible:false;"
        );

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Publications", pubs));
        series.getData().add(new XYChart.Data<>("Commentaires", comments));
        series.getData().add(new XYChart.Data<>("Réactions", reactions));
        chart.getData().add(series);

        chartContainer.getChildren().setAll(chart);
    }

    // ===============================
    // BAD WORD HELPERS
    // ===============================
    private boolean containsBadWord(String... texts) {
        for (String text : texts) {
            if (text == null) continue;
            String lower = text.toLowerCase();
            for (String bad : BAD_WORDS) {
                if (lower.contains(bad.toLowerCase())) return true;
            }
        }
        return false;
    }

    private void showBadWordAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Contenu inapproprié");
        alert.setHeaderText("Mot interdit détecté !");
        alert.setContentText("Votre texte contient un mot interdit.\nVeuillez modifier votre contenu avant de continuer.");

        // Load stop sign image
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

    // ===============================
    // UTILITAIRES
    // ===============================
    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void clearForm() {
        tfImgURL.clear();
        lblSelectedImage.setText("Aucune image sélectionnée");
        imgPreview.setImage(null);
        imgPreview.setVisible(false);
        lblImagePlaceholder.setVisible(true);
        cbTypeCible.setValue(null);
        cbStatut.setValue(null);
        cbVerifie.setSelected(false);
        tfDescription.clear();
    }

    @FXML
    private void handleExportPDF(ActionEvent event) {
        showInfo("Export PDF", "Fonctionnalité d'export PDF à implémenter");
    }
    @FXML
    void handleShowUsers(ActionEvent event) {
        if (!checkUserAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/back/users.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            // Ajouter le CSS
            try {
                String css = getClass().getResource("/user/back/users.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("⚠️ CSS utilisateurs non trouvé, chargement sans styles");
            }

            stage.setScene(scene);
            stage.setTitle("Gestion des Utilisateurs - RE7LA");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger l'interface utilisateurs: " + e.getMessage());
        }
    }



    @FXML
    void handleShowAccounts(ActionEvent event) {
        if (!checkUserAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/hebergement/back/HebergementBack.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            // Ajouter le CSS
            try {
                String css = getClass().getResource("/hebergement/back/StyleHB.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("⚠️ CSS hébergement non trouvé, chargement sans styles");
            }

            stage.setScene(scene);
            stage.setTitle("Gestion des Hébergements - RE7LA");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger l'interface hébergement: " + e.getMessage());
        }
    }

    @FXML
    void handleShowTransactions(ActionEvent event) {
        if (!checkUserAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/activite/views/backoffice/ActiviteBackOffice.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            // Ajouter le CSS
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
            showAlert("Erreur", "Impossible de charger l'interface Activités: " + e.getMessage());
        }
    }

    @FXML
    void handleShowCredits(ActionEvent event) {
        if (!checkUserAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Trajet/gestionvoitureettrajet.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            // Ajouter le CSS
            try {
                String css = getClass().getResource("/Trajet/style.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("⚠️ CSS trajets non trouvé, chargement sans styles");
            }

            stage.setScene(scene);
            stage.setTitle("Gestion des Voitures et Trajets - RE7LA");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger l'interface Trajet: " + e.getMessage());
        }
    }

    @FXML
    void handleShowCashback(ActionEvent event) {
        if (!checkUserAuth()) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/promotion/back/PromotionBack.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            // Ajouter le CSS
            try {
                String css = getClass().getResource("/promotion/back/promotion.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("⚠️ CSS promotions non trouvé, chargement sans styles");
            }

            stage.setScene(scene);
            stage.setTitle("Gestion des Promotions - RE7LA");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger l'interface promotions: " + e.getMessage());
        }
    }



    @FXML
    void handlelogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Déconnexion");
        alert.setHeaderText("Voulez-vous vraiment vous déconnecter ?");
        alert.setContentText("Vous serez redirigé vers la page de connexion.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Effacer la session
                UserSession.getInstance().clearSession();

                // Rediriger vers la page de login
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/login/login.fxml"));
                    Parent root = loader.load();

                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    Scene scene = new Scene(root);

                    // Ajouter le CSS du login si nécessaire
                    try {
                        String css = getClass().getResource("/user/login/login.css").toExternalForm();
                        scene.getStylesheets().add(css);
                    } catch (Exception e) {
                        System.out.println("⚠️ CSS login non trouvé");
                    }

                    stage.setScene(scene);
                    stage.setTitle("RE7LA Tunisie - Connexion");
                    stage.centerOnScreen();
                    stage.show();

                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert("Erreur", "Impossible de retourner à login: " + e.getMessage());
                }
            }
        });
    }
}
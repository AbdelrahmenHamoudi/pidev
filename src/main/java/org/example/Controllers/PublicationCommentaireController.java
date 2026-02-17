package org.example.Controllers;

import org.example.Entites.*;
import org.example.Services.CommentaireCRUD;
import org.example.Services.PublicationCRUD;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class PublicationCommentaireController {

    // ===============================
    // EXISTING LAYOUT
    // ===============================

    @FXML private StackPane mainContentStack;
    @FXML private ScrollPane viewHebergement;
    @FXML private ScrollPane viewCommunaute;
    @FXML private VBox viewEmpty;

    // ===============================
    // TABLEVIEW
    // ===============================

    @FXML private TableView<Publication> tableCommunaute;

    @FXML private TableColumn<Publication, Integer> colId;
    @FXML private TableColumn<Publication, String> colImg;
    @FXML private TableColumn<Publication, TypeCible> colTypeCible;
    @FXML private TableColumn<Publication, String> colDateC;
    @FXML private TableColumn<Publication, String> colDateM;
    @FXML private TableColumn<Publication, StatutP> colStatut;
    @FXML private TableColumn<Publication, Boolean> colVerifie;
    @FXML private TableColumn<Publication, String> colDesc;

    // ===============================
    // FORM FIELDS
    // ===============================

    @FXML private TextField tfImgURL;
    @FXML private ComboBox<TypeCible> cbTypeCible;
    @FXML private ComboBox<StatutP> cbStatut;
    @FXML private CheckBox cbVerifie;
    @FXML private TextArea tfDescription;
    // ===============================
    // TABLEVIEW – COMMENTAIRE
    // ===============================
    @FXML private TableView<Commentaire> tableCommentaires;
    @FXML private TableColumn<Commentaire, Integer> colIdCommentaire;
    @FXML private TableColumn<Commentaire, String> colContenu;
    @FXML private TableColumn<Commentaire, String> colDateCreationC;
    @FXML private TableColumn<Commentaire, Boolean> colStatutC;
    @FXML private TableColumn<Commentaire, String> colUserC;

    // ===============================
    // FORM FIELDS – COMMENTAIRE
    // ===============================
    @FXML private TextField tfContenuCommentaire;

    // ===============================
    // SERVICE
    // ===============================

    private PublicationCRUD publicationService = new PublicationCRUD();
    private CommentaireCRUD commentaireService = new CommentaireCRUD();

    // ===============================
    // INITIALIZE
    // ===============================

    @FXML
    void initialize() {

        // Fill Enum ComboBoxes automatically
        cbTypeCible.setItems(FXCollections.observableArrayList(TypeCible.values()));
        cbStatut.setItems(FXCollections.observableArrayList(StatutP.values()));

        // Configure Table Columns
        colId.setCellValueFactory(new PropertyValueFactory<>("idPublication"));
        colImg.setCellValueFactory(new PropertyValueFactory<>("ImgURL"));
        colTypeCible.setCellValueFactory(new PropertyValueFactory<>("typeCible"));
        colDateC.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        colDateM.setCellValueFactory(new PropertyValueFactory<>("dateModif"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statutP"));
        colVerifie.setCellValueFactory(new PropertyValueFactory<>("estVerifie"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("DescriptionP"));

        // Auto-fill form when selecting row
        tableCommunaute.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                tfImgURL.setText(selected.getImgURL());
                cbTypeCible.setValue(selected.getTypeCible());
                cbStatut.setValue(selected.getStatutP());
                cbVerifie.setSelected(selected.isEstVerifie());
                tfDescription.setText(selected.getDescriptionP());

                // Load comments for this publication
                loadCommentaires(selected);
            }
        });
        // Auto-fill comment form when selecting a comment
        tableCommentaires.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                tfContenuCommentaire.setText(selected.getContenuC());
            }
        });


        loadPublications();
        // Configure Commentaire Table Columns
        colIdCommentaire.setCellValueFactory(new PropertyValueFactory<>("idCommentaire"));
        colContenu.setCellValueFactory(new PropertyValueFactory<>("contenuC"));
        colDateCreationC.setCellValueFactory(new PropertyValueFactory<>("dateCreationC"));
        colStatutC.setCellValueFactory(new PropertyValueFactory<>("statutC"));
        colUserC.setCellValueFactory(new PropertyValueFactory<>("id_utilisateur")); // You might override toString() in User

    }

    // ===============================
    // LOAD DATA
    // ===============================

    private void loadPublications() {
        try {
            ObservableList<Publication> list =
                    FXCollections.observableArrayList(publicationService.afficherh());
            tableCommunaute.setItems(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void loadCommentaires(Publication pub) {
        try {
            ObservableList<Commentaire> list = FXCollections.observableArrayList(
                    commentaireService.afficherParPublication(pub.getIdPublication())
            );
            tableCommentaires.setItems(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ===============================
    // SHOW COMMUNITY VIEW
    // ===============================

    @FXML
    private void handleShowCommunaute() {
        viewEmpty.setVisible(false);
        viewCommunaute.setVisible(true);
        loadPublications(); // refresh when entering page
    }

    // ===============================
    // ADD
    // ===============================

    @FXML
    private void handleAjouter() {
        try {
            // ✅ Format date for VARCHAR safely (length 19)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String now = LocalDateTime.now().format(formatter);

            // --- Input Validation ---

            // Validate Image URL
            String imgURL = tfImgURL.getText().trim();
            if (imgURL.isEmpty() ||
                    !(imgURL.endsWith(".jpeg") || imgURL.endsWith(".png") || imgURL.endsWith(".svg"))) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur de saisie");
                alert.setHeaderText(null);
                alert.setContentText("Veuillez entrer une URL d'image valide se terminant par .jpeg, .png ou .svg.");
                alert.showAndWait();
                return;
            }

            // Validate TypeCible dropdown
            if (cbTypeCible.getValue() == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur de saisie");
                alert.setHeaderText(null);
                alert.setContentText("Veuillez sélectionner un TypeCible.");
                alert.showAndWait();
                return;
            }

            // Validate Statut dropdown
            if (cbStatut.getValue() == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur de saisie");
                alert.setHeaderText(null);
                alert.setContentText("Veuillez sélectionner un Statut.");
                alert.showAndWait();
                return;
            }

            // Validate Description length
            String desc = tfDescription.getText().trim();
            int maxLength = 1000; // or whatever limit you want
            if (desc.length() > maxLength) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur de saisie");
                alert.setHeaderText(null);
                alert.setContentText("La description ne peut pas dépasser " + maxLength + " caractères.");
                alert.showAndWait();
                return;
            }

            // --- Assign User and Create Publication object ---
            User currentUser = new User();
            currentUser.setId(16); // replace with actual logged-in user ID
            currentUser.setNom("Admin"); // optional

            Publication p = new Publication();
            p.setId_utilisateur(currentUser); // ⚠ Must not be null
            p.setImgURL(imgURL);
            p.setTypeCible(cbTypeCible.getValue());
            p.setStatutP(cbStatut.getValue());
            p.setEstVerifie(cbVerifie.isSelected());
            p.setDescriptionP(desc);
            p.setDateCreation(now); // formatted date
            p.setDateModif(now);    // formatted date

            // --- Save to Database ---
            publicationService.ajouterh(p);

            // --- Refresh the TableView and Clear Form ---
            loadPublications();
            clearForm();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    // ===============================
    // UPDATE
    // ===============================

    @FXML
    private void handleModifier() {
        try {

            Publication selected = tableCommunaute.getSelectionModel().getSelectedItem();

            if (selected != null) {

                selected.setImgURL(tfImgURL.getText());
                selected.setTypeCible(cbTypeCible.getValue());
                selected.setStatutP(cbStatut.getValue());
                selected.setEstVerifie(cbVerifie.isSelected());
                selected.setDescriptionP(tfDescription.getText());
                selected.setDateModif(LocalDateTime.now().toString());

                publicationService.modifierh(selected);
                loadPublications();
                clearForm();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===============================
    // DELETE
    // ===============================

    @FXML
    private void handleDelete() {
        try {
            Publication selected = tableCommunaute.getSelectionModel().getSelectedItem();

            if (selected == null) {
                // Optional: warn if no publication is selected
                Alert warning = new Alert(Alert.AlertType.WARNING);
                warning.setTitle("Aucune sélection");
                warning.setHeaderText(null);
                warning.setContentText("Veuillez sélectionner une publication à supprimer !");
                warning.showAndWait();
                return;
            }

            // Confirmation dialog
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation de suppression");
            alert.setHeaderText(null);
            alert.setContentText("Voulez-vous vraiment supprimer cette publication ?");

            ButtonType buttonOui = new ButtonType("Oui");
            ButtonType buttonNon = new ButtonType("Non");

            alert.getButtonTypes().setAll(buttonOui, buttonNon);

            alert.showAndWait().ifPresent(type -> {
                if (type == buttonOui) {
                    try {
                        publicationService.supprimerh(selected);
                        loadPublications(); // refresh TableView
                        clearForm();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                // If "Non", do nothing
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
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

            // Current logged-in user
            User currentUser = new User();
            currentUser.setId(16); // replace with actual logged-in user ID
            currentUser.setNom("Admin");

            // Format date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String now = LocalDateTime.now().format(formatter);

            Commentaire c = new Commentaire();
            c.setId_utilisateur(currentUser);
            c.setPublication(pub);
            c.setContenuC(contenu);
            c.setDateCreationC(now);
            c.setStatutC(true);

            commentaireService.ajouterh(c);

            loadCommentaires(pub);
            tfContenuCommentaire.clear();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // ===============================
    // UPDATE COMMENTAIRE
    // ===============================
    @FXML
    private void handleModifierCommentaire() {
        try {
            // Get selected comment
            Commentaire selected = tableCommentaires.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Erreur", "Veuillez sélectionner un commentaire à modifier !");
                return;
            }

            // Get new content
            String nouveauContenu = tfContenuCommentaire.getText().trim();

            // Validation: not empty
            if (nouveauContenu.isEmpty()) {
                showAlert("Erreur", "Le commentaire ne peut pas être vide !");
                return;
            }

            // Validation: max length
            if (nouveauContenu.length() > 5000) {
                showAlert("Erreur", "Le commentaire est trop long !");
                return;
            }

            // Confirmation dialog
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation de modification");
            confirm.setHeaderText(null);
            confirm.setContentText("Voulez-vous vraiment modifier ce commentaire ?");

            ButtonType buttonOui = new ButtonType("Oui");
            ButtonType buttonNon = new ButtonType("Non");
            confirm.getButtonTypes().setAll(buttonOui, buttonNon);

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == buttonOui) {
                try {
                    // Apply modification
                    selected.setContenuC(nouveauContenu);
                    commentaireService.modifierh(selected); // handle SQLException inside try-catch
                    loadCommentaires(selected.getPublication());
                    tfContenuCommentaire.clear();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert("Erreur", "Une erreur est survenue lors de la modification du commentaire !");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Une erreur inattendue est survenue !");
        }
    }
    // ===============================
    // DELETE COMMENTAIRE
    // ===============================
    @FXML
    private void handleSupprimerCommentaire() {
        try {
            Commentaire selected = tableCommentaires.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Erreur", "Veuillez sélectionner un commentaire à supprimer !");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer ce commentaire ?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(type -> {
                if (type == ButtonType.YES) {
                    try {
                        commentaireService.supprimerh(selected);
                        loadCommentaires(selected.getPublication());
                        tfContenuCommentaire.clear();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // ===============================
    // HELPER
    // ===============================
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }



    // ===============================
    // CLEAR FORM
    // ===============================

    private void clearForm() {
        tfImgURL.clear();
        cbTypeCible.setValue(null);
        cbStatut.setValue(null);
        cbVerifie.setSelected(false);
        tfDescription.clear();
    }

}

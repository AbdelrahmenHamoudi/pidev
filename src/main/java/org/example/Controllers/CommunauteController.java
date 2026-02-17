package org.example.Controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.example.Entites.Commentaire;
import org.example.Entites.Publication;
import org.example.Entites.User;
import org.example.Services.CommentaireCRUD;
import org.example.Services.PublicationCRUD;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class CommunauteController {

    @FXML
    private TilePane tilePublications;

    @FXML
    private VBox publicationDetailCard;

    @FXML
    private ImageView imgPub;

    @FXML
    private Label lblPubDesc, lblPubType, lblPubStatut, lblPubVerifie;

    @FXML
    private ListView<Commentaire> listCommentaires;

    @FXML
    private TextField tfNewComment;

    private final PublicationCRUD publicationService = new PublicationCRUD();
    private final CommentaireCRUD commentaireService = new CommentaireCRUD();

    private Publication selectedPublication;
    @FXML
    private ScrollPane frontOfficeScroll;


    @FXML
    void initialize() {
        loadPublicationsFront();
        // Make ListView show only comment content
        listCommentaires.setCellFactory(lv -> new ListCell<Commentaire>() {
            @Override
            protected void updateItem(Commentaire item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getContenuC()); // Only show the comment content
                }
            }
        });
    }

    /** Load all publications and display as cards */
    private void loadPublicationsFront() {
        try {
            List<Publication> publications = publicationService.afficherh();
            System.out.println("Publications fetched: " + publications.size());
            tilePublications.getChildren().clear();

            for (Publication p : publications) {
                VBox card = createPublicationCard(p);
                tilePublications.getChildren().add(card);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Create a single publication card with image and description */
    private VBox createPublicationCard(Publication p) {
        VBox card = new VBox(5);
        card.getStyleClass().add("card");
        card.setMaxWidth(220); // slightly wider than the image

        ImageView img = new ImageView();
        try {
            img.setImage(new Image(getClass().getResourceAsStream("/img/" + p.getImgURL())));
        } catch (Exception e) {
            img.setImage(new Image(getClass().getResourceAsStream("/img/default.png")));
        }
        img.setFitWidth(200);
        img.setFitHeight(150);
        img.setPreserveRatio(true);
        img.setMouseTransparent(true); // allow VBox to handle clicks

        Label desc = new Label(p.getDescriptionP());
        desc.setWrapText(true);
        desc.setMouseTransparent(true); // allow VBox to handle clicks

        card.getChildren().addAll(img, desc);
        double tileWidth = tilePublications.getWidth();
        int columns = 3; // desired number of columns
        double hGap = tilePublications.getHgap();
        double cardWidth = (tileWidth - (columns - 1) * hGap) / columns;
        card.setPrefWidth(cardWidth);

        card.setOnMouseClicked(e -> showPublicationDetails(p));

        return card;
    }



    /** Show selected publication details and load its comments */
    private void showPublicationDetails(Publication p) {
        selectedPublication = p;
        publicationDetailCard.setVisible(true);

        try {
            // Attempt to load the image from resources/img/
            String imgPath = "/img/" + p.getImgURL(); // e.g., "/img/photo1.png"
            Image img = new Image(getClass().getResourceAsStream(imgPath));
            imgPub.setImage(img);
        } catch (Exception e) {
            // Fallback image if the file is not found
            Image fallback = new Image(getClass().getResourceAsStream("/img/Placeholderimagenotfound.jpg"));
            imgPub.setImage(fallback);
            System.out.println("Image not found for publication: " + p.getImgURL() + ". Using fallback.");
        }

        lblPubDesc.setText(p.getDescriptionP());
        lblPubType.setText("Type: " + p.getTypeCible());
        lblPubStatut.setText("Statut: " + p.getStatutP());
        lblPubVerifie.setText("Vérifié: " + (p.isEstVerifie() ? "Oui" : "Non"));

        loadCommentaires(p);
        // Scroll to show the newly revealed detail card
        frontOfficeScroll.layout(); // recalc bounds
        frontOfficeScroll.setVvalue(1.0); // scrolls to bottom
    }


    /** Load comments for a given publication */
    private void loadCommentaires(Publication p) {
        try {
            List<Commentaire> commentaires = commentaireService.afficherh();
            List<Commentaire> filtered = commentaires.stream()
                    .filter(c -> c.getPublication().getIdPublication() == p.getIdPublication())
                    .collect(Collectors.toList());
            listCommentaires.setItems(FXCollections.observableArrayList(filtered));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Add a new comment to the selected publication */
    @FXML
    private void handleAjouterCommentaireFront() {
        try {
            String content = tfNewComment.getText().trim();
            if (content.isEmpty() || selectedPublication == null) return;

            User currentUser = new User();
            currentUser.setId(1); // Replace with actual logged-in client ID
            currentUser.setNom("Client");

            Commentaire c = new Commentaire();
            c.setContenuC(content);
            c.setId_utilisateur(currentUser);
            c.setPublication(selectedPublication);
            c.setDateCreationC(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            c.setStatutC(true);

            commentaireService.ajouterh(c);
            loadCommentaires(selectedPublication);
            tfNewComment.clear();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleHoverButton(MouseEvent event) {
        Button btn = (Button) event.getSource();
        btn.setStyle("-fx-background-color: #16a085; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20;");
    }

    @FXML
    private void handleExitButton(MouseEvent event) {
        Button btn = (Button) event.getSource();
        btn.setStyle("-fx-background-color: #1ABC9C; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20;");
    }
}

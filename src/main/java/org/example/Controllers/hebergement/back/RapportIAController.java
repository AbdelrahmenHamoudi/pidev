package org.example.Controllers.hebergement.back;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.example.Entites.hebergement.Hebergement;
import org.example.Entites.hebergement.Reservation;
import org.example.Services.hebergement.GeminiReportService;
import org.example.Services.hebergement.GeminiReportService.RapportStats;
import org.example.Services.hebergement.HebergementCRUD;
import org.example.Services.hebergement.ReservationCRUD;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

/**
 * ✅ Contrôleur du rapport IA mensuel avec export PDF (iText 8)
 *
 * Dépendance Maven à ajouter dans pom.xml :
 * <dependency>
 *     <groupId>com.itextpdf</groupId>
 *     <artifactId>itext-core</artifactId>
 *     <version>8.0.4</version>
 *     <type>pom</type>
 * </dependency>
 */
public class RapportIAController implements Initializable {

    @FXML private Label lblMoisAnnee;
    @FXML private Label lblStatusIA;
    @FXML private TextArea txtRapport;
    @FXML private Button btnGenerer;
    @FXML private Button btnCopier;
    @FXML private Button btnExportPDF;   // ✅ NOUVEAU bouton PDF
    @FXML private ProgressIndicator progressIA;
    @FXML private VBox statsPreview;

    @FXML private Label lblStatReservations;
    @FXML private Label lblStatRevenus;
    @FXML private Label lblStatOccupation;
    @FXML private Label lblStatVille;
    @FXML private Label lblStatType;
    @FXML private Label lblStatHebergements;

    private final HebergementCRUD hebergementCRUD = new HebergementCRUD();
    private final ReservationCRUD reservationCRUD = new ReservationCRUD();
    private final GeminiReportService geminiService = new GeminiReportService();

    // Garde en mémoire les stats pour le PDF
    private RapportStats dernieresStats = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String moisActuel = LocalDate.now().getMonth()
                .getDisplayName(TextStyle.FULL, Locale.FRENCH);
        lblMoisAnnee.setText(moisActuel.toUpperCase() + " " + LocalDate.now().getYear());

        progressIA.setVisible(false);
        btnCopier.setDisable(true);
        btnExportPDF.setDisable(true); // Désactivé jusqu'à génération

        chargerStatsPreview();
    }

    private void chargerStatsPreview() {
        try {
            dernieresStats = calculerStatsMensuelles();
            lblStatReservations.setText(dernieresStats.getTotalReservations() + " réservations");
            lblStatRevenus.setText(String.format("%.0f DT", dernieresStats.getRevenusTotaux()));
            lblStatOccupation.setText(String.format("%.1f%%", dernieresStats.getTauxOccupation()));
            lblStatVille.setText(dernieresStats.getVillePlusDemandee());
            lblStatType.setText(dernieresStats.getTypePlusReserve());
            lblStatHebergements.setText(dernieresStats.getNombreHebergements() + " actifs");
        } catch (Exception e) {
            lblStatusIA.setText("⚠️ Erreur chargement stats : " + e.getMessage());
        }
    }

    @FXML
    private void genererRapport() {
        btnGenerer.setDisable(true);
        btnCopier.setDisable(true);
        btnExportPDF.setDisable(true);
        progressIA.setVisible(true);
        lblStatusIA.setText("🔄 Analyse des données en cours...");
        txtRapport.setText("⏳ L'IA analyse vos données...\n\nCela peut prendre quelques secondes.");

        new Thread(() -> {
            try {
                RapportStats stats = calculerStatsMensuelles();
                dernieresStats = stats;

                Platform.runLater(() ->
                        lblStatusIA.setText("🤖 Génération du rapport par Groq AI..."));

                String rapport = geminiService.genererRapportMensuel(stats);

                Platform.runLater(() -> {
                    txtRapport.setText(rapport);
                    lblStatusIA.setText("✅ Rapport généré avec succès !");
                    btnGenerer.setDisable(false);
                    btnCopier.setDisable(false);
                    btnExportPDF.setDisable(false); // ✅ Activer le bouton PDF
                    progressIA.setVisible(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    txtRapport.setText("❌ Erreur lors de la génération :\n\n" + e.getMessage());
                    lblStatusIA.setText("❌ Erreur API");
                    btnGenerer.setDisable(false);
                    progressIA.setVisible(false);
                });
            }
        }).start();
    }

    @FXML
    private void copierRapport() {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(txtRapport.getText());
        clipboard.setContent(content);
        lblStatusIA.setText("📋 Rapport copié dans le presse-papiers !");
    }

    // =====================================================================
    // ✅ EXPORT PDF – Déclenché par le bouton "Télécharger PDF"
    // =====================================================================
    @FXML
    private void exporterPDF() {
        String contenuRapport = txtRapport.getText();
        if (contenuRapport == null || contenuRapport.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Attention",
                    "Veuillez d'abord générer un rapport avant de l'exporter.");
            return;
        }

        // Dialogue de sauvegarde
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport PDF");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        String nomFichier = "Rapport_RE7LA_" +
                (dernieresStats != null ? dernieresStats.getMois() + "_" + dernieresStats.getAnnee() : "mensuel")
                + ".pdf";
        fileChooser.setInitialFileName(nomFichier);

        Stage stage = (Stage) btnExportPDF.getScene().getWindow();
        File fichier = fileChooser.showSaveDialog(stage);

        if (fichier != null) {
            try {
                genererPDF(fichier.getAbsolutePath(), contenuRapport);
                lblStatusIA.setText("✅ PDF exporté : " + fichier.getName());
                showAlert(Alert.AlertType.INFORMATION, "PDF exporté !",
                        "Rapport sauvegardé avec succès :\n" + fichier.getAbsolutePath());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur PDF",
                        "Impossible de générer le PDF :\n" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * ✅ Génère le PDF professionnel avec iText 8
     */
    private void genererPDF(String cheminFichier, String contenuRapport) throws Exception {

        // Couleurs RE7LA
        DeviceRgb couleurOrange    = new DeviceRgb(243, 156, 18);   // #F39C12
        DeviceRgb couleurBleuNuit  = new DeviceRgb(44,  62,  80);   // #2C3E50
        DeviceRgb couleurTurquoise = new DeviceRgb(26,  188, 156);  // #1ABC9C
        DeviceRgb couleurGrisClair = new DeviceRgb(248, 249, 250);  // #F8F9FA
        DeviceRgb couleurTexte     = new DeviceRgb(51,  51,  51);   // #333333

        PdfWriter writer   = new PdfWriter(cheminFichier);
        PdfDocument pdf    = new PdfDocument(writer);
        Document document  = new Document(pdf, PageSize.A4);
        document.setMargins(40, 50, 40, 50);

        // ── FONTS ──
        PdfFont fontRegular = PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
        PdfFont fontBold    = PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);

        // ═══════════════════════════════════════
        // HEADER – Bannière orange
        // ═══════════════════════════════════════
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(couleurBleuNuit)
                .setBorder(Border.NO_BORDER);

        // Logo / Titre gauche
        Cell cellTitre = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(20);
        cellTitre.add(new Paragraph("RE7LA Tunisie")
                .setFont(fontBold).setFontSize(22)
                .setFontColor(couleurOrange));
        cellTitre.add(new Paragraph("Rapport Mensuel Intelligent")
                .setFont(fontRegular).setFontSize(13)
                .setFontColor(ColorConstants.WHITE));
        headerTable.addCell(cellTitre);

        // Date droite
        String moisLabel = dernieresStats != null
                ? dernieresStats.getMois().toUpperCase() + " " + dernieresStats.getAnnee()
                : LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH).toUpperCase()
                + " " + LocalDate.now().getYear();

        Cell cellDate = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(20)
                .setTextAlignment(TextAlignment.RIGHT);
        cellDate.add(new Paragraph("📊")
                .setFontSize(30).setFontColor(couleurTurquoise)
                .setTextAlignment(TextAlignment.RIGHT));
        cellDate.add(new Paragraph(moisLabel)
                .setFont(fontBold).setFontSize(11)
                .setFontColor(couleurOrange)
                .setTextAlignment(TextAlignment.RIGHT));
        cellDate.add(new Paragraph("Généré par Groq AI")
                .setFont(fontRegular).setFontSize(9)
                .setFontColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.RIGHT));
        headerTable.addCell(cellDate);

        document.add(headerTable);
        document.add(new Paragraph("\n"));

        // ═══════════════════════════════════════
        // TABLEAU DES STATISTIQUES CLÉS
        // ═══════════════════════════════════════
        if (dernieresStats != null) {
            document.add(new Paragraph("Statistiques du mois")
                    .setFont(fontBold).setFontSize(13)
                    .setFontColor(couleurBleuNuit)
                    .setMarginBottom(8));

            Table statsTable = new Table(UnitValue.createPercentArray(new float[]{33, 33, 34}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(20);

            // Ligne 1
            ajouterCellStat(statsTable, "📅 Réservations",
                    String.valueOf(dernieresStats.getTotalReservations()),
                    couleurBleuNuit, couleurGrisClair, fontBold, fontRegular);
            ajouterCellStat(statsTable, "💰 Revenus",
                    String.format("%.0f DT", dernieresStats.getRevenusTotaux()),
                    couleurTurquoise, couleurGrisClair, fontBold, fontRegular);
            ajouterCellStat(statsTable, "📊 Taux d'occupation",
                    String.format("%.1f%%", dernieresStats.getTauxOccupation()),
                    couleurOrange, couleurGrisClair, fontBold, fontRegular);

            // Ligne 2
            ajouterCellStat(statsTable, "📍 Ville leader",
                    dernieresStats.getVillePlusDemandee(),
                    couleurBleuNuit, couleurGrisClair, fontBold, fontRegular);
            ajouterCellStat(statsTable, "🏠 Type préféré",
                    dernieresStats.getTypePlusReserve(),
                    couleurTurquoise, couleurGrisClair, fontBold, fontRegular);
            ajouterCellStat(statsTable, "🏨 Hébergements",
                    dernieresStats.getNombreHebergements() + " actifs",
                    couleurOrange, couleurGrisClair, fontBold, fontRegular);

            document.add(statsTable);
        }

        // ═══════════════════════════════════════
        // SÉPARATEUR
        // ═══════════════════════════════════════
        SolidLine ligne = new SolidLine(1.5f);
        ligne.setColor(couleurOrange);
        document.add(new LineSeparator(ligne).setMarginBottom(15));

        // ═══════════════════════════════════════
        // TITRE DU RAPPORT IA
        // ═══════════════════════════════════════
        document.add(new Paragraph("Analyse et Recommandations IA")
                .setFont(fontBold).setFontSize(15)
                .setFontColor(couleurBleuNuit)
                .setMarginBottom(12));

        // ═══════════════════════════════════════
        // CONTENU DU RAPPORT (texte IA)
        // ═══════════════════════════════════════
        String[] lignes = contenuRapport.split("\n");
        for (String ligne2 : lignes) {
            String l = ligne2.trim();
            if (l.isEmpty()) {
                document.add(new Paragraph(" ").setFontSize(5));
                continue;
            }

            // Détection des titres de sections (contiennent des emojis ou chiffres + point)
            boolean estTitre = l.matches("^[0-9]+[.)\\-].*")
                    || l.startsWith("##")
                    || l.startsWith("**")
                    || l.matches("^[📊📈🔍💡🔮📅💰🏠🏨📍].*");

            if (estTitre) {
                // Nettoyer les marqueurs markdown
                String titre = l.replaceAll("^#+\\s*", "")
                        .replaceAll("\\*\\*", "")
                        .trim();
                document.add(new Paragraph(titre)
                        .setFont(fontBold).setFontSize(12)
                        .setFontColor(couleurBleuNuit)
                        .setMarginTop(10).setMarginBottom(4)
                        .setBackgroundColor(couleurGrisClair)
                        .setPadding(6)
                        .setBorderLeft(new SolidBorder(couleurOrange, 3)));
            } else if (l.startsWith("•") || l.startsWith("-") || l.startsWith("*")) {
                // Puces
                String puce = "•  " + l.replaceAll("^[•\\-*]\\s*", "")
                        .replaceAll("\\*\\*", "");
                document.add(new Paragraph(puce)
                        .setFont(fontRegular).setFontSize(10)
                        .setFontColor(couleurTexte)
                        .setMarginLeft(15).setMarginBottom(2));
            } else {
                // Texte normal
                String texte = l.replaceAll("\\*\\*", "");
                document.add(new Paragraph(texte)
                        .setFont(fontRegular).setFontSize(10)
                        .setFontColor(couleurTexte)
                        .setMarginBottom(3)
                        .setTextAlignment(TextAlignment.JUSTIFIED));
            }
        }

        // ═══════════════════════════════════════
        // FOOTER
        // ═══════════════════════════════════════
        document.add(new Paragraph("\n"));
        SolidLine ligneFooter = new SolidLine(0.5f);
        ligneFooter.setColor(new DeviceRgb(200, 200, 200));
        document.add(new LineSeparator(ligneFooter));

        document.add(new Paragraph(
                "RE7LA Tunisie  •  Rapport généré automatiquement par IA  •  " +
                        LocalDate.now())
                .setFont(fontRegular).setFontSize(8)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(8));

        document.close();
    }

    /**
     * Helper : crée une cellule de statistique stylisée
     */
    private void ajouterCellStat(Table table, String label, String valeur,
                                 DeviceRgb couleurAccent, DeviceRgb couleurFond,
                                 PdfFont fontBold, PdfFont fontRegular) {
        Cell cell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(couleurFond)
                .setPadding(12)
                .setMargin(3)
                .setBorderTop(new SolidBorder(couleurAccent, 3));

        cell.add(new Paragraph(label)
                .setFont(fontRegular).setFontSize(9)
                .setFontColor(new DeviceRgb(100, 116, 139)));
        cell.add(new Paragraph(valeur)
                .setFont(fontBold).setFontSize(14)
                .setFontColor(couleurAccent));

        table.addCell(cell);
    }

    // ── Calcul des statistiques ──────────────────────────────────────────
    private RapportStats calculerStatsMensuelles() throws Exception {
        List<Hebergement> hebergements = hebergementCRUD.afficherh();
        List<Reservation> toutesReservations = reservationCRUD.afficherh();

        LocalDate maintenant = LocalDate.now();
        int moisActuel = maintenant.getMonthValue();
        int annee = maintenant.getYear();

        List<Reservation> reservationsMois = toutesReservations.stream()
                .filter(r -> {
                    try {
                        String dateStr = r.getDateDebutR().toString();
                        return dateStr.startsWith(annee + "-" +
                                String.format("%02d", moisActuel));
                    } catch (Exception e) { return true; }
                }).toList();

        double revenusTotaux = reservationsMois.stream()
                .mapToDouble(r -> r.getHebergement() != null
                        ? r.getHebergement().getPrixParNuit() : 0).sum();

        long disponibles = hebergements.stream()
                .filter(Hebergement::isDisponible_heberg).count();
        double tauxOccupation = hebergements.isEmpty() ? 0 :
                (1.0 - (double) disponibles / hebergements.size()) * 100;

        Map<String, Long> parVille = new HashMap<>();
        Map<String, Long> parType  = new HashMap<>();
        for (Reservation r : reservationsMois) {
            if (r.getHebergement() != null) {
                parVille.merge(r.getHebergement().getTitre(), 1L, Long::sum);
                parType.merge(r.getHebergement().getType_hebergement(), 1L, Long::sum);
            }
        }

        String villePlus  = parVille.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("N/A");
        String villeMoins = parVille.entrySet().stream().min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("N/A");
        double pctVille   = parVille.isEmpty() || reservationsMois.isEmpty() ? 0 :
                (double) parVille.getOrDefault(villePlus, 0L) / reservationsMois.size() * 100;
        String typePlus   = parType.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("N/A");

        int meilleurNb = (int) parVille.getOrDefault(villePlus, 0L).longValue();
        int pireNb     = (int) parVille.getOrDefault(villeMoins, 0L).longValue();

        double prixMoyen     = hebergements.stream()
                .mapToDouble(Hebergement::getPrixParNuit).average().orElse(0);
        int capaciteMoyenne  = (int) hebergements.stream()
                .mapToInt(Hebergement::getCapacite).average().orElse(0);

        String nomMois = Month.of(moisActuel).getDisplayName(TextStyle.FULL, Locale.FRENCH);
        // Mettre en majuscule la première lettre
        nomMois = nomMois.substring(0, 1).toUpperCase() + nomMois.substring(1);

        return new RapportStats()
                .mois(nomMois).annee(annee)
                .totalReservations(reservationsMois.size())
                .revenusTotaux(revenusTotaux).tauxOccupation(tauxOccupation)
                .nombreHebergements(hebergements.size())
                .meilleurHebergement(villePlus).meilleurHebergementNbRes(meilleurNb)
                .pireHebergement(villeMoins).pireHebergementNbRes(pireNb)
                .villePlusDemandee(villePlus).pourcentageVillePrincipale(pctVille)
                .villeMoinsDemandee(villeMoins).typePlusReserve(typePlus)
                .prixMoyen(prixMoyen).capaciteMoyenne(capaciteMoyenne);
    }

    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
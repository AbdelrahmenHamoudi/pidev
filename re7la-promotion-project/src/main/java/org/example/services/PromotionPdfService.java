package org.example.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.layout.properties.BorderRadius;
import org.example.models.Promotion;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class PromotionPdfService {

    // ═══ Palette RE7LA ═══
    private static final DeviceRgb ORANGE    = new DeviceRgb(0xF3, 0x9C, 0x12);
    private static final DeviceRgb JAUNE     = new DeviceRgb(0xF7, 0xDC, 0x6F);
    private static final DeviceRgb BLEU_NUIT = new DeviceRgb(0x2C, 0x3E, 0x50);
    private static final DeviceRgb TURQUOISE = new DeviceRgb(0x1A, 0xBC, 0x9C);
    private static final DeviceRgb BG_LIGHT  = new DeviceRgb(0xEE, 0xF2, 0xF6);
    private static final DeviceRgb WHITE     = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb GRAY      = new DeviceRgb(0x64, 0x74, 0x8B);

    // ═══ Traductions FR / EN ═══
    private static final Map<String, Map<String, String>> TR = new HashMap<>();
    static {
        Map<String, String> fr = new HashMap<>();
        fr.put("title","FICHE PROMOTION"); fr.put("details","Détails de la Promotion");
        fr.put("name","Nom"); fr.put("description","Description");
        fr.put("discount_pct","Réduction (%)"); fr.put("discount_fix","Réduction Fixe (TND)");
        fr.put("date_debut","Date de Début");
        fr.put("date_fin","Date de Fin"); fr.put("type","Type");
        fr.put("pack","Pack Combiné"); fr.put("individuel","Individuel");
        fr.put("stats","Statistiques"); fr.put("vues","Vues totales");
        fr.put("reservations","Réservations"); fr.put("status","Statut");
        fr.put("locked","🔒 Verrouillée"); fr.put("unlocked","🔓 Ouverte");
        fr.put("active","✅ Active"); fr.put("expired","⛔ Expirée");
        fr.put("qr_title","Code Promo & QR Code");
        fr.put("qr_info","Scannez ce QR Code ou saisissez le code pour débloquer.");
        fr.put("cachet","VALIDÉ PAR RE7LA"); fr.put("generated","Généré le");
        fr.put("footer","RE7LA · Plateforme Touristique Tunisienne · Tous droits réservés");
        TR.put("fr", fr);

        Map<String, String> en = new HashMap<>();
        en.put("title","PROMOTION SHEET"); en.put("details","Promotion Details");
        en.put("name","Name"); en.put("description","Description");
        en.put("discount_pct","Discount (%)"); en.put("discount_fix","Fixed Discount (TND)");
        en.put("date_debut","Start Date");
        en.put("date_fin","End Date"); en.put("type","Type");
        en.put("pack","Combined Pack"); en.put("individuel","Individual");
        en.put("stats","Statistics"); en.put("vues","Total Views");
        en.put("reservations","Reservations"); en.put("status","Status");
        en.put("locked","🔒 Locked"); en.put("unlocked","🔓 Open");
        en.put("active","✅ Active"); en.put("expired","⛔ Expired");
        en.put("qr_title","Promo Code & QR Code");
        en.put("qr_info","Scan this QR Code or enter the code to unlock this promotion.");
        en.put("cachet","VALIDATED BY RE7LA"); en.put("generated","Generated on");
        en.put("footer","RE7LA · Tunisian Tourism Platform · All rights reserved");
        TR.put("en", en);
    }

    private String t(String key, String lang) {
        return TR.getOrDefault(lang, TR.get("fr")).getOrDefault(key, key);
    }

    // ═══════════════════════════════════════════════
    // POINT D'ENTRÉE PRINCIPAL
    // ═══════════════════════════════════════════════
    public void generatePdf(Promotion promo, String promoCode, String lang, String outputPath) throws Exception {
        PdfWriter writer = new PdfWriter(outputPath);
        PdfDocument pdf  = new PdfDocument(writer);
        Document doc     = new Document(pdf, PageSize.A4);
        doc.setMargins(0, 0, 30, 0);

        addHeader(doc, promo, lang);

        doc.setLeftMargin(50);
        doc.setRightMargin(50);
        doc.add(new Paragraph(" ").setMarginTop(15));

        addDetailsSection(doc, promo, lang);
        addStatsSection(doc, promo, lang);

        if (promo.isLocked() && promoCode != null && !promoCode.isEmpty()) {
            addQrSection(doc, promoCode, promo.getId(), lang);
        }

        addCachet(doc, lang);
        addFooter(doc, lang);
        doc.close();
    }

    // ═══ HEADER BANNIÈRE ═══
    private void addHeader(Document doc, Promotion promo, String lang) {
        // ── Single-color top bar ──
        Table topBar = new Table(1).useAllAvailableWidth().setMargin(0).setPadding(0);
        Cell topCell = new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(BLEU_NUIT).setPadding(0);

        // Logo + title in a single clean row
        Table inner = new Table(UnitValue.createPercentArray(new float[]{1f, 3f, 1f}))
                .useAllAvailableWidth().setPadding(0).setMargin(0);

        // Left — logo
        Cell logoCell = new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(BLEU_NUIT).setPadding(24).setVerticalAlignment(VerticalAlignment.MIDDLE);
        try {
            URL logoUrl = getClass().getResource("/images/logo.png");
            if (logoUrl != null) {
                Image logo = new Image(ImageDataFactory.create(logoUrl)).setWidth(65);
                logoCell.add(logo.setHorizontalAlignment(HorizontalAlignment.CENTER));
            } else {
                logoCell.add(new Paragraph("RE7LA").setFontColor(ORANGE).setFontSize(22).setBold()
                        .setTextAlignment(TextAlignment.CENTER));
            }
        } catch (Exception e) {
            logoCell.add(new Paragraph("RE7LA").setFontColor(ORANGE).setFontSize(22).setBold()
                    .setTextAlignment(TextAlignment.CENTER));
        }

        // Center — title block
        Cell titleCell = new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(BLEU_NUIT).setPadding(28).setVerticalAlignment(VerticalAlignment.MIDDLE);
        titleCell.add(new Paragraph(t("title", lang))
                .setFontColor(ORANGE).setFontSize(10).setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setCharacterSpacing(3).setMarginBottom(6));
        titleCell.add(new Paragraph(promo.getName())
                .setFontColor(WHITE).setFontSize(20).setBold()
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4));
        titleCell.add(new Paragraph("ID #" + promo.getId())
                .setFontColor(JAUNE).setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER));

        // Right — status badges
        Cell badgeCell = new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(BLEU_NUIT).setPadding(24).setVerticalAlignment(VerticalAlignment.MIDDLE);
        String typeLabel = promo.isPack() ? "📦 " + t("pack", lang) : "🎁 " + t("individuel", lang);
        badgeCell.add(new Paragraph(typeLabel).setFontColor(JAUNE).setFontSize(10).setBold()
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(8));
        String lockStr = promo.isLocked() ? t("locked", lang) : t("unlocked", lang);
        badgeCell.add(new Paragraph(lockStr).setFontColor(WHITE).setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(6));
        String statusStr = promo.isActive() ? t("active", lang) : t("expired", lang);
        badgeCell.add(new Paragraph(statusStr).setFontColor(promo.isActive() ? TURQUOISE : GRAY)
                .setFontSize(9).setBold().setTextAlignment(TextAlignment.CENTER));

        inner.addCell(logoCell); inner.addCell(titleCell); inner.addCell(badgeCell);
        topCell.add(inner);
        topBar.addCell(topCell);
        doc.add(topBar);

        // ── Orange accent stripe ──
        Table stripe = new Table(1).useAllAvailableWidth().setMargin(0);
        Cell sc = new Cell().setBorder(Border.NO_BORDER).setBackgroundColor(ORANGE)
                .setHeight(5).setPadding(0);
        stripe.addCell(sc);
        doc.add(stripe);
    }

    // ═══ SECTION DÉTAILS ═══
    private void addDetailsSection(Document doc, Promotion promo, String lang) {
        doc.add(sectionTitle("◆  " + t("details", lang)));

        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1.6f}))
                .useAllAvailableWidth().setMarginBottom(14);

        addRow(table, t("name", lang), promo.getName());
        addRow(table, t("type", lang), promo.isPack() ? t("pack", lang) : t("individuel", lang));
        addRow(table, t("date_debut", lang), promo.getStartDate().toString());
        addRow(table, t("date_fin", lang), promo.getEndDate().toString());

        if (promo.getDiscountPercentage() != null)
            addRow(table, t("discount_pct", lang), promo.getDiscountPercentage() + " %");
        if (promo.getDiscountFixed() != null)
            addRow(table, t("discount_fix", lang), promo.getDiscountFixed() + " TND");

        doc.add(table);

        // Description block with improved alignment
        doc.add(new Paragraph("◆  " + t("description", lang))
                .setFontColor(BLEU_NUIT).setBold().setFontSize(11)
                .setMarginBottom(5).setMarginTop(2));
        doc.add(new Paragraph(promo.getDescription())
                .setFontColor(GRAY).setFontSize(9.5f)
                .setBackgroundColor(BG_LIGHT)
                .setPaddingLeft(16).setPaddingRight(16).setPaddingTop(12).setPaddingBottom(12)
                .setMarginBottom(18)
                .setBorderLeft(new SolidBorder(ORANGE, 3))
                .setTextAlignment(TextAlignment.JUSTIFIED));
    }

    // ═══ SECTION STATS ═══
    private void addStatsSection(Document doc, Promotion promo, String lang) {
        doc.add(sectionTitle("◆  " + t("stats", lang)));

        Table st = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth().setMarginBottom(20);

        // Vues
        Cell vc = new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(new DeviceRgb(0xFF, 0xF9, 0xE6))
                .setPadding(20).setMargin(4).setBorderLeft(new SolidBorder(JAUNE, 4));
        vc.add(new Paragraph(String.valueOf(promo.getNbVues()))
                .setFontColor(BLEU_NUIT).setFontSize(38).setBold().setTextAlignment(TextAlignment.CENTER));
        vc.add(new Paragraph("👁  " + t("vues", lang))
                .setFontColor(GRAY).setFontSize(11).setTextAlignment(TextAlignment.CENTER));
        st.addCell(vc);

        // Réservations
        Cell rc = new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(new DeviceRgb(0xE6, 0xF5, 0xF1))
                .setPadding(20).setMargin(4).setBorderLeft(new SolidBorder(TURQUOISE, 4));
        rc.add(new Paragraph(String.valueOf(promo.getNbReservations()))
                .setFontColor(BLEU_NUIT).setFontSize(38).setBold().setTextAlignment(TextAlignment.CENTER));
        rc.add(new Paragraph("📋  " + t("reservations", lang))
                .setFontColor(GRAY).setFontSize(11).setTextAlignment(TextAlignment.CENTER));
        st.addCell(rc);

        doc.add(st);
    }

    // ═══ SECTION QR CODE ═══
    private void addQrSection(Document doc, String promoCode, int promoId, String lang) throws Exception {
        doc.add(sectionTitle("◆  " + t("qr_title", lang)));

        String qrContent = "{\"code\":\"" + promoCode + "\",\"promotion_id\":" + promoId + ",\"app\":\"RE7LA\"}";
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 1);
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bm = writer.encode(qrContent, BarcodeFormat.QR_CODE, 220, 220, hints);
        BufferedImage qrBuf = MatrixToImageWriter.toBufferedImage(bm);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(qrBuf, "PNG", baos);
        Image qrImg = new Image(ImageDataFactory.create(baos.toByteArray()))
                .setWidth(140).setHorizontalAlignment(HorizontalAlignment.CENTER);

        Table qt = new Table(UnitValue.createPercentArray(new float[]{1, 1.8f}))
                .useAllAvailableWidth().setMarginBottom(20);

        // QR image
        Cell qc = new Cell().setBorder(new SolidBorder(JAUNE, 2))
                .setPadding(14).setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.CENTER).setBackgroundColor(WHITE);
        qc.add(qrImg);
        qt.addCell(qc);

        // Info + code
        Cell ic = new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(BG_LIGHT).setPadding(22)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorderLeft(new SolidBorder(ORANGE, 3));
        ic.add(new Paragraph(t("qr_info", lang)).setFontColor(GRAY).setFontSize(10).setMarginBottom(18));
        ic.add(new Paragraph(promoCode).setFontColor(ORANGE).setFontSize(20).setBold()
                .setTextAlignment(TextAlignment.CENTER).setBackgroundColor(WHITE)
                .setPadding(12).setMarginBottom(0));
        qt.addCell(ic);

        doc.add(qt);
    }

    // ═══ CACHET VIRTUEL ═══
    private void addCachet(Document doc, String lang) {
        Table ct = new Table(UnitValue.createPercentArray(new float[]{1.4f, 1f}))
                .useAllAvailableWidth().setMarginTop(18).setMarginBottom(10);

        Cell stamp = new Cell().setBorder(new SolidBorder(ORANGE, 2.5f))
                .setPadding(14).setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(new DeviceRgb(0xFF, 0xF9, 0xE6));
        stamp.add(new Paragraph("✦  " + t("cachet", lang) + "  ✦")
                .setFontColor(ORANGE).setFontSize(13).setBold());
        stamp.add(new Paragraph(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm")))
                .setFontColor(GRAY).setFontSize(9).setMarginTop(4));

        Cell dateCell = new Cell().setBorder(Border.NO_BORDER)
                .setPadding(14).setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.RIGHT);
        dateCell.add(new Paragraph(t("generated", lang) + " :")
                .setFontColor(GRAY).setFontSize(9).setMarginBottom(2));
        dateCell.add(new Paragraph(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy, HH:mm")))
                .setFontColor(BLEU_NUIT).setFontSize(9).setBold());

        ct.addCell(stamp); ct.addCell(dateCell);
        doc.add(ct);
    }

    // ═══ FOOTER ═══
    private void addFooter(Document doc, String lang) {
        Table ft = new Table(1).useAllAvailableWidth();
        Cell fc = new Cell().setBackgroundColor(BLEU_NUIT).setBorder(Border.NO_BORDER).setPadding(13);
        fc.add(new Paragraph(t("footer", lang))
                .setFontColor(WHITE).setFontSize(9).setTextAlignment(TextAlignment.CENTER));
        ft.addCell(fc);
        doc.add(ft);
    }

    // ═══ HELPERS ═══
    private Paragraph sectionTitle(String text) {
        return new Paragraph(text).setFontColor(BLEU_NUIT).setFontSize(13).setBold()
                .setBorderBottom(new SolidBorder(ORANGE, 2))
                .setMarginBottom(10).setMarginTop(4).setPaddingBottom(5);
    }

    private void addRow(Table table, String label, String value) {
        Cell lc = new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(BG_LIGHT).setPadding(10)
                .setBorderBottom(new SolidBorder(WHITE, 1));
        lc.add(new Paragraph(label).setFontColor(GRAY).setFontSize(8.5f).setBold());

        Cell vc = new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(WHITE).setPadding(10)
                .setBorderLeft(new SolidBorder(ORANGE, 2))
                .setBorderBottom(new SolidBorder(BG_LIGHT, 1));
        vc.add(new Paragraph(value != null ? value : "—").setFontColor(BLEU_NUIT).setFontSize(10));

        table.addCell(lc); table.addCell(vc);
    }
}
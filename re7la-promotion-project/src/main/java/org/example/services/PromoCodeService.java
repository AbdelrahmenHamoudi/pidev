package org.example.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.Image;
import org.example.models.PromoCode;
import org.example.utils.DatabaseConnection;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;

public class PromoCodeService {

    // ── Génération code texte unique ──
    private String generateUniqueCode() {
        String uuid = UUID.randomUUID().toString().toUpperCase().replace("-", "");
        return "RE7LA-" + uuid.substring(0, 4) + "-" + uuid.substring(4, 8);
    }

    private String buildQrContent(String code, int promotionId) {
        return "{\"code\":\"" + code + "\",\"promotion_id\":" + promotionId + ",\"app\":\"RE7LA\"}";
    }

    // ── Génération QR Code → JavaFX Image ──
    public Image generateQrCodeImage(String content, int size) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 2);
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bm = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage img = MatrixToImageWriter.toBufferedImage(bm);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", baos);
            return new Image(new ByteArrayInputStream(baos.toByteArray()));
        } catch (WriterException | IOException e) {
            System.err.println("Erreur QR : " + e.getMessage());
            return null;
        }
    }

    // ── Créer et sauvegarder un PromoCode ──
    public PromoCode createPromoCode(int promotionId) {
        String code = generateUniqueCode();
        String qrContent = buildQrContent(code, promotionId);
        String sql = "INSERT INTO promo_code (promotion_id, code, qr_content) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (conn == null) return null;
            ps.setInt(1, promotionId); ps.setString(2, code); ps.setString(3, qrContent);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                PromoCode pc = new PromoCode(promotionId, code, qrContent);
                pc.setId(keys.getInt(1));
                return pc;
            }
        } catch (SQLException e) { System.err.println("Erreur createPromoCode: " + e.getMessage()); }
        return null;
    }

    // ── Valider un code saisi ──
    public boolean validateCode(String code, int promotionId) {
        String sql = "SELECT is_used FROM promo_code WHERE code=? AND promotion_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return false;
            ps.setString(1, code.trim().toUpperCase()); ps.setInt(2, promotionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return !rs.getBoolean("is_used");
        } catch (SQLException e) { System.err.println("Erreur validateCode: " + e.getMessage()); }
        return false;
    }

    // ── Marquer comme utilisé ──
    public void markCodeAsUsed(String code, int userId) {
        String sql = "UPDATE promo_code SET is_used=1, used_by=?, used_at=NOW() WHERE code=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return;
            ps.setInt(1, userId); ps.setString(2, code.trim().toUpperCase()); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Erreur markCodeAsUsed: " + e.getMessage()); }
    }

    // ── Récupérer le dernier code non-utilisé pour une promotion ──
    // Utilisé par le PDF export
    public PromoCode getByCode(int promotionId) {
        String sql = "SELECT * FROM promo_code WHERE promotion_id=? AND is_used=0 ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return null;
            ps.setInt(1, promotionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                PromoCode pc = new PromoCode();
                pc.setId(rs.getInt("id"));
                pc.setPromotionId(rs.getInt("promotion_id"));
                pc.setCode(rs.getString("code"));
                pc.setQrContent(rs.getString("qr_content"));
                pc.setUsed(rs.getBoolean("is_used"));
                return pc;
            }
        } catch (SQLException e) { System.err.println("Erreur getByCode: " + e.getMessage()); }
        return null;
    }

    // ── Récupérer par code texte ──
    public PromoCode getByCodeString(String code) {
        String sql = "SELECT * FROM promo_code WHERE code=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return null;
            ps.setString(1, code.trim().toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                PromoCode pc = new PromoCode();
                pc.setId(rs.getInt("id"));
                pc.setPromotionId(rs.getInt("promotion_id"));
                pc.setCode(rs.getString("code"));
                pc.setQrContent(rs.getString("qr_content"));
                pc.setUsed(rs.getBoolean("is_used"));
                return pc;
            }
        } catch (SQLException e) { System.err.println("Erreur getByCodeString: " + e.getMessage()); }
        return null;
    }
}
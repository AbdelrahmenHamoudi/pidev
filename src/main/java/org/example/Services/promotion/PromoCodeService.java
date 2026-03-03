package org.example.Services.promotion;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.scene.image.Image;
import org.example.Entites.promotion.PromoCode;
import org.example.Entites.user.User;
import org.example.Utils.MyBD;
import org.example.Utils.UserSession;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PromoCodeService {

    public String generateUniqueCode() {
        String uuid = UUID.randomUUID().toString().toUpperCase().replace("-", "");
        return "RE7LA-" + uuid.substring(0, 4) + "-" + uuid.substring(4, 8);
    }

    public String buildQrContent(String code, int promotionId) {
        return "{\"code\":\"" + code + "\",\"promotion_id\":" + promotionId + ",\"app\":\"RE7LA\"}";
    }

    public Image generateQrCodeImage(String content, int size) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 2);
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bm = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage img = MatrixToImageWriter.toBufferedImage(bm);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "PNG", baos);
            return new Image(new ByteArrayInputStream(baos.toByteArray()));
        } catch (WriterException | java.io.IOException e) {
            System.err.println("Erreur QR : " + e.getMessage());
            return null;
        }
    }

    public PromoCode createPromoCode(int promotionId) {
        String code = generateUniqueCode();
        String qrContent = buildQrContent(code, promotionId);
        String sql = "INSERT INTO promo_code (promotion_id, code, qr_content) VALUES (?, ?, ?)";

        try (Connection conn = MyBD.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, promotionId);
            ps.setString(2, code);
            ps.setString(3, qrContent);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                PromoCode pc = new PromoCode(promotionId, code, qrContent);
                pc.setId(keys.getInt(1));
                return pc;
            }
        } catch (SQLException e) {
            System.err.println("Erreur createPromoCode: " + e.getMessage());
        }
        return null;
    }

    public boolean validateCode(String code, int promotionId) {
        String sql = "SELECT is_used FROM promo_code WHERE code=? AND promotion_id=?";
        try (Connection conn = MyBD.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, code.trim().toUpperCase());
            ps.setInt(2, promotionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return !rs.getBoolean("is_used");
        } catch (SQLException e) {
            System.err.println("Erreur validateCode: " + e.getMessage());
        }
        return false;
    }

    public void markCodeAsUsed(String code) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String sql = "UPDATE promo_code SET is_used=1, used_by=?, used_at=NOW() WHERE code=?";
        try (Connection conn = MyBD.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, currentUser.getId());
            ps.setString(2, code.trim().toUpperCase());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur markCodeAsUsed: " + e.getMessage());
        }
    }

    public PromoCode getByPromotionId(int promotionId) {
        String sql = "SELECT pc.*, u.id as user_id, u.nom, u.prenom, u.e_mail " +
                "FROM promo_code pc " +
                "LEFT JOIN users u ON pc.used_by = u.id " +
                "WHERE pc.promotion_id=? AND pc.is_used=0 " +
                "ORDER BY pc.created_at DESC LIMIT 1";
        try (Connection conn = MyBD.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, promotionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToPromoCode(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erreur getByPromotionId: " + e.getMessage());
        }
        return null;
    }

    public PromoCode getByCodeString(String code) {
        String sql = "SELECT pc.*, u.id as user_id, u.nom, u.prenom, u.e_mail " +
                "FROM promo_code pc " +
                "LEFT JOIN users u ON pc.used_by = u.id " +
                "WHERE pc.code=?";
        try (Connection conn = MyBD.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, code.trim().toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToPromoCode(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erreur getByCodeString: " + e.getMessage());
        }
        return null;
    }

    private PromoCode mapResultSetToPromoCode(ResultSet rs) throws SQLException {
        PromoCode pc = new PromoCode();
        pc.setId(rs.getInt("id"));
        pc.setPromotionId(rs.getInt("promotion_id"));
        pc.setCode(rs.getString("code"));
        pc.setQrContent(rs.getString("qr_content"));
        pc.setUsed(rs.getBoolean("is_used"));
        pc.setUsedAt(rs.getTimestamp("used_at"));
        pc.setCreatedAt(rs.getTimestamp("created_at"));

        // Charger l'utilisateur si présent
        int userId = rs.getInt("user_id");
        if (!rs.wasNull()) {
            User user = new User();
            user.setId(userId);
            user.setNom(rs.getString("nom"));
            user.setPrenom(rs.getString("prenom"));
            user.setE_mail(rs.getString("e_mail"));
            pc.setUsedBy(user);
        }

        return pc;
    }
}
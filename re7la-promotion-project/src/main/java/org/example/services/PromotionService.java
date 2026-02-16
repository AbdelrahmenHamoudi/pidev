package org.example.services;

import org.example.models.Promotion;
import org.example.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PromotionService {

    public List<Promotion> getAll() {
        List<Promotion> promotions = new ArrayList<>();
        String sql = "SELECT * FROM promotion ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Promotion promo = extractPromotionFromResultSet(rs);
                promotions.add(promo);
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des promotions");
            e.printStackTrace();
        }

        return promotions;
    }

    public Optional<Promotion> getById(int id) {
        String sql = "SELECT * FROM promotion WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(extractPromotionFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de la promotion #" + id);
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public Promotion add(Promotion promotion) {
        // ⭐ MODIFIÉ - Ajout de prix_par_jour
        String sql = "INSERT INTO promotion (name, description, discount_percentage, " +
                "discount_fixed, start_date, end_date, is_pack, prix_par_jour) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, promotion.getName());
            pstmt.setString(2, promotion.getDescription());

            if (promotion.getDiscountPercentage() != null) {
                pstmt.setFloat(3, promotion.getDiscountPercentage());
            } else {
                pstmt.setNull(3, Types.FLOAT);
            }

            if (promotion.getDiscountFixed() != null) {
                pstmt.setFloat(4, promotion.getDiscountFixed());
            } else {
                pstmt.setNull(4, Types.FLOAT);
            }

            pstmt.setDate(5, promotion.getStartDate());
            pstmt.setDate(6, promotion.getEndDate());
            pstmt.setBoolean(7, promotion.isPack());
            pstmt.setFloat(8, promotion.getPrixParJour() != null ? promotion.getPrixParJour() : 50.0f);  // ⭐ AJOUTÉ

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    promotion.setId(generatedKeys.getInt(1));
                }
            }

            System.out.println("✅ Promotion ajoutée: " + promotion.getName());
            return promotion;

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout de la promotion");
            e.printStackTrace();
            return null;
        }
    }

    public boolean update(Promotion promotion) {
        // ⭐ MODIFIÉ - Ajout de prix_par_jour
        String sql = "UPDATE promotion SET name = ?, description = ?, " +
                "discount_percentage = ?, discount_fixed = ?, " +
                "start_date = ?, end_date = ?, is_pack = ?, prix_par_jour = ? " +
                "WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, promotion.getName());
            pstmt.setString(2, promotion.getDescription());

            if (promotion.getDiscountPercentage() != null) {
                pstmt.setFloat(3, promotion.getDiscountPercentage());
            } else {
                pstmt.setNull(3, Types.FLOAT);
            }

            if (promotion.getDiscountFixed() != null) {
                pstmt.setFloat(4, promotion.getDiscountFixed());
            } else {
                pstmt.setNull(4, Types.FLOAT);
            }

            pstmt.setDate(5, promotion.getStartDate());
            pstmt.setDate(6, promotion.getEndDate());
            pstmt.setBoolean(7, promotion.isPack());
            pstmt.setFloat(8, promotion.getPrixParJour() != null ? promotion.getPrixParJour() : 50.0f);  // ⭐ AJOUTÉ
            pstmt.setInt(9, promotion.getId());  // ⚠️ CHANGÉ DE 8 à 9

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✅ Promotion modifiée: " + promotion.getName());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification de la promotion");
            e.printStackTrace();
        }

        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM promotion WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✅ Promotion supprimée (ID: " + id + ")");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de la promotion");
            e.printStackTrace();
        }

        return false;
    }

    public List<Promotion> searchByName(String keyword) {
        List<Promotion> promotions = new ArrayList<>();
        String sql = "SELECT * FROM promotion WHERE name LIKE ? ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + keyword + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                promotions.add(extractPromotionFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche de promotions");
            e.printStackTrace();
        }

        return promotions;
    }

    public List<Promotion> getPackPromotions() {
        List<Promotion> promotions = new ArrayList<>();
        String sql = "SELECT * FROM promotion WHERE is_pack = TRUE ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                promotions.add(extractPromotionFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des packs");
            e.printStackTrace();
        }

        return promotions;
    }

    public List<Promotion> getIndividualPromotions() {
        List<Promotion> promotions = new ArrayList<>();
        String sql = "SELECT * FROM promotion WHERE is_pack = FALSE ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                promotions.add(extractPromotionFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des promotions individuelles");
            e.printStackTrace();
        }

        return promotions;
    }

    // ⭐ MODIFIÉ - Ajout extraction prix_par_jour
    private Promotion extractPromotionFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String description = rs.getString("description");

        Float discountPercentage = rs.getFloat("discount_percentage");
        if (rs.wasNull()) discountPercentage = null;

        Float discountFixed = rs.getFloat("discount_fixed");
        if (rs.wasNull()) discountFixed = null;

        Date startDate = rs.getDate("start_date");
        Date endDate = rs.getDate("end_date");
        boolean isPack = rs.getBoolean("is_pack");

        Float prixParJour = rs.getFloat("prix_par_jour");  // ⭐ AJOUTÉ
        if (rs.wasNull()) prixParJour = 50.0f;
        System.out.println("🔍 DEBUG - Promotion chargée: " + name + " | Prix par jour: " + prixParJour + " TND");  // ⭐ DEBUG

        return new Promotion(id, name, description, discountPercentage,
                discountFixed, startDate, endDate, isPack, prixParJour);  // ⭐ AJOUTÉ
    }
}
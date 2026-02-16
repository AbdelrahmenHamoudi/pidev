package org.example.services;

import org.example.models.PromotionTarget;
import org.example.models.TargetType;
import org.example.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PromotionTargetService {

    public List<PromotionTarget> getAll() {
        List<PromotionTarget> targets = new ArrayList<>();
        String sql = "SELECT * FROM promotion_target ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                targets.add(extractFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des targets");
            e.printStackTrace();
        }

        return targets;
    }

    public List<PromotionTarget> getByPromotionId(int promotionId) {
        List<PromotionTarget> targets = new ArrayList<>();
        String sql = "SELECT * FROM promotion_target WHERE promotion_id = ? ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, promotionId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                targets.add(extractFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des targets pour promotion #" + promotionId);
            e.printStackTrace();
        }

        return targets;
    }

    public PromotionTarget add(PromotionTarget target) {
        String sql = "INSERT INTO promotion_target (promotion_id, target_type, target_id) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, target.getPromotionId());
            pstmt.setString(2, target.getTargetType().name());
            pstmt.setInt(3, target.getTargetId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    target.setId(generatedKeys.getInt(1));
                }
            }

            System.out.println("✅ Target ajouté: " + target.getTargetType());
            return target;

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du target");
            e.printStackTrace();
            return null;
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM promotion_target WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✅ Target supprimé (ID: " + id + ")");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du target");
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteByPromotionId(int promotionId) {
        String sql = "DELETE FROM promotion_target WHERE promotion_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, promotionId);
            int affectedRows = pstmt.executeUpdate();

            System.out.println("✅ " + affectedRows + " target(s) supprimé(s) pour promotion #" + promotionId);
            return true;

        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression des targets");
            e.printStackTrace();
        }

        return false;
    }

    public List<PromotionTarget> getByTargetType(TargetType targetType) {
        List<PromotionTarget> targets = new ArrayList<>();
        String sql = "SELECT * FROM promotion_target WHERE target_type = ? ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, targetType.name());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                targets.add(extractFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des targets par type");
            e.printStackTrace();
        }

        return targets;
    }

    private PromotionTarget extractFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int promotionId = rs.getInt("promotion_id");
        String typeStr = rs.getString("target_type");
        int targetId = rs.getInt("target_id");

        TargetType targetType = TargetType.valueOf(typeStr);

        return new PromotionTarget(id, promotionId, targetType, targetId);
    }
}
package org.example.services;

import org.example.models.Promotion;
import org.example.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ✅ FIXED: Removed all prix_par_jour references from SQL queries.
 * Promotion no longer stores a static price — prices are dynamic via PriceCalculatorService.
 */
public class PromotionService {

    public List<Promotion> getAll() {
        List<Promotion> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return list;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM promotion ORDER BY id DESC")) {
                while (rs.next()) list.add(extract(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public Optional<Promotion> getById(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM promotion WHERE id=?")) {
            if (conn == null) return Optional.empty();
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(extract(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public Promotion add(Promotion p) {
        String sql = "INSERT INTO promotion (name, description, discount_percentage, discount_fixed, " +
                "start_date, end_date, is_pack, is_locked) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (conn == null) return null;
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            if (p.getDiscountPercentage() != null) ps.setFloat(3, p.getDiscountPercentage()); else ps.setNull(3, Types.FLOAT);
            if (p.getDiscountFixed()      != null) ps.setFloat(4, p.getDiscountFixed());      else ps.setNull(4, Types.FLOAT);
            ps.setDate(5, p.getStartDate());
            ps.setDate(6, p.getEndDate());
            ps.setBoolean(7, p.isPack());
            ps.setBoolean(8, p.isLocked());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) p.setId(keys.getInt(1));
            return p;
        } catch (SQLException e) { e.printStackTrace(); return null; }
    }

    public boolean update(Promotion p) {
        String sql = "UPDATE promotion SET name=?, description=?, discount_percentage=?, " +
                "discount_fixed=?, start_date=?, end_date=?, is_pack=?, is_locked=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return false;
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            if (p.getDiscountPercentage() != null) ps.setFloat(3, p.getDiscountPercentage()); else ps.setNull(3, Types.FLOAT);
            if (p.getDiscountFixed()      != null) ps.setFloat(4, p.getDiscountFixed());      else ps.setNull(4, Types.FLOAT);
            ps.setDate(5, p.getStartDate());
            ps.setDate(6, p.getEndDate());
            ps.setBoolean(7, p.isPack());
            ps.setBoolean(8, p.isLocked());
            ps.setInt(9, p.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean delete(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM promotion WHERE id=?")) {
            if (conn == null) return false;
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public void incrementVues(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE promotion SET nb_vues = nb_vues + 1 WHERE id=?")) {
            if (conn == null) return;
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void incrementReservations(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE promotion SET nb_reservations = nb_reservations + 1 WHERE id=?")) {
            if (conn == null) return;
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private Promotion extract(ResultSet rs) throws SQLException {
        Float pct = rs.getFloat("discount_percentage"); if (rs.wasNull()) pct = null;
        Float fix = rs.getFloat("discount_fixed");      if (rs.wasNull()) fix = null;
        boolean locked = false;
        try { locked = rs.getBoolean("is_locked"); } catch (SQLException ignored) {}
        int vues = 0, resa = 0;
        try { vues = rs.getInt("nb_vues"); }         catch (SQLException ignored) {}
        try { resa = rs.getInt("nb_reservations"); } catch (SQLException ignored) {}
        boolean aiGen = false;
        try { aiGen = rs.getBoolean("generated_by_ai"); } catch (SQLException ignored) {}

        Promotion p = new Promotion(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                pct, fix,
                rs.getDate("start_date"),
                rs.getDate("end_date"),
                rs.getBoolean("is_pack"),
                locked, vues, resa
        );
        p.setGeneratedByAi(aiGen);
        return p;
    }

    /**
     * Creates a pack promotion from an AI suggestion.
     * Writes generated_by_ai = true if the column exists, otherwise falls back gracefully.
     * Uses existing add() logic — no direct DB bypass.
     */
    public Promotion createFromAiSuggestion(org.example.models.PackSuggestionDTO dto) {
        java.time.LocalDate today = java.time.LocalDate.now();
        Promotion p = new Promotion(
                dto.getSuggestedName(),
                dto.getSuggestedDescription(),
                dto.getSuggestedDiscount(),
                null,
                java.sql.Date.valueOf(today),
                java.sql.Date.valueOf(today.plusMonths(3)),
                true   // isPack
        );
        p.setLocked(false);
        p.setGeneratedByAi(true);

        // Try INSERT with generated_by_ai column; fall back to base add() if column absent
        String sql = "INSERT INTO promotion (name, description, discount_percentage, discount_fixed, " +
                "start_date, end_date, is_pack, is_locked, generated_by_ai) VALUES (?,?,?,?,?,?,?,?,?)";
        try (java.sql.Connection conn = org.example.utils.DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setFloat(3, p.getDiscountPercentage());
            ps.setNull(4, java.sql.Types.FLOAT);
            ps.setDate(5, p.getStartDate());
            ps.setDate(6, p.getEndDate());
            ps.setBoolean(7, true);
            ps.setBoolean(8, false);
            ps.setBoolean(9, true);
            ps.executeUpdate();
            java.sql.ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) p.setId(keys.getInt(1));
            System.out.println("[PromotionService] AI pack created with generated_by_ai=true, id=" + p.getId());
            return p;
        } catch (java.sql.SQLException e) {
            // Column might not exist yet — fall back to standard add()
            System.out.println("[PromotionService] generated_by_ai column absent, using fallback add(). Msg: " + e.getMessage());
            return add(p);
        }
    }
}
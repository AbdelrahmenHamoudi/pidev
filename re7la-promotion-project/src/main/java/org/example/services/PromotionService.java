package org.example.services;

import org.example.models.Promotion;
import org.example.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        String sql = "INSERT INTO promotion (name,description,discount_percentage,discount_fixed," +
                "start_date,end_date,is_pack,prix_par_jour,is_locked) VALUES(?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (conn == null) return null;
            ps.setString(1, p.getName()); ps.setString(2, p.getDescription());
            if (p.getDiscountPercentage()!=null) ps.setFloat(3,p.getDiscountPercentage()); else ps.setNull(3,Types.FLOAT);
            if (p.getDiscountFixed()!=null) ps.setFloat(4,p.getDiscountFixed()); else ps.setNull(4,Types.FLOAT);
            ps.setDate(5,p.getStartDate()); ps.setDate(6,p.getEndDate());
            ps.setBoolean(7,p.isPack());
            ps.setFloat(8,p.getPrixParJour()!=null?p.getPrixParJour():50f);
            ps.setBoolean(9,p.isLocked());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) p.setId(keys.getInt(1));
            return p;
        } catch (SQLException e) { e.printStackTrace(); return null; }
    }

    public boolean update(Promotion p) {
        String sql = "UPDATE promotion SET name=?,description=?,discount_percentage=?,discount_fixed=?," +
                "start_date=?,end_date=?,is_pack=?,prix_par_jour=?,is_locked=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return false;
            ps.setString(1,p.getName()); ps.setString(2,p.getDescription());
            if (p.getDiscountPercentage()!=null) ps.setFloat(3,p.getDiscountPercentage()); else ps.setNull(3,Types.FLOAT);
            if (p.getDiscountFixed()!=null) ps.setFloat(4,p.getDiscountFixed()); else ps.setNull(4,Types.FLOAT);
            ps.setDate(5,p.getStartDate()); ps.setDate(6,p.getEndDate());
            ps.setBoolean(7,p.isPack());
            ps.setFloat(8,p.getPrixParJour()!=null?p.getPrixParJour():50f);
            ps.setBoolean(9,p.isLocked()); ps.setInt(10,p.getId());
            return ps.executeUpdate()>0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean delete(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM promotion WHERE id=?")) {
            if (conn == null) return false;
            ps.setInt(1,id); return ps.executeUpdate()>0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public void incrementVues(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE promotion SET nb_vues=nb_vues+1 WHERE id=?")) {
            if (conn==null) return; ps.setInt(1,id); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void incrementReservations(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE promotion SET nb_reservations=nb_reservations+1 WHERE id=?")) {
            if (conn==null) return; ps.setInt(1,id); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private Promotion extract(ResultSet rs) throws SQLException {
        Float pct = rs.getFloat("discount_percentage"); if (rs.wasNull()) pct = null;
        Float fix = rs.getFloat("discount_fixed");      if (rs.wasNull()) fix = null;
        Float ppj = rs.getFloat("prix_par_jour");       if (rs.wasNull()) ppj = 50f;
        boolean locked = false;
        try { locked = rs.getBoolean("is_locked"); } catch (SQLException ignored) {}
        int vues = 0, resa = 0;
        try { vues = rs.getInt("nb_vues"); } catch (SQLException ignored) {}
        try { resa = rs.getInt("nb_reservations"); } catch (SQLException ignored) {}
        return new Promotion(rs.getInt("id"), rs.getString("name"), rs.getString("description"),
                pct, fix, rs.getDate("start_date"), rs.getDate("end_date"),
                rs.getBoolean("is_pack"), ppj, locked, vues, resa);
    }
}
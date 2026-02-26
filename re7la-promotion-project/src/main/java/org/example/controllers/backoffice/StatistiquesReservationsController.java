package org.example.controllers.backoffice;

import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.utils.DatabaseConnection;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.SwingUtilities;
import java.awt.*;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class StatistiquesReservationsController implements Initializable {

    @FXML private StackPane chartTopPromos;
    @FXML private StackPane chartTopUsers;
    @FXML private StackPane chartLeastUsers;
    @FXML private StackPane chartTypePromo;
    @FXML private StackPane chartCombosPacks;

    // KPI labels dans le header
    @FXML private Label kpiTotalLabel;
    @FXML private Label kpiReservationsLabel;

    // ═══ Palette RE7LA (identique au CSS) ═══
    private static final Color ORANGE       = new Color(0xF3, 0x9C, 0x12); // #F39C12
    private static final Color JAUNE        = new Color(0xF7, 0xDC, 0x6F); // #F7DC6F
    private static final Color BLEU_NUIT    = new Color(0x2C, 0x3E, 0x50); // #2C3E50
    private static final Color TURQUOISE    = new Color(0x1A, 0xBC, 0x9C); // #1ABC9C
    private static final Color BG_LIGHT     = new Color(0xEE, 0xF2, 0xF6); // #eef2f6
    private static final Color WHITE        = Color.WHITE;
    private static final Color GRID_COLOR   = new Color(0xE2, 0xE8, 0xF0); // #E2E8F0
    private static final Color TEXT_COLOR   = new Color(0x64, 0x74, 0x8B); // #64748B

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            loadKpis();
            loadAllCharts();
        });
    }

    // ═══════════════════════════════════════════════════
    // KPI HEADER
    // ═══════════════════════════════════════════════════
    private void loadKpis() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return;

            // Total promotions
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM promotion")) {
                if (rs.next() && kpiTotalLabel != null)
                    kpiTotalLabel.setText(String.valueOf(rs.getInt(1)));
            }
            // Total réservations avec promo
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM reservation_promo")) {
                if (rs.next() && kpiReservationsLabel != null)
                    kpiReservationsLabel.setText(String.valueOf(rs.getInt(1)));
            }
        } catch (Exception e) {
            if (kpiTotalLabel != null) kpiTotalLabel.setText("—");
            if (kpiReservationsLabel != null) kpiReservationsLabel.setText("—");
        }
    }

    private void loadAllCharts() {
        createTopPromosChart();
        createTopUsersChart();
        createLeastUsersChart();
        createTypePromoChart();
        createComboPacksChart();
    }

    @FXML
    private void handleRefresh() {
        chartTopPromos.getChildren().clear();
        chartTopUsers.getChildren().clear();
        chartLeastUsers.getChildren().clear();
        chartTypePromo.getChildren().clear();
        chartCombosPacks.getChildren().clear();
        loadKpis();
        loadAllCharts();
    }

    @FXML
    private void handleRetour() {
        Stage stage = (Stage) chartTopPromos.getScene().getWindow();
        stage.close();
    }

    // ═══════════════════════════════════════════════════
    // HELPER : style commun pour tous les charts
    // ═══════════════════════════════════════════════════
    private void styleChart(JFreeChart chart) {
        chart.setBackgroundPaint(WHITE);
        chart.setBorderVisible(false);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 0)); // titre caché (déjà dans FXML)
        chart.getTitle().setPaint(WHITE); // invisible
        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(WHITE);
            chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 11));
            chart.getLegend().setItemPaint(BLEU_NUIT);
        }
    }

    private void styleBarPlot(CategoryPlot plot, Color barColor) {
        plot.setBackgroundPaint(WHITE);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(GRID_COLOR);
        plot.setRangeGridlineStroke(new BasicStroke(0.8f));
        plot.setDomainGridlinesVisible(false);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setSeriesPaint(0, barColor);
        renderer.setShadowVisible(false);
        renderer.setItemMargin(0.15);
        renderer.setMaximumBarWidth(0.6);

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 11));
        domainAxis.setTickLabelPaint(BLEU_NUIT);
        domainAxis.setAxisLineVisible(false);
        domainAxis.setTickMarksVisible(false);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 11));
        rangeAxis.setTickLabelPaint(TEXT_COLOR);
        rangeAxis.setAxisLineVisible(false);
        rangeAxis.setTickMarksVisible(false);
    }

    private void addChartToPane(JFreeChart chart, StackPane pane) {
        SwingUtilities.invokeLater(() -> {
            ChartPanel cp = new ChartPanel(chart);
            cp.setPreferredSize(new Dimension(600, 240));
            cp.setBackground(java.awt.Color.WHITE);
            cp.setMouseWheelEnabled(false);
            cp.setDomainZoomable(false);
            cp.setRangeZoomable(false);

            SwingNode node = new SwingNode();
            node.setContent(cp);

            Platform.runLater(() -> {
                pane.getChildren().clear();
                pane.getChildren().add(node);
            });
        });
    }

    // ═══════════════════════════════════════════════════
    // GRAPHIQUE 1 — Top Promotions (barre horizontale, ORANGE)
    // ═══════════════════════════════════════════════════
    private void createTopPromosChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                String sql = "SELECT p.name, COUNT(r.id) as nb " +
                        "FROM promotion p LEFT JOIN reservation_promo r ON p.id = r.promotion_id " +
                        "GROUP BY p.id, p.name ORDER BY nb DESC LIMIT 10";
                try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) dataset.addValue(rs.getInt("nb"), "Réservations", rs.getString("name"));
                }
            }
        } catch (Exception e) { System.err.println("Chart1: " + e.getMessage()); }

        if (dataset.getRowCount() == 0) {
            dataset.addValue(12, "Réservations", "Promo Été");
            dataset.addValue(8,  "Réservations", "Black Friday");
            dataset.addValue(5,  "Réservations", "Pack Famille");
        }

        JFreeChart chart = ChartFactory.createBarChart("", "", "", dataset, PlotOrientation.HORIZONTAL, false, false, false);
        styleChart(chart);
        styleBarPlot(chart.getCategoryPlot(), ORANGE);
        addChartToPane(chart, chartTopPromos);
    }

    // ═══════════════════════════════════════════════════
    // GRAPHIQUE 2 — Top Users actifs (barre vertical, TURQUOISE)
    // ═══════════════════════════════════════════════════
    private void createTopUsersChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                String sql = "SELECT u.nom, u.prenom, COUNT(r.id) as nb " +
                        "FROM user u LEFT JOIN reservation_promo r ON u.id = r.user_id " +
                        "GROUP BY u.id, u.nom, u.prenom ORDER BY nb DESC LIMIT 10";
                try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        dataset.addValue(rs.getInt("nb"), "Réservations", rs.getString("nom") + " " + rs.getString("prenom"));
                }
            }
        } catch (Exception e) { System.err.println("Chart2: " + e.getMessage()); }

        if (dataset.getRowCount() == 0) {
            dataset.addValue(15, "Réservations", "Ahmed B.");
            dataset.addValue(10, "Réservations", "Sana M.");
            dataset.addValue(7,  "Réservations", "Youssef K.");
        }

        JFreeChart chart = ChartFactory.createBarChart("", "", "", dataset, PlotOrientation.VERTICAL, false, false, false);
        styleChart(chart);
        styleBarPlot(chart.getCategoryPlot(), TURQUOISE);
        addChartToPane(chart, chartTopUsers);
    }

    // ═══════════════════════════════════════════════════
    // GRAPHIQUE 3 — Users moins actifs (barre vertical, BLEU NUIT)
    // ═══════════════════════════════════════════════════
    private void createLeastUsersChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                String sql = "SELECT u.nom, u.prenom, COUNT(r.id) as nb " +
                        "FROM user u LEFT JOIN reservation_promo r ON u.id = r.user_id " +
                        "GROUP BY u.id, u.nom, u.prenom ORDER BY nb ASC LIMIT 10";
                try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        dataset.addValue(rs.getInt("nb"), "Réservations", rs.getString("nom") + " " + rs.getString("prenom"));
                }
            }
        } catch (Exception e) { System.err.println("Chart3: " + e.getMessage()); }

        if (dataset.getRowCount() == 0) {
            dataset.addValue(0, "Réservations", "Ali T.");
            dataset.addValue(1, "Réservations", "Rima S.");
            dataset.addValue(2, "Réservations", "Omar F.");
        }

        JFreeChart chart = ChartFactory.createBarChart("", "", "", dataset, PlotOrientation.VERTICAL, false, false, false);
        styleChart(chart);
        styleBarPlot(chart.getCategoryPlot(), BLEU_NUIT);
        addChartToPane(chart, chartLeastUsers);
    }

    // ═══════════════════════════════════════════════════
    // GRAPHIQUE 4 — Camembert (ORANGE + TURQUOISE uniquement)
    // ═══════════════════════════════════════════════════
    private void createTypePromoChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                String sql = "SELECT CASE WHEN p.is_pack=1 THEN 'Packs' ELSE 'Individuelles' END as type, COUNT(r.id) as nb " +
                        "FROM reservation_promo r JOIN promotion p ON r.promotion_id=p.id GROUP BY p.is_pack";
                try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) dataset.setValue(rs.getString("type"), rs.getInt("nb"));
                }
            }
        } catch (Exception e) { System.err.println("Chart4: " + e.getMessage()); }

        if (dataset.getItemCount() == 0) {
            dataset.setValue("Individuelles", 60);
            dataset.setValue("Packs", 40);
        }

        JFreeChart chart = ChartFactory.createPieChart("", dataset, true, false, false);
        styleChart(chart);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(WHITE);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        // Seulement les couleurs de la charte — pas de rouge
        plot.setSectionPaint("Individuelles", ORANGE);
        plot.setSectionPaint("Packs",         TURQUOISE);
        plot.setSectionPaint(0, ORANGE);
        plot.setSectionPaint(1, TURQUOISE);
        plot.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        plot.setLabelPaint(BLEU_NUIT);
        plot.setLabelBackgroundPaint(new Color(255, 255, 255, 200));
        plot.setLabelOutlineStroke(null);
        plot.setLabelShadowPaint(null);
        plot.setLabelGap(0.02);

        if (chart.getLegend() != null) {
            chart.getLegend().setVisible(true);
        }

        addChartToPane(chart, chartTypePromo);
    }

    // ═══════════════════════════════════════════════════
    // GRAPHIQUE 5 — Combos Packs (barre horizontal, JAUNE→ORANGE gradient visuel)
    // ═══════════════════════════════════════════════════
    private void createComboPacksChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                String sql = "SELECT GROUP_CONCAT(DISTINCT pt.target_type ORDER BY pt.target_type) as combo, COUNT(r.id) as nb " +
                        "FROM promotion p " +
                        "LEFT JOIN promotion_target pt ON p.id=pt.promotion_id " +
                        "LEFT JOIN reservation_promo r ON p.id=r.promotion_id " +
                        "WHERE p.is_pack=1 GROUP BY p.id ORDER BY nb DESC";
                try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String combo = rs.getString("combo");
                        dataset.addValue(rs.getInt("nb"), "Réservations", combo != null ? combo : "Sans offres");
                    }
                }
            }
        } catch (Exception e) { System.err.println("Chart5: " + e.getMessage()); }

        if (dataset.getRowCount() == 0) {
            dataset.addValue(20, "Réservations", "Héberg+Activité+Trajet");
            dataset.addValue(14, "Réservations", "Héberg+Activité");
            dataset.addValue(9,  "Réservations", "Activité+Trajet");
            dataset.addValue(6,  "Réservations", "Trajet+Héberg");
        }

        JFreeChart chart = ChartFactory.createBarChart("", "", "", dataset, PlotOrientation.HORIZONTAL, false, false, false);
        styleChart(chart);
        styleBarPlot(chart.getCategoryPlot(), JAUNE);

        // Rendre les barres jaune foncé pour être lisibles
        BarRenderer renderer = (BarRenderer) chart.getCategoryPlot().getRenderer();
        renderer.setSeriesPaint(0, new Color(0xF3, 0x9C, 0x12)); // orange pour contraste

        addChartToPane(chart, chartCombosPacks);
    }
}
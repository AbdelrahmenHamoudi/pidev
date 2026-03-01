package org.example.controllers.backoffice;

import animatefx.animation.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.utils.AnimationHelper;
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

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class StatistiquesReservationsController implements Initializable {

    // ── Chart panes ──
    @FXML private StackPane chartTopPromos;
    @FXML private StackPane chartTopUsers;
    @FXML private StackPane chartLeastUsers;
    @FXML private StackPane chartTypePromo;
    @FXML private StackPane chartCombosPacks;

    // ── KPI labels ──
    @FXML private Label kpiTotalLabel;
    @FXML private Label kpiReservationsLabel;
    @FXML private Label kpiUsersLabel;

    // ── Containers pour animation (à binder dans FXML si présents) ──
    @FXML private HBox kpiRow;
    @FXML private VBox chartsGrid;

    // ═══ Palette RE7LA ═══
    private static final Color ORANGE    = new Color(0xF3, 0x9C, 0x12);
    private static final Color JAUNE     = new Color(0xF7, 0xDC, 0x6F);
    private static final Color BLEU_NUIT = new Color(0x2C, 0x3E, 0x50);
    private static final Color TURQUOISE = new Color(0x1A, 0xBC, 0x9C);
    private static final Color WHITE     = Color.WHITE;
    private static final Color GRID_COLOR= new Color(0xE2, 0xE8, 0xF0);
    private static final Color TEXT_COLOR= new Color(0x64, 0x74, 0x8B);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ⭐ ANIMATEFX — animation d'entrée du dashboard stats
        Platform.runLater(() -> {
            // 1. KPI labels arrivent avec BounceIn
            if (kpiRow != null) {
                List<javafx.scene.Node> kpiNodes = new ArrayList<>(kpiRow.getChildren());
                AnimationHelper.staggeredBounceIn(kpiNodes, 150);
            } else {
                // Fallback si pas de kpiRow FXML : animer les labels directement
                if (kpiTotalLabel != null)        new BounceIn(kpiTotalLabel).play();
                if (kpiReservationsLabel != null) {
                    PauseTransition pause = new PauseTransition(Duration.millis(150));
                    pause.setOnFinished(e -> new BounceIn(kpiReservationsLabel).play());
                    pause.play();
                }
            }

            // 2. Charger les données
            loadKpis();

            // 3. Charts apparaissent en cascade après 300ms
            PauseTransition delay = new PauseTransition(Duration.millis(300));
            delay.setOnFinished(e -> loadAllCharts());
            delay.play();
        });
    }

    // ═══════════════════════════════════════════════════
    // KPI — avec CountUp AnimateFX
    // ═══════════════════════════════════════════════════
    private void loadKpis() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return;

            // Total promotions
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM promotion")) {
                if (rs.next() && kpiTotalLabel != null) {
                    int total = rs.getInt(1);
                    // ⭐ CountUp 0 → total en 1.2s
                    AnimationHelper.countUp(kpiTotalLabel, 0, total, 1200);
                }
            }

            // Total réservations
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM reservation_promo")) {
                if (rs.next() && kpiReservationsLabel != null) {
                    int resa = rs.getInt(1);
                    // ⭐ CountUp avec délai 400ms
                    PauseTransition d = new PauseTransition(Duration.millis(400));
                    d.setOnFinished(e -> AnimationHelper.countUp(kpiReservationsLabel, 0, resa, 1200));
                    d.play();
                }
            }
            // Total users
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && kpiUsersLabel != null) {
                    int totalUsers = rs.getInt(1);
                    PauseTransition d2 = new PauseTransition(Duration.millis(800));
                    d2.setOnFinished(e -> AnimationHelper.countUp(kpiUsersLabel, 0, totalUsers, 1200));
                    d2.play();
                }
            }
        } catch (Exception e) {
            if (kpiTotalLabel        != null) kpiTotalLabel.setText("—");
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
        // ⭐ Flash sur chaque pane avant refresh
        for (StackPane pane : List.of(chartTopPromos, chartTopUsers, chartLeastUsers, chartTypePromo, chartCombosPacks)) {
            if (pane != null) new Flash(pane).play();
        }

        // Petit délai puis rechargement
        PauseTransition delay = new PauseTransition(Duration.millis(300));
        delay.setOnFinished(e -> {
            for (StackPane pane : List.of(chartTopPromos, chartTopUsers, chartLeastUsers, chartTypePromo, chartCombosPacks)) {
                if (pane != null) pane.getChildren().clear();
            }
            loadKpis();
            loadAllCharts();
        });
        delay.play();
    }

    @FXML
    private void handleRetour() {
        Stage stage = (Stage) chartTopPromos.getScene().getWindow();
        // ⭐ FadeOut avant fermeture
        javafx.scene.Node root = chartTopPromos.getScene().getRoot();
        AnimationHelper.fadeOut(root, stage::close);
    }

    // ═══════════════════════════════════════════════════
    // HELPER STYLE CHART
    // ═══════════════════════════════════════════════════
    private void styleChart(JFreeChart chart) {
        chart.setBackgroundPaint(WHITE);
        chart.setBorderVisible(false);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 0));
        chart.getTitle().setPaint(WHITE);
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

    /**
     * Ajoute un chart dans un StackPane avec animation ZoomIn AnimateFX.
     * @param delayMs délai avant l'animation (cascade)
     */
    private void addChartToPane(JFreeChart chart, StackPane pane, int delayMs) {
        SwingUtilities.invokeLater(() -> {
            ChartPanel cp = new ChartPanel(chart);
            cp.setPreferredSize(new Dimension(600, 240));
            cp.setBackground(WHITE);
            cp.setMouseWheelEnabled(false);
            cp.setDomainZoomable(false);
            cp.setRangeZoomable(false);

            SwingNode node = new SwingNode();
            node.setContent(cp);

            Platform.runLater(() -> {
                pane.getChildren().clear();
                pane.getChildren().add(node);

                // ⭐ AnimateFX — ZoomIn en cascade
                pane.setOpacity(0);
                PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
                pause.setOnFinished(e -> {
                    pane.setOpacity(1);
                    ZoomIn anim = new ZoomIn(pane);
                    anim.play();
                });
                pause.play();
            });
        });
    }

    // ═══════════════════════════════════════════════════
    // GRAPHIQUE 1 — Top Promotions (ORANGE) — délai 0ms
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



        JFreeChart chart = ChartFactory.createBarChart("", "", "", dataset, PlotOrientation.HORIZONTAL, false, false, false);
        styleChart(chart);
        styleBarPlot(chart.getCategoryPlot(), ORANGE);
        addChartToPane(chart, chartTopPromos, 0);
    }

    // ═══════════════════════════════════════════════════
    // GRAPHIQUE 2 — Top Users (TURQUOISE) — délai 120ms
    // ═══════════════════════════════════════════════════
    private void createTopUsersChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                String sql = "SELECT u.nom, u.prenom, COUNT(r.id) as nb " +
                        "FROM users u LEFT JOIN reservation_promo r ON u.id = r.user_id " +
                        "GROUP BY u.id, u.nom, u.prenom ORDER BY nb DESC LIMIT 10";
                try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        dataset.addValue(rs.getInt("nb"), "Réservations", rs.getString("nom") + " " + rs.getString("prenom"));
                }
            }
        } catch (Exception e) { System.err.println("Chart2: " + e.getMessage()); }



        JFreeChart chart = ChartFactory.createBarChart("", "", "", dataset, PlotOrientation.VERTICAL, false, false, false);
        styleChart(chart);
        styleBarPlot(chart.getCategoryPlot(), TURQUOISE);
        addChartToPane(chart, chartTopUsers, 120);
    }

    // ═══════════════════════════════════════════════════
    // GRAPHIQUE 3 — Users moins actifs (BLEU NUIT) — délai 240ms
    // ═══════════════════════════════════════════════════
    private void createLeastUsersChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                String sql = "SELECT u.nom, u.prenom, COUNT(r.id) as nb " +
                        "FROM users u LEFT JOIN reservation_promo r ON u.id = r.user_id " +
                        "GROUP BY u.id, u.nom, u.prenom ORDER BY nb ASC LIMIT 10";
                try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        dataset.addValue(rs.getInt("nb"), "Réservations", rs.getString("nom") + " " + rs.getString("prenom"));
                }
            }
        } catch (Exception e) { System.err.println("Chart3: " + e.getMessage()); }



        JFreeChart chart = ChartFactory.createBarChart("", "", "", dataset, PlotOrientation.VERTICAL, false, false, false);
        styleChart(chart);
        styleBarPlot(chart.getCategoryPlot(), BLEU_NUIT);
        addChartToPane(chart, chartLeastUsers, 240);
    }

    // ═══════════════════════════════════════════════════
    // GRAPHIQUE 4 — Camembert ORANGE/TURQUOISE — délai 360ms
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



        JFreeChart chart = ChartFactory.createPieChart("", dataset, true, false, false);
        styleChart(chart);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(WHITE);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        plot.setSectionPaint("Individuelles", ORANGE);
        plot.setSectionPaint("Packs", TURQUOISE);
        plot.setSectionPaint(0, ORANGE);
        plot.setSectionPaint(1, TURQUOISE);
        plot.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        plot.setLabelPaint(BLEU_NUIT);
        plot.setLabelBackgroundPaint(new Color(255, 255, 255, 200));
        plot.setLabelOutlineStroke(null);
        plot.setLabelShadowPaint(null);
        plot.setLabelGap(0.02);
        if (chart.getLegend() != null) chart.getLegend().setVisible(true);

        addChartToPane(chart, chartTypePromo, 360);
    }

    // ═══════════════════════════════════════════════════
    // GRAPHIQUE 5 — Combos Packs (ORANGE) — délai 480ms
    // ═══════════════════════════════════════════════════
    private void createComboPacksChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                String sql = "SELECT GROUP_CONCAT(DISTINCT pt.target_type ORDER BY pt.target_type) as combo, COUNT(r.id) as nb " +
                        "FROM promotion p " +
                        "LEFT JOIN promotion_target pt ON p.id=pt.promotion_id " +
                        "LEFT JOIN reservation_promo r  ON p.id=r.promotion_id " +
                        "WHERE p.is_pack=1 GROUP BY p.id ORDER BY nb DESC";
                try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String combo = rs.getString("combo");
                        dataset.addValue(rs.getInt("nb"), "Réservations", combo != null ? combo : "Sans offres");
                    }
                }
            }
        } catch (Exception e) { System.err.println("Chart5: " + e.getMessage()); }



        JFreeChart chart = ChartFactory.createBarChart("", "", "", dataset, PlotOrientation.HORIZONTAL, false, false, false);
        styleChart(chart);
        styleBarPlot(chart.getCategoryPlot(), ORANGE);

        addChartToPane(chart, chartCombosPacks, 480);
    }
}
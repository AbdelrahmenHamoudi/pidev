package org.example.Controllers.Trajet;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.example.Entites.trajet.StatutVoiture;
import org.example.Entites.trajet.Trajet;
import org.example.Entites.trajet.Voiture;
import org.example.Services.trajet.TrajetCRUD;
import org.example.Services.trajet.VoitureCRUD;


import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class StatistiquesController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(StatistiquesController.class.getName());

    @FXML private WebView chartsWebView;
    @FXML private Label kpiVoituresLabel;
    @FXML private Label kpiTrajetsLabel;
    @FXML private Label kpiRevenusLabel;
    @FXML private Label kpiDispoLabel;

    private final VoitureCRUD voitureCRUD = new VoitureCRUD();
    private final TrajetCRUD  trajetCRUD  = new TrajetCRUD();

    private WebEngine webEngine;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        webEngine = chartsWebView.getEngine();

        try {
            List<Voiture> voitures = voitureCRUD.afficherh();
            List<Trajet>  trajets  = trajetCRUD.afficherh();

            // KPI Labels
            int nbVoitures    = voitures.size();
            int nbTrajets     = trajets.size();
            int nbDisponibles = (int) voitures.stream().filter(Voiture::isDisponibilite).count();
            double revenus    = trajets.stream().mapToDouble(t ->
                    t.getIdVoiture() != null ? t.getIdVoiture().getPrixKm() * t.getDistanceKm() : 0
            ).sum();

            if (kpiVoituresLabel != null) kpiVoituresLabel.setText(String.valueOf(nbVoitures));
            if (kpiTrajetsLabel  != null) kpiTrajetsLabel.setText(String.valueOf(nbTrajets));
            if (kpiRevenusLabel  != null) kpiRevenusLabel.setText(String.format("%.0f DT", revenus));
            if (kpiDispoLabel    != null) kpiDispoLabel.setText(String.valueOf(nbDisponibles));

            String chartData = buildChartData(voitures, trajets);
            String html      = buildDashboardHTML(chartData);
            webEngine.loadContent(html);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement stats", e);
            webEngine.loadContent(buildErrorHTML(e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CONSTRUCTION DES DONNÉES JSON
    // ═══════════════════════════════════════════════════════════════════

    private String buildChartData(List<Voiture> voitures, List<Trajet> trajets) {

        // 1. Voitures par marque (donut)
        Map<String, Long> parMarque = voitures.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getMarque() != null ? v.getMarque() : "Autre",
                        Collectors.counting()));
        String marqueLabels = parMarque.keySet().stream().map(k -> "\"" + k + "\"").collect(Collectors.joining(","));
        String marqueData   = parMarque.values().stream().map(String::valueOf).collect(Collectors.joining(","));

        // 2. Disponibilité
        long dispo   = voitures.stream().filter(Voiture::isDisponibilite).count();
        long indispo  = voitures.size() - dispo;

        // 3. Prix/km par voiture (top 8)
        List<Voiture> topVoitures = voitures.stream()
                .sorted(Comparator.comparing(Voiture::getPrixKm).reversed())
                .limit(8).collect(Collectors.toList());
        String prixLabels = topVoitures.stream()
                .map(v -> "\"" + v.getMarque() + " " + v.getModele() + "\"").collect(Collectors.joining(","));
        String prixData   = topVoitures.stream()
                .map(v -> String.format("%.2f", v.getPrixKm())).collect(Collectors.joining(","));

        // 4. Trajets par statut
        long reserve    = trajets.stream().filter(t -> t.getStatut() == StatutVoiture.Reserve).count();
        long enCours    = trajets.stream().filter(t -> t.getStatut() == StatutVoiture.En_cours).count();
        long disponible = trajets.stream().filter(t -> t.getStatut() == StatutVoiture.Disponible).count();

        // 5. Revenus par mois (6 derniers mois)
        Map<String, Double> revenusMois = new LinkedHashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.FRENCH);
        for (int i = 5; i >= 0; i--) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.MONTH, -i);
            revenusMois.put(sdf.format(c.getTime()), 0.0);
        }
        for (Trajet t : trajets) {
            if (t.getDateReservation() != null && t.getIdVoiture() != null) {
                String mois = sdf.format(t.getDateReservation());
                if (revenusMois.containsKey(mois)) {
                    revenusMois.merge(mois, (double) t.getIdVoiture().getPrixKm() * t.getDistanceKm(), Double::sum);                }
            }
        }
        String moisLabels = revenusMois.keySet().stream().map(k -> "\"" + k + "\"").collect(Collectors.joining(","));
        String moisData   = revenusMois.values().stream().map(v -> String.format("%.2f", v)).collect(Collectors.joining(","));

        // 6. Top villes départ
        Map<String, Long> parVille = trajets.stream()
                .filter(t -> t.getPointDepart() != null)
                .collect(Collectors.groupingBy(Trajet::getPointDepart, Collectors.counting()));
        List<Map.Entry<String, Long>> topVilles = parVille.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(6).collect(Collectors.toList());
        String villeLabels = topVilles.stream().map(e -> "\"" + e.getKey() + "\"").collect(Collectors.joining(","));
        String villeData   = topVilles.stream().map(e -> String.valueOf(e.getValue())).collect(Collectors.joining(","));

        // 7. Capacité par nb places
        Map<Integer, Long> parPlaces = voitures.stream()
                .collect(Collectors.groupingBy(Voiture::getNb_places, Collectors.counting()));
        List<Map.Entry<Integer, Long>> sortedPlaces = new ArrayList<>(parPlaces.entrySet());
        sortedPlaces.sort(Map.Entry.comparingByKey());
        String placesLabels = sortedPlaces.stream().map(e -> "\"" + e.getKey() + " places\"").collect(Collectors.joining(","));
        String placesData   = sortedPlaces.stream().map(e -> String.valueOf(e.getValue())).collect(Collectors.joining(","));

        return String.format(
                "{ marqueLabels:[%s], marqueData:[%s], dispo:%d, indispo:%d, " +
                        "prixLabels:[%s], prixData:[%s], reserve:%d, enCours:%d, disponible:%d, " +
                        "moisLabels:[%s], moisData:[%s], villeLabels:[%s], villeData:[%s], " +
                        "placesLabels:[%s], placesData:[%s] }",
                marqueLabels, marqueData, dispo, indispo,
                prixLabels, prixData, reserve, enCours, disponible,
                moisLabels, moisData, villeLabels, villeData,
                placesLabels, placesData
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    //  HTML — TOUS LES EMOJIS SUPPRIMÉS, REMPLACÉS PAR DES ICÔNES CSS
    // ═══════════════════════════════════════════════════════════════════

    private String buildDashboardHTML(String data) {
        return buildRawHTML().replace("__CHART_DATA__", data);
    }

    private String buildRawHTML() {
        return "<!DOCTYPE html>\n"
                + "<html lang=\"fr\">\n"
                + "<head>\n"
                + "<meta charset=\"UTF-8\"/>\n"
                + "<script src=\"https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js\"></script>\n"
                + "<style>\n"
                + ":root {\n"
                + "  --bg:#0f172a; --card:#1e293b; --border:#334155;\n"
                + "  --text:#e2e8f0; --muted:#64748b;\n"
                + "  --blue:#3b82f6; --green:#10b981; --amber:#f59e0b;\n"
                + "  --red:#ef4444; --violet:#8b5cf6; --cyan:#06b6d4;\n"
                + "}\n"
                + "* { margin:0; padding:0; box-sizing:border-box; }\n"
                + "html,body { background:var(--bg); color:var(--text);\n"
                + "  font-family:'Segoe UI',system-ui,sans-serif; overflow-y:auto; }\n"
                + "body { padding:24px; }\n"
                + ".grid { display:grid; grid-template-columns:repeat(2,1fr); gap:20px; }\n"
                + ".grid-3 { display:grid; grid-template-columns:repeat(3,1fr); gap:20px; margin-bottom:20px; }\n"
                + ".chart-card {\n"
                + "  background:var(--card); border:1px solid var(--border);\n"
                + "  border-radius:16px; padding:20px; position:relative;\n"
                + "  transition:transform .2s,box-shadow .2s;\n"
                + "}\n"
                + ".chart-card:hover { transform:translateY(-2px); box-shadow:0 8px 32px rgba(0,0,0,0.3); }\n"
                + ".chart-card.wide { grid-column:span 2; }\n"
                + ".card-header { display:flex; align-items:center; gap:10px; margin-bottom:18px; }\n"

                // Icones CSS (cercles colorés avec lettre) — ZERO emoji
                + ".card-icon {\n"
                + "  width:36px; height:36px; border-radius:10px;\n"
                + "  display:flex; align-items:center; justify-content:center;\n"
                + "  font-size:15px; font-weight:800; flex-shrink:0;\n"
                + "}\n"
                + ".icon-blue   { background:rgba(59,130,246,0.2);  color:#3b82f6; }\n"
                + ".icon-green  { background:rgba(16,185,129,0.2);  color:#10b981; }\n"
                + ".icon-amber  { background:rgba(245,158,11,0.2);  color:#f59e0b; }\n"
                + ".icon-violet { background:rgba(139,92,246,0.2);  color:#8b5cf6; }\n"
                + ".icon-cyan   { background:rgba(6,182,212,0.2);   color:#06b6d4; }\n"
                + ".icon-red    { background:rgba(239,68,68,0.2);   color:#ef4444; }\n"

                + ".card-title { font-size:14px; font-weight:700; color:var(--text); letter-spacing:.3px; }\n"
                + ".card-subtitle { font-size:11px; color:var(--muted); margin-top:2px; }\n"
                + "canvas { max-height:240px; }\n"
                + ".canvas-sm { max-height:200px; }\n"

                // Section titre — sans emoji
                + ".section-title {\n"
                + "  font-size:18px; font-weight:800; color:var(--text);\n"
                + "  margin-bottom:20px; letter-spacing:-.3px;\n"
                + "  display:flex; align-items:center; gap:10px;\n"
                + "  padding:10px 16px;\n"
                + "  background:var(--card); border:1px solid var(--border);\n"
                + "  border-radius:12px; margin-bottom:20px;\n"
                + "}\n"
                + ".section-title .bar {\n"
                + "  width:4px; height:24px; border-radius:2px; flex-shrink:0;\n"
                + "  background:linear-gradient(to bottom, #3b82f6, #8b5cf6);\n"
                + "}\n"
                + "</style>\n"
                + "</head>\n"
                + "<body>\n"

                // === SECTION VOITURES ===
                + "<div class=\"section-title\"><div class=\"bar\"></div>Statistiques Voitures</div>\n"

                + "<div class=\"grid-3\">\n"

                // 1. Donut — Marques
                + "<div class=\"chart-card\">\n"
                + "  <div class=\"card-header\">\n"
                + "    <div class=\"card-icon icon-blue\">V</div>\n"
                + "    <div><div class=\"card-title\">Voitures par Marque</div>"
                +         "<div class=\"card-subtitle\">Repartition du parc</div></div>\n"
                + "  </div>\n"
                + "  <canvas id=\"chartMarque\" class=\"canvas-sm\"></canvas>\n"
                + "</div>\n"

                // 2. Pie — Disponibilité
                + "<div class=\"chart-card\">\n"
                + "  <div class=\"card-header\">\n"
                + "    <div class=\"card-icon icon-green\">D</div>\n"
                + "    <div><div class=\"card-title\">Disponibilite</div>"
                +         "<div class=\"card-subtitle\">Disponibles vs Indisponibles</div></div>\n"
                + "  </div>\n"
                + "  <canvas id=\"chartDispo\" class=\"canvas-sm\"></canvas>\n"
                + "</div>\n"

                // 3. Bar — Places
                + "<div class=\"chart-card\">\n"
                + "  <div class=\"card-header\">\n"
                + "    <div class=\"card-icon icon-violet\">P</div>\n"
                + "    <div><div class=\"card-title\">Capacite du Parc</div>"
                +         "<div class=\"card-subtitle\">Voitures par nombre de places</div></div>\n"
                + "  </div>\n"
                + "  <canvas id=\"chartPlaces\" class=\"canvas-sm\"></canvas>\n"
                + "</div>\n"

                + "</div>\n"

                // 4. Bar horizontal — Prix/km (full width)
                + "<div class=\"chart-card wide\" style=\"margin-bottom:20px;\">\n"
                + "  <div class=\"card-header\">\n"
                + "    <div class=\"card-icon icon-amber\">$</div>\n"
                + "    <div><div class=\"card-title\">Prix / km par Vehicule</div>"
                +         "<div class=\"card-subtitle\">Top 8 voitures - tarif en DT/km</div></div>\n"
                + "  </div>\n"
                + "  <canvas id=\"chartPrix\" style=\"max-height:220px;\"></canvas>\n"
                + "</div>\n"

                // === SECTION TRAJETS ===
                + "<div class=\"section-title\"><div class=\"bar\"></div>Statistiques Trajets</div>\n"

                + "<div class=\"grid\">\n"

                // 5. Line — Revenus (full width)
                + "<div class=\"chart-card wide\">\n"
                + "  <div class=\"card-header\">\n"
                + "    <div class=\"card-icon icon-green\">R</div>\n"
                + "    <div><div class=\"card-title\">Revenus sur 6 Mois</div>"
                +         "<div class=\"card-subtitle\">Chiffre d'affaires estime (DT)</div></div>\n"
                + "  </div>\n"
                + "  <canvas id=\"chartRevenus\" style=\"max-height:200px;\"></canvas>\n"
                + "</div>\n"

                // 6. Doughnut — Statuts
                + "<div class=\"chart-card\">\n"
                + "  <div class=\"card-header\">\n"
                + "    <div class=\"card-icon icon-cyan\">S</div>\n"
                + "    <div><div class=\"card-title\">Statuts des Trajets</div>"
                +         "<div class=\"card-subtitle\">Reserve - En cours - Disponible</div></div>\n"
                + "  </div>\n"
                + "  <canvas id=\"chartStatuts\" class=\"canvas-sm\"></canvas>\n"
                + "</div>\n"

                // 7. Bar — Villes
                + "<div class=\"chart-card\">\n"
                + "  <div class=\"card-header\">\n"
                + "    <div class=\"card-icon icon-red\">T</div>\n"
                + "    <div><div class=\"card-title\">Top Villes de Depart</div>"
                +         "<div class=\"card-subtitle\">Nombre de trajets par ville</div></div>\n"
                + "  </div>\n"
                + "  <canvas id=\"chartVilles\" class=\"canvas-sm\"></canvas>\n"
                + "</div>\n"

                + "</div>\n"

                // === JAVASCRIPT CHART.JS ===
                + "<script>\n"
                + "const D = __CHART_DATA__;\n"
                + "const COLORS = ['#3b82f6','#10b981','#f59e0b','#ef4444','#8b5cf6','#06b6d4','#f97316','#84cc16','#ec4899','#14b8a6'];\n"
                + "const BASE = { responsive:true, maintainAspectRatio:true,\n"
                + "  plugins:{\n"
                + "    legend:{ labels:{color:'#94a3b8',font:{size:11},padding:12} },\n"
                + "    tooltip:{ backgroundColor:'#1e293b',borderColor:'#334155',borderWidth:1,\n"
                + "      titleColor:'#e2e8f0',bodyColor:'#94a3b8',padding:10 }\n"
                + "  }\n"
                + "};\n"

                // Chart 1 — Donut marques
                + "new Chart(document.getElementById('chartMarque'),{\n"
                + "  type:'doughnut',\n"
                + "  data:{ labels:D.marqueLabels, datasets:[{ data:D.marqueData,\n"
                + "    backgroundColor:COLORS, borderColor:'#0f172a', borderWidth:3, hoverOffset:8 }] },\n"
                + "  options:{...BASE, cutout:'65%',\n"
                + "    plugins:{...BASE.plugins, legend:{position:'bottom',labels:{color:'#94a3b8',font:{size:10},padding:8}}}}\n"
                + "});\n"

                // Chart 2 — Pie dispo
                + "new Chart(document.getElementById('chartDispo'),{\n"
                + "  type:'pie',\n"
                + "  data:{ labels:['Disponibles','Indisponibles'],\n"
                + "    datasets:[{ data:[D.dispo,D.indispo],\n"
                + "      backgroundColor:['#10b981','#ef4444'], borderColor:'#0f172a', borderWidth:3, hoverOffset:8 }] },\n"
                + "  options:{...BASE, plugins:{...BASE.plugins, legend:{position:'bottom',labels:{color:'#94a3b8',font:{size:11},padding:10}}}}\n"
                + "});\n"

                // Chart 3 — Bar places
                + "new Chart(document.getElementById('chartPlaces'),{\n"
                + "  type:'bar',\n"
                + "  data:{ labels:D.placesLabels, datasets:[{ label:'Nb voitures', data:D.placesData,\n"
                + "    backgroundColor:'rgba(139,92,246,0.7)', borderColor:'#8b5cf6',\n"
                + "    borderWidth:2, borderRadius:8, borderSkipped:false }] },\n"
                + "  options:{...BASE,\n"
                + "    scales:{\n"
                + "      x:{ticks:{color:'#64748b',font:{size:10}},grid:{color:'rgba(51,65,85,0.5)'}},\n"
                + "      y:{ticks:{color:'#64748b',font:{size:10}},grid:{color:'rgba(51,65,85,0.5)'},beginAtZero:true}\n"
                + "    },\n"
                + "    plugins:{...BASE.plugins, legend:{display:false}}\n"
                + "  }\n"
                + "});\n"

                // Chart 4 — Bar horizontal prix
                + "new Chart(document.getElementById('chartPrix'),{\n"
                + "  type:'bar',\n"
                + "  data:{ labels:D.prixLabels, datasets:[{ label:'Prix DT/km', data:D.prixData,\n"
                + "    backgroundColor:D.prixData.map(function(v,i){return 'rgba(245,158,11,'+(0.4+i*0.07)+')';}),\n"
                + "    borderColor:'#f59e0b', borderWidth:1, borderRadius:6, borderSkipped:false }] },\n"
                + "  options:{...BASE, indexAxis:'y',\n"
                + "    scales:{\n"
                + "      x:{ticks:{color:'#64748b',font:{size:10}},grid:{color:'rgba(51,65,85,0.4)'},beginAtZero:true},\n"
                + "      y:{ticks:{color:'#e2e8f0',font:{size:11,weight:'600'}},grid:{display:false}}\n"
                + "    },\n"
                + "    plugins:{...BASE.plugins, legend:{display:false}}\n"
                + "  }\n"
                + "});\n"

                // Chart 5 — Line revenus
                + "new Chart(document.getElementById('chartRevenus'),{\n"
                + "  type:'line',\n"
                + "  data:{ labels:D.moisLabels, datasets:[{ label:'Revenus (DT)', data:D.moisData,\n"
                + "    borderColor:'#10b981', backgroundColor:'rgba(16,185,129,0.08)',\n"
                + "    borderWidth:2.5, pointBackgroundColor:'#10b981',\n"
                + "    pointBorderColor:'#0f172a', pointBorderWidth:2, pointRadius:5,\n"
                + "    fill:true, tension:0.4 }] },\n"
                + "  options:{...BASE,\n"
                + "    scales:{\n"
                + "      x:{ticks:{color:'#64748b',font:{size:10}},grid:{color:'rgba(51,65,85,0.4)'}},\n"
                + "      y:{ticks:{color:'#64748b',font:{size:10}},grid:{color:'rgba(51,65,85,0.4)'},beginAtZero:true}\n"
                + "    }\n"
                + "  }\n"
                + "});\n"

                // Chart 6 — Doughnut statuts
                + "new Chart(document.getElementById('chartStatuts'),{\n"
                + "  type:'doughnut',\n"
                + "  data:{ labels:['Reserve','En cours','Disponible'],\n"
                + "    datasets:[{ data:[D.reserve,D.enCours,D.disponible],\n"
                + "      backgroundColor:['#ef4444','#f59e0b','#10b981'],\n"
                + "      borderColor:'#0f172a', borderWidth:3, hoverOffset:8 }] },\n"
                + "  options:{...BASE, cutout:'60%',\n"
                + "    plugins:{...BASE.plugins, legend:{position:'bottom',labels:{color:'#94a3b8',font:{size:10},padding:8}}}}\n"
                + "});\n"

                // Chart 7 — Bar villes
                + "new Chart(document.getElementById('chartVilles'),{\n"
                + "  type:'bar',\n"
                + "  data:{ labels:D.villeLabels, datasets:[{ label:'Trajets', data:D.villeData,\n"
                + "    backgroundColor:COLORS.slice(0,D.villeData.length).map(function(c){return c+'cc';}),\n"
                + "    borderColor:COLORS.slice(0,D.villeData.length),\n"
                + "    borderWidth:2, borderRadius:8, borderSkipped:false }] },\n"
                + "  options:{...BASE,\n"
                + "    scales:{\n"
                + "      x:{ticks:{color:'#64748b',font:{size:10}},grid:{display:false}},\n"
                + "      y:{ticks:{color:'#64748b',font:{size:10}},grid:{color:'rgba(51,65,85,0.4)'},beginAtZero:true}\n"
                + "    },\n"
                + "    plugins:{...BASE.plugins, legend:{display:false}}\n"
                + "  }\n"
                + "});\n"
                + "</script>\n"
                + "</body>\n"
                + "</html>";
    }

    private String buildErrorHTML(String msg) {
        return "<html><body style='background:#0f172a;color:#ef4444;font-family:monospace;padding:20px'>"
                + "<h3>Erreur chargement statistiques</h3><p>" + msg + "</p></body></html>";
    }

    @FXML
    private void fermer() {
        ((Stage) chartsWebView.getScene().getWindow()).close();
    }
}

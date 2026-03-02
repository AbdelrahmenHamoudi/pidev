package org.example.Services.hebergement;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * ✅ Service Groq AI – Génération de rapports administrateur
 *
 * =====================================================
 *  API utilisée : GROQ (100% GRATUITE, pas Grok/xAI)
 *  Site        : https://console.groq.com
 *  Clé API     : https://console.groq.com/keys
 * =====================================================
 *
 * 🎁 Free tier Groq :
 *   - 14 400 requêtes / jour
 *   - 6 000 tokens / minute
 *   - Aucune carte bancaire requise
 *
 * 🔧 Modèles gratuits disponibles :
 *   - llama-3.3-70b-versatile  (très performant, recommandé)
 *   - llama-3.1-8b-instant     (rapide, léger)
 *   - mixtral-8x7b-32768       (contexte long)
 *   - gemma2-9b-it             (Google open-source)
 *
 * 📦 Dépendance Maven (pom.xml) :
 * <dependency>
 *     <groupId>com.google.code.gson</groupId>
 *     <artifactId>gson</artifactId>
 *     <version>2.10.1</version>
 * </dependency>
 */
public class GeminiReportService {

    // ✅ 1. Allez sur https://console.groq.com/keys
    // ✅ 2. Cliquez "Create API Key"
    // ✅ 3. Collez votre clé ici (commence par "gsk_...")
    private static final String API_KEY = "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";

    // URL de l'API Groq (compatible OpenAI)
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    // Modèles essayés dans l'ordre (fallback automatique)
    private static final String[] MODELS = {
            "llama-3.3-70b-versatile",   // 🥇 Meilleur rapport qualité/quota
            "llama-3.1-8b-instant",      // 🥈 Très rapide, quota séparé
            "mixtral-8x7b-32768",        // 🥉 Contexte long
            "gemma2-9b-it"               // 🏅 Fallback final
    };

    private final HttpClient httpClient;

    public GeminiReportService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Génère un rapport mensuel intelligent via l'API Groq.
     * Essaie automatiquement plusieurs modèles en cas d'erreur.
     *
     * @param stats Statistiques mensuelles à analyser
     * @return Rapport textuel généré par l'IA
     */
    public String genererRapportMensuel(RapportStats stats) throws Exception {
        String prompt = construirePrompt(stats);
        Exception derniereErreur = null;

        for (String model : MODELS) {
            System.out.println("🤖 Essai modèle Groq : " + model);

            try {
                // Construction du body JSON (format OpenAI compatible)
                JsonObject message = new JsonObject();
                message.addProperty("role", "user");
                message.addProperty("content", prompt);

                JsonArray messages = new JsonArray();
                messages.add(message);

                JsonObject body = new JsonObject();
                body.addProperty("model", model);
                body.add("messages", messages);
                body.addProperty("max_tokens", 1500);
                body.addProperty("temperature", 0.7);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + API_KEY)
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .timeout(Duration.ofSeconds(60))
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString());

                int status = response.statusCode();

                if (status == 200) {
                    System.out.println("✅ Succès avec : " + model);
                    return extraireTexteReponse(response.body());
                }

                // 429 = quota dépassé → essayer modèle suivant
                if (status == 429) {
                    System.out.println("⚠️ Quota 429 sur " + model + " → modèle suivant...");
                    derniereErreur = new Exception(
                            "Quota temporairement dépassé. Réessayez dans quelques secondes.\n" +
                                    "Groq réinitialise le quota toutes les minutes.");
                    Thread.sleep(2000); // attendre 2s avant de réessayer
                    continue;
                }

                // Autre erreur
                throw new Exception("Erreur API Groq [" + status + "] :\n" + response.body());

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().startsWith("Erreur API Groq")) {
                    throw e;
                }
                derniereErreur = e;
            }
        }

        throw new Exception(derniereErreur != null
                ? derniereErreur.getMessage()
                : "Impossible de contacter l'API Groq. Vérifiez votre connexion et votre clé API.");
    }

    /**
     * Construit le prompt envoyé à l'IA
     */
    private String construirePrompt(RapportStats stats) {
        return String.format(
                "Tu es un expert en analyse hôtelière et touristique en Tunisie.\n" +
                        "Génère un rapport mensuel professionnel en français à partir de ces données.\n\n" +
                        "=== DONNÉES %s %d ===\n" +
                        "- Réservations totales : %d\n" +
                        "- Revenus totaux : %.0f DT\n" +
                        "- Taux d'occupation : %.1f%%\n" +
                        "- Hébergements actifs : %d\n" +
                        "- Meilleur hébergement : %s (%d réservations)\n" +
                        "- Moins performant : %s (%d réservations)\n" +
                        "- Ville leader : %s (%.0f%% des réservations)\n" +
                        "- Ville moins demandée : %s\n" +
                        "- Type le plus réservé : %s\n" +
                        "- Prix moyen/nuit : %.0f DT\n" +
                        "- Capacité moyenne : %d personnes\n\n" +
                        "Génère un rapport structuré avec :\n" +
                        "1. 📊 Résumé exécutif (3-4 phrases)\n" +
                        "2. 📈 Analyse détaillée des performances\n" +
                        "3. 🔍 Points forts et points faibles\n" +
                        "4. 💡 4 recommandations stratégiques chiffrées\n" +
                        "5. 🔮 Prévisions pour le mois prochain\n\n" +
                        "Sois précis, professionnel et orienté action.",
                stats.getMois(), stats.getAnnee(),
                stats.getTotalReservations(),
                stats.getRevenusTotaux(),
                stats.getTauxOccupation(),
                stats.getNombreHebergements(),
                stats.getMeilleurHebergement(), stats.getMeilleurHebergementNbRes(),
                stats.getPireHebergement(), stats.getPireHebergementNbRes(),
                stats.getVillePlusDemandee(), stats.getPourcentageVillePrincipale(),
                stats.getVilleMoinsDemandee(),
                stats.getTypePlusReserve(),
                stats.getPrixMoyen(),
                stats.getCapaciteMoyenne()
        );
    }

    /**
     * Extrait le texte de la réponse JSON Groq (format OpenAI)
     */
    private String extraireTexteReponse(String jsonResponse) throws Exception {
        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            return root
                    .getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        } catch (Exception e) {
            throw new Exception("Impossible de parser la réponse Groq :\n" + jsonResponse);
        }
    }

    // =========================================================
    // ✅ INNER CLASS : Conteneur de statistiques mensuelles
    // =========================================================
    public static class RapportStats {
        private String mois;
        private int annee;
        private int totalReservations;
        private double revenusTotaux;
        private double tauxOccupation;
        private int nombreHebergements;
        private String meilleurHebergement;
        private int meilleurHebergementNbRes;
        private String pireHebergement;
        private int pireHebergementNbRes;
        private String villePlusDemandee;
        private double pourcentageVillePrincipale;
        private String villeMoinsDemandee;
        private String typePlusReserve;
        private double prixMoyen;
        private int capaciteMoyenne;

        public RapportStats() {}

        // Getters
        public String getMois()                       { return mois; }
        public int getAnnee()                         { return annee; }
        public int getTotalReservations()             { return totalReservations; }
        public double getRevenusTotaux()              { return revenusTotaux; }
        public double getTauxOccupation()             { return tauxOccupation; }
        public int getNombreHebergements()            { return nombreHebergements; }
        public String getMeilleurHebergement()        { return meilleurHebergement; }
        public int getMeilleurHebergementNbRes()      { return meilleurHebergementNbRes; }
        public String getPireHebergement()            { return pireHebergement; }
        public int getPireHebergementNbRes()          { return pireHebergementNbRes; }
        public String getVillePlusDemandee()          { return villePlusDemandee; }
        public double getPourcentageVillePrincipale() { return pourcentageVillePrincipale; }
        public String getVilleMoinsDemandee()         { return villeMoinsDemandee; }
        public String getTypePlusReserve()            { return typePlusReserve; }
        public double getPrixMoyen()                  { return prixMoyen; }
        public int getCapaciteMoyenne()               { return capaciteMoyenne; }

        // Setters (builder style)
        public RapportStats mois(String v)                       { this.mois = v; return this; }
        public RapportStats annee(int v)                         { this.annee = v; return this; }
        public RapportStats totalReservations(int v)             { this.totalReservations = v; return this; }
        public RapportStats revenusTotaux(double v)              { this.revenusTotaux = v; return this; }
        public RapportStats tauxOccupation(double v)             { this.tauxOccupation = v; return this; }
        public RapportStats nombreHebergements(int v)            { this.nombreHebergements = v; return this; }
        public RapportStats meilleurHebergement(String v)        { this.meilleurHebergement = v; return this; }
        public RapportStats meilleurHebergementNbRes(int v)      { this.meilleurHebergementNbRes = v; return this; }
        public RapportStats pireHebergement(String v)            { this.pireHebergement = v; return this; }
        public RapportStats pireHebergementNbRes(int v)          { this.pireHebergementNbRes = v; return this; }
        public RapportStats villePlusDemandee(String v)          { this.villePlusDemandee = v; return this; }
        public RapportStats pourcentageVillePrincipale(double v) { this.pourcentageVillePrincipale = v; return this; }
        public RapportStats villeMoinsDemandee(String v)         { this.villeMoinsDemandee = v; return this; }
        public RapportStats typePlusReserve(String v)            { this.typePlusReserve = v; return this; }
        public RapportStats prixMoyen(double v)                  { this.prixMoyen = v; return this; }
        public RapportStats capaciteMoyenne(int v)               { this.capaciteMoyenne = v; return this; }
    }
}
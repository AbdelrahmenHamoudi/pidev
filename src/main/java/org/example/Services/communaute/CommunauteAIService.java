package org.example.Services.communaute;

import org.example.Entites.communaute.Commentaire;
import org.example.Entites.communaute.Publication;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 🤖 CommunauteAIService
 * ──────────────────────────────────────────────────────────────────
 * Provider : SambaNova  (free via HuggingFace routing)
 * Model    : meta-llama/Llama-3.1-8B-Instruct:sambanova
 * Endpoint : https://router.huggingface.co/v1/chat/completions
 *
 * Why the switch from Mistral-7B-v0.2?
 *   → fireworks-ai : model_not_supported (400)
 *   → Mistral-7B-v0.2 is no longer hosted by any free provider on HF
 *   → Llama-3.1-8B-Instruct on SambaNova is free, fast, and
 *     confirmed working through the HF router as of 2025.
 *
 * SETUP:
 *   1. Go to https://huggingface.co/settings/tokens
 *   2. Create a fine-grained token
 *   3. Enable permission: "Make calls to Inference Providers"
 *   4. Paste your token below replacing hf_YOUR_TOKEN_HERE
 * ──────────────────────────────────────────────────────────────────
 */
public class CommunauteAIService {

    // ── ⚠️  Your HuggingFace token ────────────────────────────────
    private static final String HF_API_KEY = "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";

    // ── Router endpoint ────────────────────────────────────────────
    private static final String HF_ENDPOINT =
            "https://router.huggingface.co/v1/chat/completions";

    // ── Model : Llama-3.1-8B via SambaNova (free, confirmed working)
    private static final String MODEL =
            "meta-llama/Llama-3.1-8B-Instruct:sambanova";

    // ── Shared HTTP client ─────────────────────────────────────────
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    // ═══════════════════════════════════════════════════════════════
    //  1️⃣  PUBLICATION SUMMARY
    // ═══════════════════════════════════════════════════════════════
    public String summarisePublication(Publication p) {
        return callRouter(
                "Tu es un assistant de voyage intelligent pour l'application RE7LA. " +
                        "Réponds toujours en français.",
                "Résume cette publication de voyage en 2-3 phrases courtes et engageantes.\n\n" +
                        "Type : " + p.getTypeCible() + "\n" +
                        "Date : " + p.getDateCreation() + "\n" +
                        "Description : " + p.getDescriptionP()
        );
    }

    // ═══════════════════════════════════════════════════════════════
    //  2️⃣  COMMENT SENTIMENT ANALYSIS
    // ═══════════════════════════════════════════════════════════════
    public String analyseCommentSentiment(Publication p, List<Commentaire> comments) {
        if (comments.isEmpty())
            return "Aucun commentaire à analyser pour cette publication.";

        String commentBlock = comments.stream()
                .map(c -> "- " + c.getContenuC())
                .collect(Collectors.joining("\n"));

        return callRouter(
                "Tu es un analyste de réseaux sociaux. Réponds toujours en français.",
                "Analyse le sentiment des commentaires suivants pour la publication : \"" +
                        truncate(p.getDescriptionP(), 80) + "\".\n\n" +
                        "Commentaires :\n" + commentBlock + "\n\n" +
                        "Donne :\n" +
                        "• Le pourcentage approximatif positifs / négatifs / neutres\n" +
                        "• Le sentiment dominant\n" +
                        "• Un exemple positif et un négatif si disponibles\n" +
                        "Sois concis (4-6 lignes maximum)."
        );
    }

    // ═══════════════════════════════════════════════════════════════
    //  3️⃣  COMMUNITY Q&A
    // ═══════════════════════════════════════════════════════════════
    public String answerCommunityQuestion(String userQuestion,
                                          List<Publication> publications,
                                          Map<Integer, Integer> commentCounts,
                                          Map<Integer, Integer> reactionTotals) {
        StringBuilder ctx = new StringBuilder("Publications disponibles :\n\n");
        for (Publication p : publications) {
            ctx.append("• Auteur=").append(safeUserName(p))
                    .append(" | Date=").append(p.getDateCreation())
                    .append(" | Type=").append(p.getTypeCible())
                    .append(" | Réactions=").append(reactionTotals.getOrDefault(p.getIdPublication(), 0))
                    .append(" | Commentaires=").append(commentCounts.getOrDefault(p.getIdPublication(), 0))
                    .append("\n  Description: ").append(truncate(p.getDescriptionP(), 100))
                    .append("\n\n");
        }

        return callRouter(
                "Tu es un assistant pour l'application de voyage RE7LA. " +
                        "Utilise UNIQUEMENT les données fournies. Ne jamais inventer. Réponds en français.",
                ctx + "Question : " + userQuestion + "\nRéponse (maximum 5 lignes) :"
        );
    }

    // ═══════════════════════════════════════════════════════════════
    //  4️⃣  SMART COMMENT SUGGESTION
    // ═══════════════════════════════════════════════════════════════
    public String suggestBetterComment(String rawComment, Publication p) {
        return callRouter(
                "Tu es un assistant de rédaction pour une application de voyage. " +
                        "Améliore les commentaires pour les rendre professionnels. Réponds en français.",
                "Publication : \"" + truncate(p.getDescriptionP(), 80) + "\"\n" +
                        "Brouillon : \"" + rawComment + "\"\n\n" +
                        "Écris une version améliorée en 1-2 phrases. " +
                        "Réponds UNIQUEMENT avec la version améliorée, sans introduction."
        );
    }

    // ═══════════════════════════════════════════════════════════════
    //  HTTP CALLER
    // ═══════════════════════════════════════════════════════════════
    private String callRouter(String systemPrompt, String userMessage) {
        String body = "{"
                + "\"model\":\"" + MODEL + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escapeJson(systemPrompt) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escapeJson(userMessage) + "\"}"
                + "],"
                + "\"max_tokens\":400,"
                + "\"temperature\":0.7"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HF_ENDPOINT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + HF_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(60))
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            if (status == 401)
                return "❌ Token invalide — vérifiez votre clé HuggingFace et la permission « Make calls to Inference Providers ».";
            if (status == 429)
                return "⏳ Limite de requêtes atteinte. Patientez quelques secondes et réessayez.";
            if (status == 503)
                return "⏳ Service temporairement indisponible. Réessayez dans un moment.";
            if (status != 200)
                return "❌ Erreur API (" + status + ") : " + response.body();

            return parseResponse(response.body());

        } catch (IOException | InterruptedException e) {
            return "❌ Erreur de connexion : " + e.getMessage();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  RESPONSE PARSER
    //  Standard OpenAI format: {"choices":[{"message":{"content":"..."}}]}
    //  We skip to "choices" first to avoid matching "content" in the
    //  echoed request messages.
    // ═══════════════════════════════════════════════════════════════
    private String parseResponse(String json) {
        try {
            // Find "choices" array first
            int choicesIdx = json.indexOf("\"choices\"");
            if (choicesIdx == -1) return extractError(json);

            // Then find "content" only after that position
            String marker = "\"content\":\"";
            int contentIdx = json.indexOf(marker, choicesIdx);
            if (contentIdx == -1) return extractError(json);

            int start = contentIdx + marker.length();

            StringBuilder sb = new StringBuilder();
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    switch (next) {
                        case '"':  sb.append('"');  i++; break;
                        case 'n':  sb.append('\n'); i++; break;
                        case 't':  sb.append('\t'); i++; break;
                        case '\\': sb.append('\\'); i++; break;
                        case 'r':                   i++; break;
                        default:   sb.append(c);
                    }
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                }
            }

            String result = sb.toString().trim();
            return result.isEmpty() ? "⚠️ Le modèle n'a pas retourné de réponse." : result;

        } catch (Exception e) {
            return "⚠️ Erreur lecture réponse : " + e.getMessage();
        }
    }

    private String extractError(String json) {
        try {
            if (json.contains("\"error\"")) {
                int s = json.indexOf("\"message\":\"") + 11;
                int e = json.indexOf("\"", s);
                if (s > 10 && e > s) return "⚠️ Erreur API : " + json.substring(s, e);
            }
        } catch (Exception ignored) {}
        return "⚠️ Réponse inattendue. Brute : " + truncate(json, 200);
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
    }

    private String safeUserName(Publication p) {
        try { return p.getId_utilisateur().getNom() + " " + p.getId_utilisateur().getPrenom(); }
        catch (Exception e) { return "Inconnu"; }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}

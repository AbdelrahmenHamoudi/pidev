package org.example.Services.promotion;

import com.google.gson.*;
import org.example.Entites.activite.Activite;
import org.example.Entites.hebergement.Hebergement;
import org.example.Entites.promotion.PackSuggestionDTO;
import org.example.Entites.promotion.TargetType;
import org.example.Utils.MyBD;

import java.net.URI;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.*;
import java.time.Duration;
import java.util.*;

/**
 * ════════════════════════════════════════════════════════════
 *  AI Smart Pack Suggestion Service
 *  Uses REAL Claude AI (Anthropic API) for name, description,
 *  discount, and confidence — not templates.
 *
 *  Flow:
 *   1. Read co-occurrence data from DB (read-only)
 *   2. Build structured context for Claude
 *   3. Call claude-sonnet-4-20250514 via Anthropic API
 *   4. Parse AI JSON response → PackSuggestionDTO list
 *   5. Graceful fallback if API unreachable / key not set
 * ════════════════════════════════════════════════════════════
 */
public class PackSuggestionService {

    // ── Anthropic API ────────────────────────────────────────
    // IMPORTANT: Replace with your real Anthropic API key
    private static final String API_KEY = System.getenv("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");
    private static final String API_URL   = "https://api.anthropic.com/v1/messages";
    private static final String API_MODEL = "claude-sonnet-4-20250514";
    private static final int    MAX_TOKENS = 2048;

    // ── Min co-reservations to consider a pair ───────────────
    private static final int MIN_FREQUENCY = 1;

    // ── Singleton ────────────────────────────────────────────
    private static PackSuggestionService instance;
    public static PackSuggestionService getInstance() {
        if (instance == null) instance = new PackSuggestionService();
        return instance;
    }
    private PackSuggestionService() {}

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15)).build();
    private final Gson gson = new GsonBuilder().create();

    // ════════════════════════════════════════════════════════════
    //  MAIN ENTRY
    // ════════════════════════════════════════════════════════════

    public List<PackSuggestionDTO> generateSuggestions() {
        Offresservice offres = Offresservice.getInstance();

        // 1. Build co-occurrence map from DB
        Map<Integer, List<OfferRef>> promoOffers = loadPromoOffers();
        Map<Integer, List<Integer>>  userPromos  = loadUserPromos();
        Map<String, PairCount>       coMap       = new LinkedHashMap<>();

        for (List<OfferRef> refs : promoOffers.values())
            addPairs(refs, coMap);

        for (List<Integer> promoIds : userPromos.values()) {
            if (promoIds.size() < 2) continue;
            List<OfferRef> all = new ArrayList<>();
            for (int pid : promoIds)
                all.addAll(promoOffers.getOrDefault(pid, List.of()));
            for (int i = 0; i < all.size(); i++)
                for (int j = i + 1; j < all.size(); j++) {
                    OfferRef a = all.get(i), b = all.get(j);
                    if (a.promoId == b.promoId || skipPair(a.type, b.type)) continue;
                    coMap.computeIfAbsent(pairKey(a, b), k -> new PairCount(a, b)).count++;
                }
        }

        // 2. Resolve names, lieu, prices
        List<RawPair> rawPairs = new ArrayList<>();
        for (PairCount pc : coMap.values()) {
            if (pc.count < MIN_FREQUENCY) continue;
            String n1 = offres.getOfferName(pc.a.type, pc.a.id);
            String n2 = offres.getOfferName(pc.b.type, pc.b.id);
            rawPairs.add(new RawPair(
                    pc.a.id, pc.b.id, pc.a.type, pc.b.type,
                    n1 != null ? n1 : typeLabel(pc.a.type) + " #" + pc.a.id,
                    n2 != null ? n2 : typeLabel(pc.b.type) + " #" + pc.b.id,
                    extractLieu(offres, pc.a, pc.b),
                    pc.count,
                    basePrice(offres, pc.a.type, pc.a.id) + basePrice(offres, pc.b.type, pc.b.id)
            ));
        }

        if (rawPairs.isEmpty()) return Collections.emptyList();

        // 3. Ask Claude
        return callClaudeApi(rawPairs);
    }

    // ════════════════════════════════════════════════════════════
    //  CLAUDE API CALL
    // ════════════════════════════════════════════════════════════

    private List<PackSuggestionDTO> callClaudeApi(List<RawPair> pairs) {
        // Build JSON data for the prompt
        JsonArray data = new JsonArray();
        for (RawPair p : pairs) {
            JsonObject o = new JsonObject();
            o.addProperty("offer1_id",          p.id1);
            o.addProperty("offer2_id",          p.id2);
            o.addProperty("offer1_type",        p.type1.name());
            o.addProperty("offer2_type",        p.type2.name());
            o.addProperty("offer1_name",        p.name1);
            o.addProperty("offer2_name",        p.name2);
            o.addProperty("location_hint",      p.lieu != null ? p.lieu : "");
            o.addProperty("co_reservations",    p.frequency);
            o.addProperty("total_price_tnd",    Math.round(p.totalPrice * 100.0) / 100.0);
            data.add(o);
        }

        String prompt =
                "You are an AI assistant for RE7LA, a Tunisian travel booking platform.\n" +
                        "Analyse these co-reserved offer pairs and generate professional pack promotions.\n\n" +
                        "OFFER TYPES: HEBERGEMENT=hotel, ACTIVITE=activity/experience, VOITURE=car rental\n\n" +
                        "DATA:\n" + gson.toJson(data) + "\n\n" +
                        "For EACH item, return ONE JSON object in a JSON array with EXACTLY these fields:\n" +
                        "  \"offer1_id\": (copy from input)\n" +
                        "  \"offer2_id\": (copy from input)\n" +
                        "  \"pack_name\": creative French pack name max 55 chars, include city if detectable\n" +
                        "  \"description\": marketing French description 2-3 sentences, warm tone, mention both offers\n" +
                        "  \"city\": city name from location_hint or offer names, or \"Tunisie\" if unknown\n" +
                        "  \"discount_percent\": float 5.0-20.0 — higher for frequent/complementary pairs\n" +
                        "  \"confidence_score\": float 0-100 — your confidence this is a valuable pack\n" +
                        "  \"ai_reasoning\": one French sentence explaining why this pack is a good idea\n\n" +
                        "RULES:\n" +
                        "- Hotel+Activity combos get highest confidence (80-95)\n" +
                        "- Hotel+Car gets medium confidence (60-80)\n" +
                        "- Activity+Car gets lower confidence (40-70)\n" +
                        "- More co_reservations = higher discount and confidence\n" +
                        "- Pack names must be tourism-quality, not generic\n" +
                        "- Return ONLY valid JSON array, no markdown, no extra text\n\n" +
                        "Example: [{\"offer1_id\":1,\"offer2_id\":2,\"pack_name\":\"Djerba — Aventure & Soleil\",\"description\":\"Vivez une expérience...\",\"city\":\"Djerba\",\"discount_percent\":12.5,\"confidence_score\":88.0,\"ai_reasoning\":\"Association hébergement + activité très recherchée.\"}]";

        JsonObject body = new JsonObject();
        body.addProperty("model",      API_MODEL);
        body.addProperty("max_tokens", MAX_TOKENS);
        JsonArray msgs = new JsonArray();
        JsonObject msg = new JsonObject();
        msg.addProperty("role",    "user");
        msg.addProperty("content", prompt);
        msgs.add(msg);
        body.add("messages", msgs);

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type",     "application/json")
                    .header("x-api-key",         API_KEY)
                    .header("anthropic-version", "2023-06-01")
                    .timeout(Duration.ofSeconds(60))
                    .POST(BodyPublishers.ofString(gson.toJson(body)))
                    .build();

            HttpResponse<String> resp = http.send(req, BodyHandlers.ofString());
            System.out.println("[PackSuggestionService] HTTP " + resp.statusCode());

            if (resp.statusCode() != 200) {
                System.err.println("[PackSuggestionService] API error body: " + resp.body());
                return fallback(pairs);
            }
            return parseResponse(resp.body(), pairs);

        } catch (Exception e) {
            System.err.println("[PackSuggestionService] Request failed: " + e.getMessage());
            return fallback(pairs);
        }
    }

    // ════════════════════════════════════════════════════════════
    //  PARSE RESPONSE
    // ════════════════════════════════════════════════════════════

    private List<PackSuggestionDTO> parseResponse(String body, List<RawPair> rawPairs) {
        try {
            JsonObject root    = JsonParser.parseString(body).getAsJsonObject();
            JsonArray  content = root.getAsJsonArray("content");
            if (content == null || content.isEmpty()) return fallback(rawPairs);

            String text = content.get(0).getAsJsonObject().get("text").getAsString().trim();
            // Strip markdown fences if present
            text = text.replaceAll("(?s)^```[a-z]*\\s*", "").replaceAll("```\\s*$", "").trim();

            System.out.println("[PackSuggestionService] AI text preview: "
                    + text.substring(0, Math.min(300, text.length())));

            JsonArray aiArr = JsonParser.parseString(text).getAsJsonArray();

            // Build lookup by id pair
            Map<String, RawPair> lookup = new LinkedHashMap<>();
            for (RawPair rp : rawPairs) {
                lookup.put(rp.id1 + ":" + rp.id2, rp);
                lookup.put(rp.id2 + ":" + rp.id1, rp);
            }

            List<PackSuggestionDTO> results = new ArrayList<>();
            for (JsonElement el : aiArr) {
                JsonObject ai = el.getAsJsonObject();
                int id1 = ai.get("offer1_id").getAsInt();
                int id2 = ai.get("offer2_id").getAsInt();

                RawPair rp = lookup.get(id1 + ":" + id2);
                if (rp == null) continue;

                PackSuggestionDTO dto = new PackSuggestionDTO();
                dto.setOffer1Id(rp.id1);        dto.setOffer2Id(rp.id2);
                dto.setOffer1Type(rp.type1);    dto.setOffer2Type(rp.type2);
                dto.setOffer1Name(rp.name1);    dto.setOffer2Name(rp.name2);
                dto.setFrequency(rp.frequency);
                dto.setEstimatedTotalPrice(rp.totalPrice);
                dto.setSuggestedName(       getStr(ai, "pack_name",        rp.name1 + " + " + rp.name2));
                dto.setSuggestedDescription(getStr(ai, "description",      "Pack RE7LA."));
                dto.setDetectedCity(        getStr(ai, "city",             "Tunisie"));
                dto.setSuggestedDiscount(   getFloat(ai, "discount_percent", 10f));
                dto.setConfidenceScore(     getDouble(ai, "confidence_score", 50.0));
                dto.setAiReasoning(         getStr(ai, "ai_reasoning",     ""));
                results.add(dto);
            }

            results.sort(Comparator.comparingDouble(PackSuggestionDTO::getConfidenceScore).reversed());
            return results;

        } catch (Exception e) {
            System.err.println("[PackSuggestionService] Parse failed: " + e.getMessage());
            return fallback(rawPairs);
        }
    }

    // ════════════════════════════════════════════════════════════
    //  FALLBACK — when API unavailable or key not configured
    // ════════════════════════════════════════════════════════════

    private List<PackSuggestionDTO> fallback(List<RawPair> pairs) {
        System.out.println("[PackSuggestionService] Fallback mode (no Claude API)");
        double maxF = pairs.stream().mapToInt(p -> p.frequency).max().orElse(1);
        List<PackSuggestionDTO> results = new ArrayList<>();
        for (RawPair rp : pairs) {
            PackSuggestionDTO dto = new PackSuggestionDTO();
            dto.setOffer1Id(rp.id1);     dto.setOffer2Id(rp.id2);
            dto.setOffer1Type(rp.type1); dto.setOffer2Type(rp.type2);
            dto.setOffer1Name(rp.name1); dto.setOffer2Name(rp.name2);
            dto.setFrequency(rp.frequency);
            dto.setEstimatedTotalPrice(rp.totalPrice);
            dto.setDetectedCity(rp.lieu != null && !rp.lieu.isBlank() ? rp.lieu : "Tunisie");
            dto.setSuggestedName(fallbackName(rp));
            dto.setSuggestedDescription("Combinaison " + rp.name1 + " + " + rp.name2
                    + ". Pack suggéré par analyse des habitudes de réservation RE7LA.");
            dto.setSuggestedDiscount((float) Math.min(20, Math.max(5, rp.frequency * 1.5)));
            dto.setConfidenceScore(Math.min(100, (rp.frequency / maxF) * 100));
            dto.setAiReasoning("⚠️ Hors-ligne — configurez votre clé API Anthropic");
            results.add(dto);
        }
        results.sort(Comparator.comparingDouble(PackSuggestionDTO::getConfidenceScore).reversed());
        return results;
    }

    private String fallbackName(RawPair rp) {
        String pre = rp.lieu != null && !rp.lieu.isBlank() ? rp.lieu + " — " : "";
        if (has(rp.type1, rp.type2, TargetType.HEBERGEMENT) && has(rp.type1, rp.type2, TargetType.ACTIVITE))
            return pre + "Aventure & Séjour";
        if (has(rp.type1, rp.type2, TargetType.HEBERGEMENT) && has(rp.type1, rp.type2, TargetType.VOITURE))
            return pre + "Séjour & Mobilité";
        if (has(rp.type1, rp.type2, TargetType.ACTIVITE) && has(rp.type1, rp.type2, TargetType.VOITURE))
            return pre + "Découverte & Transport";
        return pre + "Pack Complet";
    }

    // ════════════════════════════════════════════════════════════
    //  LIEU EXTRACTION
    // ════════════════════════════════════════════════════════════

    private String extractLieu(Offresservice svc, OfferRef a, OfferRef b) {
        for (OfferRef r : List.of(a, b)) {
            if (r.type == TargetType.ACTIVITE) {
                Activite act = svc.getActiviteById(r.id);
                if (act != null && act.getLieu() != null && !act.getLieu().isBlank())
                    return act.getLieu().trim();
            }
        }
        for (OfferRef r : List.of(a, b)) {
            if (r.type == TargetType.HEBERGEMENT) {
                Hebergement h = svc.getHebergementById(r.id);
                if (h != null && h.getTitre() != null && !h.getTitre().isBlank())
                    return h.getTitre().trim();
            }
        }
        return "";
    }

    // ════════════════════════════════════════════════════════════
    //  DB READS
    // ════════════════════════════════════════════════════════════

    private Map<Integer, List<OfferRef>> loadPromoOffers() {
        Map<Integer, List<OfferRef>> map = new LinkedHashMap<>();
        try (Connection c = MyBD.getInstance().getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT promotion_id, target_type, target_id FROM promotion_target ORDER BY promotion_id")) {
            while (rs.next()) {
                int pid = rs.getInt("promotion_id");
                TargetType t = TargetType.valueOf(rs.getString("target_type").toUpperCase());
                map.computeIfAbsent(pid, k -> new ArrayList<>())
                        .add(new OfferRef(pid, t, rs.getInt("target_id")));
            }
        } catch (Exception e) {
            System.err.println("[PackSuggestionService] " + e.getMessage());
        }
        return map;
    }

    private Map<Integer, List<Integer>> loadUserPromos() {
        Map<Integer, List<Integer>> map = new LinkedHashMap<>();
        try (Connection c = MyBD.getInstance().getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT DISTINCT user_id, promotion_id FROM reservation_promo")) {
            while (rs.next())
                map.computeIfAbsent(rs.getInt("user_id"), k -> new ArrayList<>())
                        .add(rs.getInt("promotion_id"));
        } catch (Exception e) {
            System.err.println("[PackSuggestionService] " + e.getMessage());
        }
        return map;
    }

    // ════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════

    private void addPairs(List<OfferRef> refs, Map<String, PairCount> map) {
        for (int i = 0; i < refs.size(); i++)
            for (int j = i + 1; j < refs.size(); j++) {
                OfferRef a = refs.get(i), b = refs.get(j);
                if (!skipPair(a.type, b.type))
                    map.computeIfAbsent(pairKey(a, b), k -> new PairCount(a, b)).count++;
            }
    }

    private boolean skipPair(TargetType t1, TargetType t2) {
        return t1 == TargetType.VOITURE && t2 == TargetType.VOITURE;
    }
    private boolean has(TargetType t1, TargetType t2, TargetType tgt) { return t1 == tgt || t2 == tgt; }

    private float basePrice(Offresservice svc, TargetType type, int id) {
        return switch (type) {
            case HEBERGEMENT -> svc.getPrixParNuit(id);
            case ACTIVITE    -> svc.getPrixParPersonne(id);
            case VOITURE     -> svc.getPrixKm(id) * 100f;
        };
    }

    private String typeLabel(TargetType t) {
        return switch (t) {
            case HEBERGEMENT -> "Hébergement";
            case ACTIVITE -> "Activité";
            case VOITURE -> "Voiture";
        };
    }

    private String pairKey(OfferRef a, OfferRef b) {
        String ka = a.type.name() + "_" + a.id, kb = b.type.name() + "_" + b.id;
        return ka.compareTo(kb) <= 0 ? ka + "::" + kb : kb + "::" + ka;
    }

    private String  getStr(JsonObject o, String k, String d)    {
        try { return o.get(k).getAsString(); } catch(Exception e){ return d; }
    }
    private float   getFloat(JsonObject o, String k, float d)   {
        try { return o.get(k).getAsFloat(); } catch(Exception e){ return d; }
    }
    private double  getDouble(JsonObject o, String k, double d) {
        try { return o.get(k).getAsDouble(); } catch(Exception e){ return d; }
    }

    // ── Inner classes ────────────────────────────────────────
    private static class OfferRef {
        final int promoId; final TargetType type; final int id;
        OfferRef(int p, TargetType t, int i) { promoId=p; type=t; id=i; }
    }
    private static class PairCount {
        final OfferRef a, b; int count=0;
        PairCount(OfferRef a, OfferRef b) { this.a=a; this.b=b; }
    }
    private static class RawPair {
        final int id1, id2; final TargetType type1, type2;
        final String name1, name2, lieu; final int frequency; final float totalPrice;
        RawPair(int i1, int i2, TargetType t1, TargetType t2,
                String n1, String n2, String l, int f, float p) {
            id1=i1; id2=i2; type1=t1; type2=t2; name1=n1; name2=n2; lieu=l; frequency=f; totalPrice=p;
        }
    }
}
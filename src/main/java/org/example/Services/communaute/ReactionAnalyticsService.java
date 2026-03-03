package org.example.Services.communaute;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * 🏆 ReactionAnalyticsService — local JSON file persistence
 * ──────────────────────────────────────────────────────────
 * Reactions are saved to:
 *   [user home]/.re7la/reactions.json
 *
 * No database table needed. No external library needed.
 * Pure Java — reads/writes a simple JSON file.
 *
 * File format:
 * {
 *   "12": { "LIKE": 3, "LOVE": 1 },
 *   "7":  { "WOW": 2, "SAD": 1 }
 * }
 */
public class ReactionAnalyticsService {

    // ─────────────────────────────────────────────
    //  Emoji enum
    // ─────────────────────────────────────────────
    public enum Reaction {
        LIKE("👍", "Like"),
        LOVE("❤️", "Love"),
        WOW("😮",  "Wow"),
        SAD("😢",  "Sad");

        public final String emoji;
        public final String label;

        Reaction(String emoji, String label) {
            this.emoji = emoji;
            this.label = label;
        }
    }

    // ─────────────────────────────────────────────
    //  Storage file path
    //  e.g. C:\Users\John\.re7la\reactions.json
    //  or   /home/john/.re7la/reactions.json
    // ─────────────────────────────────────────────
    private static final Path STORAGE_DIR  =
            Paths.get(System.getProperty("user.home"), ".re7la");
    private static final Path STORAGE_FILE =
            STORAGE_DIR.resolve("reactions.json");

    // ─────────────────────────────────────────────
    //  Singleton
    // ─────────────────────────────────────────────
    private static ReactionAnalyticsService instance;

    public static ReactionAnalyticsService getInstance() {
        if (instance == null) instance = new ReactionAnalyticsService();
        return instance;
    }

    // ─────────────────────────────────────────────
    //  In-memory cache
    //  Key: publicationId → Map<Reaction, count>
    // ─────────────────────────────────────────────
    private final Map<Integer, Map<Reaction, Integer>> cache = new HashMap<>();

    private ReactionAnalyticsService() {
        loadFromFile();
    }

    // ─────────────────────────────────────────────
    //  ADD REACTION — update cache + save file
    // ─────────────────────────────────────────────
    public void addReaction(int publicationId, Reaction reaction) {
        cache.computeIfAbsent(publicationId, k -> new EnumMap<>(Reaction.class))
             .merge(reaction, 1, Integer::sum);
        saveToFile();
    }

    // ─────────────────────────────────────────────
    //  GETTERS  (all read from cache only)
    // ─────────────────────────────────────────────
    public int getReactionCount(int publicationId, Reaction reaction) {
        return cache.getOrDefault(publicationId, Collections.emptyMap())
                    .getOrDefault(reaction, 0);
    }

    public int getTotalReactions(int publicationId) {
        return cache.getOrDefault(publicationId, Collections.emptyMap())
                    .values().stream().mapToInt(Integer::intValue).sum();
    }

    public Map<Reaction, Integer> getAllReactions(int publicationId) {
        return cache.getOrDefault(publicationId, new EnumMap<>(Reaction.class));
    }

    public Optional<Reaction> getDominantReaction(int publicationId) {
        return cache.getOrDefault(publicationId, Collections.emptyMap())
                    .entrySet().stream()
                    .max(Comparator.comparingInt(Map.Entry::getValue))
                    .map(Map.Entry::getKey);
    }

    // ─────────────────────────────────────────────
    //  ANALYTICS
    // ─────────────────────────────────────────────
    public int getMostReactedPublicationId() {
        return cache.entrySet().stream()
                .max(Comparator.comparingInt(e ->
                        e.getValue().values().stream().mapToInt(Integer::intValue).sum()))
                .map(Map.Entry::getKey)
                .orElse(-1);
    }

    public int getTrendingPublicationId(Map<Integer, Integer> commentCounts) {
        Set<Integer> allIds = new HashSet<>(cache.keySet());
        allIds.addAll(commentCounts.keySet());
        return allIds.stream()
                .max(Comparator.comparingInt(id ->
                        getTotalReactions(id) * 2 + commentCounts.getOrDefault(id, 0)))
                .orElse(-1);
    }

    public void clearReactions(int publicationId) {
        cache.remove(publicationId);
        saveToFile();
    }

    // ─────────────────────────────────────────────
    //  LOAD from JSON file
    // ─────────────────────────────────────────────
    private void loadFromFile() {
        if (!Files.exists(STORAGE_FILE)) return;

        try {
            String json = new String(Files.readAllBytes(STORAGE_FILE));
            json = json.trim();

            // Parse: { "pubId": { "REACTION": count, ... }, ... }
            if (!json.startsWith("{") || json.length() < 2) return;

            // Strip outer braces
            String inner = json.substring(1, json.length() - 1).trim();
            if (inner.isEmpty()) return;

            // Split into "pubId": { ... } blocks
            for (String pubBlock : splitTopLevelEntries(inner)) {
                pubBlock = pubBlock.trim();
                int colonIdx = pubBlock.indexOf(':');
                if (colonIdx == -1) continue;

                String pubIdStr = pubBlock.substring(0, colonIdx)
                        .replace("\"", "").trim();
                String reactionsJson = pubBlock.substring(colonIdx + 1).trim();

                int pubId;
                try { pubId = Integer.parseInt(pubIdStr); }
                catch (NumberFormatException e) { continue; }

                // Strip inner braces
                if (!reactionsJson.startsWith("{")) continue;
                String reactInner = reactionsJson
                        .substring(1, reactionsJson.length() - 1).trim();
                if (reactInner.isEmpty()) continue;

                Map<Reaction, Integer> reactionMap = new EnumMap<>(Reaction.class);

                for (String entry : reactInner.split(",")) {
                    entry = entry.trim();
                    String[] parts = entry.split(":");
                    if (parts.length != 2) continue;

                    String reactionName = parts[0].replace("\"", "").trim();
                    String countStr     = parts[1].replace("\"", "").trim();

                    try {
                        Reaction r = Reaction.valueOf(reactionName);
                        int count  = Integer.parseInt(countStr);
                        reactionMap.put(r, count);
                    } catch (Exception ignored) {}
                }

                if (!reactionMap.isEmpty()) {
                    cache.put(pubId, reactionMap);
                }
            }

        } catch (IOException e) {
            System.err.println("⚠️ ReactionAnalyticsService: could not read reactions file — " + e.getMessage());
        }
    }

    /**
     * Splits a JSON object's top-level "key": { ... } entries correctly,
     * respecting nested braces so we don't split inside a reaction map.
     */
    private List<String> splitTopLevelEntries(String s) {
        List<String> entries = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            else if (c == ',' && depth == 0) {
                entries.add(s.substring(start, i));
                start = i + 1;
            }
        }
        if (start < s.length()) {
            entries.add(s.substring(start));
        }
        return entries;
    }

    // ─────────────────────────────────────────────
    //  SAVE to JSON file
    // ─────────────────────────────────────────────
    private void saveToFile() {
        try {
            // Create directory if it doesn't exist
            if (!Files.exists(STORAGE_DIR)) {
                Files.createDirectories(STORAGE_DIR);
            }

            // Build JSON manually — no external library needed
            StringBuilder json = new StringBuilder("{\n");
            boolean firstPub = true;

            for (Map.Entry<Integer, Map<Reaction, Integer>> pubEntry : cache.entrySet()) {
                if (!firstPub) json.append(",\n");
                firstPub = false;

                json.append("  \"").append(pubEntry.getKey()).append("\": {");

                boolean firstReaction = true;
                for (Map.Entry<Reaction, Integer> reactionEntry : pubEntry.getValue().entrySet()) {
                    if (!firstReaction) json.append(", ");
                    firstReaction = false;
                    json.append("\"").append(reactionEntry.getKey().name())
                        .append("\": ").append(reactionEntry.getValue());
                }

                json.append("}");
            }

            json.append("\n}");

            Files.write(STORAGE_FILE,
                        json.toString().getBytes(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException e) {
            System.err.println("⚠️ ReactionAnalyticsService: could not save reactions — " + e.getMessage());
        }
    }
}

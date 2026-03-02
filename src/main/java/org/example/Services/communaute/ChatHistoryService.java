package org.example.Services.communaute;

import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 💾 ChatHistoryService
 * ─────────────────────────────────────────────────────────────────
 * Persists chat messages to a local JSON file — no database needed.
 *
 * File location:  C:\Users\YourName\.re7la\chat-history.json
 *   (same .re7la folder used by ReactionAnalyticsService)
 *
 * File format:
 * [
 *   {"username":"John Doe","text":"Salut !","timestamp":1720000000000,"own":true},
 *   {"username":"Alice",   "text":"Bonjour !","timestamp":1720000001000,"own":false}
 * ]
 *
 * Max 500 messages kept — oldest are trimmed automatically.
 * ─────────────────────────────────────────────────────────────────
 */
public class ChatHistoryService {

    private static final Path STORAGE_DIR  =
            Paths.get(System.getProperty("user.home"), ".re7la");
    private static final Path STORAGE_FILE =
            STORAGE_DIR.resolve("chat-history.json");

    private static final int MAX_MESSAGES = 500;

    // ── Singleton ─────────────────────────────────────────────────
    private static ChatHistoryService instance;
    public static ChatHistoryService getInstance() {
        if (instance == null) instance = new ChatHistoryService();
        return instance;
    }
    private ChatHistoryService() {}

    // ─────────────────────────────────────────────────────────────
    //  DATA CLASS
    // ─────────────────────────────────────────────────────────────
    public static class HistoryMessage {
        public final String  username;
        public final String  text;
        public final long    timestamp;
        public final boolean own;         // true = sent by this user

        public HistoryMessage(String username, String text, long timestamp, boolean own) {
            this.username  = username;
            this.text      = text;
            this.timestamp = timestamp;
            this.own       = own;
        }

        /** Human-readable time e.g. "14:32" */
        public String formattedTime() {
            return new SimpleDateFormat("HH:mm").format(new Date(timestamp));
        }

        /** Human-readable date e.g. "25/02/2026" */
        public String formattedDate() {
            return new SimpleDateFormat("dd/MM/yyyy").format(new Date(timestamp));
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  SAVE — append one message and write to disk
    // ─────────────────────────────────────────────────────────────
    public void save(String username, String text, boolean own) {
        List<HistoryMessage> history = load();
        history.add(new HistoryMessage(username, text, System.currentTimeMillis(), own));

        // Trim to max
        if (history.size() > MAX_MESSAGES) {
            history = history.subList(history.size() - MAX_MESSAGES, history.size());
        }

        writeToFile(history);
    }

    // ─────────────────────────────────────────────────────────────
    //  LOAD — read all messages from disk
    // ─────────────────────────────────────────────────────────────
    public List<HistoryMessage> load() {
        List<HistoryMessage> result = new ArrayList<>();
        if (!Files.exists(STORAGE_FILE)) return result;

        try {
            String json = new String(Files.readAllBytes(STORAGE_FILE)).trim();
            if (json.isEmpty() || json.equals("[]")) return result;

            // Strip outer [ ]
            json = json.substring(1, json.length() - 1).trim();
            if (json.isEmpty()) return result;

            // Split into individual { } objects
            for (String entry : splitObjects(json)) {
                HistoryMessage m = parseEntry(entry.trim());
                if (m != null) result.add(m);
            }
        } catch (IOException e) {
            System.err.println("⚠️ ChatHistoryService: load failed — " + e.getMessage());
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────
    //  CLEAR — wipe the history file
    // ─────────────────────────────────────────────────────────────
    public void clear() {
        try {
            Files.writeString(STORAGE_FILE, "[]",
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("⚠️ ChatHistoryService: clear failed — " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  WRITE JSON to disk
    // ─────────────────────────────────────────────────────────────
    private void writeToFile(List<HistoryMessage> history) {
        try {
            if (!Files.exists(STORAGE_DIR)) Files.createDirectories(STORAGE_DIR);

            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < history.size(); i++) {
                HistoryMessage m = history.get(i);
                sb.append("  {")
                  .append("\"username\":\"").append(escapeJson(m.username)).append("\",")
                  .append("\"text\":\"").append(escapeJson(m.text)).append("\",")
                  .append("\"timestamp\":").append(m.timestamp).append(",")
                  .append("\"own\":").append(m.own)
                  .append("}");
                if (i < history.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]");

            Files.writeString(STORAGE_FILE, sb.toString(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException e) {
            System.err.println("⚠️ ChatHistoryService: save failed — " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  PARSE a single JSON object entry
    // ─────────────────────────────────────────────────────────────
    private HistoryMessage parseEntry(String json) {
        try {
            String username  = extractString(json, "username");
            String text      = extractString(json, "text");
            long   timestamp = extractLong(json, "timestamp");
            boolean own      = extractBool(json, "own");
            if (username == null || text == null) return null;
            return new HistoryMessage(username, text, timestamp, own);
        } catch (Exception e) {
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  MINIMAL JSON HELPERS  (no external library needed)
    // ─────────────────────────────────────────────────────────────

    /** Splits a comma-separated list of {...} objects at the top level */
    private List<String> splitObjects(String s) {
        List<String> parts = new ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if      (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0) parts.add(s.substring(start, i + 1)); }
        }
        return parts;
    }

    private String extractString(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int s = json.indexOf(marker);
        if (s == -1) return null;
        s += marker.length();
        StringBuilder sb = new StringBuilder();
        for (int i = s; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char n = json.charAt(i + 1);
                if      (n == '"')  { sb.append('"');  i++; }
                else if (n == 'n')  { sb.append('\n'); i++; }
                else if (n == '\\') { sb.append('\\'); i++; }
                else                { sb.append(c); }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private long extractLong(String json, String key) {
        String marker = "\"" + key + "\":";
        int s = json.indexOf(marker);
        if (s == -1) return 0;
        s += marker.length();
        int e = s;
        while (e < json.length() && (Character.isDigit(json.charAt(e)) || json.charAt(e) == '-')) e++;
        return Long.parseLong(json.substring(s, e));
    }

    private boolean extractBool(String json, String key) {
        String marker = "\"" + key + "\":";
        int s = json.indexOf(marker);
        if (s == -1) return false;
        s += marker.length();
        return json.startsWith("true", s);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
    }
}

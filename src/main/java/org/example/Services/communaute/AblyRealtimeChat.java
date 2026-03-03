package org.example.Services.communaute;

import io.ably.lib.realtime.AblyRealtime;
import io.ably.lib.realtime.Channel;
import io.ably.lib.realtime.ConnectionEvent;
import io.ably.lib.types.ClientOptions;
import io.ably.lib.types.Message;

import java.util.function.Consumer;

/**
 * 💬 AblyRealtimeChat
 * ─────────────────────────────────────────────────────────────────
 * Wraps the Ably Java SDK for a zero-DB global community chat.
 *
 * MAVEN DEPENDENCY — add to pom.xml:
 * ┌─────────────────────────────────────────────────────────────┐
 * │  <dependency>                                               │
 * │    <groupId>io.ably</groupId>                               │
 * │    <artifactId>ably-java</artifactId>                       │
 * │    <version>1.6.1</version>                                 │
 * │  </dependency>                                              │
 * └─────────────────────────────────────────────────────────────┘
 *
 * SETUP (one-time, 2 minutes):
 *   1. https://ably.com → Sign up free
 *   2. Dashboard → Create App → Copy the API key (looks like xxxx.yyyy:zzzz)
 *   3. Paste it below replacing ABLY_API_KEY_HERE
 * ─────────────────────────────────────────────────────────────────
 */
public class AblyRealtimeChat {

    // ── ⚠️  Paste your Ably API key here ──────────────────────────
    private static final String ABLY_API_KEY  = "j1eqaw.8z2dYg:IKMqWfVcCmgERgnDyS1qvdGLAl4C8j4KUcj65k6ivFY";
    private static final String CHANNEL_NAME  = "re7la-global-chat";
    private static final String MSG_EVENT     = "chat";

    // ── Singleton ─────────────────────────────────────────────────
    private static AblyRealtimeChat instance;
    public static AblyRealtimeChat getInstance() {
        if (instance == null) instance = new AblyRealtimeChat();
        return instance;
    }
    private AblyRealtimeChat() {}

    // ── State ─────────────────────────────────────────────────────
    private AblyRealtime ably;
    private Channel      channel;
    private boolean      connected = false;

    // ═══════════════════════════════════════════════════════════════
    //  CONNECT  — call once when the chat window opens
    // ═══════════════════════════════════════════════════════════════
    public void connect(String username,
                        Consumer<ChatMessage> onMessage,
                        Runnable              onConnected,
                        Consumer<String>      onError) {
        // Already connected — just re-subscribe
        if (connected && channel != null) {
            if (onConnected != null) onConnected.run();
            return;
        }
        try {
            ClientOptions opts = new ClientOptions(ABLY_API_KEY);
            opts.clientId      = username;
            ably               = new AblyRealtime(opts);

            ably.connection.on(ConnectionEvent.connected, state -> {
                connected = true;
                channel   = ably.channels.get(CHANNEL_NAME);
                try {
                    channel.subscribe(MSG_EVENT, raw -> {
                        ChatMessage m = parse(raw);
                        if (m != null) onMessage.accept(m);
                    });
                } catch (Exception e) {
                    onError.accept("Erreur canal : " + e.getMessage());
                }
                if (onConnected != null) onConnected.run();
            });

            ably.connection.on(ConnectionEvent.failed, state ->
                    onError.accept("Connexion échouée : " +
                            (state.reason != null ? state.reason.message : "inconnue")));

            ably.connection.on(ConnectionEvent.disconnected, state -> connected = false);

        } catch (Exception e) {
            onError.accept("Ably init error : " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  SEND
    // ═══════════════════════════════════════════════════════════════
    public void send(String username, String text) {
        if (!connected || channel == null) return;
        try {
            // payload: "username§text"  (§ is unlikely to appear in chat)
            channel.publish(MSG_EVENT, username + "§" + text);
        } catch (Exception e) {
            System.err.println("⚠️ AblyChat send failed: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  DISCONNECT
    // ═══════════════════════════════════════════════════════════════
    public void disconnect() {
        try { if (ably != null) ably.close(); } catch (Exception ignored) {}
        connected = false;
        channel   = null;
        instance  = null;   // reset singleton so next open reconnects cleanly
    }

    public boolean isConnected() { return connected; }

    // ═══════════════════════════════════════════════════════════════
    //  PARSE incoming Ably message
    // ═══════════════════════════════════════════════════════════════
    private ChatMessage parse(Message raw) {
        try {
            String payload = (String) raw.data;
            int sep = payload.indexOf('§');
            if (sep == -1) return new ChatMessage("?", payload);
            return new ChatMessage(payload.substring(0, sep), payload.substring(sep + 1));
        } catch (Exception e) { return null; }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Data class
    // ═══════════════════════════════════════════════════════════════
    public static class ChatMessage {
        public final String username;
        public final String text;
        public final long   timestamp = System.currentTimeMillis();

        public ChatMessage(String username, String text) {
            this.username = username;
            this.text     = text;
        }
    }
}

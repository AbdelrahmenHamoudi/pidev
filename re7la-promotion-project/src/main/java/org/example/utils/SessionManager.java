package org.example.utils;

/**
 * ✅ FIXED: SessionManager now holds a real dynamic user.
 *
 * HOW TO USE IN INTEGRATION:
 *   When the user logs in via the User module, that module should call:
 *       SessionManager.setCurrentUser(userId, fullName, email);
 *   Your Promotion module then reads it transparently via getCurrentUserId() etc.
 *
 *   For standalone testing (no login screen), default fallback uses user id=42
 *   (abdou abdou — a real user present in re7la_3a9).
 */
public class SessionManager {

    // ── Dynamic session fields ──
    private static int    currentUserId   = 36;           // fallback: abdou abdou (real user in re7la_3a9)
    private static String currentUserName = "abdou abdou"; // fallback display name
    private static String currentUserEmail = "abdelrahmanhamoudi8@gmail.com";

    /**
     * Called by the User module after successful login.
     * This is the integration point — no login UI needed in Promotion module.
     */
    public static void setCurrentUser(int userId, String fullName, String email) {
        currentUserId    = userId;
        currentUserName  = fullName;
        currentUserEmail = email;
        System.out.println("✅ [SessionManager] User connecté : " + fullName + " (ID=" + userId + ")");
    }

    /** Called by User module on logout. */
    public static void clearSession() {
        currentUserId    = 0;
        currentUserName  = null;
        currentUserEmail = null;
    }

    public static int    getCurrentUserId()    { return currentUserId; }
    public static String getCurrentUserName()  { return currentUserName != null ? currentUserName : "Utilisateur"; }
    public static String getCurrentUserEmail() { return currentUserEmail != null ? currentUserEmail : ""; }
    public static boolean isLoggedIn()         { return currentUserId > 0; }
}
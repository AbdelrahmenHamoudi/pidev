package org.example.utils;

/**
 * FIXED v2 - Session always reflects the currently logged-in user.
 *
 * ROOT CAUSE OF OLD BUG:
 *   The previous version initialised currentUserId = 36 at class-load time.
 *   That made it impossible for any subsequent setCurrentUser() call to
 *   "feel" like a change. The fix is to start with 0 and rely exclusively
 *   on setCurrentUser().
 *
 * HOW TO INTEGRATE:
 *   On login success (User module):
 *       SessionManager.setCurrentUser(userId, fullName, email);
 *   On logout:
 *       SessionManager.clearSession();
 */
public class SessionManager {

    // Fallback for standalone / dev mode (no login screen)
    private static final int    FALLBACK_ID    = 36;
    private static final String FALLBACK_NAME  = "abdou abdou";
    private static final String FALLBACK_EMAIL = "abdelrahmanhamoudi8@gmail.com";

    // Live session fields - start EMPTY, filled only by setCurrentUser()
    private static int    currentUserId    = 0;
    private static String currentUserName  = null;
    private static String currentUserEmail = null;

    /**
     * Called by the User module immediately after a successful login.
     * Completely replaces any previous session - no caching, no residue.
     */
    public static void setCurrentUser(int userId, String fullName, String email) {
        currentUserId    = userId;
        currentUserName  = (fullName != null && !fullName.isBlank())  ? fullName.trim()  : "Utilisateur";
        currentUserEmail = (email    != null && !email.isBlank())     ? email.trim()     : "";
        System.out.println("[SessionManager] Session updated -> "
                + currentUserName + " <" + currentUserEmail + "> (ID=" + currentUserId + ")");
    }

    /**
     * Called by the User module on logout.
     * Resets all fields so the next login starts completely clean.
     */
    public static void clearSession() {
        currentUserId    = 0;
        currentUserName  = null;
        currentUserEmail = null;
        System.out.println("[SessionManager] Session cleared.");
    }

    public static int getCurrentUserId() {
        return (currentUserId > 0) ? currentUserId : FALLBACK_ID;
    }

    public static String getCurrentUserName() {
        return (currentUserName != null) ? currentUserName : FALLBACK_NAME;
    }

    public static String getCurrentUserEmail() {
        return (currentUserEmail != null) ? currentUserEmail : FALLBACK_EMAIL;
    }

    public static boolean isLoggedIn() {
        return currentUserId > 0;
    }
}
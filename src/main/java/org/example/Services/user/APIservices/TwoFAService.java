package org.example.Services.user.APIservices;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.example.Entites.user.User;
import org.example.Entites.user.User2FA;
import org.example.Services.user.UserCRUD;
import org.example.Utils.MyBD;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TwoFAService {

    private Connection conn;
    private GoogleAuthenticator gAuth;
    private final UserCRUD userCRUD = new UserCRUD();

    public TwoFAService() {
        conn = MyBD.getInstance().getConnection();

        // Configuration de Google Authenticator avec fenêtre de tolérance plus large
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setTimeStepSizeInMillis(TimeUnit.SECONDS.toMillis(30))
                .setWindowSize(5) // Augmenté de 3 à 5 pour plus de tolérance
                .build();
        gAuth = new GoogleAuthenticator(config);

        System.out.println("✅ Service 2FA initialisé avec windowSize=5");
    }

    /**
     * Génère une clé secrète et un QR code pour un utilisateur
     */
    public TwoFASetup generateSecret(int userId) {
        // Générer une clé secrète
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secretKey = key.getKey();

        // Récupérer l'utilisateur
        User user = userCRUD.getUserById(userId);
        if (user == null) return null;

        // Générer le QR code
        String otpAuthUrl = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                "RE7LA Tunisie",
                user.getE_mail(),
                key
        );

        // Sauvegarder la clé en base
        saveSecretKey(userId, secretKey);

        System.out.println("🔐 Clé secrète générée pour l'utilisateur " + userId + ": " + secretKey);

        return new TwoFASetup(secretKey, generateQRCode(otpAuthUrl));
    }

    /**
     * Sauvegarde la clé secrète en base
     */
    private void saveSecretKey(int userId, String secretKey) {
        // Supprimer l'ancienne configuration si elle existe
        String deleteSql = "DELETE FROM user_2fa WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression ancienne 2FA: " + e.getMessage());
        }

        // Insérer la nouvelle clé
        String sql = "INSERT INTO user_2fa (user_id, secret_key) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, secretKey);
            pstmt.executeUpdate();
            System.out.println("✅ Clé 2FA sauvegardée pour l'utilisateur ID: " + userId);
        } catch (SQLException e) {
            System.err.println("❌ Erreur sauvegarde clé 2FA: " + e.getMessage());
        }
    }

    /**
     * Génère une image QR code en Base64
     */
    private String generateQRCode(String otpAuthUrl) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(otpAuthUrl, BarcodeFormat.QR_CODE, 200, 200);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            return Base64.getEncoder().encodeToString(pngData);

        } catch (WriterException | IOException e) {
            System.err.println("❌ Erreur génération QR code: " + e.getMessage());
            return null;
        }
    }

    /**
     * Récupère la clé secrète d'un utilisateur (même si 2FA pas encore activée)
     */
    private String getPendingSecretKey(int userId) {
        String sql = "SELECT secret_key FROM user_2fa WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("secret_key");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération clé 2FA: " + e.getMessage());
        }
        return null;
    }

    /**
     * Récupère la clé secrète d'un utilisateur (seulement si 2FA activée)
     */
    private String getSecretKey(int userId) {
        String sql = "SELECT secret_key FROM user_2fa WHERE user_id = ? AND enabled = true";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("secret_key");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération clé 2FA: " + e.getMessage());
        }
        return null;
    }

    /**
     * Active la 2FA pour un utilisateur (version avec débogage)
     */
    public boolean enable2FA(int userId, int verificationCode) {
        try {
            // Récupérer la clé secrète (même si pas encore activée)
            String secretKey = getPendingSecretKey(userId);
            if (secretKey == null) {
                System.out.println("❌ Clé secrète non trouvée pour l'utilisateur " + userId);
                return false;
            }

            System.out.println("\n🔐 === VÉRIFICATION 2FA ===");
            System.out.println("🔐 Clé secrète: " + secretKey);
            System.out.println("🔐 Code saisi: " + verificationCode);
            System.out.println("🔐 Heure système: " + System.currentTimeMillis());

            // Vérifier le code avec la méthode standard
            boolean isValid = gAuth.authorize(secretKey, verificationCode);

            if (!isValid) {
                // Si ça ne marche pas, tester manuellement avec plus de fenêtres
                System.out.println("🔍 Test manuel des codes attendus:");

                // Récupérer l'heure actuelle en secondes
                long currentTimeSeconds = System.currentTimeMillis() / 1000;
                long timeStep = 30; // 30 secondes par défaut

                // Tester les 5 dernières et 5 prochaines fenêtres
                for (int i = -5; i <= 5; i++) {
                    long testTime = currentTimeSeconds + (i * timeStep);
                    int expectedCode = gAuth.getTotpPassword(secretKey, testTime);
                    System.out.println("   Fenêtre " + i + " (temps: " + testTime + "): " + expectedCode);

                    if (expectedCode == verificationCode) {
                        System.out.println("✅ Code valide trouvé à la fenêtre " + i);
                        isValid = true;
                        break;
                    }
                }
            }

            if (isValid) {
                // Activer la 2FA
                String sql = "UPDATE user_2fa SET enabled = true WHERE user_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, userId);
                    pstmt.executeUpdate();

                    // Générer et sauvegarder les codes de secours
                    generateBackupCodes(userId);

                    System.out.println("✅ 2FA activée pour l'utilisateur ID: " + userId);
                    return true;
                }
            } else {
                System.out.println("❌ Code invalide pour l'utilisateur " + userId);
            }
            return false;

        } catch (SQLException e) {
            System.err.println("❌ Erreur activation 2FA: " + e.getMessage());
            return false;
        }
    }

    /**
     * Vérifie un code 2FA
     */
    public boolean verifyCode(int userId, int code) {
        try {
            // Vérifier d'abord les codes de secours
            if (verifyBackupCode(userId, code)) {
                System.out.println("✅ Code de secours valide pour l'utilisateur " + userId);
                return true;
            }

            // Sinon vérifier avec Google Authenticator
            String secretKey = getSecretKey(userId);
            if (secretKey == null) {
                System.out.println("❌ Clé secrète non trouvée pour l'utilisateur " + userId);
                return false;
            }

            boolean isValid = gAuth.authorize(secretKey, code);
            System.out.println("🔐 Vérification 2FA pour " + userId + ": " + (isValid ? "VALIDE" : "INVALIDE"));

            return isValid;

        } catch (Exception e) {
            System.err.println("❌ Erreur vérification 2FA: " + e.getMessage());
            return false;
        }
    }

    /**
     * Vérifie si l'utilisateur a la 2FA activée
     */
    public boolean is2FAEnabled(int userId) {
        String sql = "SELECT enabled FROM user_2fa WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("enabled");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur vérification 2FA: " + e.getMessage());
        }
        return false;
    }

    /**
     * Génère des codes de secours
     */
    private void generateBackupCodes(int userId) {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            codes.add(String.format("%06d", (int)(Math.random() * 1000000)));
        }

        String codesString = String.join(",", codes);

        String sql = "UPDATE user_2fa SET backup_codes = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, codesString);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();

            System.out.println("🔐 Codes de secours générés pour l'utilisateur ID: " + userId);
        } catch (SQLException e) {
            System.err.println("❌ Erreur sauvegarde codes secours: " + e.getMessage());
        }
    }

    /**
     * Vérifie un code de secours
     */
    private boolean verifyBackupCode(int userId, int code) {
        String sql = "SELECT backup_codes FROM user_2fa WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String codesString = rs.getString("backup_codes");
                if (codesString != null) {
                    String[] codes = codesString.split(",");
                    String codeStr = String.format("%06d", code);

                    for (int i = 0; i < codes.length; i++) {
                        if (codes[i].equals(codeStr)) {
                            // Supprimer le code utilisé
                            removeBackupCode(userId, i);
                            return true;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur vérification code secours: " + e.getMessage());
        }
        return false;
    }

    /**
     * Supprime un code de secours utilisé
     */
    private void removeBackupCode(int userId, int index) {
        try {
            String sql = "SELECT backup_codes FROM user_2fa WHERE user_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String[] codes = rs.getString("backup_codes").split(",");
                codes[index] = "USED";
                String newCodes = String.join(",", codes);

                String updateSql = "UPDATE user_2fa SET backup_codes = ? WHERE user_id = ?";
                try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                    updatePstmt.setString(1, newCodes);
                    updatePstmt.setInt(2, userId);
                    updatePstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression code secours: " + e.getMessage());
        }
    }

    /**
     * Désactive la 2FA
     */
    public boolean disable2FA(int userId) {
        String sql = "DELETE FROM user_2fa WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            System.out.println("✅ 2FA désactivée pour l'utilisateur ID: " + userId);
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Erreur désactivation 2FA: " + e.getMessage());
            return false;
        }
    }

    /**
     * Classe interne pour le retour de configuration 2FA
     */
    public static class TwoFASetup {
        private String secretKey;
        private String qrCodeBase64;

        public TwoFASetup(String secretKey, String qrCodeBase64) {
            this.secretKey = secretKey;
            this.qrCodeBase64 = qrCodeBase64;
        }

        public String getSecretKey() { return secretKey; }
        public String getQrCodeBase64() { return qrCodeBase64; }
    }
}
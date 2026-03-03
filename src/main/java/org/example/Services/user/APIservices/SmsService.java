package org.example.Services.user.APIservices;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.example.Entites.user.User;

import java.io.InputStream;
import java.util.Properties;

public class SmsService {

    private static String ACCOUNT_SID;
    private static String AUTH_TOKEN;
    private static String TWILIO_PHONE_NUMBER;
    private static boolean isInitialized = false;
    private static boolean TEST_MODE = true; // Mettre false pour production

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream input = SmsService.class.getClassLoader().getResourceAsStream("twilio.properties")) {
            Properties prop = new Properties();
            if (input != null) {
                prop.load(input);
                ACCOUNT_SID = prop.getProperty("twilio.account.sid");
                AUTH_TOKEN = prop.getProperty("twilio.auth.token");
                TWILIO_PHONE_NUMBER = prop.getProperty("twilio.phone.number");

                if (ACCOUNT_SID != null && AUTH_TOKEN != null &&
                        !ACCOUNT_SID.startsWith("ACx") && !AUTH_TOKEN.equals("your_auth_token_here")) {
                    Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
                    isInitialized = true;
                    System.out.println("✅ Twilio initialisé avec succès");
                    System.out.println("📱 Numéro Twilio: " + TWILIO_PHONE_NUMBER);
                } else {
                    System.out.println("⚠️ Mode test activé (clés Twilio non configurées)");
                }
            } else {
                System.err.println("❌ Fichier twilio.properties non trouvé dans resources");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement configuration Twilio: " + e.getMessage());
        }
    }

    /**
     * Envoie un SMS de bienvenue après inscription
     */
    public static boolean sendWelcomeSms(User user) {
        if (user.getNum_tel() == null || user.getNum_tel().isEmpty()) {
            System.out.println("📱 Pas de numéro de téléphone pour " + user.getE_mail());
            return false;
        }

        String message = String.format(
                "👋 Bienvenue %s %s sur RE7LA Tunisie !\n" +
                        "Votre compte a été créé avec succès.\n" +
                        "🌍 Explorez les plus belles destinations tunisiennes !",
                user.getPrenom(), user.getNom()
        );

        return sendSms(user.getNum_tel(), message);
    }

    /**
     * Envoie un SMS de confirmation de réservation
     */
    public static boolean sendReservationConfirmation(User user, String details) {
        if (user.getNum_tel() == null || user.getNum_tel().isEmpty()) return false;

        String message = String.format(
                "✅ Réservation confirmée %s !\n%s\n" +
                        "Merci de votre confiance - RE7LA Tunisie",
                user.getPrenom(), details
        );

        return sendSms(user.getNum_tel(), message);
    }

    /**
     * Envoie un SMS de code de vérification
     */
    public static boolean sendVerificationCode(User user, String code) {
        if (user.getNum_tel() == null || user.getNum_tel().isEmpty()) return false;

        String message = String.format(
                "🔐 Votre code de vérification RE7LA : %s\n" +
                        "Valable 10 minutes.",
                code
        );

        return sendSms(user.getNum_tel(), message);
    }

    /**
     * Méthode générique d'envoi SMS
     */
    private static boolean sendSms(String toNumber, String messageText) {
        // Mode test (pour développement sans crédit)
        if (TEST_MODE || !isInitialized) {
            System.out.println("\n📱 ===== SMS (MODE TEST) =====");
            System.out.println("À: " + formatPhoneNumber(toNumber));
            System.out.println("Message: " + messageText);
            System.out.println("📱 ===========================\n");
            return true;
        }

        try {
            // Formater le numéro (doit être au format international)
            String formattedNumber = formatPhoneNumber(toNumber);

            Message message = Message.creator(
                    new PhoneNumber(formattedNumber),      // Destinataire
                    new PhoneNumber(TWILIO_PHONE_NUMBER),  // Votre numéro Twilio
                    messageText                              // Message
            ).create();

            System.out.println("✅ SMS envoyé à " + formattedNumber + " (SID: " + message.getSid() + ")");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi SMS: " + e.getMessage());
            return false;
        }
    }

    /**
     * Formate le numéro de téléphone au format international
     */
    private static String formatPhoneNumber(String number) {
        if (number == null) return "";

        // Nettoyer le numéro (enlever espaces, tirets, etc.)
        String clean = number.replaceAll("[^0-9+]", "");

        // Si c'est un numéro tunisien sans indicatif (ex: 99123456)
        if (clean.matches("^[2-9][0-9]{7}$")) {
            return "+216" + clean;
        }
        // Si commence par 0 (ex: 099123456)
        else if (clean.matches("^0[2-9][0-9]{7}$")) {
            return "+216" + clean.substring(1);
        }
        // Si déjà au format international (ex: +21699123456)
        else if (clean.matches("^\\+216[0-9]{8}$")) {
            return clean;
        }
        // Sinon, retourner tel quel
        return clean;
    }

    /**
     * Active/désactive le mode test
     */
    public static void setTestMode(boolean mode) {
        TEST_MODE = mode;
        System.out.println("📱 Mode test SMS: " + (mode ? "ACTIVÉ" : "DÉSACTIVÉ"));
    }
}
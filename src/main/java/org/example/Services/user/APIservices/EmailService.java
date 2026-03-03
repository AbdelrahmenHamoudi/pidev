package org.example.Services.user.APIservices;


import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Random;

public class EmailService {

    // Configuration Gmail
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EMAIL = "abdelrahmanhamoudi8@gmail.com";
    private static final String PASSWORD = "rwlu tkmt zfpd sklk";

    private static final Random random = new Random();

    /**
     * Envoie un email de vérification
     */
    public static boolean sendVerificationEmail(String to, String name, String code) {
        String subject = "🔐 RE7LA Tunisie - Vérification de votre email";
        String content = getVerificationEmailTemplate(name, code);
        return sendEmail(to, subject, content);
    }

    /**
     * Envoie un email de réinitialisation de mot de passe avec CODE
     */
    public static boolean sendResetPasswordCode(String to, String name, String code) {
        String subject = "🔄 RE7LA Tunisie - Code de réinitialisation";
        String content = getResetCodeTemplate(name, code);
        return sendEmail(to, subject, content);
    }

    /**
     * Envoie un email de bienvenue
     */
    public static boolean sendWelcomeEmail(String to, String name) {
        String subject = "👋 Bienvenue sur RE7LA Tunisie !";
        String content = getWelcomeEmailTemplate(name);
        return sendEmail(to, subject, content);
    }

    /**
     * Envoie un email de notification de réservation
     */
    public static boolean sendReservationConfirmation(String to, String name, String details) {
        String subject = "✅ RE7LA Tunisie - Confirmation de réservation";
        String content = getReservationTemplate(name, details);
        return sendEmail(to, subject, content);
    }

    /**
     * Méthode générique d'envoi d'email
     */
    private static boolean sendEmail(String to, String subject, String content) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL, PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL, "RE7LA Tunisie"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ Email envoyé avec succès à: " + to);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Génère un code de vérification aléatoire (6 chiffres)
     */
    public static String generateVerificationCode() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Génère un token pour réinitialisation (optionnel, gardé pour compatibilité)
     */
    public static String generateResetToken() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Template email de vérification
     */
    private static String getVerificationEmailTemplate(String name, String code) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 20px auto; background: white; border-radius: 15px; overflow: hidden; box-shadow: 0 5px 15px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #1ABC9C, #16a085); color: white; padding: 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; }
                    .content { padding: 40px 30px; text-align: center; }
                    .code { font-size: 48px; font-weight: bold; color: #F39C12; letter-spacing: 10px; margin: 30px 0; padding: 20px; background: #f8f9fa; border-radius: 10px; border: 2px dashed #1ABC9C; }
                    .footer { background: #2C3E50; color: white; padding: 20px; text-align: center; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class='container'>
                    <div class='header'>
                        <h1>🏝️ RE7LA Tunisie</h1>
                    </div>
                    <div class='content'>
                        <h2>Bonjour %s,</h2>
                        <p>Merci de vous être inscrit sur RE7LA Tunisie !</p>
                        <p>Pour vérifier votre adresse email, veuillez utiliser le code suivant :</p>
                        <div class='code'>%s</div>
                        <p>Ce code expire dans 24 heures.</p>
                        <p>Si vous n'avez pas créé de compte, ignorez cet email.</p>
                    </div>
                    <div class='footer'>
                        <p>© 2024 RE7LA Tunisie - Tous droits réservés</p>
                        <p>🇹🇳 La plus belle destination</p>
                    </div>
                </div>
            </body>
            </html>
            """, name, code);
    }

    /**
     * Template email de réinitialisation avec CODE
     */
    private static String getResetCodeTemplate(String name, String code) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 20px auto; background: white; border-radius: 15px; overflow: hidden; box-shadow: 0 5px 15px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #1ABC9C, #16a085); color: white; padding: 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; }
                    .content { padding: 40px 30px; text-align: center; }
                    .code { font-size: 48px; font-weight: bold; color: #F39C12; letter-spacing: 10px; margin: 30px 0; padding: 20px; background: #f8f9fa; border-radius: 10px; border: 2px dashed #1ABC9C; }
                    .warning { background: #fef5e7; color: #e67e22; padding: 15px; border-radius: 10px; margin: 20px 0; }
                    .footer { background: #2C3E50; color: white; padding: 20px; text-align: center; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class='container'>
                    <div class='header'>
                        <h1>🏝️ RE7LA Tunisie</h1>
                    </div>
                    <div class='content'>
                        <h2>Bonjour %s,</h2>
                        <p>Nous avons reçu une demande de réinitialisation de mot de passe pour votre compte.</p>
                        <p>Voici votre code de réinitialisation :</p>
                        <div class='code'>%s</div>
                        <p>Ce code expire dans 1 heure.</p>
                        <div class='warning'>
                            ⚠️ Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.
                        </div>
                    </div>
                    <div class='footer'>
                        <p>© 2024 RE7LA Tunisie - Tous droits réservés</p>
                    </div>
                </div>
            </body>
            </html>
            """, name, code);
    }

    /**
     * Template email de bienvenue
     */
    private static String getWelcomeEmailTemplate(String name) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 20px auto; background: white; border-radius: 15px; overflow: hidden; box-shadow: 0 5px 15px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #1ABC9C, #16a085); color: white; padding: 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; }
                    .content { padding: 40px 30px; }
                    .features { display: flex; justify-content: space-around; margin: 30px 0; }
                    .feature { text-align: center; }
                    .feature-icon { font-size: 40px; }
                    .footer { background: #2C3E50; color: white; padding: 20px; text-align: center; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class='container'>
                    <div class='header'>
                        <h1>🏝️ Bienvenue sur RE7LA Tunisie !</h1>
                    </div>
                    <div class='content'>
                        <h2>Bonjour %s,</h2>
                        <p>Nous sommes ravis de vous accueillir sur RE7LA Tunisie, votre plateforme de voyage et loisirs.</p>
                        
                        <div class='features'>
                            <div class='feature'>
                                <div class='feature-icon'>🏨</div>
                                <h3>Hébergements</h3>
                                <p>Des milliers d'options</p>
                            </div>
                            <div class='feature'>
                                <div class='feature-icon'>🎯</div>
                                <h3>Activités</h3>
                                <p>Excursions et loisirs</p>
                            </div>
                            <div class='feature'>
                                <div class='feature-icon'>🚗</div>
                                <h3>Trajets</h3>
                                <p>Déplacements faciles</p>
                            </div>
                        </div>
                        
                        <p>Commencez dès maintenant à explorer la Tunisie !</p>
                        <p>🇹🇳 À bientôt,<br>L'équipe RE7LA</p>
                    </div>
                    <div class='footer'>
                        <p>© 2024 RE7LA Tunisie - Tous droits réservés</p>
                    </div>
                </div>
            </body>
            </html>
            """, name);
    }

    /**
     * Template confirmation réservation
     */
    private static String getReservationTemplate(String name, String details) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 20px auto; background: white; border-radius: 15px; overflow: hidden; box-shadow: 0 5px 15px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #1ABC9C, #16a085); color: white; padding: 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; }
                    .content { padding: 40px 30px; }
                    .details { background: #f8f9fa; padding: 20px; border-radius: 10px; margin: 20px 0; }
                    .footer { background: #2C3E50; color: white; padding: 20px; text-align: center; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class='container'>
                    <div class='header'>
                        <h1>✅ Réservation confirmée</h1>
                    </div>
                    <div class='content'>
                        <h2>Bonjour %s,</h2>
                        <p>Votre réservation a été confirmée avec succès !</p>
                        
                        <div class='details'>
                            <h3>📋 Détails de la réservation :</h3>
                            <p>%s</p>
                        </div>
                        
                        <p>Nous vous remercions pour votre confiance.</p>
                        <p>🇹🇳 À bientôt en Tunisie !</p>
                    </div>
                    <div class='footer'>
                        <p>© 2024 RE7LA Tunisie - Tous droits réservés</p>
                    </div>
                </div>
            </body>
            </html>
            """, name, details);
    }
}
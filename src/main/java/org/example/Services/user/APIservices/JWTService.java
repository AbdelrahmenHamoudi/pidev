package org.example.Services.user.APIservices;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.example.Entites.user.Role;
import org.example.Entites.user.Status;
import org.example.Entites.user.User;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class JWTService {

    // Clé secrète (à garder précieusement)
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final Key SIGNING_KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    // Durée de validité : 24 heures
    private static final long EXPIRATION_TIME = 86400000;

    /**
     * Génère un token JWT pour un utilisateur
     */
    public static String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("email", user.getE_mail());
        claims.put("role", user.getRole().name());
        claims.put("nom", user.getNom());
        claims.put("prenom", user.getPrenom());
        claims.put("status", user.getStatus().name());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getE_mail())
                .setIssuedAt(new Date())
                .setIssuer("RE7LA-Tunisie")
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Valide un token et retourne les claims
     */
    public static Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SIGNING_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            System.err.println("❌ Token expiré");
            return null;
        } catch (JwtException e) {
            System.err.println("❌ Token invalide: " + e.getMessage());
            return null;
        }
    }

    /**
     * Vérifie si le token est expiré
     */
    public static boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Extrait l'email du token
     */
    public static String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrait l'ID utilisateur du token
     */
    public static Integer extractUserId(String token) {
        Claims claims = validateToken(token);
        return claims != null ? claims.get("id", Integer.class) : null;
    }

    /**
     * Extrait le rôle du token
     */
    public static String extractRole(String token) {
        Claims claims = validateToken(token);
        return claims != null ? claims.get("role", String.class) : null;
    }

    /**
     * Extrait la date d'expiration
     */
    public static Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrait un claim spécifique
     */
    private static <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claims != null ? claimsResolver.apply(claims) : null;
    }

    /**
     * Extrait tous les claims
     */
    private static Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SIGNING_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Rafraîchit un token
     */
    public static String refreshToken(String token) {
        if (isTokenExpired(token)) {
            return null;
        }

        Claims claims = extractAllClaims(token);
        if (claims == null) return null;

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extrait l'utilisateur complet depuis le token
     */
    public static User extractUserFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims == null) return null;

        User user = new User();
        user.setId(claims.get("id", Integer.class));
        user.setE_mail(claims.getSubject());
        user.setRole(Role.valueOf(claims.get("role", String.class)));
        user.setStatus(Status.valueOf(claims.get("status", String.class)));
        user.setNom(claims.get("nom", String.class));
        user.setPrenom(claims.get("prenom", String.class));

        return user;
    }
}
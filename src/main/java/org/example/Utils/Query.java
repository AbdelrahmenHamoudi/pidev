package org.example.Utils;

public class Query {

    // user query
    public static String addUserQuery =
            "INSERT INTO users (nom, prenom, date_naiss, e_mail, num_tel, mot_de_pass, image, role, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    public static final String getUserByEmailQuery = "SELECT * FROM users WHERE e_mail = ?";
    public static String deleteUserQuery = "DELETE FROM users WHERE id=?";
    public static String updateUserQuery =
            "UPDATE users SET " + "nom = ?, " +
                    "prenom = ?, " +
                    "date_naiss = ?, " +
                    "e_mail = ?, " +
                    "num_tel = ?, " +
                    "role = ?, " +
                    "status = ? " +
                    "WHERE id = ?";
    public static String updatePasswordQuery =
            "UPDATE users SET " +
                    "mot_de_pass = ? " +
                    "WHERE id = ?";
    public static String updateImageQuery ="UPDATE users SET image = ? WHERE id = ?";
    public static String getUserByName = "SELECT * FROM users WHERE nom LIKE ?";
    public static String showUsers = "SELECT * FROM users";
    public static String signIn = "SELECT * FROM users WHERE e_mail = ? AND mot_de_pass = ?";


    //hebergement
    public static String addhebergementQuery =
            "INSERT INTO hebergement (titre, desc_hebergement, capacite, type_hebergement, disponible_heberg, prixParNuit, image) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

    public static String updatehebergementQuery =
            "UPDATE hebergement SET titre=?, desc_hebergement=?, capacite=?, type_hebergement=?, disponible_heberg=?, prixParNuit=?, image=? " +
                    "WHERE id_hebergement=?";

    public static String deletehebergementQuery =
            "DELETE FROM hebergement WHERE id_hebergement=?";

    public static String showhebergementQuery =
            "SELECT * FROM hebergement";

    // INSERT : Ajouter une réservation
    public static String addReservationQuery =
            "INSERT INTO reservation (hebergement_id, user_id, dateDebutR, dateFinR, statutR) " +
                    "VALUES (?, ?, ?, ?, ?)";

    // UPDATE : Modifier une réservation
    public static String updateReservationQuery =
            "UPDATE reservation SET hebergement_id=?, user_id=?, dateDebutR=?, dateFinR=?, statutR=? " +
                    "WHERE id_reservation=?";

    // DELETE : Supprimer une réservation
    public static String deleteReservationQuery =
            "DELETE FROM reservation WHERE id_reservation=?";

    // SELECT : Afficher toutes les réservations avec les détails de l'hébergement et de l'utilisateur
    public static String showReservationQuery =
            "SELECT " +
                    "r.id_reservation, " +
                    "r.dateDebutR, " +
                    "r.dateFinR, " +
                    "r.statutR, " +
                    "h.id_hebergement, " +
                    "h.titre, " +
                    "h.desc_hebergement, " +
                    "h.capacite, " +
                    "h.type_hebergement, " +
                    "h.disponible_heberg, " +
                    "h.prixParNuit, " +
                    "h.image, " +
                    "u.id as id_user, " +      // ← CORRIGÉ: u.id au lieu de u.id_user
                    "u.nom, " +
                    "u.prenom, " +
                    "u.e_mail as email, " +    // ← CORRIGÉ: u.e_mail au lieu de u.email
                    "u.num_tel as tel " +       // ← CORRIGÉ: u.num_tel au lieu de u.tel
                    "FROM reservation r " +
                    "INNER JOIN hebergement h ON r.hebergement_id = h.id_hebergement " +
                    "INNER JOIN users u ON r.user_id = u.id";  // ← CORRIGÉ: u.id au lieu de u.id_user
}
package org.example.Utils;

public class Query {

    // user query
    public static String addUserQuery ="INSERT INTO users " +
            "(nom, prenom, date_naiss, e_mail, num_tel, mot_de_pass, image, role, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)" ;
    public  static String deleteUserQuery ="DELETE FROM users WHERE id=?";
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
    public static String updateImageQuery =
            "UPDATE users SET " +
                    "image = ? " +
                    "WHERE id = ?";
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

    public static String addReservationQuery =
            "INSERT INTO reservation (id_reservation,id_utilisateur, id_hebergement, dateDebutR, dateFinR, prixTotalR, prixKM, statutR) " +
                    "VALUES (?,?, ?, ?, ?, ?, ?, ?)";

    public static String updateReservationQuery =
            "UPDATE reservation SET id_user=?, id_hebergement=?, dateDebutR=?, dateFinR=?, prixTotalR=?, prixKM=?, statutR=? " +
                    "WHERE id_reservation=?";

    public static String deleteReservationQuery =
            "DELETE FROM reservation WHERE id_reservation=?";

    public static String showReservationQuery =
            "SELECT * FROM reservation";

}

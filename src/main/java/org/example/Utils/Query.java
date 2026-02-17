package org.example.Utils;

public class Query {

    // user query
    public static String addUserQuery =
            "INSERT INTO users (nom, prenom, date_naiss, e_mail, num_tel, mot_de_pass, image, role, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

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

        //Publication
public static String addPublicationQuery =
        "INSERT INTO publication (id_utilisateur, ImgURL, typeCible, dateCreation, dateModif, statutP, estVerifie, DescriptionP)\n" +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?);\n";

    public static String updatePublicationQuery =
            "UPDATE publication SET ImgURL=?, typeCible=?, dateCreation=?, dateModif=?, statutP=?, estVerifie=?, DescriptionP=?" +
                    "WHERE idPublication=?";

    public static String deletePublicationQuery =
            "DELETE FROM publication WHERE idPublication=?";

    public static String showPublicationQuery =
            "SELECT * FROM publication";

    public static String addCommentaireQuery =
            "INSERT INTO commentaire (idCommentaire,id_utilisateur, idPublication, contenuC, dateCreationC, statutC)" +
                    "VALUES (?, ?, ?, ?, ?, ?)";

    public static String updateCommentaireQuery =
            "UPDATE commentaire SET  id_utilisateur=?, idPublication=?, contenuC=?, dateCreationC=?, statutC=? " +
                    "WHERE idCommentaire=?";

    public static String deleteCommentaireQuery =
            "DELETE FROM commentaire WHERE idCommentaire=?";

    public static String showCommentaireQuery =
            "SELECT * FROM commentaire";

}

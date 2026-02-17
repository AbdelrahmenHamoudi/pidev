package org.example.Tests;

import org.example.Entites.*;
import org.example.Services.CommentaireCRUD;
import org.example.Services.PublicationCRUD;
import org.example.Services.UserCRUD;

import java.sql.SQLException;

public class MainConnection {
    public static void main(String[] args) {
        PublicationCRUD publicationCRUD = new PublicationCRUD();
        CommentaireCRUD commentaireCRUD = new CommentaireCRUD();
        UserCRUD userCRUD = new UserCRUD();
        // 🔹 Création User
        User u1 = new User(
                "Awadhi",
                "Chams",
                "2003-07-14",
                "chams.awadhi@gmail.com",
                "22123456",
                "Pass123!",
                Role.user,
                "chams.png",
                Status.Unbanned
        );

        UserCRUD userService = new UserCRUD();

        try {
            userService.createUser(u1);
            System.out.println("User ajouté avec succès ✅");
        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }

        Publication pub1 = new Publication(
                null,
                u1,
                "beach_sunset.jpg",
                TypeCible.HEBERGEMENT,
                "2026-02-11",
                "2026-02-11",
                StatutP.EN_ATTENTE,
                true,
                "Beautiful sunset at the beach 🌅"
        );
        // 🔹 Création Commentaire
        Commentaire c1 = new Commentaire(
                u1,
                pub1,
                "This looks incredible! 🔥",
                "2026-02-11",
                true
        );

        // 🔹 Si tu as un setter dans Publication
        // pub1.setIdCommentaire(c1);
       try{
           publicationCRUD.ajouterh(pub1);
           commentaireCRUD.ajouterh(c1);
           System.out.println("Publication et commentaire créés avec succès 🚀");
       }catch(SQLException e){
           e.printStackTrace();
       }
    }
}

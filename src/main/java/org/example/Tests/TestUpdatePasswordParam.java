package org.example.Tests;

import org.example.Entites.user.User;
import org.example.Services.user.UserCRUD;

import java.sql.SQLException;
import java.util.Scanner;

public class TestUpdatePasswordParam {
    public static void main(String[] args) {
        UserCRUD userCRUD = new UserCRUD();
        Scanner scanner = new Scanner(System.in);

        try {
            // Récupérer l'utilisateur par email
            String email = "Abir.bani@esprit.tn";
            User user = userCRUD.getUserByEmail(email);

            if (user == null) {
                System.out.println("Utilisateur introuvable !");
                return;
            }

            System.out.println("Utilisateur trouvé : " + user.getNom() + " (" + user.getE_mail() + ")");

            // Demander le nouveau mot de passe
            System.out.print("Entrez le nouveau mot de passe : ");
            String nouveauMotDePasse = scanner.nextLine();

            // Mettre à jour le mot de passe
            user.setMot_de_pass(nouveauMotDePasse);
            userCRUD.updatePassword(user);

            System.out.println("Mot de passe mis à jour avec succès !");

            // Vérification via connexion
            User loginTest = new User();
            loginTest.setE_mail(email);
            loginTest.setMot_de_pass(nouveauMotDePasse);

            User connecté = userCRUD.signIn(loginTest);
            System.out.println("Connexion réussie avec le nouveau mot de passe : " + connecté.getNom());

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}

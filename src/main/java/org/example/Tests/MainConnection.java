package org.example.Tests;

import org.example.Entites.Role;
import org.example.Entites.Status;
import org.example.Entites.User;
import org.example.Services.UserCRUD;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class MainConnection {
    public static void main(String[] args) {
        UserCRUD userCRUD = new UserCRUD();
        Scanner scanner = new Scanner(System.in);

        try {

            System.out.println("-- Création d'un nouvel utilisateur --");
            System.out.print("Nom : ");
            String nom = scanner.nextLine();

            System.out.print("Prénom : ");
            String prenom = scanner.nextLine();

            System.out.print("Date de naissance (YYYY-MM-DD) : ");
            String dateNaiss = scanner.nextLine();

            System.out.print("Email : ");
            String email = scanner.nextLine();

            System.out.print("Numéro de téléphone : ");
            String numTel = scanner.nextLine();

            System.out.print("Mot de passe : ");
            String motDePass = scanner.nextLine();

            System.out.print("Image : ");
            String image = scanner.nextLine();


            Role role = Role.user;
            Status status = Status.Unbanned;

            User newUser = new User(nom, prenom, dateNaiss, email, numTel, motDePass, role, image, status);
            userCRUD.createUser(newUser);
            System.out.println("Utilisateur créé avec succès !");


            System.out.println("\n-- Tous les utilisateurs --");
            List<User> allUsers = userCRUD.ShowUsers();
            for (User u : allUsers) System.out.println(u);


            System.out.print("\nRechercher un utilisateur par nom : ");
            String searchName = scanner.nextLine();
            List<User> foundUsers = userCRUD.getUserByName(searchName);
            if (foundUsers.isEmpty()) {
                System.out.println("Aucun utilisateur trouvé.");
            } else {
                System.out.println("-- Utilisateurs trouvés --");
                for (User u : foundUsers) System.out.println(u);
            }

            if (!foundUsers.isEmpty()) {
                User toUpdate = foundUsers.get(0);
                System.out.print("\nNouveau mot de passe pour " + toUpdate.getNom() + " : ");
                String newPass = scanner.nextLine();
                toUpdate.setMot_de_pass(newPass);
                userCRUD.updatePassword(toUpdate);
                System.out.println("Mot de passe mis à jour !");
            }


            System.out.println("\n-- Connexion --");
            System.out.print("Email : ");
            String loginEmail = scanner.nextLine();
            System.out.print("Mot de passe : ");
            String loginPass = scanner.nextLine();

            User loginUser = new User();
            loginUser.setE_mail(loginEmail);
            loginUser.setMot_de_pass(loginPass);
            userCRUD.signIn(loginUser);


            if (!foundUsers.isEmpty()) {
                System.out.print("\nSupprimer l'utilisateur " + foundUsers.get(0).getNom() + " ? (oui/non) : ");
                String answer = scanner.nextLine();
                if (answer.equalsIgnoreCase("oui")) {
                    userCRUD.deleteUser(foundUsers.get(0));
                    System.out.println("Utilisateur supprimé !");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}

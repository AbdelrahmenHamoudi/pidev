package org.example.Entites;

import java.util.Objects;

public class User {
    private int id;
    private String nom;
    private String prenom;
    private String date_naiss;
    private String e_mail;
    private String num_tel;
    private String mot_de_pass;
    private String image;
    private Role role;
    private String status;


    public User() {
    }

    public User(int id, String nom, String prenom, String date_naiss, String e_mail, String num_tel, String mot_de_pass, String image, Role role, String status) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.date_naiss = date_naiss;
        this.e_mail = e_mail;
        this.num_tel = num_tel;
        this.mot_de_pass = mot_de_pass;
        this.image = image;
        this.role = role;
        this.status = status;
    }

    public User(String nom, String prenom, String e_mail, String date_naiss, String num_tel, String mot_de_pass, Role role, String image, String status) {
        this.nom = nom;
        this.prenom = prenom;
        this.e_mail = e_mail;
        this.date_naiss = date_naiss;
        this.num_tel = num_tel;
        this.mot_de_pass = mot_de_pass;
        this.role = role;
        this.image = image;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getDate_naiss() {
        return date_naiss;
    }

    public void setDate_naiss(String date_naiss) {
        this.date_naiss = date_naiss;
    }

    public String getE_mail() {
        return e_mail;
    }

    public void setE_mail(String e_mail) {
        this.e_mail = e_mail;
    }

    public String getNum_tel() {
        return num_tel;
    }

    public void setNum_tel(String num_tel) {
        this.num_tel = num_tel;
    }

    public String getMot_de_pass() {
        return mot_de_pass;
    }

    public void setMot_de_pass(String mot_de_pass) {
        this.mot_de_pass = mot_de_pass;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public  Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", date_naiss='" + date_naiss + '\'' +
                ", e_mail='" + e_mail + '\'' +
                ", num_tel='" + num_tel + '\'' +
                ", mot_de_pass='" + mot_de_pass + '\'' +
                ", image='" + image + '\'' +
                ", role=" + role +
                ", status='" + status + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id && Objects.equals(nom, user.nom) && Objects.equals(prenom, user.prenom) && Objects.equals(date_naiss, user.date_naiss) && Objects.equals(e_mail, user.e_mail) && Objects.equals(num_tel, user.num_tel) && Objects.equals(mot_de_pass, user.mot_de_pass) && Objects.equals(image, user.image) && role == user.role && Objects.equals(status, user.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nom, prenom, date_naiss, e_mail, num_tel, mot_de_pass, image, role, status);
    }
}

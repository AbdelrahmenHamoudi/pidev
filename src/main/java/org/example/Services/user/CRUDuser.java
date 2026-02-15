package org.example.Services.user;

import org.example.Entites.user.User;

import java.sql.SQLException;
import java.util.List;

public interface CRUDuser<T>{
    void createUser(T t) throws SQLException;
    void updateUser(T t) throws SQLException;
    void deleteUser(T t) throws SQLException;
    void updateImageUser(T t) throws SQLException;
    void updatePassword(T t) throws SQLException;
    List<T> getUserByName(String name) throws SQLException;
    List<T> ShowUsers() throws SQLException;
    User signIn(T t) throws SQLException;
}

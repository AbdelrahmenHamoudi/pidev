package org.example.Services;

import java.sql.*;
import java.util.List;

public interface CRUD <T> {

    void ajouterh(T t)throws SQLException;
    void modifierh(T t) throws SQLException;
    void supprimerh(T t)throws SQLException;
    List<T> afficherh()throws SQLException;

}

package org.example.Services.user;

import org.example.Controllers.user.customUserException;
import org.example.Entites.user.Role;
import org.example.Entites.user.Status;
import org.example.Entites.user.User;
import org.example.Utils.MyBD;
import org.example.Utils.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCRUDTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    private UserCRUD userCRUD;
    private MyBD mockMyBD;

    @BeforeEach
    void setUp() throws SQLException {
        mockMyBD = mock(MyBD.class);
        when(mockMyBD.getConnection()).thenReturn(mockConnection);

        try (MockedStatic<MyBD> mockedStatic = mockStatic(MyBD.class)) {
            mockedStatic.when(MyBD::getInstance).thenReturn(mockMyBD);
            userCRUD = new UserCRUD();
        }
    }

    @Test
    void createUser_Success() throws SQLException {
        User user = createTestUser();

        UserCRUD spyUserCRUD = spy(userCRUD);
        doReturn(null).when(spyUserCRUD).getUserByEmail(user.getE_mail());

        when(mockConnection.prepareStatement(Query.addUserQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() -> spyUserCRUD.createUser(user));

        verify(mockPreparedStatement, times(1)).setString(1, user.getNom());
        verify(mockPreparedStatement, times(1)).setString(2, user.getPrenom());
        verify(mockPreparedStatement, times(1)).setString(3, user.getDate_naiss());
        verify(mockPreparedStatement, times(1)).setString(4, user.getE_mail());
        verify(mockPreparedStatement, times(1)).setString(5, user.getNum_tel());
        verify(mockPreparedStatement, times(1)).setString(eq(6), anyString());
        verify(mockPreparedStatement, times(1)).setString(7, user.getImage());
        verify(mockPreparedStatement, times(1)).setString(8, user.getRole().name());
        verify(mockPreparedStatement, times(1)).setString(9, user.getStatus().name());
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void createUser_EmailAlreadyExists_ThrowsException() throws SQLException {
        User user = createTestUser();
        User existingUser = createTestUser();

        UserCRUD spyUserCRUD = spy(userCRUD);
        doReturn(existingUser).when(spyUserCRUD).getUserByEmail(user.getE_mail());

        customUserException exception = assertThrows(customUserException.class,
                () -> spyUserCRUD.createUser(user));
        assertEquals("Cet email est déjà utilisé !", exception.getMessage());

        verify(mockConnection, never()).prepareStatement(anyString());
    }

    @Test
    void createUser_WithNullRoleAndStatus_SetsDefaultValues() throws SQLException {
        User user = createTestUser();
        user.setRole(null);
        user.setStatus(null);

        UserCRUD spyUserCRUD = spy(userCRUD);
        doReturn(null).when(spyUserCRUD).getUserByEmail(user.getE_mail());

        when(mockConnection.prepareStatement(Query.addUserQuery)).thenReturn(mockPreparedStatement);

        spyUserCRUD.createUser(user);

        assertEquals(Role.user, user.getRole());
        assertEquals(Status.Unbanned, user.getStatus());
    }

    @Test
    void updateUser_Success() throws SQLException {
        User user = createTestUser();
        user.setId(1);
        when(mockConnection.prepareStatement(Query.updateUserQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() -> userCRUD.updateUser(user));

        verify(mockPreparedStatement, times(1)).setString(1, user.getNom());
        verify(mockPreparedStatement, times(1)).setString(2, user.getPrenom());
        verify(mockPreparedStatement, times(1)).setString(3, user.getDate_naiss());
        verify(mockPreparedStatement, times(1)).setString(4, user.getE_mail());
        verify(mockPreparedStatement, times(1)).setString(5, user.getNum_tel());
        verify(mockPreparedStatement, times(1)).setString(6, user.getRole().name());
        verify(mockPreparedStatement, times(1)).setString(7, user.getStatus().name());
        verify(mockPreparedStatement, times(1)).setInt(8, user.getId());
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void deleteUser_Success() throws SQLException {
        User user = createTestUser();
        user.setId(1);
        when(mockConnection.prepareStatement(Query.deleteUserQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() -> userCRUD.deleteUser(user));

        verify(mockPreparedStatement, times(1)).setInt(1, user.getId());
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void updateImageUser_Success() throws SQLException {
        User user = createTestUser();
        user.setId(1);
        user.setImage("new-image.jpg");
        when(mockConnection.prepareStatement(Query.updateImageQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() -> userCRUD.updateImageUser(user));

        verify(mockPreparedStatement, times(1)).setString(1, user.getImage());
        verify(mockPreparedStatement, times(1)).setInt(2, user.getId());
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void updatePassword_Success() throws SQLException {
        User user = createTestUser();
        user.setId(1);
        user.setMot_de_pass("newPassword123");
        when(mockConnection.prepareStatement(Query.updatePasswordQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() -> userCRUD.updatePassword(user));

        verify(mockPreparedStatement, times(1)).setString(eq(1), anyString());
        verify(mockPreparedStatement, times(1)).setInt(2, user.getId());
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void getUserByName_ReturnsListOfUsers() throws SQLException {
        String searchName = "John";
        when(mockConnection.prepareStatement(Query.getUserByName)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getInt("id")).thenReturn(1, 2);
        when(mockResultSet.getString("nom")).thenReturn("Doe", "Smith");
        when(mockResultSet.getString("prenom")).thenReturn("John", "Jane");
        when(mockResultSet.getString("date_naiss")).thenReturn("1990-01-01", "1992-02-02");
        when(mockResultSet.getString("e_mail")).thenReturn("john@test.com", "jane@test.com");
        when(mockResultSet.getString("num_tel")).thenReturn("123456789", "987654321");
        when(mockResultSet.getString("mot_de_pass")).thenReturn("hash1", "hash2");
        when(mockResultSet.getString("image")).thenReturn("img1.jpg", "img2.jpg");
        when(mockResultSet.getString("role")).thenReturn("user", "admin");
        when(mockResultSet.getString("status")).thenReturn("Unbanned", "Unbanned");

        List<User> users = userCRUD.getUserByName(searchName);

        assertEquals(2, users.size());
        assertEquals("Doe", users.get(0).getNom());
        assertEquals("Smith", users.get(1).getNom());

        verify(mockPreparedStatement, times(1)).setString(1, "%" + searchName + "%");
    }

    @Test
    void showUsers_ReturnsAllUsers() throws SQLException {
        when(mockConnection.prepareStatement(Query.showUsers)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true, true, true, false);
        when(mockResultSet.getInt("id")).thenReturn(1, 2, 3);
        when(mockResultSet.getString("nom")).thenReturn("Doe", "Smith", "Brown");
        when(mockResultSet.getString("prenom")).thenReturn("John", "Jane", "Bob");
        when(mockResultSet.getString("date_naiss")).thenReturn("1990-01-01", "1992-02-02", "1988-03-03");
        when(mockResultSet.getString("e_mail")).thenReturn("john@test.com", "jane@test.com", "bob@test.com");
        when(mockResultSet.getString("num_tel")).thenReturn("123", "456", "789");
        when(mockResultSet.getString("mot_de_pass")).thenReturn("hash1", "hash2", "hash3");
        when(mockResultSet.getString("image")).thenReturn("img1.jpg", "img2.jpg", "img3.jpg");
        when(mockResultSet.getString("role")).thenReturn("user", "admin", "user");
        when(mockResultSet.getString("status")).thenReturn("Unbanned", "Banned", "Unbanned");

        List<User> users = userCRUD.ShowUsers();

        assertEquals(3, users.size());
        verify(mockPreparedStatement, times(1)).executeQuery();
    }

    @Test
    void getUserByEmail_ReturnsUser() throws SQLException {
        String email = "john@test.com";
        when(mockConnection.prepareStatement(Query.getUserByEmailQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getString("nom")).thenReturn("Doe");
        when(mockResultSet.getString("prenom")).thenReturn("John");
        when(mockResultSet.getString("date_naiss")).thenReturn("1990-01-01");
        when(mockResultSet.getString("e_mail")).thenReturn(email);
        when(mockResultSet.getString("num_tel")).thenReturn("123456789");
        when(mockResultSet.getString("mot_de_pass")).thenReturn("hashedPassword");
        when(mockResultSet.getString("image")).thenReturn("img.jpg");
        when(mockResultSet.getString("role")).thenReturn("user");
        when(mockResultSet.getString("status")).thenReturn("Unbanned");

        User result = userCRUD.getUserByEmail(email);

        assertNotNull(result);
        assertEquals(email, result.getE_mail());
        assertEquals("Doe", result.getNom());

        verify(mockPreparedStatement, times(1)).setString(1, email);
    }

    @Test
    void getUserByEmail_NotFound_ReturnsNull() throws SQLException {
        String email = "nonexistent@test.com";
        when(mockConnection.prepareStatement(Query.getUserByEmailQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        User result = userCRUD.getUserByEmail(email);

        assertNull(result);
    }

    @Test
    void signIn_Success() throws SQLException {
        User inputUser = new User();
        inputUser.setE_mail("john@test.com");
        inputUser.setMot_de_pass("password123");

        when(mockConnection.prepareStatement(Query.signIn)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getString("nom")).thenReturn("Doe");
        when(mockResultSet.getString("prenom")).thenReturn("John");
        when(mockResultSet.getString("date_naiss")).thenReturn("1990-01-01");
        when(mockResultSet.getString("e_mail")).thenReturn("john@test.com");
        when(mockResultSet.getString("num_tel")).thenReturn("123456789");
        when(mockResultSet.getString("mot_de_pass")).thenReturn("hashedPassword");
        when(mockResultSet.getString("image")).thenReturn("img.jpg");
        when(mockResultSet.getString("role")).thenReturn("user");
        when(mockResultSet.getString("status")).thenReturn("Unbanned");

        User result = userCRUD.signIn(inputUser);

        assertNotNull(result);
        assertEquals("john@test.com", result.getE_mail());
    }

    @Test
    void signIn_Failure_ThrowsException() throws SQLException {
        User inputUser = new User();
        inputUser.setE_mail("wrong@test.com");
        inputUser.setMot_de_pass("wrongpassword");

        when(mockConnection.prepareStatement(Query.signIn)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        customUserException exception = assertThrows(customUserException.class,
                () -> userCRUD.signIn(inputUser));
        assertEquals("Email ou mot de passe incorrect !", exception.getMessage());
    }

    private User createTestUser() {
        User user = new User();
        user.setNom("Doe");
        user.setPrenom("John");
        user.setDate_naiss("1990-01-01");
        user.setE_mail("john.doe@test.com");
        user.setNum_tel("0123456789");
        user.setMot_de_pass("password123");
        user.setImage("profile.jpg");
        user.setRole(Role.user);
        user.setStatus(Status.Unbanned);
        return user;
    }
}
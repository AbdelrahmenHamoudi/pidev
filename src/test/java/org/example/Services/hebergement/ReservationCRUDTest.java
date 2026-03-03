package org.example.Services.hebergement;

import org.example.Entites.hebergement.Hebergement;
import org.example.Entites.hebergement.Reservation;
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
class ReservationCRUDTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private Statement mockStatement;

    @Mock
    private ResultSet mockResultSet;

    private ReservationCRUD reservationCRUD;
    private MyBD mockMyBD;

    @BeforeEach
    void setUp() throws SQLException {
        mockMyBD = mock(MyBD.class);
        when(mockMyBD.getConnection()).thenReturn(mockConnection);

        try (MockedStatic<MyBD> mockedStatic = mockStatic(MyBD.class)) {
            mockedStatic.when(MyBD::getInstance).thenReturn(mockMyBD);
            reservationCRUD = new ReservationCRUD();
        }
    }

    @Test
    void ajouterh_Success() throws SQLException {
        Reservation reservation = createTestReservation();

        when(mockConnection.prepareStatement(Query.addReservationQuery))
                .thenReturn(mockPreparedStatement);

        assertDoesNotThrow(() -> reservationCRUD.ajouterh(reservation));

        verify(mockPreparedStatement, times(1)).setInt(1, reservation.getHebergement().getId_hebergement());
        verify(mockPreparedStatement, times(1)).setInt(2, reservation.getUser().getId());
        verify(mockPreparedStatement, times(1)).setString(3, reservation.getDateDebutR());
        verify(mockPreparedStatement, times(1)).setString(4, reservation.getDateFinR());
        verify(mockPreparedStatement, times(1)).setString(5, reservation.getStatutR());
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void ajouterh_ThrowsSQLException() throws SQLException {
        Reservation reservation = createTestReservation();

        when(mockConnection.prepareStatement(Query.addReservationQuery))
                .thenThrow(new SQLException("Database error"));

        assertThrows(SQLException.class, () -> reservationCRUD.ajouterh(reservation));
    }

    @Test
    void modifierh_Success() throws SQLException {
        Reservation reservation = createTestReservation();
        reservation.setId_reservation(1);

        when(mockConnection.prepareStatement(Query.updateReservationQuery))
                .thenReturn(mockPreparedStatement);

        assertDoesNotThrow(() -> reservationCRUD.modifierh(reservation));

        verify(mockPreparedStatement, times(1)).setInt(1, reservation.getHebergement().getId_hebergement());
        verify(mockPreparedStatement, times(1)).setInt(2, reservation.getUser().getId());
        verify(mockPreparedStatement, times(1)).setString(3, reservation.getDateDebutR());
        verify(mockPreparedStatement, times(1)).setString(4, reservation.getDateFinR());
        verify(mockPreparedStatement, times(1)).setString(5, reservation.getStatutR());
        verify(mockPreparedStatement, times(1)).setInt(6, reservation.getId_reservation());
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void modifierh_ThrowsSQLException() throws SQLException {
        Reservation reservation = createTestReservation();

        when(mockConnection.prepareStatement(Query.updateReservationQuery))
                .thenThrow(new SQLException("Database error"));

        assertThrows(SQLException.class, () -> reservationCRUD.modifierh(reservation));
    }

    @Test
    void supprimerh_Success() throws SQLException {
        Reservation reservation = createTestReservation();
        reservation.setId_reservation(1);

        when(mockConnection.prepareStatement(Query.deleteReservationQuery))
                .thenReturn(mockPreparedStatement);

        assertDoesNotThrow(() -> reservationCRUD.supprimerh(reservation));

        verify(mockPreparedStatement, times(1)).setInt(1, reservation.getId_reservation());
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void supprimerh_ThrowsSQLException() throws SQLException {
        Reservation reservation = createTestReservation();

        when(mockConnection.prepareStatement(Query.deleteReservationQuery))
                .thenThrow(new SQLException("Database error"));

        assertThrows(SQLException.class, () -> reservationCRUD.supprimerh(reservation));
    }

    @Test
    void afficherh_ReturnsListOfReservations() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true, true, false);

        when(mockResultSet.getInt("r.id_reservation")).thenReturn(1, 2);
        when(mockResultSet.getString("r.dateDebutR")).thenReturn("2024-01-01", "2024-02-01");
        when(mockResultSet.getString("r.dateFinR")).thenReturn("2024-01-10", "2024-02-10");
        when(mockResultSet.getString("r.statutR")).thenReturn("Confirmée", "En attente");

        when(mockResultSet.getInt("h.id_hebergement")).thenReturn(101, 102);
        when(mockResultSet.getString("h.titre")).thenReturn("Hotel Paris", "Villa Cannes");
        when(mockResultSet.getString("h.desc_hebergement")).thenReturn("Description 1", "Description 2");
        when(mockResultSet.getInt("h.capacite")).thenReturn(2, 6);
        when(mockResultSet.getString("h.type_hebergement")).thenReturn("Hotel", "Villa");
        when(mockResultSet.getBoolean("h.disponible_heberg")).thenReturn(true, false);
        when(mockResultSet.getFloat("h.prixParNuit")).thenReturn(120.5f, 350.0f);
        when(mockResultSet.getString("h.image")).thenReturn("paris.jpg", "cannes.jpg");

        when(mockResultSet.getInt("id_user")).thenReturn(1001, 1002);
        when(mockResultSet.getString("nom")).thenReturn("Doe", "Smith");
        when(mockResultSet.getString("prenom")).thenReturn("John", "Jane");
        when(mockResultSet.getString("email")).thenReturn("john@test.com", "jane@test.com");
        when(mockResultSet.getString("tel")).thenReturn("123456789", "987654321");

        List<Reservation> result = reservationCRUD.afficherh();

        assertEquals(2, result.size());

        Reservation first = result.get(0);
        assertEquals(1, first.getId_reservation());
        assertEquals("2024-01-01", first.getDateDebutR());
        assertEquals("2024-01-10", first.getDateFinR());
        assertEquals("Confirmée", first.getStatutR());
        assertEquals(101, first.getHebergement().getId_hebergement());
        assertEquals("Hotel Paris", first.getHebergement().getTitre());
        assertEquals(1001, first.getUser().getId());
        assertEquals("Doe", first.getUser().getNom());

        Reservation second = result.get(1);
        assertEquals(2, second.getId_reservation());
        assertEquals("2024-02-01", second.getDateDebutR());
        assertEquals("En attente", second.getStatutR());

        verify(mockStatement, times(1)).executeQuery(anyString());
    }

    @Test
    void afficherh_ReturnsEmptyList() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        List<Reservation> result = reservationCRUD.afficherh();

        assertTrue(result.isEmpty());
    }

    @Test
    void afficherh_ThrowsSQLException() throws SQLException {
        when(mockConnection.createStatement()).thenThrow(new SQLException("Database error"));

        assertThrows(SQLException.class, () -> reservationCRUD.afficherh());
    }

    @Test
    void getReservationsByUser_ReturnsListOfReservations() throws SQLException {
        int userId = 1001;

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true, true, false);

        when(mockResultSet.getInt("r.id_reservation")).thenReturn(1, 2);
        when(mockResultSet.getString("r.dateDebutR")).thenReturn("2024-01-01", "2024-02-01");
        when(mockResultSet.getString("r.dateFinR")).thenReturn("2024-01-10", "2024-02-10");
        when(mockResultSet.getString("r.statutR")).thenReturn("Confirmée", "En attente");

        when(mockResultSet.getInt("h.id_hebergement")).thenReturn(101, 102);
        when(mockResultSet.getString("h.titre")).thenReturn("Hotel Paris", "Villa Cannes");
        when(mockResultSet.getString("h.desc_hebergement")).thenReturn("Description 1", "Description 2");
        when(mockResultSet.getInt("h.capacite")).thenReturn(2, 6);
        when(mockResultSet.getString("h.type_hebergement")).thenReturn("Hotel", "Villa");
        when(mockResultSet.getBoolean("h.disponible_heberg")).thenReturn(true, false);
        when(mockResultSet.getFloat("h.prixParNuit")).thenReturn(120.5f, 350.0f);
        when(mockResultSet.getString("h.image")).thenReturn("paris.jpg", "cannes.jpg");

        when(mockResultSet.getInt("id_user")).thenReturn(1001, 1001);
        when(mockResultSet.getString("nom")).thenReturn("Doe", "Doe");
        when(mockResultSet.getString("prenom")).thenReturn("John", "John");
        when(mockResultSet.getString("email")).thenReturn("john@test.com", "john@test.com");
        when(mockResultSet.getString("tel")).thenReturn("123456789", "123456789");

        List<Reservation> result = reservationCRUD.getReservationsByUser(userId);

        assertEquals(2, result.size());

        verify(mockPreparedStatement, times(1)).setInt(1, userId);
        verify(mockPreparedStatement, times(1)).executeQuery();

        for (Reservation r : result) {
            assertEquals(userId, r.getUser().getId());
        }
    }

    @Test
    void getReservationsByUser_ReturnsEmptyList() throws SQLException {
        int userId = 999;

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        List<Reservation> result = reservationCRUD.getReservationsByUser(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getReservationsByUser_ThrowsSQLException() throws SQLException {
        int userId = 1001;

        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));

        assertThrows(SQLException.class, () -> reservationCRUD.getReservationsByUser(userId));
    }

    @Test
    void getReservationsByHebergement_ReturnsListOfReservations() throws SQLException {
        int hebergementId = 101;

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true, true, false);

        when(mockResultSet.getInt("r.id_reservation")).thenReturn(1, 2);
        when(mockResultSet.getString("r.dateDebutR")).thenReturn("2024-01-01", "2024-02-01");
        when(mockResultSet.getString("r.dateFinR")).thenReturn("2024-01-10", "2024-02-10");
        when(mockResultSet.getString("r.statutR")).thenReturn("Confirmée", "Annulée");

        when(mockResultSet.getInt("h.id_hebergement")).thenReturn(101, 101);
        when(mockResultSet.getString("h.titre")).thenReturn("Hotel Paris", "Hotel Paris");
        when(mockResultSet.getString("h.desc_hebergement")).thenReturn("Description", "Description");
        when(mockResultSet.getInt("h.capacite")).thenReturn(2, 2);
        when(mockResultSet.getString("h.type_hebergement")).thenReturn("Hotel", "Hotel");
        when(mockResultSet.getBoolean("h.disponible_heberg")).thenReturn(true, true);
        when(mockResultSet.getFloat("h.prixParNuit")).thenReturn(120.5f, 120.5f);
        when(mockResultSet.getString("h.image")).thenReturn("paris.jpg", "paris.jpg");

        when(mockResultSet.getInt("id_user")).thenReturn(1001, 1002);
        when(mockResultSet.getString("nom")).thenReturn("Doe", "Smith");
        when(mockResultSet.getString("prenom")).thenReturn("John", "Jane");
        when(mockResultSet.getString("email")).thenReturn("john@test.com", "jane@test.com");
        when(mockResultSet.getString("tel")).thenReturn("123456789", "987654321");

        List<Reservation> result = reservationCRUD.getReservationsByHebergement(hebergementId);

        assertEquals(2, result.size());

        verify(mockPreparedStatement, times(1)).setInt(1, hebergementId);
        verify(mockPreparedStatement, times(1)).executeQuery();

        for (Reservation r : result) {
            assertEquals(hebergementId, r.getHebergement().getId_hebergement());
        }
    }

    @Test
    void getReservationsByHebergement_ReturnsEmptyList() throws SQLException {
        int hebergementId = 999;

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        List<Reservation> result = reservationCRUD.getReservationsByHebergement(hebergementId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getReservationsByHebergement_ThrowsSQLException() throws SQLException {
        int hebergementId = 101;

        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));

        assertThrows(SQLException.class, () -> reservationCRUD.getReservationsByHebergement(hebergementId));
    }

    private Reservation createTestReservation() {
        User user = new User();
        user.setId(1);
        user.setNom("Doe");
        user.setPrenom("John");
        user.setE_mail("john@test.com");
        user.setNum_tel("123456789");

        Hebergement hebergement = new Hebergement();
        hebergement.setId_hebergement(1);
        hebergement.setTitre("Hotel Test");
        hebergement.setDesc_hebergement("Description test");
        hebergement.setCapacite(4);
        hebergement.setType_hebergement("Hotel");
        hebergement.setDisponible_heberg(true);
        hebergement.setPrixParNuit(150.0f);
        hebergement.setImage("test.jpg");

        Reservation reservation = new Reservation();
        reservation.setHebergement(hebergement);
        reservation.setUser(user);
        reservation.setDateDebutR("2024-01-01");
        reservation.setDateFinR("2024-01-10");
        reservation.setStatutR("Confirmée");

        return reservation;
    }
}
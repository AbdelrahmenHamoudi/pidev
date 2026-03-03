package org.example.Services.hebergement;

import org.example.Entites.hebergement.Hebergement;
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
class HebergementCRUDTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private Statement mockStatement;

    @Mock
    private ResultSet mockResultSet;

    private HebergementCRUD hebergementCRUD;
    private MyBD mockMyBD;

    @BeforeEach
    void setUp() throws SQLException {
        mockMyBD = mock(MyBD.class);
        when(mockMyBD.getConnection()).thenReturn(mockConnection);

        try (MockedStatic<MyBD> mockedStatic = mockStatic(MyBD.class)) {
            mockedStatic.when(MyBD::getInstance).thenReturn(mockMyBD);
            hebergementCRUD = new HebergementCRUD();
        }
    }

    @Test
    void ajouterh_Success() throws SQLException {
        Hebergement hebergement = createTestHebergement();

        when(mockConnection.prepareStatement(Query.addhebergementQuery, Statement.RETURN_GENERATED_KEYS))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(1);

        assertDoesNotThrow(() -> hebergementCRUD.ajouterh(hebergement));

        verify(mockPreparedStatement, times(1)).setString(1, hebergement.getTitre());
        verify(mockPreparedStatement, times(1)).setString(2, hebergement.getDesc_hebergement());
        verify(mockPreparedStatement, times(1)).setInt(3, hebergement.getCapacite());
        verify(mockPreparedStatement, times(1)).setString(4, hebergement.getType_hebergement());
        verify(mockPreparedStatement, times(1)).setBoolean(5, hebergement.isDisponible_heberg());
        verify(mockPreparedStatement, times(1)).setFloat(6, hebergement.getPrixParNuit());
        verify(mockPreparedStatement, times(1)).setString(7, hebergement.getImage());
        verify(mockPreparedStatement, times(1)).executeUpdate();

        assertEquals(1, hebergement.getId_hebergement());
    }

    @Test
    void ajouterh_WithoutGeneratedKeys() throws SQLException {
        Hebergement hebergement = createTestHebergement();

        when(mockConnection.prepareStatement(Query.addhebergementQuery, Statement.RETURN_GENERATED_KEYS))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        assertDoesNotThrow(() -> hebergementCRUD.ajouterh(hebergement));

        assertEquals(0, hebergement.getId_hebergement());
    }

    @Test
    void modifierh_Success() throws SQLException {
        Hebergement hebergement = createTestHebergement();
        hebergement.setId_hebergement(1);

        when(mockConnection.prepareStatement(Query.updatehebergementQuery))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() -> hebergementCRUD.modifierh(hebergement));

        verify(mockPreparedStatement, times(1)).setString(1, hebergement.getTitre());
        verify(mockPreparedStatement, times(1)).setString(2, hebergement.getDesc_hebergement());
        verify(mockPreparedStatement, times(1)).setInt(3, hebergement.getCapacite());
        verify(mockPreparedStatement, times(1)).setString(4, hebergement.getType_hebergement());
        verify(mockPreparedStatement, times(1)).setBoolean(5, hebergement.isDisponible_heberg());
        verify(mockPreparedStatement, times(1)).setFloat(6, hebergement.getPrixParNuit());
        verify(mockPreparedStatement, times(1)).setString(7, hebergement.getImage());
        verify(mockPreparedStatement, times(1)).setInt(8, hebergement.getId_hebergement());
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void supprimerh_Success() throws SQLException {
        Hebergement hebergement = createTestHebergement();
        hebergement.setId_hebergement(1);

        when(mockConnection.prepareStatement(Query.deletehebergementQuery))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() -> hebergementCRUD.supprimerh(hebergement));

        verify(mockPreparedStatement, times(1)).setInt(1, hebergement.getId_hebergement());
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void afficherh_ReturnsListOfHebergements() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(Query.showhebergementQuery)).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true, true, true, false);
        when(mockResultSet.getInt("id_hebergement")).thenReturn(1, 2, 3);
        when(mockResultSet.getString("titre")).thenReturn("Hotel Paris", "Villa Cannes", "Appartement Lyon");
        when(mockResultSet.getString("desc_hebergement")).thenReturn("Description 1", "Description 2", "Description 3");
        when(mockResultSet.getInt("capacite")).thenReturn(2, 6, 4);
        when(mockResultSet.getString("type_hebergement")).thenReturn("Hotel", "Villa", "Appartement");
        when(mockResultSet.getBoolean("disponible_heberg")).thenReturn(true, false, true);
        when(mockResultSet.getFloat("prixParNuit")).thenReturn(120.5f, 350.0f, 85.0f);
        when(mockResultSet.getString("image")).thenReturn("paris.jpg", "cannes.jpg", "lyon.jpg");

        List<Hebergement> result = hebergementCRUD.afficherh();

        assertEquals(3, result.size());

        Hebergement first = result.get(0);
        assertEquals(1, first.getId_hebergement());
        assertEquals("Hotel Paris", first.getTitre());
        assertEquals("Description 1", first.getDesc_hebergement());
        assertEquals(2, first.getCapacite());
        assertEquals("Hotel", first.getType_hebergement());
        assertTrue(first.isDisponible_heberg());
        assertEquals(120.5f, first.getPrixParNuit());
        assertEquals("paris.jpg", first.getImage());

        Hebergement second = result.get(1);
        assertEquals(2, second.getId_hebergement());
        assertEquals("Villa Cannes", second.getTitre());
        assertFalse(second.isDisponible_heberg());

        verify(mockStatement, times(1)).executeQuery(Query.showhebergementQuery);
    }

    @Test
    void afficherh_ReturnsEmptyList() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(Query.showhebergementQuery)).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        List<Hebergement> result = hebergementCRUD.afficherh();

        assertTrue(result.isEmpty());
    }

    @Test
    void ajouterh_ThrowsSQLException() throws SQLException {
        Hebergement hebergement = createTestHebergement();

        when(mockConnection.prepareStatement(Query.addhebergementQuery, Statement.RETURN_GENERATED_KEYS))
                .thenThrow(new SQLException("Database error"));

        assertThrows(SQLException.class, () -> hebergementCRUD.ajouterh(hebergement));
    }

    @Test
    void modifierh_ThrowsSQLException() throws SQLException {
        Hebergement hebergement = createTestHebergement();

        when(mockConnection.prepareStatement(Query.updatehebergementQuery))
                .thenThrow(new SQLException("Database error"));

        assertThrows(SQLException.class, () -> hebergementCRUD.modifierh(hebergement));
    }

    @Test
    void supprimerh_ThrowsSQLException() throws SQLException {
        Hebergement hebergement = createTestHebergement();

        when(mockConnection.prepareStatement(Query.deletehebergementQuery))
                .thenThrow(new SQLException("Database error"));

        assertThrows(SQLException.class, () -> hebergementCRUD.supprimerh(hebergement));
    }

    @Test
    void afficherh_ThrowsSQLException() throws SQLException {
        when(mockConnection.createStatement()).thenThrow(new SQLException("Database error"));

        assertThrows(SQLException.class, () -> hebergementCRUD.afficherh());
    }

    private Hebergement createTestHebergement() {
        Hebergement h = new Hebergement();
        h.setTitre("Hotel Test");
        h.setDesc_hebergement("Description test");
        h.setCapacite(4);
        h.setType_hebergement("Hotel");
        h.setDisponible_heberg(true);
        h.setPrixParNuit(150.0f);
        h.setImage("test.jpg");
        return h;
    }
}
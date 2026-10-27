package com.cmpe172.game_coaching_booking.observability;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class ManagementControllerTest {
    @Test
    void databaseFailureReturns503WithoutLeakingConnectionDetails() throws Exception {
        DataSource source = mock(DataSource.class);
        when(source.getConnection()).thenThrow(new SQLException("private database details"));
        var response = new ManagementController(source, new SchedulerMetrics()).health();
        assertEquals(503, response.getStatusCode().value());
        assertEquals("DOWN", response.getBody().get("database"));
        assertFalse(response.getBody().toString().contains("private"));
    }

    @Test
    void healthyConnectionIsClosedAfterProbe() throws Exception {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);
        assertEquals(200, new ManagementController(source, new SchedulerMetrics()).health().getStatusCode().value());
        verify(connection).close();
    }
}

package ar.edu.um.turnos.appointment.reservation.infrastructure.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationConfirmResult;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationHold;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import ar.edu.um.turnos.appointment.shared.technical.CatedraTechnicalClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentHoldGatewayAdapterTest {

    @Mock private CatedraTechnicalClient technicalClient;
    private AppointmentHoldGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AppointmentHoldGatewayAdapter(technicalClient, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void createsHoldUsingTheContractAndReturnsPersistableRequestPayload() throws Exception {
        when(technicalClient.post(eq("/api/appointment-holds"), any()))
                .thenReturn("""
                        {"holdId":"hold-1","reservationProcessId":"process-1",
                         "status":"HELD","expiresAt":"2026-10-26T10:00:00Z"}
                        """);

        ReservationHold hold = adapter.createHold(
                15, LocalDate.parse("2026-10-26"), LocalTime.parse("09:30"), "user-1");

        assertEquals("hold-1", hold.holdId());
        assertEquals("process-1", hold.reservationProcessId());
        var request = new ObjectMapper().findAndRegisterModules().readTree(hold.requestPayload());
        assertEquals(15, request.get("professionalId").asLong());
        assertEquals("2026-10-26", request.get("date").asText());
        assertEquals("09:30", request.get("startTime").asText());
        assertEquals("user-1", request.get("externalPatientId").asText());
    }

    @Test
    void confirmsHoldWithTechnicalEndpointAndUserNames() {
        when(technicalClient.post(eq("/api/appointment-holds/hold-1/confirm"), any()))
                .thenReturn("""
                        {"reservationProcessId":"process-1","status":"WAITING_FOR_PHONE",
                         "expiresAt":"2026-10-26T10:00:00Z"}
                        """);

        ReservationConfirmResult result = adapter.confirmHold(
                "hold-1", "process-1", "user-1", "Juan", "Perez");

        assertEquals("WAITING_FOR_PHONE", result.status());
        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(technicalClient).post(eq("/api/appointment-holds/hold-1/confirm"), body.capture());
        var request = new ObjectMapper().valueToTree(body.getValue());
        assertEquals("user-1", request.get("externalPatientId").asText());
        assertEquals("Juan", request.get("patientFirstName").asText());
        assertEquals("Perez", request.get("patientLastName").asText());
    }

    @Test
    void reconcilesAmbiguousConfirmationWithoutRepeatingPost() {
        when(technicalClient.post(eq("/api/appointment-holds/hold-1/confirm"), any()))
                .thenThrow(new CatedraClientException("CATEDRA_UNAVAILABLE", 503, "unavailable"));
        when(technicalClient.get(eq("/api/appointments?externalPatientId=user-1&page=0&size=100")))
                .thenReturn("""
                        [{"reservationProcessId":"process-1","status":"PHONE_PENDING"}]
                        """);

        ReservationConfirmResult result = adapter.confirmHold(
                "hold-1", "process-1", "user-1", "Juan", "Perez");

        assertEquals("WAITING_FOR_PHONE", result.status());
        verify(technicalClient).post(eq("/api/appointment-holds/hold-1/confirm"), any());
        verify(technicalClient).get(eq("/api/appointments?externalPatientId=user-1&page=0&size=100"));
    }

    @Test
    void leavesAmbiguousConfirmationUnresolvedWhenRemoteProcessIsAbsent() {
        CatedraClientException timeout = new CatedraClientException("CATEDRA_UNAVAILABLE", 503, "unavailable");
        when(technicalClient.post(eq("/api/appointment-holds/hold-1/confirm"), any())).thenThrow(timeout);
        when(technicalClient.get(any())).thenReturn("[]");

        assertThrows(CatedraClientException.class,
                () -> adapter.confirmHold("hold-1", "process-1", "user-1", "Juan", "Perez"));
        verify(technicalClient).post(eq("/api/appointment-holds/hold-1/confirm"), any());
    }
}

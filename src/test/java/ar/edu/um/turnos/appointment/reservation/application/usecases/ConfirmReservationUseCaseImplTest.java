package ar.edu.um.turnos.appointment.reservation.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.turnos.appointment.reservation.application.exception.ReservationException;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationConfirmResult;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcessStatus;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.AppointmentHoldGateway;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.ReservationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfirmReservationUseCaseImplTest {

    private static final UUID OWNER = UUID.fromString("a43d7a0b-fec6-4f3a-98c6-d2e283194bd9");
    private static final Instant NOW = Instant.parse("2026-10-25T12:00:00Z");

    @Mock private ReservationRepository reservations;
    @Mock private AppointmentHoldGateway catedra;
    private ConfirmReservationUseCaseImpl useCase;
    private ReservationProcess held;

    @BeforeEach
    void setUp() {
        useCase = new ConfirmReservationUseCaseImpl(
                reservations, catedra, Clock.fixed(NOW, ZoneOffset.UTC));
        held = ReservationProcess.created(OWNER, 15, LocalDate.parse("2026-10-26"),
                        LocalTime.parse("09:00"), LocalTime.parse("09:30"), "Ana", "Gomez", "{}", NOW)
                .withId(8L)
                .held("process-1", "hold-1", NOW.plusSeconds(600), "{}", NOW);
        when(reservations.findProcess("process-1")).thenReturn(Optional.of(held));
    }

    @Test
    void confirmsOwnedHoldWithNamesFromAuthenticatedClaims() {
        ReservationConfirmResult result = new ReservationConfirmResult(
                "process-1", "WAITING_FOR_PHONE", NOW.plusSeconds(500), "confirm-payload");
        when(catedra.confirmHold("hold-1", "process-1", OWNER.toString(), "Juan", "Perez"))
                .thenReturn(result);

        ReservationConfirmResult response = useCase.confirm("process-1", OWNER, "Juan", "Perez");

        assertEquals("WAITING_FOR_PHONE", response.status());
        verify(reservations).saveConfirmation(
                org.mockito.ArgumentMatchers.argThat(process ->
                        process.status() == ReservationProcessStatus.WAITING_FOR_PHONE
                                && "Juan".equals(process.patientFirstName())
                                && "confirm-payload".equals(process.confirmRequestPayload())),
                org.mockito.ArgumentMatchers.argThat(reservation ->
                        "PHONE_PENDING".equals(reservation.status())
                                && OWNER.equals(reservation.ownerPublicId())));
    }

    @Test
    void hidesHoldOwnedByAnotherUser() {
        UUID anotherUser = UUID.randomUUID();
        assertEquals("HOLD_NOT_FOUND", assertThrows(ReservationException.class,
                () -> useCase.confirm("process-1", anotherUser, "Juan", "Perez")).getCode());
        verify(catedra, never()).confirmHold(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void refusesToInventMissingUserProfileNames() {
        assertEquals("USER_PROFILE_CLAIMS_MISSING", assertThrows(ReservationException.class,
                () -> useCase.confirm("process-1", OWNER, null, null)).getCode());
        verify(catedra, never()).confirmHold(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsExpiredHoldWithoutCallingCatedra() {
        ReservationProcess expired = ReservationProcess.created(OWNER, 15,
                        LocalDate.parse("2026-10-26"), LocalTime.parse("09:00"),
                        LocalTime.parse("09:30"), "Ana", "Gomez", "{}", NOW)
                .withId(8L)
                .held("process-1", "hold-1", NOW, "{}", NOW);
        when(reservations.findProcess("process-1")).thenReturn(Optional.of(expired));

        assertEquals("HOLD_EXPIRED", assertThrows(ReservationException.class,
                () -> useCase.confirm("process-1", OWNER, "Juan", "Perez")).getCode());
        verify(catedra, never()).confirmHold(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}

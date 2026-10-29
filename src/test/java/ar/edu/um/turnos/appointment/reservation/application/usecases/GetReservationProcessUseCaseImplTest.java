package ar.edu.um.turnos.appointment.reservation.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.turnos.appointment.reservation.application.exception.ReservationException;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionStatus;
import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.ReservationRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetReservationProcessUseCaseImplTest {

    private static final UUID OWNER = UUID.fromString("a43d7a0b-fec6-4f3a-98c6-d2e283194bd9");
    @Mock private ReservationRepository reservations;
    private GetReservationProcessUseCaseImpl useCase;
    private ReservationProcess process;

    @BeforeEach
    void setUp() {
        useCase = new GetReservationProcessUseCaseImpl(reservations);
        Instant now = Instant.parse("2026-10-25T12:00:00Z");
        process = ReservationProcess.created(OWNER, 15, LocalDate.parse("2026-10-26"),
                        LocalTime.parse("09:00"), LocalTime.parse("09:30"), "Ana", "Gomez", "{}", now)
                .withId(9L)
                .held("process-1", "hold-1", now.plusSeconds(900), "{}", now);
    }

    @Test
    void returnsOnlyClientSafeStateForTheOwner() {
        Reservation reservation = new Reservation(3L, 9L, 8251L, "process-1", OWNER, OWNER.toString(), 15,
                LocalDate.parse("2026-10-26"), LocalTime.parse("09:00"), LocalTime.parse("09:30"),
                "Ana", "Gomez", "Juan", "Perez", null, PhoneSubmissionStatus.PENDING,
                "event-1", "PHONE_PENDING", process.createdAt(), null, null, null);
        when(reservations.findProcess("process-1")).thenReturn(Optional.of(process));
        when(reservations.findReservationByProcessId(9L)).thenReturn(Optional.of(reservation));

        var details = useCase.get("process-1", OWNER);

        assertEquals("HELD", details.status());
        assertEquals("PENDING", details.phoneSubmissionStatus());
        assertEquals(8251L, details.reservationId());
        assertNull(details.error());
    }

    @Test
    void doesNotRevealAProcessToAnotherUser() {
        when(reservations.findProcess("process-1")).thenReturn(Optional.of(process));

        ReservationException failure = assertThrows(ReservationException.class,
                () -> useCase.get("process-1", UUID.randomUUID()));

        assertEquals("RESERVATION_NOT_FOUND", failure.getCode());
        verify(reservations).findProcess("process-1");
        verify(reservations, never()).findReservationByProcessId(9L);
    }
}

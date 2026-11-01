package ar.edu.um.turnos.appointment.reservation.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.turnos.appointment.reservation.application.exception.ReservationException;
import ar.edu.um.turnos.appointment.reservation.domain.model.AppointmentCancellationResult;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionStatus;
import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.AppointmentCancellationGateway;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.ReservationRepository;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelReservationUseCaseImplTest {

    private static final UUID OWNER = UUID.fromString("a43d7a0b-fec6-4f3a-98c6-d2e283194bd9");
    private static final UUID OTHER_OWNER = UUID.fromString("b43d7a0b-fec6-4f3a-98c6-d2e283194bd9");
    private static final Instant NOW = Instant.parse("2026-10-26T08:00:00Z");

    @Mock private ReservationRepository reservations;
    @Mock private AppointmentCancellationGateway appointmentCancellation;
    @InjectMocks private CancelReservationUseCaseImpl useCase;

    private Reservation confirmedReservation;
    private ReservationProcess confirmedProcess;

    @BeforeEach
    void setUp() {
        confirmedReservation = reservation(OWNER, "CONFIRMED");
        confirmedProcess = ReservationProcess.created(OWNER, 15, LocalDate.parse("2026-10-26"),
                        LocalTime.parse("09:00"), LocalTime.parse("09:30"), "Ana", "Gomez", "{}", NOW)
                .withId(9L)
                .held("process-1", "hold-1", NOW.plusSeconds(600), "{}", NOW)
                .waitingForPhone("Juan", "Perez", "{}", NOW.plusSeconds(600), NOW)
                .apply(new ar.edu.um.turnos.appointment.reservation.domain.model.ReservationEvent(
                        "confirm-event", "AppointmentConfirmed", "process-1", "hold-1", "request-1",
                        8251L, 15L, "2026-10-26", "09:00", "CONFIRMED", null, null,
                        null, NOW, 1), NOW);
    }

    @Test
    void verifiesOwnershipCallsCatedraAndPersistsCancellationOnlyAfterSuccess() {
        when(reservations.findReservationByReservationProcessId("process-1"))
                .thenReturn(Optional.of(confirmedReservation));
        when(reservations.findProcess("process-1")).thenReturn(Optional.of(confirmedProcess));
        when(appointmentCancellation.cancel("process-1", "Cambio de planes"))
                .thenReturn(new AppointmentCancellationResult(8251L, "process-1", "CANCELLED", NOW));

        Reservation result = useCase.cancel("process-1", OWNER, "  Cambio de planes  ");

        assertThat(result.status()).isEqualTo("CANCELLED");
        assertThat(result.cancelledAt()).isEqualTo(NOW);
        assertThat(result.cancellationReason()).isEqualTo("Cambio de planes");
        verify(appointmentCancellation).cancel("process-1", "Cambio de planes");
        verify(reservations).saveCancellation(
                org.mockito.ArgumentMatchers.argThat(process -> process.status().name().equals("CANCELLED")),
                org.mockito.ArgumentMatchers.argThat(reservation -> "CANCELLED".equals(reservation.status())
                        && "Cambio de planes".equals(reservation.cancellationReason())));
    }

    @Test
    void hidesMissingAndForeignReservationsWithTheSameNotFoundResponse() {
        when(reservations.findReservationByReservationProcessId("missing")).thenReturn(Optional.empty());
        when(reservations.findReservationByReservationProcessId("process-1"))
                .thenReturn(Optional.of(reservation(OTHER_OWNER, "CONFIRMED")));

        assertNotFound(() -> useCase.cancel("missing", OWNER, null));
        assertNotFound(() -> useCase.cancel("process-1", OWNER, null));
        verify(appointmentCancellation, never()).cancel(any(), any());
        verify(reservations, never()).saveCancellation(any(), any());
    }

    @Test
    void returnsAnAlreadyCancelledReservationWithoutCallingCatedraAgain() {
        Reservation cancelled = confirmedReservation.cancelled(NOW, "already cancelled");
        when(reservations.findReservationByReservationProcessId("process-1"))
                .thenReturn(Optional.of(cancelled));

        assertThat(useCase.cancel("process-1", OWNER, "new reason")).isEqualTo(cancelled);
        verify(appointmentCancellation, never()).cancel(any(), any());
        verify(reservations, never()).saveCancellation(any(), any());
    }

    @Test
    void rejectsReservationsThatAreNotConfirmedAndDoesNotPersistWhenCatedraFails() {
        when(reservations.findReservationByReservationProcessId("process-1"))
                .thenReturn(Optional.of(reservation(OWNER, "PHONE_PENDING")));
        assertThatThrownBy(() -> useCase.cancel("process-1", OWNER, null))
                .isInstanceOf(ReservationException.class)
                .satisfies(failure -> assertThat(((ReservationException) failure).getCode())
                        .isEqualTo("RESERVATION_NOT_CANCELLABLE"));
        verify(appointmentCancellation, never()).cancel(any(), any());

        when(reservations.findReservationByReservationProcessId("process-1"))
                .thenReturn(Optional.of(confirmedReservation));
        when(reservations.findProcess("process-1")).thenReturn(Optional.of(confirmedProcess));
        when(appointmentCancellation.cancel("process-1", null)).thenThrow(
                new CatedraClientException("CATEDRA_UNAVAILABLE", 503, "Unavailable."));
        assertThatThrownBy(() -> useCase.cancel("process-1", OWNER, null))
                .isInstanceOf(CatedraClientException.class);
        verify(reservations, never()).saveCancellation(any(), any());
    }

    @Test
    void refusesAProcessOwnedBySomeoneElseEvenIfReservationProjectionIsInconsistent() {
        when(reservations.findReservationByReservationProcessId("process-1"))
                .thenReturn(Optional.of(confirmedReservation));
        ReservationProcess otherProcess = new ReservationProcess(9L, "process-1", "hold-1", OTHER_OWNER,
                confirmedProcess.professionalId(), confirmedProcess.appointmentDate(), confirmedProcess.startTime(),
                confirmedProcess.endTime(), "Ana", "Gomez", "Juan", "Perez", confirmedProcess.status(),
                confirmedProcess.expiresAt(), confirmedProcess.requestEventId(), confirmedProcess.reservationId(),
                confirmedProcess.resultStatus(), null, "{}", "{}", NOW, NOW);
        when(reservations.findProcess("process-1")).thenReturn(Optional.of(otherProcess));

        assertNotFound(() -> useCase.cancel("process-1", OWNER, null));
        verify(appointmentCancellation, never()).cancel(any(), any());
    }

    private static Reservation reservation(UUID owner, String status) {
        return new Reservation(4L, 9L, 8251L, "process-1", owner, owner.toString(), 15,
                LocalDate.parse("2026-10-26"), LocalTime.parse("09:00"), LocalTime.parse("09:30"),
                "Ana", "Gomez", "Juan", "Perez", null, PhoneSubmissionStatus.NOT_SENT, null,
                status, NOW.minusSeconds(3600), NOW.minusSeconds(3000), null, null);
    }

    private static void assertNotFound(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(ReservationException.class)
                .satisfies(failure -> {
                    ReservationException reservationFailure = (ReservationException) failure;
                    assertThat(reservationFailure.getHttpStatus()).isEqualTo(404);
                    assertThat(reservationFailure.getCode()).isEqualTo("RESERVATION_NOT_FOUND");
                });
    }
}

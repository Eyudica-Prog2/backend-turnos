package ar.edu.um.turnos.appointment.reservation.application.usecases;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationEvent;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionStatus;
import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
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
class ProcessReservationEventUseCaseImplTest {

    private static final UUID OWNER = UUID.fromString("a43d7a0b-fec6-4f3a-98c6-d2e283194bd9");
    private static final Instant NOW = Instant.parse("2026-10-25T12:00:00Z");

    @Mock private ReservationRepository reservations;
    private ProcessReservationEventUseCaseImpl useCase;
    private ReservationProcess held;

    @BeforeEach
    void setUp() {
        useCase = new ProcessReservationEventUseCaseImpl(
                reservations, Clock.fixed(NOW, ZoneOffset.UTC));
        held = ReservationProcess.created(OWNER, 15, LocalDate.parse("2026-10-26"),
                        LocalTime.parse("09:00"), LocalTime.parse("09:30"), "Ana", "Gomez", "{}", NOW)
                .withId(8L)
                .held("process-1", "hold-1", NOW.plusSeconds(600), "{}", NOW);
    }

    @Test
    void appliesAdditionalInformationRequestAndPersistsItBeforeAckCanHappen() {
        ReservationEvent event = event("AdditionalInformationRequested", "event-1", "hold-1",
                null, null, null, "PHONE_PENDING", null, NOW.plusSeconds(600));
        when(reservations.registerEventIfNew("event-1", event.eventType(), NOW)).thenReturn(true);
        when(reservations.findProcess("process-1")).thenReturn(Optional.of(held));
        when(reservations.findReservationByProcessId(8L)).thenReturn(Optional.empty());

        useCase.process(event);

        verify(reservations).saveProcess(org.mockito.ArgumentMatchers.argThat(process ->
                process.status().name().equals("WAITING_FOR_PHONE")
                        && "event-1".equals(process.requestEventId())));
        verify(reservations).saveReservation(org.mockito.ArgumentMatchers.argThat(reservation ->
                "PHONE_PENDING".equals(reservation.status()) && OWNER.equals(reservation.ownerPublicId())));
    }

    @Test
    void ignoresAnAlreadyProcessedEvent() {
        ReservationEvent event = event("AdditionalInformationRequested", "event-1", "hold-1",
                null, null, null, "PHONE_PENDING", null, NOW.plusSeconds(600));
        when(reservations.registerEventIfNew("event-1", event.eventType(), NOW)).thenReturn(false);

        useCase.process(event);

        verify(reservations, never()).findProcess("process-1");
        verify(reservations, never()).saveProcess(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void lateRequestCannotReopenAConfirmedProcess() {
        ReservationProcess confirmed = held.apply(event("AppointmentConfirmed", "confirm-event", "hold-1",
                8251L, 15L, "2026-10-26", "CONFIRMED", null, null), NOW);
        ReservationEvent late = event("AdditionalInformationRequested", "late-event", "hold-1",
                null, null, null, "PHONE_PENDING", null, NOW.plusSeconds(600));
        when(reservations.registerEventIfNew("late-event", late.eventType(), NOW)).thenReturn(true);
        when(reservations.findProcess("process-1")).thenReturn(Optional.of(confirmed));

        useCase.process(late);

        verify(reservations, never()).saveProcess(org.mockito.ArgumentMatchers.any());
        verify(reservations, never()).saveReservation(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectedPhoneResetsLocalSubmissionStateForAUserRetry() {
        ReservationEvent event = event("AdditionalInformationRejected", "reject-event", "hold-1",
                null, null, null, null, "INVALID_PHONE_NUMBER", NOW.plusSeconds(600));
        Reservation submitted = new Reservation(3L, 8L, null, "process-1", OWNER, OWNER.toString(), 15,
                LocalDate.parse("2026-10-26"), LocalTime.parse("09:00"), LocalTime.parse("09:30"),
                "Ana", "Gomez", "Juan", "Perez", "+54900000000", PhoneSubmissionStatus.SENT,
                "phone-event", "PHONE_PENDING", NOW, null, null, null);
        when(reservations.registerEventIfNew("reject-event", event.eventType(), NOW)).thenReturn(true);
        when(reservations.findProcess("process-1")).thenReturn(Optional.of(held));
        when(reservations.findReservationByProcessId(8L)).thenReturn(Optional.of(submitted));

        useCase.process(event);

        verify(reservations).saveReservation(org.mockito.ArgumentMatchers.argThat(reservation ->
                reservation.phoneSubmissionStatus() == PhoneSubmissionStatus.NOT_SENT
                        && reservation.patientPhone() == null));
    }

    @Test
    void rejectsUnsupportedSchemaBeforeRegisteringEvent() {
        ReservationEvent invalid = new ReservationEvent("event-1", "AppointmentProcessExpired",
                "process-1", "hold-1", null, null, null, null, null, null,
                "HOLD_EXPIRED", null, null, NOW, 2);

        assertThrows(IllegalArgumentException.class, () -> useCase.process(invalid));
        verify(reservations, never()).registerEventIfNew(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    private static ReservationEvent event(String type, String eventId, String holdId,
                                          Long reservationId, Long professionalId, String date,
                                          String status, String reason, Instant expiresAt) {
        return new ReservationEvent(eventId, type, "process-1", holdId, "request-1", reservationId,
                professionalId, date, date == null ? null : "09:00", status, reason,
                "message", expiresAt, NOW, 1);
    }
}

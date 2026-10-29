package ar.edu.um.turnos.appointment.reservation.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.turnos.appointment.reservation.application.exception.ReservationException;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmission;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionConfiguration;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionStatus;
import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationEvent;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcessStatus;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.PhoneSubmissionConfigurationGateway;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.PhoneSubmissionGateway;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmitReservationPhoneUseCaseImplTest {

    private static final UUID OWNER = UUID.fromString("a43d7a0b-fec6-4f3a-98c6-d2e283194bd9");
    private static final Instant NOW = Instant.parse("2026-10-25T12:00:00Z");
    private static final Instant EXPIRATION = NOW.plusSeconds(900);

    @Mock private ReservationRepository reservations;
    @Mock private PhoneSubmissionConfigurationGateway configurationGateway;
    @Mock private PhoneSubmissionGateway phoneSubmissionGateway;

    private SubmitReservationPhoneUseCaseImpl useCase;
    private ReservationProcess waitingProcess;
    private Reservation reservation;
    private PhoneSubmissionConfiguration configuration;

    @BeforeEach
    void setUp() {
        useCase = new SubmitReservationPhoneUseCaseImpl(reservations, configurationGateway,
                phoneSubmissionGateway, Clock.fixed(NOW, ZoneOffset.UTC));
        waitingProcess = ReservationProcess.created(OWNER, 15,
                        LocalDate.parse("2026-10-26"), LocalTime.parse("09:00"),
                        LocalTime.parse("09:30"), "Ana", "Gomez", "{}", NOW)
                .withId(9L)
                .held("process-1", "hold-1", EXPIRATION, "{}", NOW)
                .waitingForPhone("Juan", "Perez", "{}", EXPIRATION, NOW)
                .apply(new ReservationEvent("request-event-1", "AdditionalInformationRequested",
                        "process-1", "hold-1", null, null, null, null, null,
                        null, null, null, EXPIRATION, NOW, 1), NOW);
        reservation = new Reservation(3L, 9L, null, "process-1", OWNER, OWNER.toString(), 15,
                LocalDate.parse("2026-10-26"), LocalTime.parse("09:00"), LocalTime.parse("09:30"),
                "Ana", "Gomez", "Juan", "Perez", null, PhoneSubmissionStatus.NOT_SENT,
                null, "PHONE_PENDING", NOW, null, null, null);
        configuration = new PhoneSubmissionConfiguration(
                "group-1", "kafka.example.test:9092", "catedra.turnos.telefono.group-1");
        lenient().when(reservations.findProcess("process-1")).thenReturn(Optional.of(waitingProcess));
        lenient().when(reservations.findReservationByProcessId(9L)).thenReturn(Optional.of(reservation));
        lenient().when(configurationGateway.currentConfiguration()).thenReturn(configuration);
        lenient().when(reservations.beginPhoneSubmission(
                any(Reservation.class), eq("request-event-1"), any(Instant.class)))
                .thenReturn(true);
        lenient().when(reservations.markPhoneSubmissionSent(any(Reservation.class))).thenReturn(true);
    }

    @Test
    void normalizesPersistsAndPublishesThePhoneWithTheCurrentRequestEvent() {
        var result = useCase.submit("process-1", OWNER, " +54 (9) 261-555.1234 ");

        assertEquals("process-1", result.reservationProcessId());
        assertEquals("SENT", result.phoneSubmissionStatus());
        ArgumentCaptor<PhoneSubmission> submission = ArgumentCaptor.forClass(PhoneSubmission.class);
        verify(phoneSubmissionGateway).publish(submission.capture(), org.mockito.ArgumentMatchers.eq(configuration));
        assertEquals("process-1", submission.getValue().reservationProcessId());
        assertEquals("hold-1", submission.getValue().holdId());
        assertEquals("+5492615551234", submission.getValue().phoneNumber());
        assertEquals("request-event-1", submission.getValue().requestEventId());
        verify(reservations).beginPhoneSubmission(org.mockito.ArgumentMatchers.argThat(pending ->
                pending.phoneSubmissionStatus() == PhoneSubmissionStatus.PENDING
                        && "+5492615551234".equals(pending.patientPhone())),
                eq("request-event-1"), eq(NOW));
        verify(reservations).markPhoneSubmissionSent(org.mockito.ArgumentMatchers.argThat(sent ->
                sent.phoneSubmissionStatus() == PhoneSubmissionStatus.SENT
                        && submission.getValue().eventId().equals(sent.phoneSubmissionEventId())));
    }

    @Test
    void rejectsInvalidPhoneBeforeLookingUpKafkaOrClaimingSubmission() {
        ReservationException failure = assertThrows(ReservationException.class,
                () -> useCase.submit("process-1", OWNER, "call me 123"));

        assertEquals("INVALID_PHONE_NUMBER", failure.getCode());
        verify(configurationGateway, never()).currentConfiguration();
        verify(phoneSubmissionGateway, never()).publish(any(), any());
        verify(reservations, never()).beginPhoneSubmission(any(Reservation.class), anyString(), any(Instant.class));
    }

    @Test
    void rejectsCrossUserAccessWithoutPublishing() {
        ReservationException failure = assertThrows(ReservationException.class,
                () -> useCase.submit("process-1", UUID.randomUUID(), "+5492615551234"));

        assertEquals("RESERVATION_NOT_FOUND", failure.getCode());
        verify(configurationGateway, never()).currentConfiguration();
        verify(phoneSubmissionGateway, never()).publish(any(), any());
    }

    @Test
    void rejectsExpiredProcessWithoutPublishing() {
        ReservationProcess expired = ReservationProcess.created(OWNER, 15,
                        LocalDate.parse("2026-10-26"), LocalTime.parse("09:00"),
                        LocalTime.parse("09:30"), "Ana", "Gomez", "{}", NOW)
                .withId(9L).held("process-1", "hold-1", NOW, "{}", NOW)
                .waitingForPhone("Juan", "Perez", "{}", NOW, NOW);
        when(reservations.findProcess("process-1")).thenReturn(Optional.of(expired));

        ReservationException failure = assertThrows(ReservationException.class,
                () -> useCase.submit("process-1", OWNER, "+5492615551234"));

        assertEquals("RESERVATION_PROCESS_EXPIRED", failure.getCode());
        verify(configurationGateway, never()).currentConfiguration();
    }

    @Test
    void leavesSubmissionPendingAfterAnAmbiguousBrokerFailureAndDoesNotMarkItSent() {
        org.mockito.Mockito.doThrow(new IllegalStateException("broker timeout"))
                .when(phoneSubmissionGateway).publish(any(), any());

        ReservationException failure = assertThrows(ReservationException.class,
                () -> useCase.submit("process-1", OWNER, "+5492615551234"));

        assertEquals("PHONE_SUBMISSION_UNCERTAIN", failure.getCode());
        verify(reservations, never()).markPhoneSubmissionSent(any(Reservation.class));
        verify(reservations).recordProcessError(eq(9L), contains("uncertain"), any(Instant.class));
    }

    @Test
    void rejectsASecondSendWhenThePreviousOneIsStillUncertain() {
        Reservation pending = new Reservation(3L, 9L, null, "process-1", OWNER, OWNER.toString(), 15,
                LocalDate.parse("2026-10-26"), LocalTime.parse("09:00"), LocalTime.parse("09:30"),
                "Ana", "Gomez", "Juan", "Perez", "+5492615551234",
                PhoneSubmissionStatus.PENDING, "event-1", "PHONE_PENDING", NOW, null, null, null);
        when(reservations.findReservationByProcessId(9L)).thenReturn(Optional.of(pending));

        ReservationException failure = assertThrows(ReservationException.class,
                () -> useCase.submit("process-1", OWNER, "+5492615551234"));

        assertEquals("PHONE_SUBMISSION_PENDING", failure.getCode());
        verify(configurationGateway, never()).currentConfiguration();
        verify(phoneSubmissionGateway, never()).publish(any(), any());
    }
}

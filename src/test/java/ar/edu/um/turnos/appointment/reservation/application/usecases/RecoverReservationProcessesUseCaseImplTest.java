package ar.edu.um.turnos.appointment.reservation.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.appointment.reservation.domain.model.AppointmentListingPage;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionStatus;
import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcessStatus;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationQuery;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationRecoveryPolicy;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.AppointmentListingGateway;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.ReservationRepository;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;

@ExtendWith(MockitoExtension.class)
class RecoverReservationProcessesUseCaseImplTest {

    private static final UUID OWNER = UUID.fromString("a43d7a0b-fec6-4f3a-98c6-d2e283194bd9");
    private static final Instant NOW = Instant.parse("2026-11-13T12:00:00Z");
    private static final Instant EXPIRES_AT = NOW.minusSeconds(600);
    private static final ReservationQuery QUERY = new ReservationQuery(
            null, LocalDate.parse("2027-01-04"), LocalDate.parse("2027-01-04"), 0, 2);

    @Mock private ReservationRepository reservations;
    @Mock private AppointmentListingGateway appointmentListing;
    private RecoverReservationProcessesUseCaseImpl useCase;
    private ReservationProcess waitingProcess;

    @BeforeEach
    void setUp() {
        useCase = new RecoverReservationProcessesUseCaseImpl(reservations, appointmentListing,
                new ReservationRecoveryPolicy(Duration.ofSeconds(60), 5, 2, 2),
                Clock.fixed(NOW, ZoneOffset.UTC));
        waitingProcess = process(ReservationProcessStatus.WAITING_FOR_PHONE);
        when(reservations.countPendingProcesses()).thenReturn(1L);
        when(reservations.findExpiredProcesses(NOW, NOW.minusSeconds(60), 5))
                .thenReturn(List.of(waitingProcess));
    }

    @Test
    void expiresAndMarksTheLocalReservationFailedOnlyWhenTheRemoteProcessIsAbsent() {
        when(appointmentListing.list(OWNER, QUERY)).thenReturn(new AppointmentListingPage(List.of(), 0, null));
        when(reservations.completeRecovery(eq(waitingProcess), any(ReservationProcess.class), eq(null), eq(true)))
                .thenReturn(true);

        var report = useCase.reconcileExpiredProcesses();

        assertThat(report.pendingProcesses()).isEqualTo(1);
        assertThat(report.expired()).isEqualTo(1);
        verify(reservations).completeRecovery(eq(waitingProcess),
                org.mockito.ArgumentMatchers.argThat(process -> process.status() == ReservationProcessStatus.EXPIRED
                        && "HOLD_EXPIRED".equals(process.resultStatus())), eq(null), eq(true));
        verify(reservations, never()).deferRecovery(any(), any(), any());
    }

    @Test
    void adoptsAConfirmedRemoteAppointmentInsteadOfFalselyExpiringIt() {
        Reservation remote = remoteAppointment("CONFIRMED", 8251L);
        when(appointmentListing.list(OWNER, QUERY))
                .thenReturn(new AppointmentListingPage(List.of(remote), 1, null));
        when(reservations.completeRecovery(eq(waitingProcess), any(ReservationProcess.class), eq(remote), eq(false)))
                .thenReturn(true);

        var report = useCase.reconcileExpiredProcesses();

        assertThat(report.reconciled()).isEqualTo(1);
        assertThat(report.expired()).isZero();
        verify(reservations).completeRecovery(eq(waitingProcess),
                org.mockito.ArgumentMatchers.argThat(process -> process.status() == ReservationProcessStatus.CONFIRMED
                        && Long.valueOf(8251L).equals(process.reservationId())), eq(remote), eq(false));
    }

    @Test
    void keepsAStillPendingRemoteProcessOpenAndSchedulesAnotherCheck() {
        Reservation remote = remoteAppointment("PHONE_PENDING", 8251L);
        when(appointmentListing.list(OWNER, QUERY))
                .thenReturn(new AppointmentListingPage(List.of(remote), 1, null));
        when(reservations.completeRecovery(eq(waitingProcess), any(ReservationProcess.class), eq(remote), eq(false)))
                .thenReturn(true);

        var report = useCase.reconcileExpiredProcesses();

        assertThat(report.reconciled()).isEqualTo(1);
        assertThat(report.deferred()).isEqualTo(1);
        verify(reservations).completeRecovery(eq(waitingProcess),
                org.mockito.ArgumentMatchers.argThat(process ->
                        process.status() == ReservationProcessStatus.WAITING_FOR_PHONE
                                && process.updatedAt().equals(NOW)), eq(remote), eq(false));
    }

    @Test
    void searchesSubsequentPagesBeforeConcludingThatTheRemoteAppointmentIsMissing() {
        Reservation secondPageAppointment = remoteAppointment("CANCELLED", 8251L);
        ReservationQuery secondPage = new ReservationQuery(null, QUERY.from(), QUERY.to(), 1, 2);
        when(appointmentListing.list(OWNER, QUERY)).thenReturn(new AppointmentListingPage(
                List.of(remoteAppointment("CONFIRMED", 8001L, "other-process-1"),
                        remoteAppointment("CONFIRMED", 8002L, "other-process-2")), 3, null));
        when(appointmentListing.list(OWNER, secondPage))
                .thenReturn(new AppointmentListingPage(List.of(secondPageAppointment), 3, null));
        when(reservations.completeRecovery(eq(waitingProcess), any(ReservationProcess.class),
                eq(secondPageAppointment), eq(false))).thenReturn(true);

        var report = useCase.reconcileExpiredProcesses();

        assertThat(report.reconciled()).isEqualTo(1);
        verify(appointmentListing).list(OWNER, QUERY);
        verify(appointmentListing).list(OWNER, secondPage);
    }

    @Test
    void doesNotExpireWhenTheConfiguredPageLimitCannotProveAbsence() {
        when(appointmentListing.list(eq(OWNER), any(ReservationQuery.class)))
                .thenReturn(new AppointmentListingPage(List.of(
                        remoteAppointment("CONFIRMED", 8001L, "other-process-1"),
                        remoteAppointment("CONFIRMED", 8002L, "other-process-2")), 5, null));
        when(reservations.deferRecovery(waitingProcess, "REMOTE_LOOKUP_INCOMPLETE", NOW)).thenReturn(true);

        var report = useCase.reconcileExpiredProcesses();

        assertThat(report.expired()).isZero();
        assertThat(report.deferred()).isEqualTo(1);
        verify(appointmentListing, org.mockito.Mockito.times(2)).list(eq(OWNER), any(ReservationQuery.class));
        verify(reservations, never()).completeRecovery(any(), any(), any(), anyBoolean());
    }

    @Test
    void retainsThePendingStateAndRecordsOnlyTheStableCodeWhenCatedraIsUnavailable() {
        when(appointmentListing.list(OWNER, QUERY)).thenThrow(
                new CatedraClientException("CATEDRA_UNAVAILABLE", 503, "private transport detail"));
        when(reservations.deferRecovery(waitingProcess, "CATEDRA_UNAVAILABLE", NOW)).thenReturn(true);

        var report = useCase.reconcileExpiredProcesses();

        assertThat(report.deferred()).isEqualTo(1);
        assertThat(report.expired()).isZero();
        verify(reservations).deferRecovery(waitingProcess, "CATEDRA_UNAVAILABLE", NOW);
        verify(reservations, never()).completeRecovery(any(), any(), any(), anyBoolean());
    }

    @Test
    void skipsPersistenceWhenAnEventChangedTheProcessDuringRemoteLookup() {
        when(appointmentListing.list(OWNER, QUERY)).thenReturn(new AppointmentListingPage(List.of(), 0, null));
        when(reservations.completeRecovery(eq(waitingProcess), any(ReservationProcess.class), eq(null), eq(true)))
                .thenReturn(false);

        var report = useCase.reconcileExpiredProcesses();

        assertThat(report.skipped()).isEqualTo(1);
        assertThat(report.expired()).isZero();
    }

    @Test
    void treatsCrossUserRemoteDataAsAnInvalidUpstreamResponseAndNeverPersistsIt() {
        Reservation foreignRemote = new Reservation(null, null, 8251L, waitingProcess.reservationProcessId(),
                UUID.randomUUID(), "someone-else", waitingProcess.professionalId(), waitingProcess.appointmentDate(),
                waitingProcess.startTime(), waitingProcess.endTime(), "Ana", "Gomez", "Other", "Patient", null,
                PhoneSubmissionStatus.NOT_SENT, null, "CONFIRMED", NOW, NOW, null, null);
        when(appointmentListing.list(OWNER, QUERY))
                .thenReturn(new AppointmentListingPage(List.of(foreignRemote), 1, null));
        when(reservations.deferRecovery(waitingProcess, "CATEDRA_INVALID_RESPONSE", NOW)).thenReturn(true);

        var report = useCase.reconcileExpiredProcesses();

        assertThat(report.deferred()).isEqualTo(1);
        verify(reservations, never()).completeRecovery(any(), any(), any(), anyBoolean());
    }

    private static ReservationProcess process(ReservationProcessStatus status) {
        return new ReservationProcess(9L, "process-1", "hold-1", OWNER, 15,
                LocalDate.parse("2027-01-04"), LocalTime.parse("09:00"), LocalTime.parse("09:30"),
                "Ana", "Gomez", "Juan", "Perez", status, EXPIRES_AT, "request-1", null,
                "PHONE_PENDING", null, "{}", "{}", NOW.minusSeconds(3600), NOW.minusSeconds(600));
    }

    private static Reservation remoteAppointment(String status, Long reservationId) {
        return remoteAppointment(status, reservationId, "process-1");
    }

    private static Reservation remoteAppointment(String status, Long reservationId, String processId) {
        return new Reservation(null, null, reservationId, processId, OWNER, OWNER.toString(), 15,
                LocalDate.parse("2027-01-04"), LocalTime.parse("09:00"), LocalTime.parse("09:30"),
                "Ana", "Gomez", "Juan", "Perez", null, PhoneSubmissionStatus.NOT_SENT, null,
                status, NOW.minusSeconds(1800), NOW.minusSeconds(900),
                "CANCELLED".equals(status) ? NOW.minusSeconds(300) : null, null);
    }
}

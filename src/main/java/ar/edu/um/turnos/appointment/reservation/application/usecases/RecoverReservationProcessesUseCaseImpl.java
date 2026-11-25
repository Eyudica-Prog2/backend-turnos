package ar.edu.um.turnos.appointment.reservation.application.usecases;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.appointment.reservation.domain.model.AppointmentListingPage;
import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationQuery;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationRecoveryPolicy;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationRecoveryReport;
import ar.edu.um.turnos.appointment.reservation.domain.ports.in.RecoverReservationProcessesUseCase;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.AppointmentListingGateway;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.ReservationRepository;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;

@Component
public class RecoverReservationProcessesUseCaseImpl implements RecoverReservationProcessesUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecoverReservationProcessesUseCaseImpl.class);

    private final ReservationRepository reservations;
    private final AppointmentListingGateway appointmentListing;
    private final ReservationRecoveryPolicy policy;
    private final Clock clock;

    public RecoverReservationProcessesUseCaseImpl(ReservationRepository reservations,
                                                 AppointmentListingGateway appointmentListing,
                                                 ReservationRecoveryPolicy policy,
                                                 Clock clock) {
        this.reservations = reservations;
        this.appointmentListing = appointmentListing;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    public ReservationRecoveryReport reconcileExpiredProcesses() {
        Instant now = clock.instant();
        Instant retryBefore = now.minus(policy.retryBackoff());
        long pending = reservations.countPendingProcesses();
        List<ReservationProcess> candidates = reservations.findExpiredProcesses(
                now, retryBefore, policy.batchSize());
        int reconciled = 0;
        int expired = 0;
        int deferred = 0;
        int skipped = 0;

        for (ReservationProcess candidate : candidates) {
            try {
                RemoteLookup lookup = findRemoteAppointment(candidate);
                if (!lookup.complete()) {
                    if (defer(candidate, "REMOTE_LOOKUP_INCOMPLETE", now)) {
                        deferred++;
                    } else {
                        skipped++;
                    }
                    continue;
                }

                if (lookup.reservation() == null) {
                    ReservationProcess expiredProcess = candidate.expire(now);
                    if (reservations.completeRecovery(candidate, expiredProcess, null, true)) {
                        expired++;
                    } else {
                        skipped++;
                    }
                    continue;
                }

                Reservation remote = lookup.reservation();
                ReservationProcess updated = candidate.reconcileWithRemote(
                        remote.status(), remote.reservationId(), remote.patientFirstName(),
                        remote.patientLastName(), now);
                if (reservations.completeRecovery(candidate, updated, remote, false)) {
                    reconciled++;
                    if (!updated.status().isFinal()) {
                        deferred++;
                    }
                } else {
                    skipped++;
                }
            } catch (CatedraClientException dependencyFailure) {
                if (defer(candidate, dependencyFailure.getCode(), now)) {
                    deferred++;
                } else {
                    skipped++;
                }
            } catch (RuntimeException unexpectedFailure) {
                if (defer(candidate, "RECOVERY_INTERNAL_ERROR", now)) {
                    deferred++;
                } else {
                    skipped++;
                }
                LOGGER.error("Reservation recovery deferred after an internal failure of type {}.",
                        unexpectedFailure.getClass().getSimpleName());
            }
        }

        return new ReservationRecoveryReport(pending, candidates.size(), reconciled, expired, deferred, skipped);
    }

    private RemoteLookup findRemoteAppointment(ReservationProcess process) {
        if (process.reservationProcessId() == null || process.reservationProcessId().isBlank()) {
            return new RemoteLookup(null, false);
        }

        long totalCount = Long.MAX_VALUE;
        for (int page = 0; page < policy.maxPages(); page++) {
            AppointmentListingPage response = appointmentListing.list(process.ownerPublicId(),
                    new ReservationQuery(null, process.appointmentDate(), process.appointmentDate(),
                            page, policy.pageSize()));
            totalCount = response.totalCount();
            for (Reservation appointment : response.reservations()) {
                if (!process.reservationProcessId().equals(appointment.reservationProcessId())) {
                    continue;
                }
                validateRemoteOwnershipAndSlot(process, appointment);
                return new RemoteLookup(appointment, true);
            }

            long fetchedCount = (long) (page + 1) * policy.pageSize();
            if (fetchedCount >= totalCount) {
                return new RemoteLookup(null, true);
            }
            if (response.reservations().isEmpty()) {
                return new RemoteLookup(null, false);
            }
        }
        return new RemoteLookup(null, false);
    }

    private static void validateRemoteOwnershipAndSlot(ReservationProcess process, Reservation remote) {
        boolean ownershipMatches = process.ownerPublicId().equals(remote.ownerPublicId())
                && process.ownerPublicId().toString().equals(remote.externalPatientId());
        boolean slotMatches = process.professionalId() == remote.professionalId()
                && process.appointmentDate().equals(remote.appointmentDate())
                && process.startTime().equals(remote.startTime());
        if (!ownershipMatches || !slotMatches) {
            throw new CatedraClientException("CATEDRA_INVALID_RESPONSE", 503,
                    "The catedra appointment does not match its local reservation process.");
        }
    }

    private boolean defer(ReservationProcess process, String safeErrorCode, Instant attemptedAt) {
        String errorCode = safeErrorCode == null || safeErrorCode.isBlank()
                ? "RECOVERY_FAILED" : safeErrorCode;
        boolean recorded = reservations.deferRecovery(process, errorCode, attemptedAt);
        if (recorded) {
            LOGGER.warn("Reservation recovery deferred: code={}", errorCode);
        }
        return recorded;
    }

    private record RemoteLookup(Reservation reservation, boolean complete) {
    }
}

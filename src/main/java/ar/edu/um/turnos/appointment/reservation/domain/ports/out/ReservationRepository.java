package ar.edu.um.turnos.appointment.reservation.domain.ports.out;

import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {

    ReservationProcess saveProcess(ReservationProcess process);

    Optional<ReservationProcess> findProcess(String reservationProcessId);

    long countPendingProcesses();

    List<ReservationProcess> findExpiredProcesses(Instant expiredAt, Instant lastAttemptBefore, int limit);

    void deleteProcess(Long id);

    Reservation saveReservation(Reservation reservation);

    void saveConfirmation(ReservationProcess process, Reservation reservation);

    void saveCancellation(ReservationProcess process, Reservation reservation);

    boolean completeRecovery(ReservationProcess expected, ReservationProcess updated,
                             Reservation remoteReservation, boolean failLocalReservation);

    boolean deferRecovery(ReservationProcess expected, String safeErrorCode, Instant attemptedAt);

    Optional<Reservation> findReservationByProcessId(Long processId);

    Optional<Reservation> findReservationByReservationId(Long reservationId);

    Optional<Reservation> findReservationByReservationProcessId(String reservationProcessId);

    boolean beginPhoneSubmission(Reservation pendingReservation, String requestEventId, Instant now);

    boolean markPhoneSubmissionSent(Reservation sentReservation);

    void recordProcessError(Long processId, String error, Instant updatedAt);

    boolean registerEventIfNew(String eventId, String eventType, Instant processedAt);
}

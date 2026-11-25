package ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcessStatus;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.ReservationRepository;
import ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.entity.ReservationEntity;
import ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.mapper.ReservationMapper;
import ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.repository.JpaProcessedEventRepository;
import ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.repository.JpaReservationProcessRepository;
import ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.repository.JpaReservationRepository;

@Component
public class JpaReservationRepositoryAdapter implements ReservationRepository {

    private static final List<ReservationProcessStatus> PENDING_STATUSES = List.of(
            ReservationProcessStatus.CREATED,
            ReservationProcessStatus.HELD,
            ReservationProcessStatus.WAITING_FOR_PHONE,
            ReservationProcessStatus.REJECTED_RETRYABLE);

    private static final List<ReservationProcessStatus> EXPIRABLE_STATUSES = List.of(
            ReservationProcessStatus.HELD,
            ReservationProcessStatus.WAITING_FOR_PHONE,
            ReservationProcessStatus.REJECTED_RETRYABLE);

    private final JpaReservationProcessRepository processRepository;
    private final JpaReservationRepository reservationRepository;
    private final JpaProcessedEventRepository eventRepository;
    private final ReservationMapper mapper;

    public JpaReservationRepositoryAdapter(JpaReservationProcessRepository processRepository,
                                          JpaReservationRepository reservationRepository,
                                          JpaProcessedEventRepository eventRepository,
                                          ReservationMapper mapper) {
        this.processRepository = processRepository;
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ReservationProcess saveProcess(ReservationProcess process) {
        return mapper.toDomain(processRepository.save(mapper.toEntity(process)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReservationProcess> findProcess(String reservationProcessId) {
        return processRepository.findByReservationProcessId(reservationProcessId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingProcesses() {
        return processRepository.countByStatusIn(PENDING_STATUSES);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationProcess> findExpiredProcesses(Instant expiredAt, Instant lastAttemptBefore, int limit) {
        return processRepository.findByStatusInAndExpiresAtLessThanEqualAndUpdatedAtLessThanEqualOrderByExpiresAtAsc(
                        EXPIRABLE_STATUSES, expiredAt, lastAttemptBefore, PageRequest.of(0, limit))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteProcess(Long id) {
        if (id != null) {
            processRepository.deleteById(id);
        }
    }

    @Override
    @Transactional
    public Reservation saveReservation(Reservation reservation) {
        return mapper.toDomain(reservationRepository.save(mapper.toEntity(reservation)));
    }

    @Override
    @Transactional
    public void saveConfirmation(ReservationProcess process, Reservation reservation) {
        processRepository.save(mapper.toEntity(process));
        reservationRepository.save(mapper.toEntity(reservation));
    }

    @Override
    @Transactional
    public void saveCancellation(ReservationProcess process, Reservation reservation) {
        if (process != null) {
            processRepository.save(mapper.toEntity(process));
        }
        reservationRepository.save(mapper.toEntity(reservation));
    }

    @Override
    @Transactional
    public boolean completeRecovery(ReservationProcess expected, ReservationProcess updated,
                                    Reservation remoteReservation, boolean failLocalReservation) {
        ReservationProcessEntity currentEntity = processRepository.findByIdForUpdate(expected.id()).orElse(null);
        if (!matchesRecoveryCandidate(currentEntity, expected, updated.updatedAt())) {
            return false;
        }

        ReservationProcess current = mapper.toDomain(currentEntity);
        if (!current.ownerPublicId().equals(expected.ownerPublicId())) {
            return false;
        }
        ReservationEntity existingEntity = reservationRepository.findByProcessIdForUpdate(current.id())
                .orElse(null);
        Reservation existing = existingEntity == null ? null : mapper.toDomain(existingEntity);
        if (existing != null && !existing.ownerPublicId().equals(current.ownerPublicId())) {
            return false;
        }

        processRepository.save(mapper.toEntity(updated));
        if (remoteReservation != null) {
            Reservation reconciled = existing == null
                    ? remoteReservation.withProcessContext(current.id())
                    : existing.reconcileFromRemote(remoteReservation, current.id());
            reservationRepository.save(mapper.toEntity(reconciled));
        } else if (failLocalReservation && existing != null) {
            reservationRepository.save(mapper.toEntity(existing.failed()));
        }
        return true;
    }

    @Override
    @Transactional
    public boolean deferRecovery(ReservationProcess expected, String safeErrorCode, Instant attemptedAt) {
        ReservationProcessEntity currentEntity = processRepository.findByIdForUpdate(expected.id()).orElse(null);
        if (!matchesRecoveryCandidate(currentEntity, expected, attemptedAt)) {
            return false;
        }
        ReservationProcess current = mapper.toDomain(currentEntity);
        if (!current.ownerPublicId().equals(expected.ownerPublicId())) {
            return false;
        }
        processRepository.save(mapper.toEntity(current.withLastError(safeErrorCode, attemptedAt)));
        return true;
    }

    private static boolean matchesRecoveryCandidate(ReservationProcessEntity current,
                                                    ReservationProcess expected, Instant attemptedAt) {
        if (current == null || !expected.id().equals(current.getId())
                || current.getStatus() != expected.status()
                || !expected.updatedAt().equals(current.getUpdatedAt())
                || current.getExpiresAt() == null || current.getExpiresAt().isAfter(attemptedAt)) {
            return false;
        }
        return current.getStatus() == ReservationProcessStatus.HELD
                || current.getStatus() == ReservationProcessStatus.WAITING_FOR_PHONE
                || current.getStatus() == ReservationProcessStatus.REJECTED_RETRYABLE;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> findReservationByProcessId(Long processId) {
        return reservationRepository.findByProcessId(processId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> findReservationByReservationId(Long reservationId) {
        return reservationRepository.findByReservationId(reservationId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> findReservationByReservationProcessId(String reservationProcessId) {
        return reservationRepository.findByReservationProcessId(reservationProcessId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public boolean beginPhoneSubmission(Reservation pendingReservation, String requestEventId, Instant now) {
        return reservationRepository.beginPhoneSubmission(pendingReservation.processId(), requestEventId,
                pendingReservation.patientPhone(), pendingReservation.phoneSubmissionEventId(), now) == 1;
    }

    @Override
    @Transactional
    public boolean markPhoneSubmissionSent(Reservation sentReservation) {
        return reservationRepository.markPhoneSubmissionSent(
                sentReservation.processId(), sentReservation.phoneSubmissionEventId()) == 1;
    }

    @Override
    @Transactional
    public void recordProcessError(Long processId, String error, Instant updatedAt) {
        if (processId != null) {
            processRepository.updateLastError(processId, error, updatedAt);
        }
    }

    @Override
    @Transactional
    public boolean registerEventIfNew(String eventId, String eventType, Instant processedAt) {
        return eventRepository.insertIfAbsent(eventId, eventType, processedAt) == 1;
    }
}

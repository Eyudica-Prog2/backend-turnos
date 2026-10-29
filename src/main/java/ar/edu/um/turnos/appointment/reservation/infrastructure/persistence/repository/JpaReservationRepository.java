package ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.repository;

import ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.entity.ReservationEntity;
import java.util.Optional;
import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaReservationRepository extends JpaRepository<ReservationEntity, Long> {

    Optional<ReservationEntity> findByProcessId(Long processId);

    Optional<ReservationEntity> findByReservationId(Long reservationId);

    Optional<ReservationEntity> findByReservationProcessId(String reservationProcessId);

    @Modifying
    @Query(value = "UPDATE reservation r SET patient_phone = :phone, "
            + "phone_submission_status = 'PENDING', phone_submission_event_id = :eventId "
            + "FROM reservation_process p WHERE r.reservation_process_id = :processId "
            + "AND p.id = :processId AND p.status IN ('WAITING_FOR_PHONE', 'REJECTED_RETRYABLE') "
            + "AND p.expires_at > :now AND p.request_event_id = :requestEventId "
            + "AND r.phone_submission_status = 'NOT_SENT'",
            nativeQuery = true)
    int beginPhoneSubmission(@Param("processId") Long processId,
                             @Param("requestEventId") String requestEventId,
                             @Param("phone") String phone,
                             @Param("eventId") String eventId,
                             @Param("now") Instant now);

    @Modifying
    @Query(value = "UPDATE reservation SET phone_submission_status = 'SENT' "
            + "WHERE reservation_process_id = :processId AND phone_submission_status = 'PENDING' "
            + "AND phone_submission_event_id = :eventId", nativeQuery = true)
    int markPhoneSubmissionSent(@Param("processId") Long processId,
                                @Param("eventId") String eventId);
}

package ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcessStatus;
import ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.entity.ReservationProcessEntity;

public interface JpaReservationProcessRepository extends JpaRepository<ReservationProcessEntity, Long> {

    Optional<ReservationProcessEntity> findByReservationProcessId(String reservationProcessId);

    long countByStatusIn(Collection<ReservationProcessStatus> statuses);

    List<ReservationProcessEntity> findByStatusInAndExpiresAtLessThanEqualAndUpdatedAtLessThanEqualOrderByExpiresAtAsc(
            Collection<ReservationProcessStatus> statuses, Instant expiresAt, Instant updatedAt, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select process from ReservationProcessEntity process where process.id = :id")
    Optional<ReservationProcessEntity> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE reservation_process SET last_error = :error, updated_at = :updatedAt WHERE id = :id",
            nativeQuery = true)
    int updateLastError(@Param("id") Long id,
                        @Param("error") String error,
                        @Param("updatedAt") Instant updatedAt);
}

package ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.repository;

import ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface JpaReservationProcessRepository extends JpaRepository<ReservationProcessEntity, Long> {

    Optional<ReservationProcessEntity> findByReservationProcessId(String reservationProcessId);

    @Modifying
    @Query(value = "UPDATE reservation_process SET last_error = :error, updated_at = :updatedAt WHERE id = :id",
            nativeQuery = true)
    int updateLastError(@Param("id") Long id,
                        @Param("error") String error,
                        @Param("updatedAt") Instant updatedAt);
}

package ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.repository;

import ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaReservationProcessRepository extends JpaRepository<ReservationProcessEntity, Long> {

    Optional<ReservationProcessEntity> findByReservationProcessId(String reservationProcessId);
}

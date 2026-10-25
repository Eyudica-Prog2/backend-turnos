package ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.repository;

import ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.entity.ReservationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaReservationRepository extends JpaRepository<ReservationEntity, Long> {

    Optional<ReservationEntity> findByProcessId(Long processId);
}

package ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.repository;

import ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.entity.ProcessedEventEntity;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {

    @Modifying
    @Query(value = "INSERT INTO processed_event (event_id, event_type, processed_at) "
            + "VALUES (:eventId, :eventType, :processedAt) ON CONFLICT (event_id) DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(@Param("eventId") String eventId,
                       @Param("eventType") String eventType,
                       @Param("processedAt") Instant processedAt);
}

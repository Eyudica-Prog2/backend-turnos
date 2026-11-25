package ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "processed_event")
public class ProcessedEventEntity {

    @Id
    @Column(name = "event_id", length = 120)
    private String eventId;
    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEventEntity() {
    }
}

package ar.edu.um.turnos.appointment.reservation.domain.model;

import java.time.Instant;

public record ReservationEvent(
        String eventId,
        String eventType,
        String reservationProcessId,
        String holdId,
        String requestEventId,
        Long reservationId,
        Long professionalId,
        String appointmentDate,
        String startTime,
        String status,
        String reason,
        String message,
        Instant expiresAt,
        Instant occurredAt,
        Integer schemaVersion) {
}

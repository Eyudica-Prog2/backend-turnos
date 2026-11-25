package ar.edu.um.turnos.appointment.reservation.domain.model;

import java.time.Instant;

public record PhoneSubmission(
        String eventId,
        String reservationProcessId,
        String holdId,
        String requestEventId,
        String phoneNumber,
        Instant occurredAt) {
}

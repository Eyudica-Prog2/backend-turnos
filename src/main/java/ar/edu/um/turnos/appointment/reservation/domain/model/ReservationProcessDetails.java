package ar.edu.um.turnos.appointment.reservation.domain.model;

import java.time.Instant;

public record ReservationProcessDetails(
        String reservationProcessId,
        String status,
        Instant expiresAt,
        Long reservationId,
        String resultStatus,
        String error,
        String phoneSubmissionStatus) {
}

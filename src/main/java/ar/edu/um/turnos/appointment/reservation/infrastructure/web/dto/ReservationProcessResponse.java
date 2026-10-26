package ar.edu.um.turnos.appointment.reservation.infrastructure.web.dto;

import java.time.Instant;

public record ReservationProcessResponse(
        String reservationProcessId,
        String status,
        Instant expiresAt,
        Long reservationId,
        String resultStatus,
        String error,
        String phoneSubmissionStatus) {
}

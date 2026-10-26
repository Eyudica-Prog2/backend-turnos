package ar.edu.um.turnos.appointment.reservation.infrastructure.web.dto;

import java.time.Instant;

public record PhoneSubmissionResponse(
        String reservationProcessId,
        String status,
        String phoneSubmissionStatus,
        Instant expiresAt) {
}

package ar.edu.um.turnos.appointment.reservation.domain.model;

import java.time.Instant;

public record PhoneSubmissionResult(
        String reservationProcessId,
        String status,
        String phoneSubmissionStatus,
        Instant expiresAt) {
}

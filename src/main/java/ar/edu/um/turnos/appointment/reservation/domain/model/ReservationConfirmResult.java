package ar.edu.um.turnos.appointment.reservation.domain.model;

import java.time.Instant;

public record ReservationConfirmResult(
        String reservationProcessId,
        String status,
        Instant expiresAt,
        String requestPayload) {
}

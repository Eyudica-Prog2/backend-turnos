package ar.edu.um.turnos.appointment.reservation.infrastructure.web.dto;

import java.time.Instant;

public record ReservationHoldResponse(
        String reservationProcessId,
        String holdId,
        String status,
        Instant expiresAt) {
}

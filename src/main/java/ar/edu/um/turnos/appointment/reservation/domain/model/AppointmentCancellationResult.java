package ar.edu.um.turnos.appointment.reservation.domain.model;

import java.time.Instant;

public record AppointmentCancellationResult(
        Long reservationId,
        String reservationProcessId,
        String status,
        Instant cancelledAt) {
}

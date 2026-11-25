package ar.edu.um.turnos.appointment.reservation.infrastructure.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReservationResponse(
        Long reservationId,
        String reservationProcessId,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        long professionalId,
        String professionalFirstName,
        String professionalLastName,
        String patientFirstName,
        String patientLastName,
        String status,
        Instant createdAt,
        Instant confirmedAt,
        Instant cancelledAt,
        String cancellationReason) {
}

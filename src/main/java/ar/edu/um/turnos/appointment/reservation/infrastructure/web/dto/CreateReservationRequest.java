package ar.edu.um.turnos.appointment.reservation.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateReservationRequest(
        @NotNull @Positive Long professionalId,
        @NotNull LocalDate date,
        @NotNull LocalTime startTime) {
}

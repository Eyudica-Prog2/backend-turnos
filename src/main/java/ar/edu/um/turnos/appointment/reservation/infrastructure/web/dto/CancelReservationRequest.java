package ar.edu.um.turnos.appointment.reservation.infrastructure.web.dto;

import jakarta.validation.constraints.Size;

public record CancelReservationRequest(@Size(max = 500) String reason) {
}

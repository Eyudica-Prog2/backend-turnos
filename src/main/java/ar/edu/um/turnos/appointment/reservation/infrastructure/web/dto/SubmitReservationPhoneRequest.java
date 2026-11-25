package ar.edu.um.turnos.appointment.reservation.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitReservationPhoneRequest(
        @NotBlank @Size(max = 64) String phoneNumber) {
}

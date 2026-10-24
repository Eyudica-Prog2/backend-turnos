package ar.edu.um.turnos.appointment.availability.infrastructure.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Only future, unoccupied slots are returned; omitted slots are unavailable. */
public record ProfessionalAvailabilityResponse(
        long professionalId,
        LocalDate date,
        List<AvailableSlotResponse> slots) {

    public ProfessionalAvailabilityResponse {
        slots = List.copyOf(slots);
    }

    public record AvailableSlotResponse(LocalTime startTime, LocalTime endTime) {
    }
}

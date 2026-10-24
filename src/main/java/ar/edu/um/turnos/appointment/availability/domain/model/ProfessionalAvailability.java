package ar.edu.um.turnos.appointment.availability.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record ProfessionalAvailability(
        long professionalId,
        LocalDate date,
        List<AppointmentSlot> slots) {

    public ProfessionalAvailability {
        if (professionalId <= 0) {
            throw new IllegalArgumentException("Professional ID must be positive.");
        }
        Objects.requireNonNull(date, "Availability date is required.");
        slots = List.copyOf(slots);
        if (slots.stream().anyMatch(slot -> !date.equals(slot.date()))) {
            throw new IllegalArgumentException("Every slot must belong to the availability date.");
        }
    }
}

package ar.edu.um.turnos.appointment.availability.domain.model;

import java.time.LocalDate;
import java.util.Objects;

public record AvailabilityQuery(long professionalId, LocalDate date) {

    public AvailabilityQuery {
        if (professionalId <= 0) {
            throw new IllegalArgumentException("Professional ID must be positive.");
        }
        Objects.requireNonNull(date, "Availability date is required.");
    }
}

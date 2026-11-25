package ar.edu.um.turnos.appointment.availability.domain.model;

import java.time.LocalDate;
import java.util.Objects;

public record OccupancyQuery(long professionalId, LocalDate from, LocalDate to) {

    public OccupancyQuery {
        if (professionalId <= 0) {
            throw new IllegalArgumentException("Professional ID must be positive.");
        }
        Objects.requireNonNull(from, "From date is required.");
        Objects.requireNonNull(to, "To date is required.");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From date cannot be after to date.");
        }
    }
}

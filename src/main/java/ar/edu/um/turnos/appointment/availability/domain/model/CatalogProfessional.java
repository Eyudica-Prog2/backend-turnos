package ar.edu.um.turnos.appointment.availability.domain.model;

import java.util.Objects;

public record CatalogProfessional(
        long id,
        long categoryId,
        String firstName,
        String lastName,
        boolean enabled) {

    public CatalogProfessional {
        if (id <= 0 || categoryId <= 0) {
            throw new IllegalArgumentException("Professional and category IDs must be positive.");
        }
        Objects.requireNonNull(firstName, "Professional first name is required.");
        Objects.requireNonNull(lastName, "Professional last name is required.");
        if (firstName.isBlank() || lastName.isBlank()) {
            throw new IllegalArgumentException("Professional names must not be blank.");
        }
    }
}

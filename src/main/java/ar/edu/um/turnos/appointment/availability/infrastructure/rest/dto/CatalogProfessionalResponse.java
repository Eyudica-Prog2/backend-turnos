package ar.edu.um.turnos.appointment.availability.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogProfessionalResponse(
        Long id,
        Long categoryId,
        String firstName,
        String lastName,
        Boolean enabled) {
}

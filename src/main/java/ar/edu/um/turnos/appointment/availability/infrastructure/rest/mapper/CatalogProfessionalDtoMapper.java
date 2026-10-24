package ar.edu.um.turnos.appointment.availability.infrastructure.rest.mapper;

import ar.edu.um.turnos.appointment.availability.domain.model.CatalogProfessional;
import ar.edu.um.turnos.appointment.availability.infrastructure.rest.dto.CatalogProfessionalResponse;
import org.springframework.stereotype.Component;

@Component
public class CatalogProfessionalDtoMapper {

    public CatalogProfessional toDomain(CatalogProfessionalResponse response) {
        if (response == null
                || response.id() == null
                || response.categoryId() == null
                || response.firstName() == null
                || response.lastName() == null
                || response.enabled() == null) {
            throw new IllegalArgumentException("Catalog professional response is incomplete.");
        }
        return new CatalogProfessional(
                response.id(),
                response.categoryId(),
                response.firstName(),
                response.lastName(),
                response.enabled());
    }
}

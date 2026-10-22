package ar.edu.um.turnos.appointment.catalog.infrastructure.rest;

import ar.edu.um.turnos.appointment.catalog.domain.model.CatalogProfessional;
import ar.edu.um.turnos.appointment.catalog.domain.ports.out.ProfessionalCatalogGateway;
import ar.edu.um.turnos.appointment.catalog.infrastructure.rest.dto.CatalogProfessionalResponse;
import ar.edu.um.turnos.appointment.catalog.infrastructure.rest.mapper.CatalogProfessionalDtoMapper;
import ar.edu.um.turnos.appointment.shared.security.AuthenticatedUserProvider;
import org.springframework.stereotype.Component;

@Component
public class CatalogProfessionalGatewayAdapter implements ProfessionalCatalogGateway {

    private static final String PROFESSIONAL_PATH = "/api/internal/professionals/";

    private final CatalogRestClientSupport restClientSupport;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final CatalogProfessionalDtoMapper dtoMapper;

    public CatalogProfessionalGatewayAdapter(CatalogRestClientSupport restClientSupport,
                                             AuthenticatedUserProvider authenticatedUserProvider,
                                             CatalogProfessionalDtoMapper dtoMapper) {
        this.restClientSupport = restClientSupport;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.dtoMapper = dtoMapper;
    }

    @Override
    public CatalogProfessional findProfessional(long professionalId) {
        if (professionalId <= 0) {
            throw new IllegalArgumentException("Professional ID must be positive.");
        }
        CatalogProfessionalResponse response = restClientSupport.get(
                PROFESSIONAL_PATH + professionalId,
                authenticatedUserProvider.currentToken(),
                CatalogProfessionalResponse.class);
        try {
            return dtoMapper.toDomain(response);
        } catch (IllegalArgumentException failure) {
            throw CatalogRestClientSupport.invalidResponse();
        }
    }
}

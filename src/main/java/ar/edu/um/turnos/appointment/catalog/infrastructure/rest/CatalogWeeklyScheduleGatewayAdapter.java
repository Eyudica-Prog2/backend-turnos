package ar.edu.um.turnos.appointment.catalog.infrastructure.rest;

import ar.edu.um.turnos.appointment.catalog.domain.model.WeeklySchedule;
import ar.edu.um.turnos.appointment.catalog.domain.model.WeeklyScheduleQuery;
import ar.edu.um.turnos.appointment.catalog.domain.ports.out.WeeklyScheduleCatalogGateway;
import ar.edu.um.turnos.appointment.catalog.infrastructure.rest.dto.CatalogWeeklyScheduleResponse;
import ar.edu.um.turnos.appointment.catalog.infrastructure.rest.mapper.CatalogWeeklyScheduleDtoMapper;
import ar.edu.um.turnos.appointment.shared.security.AuthenticatedUserProvider;
import java.time.DateTimeException;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class CatalogWeeklyScheduleGatewayAdapter implements WeeklyScheduleCatalogGateway {

    private static final String SCHEDULES_PATH = "/api/internal/professionals/%d/weekly-schedules";
    private static final ParameterizedTypeReference<List<CatalogWeeklyScheduleResponse>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() { };

    private final CatalogRestClientSupport restClientSupport;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final CatalogWeeklyScheduleDtoMapper dtoMapper;

    public CatalogWeeklyScheduleGatewayAdapter(CatalogRestClientSupport restClientSupport,
                                               AuthenticatedUserProvider authenticatedUserProvider,
                                               CatalogWeeklyScheduleDtoMapper dtoMapper) {
        this.restClientSupport = restClientSupport;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.dtoMapper = dtoMapper;
    }

    @Override
    public List<WeeklySchedule> findWeeklySchedules(WeeklyScheduleQuery query) {
        String path = UriComponentsBuilder.fromPath(SCHEDULES_PATH.formatted(query.professionalId()))
                .queryParamIfPresent("date", java.util.Optional.ofNullable(query.date()))
                .build()
                .encode()
                .toUriString();
        List<CatalogWeeklyScheduleResponse> response = restClientSupport.get(
                path, authenticatedUserProvider.currentToken(), RESPONSE_TYPE);
        try {
            return dtoMapper.toDomain(response);
        } catch (IllegalArgumentException | DateTimeException failure) {
            throw CatalogRestClientSupport.invalidResponse();
        }
    }
}

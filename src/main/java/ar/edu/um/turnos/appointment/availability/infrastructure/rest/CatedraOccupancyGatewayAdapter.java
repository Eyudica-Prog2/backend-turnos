package ar.edu.um.turnos.appointment.availability.infrastructure.rest;

import ar.edu.um.turnos.appointment.availability.domain.model.AppointmentOccupancy;
import ar.edu.um.turnos.appointment.availability.domain.model.OccupancyQuery;
import ar.edu.um.turnos.appointment.availability.domain.ports.out.AppointmentOccupancyGateway;
import ar.edu.um.turnos.appointment.availability.infrastructure.rest.dto.AppointmentOccupanciesResponse;
import ar.edu.um.turnos.appointment.availability.infrastructure.rest.mapper.AppointmentOccupancyDtoMapper;
import ar.edu.um.turnos.appointment.catedra.infrastructure.rest.CatedraClientException;
import ar.edu.um.turnos.appointment.catedra.infrastructure.rest.CatedraTechnicalClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DateTimeException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class CatedraOccupancyGatewayAdapter implements AppointmentOccupancyGateway {

    private static final String OCCUPANCIES_PATH = "/api/appointment-occupancies";

    private final CatedraTechnicalClient technicalClient;
    private final ObjectMapper objectMapper;
    private final AppointmentOccupancyDtoMapper dtoMapper;

    public CatedraOccupancyGatewayAdapter(CatedraTechnicalClient technicalClient,
                                         ObjectMapper objectMapper,
                                         AppointmentOccupancyDtoMapper dtoMapper) {
        this.technicalClient = technicalClient;
        this.objectMapper = objectMapper;
        this.dtoMapper = dtoMapper;
    }

    @Override
    public List<AppointmentOccupancy> findOccupancies(OccupancyQuery query) {
        String path = UriComponentsBuilder.fromPath(OCCUPANCIES_PATH)
                .queryParam("professionalId", query.professionalId())
                .queryParam("from", query.from())
                .queryParam("to", query.to())
                .build()
                .encode()
                .toUriString();

        String responseBody = technicalClient.get(path);
        if (responseBody == null || responseBody.isBlank()) {
            throw invalidResponse();
        }

        try {
            AppointmentOccupanciesResponse response = objectMapper.readValue(
                    responseBody, AppointmentOccupanciesResponse.class);
            return dtoMapper.toDomain(query, response);
        } catch (JsonProcessingException | IllegalArgumentException | DateTimeException failure) {
            throw invalidResponse();
        }
    }

    private static CatedraClientException invalidResponse() {
        return new CatedraClientException(
                "CATEDRA_INVALID_RESPONSE", 503,
                "The catedra occupancy response is empty or does not match the contract.");
    }
}

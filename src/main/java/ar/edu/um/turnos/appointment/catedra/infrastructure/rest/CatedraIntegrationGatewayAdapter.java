package ar.edu.um.turnos.appointment.catedra.infrastructure.rest;

import ar.edu.um.turnos.appointment.catedra.domain.model.CatedraIntegration;
import ar.edu.um.turnos.appointment.catedra.domain.ports.out.CatedraIntegrationGateway;
import ar.edu.um.turnos.appointment.catedra.infrastructure.rest.dto.CatedraIntegrationResponse;
import ar.edu.um.turnos.appointment.catedra.infrastructure.rest.mapper.CatedraIntegrationResponseMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class CatedraIntegrationGatewayAdapter implements CatedraIntegrationGateway {

    private static final String INTEGRATION_PATH = "/api/student/integration";

    private final CatedraTechnicalClient technicalClient;
    private final ObjectMapper objectMapper;
    private final CatedraIntegrationResponseMapper responseMapper;

    public CatedraIntegrationGatewayAdapter(CatedraTechnicalClient technicalClient,
                                            ObjectMapper objectMapper,
                                            CatedraIntegrationResponseMapper responseMapper) {
        this.technicalClient = technicalClient;
        this.objectMapper = objectMapper;
        this.responseMapper = responseMapper;
    }

    @Override
    public CatedraIntegration fetchCurrentIntegration() {
        String responseBody = technicalClient.get(INTEGRATION_PATH);
        if (responseBody == null || responseBody.isBlank()) {
            throw new CatedraClientException(
                    "CATEDRA_INVALID_RESPONSE", 503,
                    "The catedra integration response is empty.");
        }

        try {
            CatedraIntegrationResponse response = objectMapper.readValue(
                    responseBody, CatedraIntegrationResponse.class);
            return responseMapper.toDomain(response);
        } catch (JsonProcessingException | IllegalArgumentException failure) {
            throw new CatedraClientException(
                    "CATEDRA_INVALID_RESPONSE", 503,
                    "The catedra integration response could not be parsed or is incomplete.");
        }
    }
}

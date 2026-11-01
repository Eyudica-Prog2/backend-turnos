package ar.edu.um.turnos.appointment.reservation.infrastructure.rest;

import ar.edu.um.turnos.appointment.reservation.domain.model.AppointmentCancellationResult;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.AppointmentCancellationGateway;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import ar.edu.um.turnos.appointment.shared.technical.CatedraTechnicalClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class AppointmentCancellationGatewayAdapter implements AppointmentCancellationGateway {

    private static final String APPOINTMENTS_PATH = "/api/appointments";

    private final CatedraTechnicalClient technicalClient;
    private final ObjectMapper objectMapper;

    public AppointmentCancellationGatewayAdapter(CatedraTechnicalClient technicalClient,
                                                 ObjectMapper objectMapper) {
        this.technicalClient = technicalClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public AppointmentCancellationResult cancel(String reservationProcessId, String reason) {
        String path = UriComponentsBuilder.fromPath(APPOINTMENTS_PATH)
                .pathSegment(reservationProcessId)
                .pathSegment("cancel")
                .build().encode().toUriString();
        String body = reason == null
                ? technicalClient.post(path)
                : technicalClient.post(path, new CancelRequest(reason));
        CancellationResponse response = read(body);
        if (response == null || response.reservationId() == null || response.reservationId() <= 0
                || !reservationProcessId.equals(response.reservationProcessId())
                || !"CANCELLED".equals(response.status()) || response.cancelledAt() == null) {
            throw invalidResponse();
        }
        return new AppointmentCancellationResult(response.reservationId(), response.reservationProcessId(),
                response.status(), response.cancelledAt());
    }

    private CancellationResponse read(String body) {
        if (body == null || body.isBlank()) {
            throw invalidResponse();
        }
        try {
            return objectMapper.readValue(body, CancellationResponse.class);
        } catch (JsonProcessingException malformedResponse) {
            throw invalidResponse();
        }
    }

    private static CatedraClientException invalidResponse() {
        return new CatedraClientException("CATEDRA_INVALID_RESPONSE", 503,
                "The catedra cancellation response is empty or does not match the contract.");
    }

    private record CancelRequest(String reason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CancellationResponse(Long reservationId, String reservationProcessId,
                                        String status, Instant cancelledAt) {
    }
}

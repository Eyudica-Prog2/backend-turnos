package ar.edu.um.turnos.appointment.reservation.infrastructure.rest;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationConfirmResult;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationHold;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.AppointmentHoldGateway;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import ar.edu.um.turnos.appointment.shared.technical.CatedraTechnicalClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class AppointmentHoldGatewayAdapter implements AppointmentHoldGateway {

    private static final String HOLDS_PATH = "/api/appointment-holds";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final CatedraTechnicalClient technicalClient;
    private final ObjectMapper objectMapper;

    public AppointmentHoldGatewayAdapter(CatedraTechnicalClient technicalClient, ObjectMapper objectMapper) {
        this.technicalClient = technicalClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ReservationHold createHold(long professionalId, LocalDate date, LocalTime startTime,
                                      String externalPatientId) {
        HoldRequest request = new HoldRequest(
                professionalId, date.toString(), TIME_FORMAT.format(startTime), externalPatientId);
        String responseBody = technicalClient.post(HOLDS_PATH, request);
        HoldResponse response = read(responseBody, HoldResponse.class);
        if (response == null || blank(response.holdId()) || blank(response.reservationProcessId())
                || !"HELD".equals(response.status()) || response.expiresAt() == null) {
            throw invalidResponse();
        }
        return new ReservationHold(response.holdId(), response.reservationProcessId(),
                response.status(), response.expiresAt(), write(request));
    }

    @Override
    public ReservationConfirmResult confirmHold(String holdId, String reservationProcessId, String externalPatientId,
                                                String patientFirstName, String patientLastName) {
        String path = UriComponentsBuilder.fromPath(HOLDS_PATH)
                .pathSegment(holdId)
                .pathSegment("confirm")
                .build().encode().toUriString();
        ConfirmRequest request = new ConfirmRequest(
                externalPatientId, patientFirstName.trim(), patientLastName.trim());
        try {
            String responseBody = technicalClient.post(path, request);
            ConfirmResponse response = read(responseBody, ConfirmResponse.class);
            if (response == null || blank(response.reservationProcessId()) || blank(response.status())
                    || response.expiresAt() == null) {
                throw invalidResponse();
            }
            return new ReservationConfirmResult(response.reservationProcessId(), response.status(),
                    response.expiresAt(), write(request));
        } catch (CatedraClientException failure) {
            if (failure.getHttpStatus() != 503
                    && !"RESERVATION_PROCESS_ALREADY_CONFIRMED".equals(failure.getCode())) {
                throw failure;
            }
            if (isConfirmedRemotely(reservationProcessId, externalPatientId)) {
                return new ReservationConfirmResult(reservationProcessId, "WAITING_FOR_PHONE", null, write(request));
            }
            throw failure;
        }
    }

    private boolean isConfirmedRemotely(String reservationProcessId, String externalPatientId) {
        String path = UriComponentsBuilder.fromPath("/api/appointments")
                .queryParam("externalPatientId", externalPatientId)
                .queryParam("page", 0)
                .queryParam("size", 100)
                .build().encode().toUriString();
        String responseBody = technicalClient.get(path);
        if (responseBody == null || responseBody.isBlank()) {
            return false;
        }
        try {
            List<AppointmentSummary> appointments = objectMapper.readValue(
                    responseBody, new TypeReference<>() { });
            return appointments != null && appointments.stream().anyMatch(appointment ->
                    reservationProcessId.equals(appointment.reservationProcessId())
                            && ("PHONE_PENDING".equals(appointment.status())
                            || "CONFIRMED".equals(appointment.status())));
        } catch (JsonProcessingException malformedResponse) {
            throw invalidResponse();
        }
    }

    private <T> T read(String responseBody, Class<T> type) {
        if (responseBody == null || responseBody.isBlank()) {
            throw invalidResponse();
        }
        try {
            return objectMapper.readValue(responseBody, type);
        } catch (JsonProcessingException malformedResponse) {
            throw invalidResponse();
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException serializationFailure) {
            throw invalidResponse();
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static CatedraClientException invalidResponse() {
        return new CatedraClientException(
                "CATEDRA_INVALID_RESPONSE", 503,
                "The catedra hold response is empty or does not match the contract.");
    }

    private record HoldRequest(long professionalId, String date, String startTime,
                               String externalPatientId) {
    }

    private record ConfirmRequest(String externalPatientId, String patientFirstName, String patientLastName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record HoldResponse(String holdId, String reservationProcessId, String status,
                                java.time.Instant expiresAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ConfirmResponse(String reservationProcessId, String status,
                                   java.time.Instant expiresAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AppointmentSummary(String reservationProcessId, String status) {
    }
}

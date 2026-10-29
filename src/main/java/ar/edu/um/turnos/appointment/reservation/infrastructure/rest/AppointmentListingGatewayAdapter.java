package ar.edu.um.turnos.appointment.reservation.infrastructure.rest;

import ar.edu.um.turnos.appointment.reservation.domain.model.AppointmentListingPage;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionStatus;
import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationQuery;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.AppointmentListingGateway;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import ar.edu.um.turnos.appointment.shared.technical.CatedraTechnicalClient;
import ar.edu.um.turnos.appointment.shared.technical.CatedraTechnicalResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class AppointmentListingGatewayAdapter implements AppointmentListingGateway {

    private static final String APPOINTMENTS_PATH = "/api/appointments";
    private static final String RESERVATIONS_PATH = "/api/reservations";
    private static final Set<String> ALLOWED_STATUSES = Set.of("PHONE_PENDING", "CONFIRMED", "CANCELLED", "FAILED");
    private static final Pattern LINK_TARGET = Pattern.compile("<([^>]*)>");

    private final CatedraTechnicalClient technicalClient;
    private final ObjectMapper objectMapper;

    public AppointmentListingGatewayAdapter(CatedraTechnicalClient technicalClient, ObjectMapper objectMapper) {
        this.technicalClient = technicalClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public AppointmentListingPage list(UUID ownerPublicId, ReservationQuery query) {
        String path = UriComponentsBuilder.fromPath(APPOINTMENTS_PATH)
                .queryParamIfPresent("status", Optional.ofNullable(query.status()).filter(value -> !value.isBlank()))
                .queryParamIfPresent("from", Optional.ofNullable(query.from()).map(LocalDate::toString))
                .queryParamIfPresent("to", Optional.ofNullable(query.to()).map(LocalDate::toString))
                .queryParam("externalPatientId", ownerPublicId)
                .queryParam("page", query.page())
                .queryParam("size", query.size())
                .build().encode().toUriString();
        CatedraTechnicalResponse response = technicalClient.getWithHeaders(path);
        List<AppointmentResponse> appointments = parseAppointments(response.body());
        long totalCount = parseTotalCount(response.totalCount());
        List<Reservation> reservations = appointments.stream()
                .map(appointment -> toDomain(appointment, ownerPublicId))
                .toList();
        return new AppointmentListingPage(reservations, totalCount, translateLink(response.link()));
    }

    private List<AppointmentResponse> parseAppointments(String body) {
        if (body == null || body.isBlank()) {
            throw invalidResponse();
        }
        try {
            List<AppointmentResponse> appointments = objectMapper.readValue(body, new TypeReference<>() { });
            if (appointments == null || appointments.stream().anyMatch(appointment -> appointment == null
                    || !valid(appointment))) {
                throw invalidResponse();
            }
            return appointments;
        } catch (JsonProcessingException malformedResponse) {
            throw invalidResponse();
        }
    }

    private static boolean valid(AppointmentResponse appointment) {
        return appointment.reservationId() != null && appointment.reservationId() > 0
                && hasText(appointment.reservationProcessId())
                && hasText(appointment.externalPatientId())
                && appointment.appointmentDate() != null
                && appointment.startTime() != null && appointment.endTime() != null
                && appointment.startTime().isBefore(appointment.endTime())
                && appointment.professionalId() != null && appointment.professionalId() > 0
                && hasText(appointment.professionalFirstName()) && hasText(appointment.professionalLastName())
                && hasText(appointment.status()) && ALLOWED_STATUSES.contains(appointment.status())
                && appointment.createdAt() != null;
    }

    private static Reservation toDomain(AppointmentResponse appointment, UUID ownerPublicId) {
        return new Reservation(null, null, appointment.reservationId(), appointment.reservationProcessId(),
                ownerPublicId, appointment.externalPatientId(), appointment.professionalId(),
                appointment.appointmentDate(), appointment.startTime(), appointment.endTime(),
                appointment.professionalFirstName(), appointment.professionalLastName(),
                appointment.patientFirstName(), appointment.patientLastName(), appointment.patientPhone(),
                PhoneSubmissionStatus.NOT_SENT, null, appointment.status(), appointment.createdAt(),
                appointment.confirmedAt(), appointment.cancelledAt(), appointment.cancellationReason());
    }

    private static long parseTotalCount(String header) {
        try {
            long totalCount = Long.parseLong(header);
            if (totalCount < 0) {
                throw invalidResponse();
            }
            return totalCount;
        } catch (NumberFormatException malformedHeader) {
            throw invalidResponse();
        }
    }

    private static String translateLink(String link) {
        if (!hasText(link)) {
            return link;
        }
        Matcher matcher = LINK_TARGET.matcher(link);
        StringBuffer translated = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(translated, Matcher.quoteReplacement(
                    "<" + translateLinkTarget(matcher.group(1)) + ">"));
        }
        matcher.appendTail(translated);
        return translated.toString();
    }

    private static String translateLinkTarget(String target) {
        try {
            var upstream = UriComponentsBuilder.fromUriString(target).build();
            UriComponentsBuilder translated = UriComponentsBuilder.fromPath(RESERVATIONS_PATH);
            upstream.getQueryParams().forEach((name, values) -> {
                if (!"externalPatientId".equals(name)) {
                    translated.queryParam(name, values.toArray());
                }
            });
            return translated.build().encode().toUriString();
        } catch (IllegalArgumentException malformedLink) {
            throw invalidResponse();
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static CatedraClientException invalidResponse() {
        return new CatedraClientException("CATEDRA_INVALID_RESPONSE", 503,
                "The catedra appointments response is empty or does not match the contract.");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AppointmentResponse(Long reservationId, String reservationProcessId, String externalPatientId,
                                       LocalDate appointmentDate, LocalTime startTime, LocalTime endTime,
                                       Long professionalId, String professionalFirstName,
                                       String professionalLastName, String patientFirstName,
                                       String patientLastName, String patientPhone, String status,
                                       Instant createdAt, Instant confirmedAt, Instant cancelledAt,
                                       String cancellationReason) {
    }
}

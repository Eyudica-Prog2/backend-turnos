package ar.edu.um.turnos.appointment.availability.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AppointmentOccupanciesResponse(
        Long professionalId,
        LocalDate from,
        LocalDate to,
        List<OccupancyResponse> occupancies) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OccupancyResponse(
            LocalDate date,
            String startTime,
            String status,
            Instant expiresAt) {
    }
}

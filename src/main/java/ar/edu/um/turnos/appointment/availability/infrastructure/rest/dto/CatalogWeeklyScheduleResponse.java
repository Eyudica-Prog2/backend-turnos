package ar.edu.um.turnos.appointment.availability.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogWeeklyScheduleResponse(
        String dayOfWeek,
        String startTime,
        String endTime,
        Integer slotDurationMinutes) {
}

package ar.edu.um.turnos.appointment.catalog.infrastructure.rest.mapper;

import ar.edu.um.turnos.appointment.catalog.domain.model.WeeklySchedule;
import ar.edu.um.turnos.appointment.catalog.infrastructure.rest.dto.CatalogWeeklyScheduleResponse;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CatalogWeeklyScheduleDtoMapper {

    public List<WeeklySchedule> toDomain(List<CatalogWeeklyScheduleResponse> responses) {
        if (responses == null) {
            throw new IllegalArgumentException("Catalog weekly schedules response is missing.");
        }
        return responses.stream()
                .map(this::toDomain)
                .toList();
    }

    private WeeklySchedule toDomain(CatalogWeeklyScheduleResponse response) {
        if (response == null
                || response.dayOfWeek() == null
                || response.startTime() == null
                || response.endTime() == null
                || response.slotDurationMinutes() == null) {
            throw new IllegalArgumentException("Catalog weekly schedule response is incomplete.");
        }
        return new WeeklySchedule(
                DayOfWeek.valueOf(response.dayOfWeek()),
                LocalTime.parse(response.startTime()),
                LocalTime.parse(response.endTime()),
                response.slotDurationMinutes());
    }
}

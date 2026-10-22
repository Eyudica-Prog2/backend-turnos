package ar.edu.um.turnos.appointment.availability.infrastructure.rest.mapper;

import ar.edu.um.turnos.appointment.availability.domain.model.AppointmentOccupancy;
import ar.edu.um.turnos.appointment.availability.domain.model.OccupancyQuery;
import ar.edu.um.turnos.appointment.availability.domain.model.OccupancyStatus;
import ar.edu.um.turnos.appointment.availability.infrastructure.rest.dto.AppointmentOccupanciesResponse;
import ar.edu.um.turnos.appointment.availability.infrastructure.rest.dto.AppointmentOccupanciesResponse.OccupancyResponse;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AppointmentOccupancyDtoMapper {

    public List<AppointmentOccupancy> toDomain(
            OccupancyQuery query,
            AppointmentOccupanciesResponse response) {
        if (response == null
                || response.professionalId() == null
                || response.professionalId() != query.professionalId()
                || !query.from().equals(response.from())
                || !query.to().equals(response.to())
                || response.occupancies() == null) {
            throw new IllegalArgumentException("The catedra occupancy response does not match the request.");
        }

        List<AppointmentOccupancy> occupancies = new ArrayList<>(response.occupancies().size());
        for (OccupancyResponse occupancy : response.occupancies()) {
            if (occupancy == null || occupancy.date() == null
                    || occupancy.date().isBefore(query.from()) || occupancy.date().isAfter(query.to())
                    || occupancy.startTime() == null || occupancy.status() == null) {
                throw new IllegalArgumentException("The catedra occupancy response contains an invalid entry.");
            }

            OccupancyStatus status = OccupancyStatus.valueOf(occupancy.status());
            occupancies.add(new AppointmentOccupancy(
                    occupancy.date(),
                    LocalTime.parse(occupancy.startTime()),
                    status,
                    occupancy.expiresAt()));
        }
        return List.copyOf(occupancies);
    }
}

package ar.edu.um.turnos.appointment.availability.domain.ports.out;

import ar.edu.um.turnos.appointment.availability.domain.model.AppointmentOccupancy;
import ar.edu.um.turnos.appointment.availability.domain.model.OccupancyQuery;
import java.util.List;

public interface AppointmentOccupancyGateway {

    List<AppointmentOccupancy> findOccupancies(OccupancyQuery query);
}

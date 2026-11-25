package ar.edu.um.turnos.appointment.availability.domain.ports.out;

import java.util.List;

import ar.edu.um.turnos.appointment.availability.domain.model.AppointmentOccupancy;
import ar.edu.um.turnos.appointment.availability.domain.model.OccupancyQuery;

public interface AppointmentOccupancyGateway {

    List<AppointmentOccupancy> findOccupancies(OccupancyQuery query);
}

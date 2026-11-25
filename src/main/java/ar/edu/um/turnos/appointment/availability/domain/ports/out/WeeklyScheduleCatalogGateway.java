package ar.edu.um.turnos.appointment.availability.domain.ports.out;

import java.util.List;

import ar.edu.um.turnos.appointment.availability.domain.model.WeeklySchedule;
import ar.edu.um.turnos.appointment.availability.domain.model.WeeklyScheduleQuery;

public interface WeeklyScheduleCatalogGateway {

    List<WeeklySchedule> findWeeklySchedules(WeeklyScheduleQuery query);
}

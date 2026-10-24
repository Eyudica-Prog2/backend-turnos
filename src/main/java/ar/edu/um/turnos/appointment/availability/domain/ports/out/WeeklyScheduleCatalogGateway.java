package ar.edu.um.turnos.appointment.availability.domain.ports.out;

import ar.edu.um.turnos.appointment.availability.domain.model.WeeklySchedule;
import ar.edu.um.turnos.appointment.availability.domain.model.WeeklyScheduleQuery;
import java.util.List;

public interface WeeklyScheduleCatalogGateway {

    List<WeeklySchedule> findWeeklySchedules(WeeklyScheduleQuery query);
}

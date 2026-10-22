package ar.edu.um.turnos.appointment.catalog.domain.ports.out;

import ar.edu.um.turnos.appointment.catalog.domain.model.WeeklySchedule;
import ar.edu.um.turnos.appointment.catalog.domain.model.WeeklyScheduleQuery;
import java.util.List;

public interface WeeklyScheduleCatalogGateway {

    List<WeeklySchedule> findWeeklySchedules(WeeklyScheduleQuery query);
}
